package com.betterskybox;

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
		assertEquals(0f, b.advance(100, 100, 8), 1e-6f);
		assertEquals(0.5f, b.advance(104, 100, 8), 1e-6f);
		assertEquals(1f, b.advance(100, 108, 8), 1e-6f);
		assertEquals(1f, b.advance(120, 120, 8), 1e-6f);
		assertFalse("finished blends release", b.active());
	}

	@Test
	public void bouncingBackContinuesFromTheCurrentValue()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.25f, b.advance(102, 100, 8), 1e-6f);
		// the renderer swapped skies, so the value toward the now-current sky is 0.75, anchored where the player stands
		b.bounce(102, 100);
		assertEquals(0.75f, b.advance(102, 100, 8), 1e-6f);
		// two more tiles in any direction finish it, no pop, no dependence on the original crossing tile
		assertEquals(1f, b.advance(102, 102, 8), 1e-6f);
		assertFalse(b.active());
	}

	@Test
	public void crossingAgainMidBlendContinuesFromTheCurrentValue()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.5f, b.advance(104, 100, 8), 1e-6f);
		// a thin area: the next border arrives before the first blend has finished
		b.cross(104, 100);
		assertTrue(b.active());
		assertEquals(0.5f, b.advance(104, 100, 8), 1e-6f);
		// only the remaining half of the radius is left to walk
		assertEquals(1f, b.advance(108, 100, 8), 1e-6f);
		assertFalse(b.active());
	}

	@Test
	public void releaseHandsTheFadeBackToTheRenderer()
	{
		BorderBlend b = new BorderBlend();
		b.cross(100, 100);
		assertEquals(0.25f, b.advance(102, 100, 8), 1e-6f);
		// teleport: the seconds fade takes over, the distance blend must not pin anything
		b.release();
		assertFalse(b.active());
		assertEquals(1f, b.advance(3200, 3200, 8), 1e-6f);
		// the next walked crossing starts from scratch, not from the abandoned value
		b.cross(3200, 3200);
		assertEquals(0f, b.advance(3200, 3200, 8), 1e-6f);
		// and a bounce right after a release behaves as if nothing had been carried over
		b.release();
		b.bounce(3200, 3200);
		assertEquals(0f, b.advance(3200, 3200, 8), 1e-6f);
	}

	@Test
	public void zeroRadiusIsInstant()
	{
		BorderBlend b = new BorderBlend();
		b.cross(0, 0);
		assertEquals(1f, b.advance(0, 0, 0), 1e-6f);
		assertFalse(b.active());
	}
}
