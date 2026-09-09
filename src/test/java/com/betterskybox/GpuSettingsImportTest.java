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

	@Test
	public void everyImportedKeyIsARendererSetting()
	{
		Set<String> declared = new HashSet<>();
		for (Method m : BetterSkyboxConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				declared.add(item.keyName());
			}
		}
		for (String key : GpuSettingsImport.KEYS)
		{
			assertTrue(key, declared.contains(key));
		}
		assertEquals(16, GpuSettingsImport.KEYS.size());
	}
}
