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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GpuSettingsImportTest
{
	@Test
	public void onlyStoredValuesAreCopied()
	{
		Map<String, String> stored = Map.of("drawDistance", "90", "numThreads", "7");
		Map<String, String> copy = GpuSettingsImport.plan(stored::get, List.of("drawDistance", "fogDepth", "numThreads"));
		assertEquals(stored, copy);
	}

	@Test
	public void copyKeepsKeyOrder()
	{
		Map<String, String> stored = Map.of("a", "1", "b", "2", "c", "3");
		Map<String, String> copy = GpuSettingsImport.plan(stored::get, List.of("c", "a", "b"));
		assertEquals(List.of("c", "a", "b"), new ArrayList<>(copy.keySet()));
	}

	@Test
	public void nothingStoredCopiesNothing()
	{
		assertTrue(GpuSettingsImport.plan(key -> null, GpuSettingsImport.KEYS).isEmpty());
	}

	/** Every {@code @ConfigItem} key of {@link BetterSkyboxConfig} that sits in {@code section}. */
	private static Set<String> keysInSection(String section)
	{
		Set<String> keys = new HashSet<>();
		for (Method m : BetterSkyboxConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null && item.section().equals(section))
			{
				keys.add(item.keyName());
			}
		}
		return keys;
	}

	@Test
	public void theImportedKeysAreExactlyTheRendererOnes()
	{
		// the patch puts every upstream item in Renderer, fogDepth aside, so an upstream key added at the next
		// client bump lands in that section and fails here until it is imported too
		Set<String> expected = new HashSet<>(keysInSection(BetterSkyboxConfig.rendererSection));
		assertEquals(15, expected.size());
		expected.add("fogDepth");
		assertEquals(expected, new HashSet<>(GpuSettingsImport.KEYS));
		assertEquals(expected.size(), GpuSettingsImport.KEYS.size());
	}

	@Test
	public void theOneUpstreamKeyOutsideRendererIsFogDepth()
	{
		assertTrue(keysInSection(BetterSkyboxConfig.fogSection).contains("fogDepth"));
	}

	@Test
	public void theMarkerStopsASecondImport()
	{
		Map<String, String> ours = Map.of(GpuSettingsImport.MARKER, "true");
		List<String> written = new ArrayList<>();
		GpuSettingsImport.run((group, key) -> ours.get(key), (group, key, value) -> written.add(key));
		assertTrue(written.isEmpty());
	}

	@Test
	public void theMarkerIsWrittenEvenWhenNothingWasCopied()
	{
		// a fresh client with no stock GPU settings stored: the import must still not run a second time
		Map<String, String> written = new HashMap<>();
		GpuSettingsImport.run((group, key) -> null, (group, key, value) -> written.put(key, value));
		assertEquals(Map.of(GpuSettingsImport.MARKER, "true"), written);
	}

	@Test
	public void nothingIsEverWrittenToTheGpuGroup()
	{
		Map<String, String> gpu = Map.of("drawDistance", "90", "numThreads", "7");
		List<String> groups = new ArrayList<>();
		Map<String, String> ours = new HashMap<>();
		GpuSettingsImport.run(
			(group, key) -> GpuSettingsImport.GPU_GROUP.equals(group) ? gpu.get(key) : ours.get(key),
			(group, key, value) ->
			{
				groups.add(group);
				ours.put(key, value);
			});
		assertEquals(Set.of(BetterSkyboxConfig.GROUP), new HashSet<>(groups));
		assertEquals(Map.of("drawDistance", "90", "numThreads", "7", GpuSettingsImport.MARKER, "true"), ours);
	}
}
