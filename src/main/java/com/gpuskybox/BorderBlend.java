package com.gpuskybox;

/**
 * Blend progress for an area change, driven by how far the player has walked from the tile where the
 * border was crossed. Stepping back across the same border reverses the blend instead of restarting it.
 */
class BorderBlend
{
	private boolean active;
	private boolean reversed;
	private int crossX;
	private int crossY;

	void cross(int x, int y)
	{
		active = true;
		reversed = false;
		crossX = x;
		crossY = y;
	}

	/** The player stepped back over the border just crossed: keep the crossing tile, flip direction. */
	void bounce()
	{
		reversed = !reversed;
	}

	boolean active()
	{
		return active;
	}

	/** 0 = still the old sky, 1 = fully the new one. Releases itself once it reaches 1. */
	float progress(int x, int y, int radiusTiles)
	{
		if (!active)
		{
			return 1f;
		}
		if (radiusTiles <= 0)
		{
			active = false;
			return 1f;
		}
		double dist = Math.hypot(x - crossX, y - crossY);
		float t = (float) Math.min(1.0, dist / radiusTiles);
		if (reversed)
		{
			t = 1f - t;
		}
		if (t >= 1f)
		{
			active = false;
			return 1f;
		}
		return t;
	}
}
