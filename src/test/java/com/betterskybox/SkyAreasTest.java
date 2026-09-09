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
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SkyAreasTest
{
	private static String bundledAreaAt(int x, int y, int plane) throws IOException
	{
		SkyAreas.Area area = new SkyAreas(SkyAreas.bundled(new Gson())).find(new WorldPoint(x, y, plane));
		return area == null ? null : area.name;
	}

	@Test
	public void landmarksResolveToTheirArea() throws IOException
	{
		assertEquals("VER_SINHAZA", bundledAreaAt(3660, 3220, 0));
		assertEquals("BARROWS", bundledAreaAt(3550, 3300, 0));
		assertEquals("MORYTANIA", bundledAreaAt(3700, 3400, 0));
		assertEquals("WILDERNESS_HIGH", bundledAreaAt(3100, 3950, 0));
		assertEquals("WILDERNESS_LOW", bundledAreaAt(3100, 3560, 0));
		assertEquals("KHARIDIAN_DESERT_DEEP", bundledAreaAt(3250, 2900, 0));
		// Lumbridge and Falador fall through to the kingdom boxes at the end of the table
		assertEquals("Misthalin", bundledAreaAt(3222, 3218, 0));
		assertEquals("Asgarnia", bundledAreaAt(2965, 3380, 0));
	}

	@Test
	public void aSpecificAreaWinsOverTheBoxThatContainsIt() throws IOException
	{
		// Darkmeyer and Meiyerditch sit inside the Morytania box; the table lists them first
		assertEquals("DARKMEYER", bundledAreaAt(3600, 3350, 0));
		assertEquals("MEIYERDITCH", bundledAreaAt(3600, 3250, 0));
		// the Frozen Waste Plateau is a corner of the high Wilderness
		assertEquals("FROZEN_WASTE_PLATEAU", bundledAreaAt(2960, 3930, 0));
		// Al Kharid overlaps Misthalin's box and is listed before it
		assertEquals("Al Kharid", bundledAreaAt(3290, 3180, 0));
	}

	@Test
	public void regionsAndRegionBoxesMatchByRegionId() throws IOException
	{
		assertEquals("THE_INFERNO", bundledAreaAt(2270, 5340, 0));
		assertEquals("REVENANT_CAVES", bundledAreaAt(3200, 10100, 0));
	}

	@Test
	public void planeRangesAreHonoured() throws IOException
	{
		assertEquals("KALPHITE_LAIR", bundledAreaAt(3480, 9500, 2));
		assertNull(bundledAreaAt(3480, 9500, 0));
		assertEquals("THE_GAUNTLET_NORMAL", bundledAreaAt(1880, 5660, 1));
		assertNull(bundledAreaAt(1880, 5660, 0));
	}

	@Test
	public void unmappedTilesFindNothing() throws IOException
	{
		assertNull(bundledAreaAt(0, 0, 0));
	}

	@Test
	public void phaseOverridesFallBackToTheDaySky()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		a.sky = "day";
		a.skyNight = "night";
		assertEquals("day", a.skyFor(SkyClock.Phase.DAY));
		assertEquals("day", a.skyFor(SkyClock.Phase.DAWN));
		assertEquals("day", a.skyFor(SkyClock.Phase.DUSK));
		assertEquals("night", a.skyFor(SkyClock.Phase.NIGHT));
	}

	@Test
	public void everyPhaseCanBringItsOwnSky()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		a.sky = "day";
		a.skyDawn = "dawn";
		a.skyDusk = "dusk";
		a.skyNight = "night";
		assertEquals("day", a.skyFor(SkyClock.Phase.DAY));
		assertEquals("dawn", a.skyFor(SkyClock.Phase.DAWN));
		assertEquals("dusk", a.skyFor(SkyClock.Phase.DUSK));
		assertEquals("night", a.skyFor(SkyClock.Phase.NIGHT));
	}

	@Test
	public void areaWithoutSkyStaysNull()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		assertNull(a.skyFor(SkyClock.Phase.DAY));
		assertNull(a.skyFor(SkyClock.Phase.NIGHT));
	}

	@Test
	public void presetStringsResolveToEnumOrNull()
	{
		assertEquals(BetterSkyboxConfig.SkyPreset.BLOOD_MOON, SkyAreas.parsePreset("BLOOD_MOON"));
		assertNull(SkyAreas.parsePreset(null));
		assertNull(SkyAreas.parsePreset("NOT_A_PRESET"));
	}
}
