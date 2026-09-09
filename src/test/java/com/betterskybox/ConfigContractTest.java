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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The config surface as the panel and the plugin see it: unique keys, every item in a declared section, and
 * every key the code compares against by string literal still declared. Reads the two sources that carry
 * {@code onConfigChanged} so a renamed key cannot silently orphan its handler.
 */
public class ConfigContractTest
{
	private static final Path SOURCES = Paths.get("src", "main", "java", "com", "betterskybox");
	private static final Pattern KEY_LITERAL = Pattern.compile("getKey\\(\\)\\.equals\\(\"([^\"]+)\"\\)");

	private static List<ConfigItem> items()
	{
		List<ConfigItem> items = new ArrayList<>();
		for (Method m : BetterSkyboxConfig.class.getDeclaredMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				items.add(item);
			}
		}
		return items;
	}

	private static Set<String> keys()
	{
		Set<String> keys = new HashSet<>();
		for (ConfigItem item : items())
		{
			keys.add(item.keyName());
		}
		return keys;
	}

	private static Set<String> sections() throws IllegalAccessException
	{
		Set<String> sections = new HashSet<>();
		for (Field f : BetterSkyboxConfig.class.getDeclaredFields())
		{
			if (f.getAnnotation(ConfigSection.class) != null)
			{
				sections.add((String) f.get(null));
			}
		}
		return sections;
	}

	private static List<String> keyLiterals(String file) throws IOException
	{
		Path source = SOURCES.resolve(file);
		assertTrue(source + " (run from the project root)", Files.isRegularFile(source));
		List<String> literals = new ArrayList<>();
		Matcher m = KEY_LITERAL.matcher(Files.readString(source, StandardCharsets.UTF_8));
		while (m.find())
		{
			literals.add(m.group(1));
		}
		assertFalse(file + " compares no keys", literals.isEmpty());
		return literals;
	}

	@Test
	public void noTwoItemsShareAKey()
	{
		Set<String> seen = new HashSet<>();
		for (ConfigItem item : items())
		{
			assertTrue("duplicate keyName " + item.keyName(), seen.add(item.keyName()));
		}
		assertTrue(seen.size() > 50);
	}

	@Test
	public void everyItemSitsInADeclaredSection() throws IllegalAccessException
	{
		Set<String> sections = sections();
		assertTrue(sections.size() >= 5);
		for (ConfigItem item : items())
		{
			assertTrue(item.keyName() + " section '" + item.section() + "'", sections.contains(item.section()));
		}
	}

	@Test
	public void everyKeyComparedInOnConfigChangedIsDeclared() throws IOException
	{
		Set<String> keys = keys();
		for (String file : new String[]{"BetterSkyboxPlugin.java", "SkyPass.java"})
		{
			for (String literal : keyLiterals(file))
			{
				assertTrue(file + " compares undeclared key " + literal, keys.contains(literal));
			}
		}
	}
}
