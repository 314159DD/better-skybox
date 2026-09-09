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

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LightningTest
{
	@Test
	public void inactiveIsAlwaysDark()
	{
		Lightning l = new Lightning(new Random(1));
		for (double t = 0; t < 60; t += 0.1)
		{
			assertEquals(0f, l.intensity(t, false), 0f);
		}
	}

	@Test
	public void strikesHappenAndFadeWithinAFewFrames()
	{
		Lightning l = new Lightning(new Random(7));
		int litFrames = 0;
		int frames = 0;
		float peak = 0;
		for (double t = 0; t < 120; t += 1 / 50.0)
		{
			float i = l.intensity(t, true);
			assertTrue(i >= 0f && i <= 1f);
			frames++;
			if (i > 0.05f)
			{
				litFrames++;
			}
			peak = Math.max(peak, i);
		}
		assertTrue("at least one strike in two minutes", peak > 0.8f);
		assertTrue("lit less than 5% of the time", litFrames < frames / 20);
	}

	@Test
	public void deactivatingResetsTheSchedule()
	{
		Lightning l = new Lightning(new Random(3));
		l.intensity(0, true);
		l.intensity(100, false);
		// after a reset the very next active frame must not be mid-flash
		assertEquals(0f, l.intensity(100.01, true), 0f);
	}
}
