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
	public void bouncingBackReversesInsteadOfRestarting()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.25f, b.progress(102, 100, 8), 1e-6f);
		b.bounce();
		// now 2 tiles from the crossing means 2 tiles back INTO the old area: 1 - 0.25
		assertEquals(0.75f, b.progress(102, 100, 8), 1e-6f);
		assertEquals(1f, b.progress(100, 100, 8), 1e-6f);
	}

	@Test
	public void zeroRadiusIsInstant()
	{
		BorderBlend b = new BorderBlend();
		b.cross(0, 0);
		assertEquals(1f, b.progress(0, 0, 0), 1e-6f);
	}
}
