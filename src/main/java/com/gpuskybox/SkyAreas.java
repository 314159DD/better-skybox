package com.gpuskybox;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

/**
 * Maps world positions to a sky and fog colour. Bundled {@code sky_areas.json} is generated from 117 HD's area and
 * environment tables by {@code tools/hd_to_sky_areas.py}; {@code ~/.runelite/gpu-skybox/sky_areas.json} replaces it
 * when present. Entries are checked in order, first match wins.
 */
@Slf4j
class SkyAreas
{
	static final File CUSTOM_FILE = new File(new File(RuneLite.RUNELITE_DIR, "gpu-skybox"), "sky_areas.json");

	static class Area
	{
		String name;
		/** Cubemap folder, or null to keep the configured one. */
		String sky;
		/** Per-phase cubemaps; null means "use sky" (and the global dawn/dusk/night defaults when sky is null too). */
		String skyDawn;
		String skyDusk;
		String skyNight;
		/** Fog colour as #RRGGBB, or null. */
		String fog;
		/** 117 HD lightningEffects: random flashes while the player is here. */
		boolean lightning;
		/** Procedural preset forced in this area (a SkyPreset name), or null. */
		String preset;
		transient GpuSkyboxConfig.SkyPreset presetValue;
		/** [x1, y1, x2, y2, plane1, plane2] in world tiles, inclusive. */
		int[][] aabbs;
		/** Region ids (x << 8 | y). */
		int[] regions;
		/** [rx1, ry1, rx2, ry2, plane1, plane2] in region coordinates, inclusive. */
		int[][] regionBoxes;

		transient int fogColor = -1;

		boolean contains(int x, int y, int plane, int regionId)
		{
			if (aabbs != null)
			{
				for (int[] b : aabbs)
				{
					if (x >= b[0] && x <= b[2] && y >= b[1] && y <= b[3] && plane >= b[4] && plane <= b[5])
					{
						return true;
					}
				}
			}
			if (regions != null)
			{
				for (int r : regions)
				{
					if (r == regionId)
					{
						return true;
					}
				}
			}
			if (regionBoxes != null)
			{
				int rx = regionId >> 8;
				int ry = regionId & 0xFF;
				for (int[] b : regionBoxes)
				{
					if (rx >= b[0] && rx <= b[2] && ry >= b[1] && ry <= b[3] && plane >= b[4] && plane <= b[5])
					{
						return true;
					}
				}
			}
			return false;
		}

		String skyFor(SkyClock.Phase phase)
		{
			switch (phase)
			{
				case DAWN:
					return skyDawn != null ? skyDawn : sky;
				case DUSK:
					return skyDusk != null ? skyDusk : sky;
				case NIGHT:
					return skyNight != null ? skyNight : sky;
				default:
					return sky;
			}
		}
	}

	static GpuSkyboxConfig.SkyPreset parsePreset(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return GpuSkyboxConfig.SkyPreset.valueOf(name);
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("sky_areas.json: unknown preset '{}'", name);
			return null;
		}
	}

	private Area[] areas = new Area[0];

	void load(Gson gson)
	{
		try
		{
			if (CUSTOM_FILE.isFile())
			{
				try (Reader in = new FileReader(CUSTOM_FILE, StandardCharsets.UTF_8))
				{
					areas = gson.fromJson(in, Area[].class);
				}
				log.info("Loaded {} sky areas from {}", areas.length, CUSTOM_FILE);
			}
			else
			{
				try (InputStream in = SkyAreas.class.getResourceAsStream("sky_areas.json"))
				{
					areas = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Area[].class);
				}
				log.info("Loaded {} bundled sky areas", areas.length);
			}
			for (Area a : areas)
			{
				a.fogColor = a.fog == null ? -1 : Integer.parseInt(a.fog.substring(1), 16);
				a.presetValue = parsePreset(a.preset);
			}
		}
		catch (Exception ex)
		{
			log.warn("sky_areas.json invalid, area skies disabled", ex);
			areas = new Area[0];
		}
	}

	/** The first area containing this point, or null when unmapped. */
	Area find(WorldPoint p)
	{
		int regionId = p.getRegionID();
		for (Area a : areas)
		{
			if (a.contains(p.getX(), p.getY(), p.getPlane(), regionId))
			{
				return a;
			}
		}
		return null;
	}
}
