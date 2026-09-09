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
import java.util.List;
import org.junit.Test;
import com.betterskybox.CubemapLoader.Cubemap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SkyboxRendererTest
{
	/** A loader that records every attempt and fails for names starting with "bad". */
	private static SkyboxRenderer renderer(List<String> attempts)
	{
		return new SkyboxRenderer((name, unit) ->
		{
			attempts.add(name);
			if (name.startsWith("bad"))
			{
				return null;
			}
			Cubemap cubemap = new Cubemap();
			cubemap.horizonColor = 0x112233;
			return cubemap;
		});
	}

	@Test
	public void failedNamesAreAllRememberedUntilRetry()
	{
		List<String> attempts = new ArrayList<>();
		SkyboxRenderer renderer = renderer(attempts);
		assertFalse(renderer.select("bad-area", 1f));
		assertFalse(renderer.select("bad-default", 1f));
		// an area sky and its fallback both missing must not thrash between the two every frame
		assertFalse(renderer.select("bad-area", 1f));
		assertFalse(renderer.select("bad-default", 1f));
		assertEquals(Arrays.asList("bad-area", "bad-default"), attempts);

		renderer.retryFailed();
		assertFalse(renderer.select("bad-area", 1f));
		assertEquals(3, attempts.size());
	}

	@Test
	public void emptyNameIsNeverLoaded()
	{
		List<String> attempts = new ArrayList<>();
		assertFalse(renderer(attempts).select("", 1f));
		assertTrue(attempts.isEmpty());
	}

	@Test
	public void loadedCubemapsStayResident()
	{
		List<String> attempts = new ArrayList<>();
		SkyboxRenderer renderer = renderer(attempts);
		assertTrue(renderer.select("sky", 1f));
		assertTrue(renderer.select("other", 1f));
		assertTrue(renderer.select("sky", 1f));
		assertEquals(Arrays.asList("sky", "other"), attempts);
	}

	@Test
	public void horizonColorIsTheFallbackBeforeTheFirstCubemap()
	{
		SkyboxRenderer renderer = renderer(new ArrayList<>());
		assertEquals(0xABCDEF, renderer.getHorizonColor(0xABCDEF));
		renderer.select("sky", 1f);
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
