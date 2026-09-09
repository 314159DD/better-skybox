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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The sky pack is a zip off the internet, so what it may contain is a pure rule: {@code <sky>/<face>.png},
 * one folder deep and nothing else. Everything the extractor does with a file it accepted lands inside
 * {@code ~/.runelite/better-skybox/pack/}.
 */
public class SkyPackTest
{
	@Test
	public void aFaceOneFolderDeepIsTaken()
	{
		assertTrue(SkyPack.packEntry("qwantani_night_puresky/px.png"));
		assertTrue(SkyPack.packEntry("stars/nz.png"));
		assertTrue(SkyPack.packEntry("stars/credits.json"));
	}

	@Test
	public void aPathThatLeavesThePackFolderIsRefused()
	{
		assertFalse(SkyPack.packEntry("../evil.png"));
		assertFalse(SkyPack.packEntry("sky/../../evil.png"));
		assertFalse(SkyPack.packEntry("./evil.png"));
		assertFalse(SkyPack.packEntry("/etc/evil.png"));
		assertFalse(SkyPack.packEntry("..\\evil.png"));
		assertFalse(SkyPack.packEntry("C:/windows/evil.png"));
	}

	@Test
	public void anyOtherDepthIsRefused()
	{
		assertFalse(SkyPack.packEntry("px.png"));
		assertFalse(SkyPack.packEntry("sky/deeper/px.png"));
		assertFalse(SkyPack.packEntry("sky/"));
	}

	@Test
	public void anythingThatIsNotAnImageOrATableIsRefused()
	{
		assertFalse(SkyPack.packEntry("sky/px.exe"));
		assertFalse(SkyPack.packEntry("sky/px.png.exe"));
		assertFalse(SkyPack.packEntry("sky/px.png/"));
	}
}
