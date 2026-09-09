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
import java.io.IOException;
import java.util.Random;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import com.betterskybox.template.Template;
import static org.lwjgl.opengl.GL33C.*;

/**
 * The sky pass: everything the renderer does before the scene is drawn. Owns the two sky renderers, the area
 * table, the clock, the lightning generator and the frame state, so the renderer copy stays verbatim upstream.
 * <p>
 * Call order per frame is fixed: {@link #beginFrame(Scene)} first, then {@link #fogColor(int)}, then
 * {@link #draw}. Both of the latter read state that {@code beginFrame} advances.
 */
@Slf4j
class SkyPass
{
	/** A larger step between two frames than any walk or run is a teleport. */
	private static final int TELEPORT_TILES = 8;
	/** Drawn when a CUSTOM folder cannot be loaded and the Cubemap setting is CUSTOM too. */
	static final BetterSkyboxConfig.SkyboxTexture DEFAULT_SKY = BetterSkyboxConfig.SkyboxTexture.PARTLY_CLOUDY;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private BetterSkyboxConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private CubemapLoads loads;

	@Inject
	private SkyboxRenderer skyboxRenderer;

	@Inject
	private ProceduralSkyRenderer proceduralSky;

	private final SkyAreas skyAreas = new SkyAreas();
	private final SkyClock skyClock = new SkyClock();
	private final Lightning lightning = new Lightning(new Random());
	private final BorderBlend borderBlend = new BorderBlend();

	/** Whether the sky pass draws this frame; decided by beginFrame, read by fogColor and the plugin. */
	private boolean drawing;
	private float flash;
	private SkyAreas.Area currentArea;
	private SkyAreas.Area previousArea;
	private WorldPoint lastWorld;
	private SkyClock.Phase lastPhase;
	/** The custom folder name last reported in chat as unloadable, so the report goes out once per name. */
	private String reportedCustomName;

	void init(Template template)
	{
		// after the plugin's own sync mode and thread setup, so the ConfigChanged events land on a live context
		GpuSettingsImport.run(configManager);
		loads.start();
		// context-wide, for every cubemap either sky samples
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		skyAreas.load(gson);
		// a sky shader that fails on this driver must not take the whole renderer down, nor the other sky
		try
		{
			skyboxRenderer.initProgram(template);
		}
		catch (ShaderException ex)
		{
			log.error("Cubemap sky shader failed to compile, cubemap sky disabled", ex);
		}
		try
		{
			proceduralSky.initProgram(template, gson);
		}
		catch (ShaderException ex)
		{
			log.error("Procedural sky shader failed to compile, procedural sky disabled", ex);
		}
		catch (IOException ex)
		{
			log.warn("sky_gradient.json invalid, procedural sky disabled", ex);
		}
	}

	void shutdown()
	{
		// first: a decode landing after this would upload into a context that is being torn down
		loads.stop();
		skyboxRenderer.freeTextures();
		skyboxRenderer.shutdownProgram();
		proceduralSky.shutdownProgram();
	}

	/**
	 * Decides whether the sky pass draws this frame and advances all of its state: the current area, the border
	 * blend, the time-of-day phase, the cubemap selection, the crossfade, the lightning flash and the procedural
	 * sky uniforms. Must run before {@link #fogColor(int)} and {@link #draw}, which only read.
	 */
	boolean beginFrame(Scene scene)
	{
		proceduralSky.starMap(config.skyboxEnabled() && procedural() && config.starMap());
		if (config.skyboxEnabled())
		{
			updateArea();
			if (procedural())
			{
				borderBlend.release();
			}
			else
			{
				SkyClock.Phase phase = config.skyboxByTime() ? SkyClock.phase(skyClock.hour(config)) : SkyClock.Phase.DAY;
				if (phase != lastPhase)
				{
					// the seconds fade owns time-of-day switches, even mid-way through a border blend
					borderBlend.release();
					lastPhase = phase;
				}
				selectCubemap(phase);
				if (borderBlend.active() && lastWorld != null)
				{
					skyboxRenderer.overrideBlend(borderBlend.advance(lastWorld.getX(), lastWorld.getY(), config.skyboxFadeTiles()));
				}
			}
		}
		// the crossfade moves exactly once per frame, after this frame's blend override has been set
		skyboxRenderer.advanceFade();
		boolean ready = procedural() ? proceduralSky.isReady() : skyboxRenderer.isReady();
		boolean draw = config.skyboxEnabled()
			&& ready
			&& (scene.getSkybox() == null || !config.preferGameSkybox())
			&& (!config.skyboxOverworldOnly() || isOverworld());
		drawing = draw;
		boolean stormy = draw && config.lightningEnabled() && currentArea != null && currentArea.lightning;
		flash = lightning.intensity(skyClock.elapsedSeconds(), stormy);
		if (draw && procedural())
		{
			BetterSkyboxConfig.SkyPreset mood = config.areaMoods() && currentArea != null ? currentArea.presetValue : null;
			proceduralSky.update(config, skyClock, mood);
		}
		return draw;
	}

	/**
	 * Fog colour for the scene this frame, packed as 0xRRGGBB. Returns {@code gameSkyColor} unchanged while the
	 * sky pass is not drawing.
	 */
	int fogColor(int gameSkyColor)
	{
		return SkyboxRenderer.mixColor(baseFogColor(gameSkyColor), 0xFFFFFF, flash * 0.6f);
	}

