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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.betterskybox.CubemapLoads.Slot;
import com.betterskybox.CubemapLoader.Faces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The decode side of a cubemap load, without threads: jobs sit in a {@link QueuedExecutor} until the test says
 * to run them, and the deliveries that would go to the client thread run inline.
 */
public class CubemapLoadsTest
{
	private final QueuedExecutor loaderThread = new QueuedExecutor();
	private final List<Runnable> clientThread = new ArrayList<>();
	private final CubemapLoads loads = new CubemapLoads();
	private final List<String> decoded = new ArrayList<>();
	private final List<String> delivered = new ArrayList<>();

	public CubemapLoadsTest()
	{
		loads.start(loaderThread, clientThread::add);
	}

	private void request(String name)
	{
		request(Slot.SKY, name);
	}

	private void request(Slot slot, String name)
	{
		loads.request(slot, name, () -> decode(name),
			faces -> delivered.add(faces == null ? name + " failed" : faces.name));
	}

	private Faces decode(String name)
	{
		decoded.add(name);
		return name.startsWith("bad") ? null : new Faces(name, 1, new int[6][1], 0x112233);
	}

	private void runClientThread()
	{
		clientThread.forEach(Runnable::run);
		clientThread.clear();
	}

	@Test
	public void theNameInFlightIsNotSubmittedAgain()
	{
		// beginFrame asks for the same sky every frame until it lands
		request("sky");
		request("sky");
		request("sky");
		loaderThread.runAll();
		runClientThread();
		assertEquals(Collections.singletonList("sky"), decoded);
		assertEquals(Collections.singletonList("sky"), delivered);
	}

	@Test
	public void aNewerNameSupersedesTheOneInFlight()
	{
		// a second border crossed while the first sky is still decoding: only the sky still wanted is handed over
		request("first");
		request("second");
		loaderThread.runAll();
		runClientThread();
		assertEquals(Arrays.asList("first", "second"), decoded);
		assertEquals(Collections.singletonList("second"), delivered);
	}

	@Test
	public void twoSlotsAskingForDifferentNamesBothLand()
	{
		// beginFrame asks for the area's sky and then for the star map: neither may supersede the other
		request(Slot.SKY, "sky");
		request(Slot.STARS, "stars");
		loaderThread.runAll();
		runClientThread();
		assertEquals(Arrays.asList("sky", "stars"), decoded);
		assertEquals(Arrays.asList("sky", "stars"), delivered);
	}

	@Test
	public void aSupersededNameLeavesTheOtherSlotAlone()
	{
		// the star map is still decoding while the player crosses two borders
		request(Slot.STARS, "stars");
		request(Slot.SKY, "first");
		request(Slot.SKY, "second");
		loaderThread.runAll();
		runClientThread();
		assertEquals(Arrays.asList("stars", "first", "second"), decoded);
		assertEquals(Arrays.asList("stars", "second"), delivered);
	}

	@Test
	public void stopDropsPixelsThatWereAlreadyOnTheirWay()
	{
		// the plugin is shutting down: this delivery would upload into a context that is being torn down
		request("sky");
		loaderThread.runAll();
		loads.stop();
		runClientThread();
		assertEquals(Collections.singletonList("sky"), decoded);
		assertTrue(delivered.isEmpty());
	}

	@Test
	public void aFolderThatCannotBeReadIsDeliveredAsNull()
	{
		request("bad-sky");
		loaderThread.runAll();
		runClientThread();
		assertEquals(Collections.singletonList("bad-sky failed"), delivered);
	}
}
