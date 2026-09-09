/*
 * Copyright (c) 2026, Steven Obst
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.betterskybox;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.callback.ClientThread;
import com.betterskybox.CubemapLoader.Faces;

/**
 * Runs cubemap decodes off the client thread, so crossing into an area with an unseen sky costs no frame time.
 * One decode is in flight at a time and a request for another name supersedes the pending one, whose pixels are
 * dropped when they land: a player crossing three borders in a second uploads only the sky still wanted at the
 * end. Every callback runs on the client thread, the only place a texture may be created.
 * <p>
 * One instance for the whole plugin ({@code @Singleton}): {@link SkyPass} starts and stops it, both sky
 * renderers request through it. Its own fields are read and written on the client thread alone; the loader
 * thread sees nothing but the decode it was handed.
 */
@Singleton
class CubemapLoads
{
	@Inject
	private ClientThread clientThread;

	private ExecutorService executor;
	private Consumer<Runnable> onClientThread;
	private String pendingName;
	private Future<?> pending;

	/** Starts the loader thread. Call from {@link SkyPass#init}. */
	void start()
	{
		start(Executors.newSingleThreadExecutor(CubemapLoads::loaderThread), clientThread::invokeLater);
	}

	/** The seam tests use: a queued executor and an inline dispatcher in place of thread and client thread. */
	void start(ExecutorService executor, Consumer<Runnable> onClientThread)
	{
		this.executor = executor;
		this.onClientThread = onClientThread;
	}

	/**
	 * Stops the loader thread and drops the pending decode. Call before the sky textures are freed, so nothing
	 * can upload into a context that is going away. Safe when {@link #start()} never ran: the renderer can fail
	 * to come up before the sky pass is initialised.
	 */
	void stop()
	{
		clear();
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	/**
	 * Decodes {@code name} on the loader thread and hands the pixels to {@code onDecoded} on the client thread,
	 * null when the folder cannot be read. Does nothing when {@code name} is the decode already in flight, so a
	 * caller that asks every frame submits one job.
	 */
	void request(String name, Supplier<Faces> decode, Consumer<Faces> onDecoded)
	{
		if (name.equals(pendingName))
		{
			return;
		}
		cancel();
		pendingName = name;
		// the client thread is inside this method, so the delivery below cannot run before pending is assigned
		pending = executor.submit(() ->
		{
			Faces faces = decode.get();
			onClientThread.accept(() -> deliver(name, faces, onDecoded));
		});
	}

	/** Forgets the pending decode; its pixels are dropped when they land. */
	void clear()
	{
		cancel();
		pendingName = null;
	}

	/** Client thread: hands the pixels over unless the request was superseded or dropped while decoding. */
	private void deliver(String name, Faces faces, Consumer<Faces> onDecoded)
	{
		if (!name.equals(pendingName))
		{
			return;
		}
		pendingName = null;
		pending = null;
		onDecoded.accept(faces);
	}

	private void cancel()
	{
		if (pending != null)
		{
			// only a job still queued stops here; one already decoding runs on and is dropped by deliver
			pending.cancel(false);
			pending = null;
		}
	}

	private static Thread loaderThread(Runnable job)
	{
		Thread thread = new Thread(job, "better-skybox-cubemap");
		thread.setDaemon(true);
		return thread;
	}
}
