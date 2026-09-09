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

import java.io.File;
import org.junit.Test;
import com.betterskybox.CubemapLoader.Faces;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * The decode half of a cubemap load is pure CPU, so it runs in a plain unit test. The upload half is GL and
 * cannot.
 */
public class CubemapLoaderTest
{
	@Test
	public void decodesTheBundledDebugSky()
	{
		Faces faces = CubemapLoader.decode("debug");
		assertNotNull(faces);
		assertEquals("debug", faces.name);
		assertEquals(512, faces.size);
		assertEquals(6, faces.pixels.length);
		for (int[] face : faces.pixels)
		{
			assertEquals(faces.size * faces.size, face.length);
		}
	}

	@Test
	public void horizonColorIsTheMeanOfTheSideFaces()
	{
		// the bundled debug sky, whose band of px nx pz nz averages to this; a swapped red and blue would show
		assertEquals(0xA1579B, CubemapLoader.decode("debug").horizonColor);
	}

	@Test
	public void aMissingFolderDecodesToNull()
	{
		assertNull(CubemapLoader.decode("no-such-sky"));
	}

	@Test
	public void theSkyPackIsSearchedBeforeAHandMadeFolder()
	{
		assertArrayEquals(new File[]{CubemapLoader.PACK_DIR, CubemapLoader.CUSTOM_DIR}, CubemapLoader.SEARCH_DIRS);
	}

	@Test
	public void theSkyPackLivesInOneFolderUnderTheCustomOne()
	{
		assertEquals(CubemapLoader.CUSTOM_DIR, CubemapLoader.PACK_DIR.getParentFile());
		assertEquals("pack", CubemapLoader.PACK_DIR.getName());
		assertEquals(CubemapLoader.PACK_DIR, CubemapLoader.PACK_MARKER.getParentFile());
	}
}
