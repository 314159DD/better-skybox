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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.betterskybox.BetterSkyboxConfig.SkyPreset;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkyClockTest
{
	private static BetterSkyboxConfig cfg(SkyPreset p)
	{
		return cfg(p, 24);
	}

	private static BetterSkyboxConfig cfg(SkyPreset p, int cycleMinutes)
	{
		return new BetterSkyboxConfig()
		{
			@Override
			public SkyPreset skyPreset()
			{
				return p;
			}

			@Override
			public int cycleMinutes()
			{
				return cycleMinutes;
			}
		};
	}

	@Test
	public void phasesCoverTheDay()
	{
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(0f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(4.99f));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(5f));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(6.99f));
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(7f));
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(16.99f));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(17f));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(19.99f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(20f));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(23.99f));
	}

	@Test
	public void sunGeometryRoundTrips()
	{
		assertEquals(60f, SkyClock.altitudeForHour(12f), 1e-4f);
		assertEquals(-60f, SkyClock.altitudeForHour(0f), 1e-4f);
		assertEquals(90f, SkyClock.azimuthForHour(6f), 1e-4f);
		assertEquals(180f, SkyClock.azimuthForHour(12f), 1e-4f);
		assertEquals(12f, SkyClock.hourForAzimuth(180f), 1e-4f);
		assertEquals(0f, SkyClock.hourForAzimuth(0f), 1e-4f);
	}

	@Test
	public void presetsMapToTheirPhase()
	{
		SkyClock c = new SkyClock();
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(c.hour(cfg(SkyPreset.DAY))));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(c.hour(cfg(SkyPreset.SUNSET))));
		assertEquals(SkyClock.Phase.DUSK, SkyClock.phase(c.hour(cfg(SkyPreset.DUSK))));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(c.hour(cfg(SkyPreset.SUNRISE))));
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(c.hour(cfg(SkyPreset.DAWN))));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(c.hour(cfg(SkyPreset.NIGHT))));
		assertEquals(SkyClock.Phase.NIGHT, SkyClock.phase(c.hour(cfg(SkyPreset.BLOOD_MOON))));
		// CUSTOM follows the azimuth slider, whose default is the DAY sun
		assertEquals(SkyClock.Phase.DAY, SkyClock.phase(c.hour(cfg(SkyPreset.CUSTOM))));
	}

	@Test
	public void fixedPresetsLeaveTheMoonToTheSlider()
	{
		SkyClock c = new SkyClock();
		assertEquals(-1f, c.moonIllumination(cfg(SkyPreset.DAY)), 0f);
		assertEquals(-1f, c.moonIllumination(cfg(SkyPreset.BLOOD_MOON)), 0f);
		assertEquals(-1f, c.moonIllumination(cfg(SkyPreset.CUSTOM)), 0f);
	}

	@Test
	public void cycleStartsAtDawnWithANewMoon()
	{
		SkyClock c = new SkyClock();
		float elapsed = c.elapsedSeconds();
		assertTrue(elapsed >= 0f && elapsed < 1f);
		// a fresh clock is at the start of its first cycle: 6h into the sky day, moon unlit
		assertEquals(6f, c.hour(cfg(SkyPreset.CYCLE)), 0.05f);
		assertEquals(SkyClock.Phase.DAWN, SkyClock.phase(c.hour(cfg(SkyPreset.CYCLE))));
		assertEquals(0f, c.moonIllumination(cfg(SkyPreset.CYCLE)), 0.01f);
	}

	@Test
	public void cycleRunsTwoWholeDaysOnAnInjectedClock()
	{
		long[] nanos = {0};
		SkyClock c = new SkyClock(() -> nanos[0]);
		// one real minute per sky day, the in-game checklist setting
		BetterSkyboxConfig config = cfg(SkyPreset.CYCLE, 1);
		assertEquals(6f, c.hour(config), 1e-4f);

		List<SkyClock.Phase> entered = new ArrayList<>();
		SkyClock.Phase lastPhase = null;
		float lastHour = c.hour(config);
		int wraps = 0;
		// half a second per step, 0.2 sky hours, for two full days
		for (int step = 1; step <= 240; step++)
		{
			nanos[0] = step * 500_000_000L;
			float hour = c.hour(config);
			assertTrue("hour " + hour, hour >= 0f && hour < 24f);
			if (hour < lastHour)
			{
				wraps++;
			}
			else
			{
				assertTrue("hour " + hour + " after " + lastHour, hour > lastHour);
			}
			SkyClock.Phase phase = SkyClock.phase(hour);
			if (phase != lastPhase)
			{
				entered.add(phase);
				lastPhase = phase;
			}
			lastHour = hour;
		}
		assertEquals(2, wraps);
		// starting mid-dawn, each phase is entered once per day and dawn once more at the very end
		assertEquals(List.of(
			SkyClock.Phase.DAWN, SkyClock.Phase.DAY, SkyClock.Phase.DUSK, SkyClock.Phase.NIGHT,
			SkyClock.Phase.DAWN, SkyClock.Phase.DAY, SkyClock.Phase.DUSK, SkyClock.Phase.NIGHT,
			SkyClock.Phase.DAWN), entered);
		// two whole days land back on the start hour, no drift
		assertEquals(6f, c.hour(config), 1e-3f);
	}

	@Test
	public void cycleMoonWaxesToFullAfterHalfASynodicMonthOfDays()
	{
		long[] nanos = {0};
		SkyClock c = new SkyClock(() -> nanos[0]);
		BetterSkyboxConfig config = cfg(SkyPreset.CYCLE, 1);
		nanos[0] = (long) (29.530588 / 2 * 60 * 1e9);
		assertEquals(1f, c.moonIllumination(config), 1e-3f);
	}

	@Test
	public void moonIlluminationFollowsTheCalendar()
	{
		// reference new moon: 2000-01-06 18:14 UTC
		assertEquals(0f, SkyClock.moonIllumination(LocalDateTime.of(2000, 1, 6, 18, 14)), 1e-3f);
		// half a synodic month later it is full
		assertEquals(1f, SkyClock.moonIllumination(LocalDateTime.of(2000, 1, 21, 4, 36)), 1e-2f);
		// first quarter: roughly half lit
		assertEquals(0.5f, SkyClock.moonIllumination(LocalDateTime.of(2000, 1, 14, 0, 0)), 0.05f);
		// one synodic month before the reference: negative day counts still land on a new moon
		assertEquals(0f, SkyClock.moonIllumination(LocalDateTime.of(1999, 12, 8, 6, 0)), 0.05f);
	}
}