	/**
	 * Clears the scene FBO to the fog colour and draws the sky into it. Leaves {@code restoreProgram} bound.
	 *
	 * @param quadVao a VAO whose attribute 0 is a fullscreen quad in clip space (-1..1), drawn as a 4-vertex fan
	 */
	void draw(int fog, float cameraPitch, float cameraYaw, int viewportWidth, int viewportHeight, int quadVao,
		int restoreProgram)
	{
		glClearColor((fog >> 16 & 0xFF) / 255f, (fog >> 8 & 0xFF) / 255f, (fog & 0xFF) / 255f, 1f);
		glClearDepth(0d);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		float[] skyProj = Mat4.scale(client.getScale(), client.getScale(), 1);
		Mat4.mul(skyProj, Mat4.projection(viewportWidth, viewportHeight, 50));
		Mat4.mul(skyProj, Mat4.rotateX(cameraPitch));
		Mat4.mul(skyProj, Mat4.rotateY(cameraYaw));
		if (procedural())
		{
			proceduralSky.draw(skyProj, fog, quadVao, config, skyClock, flash);
		}
		else
		{
			skyboxRenderer.draw(skyProj, fog, quadVao, config, flash);
		}

		glUseProgram(restoreProgram);
	}

	void onConfigChanged(ConfigChanged configChanged)
	{
		if (configChanged.getKey().equals("skyboxCubemap") || configChanged.getKey().equals("skyboxCustomName"))
		{
			// the cubemap itself is selected per frame in beginFrame; just allow a fixed folder another try
			clientThread.invokeLater(skyboxRenderer::retryFailed);
		}
	}

	private boolean procedural()
	{
		return config.skyMode() == BetterSkyboxConfig.SkyMode.PROCEDURAL;
	}

	private void updateArea()
	{
		WorldPoint world = config.skyboxByArea() ? playerWorldPoint() : null;
		SkyAreas.Area area = null;
		if (world != null)
		{
			// the table is scanned when the tile changes, not every frame
			area = world.equals(lastWorld) ? currentArea : skyAreas.find(world);
		}
		if (area != currentArea)
		{
			boolean jumped = lastWorld == null || world == null
				|| lastWorld.getPlane() != world.getPlane()
				|| lastWorld.distanceTo2D(world) > TELEPORT_TILES;
			if (jumped)
			{
				borderBlend.release();
			}
			else if (area == previousArea && borderBlend.active())
			{
				borderBlend.bounce(world.getX(), world.getY());
			}
			else
			{
				borderBlend.cross(world.getX(), world.getY());
			}
			previousArea = currentArea;
			currentArea = area;
			log.debug("Sky area: {}", area == null ? "unmapped" : area.name + " sky=" + area.sky + " fog=" + area.fog);
		}
		lastWorld = world;
	}

	/**
	 * Cubemap for this frame: the area's sky for the current phase, else the phase's configured sky. A CUSTOM
	 * folder that cannot be loaded gives way to {@link #customFallback} and is reported in chat once per name.
	 */
	private void selectCubemap(SkyClock.Phase phase)
	{
		float fade = config.skyboxFadeSeconds();
		if (currentArea != null)
		{
			String areaSky = currentArea.skyFor(phase);
			if (areaSky != null && skyboxRenderer.select(areaSky, fade))
			{
				return;
			}
		}
		BetterSkyboxConfig.SkyboxTexture texture;
		switch (phase)
		{
			case DAWN:
				texture = config.skyboxDawn();
				break;
			case DUSK:
				texture = config.skyboxDusk();
				break;
			case NIGHT:
				texture = config.skyboxNight();
				break;
			default:
				texture = config.skyboxTexture();
		}
		if (texture != BetterSkyboxConfig.SkyboxTexture.CUSTOM)
		{
			skyboxRenderer.select(texture.dir, fade);
			return;
		}
		String name = config.skyboxCustomName().trim();
		if (skyboxRenderer.select(name, fade))
		{
			return;
		}
		BetterSkyboxConfig.SkyboxTexture fallback = customFallback(config.skyboxTexture());
		skyboxRenderer.select(fallback.dir, fade);
		if (!name.equals(reportedCustomName))
		{
			reportedCustomName = name;
			String reason = name.isEmpty() ? "no custom sky folder set" : "custom sky folder " + name + " not found";
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.value("Better Skybox: " + reason + ", using " + fallback)
				.build());
		}
	}

	/**
	 * Sky drawn in place of a CUSTOM folder that cannot be loaded: the Cubemap setting, unless that is CUSTOM
	 * itself (the same folder), then {@link #DEFAULT_SKY}.
	 */
	static BetterSkyboxConfig.SkyboxTexture customFallback(BetterSkyboxConfig.SkyboxTexture configured)
	{
		return configured == BetterSkyboxConfig.SkyboxTexture.CUSTOM ? DEFAULT_SKY : configured;
	}

	private int skyHorizonColor(int gameSkyColor)
	{
		return procedural() ? proceduralSky.getHorizonColor() : skyboxRenderer.getHorizonColor(gameSkyColor);
	}

	private int baseFogColor(int gameSkyColor)
	{
		if (!drawing)
		{
			return gameSkyColor;
		}
		switch (config.skyboxFogColorMode())
		{
			case SKYBOX:
				return skyHorizonColor(gameSkyColor);
			case GAME:
				return gameSkyColor;
			case CUSTOM:
				return config.skyboxFogCustomColor().getRGB() & 0xFFFFFF;
			default:
				if (currentArea != null && currentArea.fogColor >= 0)
				{
					return currentArea.fogColor;
				}
				return gameSkyColor == 0 ? skyHorizonColor(gameSkyColor) : gameSkyColor;
		}
	}

	private WorldPoint playerWorldPoint()
	{
		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}
		LocalPoint local = player.getLocalLocation();
		return client.getTopLevelWorldView().isInstance()
			? WorldPoint.fromLocalInstance(client, local)
			: player.getWorldLocation();
	}

	private boolean isOverworld()
	{
		WorldPoint world = playerWorldPoint();
		return world != null && world.getY() < Constants.OVERWORLD_MAX_Y;
	}
}
