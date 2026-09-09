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

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import net.runelite.client.externalplugins.ExternalPluginManager;
import net.runelite.client.plugins.Plugin;

/**
 * Entry point when the jar is passed to the official RuneLite launcher as {@code -javaagent}.
 * Registers the plugin as a builtin external plugin before the client's main runs, which is the same
 * thing the Gradle test runner does, but inside the launcher-managed client so Jagex Launcher login works.
 * <p>
 * {@link ExternalPluginManager#loadBuiltin} insists on {@code -ea}; enabling assertions for the whole
 * production client is not wanted, so the builtin list is set directly.
 */
public class Agent
{
	@SuppressWarnings("unchecked")
	public static void premain(String args, Instrumentation inst) throws ReflectiveOperationException
	{
		Field builtinExternals = ExternalPluginManager.class.getDeclaredField("builtinExternals");
		builtinExternals.setAccessible(true);
		builtinExternals.set(null, new Class[]{BetterSkyboxPlugin.class});
	}
}
