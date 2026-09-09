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
