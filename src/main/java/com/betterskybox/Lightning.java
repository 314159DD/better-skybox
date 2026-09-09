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
