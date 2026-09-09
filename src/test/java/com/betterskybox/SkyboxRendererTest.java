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
import com.betterskybox.CubemapLoader.Cubemap;
import com.betterskybox.CubemapLoader.Faces;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkyboxRendererTest
{
	private final QueuedExecutor loaderThread = new QueuedExecutor();
	private final List<String> decoded = new ArrayList<>();
	private final List<String> uploaded = new ArrayList<>();

	/** A renderer whose decodes sit in {@link #loaderThread} until the test runs them; deliveries are inline. */
	private SkyboxRenderer renderer()
	{
		CubemapLoads loads = new CubemapLoads();
		loads.start(loaderThread, Runnable::run);
		return new SkyboxRenderer(loader(decoded), loads);
	}

	/** A loader that records what it decodes and uploads, fails for names starting with "bad", has no GL. */
	private SkyboxRenderer.Loader loader(List<String> decoded)
	{
		return new SkyboxRenderer.Loader()
		{
			@Override
			public Faces decode(String name)
			{
				decoded.add(name);
				return name.startsWith("bad") ? null : new Faces(name, 1, new int[6][1], 0x112233);
			}

			@Override
			public Cubemap upload(Faces faces, int textureUnit)
			{
				uploaded.add(faces.name);
				Cubemap cubemap = new Cubemap();
				cubemap.horizonColor = faces.horizonColor;
				return cubemap;
			}
		};
	}

	@Test
	public void failedNamesAreAllRememberedUntilRetry()
	{
		SkyboxRenderer renderer = renderer();
		// nothing is known to be bad on the frame that asks: the failure is only known once the decode lands
		assertTrue(renderer.select("bad-area", 1f));
		loaderThread.runAll();
		assertTrue(renderer.select("bad-default", 1f));
		loaderThread.runAll();
		// an area sky and its fallback both missing must not thrash between the two every frame
		assertFalse(renderer.select("bad-area", 1f));
		assertFalse(renderer.select("bad-default", 1f));
		assertEquals(Arrays.asList("bad-area", "bad-default"), decoded);

		renderer.retryFailed();
		assertTrue(renderer.select("bad-area", 1f));
		loaderThread.runAll();
		assertEquals(3, decoded.size());
		assertFalse(renderer.select("bad-area", 1f));
	}

	@Test
	public void emptyNameIsNeverLoaded()
	{
		assertFalse(renderer().select("", 1f));
		loaderThread.runAll();
		assertTrue(decoded.isEmpty());
	}

	@Test
	public void loadedCubemapsStayResident()
	{
		SkyboxRenderer renderer = renderer();
		assertTrue(renderer.select("sky", 1f));
		loaderThread.runAll();
		assertTrue(renderer.select("other", 1f));
		loaderThread.runAll();
		assertTrue(renderer.select("sky", 1f));
		loaderThread.runAll();
		assertEquals(Arrays.asList("sky", "other"), decoded);
	}

	@Test
	public void onlyTheSkyStillWantedIsUploaded()
	{
		// two borders crossed before the first decode lands; the sky left behind is never uploaded
		SkyboxRenderer renderer = renderer();
		renderer.select("first", 1f);
		renderer.select("second", 1f);
		loaderThread.runAll();
		assertEquals(Collections.singletonList("second"), uploaded);
	}

	@Test
	public void horizonColorIsTheFallbackUntilTheDecodeLands()
	{
		SkyboxRenderer renderer = renderer();
		assertEquals(0xABCDEF, renderer.getHorizonColor(0xABCDEF));
		renderer.select("sky", 1f);
		// the frame does not wait for the decode: it draws on with the game colour
		assertEquals(0xABCDEF, renderer.getHorizonColor(0xABCDEF));
		loaderThread.runAll();
		assertEquals(0x112233, renderer.getHorizonColor(0xABCDEF));
	}

	@Test
	public void mixColorLerpsEachChannel()
	{
		// the lightning brightening: 60 % of the way from black to white
		assertEquals(0x999999, SkyboxRenderer.mixColor(0x000000, 0xFFFFFF, 0.6f));
		assertEquals(0x80C0FF, SkyboxRenderer.mixColor(0x0080FF, 0xFFFFFF, 0.5f));
	}

	@Test
	public void mixColorEndpointsAreExact()
	{
		// no flash leaves the fog colour untouched, a full blend is the target colour
		assertEquals(0x123456, SkyboxRenderer.mixColor(0x123456, 0xFFFFFF, 0f));
		assertEquals(0xFFFFFF, SkyboxRenderer.mixColor(0x123456, 0xFFFFFF, 1f));
	}
}
