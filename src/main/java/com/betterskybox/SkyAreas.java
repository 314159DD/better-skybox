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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

/**
 * Maps world positions to a sky and fog colour. Bundled {@code sky_areas.json} is generated from 117 HD's area and
 * environment tables by {@code tools/hd_to_sky_areas.py}; {@code ~/.runelite/better-skybox/sky_areas.json} replaces it
 * when present. Entries are checked in order, first match wins.
 */
@Slf4j
class SkyAreas
{
	static final File CUSTOM_FILE = new File(new File(RuneLite.RUNELITE_DIR, "better-skybox"), "sky_areas.json");

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
		transient BetterSkyboxConfig.SkyPreset presetValue;
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

	static BetterSkyboxConfig.SkyPreset parsePreset(String name)
	{
		if (name == null)
		{
			return null;
		}
		try
		{
			return BetterSkyboxConfig.SkyPreset.valueOf(name);
		}
		catch (IllegalArgumentException ex)
		{
			log.warn("sky_areas.json: unknown preset '{}'", name);
			return null;
		}
	}

	private Area[] areas;

	SkyAreas()
	{
		this(new Area[0]);
	}

	SkyAreas(Area[] areas)
	{
		this.areas = areas;
	}

	void load(Gson gson)
	{
		try
		{
			if (CUSTOM_FILE.isFile())
			{
				try (Reader in = new FileReader(CUSTOM_FILE, StandardCharsets.UTF_8))
				{
					areas = parse(in, gson);
				}
				log.info("Loaded {} sky areas from {}", areas.length, CUSTOM_FILE);
			}
			else
			{
				areas = bundled(gson);
				log.info("Loaded {} bundled sky areas", areas.length);
			}
		}
		catch (Exception ex)
		{
			log.warn("sky_areas.json invalid, area skies disabled", ex);
			areas = new Area[0];
		}
	}

	/** The table shipped in the jar. */
	static Area[] bundled(Gson gson) throws IOException
	{
		try (InputStream in = SkyAreas.class.getResourceAsStream("sky_areas.json"))
		{
			return parse(new InputStreamReader(in, StandardCharsets.UTF_8), gson);
		}
	}

	private static Area[] parse(Reader in, Gson gson)
	{
		Area[] areas = gson.fromJson(in, Area[].class);
		for (Area a : areas)
		{
			a.fogColor = a.fog == null ? -1 : Integer.parseInt(a.fog.substring(1), 16);
			a.presetValue = parsePreset(a.preset);
		}
		return areas;
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
