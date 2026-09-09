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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import com.betterskybox.BetterSkyboxConfig.SkyPreset;
import com.betterskybox.template.Template;
import static org.lwjgl.opengl.GL33C.*;

/**
 * Gradient sky with sun, clouds, stars, nebula, moon and aurora, driven by a sun position.
 * Colour keyframes and most of the shading model come from 117 HD's day/night cycle (PR #655).
 */
@Slf4j
class ProceduralSkyRenderer
{
	static final Shader PROGRAM = new Shader()
		.add(GL_VERTEX_SHADER, "sky_vert.glsl")
		.add(GL_FRAGMENT_SHADER, "proc_sky_frag.glsl");

	private static final float[] BLOOD_MOON_COLOR = rgb(0xED3D3D);
	private static final float[] MOON_COLOR = rgb(0xE1E5FF);

	static class Keyframe
	{
		float altitude;
		/** #RRGGBB as written in the file. */
		String color;
		/** {@link #color} parsed once at load, so evaluate() neither parses nor allocates. */
		transient float[] rgb;
	}

	static class Gradient
	{
		Keyframe[] zenith;
		Keyframe[] horizon;
		Keyframe[] sunGlow;
	}

	private Gradient gradient;
	private int program;
	private float hour = 12;

	private int uniSkyProj, uniZenith, uniHorizon, uniSun, uniSunDir, uniMoonDir, uniMoonColor, uniMoonVisibility,
		uniMoonSize, uniMoonPhase, uniStarVisibility, uniStarBrightness, uniShootingStars, uniNebula, uniAurora,
		uniSunDisk, uniCloudCover, uniCloudTime, uniTime, uniFogColor, uniHorizonBlend, uniFogTint, uniBrightness,
		uniFlash, uniStarMap, uniStarMapEnabled, uniStarRot, uniHorizonOffset;

	// per-frame state, evaluated in update()
	private final float[] zenith = new float[3];
	private final float[] horizon = new float[3];
	private final float[] sunGlow = new float[3];
	private final float[] sunDir = new float[3];
	private final float[] moonDir = new float[3];
	private float[] moonColor = MOON_COLOR;
	private float moonSize = 1f;
	private float moonPhase = 1f;

	/**
	 * @throws IOException when the bundled gradient cannot be read or parsed; the program is then never compiled
	 */
	void initProgram(Template template, Gson gson) throws ShaderException, IOException
	{
		gradient = loadGradient(gson);
		program = PROGRAM.compile(template);
		uniSkyProj = glGetUniformLocation(program, "skyProj");
		uniZenith = glGetUniformLocation(program, "zenithColor");
		uniHorizon = glGetUniformLocation(program, "horizonColor");
		uniSun = glGetUniformLocation(program, "sunColor");
		uniSunDir = glGetUniformLocation(program, "sunDir");
		uniMoonDir = glGetUniformLocation(program, "moonDir");
		uniMoonColor = glGetUniformLocation(program, "moonColor");
		uniMoonVisibility = glGetUniformLocation(program, "moonVisibility");
		uniMoonSize = glGetUniformLocation(program, "moonSize");
		uniMoonPhase = glGetUniformLocation(program, "moonPhase");
		uniStarVisibility = glGetUniformLocation(program, "starVisibility");
		uniStarBrightness = glGetUniformLocation(program, "starBrightness");
		uniShootingStars = glGetUniformLocation(program, "shootingStars");
		uniNebula = glGetUniformLocation(program, "nebulaVisibility");
		uniAurora = glGetUniformLocation(program, "auroraVisibility");
		uniSunDisk = glGetUniformLocation(program, "sunDisk");
		uniCloudCover = glGetUniformLocation(program, "cloudCover");
		uniCloudTime = glGetUniformLocation(program, "cloudTime");
		uniTime = glGetUniformLocation(program, "time");
		uniFogColor = glGetUniformLocation(program, "fogColor");
		uniHorizonBlend = glGetUniformLocation(program, "horizonBlend");
		uniHorizonOffset = glGetUniformLocation(program, "horizonOffset");
		uniFogTint = glGetUniformLocation(program, "fogTint");
		uniBrightness = glGetUniformLocation(program, "brightness");
		uniFlash = glGetUniformLocation(program, "flash");
		uniStarMap = glGetUniformLocation(program, "starMap");
		uniStarMapEnabled = glGetUniformLocation(program, "starMapEnabled");
		uniStarRot = glGetUniformLocation(program, "starRot");
	}

