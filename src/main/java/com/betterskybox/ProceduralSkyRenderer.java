package com.betterskybox;

import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import com.betterskybox.CubemapLoader.Cubemap;
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

	static final int STAR_TEXTURE_UNIT = 4;
	/** Tilt of the celestial pole towards the horizon; puts the Milky Way in an arc instead of a ring. */
	private static final double POLE_TILT = Math.toRadians(40);

	private static final float[] BLOOD_MOON_COLOR = rgb(0xED3D3D);
	private static final float[] MOON_COLOR = rgb(0xE1E5FF);

	static class Keyframe
	{
		float altitude;
		String color;
	}

	static class Gradient
	{
		Keyframe[] zenith;
		Keyframe[] horizon;
		Keyframe[] sunGlow;
	}

	private Gradient gradient;
	private int program;
	private Cubemap stars;
	private final float[] starRot = new float[9];
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

	void initProgram(Template template, Gson gson) throws ShaderException
	{
		try (InputStream in = ProceduralSkyRenderer.class.getResourceAsStream("sky_gradient.json"))
		{
			gradient = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Gradient.class);
		}
		catch (Exception ex)
		{
			throw new RuntimeException("sky_gradient.json missing or invalid", ex);
		}

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

		stars = CubemapLoader.upload("stars", STAR_TEXTURE_UNIT);
	}

	void shutdownProgram()
	{
		glDeleteProgram(program);
		program = 0;
		if (stars != null)
		{
			glDeleteTextures(stars.texture);
			stars = null;
		}
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
		starRotation(hour, starRot);
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

	/**
	 * Column-major 3x3 that takes a world-space view direction (y down) to a star map lookup: flip to y up, turn the
	 * sky once per day around the celestial pole, then tilt that pole towards the horizon.
	 */
	private static void starRotation(float hour, float[] out)
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

	/** Horizon colour of the current sky as 0xRRGGBB, for fog. */
	int getHorizonColor()
	{
		return Math.round(horizon[0] * 255) << 16 | Math.round(horizon[1] * 255) << 8 | Math.round(horizon[2] * 255);
	}

	void draw(float[] skyProj, int fog, int quadVao, BetterSkyboxConfig config, SkyClock clock, float flash)
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
		boolean starMap = stars != null && config.starMap();
		glUniform1f(uniStarMapEnabled, starMap ? 1f : 0f);
		if (starMap)
		{
			glActiveTexture(GL_TEXTURE0 + STAR_TEXTURE_UNIT);
			glBindTexture(GL_TEXTURE_CUBE_MAP, stars.texture);
			glUniform1i(uniStarMap, STAR_TEXTURE_UNIT);
			glUniformMatrix3fv(uniStarRot, false, starRot);
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

	private static void evaluate(Keyframe[] keys, float altitude, float[] out)
	{
		if (altitude <= keys[0].altitude)
		{
			System.arraycopy(rgb(keys[0].color), 0, out, 0, 3);
			return;
		}
		for (int i = 1; i < keys.length; i++)
		{
			if (altitude <= keys[i].altitude)
			{
				float t = (altitude - keys[i - 1].altitude) / (keys[i].altitude - keys[i - 1].altitude);
				float[] a = rgb(keys[i - 1].color);
				float[] b = rgb(keys[i].color);
				for (int c = 0; c < 3; c++)
				{
					out[c] = a[c] + (b[c] - a[c]) * t;
				}
				return;
			}
		}
		System.arraycopy(rgb(keys[keys.length - 1].color), 0, out, 0, 3);
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
