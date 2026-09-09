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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An executor that holds its jobs until {@link #runAll()}, so a test can look at a load while it is pending.
 * The future it hands back is never completed and cancelling it does not unqueue the job: that is a decode
 * already under way, which is the case the supersede bookkeeping exists for.
 */
final class QueuedExecutor extends AbstractExecutorService
{
	private final Deque<Runnable> queued = new ArrayDeque<>();
	private boolean shutdown;

	@Override
	public void execute(Runnable job)
	{
		queued.add(job);
	}

	@Override
	public Future<?> submit(Runnable job)
	{
		execute(job);
		return new CompletableFuture<Void>();
	}

	/** Runs every queued job, in the order it was submitted. */
	void runAll()
	{
		while (!queued.isEmpty())
		{
			queued.poll().run();
		}
	}

	@Override
	public void shutdown()
	{
		shutdown = true;
	}

	@Override
	public List<Runnable> shutdownNow()
	{
		shutdown = true;
		List<Runnable> rest = new ArrayList<>(queued);
		queued.clear();
		return rest;
	}

	@Override
	public boolean isShutdown()
	{
		return shutdown;
	}

	@Override
	public boolean isTerminated()
	{
		return shutdown && queued.isEmpty();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit)
	{
		return isTerminated();
	}
}
