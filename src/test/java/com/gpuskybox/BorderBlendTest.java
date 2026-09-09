package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BorderBlendTest
{
	@Test
	public void progressGrowsWithDistanceFromTheCrossing()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertTrue(b.active());
		assertEquals(0f, b.progress(100, 100, 8), 1e-6f);
		assertEquals(0.5f, b.progress(104, 100, 8), 1e-6f);
		assertEquals(1f, b.progress(100, 108, 8), 1e-6f);
		assertEquals(1f, b.progress(120, 120, 8), 1e-6f);
		assertFalse("finished blends release", b.active());
	}

	@Test
	public void bouncingBackContinuesFromTheCurrentValue()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.25f, b.progress(102, 100, 8), 1e-6f);
		// the renderer swapped skies, so the value toward the now-current sky is 0.75, anchored where the player stands
		b.bounce(102, 100);
		assertEquals(0.75f, b.progress(102, 100, 8), 1e-6f);
		// two more tiles in any direction finish it, no pop, no dependence on the original crossing tile
		assertEquals(1f, b.progress(102, 102, 8), 1e-6f);
		assertFalse(b.active());
	}

	@Test
	public void zeroRadiusIsInstant()
	{
		BorderBlend b = new BorderBlend();
		b.cross(0, 0);
		assertEquals(1f, b.progress(0, 0, 0), 1e-6f);
		assertFalse(b.active());
	}
}
