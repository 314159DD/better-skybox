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

import java.util.function.Supplier;
import javax.inject.Inject;
import com.betterskybox.CubemapLoads.Slot;
import com.betterskybox.CubemapLoader.Cubemap;
import com.betterskybox.CubemapLoader.Faces;
import static org.lwjgl.opengl.GL33C.*;

/**
 * The bundled NASA Deep Star Map, shared by both skies: the procedural sky draws it instead of generated stars,
 * the cubemap sky draws it over night cubemaps. One instance for the plugin, handed to both renderers by
 * {@link SkyPass}, so the 9.6 MB cubemap is decoded once and lives on one texture unit.
 * <p>
 * Every method touches GL or the load state and belongs on the client thread.
 */
class StarMap
{
	/** Neither cubemap sky slot (2, 3) nor the scene textures (0, 1) may use this one. */
	static final int TEXTURE_UNIT = 4;
	/** Folder of the bundled star map cubemap. */
	private static final String STARS = "stars";
	/** Tilt of the celestial pole towards the horizon; puts the Milky Way in an arc instead of a ring. */
	private static final double POLE_TILT = Math.toRadians(40);

	private final CubemapLoads loads;
	/** The decode itself, so a test can stand in for the bundled folder instead of reading 9.6 MB of it. */
	private final Supplier<Faces> decode;

	private Cubemap stars;
	/** Set when the folder cannot be read, so a broken custom one is not handed to the loader every frame. */
	private boolean failed;
	private final float[] rot = new float[9];

	@Inject
	StarMap(CubemapLoads loads)
	{
		this(loads, () -> CubemapLoader.decode(STARS));
	}

	StarMap(CubemapLoads loads, Supplier<Faces> decode)
	{
		this.loads = loads;
		this.decode = decode;
	}

	/**
	 * Asks for the star map on the first frame it is wanted and keeps it while its setting is on, so a session
	 * that never shows stars does not decode it, no frame waits for the decode, and a night that ends does not
	 * throw away pixels the next one would pay for again. Call every frame.
	 *
	 * @param wanted    whether a sky draws the star map this frame
	 * @param settingOn whether the setting behind it is on: Star map for the procedural sky, Night stars for the
	 *                  cubemap sky. The texture is freed when that goes off, and on shutdown.
	 */
	void want(boolean wanted, boolean settingOn)
	{
		if (!settingOn)
		{
			free();
			return;
		}
		if (wanted && stars == null && !failed)
		{
			loads.request(Slot.STARS, STARS, decode, this::upload);
		}
	}

	/**
	 * Drops the texture and any decode still on its way, and lets a folder that could not be read be tried
	 * again. Call while the context is still alive.
	 */
	void free()
	{
		loads.clear(Slot.STARS);
		failed = false;
		if (stars != null)
		{
			glDeleteTextures(stars.texture);
			stars = null;
		}
	}

	/** Forgets a folder that could not be read, so it gets another attempt. Call on the client thread. */
	void retryFailed()
	{
		failed = false;
	}

	/** Whether the pixels have landed; until they do, neither sky may sample the star map. */
	boolean loaded()
	{
		return stars != null;
	}

	/**
	 * Binds the star map to its unit and sets the rotation uniform of the bound program. The sampler itself is
	 * set by both renderers on every frame, drawn stars or not, so it never points at another unit.
	 */
	void bind(int uniStarRot, float hour)
	{
		glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, stars.texture);
		rotation(hour, rot);
		glUniformMatrix3fv(uniStarRot, false, rot);
	}

	/** Client thread: null pixels mean the folder could not be read, and it is not asked for again. */
	private void upload(Faces faces)
	{
		if (faces == null)
		{
			failed = true;
			return;
		}
		stars = CubemapLoader.upload(faces, TEXTURE_UNIT);
	}

	/**
	 * Column-major 3x3 that takes a world-space view direction (y down) to a star map lookup: flip to y up, turn
	 * the sky once per day around the vertical axis, then tilt the celestial pole towards the horizon.
	 */
	static void rotation(float hour, float[] out)
	{
		double a = hour / 24 * 2 * Math.PI;
		double ca = Math.cos(a), sa = Math.sin(a);
		double ct = Math.cos(POLE_TILT), st = Math.sin(POLE_TILT);
		// M = Rx(tilt) * Ry(a) * diag(1, -1, 1); columns are M applied to the unit axes
		out[0] = (float) ca;
		out[1] = (float) (st * sa);
		out[2] = (float) (-ct * sa);
		out[3] = 0;
		out[4] = (float) -ct;
		out[5] = (float) -st;
		out[6] = (float) sa;
		out[7] = (float) (-st * ca);
		out[8] = (float) (ct * ca);
	}
}
