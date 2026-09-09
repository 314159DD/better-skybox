package com.gpuskybox;

import java.time.LocalDateTime;
import com.gpuskybox.GpuSkyboxConfig.SkyPreset;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkyClockTest
{
	private static GpuSkyboxConfig cfg(SkyPreset p)
	{
		return new GpuSkyboxConfig()
		{
			@Override
			public SkyPreset skyPreset()
			{
				return p;
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
