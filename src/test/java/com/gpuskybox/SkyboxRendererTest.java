package com.gpuskybox;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SkyboxRendererTest
{
	@Test
	public void mixColorLerpsEachChannel()
	{
		// the lightning brightening: 60 % of the way from black to white
		assertEquals(0x999999, SkyboxRenderer.mixColor(0x000000, 0xFFFFFF, 0.6f));
		assertEquals(0x80C0FF, SkyboxRenderer.mixColor(0x0080FF, 0xFFFFFF, 0.5f));
	}

	@Test
	public void mixColorEndpointsAreExact()
	{
		// no flash leaves the fog colour untouched, a full blend is the target colour
		assertEquals(0x123456, SkyboxRenderer.mixColor(0x123456, 0xFFFFFF, 0f));
		assertEquals(0xFFFFFF, SkyboxRenderer.mixColor(0x123456, 0xFFFFFF, 1f));
	}
}
