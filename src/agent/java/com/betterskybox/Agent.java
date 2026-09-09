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
