package com.gpuskybox;

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
		assertEquals("day", a.skyFor(SkyClock.Phase.DUSK));
		assertEquals("night", a.skyFor(SkyClock.Phase.NIGHT));
	}

	@Test
	public void areaWithoutSkyStaysNull()
	{
		SkyAreas.Area a = new SkyAreas.Area();
		assertNull(a.skyFor(SkyClock.Phase.DAY));
		assertNull(a.skyFor(SkyClock.Phase.NIGHT));
	}
}
