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

import java.util.EnumMap;
import java.util.Map;
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
 * One decode per {@link Slot} is in flight at a time and a request for another name supersedes that slot's
 * pending one, whose pixels are dropped when they land: a player crossing three borders in a second uploads only
 * the sky still wanted at the end. Every callback runs on the client thread, the only place a texture may be
 * created.
 * <p>
 * One instance for the whole plugin ({@code @Singleton}): {@link SkyPass} starts and stops it, both sky
 * renderers request through it. Its own fields are read and written on the client thread alone; the loader
 * thread sees nothing but the decode it was handed.
 */
@Singleton
class CubemapLoads
{
	/**
	 * Who is asking. Each slot supersedes only its own request, so the sky and the star map can ask in the same
	 * frame without either dropping the other's pixels; one loader thread serves both, in the order they asked.
	 */
	enum Slot
	{
		/** The cubemap sky, {@link SkyboxRenderer}. */
		SKY,
		/** The bundled star map, {@link StarMap}. */
		STARS
	}

	/** The load in flight for one slot: its name and its job, written and dropped as one value. */
	private static class Pending
	{
		private final String name;
		private final Future<?> future;

		private Pending(String name, Future<?> future)
		{
			this.name = name;
			this.future = future;
		}
	}

	@Inject
	private ClientThread clientThread;

	private ExecutorService executor;
	private Consumer<Runnable> onClientThread;
	private final Map<Slot, Pending> pending = new EnumMap<>(Slot.class);

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
	 * Stops the loader thread and drops every pending decode. Call before the sky textures are freed, so nothing
	 * can upload into a context that is going away. Safe when {@link #start()} never ran: the renderer can fail
	 * to come up before the sky pass is initialised.
	 */
	void stop()
	{
		for (Pending load : pending.values())
		{
			load.future.cancel(false);
		}
		pending.clear();
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
	}

	/**
	 * Decodes {@code name} on the loader thread and hands the pixels to {@code onDecoded} on the client thread,
	 * null when the folder cannot be read. Does nothing when {@code name} is the decode already in flight for
	 * {@code slot}, so a caller that asks every frame submits one job; any other name supersedes that slot's
	 * decode and leaves the other slots alone.
	 */
	void request(Slot slot, String name, Supplier<Faces> decode, Consumer<Faces> onDecoded)
	{
		Pending load = pending.get(slot);
		if (load != null && load.name.equals(name))
		{
			return;
		}
		clear(slot);
		// the client thread is inside this method, so the delivery below cannot run before the slot is filled
		Future<?> future = executor.submit(() ->
		{
			Faces faces = decode.get();
			onClientThread.accept(() -> deliver(slot, name, faces, onDecoded));
		});
		pending.put(slot, new Pending(name, future));
	}

	/** Forgets {@code slot}'s pending decode; its pixels are dropped when they land. */
	void clear(Slot slot)
	{
		Pending load = pending.remove(slot);
		if (load != null)
		{
			// only a job still queued stops here; one already decoding runs on and is dropped by deliver
			load.future.cancel(false);
		}
	}

	/** Client thread: hands the pixels over unless the request was superseded or dropped while decoding. */
	private void deliver(Slot slot, String name, Faces faces, Consumer<Faces> onDecoded)
	{
		Pending load = pending.get(slot);
		if (load == null || !load.name.equals(name))
		{
			return;
		}
		pending.remove(slot);
		onDecoded.accept(faces);
	}

	private static Thread loaderThread(Runnable job)
	{
		Thread thread = new Thread(job, "better-skybox-cubemap");
		thread.setDaemon(true);
		return thread;
	}
}
