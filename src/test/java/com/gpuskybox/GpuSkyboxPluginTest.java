package com.gpuskybox;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GpuSkyboxPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GpuSkyboxPlugin.class);
		RuneLite.main(args);
	}
}
