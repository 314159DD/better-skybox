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

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.betterskybox.CubemapLoader.Cubemap;
import com.betterskybox.template.Template;
import static org.lwjgl.opengl.GL33C.*;

/**
 * Draws a cubemap sky as a fullscreen pass before the scene.
 * Loaded cubemaps stay resident so area changes only pay the decode once; switching crossfades.
 */
@Slf4j
class SkyboxRenderer
{
	static final Shader PROGRAM = new Shader()
		.add(GL_VERTEX_SHADER, "sky_vert.glsl")
		.add(GL_FRAGMENT_SHADER, "sky_frag.glsl");

	static final int TEXTURE_UNIT = 2;
	static final int PREV_TEXTURE_UNIT = 3;

	private int program;
	private final Map<String, Cubemap> loaded = new HashMap<>();
	private String failedName;
	private Cubemap current;
	private Cubemap previous;
	private long fadeStartNanos;
	private float fadeSeconds;
	private float blendOverride = -1f;

	private int uniSkyProj;
	private int uniCubemap;
	private int uniPrevCubemap;
	private int uniBlend;
	private int uniFogColor;
	private int uniHorizonBlend;
	private int uniHorizonOffset;
	private int uniFogTint;
	private int uniBrightness;
	private int uniFlash;
	private int uniRotation;

	private final long startNanos = System.nanoTime();

	void initProgram(Template template) throws ShaderException
	{
		program = PROGRAM.compile(template);
		uniSkyProj = glGetUniformLocation(program, "skyProj");
		uniCubemap = glGetUniformLocation(program, "cubemap");
		uniPrevCubemap = glGetUniformLocation(program, "prevCubemap");
		uniBlend = glGetUniformLocation(program, "blend");
		uniFogColor = glGetUniformLocation(program, "fogColor");
		uniHorizonBlend = glGetUniformLocation(program, "horizonBlend");
		uniHorizonOffset = glGetUniformLocation(program, "horizonOffset");
		uniFogTint = glGetUniformLocation(program, "fogTint");
		uniBrightness = glGetUniformLocation(program, "brightness");
		uniFlash = glGetUniformLocation(program, "flash");
		uniRotation = glGetUniformLocation(program, "rotation");
	}

	void shutdownProgram()
	{
		glDeleteProgram(program);
		program = 0;
	}

	/**
	 * Makes {@code name} the current cubemap, loading it on first use, and starts a crossfade from the one shown
	 * before. Returns false when the cubemap cannot be loaded; that name is then skipped until {@link #retryFailed()}.
	 */
	boolean select(String name, float fadeSeconds)
	{
		if (name.isEmpty() || name.equals(failedName))
		{
			return false;
		}
		Cubemap next = loaded.get(name);
		if (next == null)
		{
			next = CubemapLoader.upload(name, TEXTURE_UNIT);
			if (next == null)
			{
				failedName = name;
				return false;
			}
			loaded.put(name, next);
		}
		if (next != current)
		{
			previous = current;
			current = next;
			fadeStartNanos = System.nanoTime();
			this.fadeSeconds = previous == null ? 0 : fadeSeconds;
			blendOverride = -1f;
		}
		return true;
	}

	/** Drive the crossfade from outside (border blending); pass -1 to go back to the time-based fade. */
	void overrideBlend(float t)
	{
		blendOverride = t;
	}

	/** Forget the last failed name so a fixed folder gets another attempt. */
	void retryFailed()
	{
		failedName = null;
	}

	void freeTextures()
	{
		for (Cubemap c : loaded.values())
		{
			glDeleteTextures(c.texture);
		}
		loaded.clear();
		current = null;
		previous = null;
		failedName = null;
	}

	boolean isReady()
	{
		return program != 0 && current != null;
	}

	/** 0 = still showing the previous cubemap, 1 = fade finished. */
	private float blend()
	{
		if (blendOverride >= 0f && previous != null)
		{
			if (blendOverride >= 1f)
			{
				previous = null;
				blendOverride = -1f;
				return 1f;
			}
			return blendOverride;
		}
		if (previous == null || fadeSeconds <= 0)
		{
			return 1f;
		}
		float t = (System.nanoTime() - fadeStartNanos) / (fadeSeconds * 1e9f);
		if (t >= 1f)
		{
			previous = null;
			return 1f;
		}
		return t;
	}

	/**
	 * Horizon colour of the cubemap being shown, packed as 0xRRGGBB, blended during a fade. Used as the fog colour
	 * so terrain fog and the sky fade meet in the same tone.
	 */
	int getHorizonColor()
	{
		float t = blend();
		if (t >= 1f)
		{
			return current.horizonColor;
		}
		return mixColor(previous.horizonColor, current.horizonColor, t);
	}

	/** Per-channel lerp of two 0xRRGGBB colours: t = 0 gives a, t = 1 gives b. */
	static int mixColor(int a, int b, float t)
	{
		int r = Math.round((a >> 16 & 0xFF) + ((b >> 16 & 0xFF) - (a >> 16 & 0xFF)) * t);
		int g = Math.round((a >> 8 & 0xFF) + ((b >> 8 & 0xFF) - (a >> 8 & 0xFF)) * t);
		int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
		return r << 16 | g << 8 | bl;
	}

	/**
	 * Expects the scene FBO to be bound and cleared. Leaves depth test, cull face and blend as they were.
	 *
	 * @param skyProj  projection * pitch * yaw, without camera translation
	 * @param quadVao  a VAO whose attribute 0 is a fullscreen quad in clip space (-1..1), drawn as a 4-vertex fan
	 */
	void draw(float[] skyProj, int sky, int quadVao, BetterSkyboxConfig config, float flash)
	{
		double elapsedMinutes = (System.nanoTime() - startNanos) / 60e9;
		float rotationDeg = (float) ((config.skyboxRotation() + elapsedMinutes * config.skyboxRotationSpeed()) % 360.0);
		float blend = blend();

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_CULL_FACE);

		glUseProgram(program);
		glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, current.texture);
		glActiveTexture(GL_TEXTURE0 + PREV_TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, blend < 1f ? previous.texture : current.texture);
		glUniform1i(uniCubemap, TEXTURE_UNIT);
		glUniform1i(uniPrevCubemap, PREV_TEXTURE_UNIT);
		glUniform1f(uniBlend, blend);
		glUniformMatrix4fv(uniSkyProj, false, skyProj);
		glUniform4f(uniFogColor, (sky >> 16 & 0xFF) / 255f, (sky >> 8 & 0xFF) / 255f, (sky & 0xFF) / 255f, 1f);
		glUniform1f(uniHorizonBlend, config.skyboxHorizonBlend() / 100f);
		glUniform1f(uniHorizonOffset, (float) Math.sin(Math.toRadians(config.skyboxHorizonOffset())));
		glUniform1f(uniFogTint, config.skyboxFogTint() / 100f);
		glUniform1f(uniBrightness, config.skyboxBrightness() / 100f);
		glUniform1f(uniFlash, flash);
		glUniform1f(uniRotation, (float) Math.toRadians(rotationDeg));

		glBindVertexArray(quadVao);
		glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
		glBindVertexArray(0);

		glActiveTexture(GL_TEXTURE0);
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
	}
}
