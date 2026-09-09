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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SkyAreasTest
{
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
