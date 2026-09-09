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
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** The star map rotation: a rigid turn of the sky around the vertical axis, once per sky day. */
public class StarMapTest
{
	private static float[] rotation(float hour)
	{
		float[] out = new float[9];
		StarMap.rotation(hour, out);
		return out;
	}

	/** {@code m} times {@code v}, with m column-major as GL takes it. */
	private static float[] apply(float[] m, float x, float y, float z)
	{
		return new float[]{
			m[0] * x + m[3] * y + m[6] * z,
			m[1] * x + m[4] * y + m[7] * z,
			m[2] * x + m[5] * y + m[8] * z};
	}

	@Test
	public void theSkyTurnsAroundTheZenith()
	{
		// straight up in world space is -y; it must look at the same star all day, or the sky would wobble
		float[] noon = apply(rotation(12), 0, -1, 0);
		for (float hour : new float[]{0, 3.5f, 6, 18, 23.9f})
		{
			assertArrayEquals("hour " + hour, noon, apply(rotation(hour), 0, -1, 0), 1e-6f);
		}
	}

	@Test
	public void halfADayTurnsTheSkyHalfway()
	{
		float[] north = apply(rotation(0), 1, 0, 0);
		float[] later = apply(rotation(12), 1, 0, 0);
		for (int i = 0; i < 3; i++)
		{
			assertEquals(-north[i], later[i], 1e-6f);
		}
	}

	@Test
	public void theRotationRepeatsEveryDay()
	{
		assertArrayEquals(rotation(3), rotation(27), 1e-6f);
	}

	@Test
	public void theRotationKeepsLengthsAndAngles()
	{
		// the star map is sampled with the raw view direction, so a non-rigid matrix would smear the stars
		float[] m = rotation(7.5f);
		float[] x = apply(m, 1, 0, 0);
		float[] y = apply(m, 0, 1, 0);
		assertEquals(1f, x[0] * x[0] + x[1] * x[1] + x[2] * x[2], 1e-6f);
		assertEquals(1f, y[0] * y[0] + y[1] * y[1] + y[2] * y[2], 1e-6f);
		assertEquals(0f, x[0] * y[0] + x[1] * y[1] + x[2] * y[2], 1e-6f);
	}
}
