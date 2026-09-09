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
