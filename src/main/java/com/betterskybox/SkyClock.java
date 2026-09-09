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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import com.betterskybox.BetterSkyboxConfig.SkyPreset;

/**
 * The one place that answers "what time is it in the sky". Both renderers and the plugin ask it, so the
 * cubemap phase, the procedural sun and the star map always agree.
 */
class SkyClock
{
	enum Phase
	{
		NIGHT,
		DAWN,
		DAY,
		DUSK
	}

	private static final double SYNODIC_MONTH_DAYS = 29.530588;
	private static final LocalDateTime REFERENCE_NEW_MOON = LocalDateTime.of(2000, 1, 6, 18, 14);
	private final long startNanos = System.nanoTime();

	float elapsedSeconds()
	{
		return (float) ((System.nanoTime() - startNanos) / 1e9);
	}

	/** Hour of the sky day in [0, 24). Presets map to the hour their sun azimuth implies. */
	float hour(BetterSkyboxConfig config)
	{
		SkyPreset preset = config.skyPreset();
		switch (preset)
		{
			case CLOCK:
			{
				LocalTime now = LocalTime.now();
				return now.getHour() + now.getMinute() / 60f + now.getSecond() / 3600f;
			}
			case CYCLE:
			{
				float day = elapsedSeconds() / (config.cycleMinutes() * 60f);
				return (6 + day * 24) % 24;
			}
			case CUSTOM:
				return hourForAzimuth(config.sunAzimuth());
			default:
				return hourForAzimuth(preset.azimuth);
		}
	}

	static Phase phase(float hour)
	{
		if (hour < 5 || hour >= 20)
		{
			return Phase.NIGHT;
		}
		if (hour < 7)
		{
			return Phase.DAWN;
		}
		if (hour < 17)
		{
			return Phase.DAY;
		}
		return Phase.DUSK;
	}

	/** Sun altitude in degrees for a 24h clock: -60 at midnight, +60 at noon. */
	static float altitudeForHour(float hour)
	{
		return (float) Math.sin((hour - 6) / 24 * 2 * Math.PI) * 60;
	}

	/** East at 6h, south at noon, west at 18h. */
	static float azimuthForHour(float hour)
	{
		return 90 + (hour - 6) * 15;
	}

	static float hourForAzimuth(float azimuth)
	{
		return ((azimuth - 90) / 15 + 6 + 24) % 24;
	}

	/** Moon disk illumination fraction from days since new moon. */
	private static float illumination(double daysSinceNewMoon)
	{
		return (float) ((1 - Math.cos(daysSinceNewMoon / SYNODIC_MONTH_DAYS * 2 * Math.PI)) / 2);
	}

	/** Fraction of the moon's disk that is lit, 0 = new, 1 = full. */
	static float moonIllumination(LocalDateTime utc)
	{
		double days = ChronoUnit.SECONDS.between(REFERENCE_NEW_MOON, utc) / 86400.0;
		double age = ((days % SYNODIC_MONTH_DAYS) + SYNODIC_MONTH_DAYS) % SYNODIC_MONTH_DAYS;
		return illumination(age);
	}

	/**
	 * Illumination for the current sky time: the real calendar under CLOCK, one lunar month per 29.5 cycles
	 * under CYCLE, or -1 when the preset leaves the moon to the slider.
	 */
	float moonIllumination(BetterSkyboxConfig config)
	{
		switch (config.skyPreset())
		{
			case CLOCK:
				return moonIllumination(LocalDateTime.now(ZoneOffset.UTC));
			case CYCLE:
			{
				double cycles = elapsedSeconds() / (config.cycleMinutes() * 60.0);
				return illumination(cycles);
			}
			default:
				return -1;
		}
	}
}
