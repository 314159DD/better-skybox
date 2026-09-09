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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** The bundled sky_areas.json must only name skies that ship in the jar and only carry well-formed rows. */
public class SkyAreasIntegrityTest
{
	private static final Pattern RGB = Pattern.compile("#[0-9a-fA-F]{6}");

	private static SkyAreas.Area[] bundled() throws IOException
	{
		return SkyAreas.bundled(new Gson());
	}

	@Test
	public void tableIsNotEmpty() throws IOException
	{
		assertTrue(bundled().length > 0);
	}

	@Test
	public void everySkyIsBundled() throws IOException
	{
		for (SkyAreas.Area a : bundled())
		{
			assertSkyBundled(a.name, "sky", a.sky);
			assertSkyBundled(a.name, "skyDawn", a.skyDawn);
			assertSkyBundled(a.name, "skyDusk", a.skyDusk);
			assertSkyBundled(a.name, "skyNight", a.skyNight);
		}
	}

	private static void assertSkyBundled(String area, String field, String sky)
	{
		if (sky != null)
		{
			assertTrue(area + "." + field + " = " + sky, ResourceIntegrityTest.cubemapBundled(sky));
		}
	}

	@Test
	public void everyFogIsRgbHex() throws IOException
	{
		for (SkyAreas.Area a : bundled())
		{
			if (a.fog != null)
			{
				assertTrue(a.name + ".fog = " + a.fog, RGB.matcher(a.fog).matches());
			}
		}
	}

	@Test
	public void everyPresetIsKnown() throws IOException
	{
		for (SkyAreas.Area a : bundled())
		{
			if (a.preset != null)
			{
				assertNotNull(a.name + ".preset = " + a.preset, SkyAreas.parsePreset(a.preset));
			}
		}
	}

	@Test
	public void everyBoxIsSixOrderedInts() throws IOException
	{
		for (SkyAreas.Area a : bundled())
		{
			assertBoxes(a.name, "aabbs", a.aabbs);
			assertBoxes(a.name, "regionBoxes", a.regionBoxes);
		}
	}

	private static void assertBoxes(String area, String field, int[][] boxes)
	{
		if (boxes == null)
		{
			return;
		}
		for (int[] b : boxes)
		{
			String where = area + "." + field + " " + Arrays.toString(b);
			assertEquals(where, 6, b.length);
			assertTrue(where, b[0] <= b[2]);
			assertTrue(where, b[1] <= b[3]);
			assertTrue(where, b[4] <= b[5]);
		}
	}
}
