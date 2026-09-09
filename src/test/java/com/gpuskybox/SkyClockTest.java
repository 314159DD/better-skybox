package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SkyClockTest
{
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
}
