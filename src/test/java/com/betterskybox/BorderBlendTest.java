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
