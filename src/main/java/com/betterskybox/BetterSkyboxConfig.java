/*
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
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

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import static com.betterskybox.BetterSkyboxPlugin.MAX_DISTANCE;
import static com.betterskybox.BetterSkyboxPlugin.MAX_FOG_DEPTH;
import com.betterskybox.config.AntiAliasingMode;
import com.betterskybox.config.ColorBlindMode;
import com.betterskybox.config.UIScalingMode;

@ConfigGroup(BetterSkyboxConfig.GROUP)
public interface BetterSkyboxConfig extends Config
{
	String GROUP = "betterskybox";

	@Range(
		max = MAX_DISTANCE
	)
	@ConfigItem(
		keyName = "drawDistance",
		name = "Draw distance",
		description = "Draw distance.",
		position = 1,
		section = rendererSection
	)
	default int drawDistance()
	{
		return 50;
	}

	@ConfigItem(
		keyName = "hideUnrelatedMaps",
		name = "Hide unrelated maps",
		description = "Hide unrelated map areas you shouldn't see.",
		position = 2,
		section = rendererSection
	)
	default boolean hideUnrelatedMaps()
	{
		return true;
	}

	@Range(
		max = 5
	)
	@ConfigItem(
		keyName = "expandedMapLoadingChunks",
		name = "Extended map loading",
		description = "Extra map area to load, in 8 tile chunks.",
		position = 3,
		section = rendererSection
	)
	default int expandedMapLoadingZones()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "smoothBanding",
		name = "Remove color banding",
		description = "Smooths out the color banding that is present in the CPU renderer.",
		position = 4,
		section = rendererSection
	)
	default boolean smoothBanding()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antiAliasingMode",
		name = "Anti aliasing",
		description = "Configures the anti-aliasing mode.",
		position = 5,
		section = rendererSection
	)
	default AntiAliasingMode antiAliasingMode()
	{
		return AntiAliasingMode.MSAA_2;
	}

	@ConfigItem(
		keyName = "uiScalingMode",
		name = "UI scaling mode",
		description = "Sampling function to use for the UI in stretched mode.",
		position = 6,
		section = rendererSection
	)
	default UIScalingMode uiScalingMode()
	{
		return UIScalingMode.HYBRID;
	}

	@Range(
		max = MAX_FOG_DEPTH
	)
	@ConfigItem(
		keyName = "fogDepth",
		name = "Fog depth",
		description = "Distance from the scene edge the fog starts.",
		position = 1,
		section = fogSection
	)
	default int fogDepth()
	{
		return 0;
	}

	@Range(
		min = 0,
		max = 16
	)
	@ConfigItem(
		keyName = "anisotropicFilteringLevel",
		name = "Anisotropic filtering",
		description = "Configures the anisotropic filtering level.",
		position = 7,
		section = rendererSection
	)
	default int anisotropicFilteringLevel()
	{
		return 1;
	}

	@ConfigItem(
		keyName = "colorBlindMode",
		name = "Colorblindness correction",
		description = "Adjusts colors to account for colorblindness.",
		position = 8,
		section = rendererSection
	)
	default ColorBlindMode colorBlindMode()
	{
		return ColorBlindMode.NONE;
	}

	@Range(
		min = 0,
		max = 100
	)
	@ConfigItem(
		keyName = "colorBlindIntensity",
		name = "Colorblindness intensity",
		description = "Strength of the colorblindness correction effect.",
		position = 9,
		section = rendererSection
	)
	default int colorBlindIntensity()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "brightTextures",
		name = "Bright textures",
		description = "Use old texture lighting method which results in brighter game textures.",
		position = 10,
		section = rendererSection
	)
	default boolean brightTextures()
	{
		return false;
	}

	@ConfigItem(
		keyName = "unlockFps",
		name = "Unlock FPS",
		description = "Removes the 50 FPS cap for camera movement.",
		position = 11,
		section = rendererSection
	)
	default boolean unlockFps()
	{
		return true;
	}

	enum SyncMode
	{
		OFF,
		ON,
		ADAPTIVE
	}

	@ConfigItem(
		keyName = "vsyncMode",
		name = "Vsync mode",
		description = "Method to synchronize frame rate with refresh rate.",
		position = 12,
		section = rendererSection
	)
	default SyncMode syncMode()
	{
		return SyncMode.OFF;
	}

	@ConfigItem(
		keyName = "fpsTarget",
		name = "FPS target",
		description = "Target FPS when 'Unlock FPS' is enabled and 'Vsync mode' is off.",
		position = 13,
		section = rendererSection
	)
	@Range(
		min = 1,
		max = 999
	)
	default int fpsTarget()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "removeVertexSnapping",
		name = "Remove vertex snapping",
		description = "Removes vertex snapping from most animations.",
		position = 14,
		section = rendererSection
	)
	default boolean removeVertexSnapping()
	{
		return true;
	}

	@ConfigItem(
		keyName = "numThreads",
		name = "Threads",
		description = "Number of render threads to use.",
		position = 15,
		section = rendererSection
	)
	@Range(min = 0, max = 15)
	default int numThreads()
	{
		return 3;
	}

	@ConfigSection(
		name = "Renderer",
		description = "The stock GPU plugin settings",
		position = 10,
		closedByDefault = true
	)
	String rendererSection = "renderer";

	@ConfigSection(
		name = "Sky",
		description = "Which sky is drawn: a cubemap texture or a procedural day/night sky",
		position = 20
	)
	String skySection = "sky";

	@ConfigSection(
		name = "Cubemap sky",
		description = "Settings for Sky type = CUBEMAP",
		position = 30
	)
	String cubemapSection = "cubemap";

	@ConfigSection(
		name = "Procedural sky",
		description = "Settings for Sky type = PROCEDURAL",
		position = 40,
		closedByDefault = true
	)
	String proceduralSection = "procedural";

	@ConfigSection(
		name = "Fog and blending",
		description = "How the sky meets the terrain, for both sky types",
		position = 50
	)
	String fogSection = "fog";

	enum SkyMode
	{
		CUBEMAP,
		PROCEDURAL
	}

	enum SkyboxTexture
	{
		PARTLY_CLOUDY("kloofendal_48d_partly_cloudy_puresky"),
		CLEAR("syferfontein_18d_clear_puresky"),
		OVERCAST("kloofendal_overcast_puresky"),
		MISTY("kloofendal_28d_misty_puresky"),
		STORM_CLOUDS("wasteland_clouds_puresky"),
		MOUNTAIN("drakensberg_solitary_mountain_puresky"),
		SNOW("snow_field_puresky"),
		SUNSET("qwantani_sunset_puresky"),
		HAZY_SUNSET("industrial_sunset_puresky"),
		DUSK("qwantani_dusk_1_puresky"),
		MOONRISE("qwantani_moonrise_puresky"),
		NIGHT("qwantani_night_puresky"),
		STORM_BREAK("ambientcg_dayskyhdri060b"),
		DEEP_BLUE("ambientcg_dayskyhdri055b"),
		PALE_MORNING("ambientcg_morningskyhdri013b"),
		GOLDEN_HOUR("ambientcg_eveningskyhdri046b"),
		AURORA_NIGHT("ambientcg_nightskyhdri007"),
		TOON_BLUE("oga_cloudy_blue"),
		TOON_SUNSET("oga_cloudy_sunset"),
		TOON_VIOLET("oga_cloudy_violet"),
		DAWN("qwantani_dawn_puresky"),
		DEBUG("debug"),
		CUSTOM(null);

		final String dir;

		SkyboxTexture(String dir)
		{
			this.dir = dir;
		}
	}

	/** Sun angles from 117 HD's sky presets (altitude, azimuth in degrees). */
	enum SkyPreset
	{
		DAY(52, 235),
		SUNRISE(7.8f, 90.2f),
		DAWN(-2.5f, 90),
		SUNSET(0, 272),
		DUSK(-2.5f, 270),
		NIGHT(-90, 0),
		BLOOD_MOON(-90, 0),
		CLOCK(0, 0),
		CYCLE(0, 0),
		CUSTOM(0, 0);

		final float altitude;
		final float azimuth;

		SkyPreset(float altitude, float azimuth)
		{
			this.altitude = altitude;
			this.azimuth = azimuth;
		}
	}

	enum FogColorMode
	{
		AUTO,
		SKYBOX,
		GAME,
		CUSTOM
	}

	@ConfigItem(
		keyName = "skyboxEnabled",
		name = "Enable sky",
		description = "Draw a sky instead of the flat fog colour.",
		position = 1,
		section = skySection
	)
	default boolean skyboxEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skyMode",
		name = "Sky type",
		description = "CUBEMAP: a texture. PROCEDURAL: gradient sky with sun, moon and stars (from 117 HD's day/night work).",
		position = 2,
		section = skySection
	)
	default SkyMode skyMode()
	{
		return SkyMode.CUBEMAP;
	}

	@ConfigItem(
		keyName = "skyboxCubemap",
		name = "Cubemap",
		description = "Bundled CC0 cubemap (Poly Haven, ambientCG, OpenGameArt), or CUSTOM to use the folder named below. With Sky by area on, this is the sky for unmapped areas.",
		position = 1,
		section = cubemapSection
	)
	default SkyboxTexture skyboxTexture()
	{
		return SkyboxTexture.PARTLY_CLOUDY;
	}

	@ConfigItem(
		keyName = "skyboxCustomName",
		name = "Custom cubemap folder",
		description = "Folder name under ~/.runelite/better-skybox/ containing px, nx, py, ny, pz, nz .png, or a single skybox.png atlas (4x2 faces: px nz nx pz / py ny).",
		position = 2,
		section = cubemapSection
	)
	default String skyboxCustomName()
	{
		return "";
	}

	@ConfigItem(
		keyName = "skyboxByTime",
		name = "Sky by time",
		description = "Follow the Time of day: dawn, day, dusk and night cubemaps. Day is the Cubemap above (or the area's sky); the other three are set below unless an area brings its own.",
		position = 3,
		section = cubemapSection
	)
	default boolean skyboxByTime()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skyboxDawn",
		name = "Dawn cubemap",
		description = "Sky by time: shown from 5:00 to 7:00 sky time.",
		position = 4,
		section = cubemapSection
	)
	default SkyboxTexture skyboxDawn()
	{
		return SkyboxTexture.DAWN;
	}

	@ConfigItem(
		keyName = "skyboxDusk",
		name = "Dusk cubemap",
		description = "Sky by time: shown from 17:00 to 20:00 sky time.",
		position = 5,
		section = cubemapSection
	)
	default SkyboxTexture skyboxDusk()
	{
		return SkyboxTexture.SUNSET;
	}

	@ConfigItem(
		keyName = "skyboxNight",
		name = "Night cubemap",
		description = "Sky by time: shown from 20:00 to 5:00 sky time.",
		position = 6,
		section = cubemapSection
	)
	default SkyboxTexture skyboxNight()
	{
		return SkyboxTexture.NIGHT;
	}

	@ConfigItem(
		keyName = "skyPreset",
		name = "Time of day",
		description = "Sets the sky's clock for both sky types. Presets fix the sun position, CLOCK follows your local time, CYCLE runs a full day in the minutes set below, CUSTOM uses the sun sliders in the Procedural section.",
		position = 5,
		section = skySection
	)
	default SkyPreset skyPreset()
	{
		return SkyPreset.DAY;
	}

	@Range(min = -90, max = 90)
	@ConfigItem(
		keyName = "sunAltitude",
		name = "Sun altitude",
		description = "Procedural sky, Time of day = CUSTOM. Degrees above the horizon, negative is night.",
		position = 1,
		section = proceduralSection
	)
	default int sunAltitude()
	{
		return 30;
	}

	@Range(min = 0, max = 359)
	@ConfigItem(
		keyName = "sunAzimuth",
		name = "Sun azimuth",
		description = "Procedural sky, Time of day = CUSTOM. Compass direction of the sun in degrees.",
		position = 2,
		section = proceduralSection
	)
	default int sunAzimuth()
	{
		return 235;
	}

	@ConfigItem(
		keyName = "moonEnabled",
		name = "Moon",
		description = "Procedural sky: draw the moon.",
		position = 4,
		section = proceduralSection
	)
	default boolean moonEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "starsEnabled",
		name = "Stars",
		description = "Procedural sky: draw stars at night.",
		position = 7,
		section = proceduralSection
	)
	default boolean starsEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "starMap",
		name = "Real star map",
		description = "Procedural sky: use NASA's Deep Star Map (Gaia DR2, 1.7 billion stars) turning with the time of day instead of generated stars.",
		position = 8,
		section = proceduralSection
	)
	default boolean starMap()
	{
		return true;
	}

	@Range(min = 1, max = 240)
	@ConfigItem(
		keyName = "cycleMinutes",
		name = "Cycle length",
		description = "Procedural sky, Time of day = CYCLE. Real minutes for one full day.",
		position = 6,
		section = skySection
	)
	default int cycleMinutes()
	{
		return 24;
	}

	@ConfigItem(
		keyName = "lightningEnabled",
		name = "Lightning",
		description = "Random lightning flashes where 117 HD marks storms: Wilderness (high), Barrows, Draynor Manor and its forest, the Misthalin Mystery Manor, Tempoross Cove.",
		position = 7,
		section = skySection
	)
	default boolean lightningEnabled()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sunDisk",
		name = "Sun disk",
		description = "Procedural sky: draw the sun itself, not just its glow.",
		position = 3,
		section = proceduralSection
	)
	default boolean sunDisk()
	{
		return true;
	}

	@Range(min = 25, max = 400)
	@ConfigItem(
		keyName = "moonSize",
		name = "Moon size",
		description = "Procedural sky: moon disk size in percent.",
		position = 5,
		section = proceduralSection
	)
	default int moonSize()
	{
		return 100;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "moonPhase",
		name = "Moon phase",
		description = "Procedural sky: 0 = new moon, 100 = full moon. Ignored under CLOCK and CYCLE, where the moon follows the calendar.",
		position = 6,
		section = proceduralSection
	)
	default int moonPhase()
	{
		return 100;
	}

	@Range(min = 0, max = 300)
	@ConfigItem(
		keyName = "starBrightness",
		name = "Star brightness",
		description = "Procedural sky: star intensity in percent.",
		position = 9,
		section = proceduralSection
	)
	default int starBrightness()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "shootingStars",
		name = "Shooting stars",
		description = "Procedural sky: occasional meteors at night.",
		position = 10,
		section = proceduralSection
	)
	default boolean shootingStars()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nebula",
		name = "Nebula",
		description = "Procedural sky: faint coloured nebula clouds behind the stars.",
		position = 11,
		section = proceduralSection
	)
	default boolean nebula()
	{
		return true;
	}

	@ConfigItem(
		keyName = "aurora",
		name = "Aurora",
		description = "Procedural sky: northern lights at night.",
		position = 12,
		section = proceduralSection
	)
	default boolean aurora()
	{
		return false;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "cloudCover",
		name = "Cloud cover",
		description = "Procedural sky: how much of the sky is clouded, in percent. 0 = clear.",
		position = 13,
		section = proceduralSection
	)
	default int cloudCover()
	{
		return 30;
	}

	@Range(min = 0, max = 500)
	@ConfigItem(
		keyName = "cloudSpeed",
		name = "Cloud speed",
		description = "Procedural sky: cloud drift speed in percent. 0 = still.",
		position = 14,
		section = proceduralSection
	)
	default int cloudSpeed()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "areaMoods",
		name = "Area moods",
		description = "Let areas override the Time of day: Morytania stays at dusk, Darkmeyer under a blood moon, the desert at high noon.",
		position = 15,
		section = proceduralSection
	)
	default boolean areaMoods()
	{
		return true;
	}

	@ConfigItem(
		keyName = "skyboxOverworldOnly",
		name = "Overworld only",
		description = "Only draw the sky in the overworld, keep the flat fog colour underground and in dungeons.",
		position = 3,
		section = skySection
	)
	default boolean skyboxOverworldOnly()
	{
		return true;
	}

	@ConfigItem(
		keyName = "preferGameSkybox",
		name = "Prefer game skybox",
		description = "Where the game ships its own skybox model, draw that instead.",
		position = 4,
		section = skySection
	)
	default boolean preferGameSkybox()
	{
		return false;
	}

	@ConfigItem(
		keyName = "skyboxFogColorMode",
		name = "Fog colour",
		description = "AUTO: the area's fog colour from 117 HD's environment table where one exists, else the game colour when set, else the sky. SKYBOX: the sky's horizon colour. GAME: the client's sky colour (needs the Skybox colour plugin, otherwise black). CUSTOM: the colour below. Fog depth 0 (above) turns terrain fog off, horizon blend 0 turns the sky fade off.",
		position = 2,
		section = fogSection
	)
	default FogColorMode skyboxFogColorMode()
	{
		return FogColorMode.AUTO;
	}

	@ConfigItem(
		keyName = "skyboxFogCustomColor",
		name = "Custom fog colour",
		description = "Fog colour used when Fog colour is set to CUSTOM.",
		position = 3,
		section = fogSection
	)
	default Color skyboxFogCustomColor()
	{
		return new Color(196, 212, 232);
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "skyboxHorizonBlend",
		name = "Horizon blend",
		description = "How far above the horizon the fog colour fades into the sky. 0 = no fade.",
		position = 4,
		section = fogSection
	)
	default int skyboxHorizonBlend()
	{
		return 5;
	}

	@Range(min = 0, max = 45)
	@ConfigItem(
		keyName = "skyboxHorizonOffset",
		name = "Horizon offset",
		description = "Degrees the sky horizon is pushed below the true horizon. Raise it until no flat band shows between the terrain edge and the sky; lower it if the sky looks squashed.",
		position = 5,
		section = fogSection
	)
	default int skyboxHorizonOffset()
	{
		return 10;
	}

	@Range(min = 0, max = 100)
	@ConfigItem(
		keyName = "skyboxFogTint",
		name = "Fog tint",
		description = "How much of the fog colour is mixed into the whole sky.",
		position = 6,
		section = fogSection
	)
	default int skyboxFogTint()
	{
		return 15;
	}

	@Range(min = 25, max = 200)
	@ConfigItem(
		keyName = "skyboxBrightness",
		name = "Sky brightness",
		description = "Brightness multiplier for the sky, in percent.",
		position = 7,
		section = fogSection
	)
	default int skyboxBrightness()
	{
		return 100;
	}

	@Range(min = 0, max = 359)
	@ConfigItem(
		keyName = "skyboxRotation",
		name = "Rotation",
		description = "Cubemap: rotate the texture around the vertical axis, in degrees.",
		position = 10,
		section = cubemapSection
	)
	default int skyboxRotation()
	{
		return 0;
	}

	@Range(min = 0, max = 60)
	@ConfigItem(
		keyName = "skyboxRotationSpeed",
		name = "Drift speed",
		description = "Cubemap: slowly rotate the texture over time, in degrees per minute. 0 disables drift.",
		position = 11,
		section = cubemapSection
	)
	default int skyboxRotationSpeed()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "skyboxByArea",
		name = "Sky by area",
		description = "Pick the sky and fog colour from the area you are in (Wilderness, Morytania, desert, ...). Areas come from 117 HD's tables via sky_areas.json, override with ~/.runelite/better-skybox/sky_areas.json. Unmapped areas use the Cubemap above.",
		position = 7,
		section = cubemapSection
	)
	default boolean skyboxByArea()
	{
		return true;
	}

	@Range(min = 0, max = 30)
	@ConfigItem(
		keyName = "skyboxFadeSeconds",
		name = "Time fade",
		description = "Seconds to crossfade when the sky changes with the time of day or the config. 0 = instant.",
		position = 8,
		section = cubemapSection
	)
	default int skyboxFadeSeconds()
	{
		return 4;
	}

	@Range(min = 0, max = 64)
	@ConfigItem(
		keyName = "skyboxFadeTiles",
		name = "Area fade",
		description = "Tiles to walk past an area border before the new sky is fully in. Walking back swaps the skies and carries the blend over. 0 = instant.",
		position = 9,
		section = cubemapSection
	)
	default int skyboxFadeTiles()
	{
		return 8;
	}
}
