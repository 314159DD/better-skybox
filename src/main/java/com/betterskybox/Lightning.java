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

/**
 * Schedules lightning strikes and shapes each one as a short burst of 1 to 3 flickers. Pure: feed it the
 * current time in seconds and whether the player stands in a stormy area.
 */
class Lightning
{
	private static final double MIN_GAP = 6;
	private static final double MAX_GAP = 22;
	private static final double FLICKER_LENGTH = 0.09;
	private static final double FLICKER_GAP = 0.07;

	private final Random random;
	private double nextStrike = -1;
	private int flickers;
	private float strength;

	Lightning(Random random)
	{
		this.random = random;
	}

	/** 0 = no flash, 1 = full white-out. */
	float intensity(double now, boolean active)
	{
		if (!active)
		{
			nextStrike = -1;
			return 0f;
		}
		if (nextStrike < 0)
		{
			schedule(now);
			return 0f;
		}
		double sinceStrike = now - nextStrike;
		if (sinceStrike < 0)
		{
			return 0f;
		}
		double burstLength = flickers * (FLICKER_LENGTH + FLICKER_GAP);
		if (sinceStrike >= burstLength)
		{
			schedule(now);
			return 0f;
		}
		double inFlicker = sinceStrike % (FLICKER_LENGTH + FLICKER_GAP);
		if (inFlicker >= FLICKER_LENGTH)
		{
			return 0f;
		}
		// sharp attack, exponential-ish decay inside each flicker
		float t = (float) (inFlicker / FLICKER_LENGTH);
		return strength * (1f - t * t);
	}

	private void schedule(double now)
	{
		nextStrike = now + MIN_GAP + random.nextDouble() * (MAX_GAP - MIN_GAP);
		flickers = 1 + random.nextInt(3);
		strength = 0.7f + random.nextFloat() * 0.3f;
	}
}
