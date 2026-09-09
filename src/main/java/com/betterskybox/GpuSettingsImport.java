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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

/**
 * Copies the stock GPU plugin's renderer settings into this plugin's config group, once. The renderer keys are
 * upstream's, unchanged, so a value stored under {@code gpu} is valid under {@code betterskybox} as it is; a key
 * the user never changed has no stored value and keeps our default. Group {@code gpu} is only ever read.
 */
@Slf4j
final class GpuSettingsImport
{
	static final String GPU_GROUP = "gpu";
	/** Set in our group after the one import, whatever it copied. */
	static final String MARKER = "importedFromGpu";
	/** Every key {@link BetterSkyboxConfig} shares with upstream {@code GpuPluginConfig}. */
	static final List<String> KEYS = List.of(
		"drawDistance",
		"hideUnrelatedMaps",
		"expandedMapLoadingChunks",
		"smoothBanding",
		"antiAliasingMode",
		"uiScalingMode",
		"fogDepth",
		"anisotropicFilteringLevel",
		"colorBlindMode",
		"colorBlindIntensity",
		"brightTextures",
		"unlockFps",
		"vsyncMode",
		"fpsTarget",
		"removeVertexSnapping",
		"numThreads");

	/** Where a value goes: group, key, value, as {@link ConfigManager#setConfiguration(String, String, String)}. */
	interface Writer
	{
		void write(String group, String key, String value);
	}

	private GpuSettingsImport()
	{
	}

	/** The values to copy, in key order: each key of {@code keys} that {@code gpuLookup} has a value for. */
	static Map<String, String> plan(Function<String, String> gpuLookup, List<String> keys)
	{
		Map<String, String> copy = new LinkedHashMap<>();
		for (String key : keys)
		{
			String value = gpuLookup.apply(key);
			if (value != null)
			{
				copy.put(key, value);
			}
		}
		return copy;
	}

	/** Runs the import unless {@link #MARKER} says it already ran. */
	static void run(ConfigManager configManager)
	{
		run(configManager::getConfiguration, configManager::setConfiguration);
	}

	/**
	 * The seam tests use: {@code read} and {@code write} take the group, so a test sees that group {@code gpu}
	 * is only ever read. The marker is written whatever was copied, so nothing stored later under {@code gpu}
	 * can arrive after the user has settings of their own.
	 */
	static void run(BiFunction<String, String, String> read, Writer write)
	{
		if (read.apply(BetterSkyboxConfig.GROUP, MARKER) != null)
		{
			return;
		}
		Map<String, String> copy = plan(key -> read.apply(GPU_GROUP, key), KEYS);
		copy.forEach((key, value) -> write.write(BetterSkyboxConfig.GROUP, key, value));
		write.write(BetterSkyboxConfig.GROUP, MARKER, "true");
		log.info("Imported {} of {} renderer settings from the GPU plugin", copy.size(), KEYS.size());
	}
}
