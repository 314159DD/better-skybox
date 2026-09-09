package com.gpuskybox;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * Maps map regions to cubemap names. Bundled {@code sky_areas.json}, or {@code ~/.runelite/gpu-skybox/sky_areas.json}
 * when that exists. First matching area wins, so put small areas (a swamp inside a larger land) first.
 */
@Slf4j
class SkyAreas
{
	static final File CUSTOM_FILE = new File(new File(RuneLite.RUNELITE_DIR, "gpu-skybox"), "sky_areas.json");

	static class Area
	{
		String name;
		String sky;
		/** [x1, y1, x2, y2] in region coordinates (region id = x << 8 | y), inclusive. */
		int[][] regions;

		boolean contains(int rx, int ry)
		{
			for (int[] r : regions)
			{
				if (rx >= r[0] && rx <= r[2] && ry >= r[1] && ry <= r[3])
				{
					return true;
				}
			}
			return false;
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
				return;
			}
			try (InputStream in = SkyAreas.class.getResourceAsStream("sky_areas.json"))
			{
				areas = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), Area[].class);
			}
			log.info("Loaded {} bundled sky areas", areas.length);
		}
		catch (Exception ex)
		{
			log.warn("sky_areas.json invalid, area skies disabled", ex);
			areas = new Area[0];
		}
	}

	/** The area containing this region, or null when unmapped. */
	Area find(int regionId)
	{
		int rx = regionId >> 8;
		int ry = regionId & 0xFF;
		for (Area a : areas)
		{
			if (a.contains(rx, ry))
			{
				return a;
			}
		}
		return null;
	}
}
