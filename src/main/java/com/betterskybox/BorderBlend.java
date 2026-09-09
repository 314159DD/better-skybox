package com.betterskybox;

/**
 * Blend progress for an area change, driven by how far the player has walked from the tile where the
 * border was crossed. Stepping back over a border re-anchors at the player's tile and carries the blend
 * value over, so there is never a pop and the blend always finishes within {@code radiusTiles} of walking.
 * <p>
 * The renderer crossfades two textures, so a third sky arriving mid-blend cannot be shown as a three-way
 * mix. A crossing during an active blend continues from the value already reached, which shortens the
 * fade into the newest sky instead of popping to it.
 */
class BorderBlend
{
	private boolean active;
	private int anchorX;
	private int anchorY;
	private float start;
	private float last = 1f;

	/** A new border was crossed at this tile: blend from 0, or from where an unfinished blend had got to. */
	void cross(int x, int y)
	{
		anchor(x, y, active ? last : 0f);
	}

	/** The player stepped back into the previous area: the renderer swapped the skies, so continue from 1 - last. */
	void bounce(int x, int y)
	{
		anchor(x, y, 1f - last);
	}

	/** Hand the transition back to the renderer's seconds fade, as after a teleport or a time-of-day change. */
	void release()
	{
		active = false;
		last = 1f;
	}

	private void anchor(int x, int y, float startProgress)
	{
		active = true;
		anchorX = x;
		anchorY = y;
		start = startProgress;
		last = startProgress;
	}

	boolean active()
	{
		return active;
	}

	/** Moves the blend to the player's tile: 0 = still the old sky, 1 = fully the new one. Releases itself once it reaches 1. */
	float advance(int x, int y, int radiusTiles)
	{
		if (!active)
		{
			return 1f;
		}
		float t = radiusTiles <= 0 ? 1f : (float) Math.min(1.0, start + Math.hypot(x - anchorX, y - anchorY) / radiusTiles);
		last = t;
		if (t >= 1f)
		{
			active = false;
		}
		return t;
	}
}