	/**
	 * The bundled gradient with every keyframe colour parsed.
	 *
	 * @throws IOException when the bundled gradient cannot be read or parsed
	 */
	static Gradient loadGradient(Gson gson) throws IOException
	{
		try (InputStream in = ProceduralSkyRenderer.class.getResourceAsStream("sky_gradient.json"))
		{
			Gradient gradient = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Gradient.class);
			for (Keyframe[] keys : new Keyframe[][]{gradient.zenith, gradient.horizon, gradient.sunGlow})
			{
				for (Keyframe key : keys)
				{
					key.rgb = rgb(key.color);
				}
			}
			return gradient;
		}
		catch (JsonParseException | NumberFormatException ex)
		{
			throw new IOException("sky_gradient.json invalid", ex);
		}
	}

	void shutdownProgram()
	{
		glDeleteProgram(program);
		program = 0;
	}

	boolean isReady()
	{
		return program != 0;
	}

	/**
	 * Evaluates sun/moon positions and gradient colours for this frame.
	 */
	void update(BetterSkyboxConfig config, SkyClock clock, SkyPreset override)
	{
		SkyPreset preset;
		if (override != null)
		{
			preset = override;
			hour = SkyClock.hourForAzimuth(override.azimuth);
		}
		else
		{
			preset = config.skyPreset();
			hour = clock.hour(config);
		}
		float altitude, azimuth;
		switch (preset)
		{
			case CLOCK:
			case CYCLE:
				altitude = SkyClock.altitudeForHour(hour);
				azimuth = SkyClock.azimuthForHour(hour);
				break;
			case CUSTOM:
				altitude = config.sunAltitude();
				azimuth = config.sunAzimuth();
				break;
			default:
				altitude = preset.altitude;
				azimuth = preset.azimuth;
		}
		direction(sunDir, altitude, azimuth);

		if (preset == SkyPreset.BLOOD_MOON)
		{
			direction(moonDir, 22.6f, 89.75f);
			moonColor = BLOOD_MOON_COLOR;
			moonSize = 3.05f * config.moonSize() / 100f;
			moonPhase = 1f;
		}
		else
		{
			direction(moonDir, 25, azimuth + 180);
			moonColor = MOON_COLOR;
			moonSize = config.moonSize() / 100f;
			float real = clock.moonIllumination(config);
			moonPhase = real >= 0 ? real : config.moonPhase() / 100f;
		}

		evaluate(gradient.zenith, altitude, zenith);
		evaluate(gradient.horizon, altitude, horizon);
		evaluate(gradient.sunGlow, altitude, sunGlow);
	}

	/** Horizon colour of the current sky as 0xRRGGBB, for fog. */
	int getHorizonColor()
	{
		return Math.round(horizon[0] * 255) << 16 | Math.round(horizon[1] * 255) << 8 | Math.round(horizon[2] * 255);
	}

	void draw(float[] skyProj, int fog, int quadVao, BetterSkyboxConfig config, SkyClock clock, float flash,
		StarMap starMap)
	{
		float seconds = clock.elapsedSeconds();

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_CULL_FACE);

		glUseProgram(program);
		glUniformMatrix4fv(uniSkyProj, false, skyProj);
		glUniform3fv(uniZenith, zenith);
		glUniform3fv(uniHorizon, horizon);
		glUniform3fv(uniSun, sunGlow);
		glUniform3fv(uniSunDir, sunDir);
		glUniform3fv(uniMoonDir, moonDir);
		glUniform3fv(uniMoonColor, moonColor);
		glUniform1f(uniMoonVisibility, config.moonEnabled() ? 1f : 0f);
		glUniform1f(uniMoonSize, moonSize);
		glUniform1f(uniMoonPhase, moonPhase);
		glUniform1f(uniStarVisibility, config.starsEnabled() ? 1f : 0f);
		glUniform1f(uniStarBrightness, config.starBrightness() / 100f);
		glUniform1f(uniShootingStars, config.shootingStars() ? 1f : 0f);
		glUniform1f(uniNebula, config.nebula() ? 1f : 0f);
		glUniform1f(uniAurora, config.aurora() ? 1f : 0f);
		glUniform1f(uniSunDisk, config.sunDisk() ? 1f : 0f);
		glUniform1f(uniCloudCover, config.cloudCover() / 100f);
		glUniform1f(uniCloudTime, seconds * config.cloudSpeed() / 100f);
		glUniform1f(uniTime, seconds);
		glUniform4f(uniFogColor, (fog >> 16 & 0xFF) / 255f, (fog >> 8 & 0xFF) / 255f, (fog & 0xFF) / 255f, 1f);
		glUniform1f(uniHorizonBlend, config.skyboxHorizonBlend() / 100f);
		glUniform1f(uniHorizonOffset, (float) Math.sin(Math.toRadians(config.skyboxHorizonOffset())));
		glUniform1f(uniFogTint, config.skyboxFogTint() / 100f);
		glUniform1f(uniBrightness, config.skyboxBrightness() / 100f);
		glUniform1f(uniFlash, flash);
		// loaded only while the star map setting is on, see StarMap.want()
		boolean stars = starMap.loaded();
		glUniform1f(uniStarMapEnabled, stars ? 1f : 0f);
		if (stars)
		{
			starMap.bind(uniStarMap, uniStarRot, hour);
		}

		glBindVertexArray(quadVao);
		glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
		glBindVertexArray(0);

		glActiveTexture(GL_TEXTURE0);
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
	}

	/** y-up direction from altitude/azimuth in degrees, matching 117 HD's convention. */
	private static void direction(float[] out, float altitudeDeg, float azimuthDeg)
	{
		double alt = Math.toRadians(altitudeDeg);
		double az = Math.toRadians(azimuthDeg);
		out[0] = (float) (Math.sin(az) * Math.cos(alt));
		out[1] = (float) Math.sin(alt);
		out[2] = (float) (Math.cos(az) * Math.cos(alt));
	}

	/** Colour of the gradient at {@code altitude}: interpolated between its neighbouring keys, clamped past the ends. */
	static void evaluate(Keyframe[] keys, float altitude, float[] out)
	{
		if (altitude <= keys[0].altitude)
		{
			System.arraycopy(keys[0].rgb, 0, out, 0, 3);
			return;
		}
		for (int i = 1; i < keys.length; i++)
		{
			if (altitude <= keys[i].altitude)
			{
				float t = (altitude - keys[i - 1].altitude) / (keys[i].altitude - keys[i - 1].altitude);
				float[] a = keys[i - 1].rgb;
				float[] b = keys[i].rgb;
				for (int c = 0; c < 3; c++)
				{
					out[c] = a[c] + (b[c] - a[c]) * t;
				}
				return;
			}
		}
		System.arraycopy(keys[keys.length - 1].rgb, 0, out, 0, 3);
	}

	private static float[] rgb(String hex)
	{
		return rgb(Integer.parseInt(hex.substring(1), 16));
	}

	private static float[] rgb(int packed)
	{
		return new float[]{(packed >> 16 & 0xFF) / 255f, (packed >> 8 & 0xFF) / 255f, (packed & 0xFF) / 255f};
	}
}
