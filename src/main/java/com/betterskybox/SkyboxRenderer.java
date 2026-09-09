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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import com.betterskybox.CubemapLoads.Slot;
import com.betterskybox.CubemapLoader.Cubemap;
import com.betterskybox.CubemapLoader.Faces;
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

	/**
	 * The two halves of a cubemap load: {@code decode} is pure CPU and touches no GL, {@code upload} is nothing
	 * but GL. {@link CubemapLoader} in the client, a stub in tests.
	 */
	interface Loader
	{
		/** Null when the folder is missing or unreadable. */
		Faces decode(String name);

		Cubemap upload(Faces faces, int textureUnit);
	}

	private static final Loader CUBEMAP_LOADER = new Loader()
	{
		@Override
		public Faces decode(String name)
		{
			return CubemapLoader.decode(name);
		}

		@Override
		public Cubemap upload(Faces faces, int textureUnit)
		{
			return CubemapLoader.upload(faces, textureUnit);
		}
	};

	private final Loader loader;
	private final CubemapLoads loads;
	private int program;
	private final Map<String, Cubemap> loaded = new HashMap<>();
	private final Set<String> failed = new HashSet<>();
	private Cubemap current;
	private Cubemap previous;
	/** Whether each slot holds a night sky, so a day-to-night crossfade brings the stars in with it. */
	private boolean currentNight;
	private boolean previousNight;
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
	private int uniStarMap;
	private int uniStarRot;
	private int uniStarAmount;
	private int uniStarBrightness;
	private int uniStarDim;

	private final long startNanos = System.nanoTime();

	@Inject
	SkyboxRenderer(CubemapLoads loads)
	{
		this(CUBEMAP_LOADER, loads);
	}

	SkyboxRenderer(Loader loader, CubemapLoads loads)
	{
		this.loader = loader;
		this.loads = loads;
	}

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
		uniStarMap = glGetUniformLocation(program, "starMap");
		uniStarRot = glGetUniformLocation(program, "starRot");
		uniStarAmount = glGetUniformLocation(program, "starAmount");
		uniStarBrightness = glGetUniformLocation(program, "starBrightness");
		uniStarDim = glGetUniformLocation(program, "starDim");
	}

	void shutdownProgram()
	{
		glDeleteProgram(program);
		program = 0;
	}

	/**
	 * Makes {@code name} the sky to show. A cubemap that is already loaded is swapped in at once, crossfading
	 * from the one before; any other is decoded off the client thread while the sky on screen keeps drawing, and
	 * swapped in when its pixels land. So the frame never waits for a decode, and the caller must not read the
	 * result as "showing now".
	 * <p>
	 * Returns false only for a name that will never show and wants replacing: an empty one, or one that failed
	 * to load and is skipped until {@link #retryFailed()}.
	 *
	 * @param night whether this sky is a night one, see {@link SkyPass#nightSky}
	 */
	boolean select(String name, float fadeSeconds, boolean night)
	{
		if (name.isEmpty() || failed.contains(name))
		{
			return false;
		}
		Cubemap next = loaded.get(name);
		if (next == null)
		{
			loads.request(Slot.SKY, name, () -> loader.decode(name), faces -> finish(name, faces, fadeSeconds, night));
			return true;
		}
		show(next, fadeSeconds, night);
		return true;
	}

	/** Client thread: uploads what the loader thread decoded, or remembers a name that cannot be loaded. */
	private void finish(String name, Faces faces, float fadeSeconds, boolean night)
	{
		if (faces == null)
		{
			failed.add(name);
			return;
		}
		Cubemap next = loader.upload(faces, TEXTURE_UNIT);
		loaded.put(name, next);
		show(next, fadeSeconds, night);
	}

	/** Puts {@code next} on screen, crossfading from whatever was showing. */
	private void show(Cubemap next, float fadeSeconds, boolean night)
	{
		if (next != current)
		{
			previous = current;
			previousNight = currentNight;
			current = next;
			fadeStartNanos = System.nanoTime();
			this.fadeSeconds = previous == null ? 0 : fadeSeconds;
			blendOverride = -1f;
		}
		// the same folder can be reached as a day sky and as a night one, and then swaps without a fade
		currentNight = night;
	}

	/** Drive the crossfade from outside (border blending); pass -1 to go back to the time-based fade. */
	void overrideBlend(float t)
	{
		blendOverride = t;
	}

	/** Forgets every failed name so a fixed folder gets another attempt. Call on the client thread. */
	void retryFailed()
	{
		failed.clear();
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
		currentNight = false;
		previousNight = false;
		failed.clear();
	}

	boolean isReady()
	{
		return program != 0 && current != null;
	}

	/**
	 * How much of the sky on screen is a night one, 0..1: the two slots' night flags mixed by the crossfade, so
	 * walking from a day area into a night one brings the stars in at the pace of the sky behind them.
	 */
	float starAmount()
	{
		float prev = previousNight ? 1f : 0f;
		float cur = currentNight ? 1f : 0f;
		return prev + (cur - prev) * blend();
	}

	/** Whether the star map is worth loading: a night sky is showing, or fading in or out. */
	boolean wantsStars()
	{
		return isReady() && starAmount() > 0f;
	}

	/**
	 * 0 = still showing the previous cubemap, 1 = fade finished. Pure: a value below 1 means {@link #previous}
	 * is set. {@link #advanceFade()} owns retiring the fade, so this can be called any number of times a frame.
	 */
	private float blend()
	{
		if (blendOverride >= 0f && previous != null)
		{
			return Math.min(blendOverride, 1f);
		}
		if (previous == null || fadeSeconds <= 0)
		{
			return 1f;
		}
		float t = (System.nanoTime() - fadeStartNanos) / (fadeSeconds * 1e9f);
		return Math.min(t, 1f);
	}

	/** Retires a finished fade, freeing the previous cubemap and any blend override. Call once per frame. */
	void advanceFade()
	{
		if (blend() < 1f)
		{
			return;
		}
		previous = null;
		blendOverride = -1f;
	}

	/**
	 * Horizon colour of the cubemap being shown, packed as 0xRRGGBB, blended during a fade. Used as the fog colour
	 * so terrain fog and the sky fade meet in the same tone. {@code fallback} while no cubemap has loaded yet.
	 */
	int getHorizonColor(int fallback)
	{
		if (current == null)
		{
			return fallback;
		}
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
	 * Expects the scene FBO bound and cleared and the sky state set: {@link SkyPass#draw} turns depth test,
	 * depth write and cull face off around both skies and puts them back.
	 *
	 * @param skyProj  projection * pitch * yaw, without camera translation
	 * @param sky      fog colour of this frame as r, g, b in 0..1
	 * @param quadVao  a VAO whose attribute 0 is a fullscreen quad in clip space (-1..1), drawn as a 4-vertex fan
	 */
	void draw(float[] skyProj, float[] sky, int quadVao, BetterSkyboxConfig config, SkyClock clock, float flash,
		StarMap starMap)
	{
		double elapsedMinutes = (System.nanoTime() - startNanos) / 60e9;
		float rotationDeg = (float) ((config.skyboxRotation() + elapsedMinutes * config.skyboxRotationSpeed()) % 360.0);
		float blend = blend();
		// nothing to sample until the pixels land, and then the dimming must not run ahead of the stars either
		float stars = starMap.loaded() ? starAmount() : 0f;

		glUseProgram(program);
		glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, current.texture);
		glActiveTexture(GL_TEXTURE0 + PREV_TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, blend < 1f ? previous.texture : current.texture);
		glUniform1i(uniCubemap, TEXTURE_UNIT);
		glUniform1i(uniPrevCubemap, PREV_TEXTURE_UNIT);
		glUniform1f(uniBlend, blend);
		glUniformMatrix4fv(uniSkyProj, false, skyProj);
		glUniform4f(uniFogColor, sky[0], sky[1], sky[2], 1f);
		glUniform1f(uniHorizonBlend, config.skyboxHorizonBlend() / 100f);
		glUniform1f(uniHorizonOffset, (float) Math.sin(Math.toRadians(config.skyboxHorizonOffset())));
		glUniform1f(uniFogTint, config.skyboxFogTint() / 100f);
		glUniform1f(uniBrightness, config.skyboxBrightness() / 100f);
		glUniform1f(uniFlash, flash);
		glUniform1f(uniRotation, (float) Math.toRadians(rotationDeg));
		glUniform1f(uniStarAmount, stars);
		glUniform1f(uniStarBrightness, config.starBrightness() / 100f);
		glUniform1f(uniStarDim, config.nightStarsDim() / 100f);
		// every frame, stars or not: left at 0 the cube sampler shares a unit with the interface 2D texture,
		// which is undefined behaviour and fails glValidateProgram on some drivers
		glUniform1i(uniStarMap, StarMap.TEXTURE_UNIT);
		if (stars > 0f)
		{
			starMap.bind(uniStarRot, clock.hour(config));
		}

		glBindVertexArray(quadVao);
		glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
		glBindVertexArray(0);
	}
}
