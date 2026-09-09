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

import com.betterskybox.BetterSkyboxConfig.SkyboxTexture;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Every resource the renderers open at runtime must be in the jar, and every sky they name must be in the sky
 * pack. Lookups mirror the production code: cubemaps relative to {@link CubemapLoader} the way
 * {@code CubemapLoader.loadImage} does, shaders and their quoted {@code #include}s relative to
 * {@link BetterSkyboxPlugin} the way {@code Template.addInclude} does. The pack itself is a release asset and
 * is not here to be read, so its folder names are checked against {@code sky_pack_manifest.json}, which
 * {@code tools/build_sky_pack.py} writes from the folders it zipped.
 */
public class ResourceIntegrityTest
{
	private static final String[] FACES = {"px", "nx", "py", "ny", "pz", "nz"};

	/** The folder names the sky pack ships, as its build script recorded them. */
	static Set<String> skyPack()
	{
		try (InputStream in = CubemapLoader.class.getResourceAsStream("sky_pack_manifest.json"))
		{
			assertNotNull("sky_pack_manifest.json", in);
			Gson gson = new Gson();
			String[] names = gson.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), String[].class);
			return new HashSet<>(Arrays.asList(names));
		}
		catch (IOException ex)
		{
			throw new UncheckedIOException(ex);
		}
	}

	/** True when {@code skybox/<name>/} ships either the atlas or all six faces. */
	static boolean cubemapBundled(String name)
	{
		if (bundled(CubemapLoader.class, "skybox/" + name + "/skybox.png"))
		{
			return true;
		}
		for (String face : FACES)
		{
			if (!bundled(CubemapLoader.class, "skybox/" + name + "/" + face + ".png"))
			{
				return false;
			}
		}
		return true;
	}

	private static boolean bundled(Class<?> relativeTo, String path)
	{
		try (InputStream in = relativeTo.getResourceAsStream(path))
		{
			return in != null;
		}
		catch (IOException ex)
		{
			throw new UncheckedIOException(ex);
		}
	}

	@Test
	public void everySkyboxTextureIsInTheSkyPack()
	{
		Set<String> pack = skyPack();
		for (SkyboxTexture texture : SkyboxTexture.values())
		{
			if (texture != SkyboxTexture.CUSTOM && texture != SkyboxTexture.DEBUG)
			{
				assertTrue(texture + " -> " + texture.dir, pack.contains(texture.dir));
			}
		}
	}

	@Test
	public void starMapIsInTheSkyPack()
	{
		assertTrue(skyPack().contains("stars"));
	}

	/** The one cubemap still in the jar: DEBUG draws without the pack, and the loader test decodes it. */
	@Test
	public void debugSkyIsBundled()
	{
		assertTrue(cubemapBundled(SkyboxTexture.DEBUG.dir));
	}

	@Test
	public void skyGradientIsBundled()
	{
		assertTrue(bundled(ProceduralSkyRenderer.class, "sky_gradient.json"));
	}

	@Test
	public void everyShaderAndItsIncludesAreBundled() throws IOException
	{
		Set<String> seen = new HashSet<>();
		for (Shader shader : new Shader[]{BetterSkyboxPlugin.PROGRAM, BetterSkyboxPlugin.UI_PROGRAM,
			SkyboxRenderer.PROGRAM, ProceduralSkyRenderer.PROGRAM})
		{
			for (Shader.Unit unit : shader.units)
			{
				assertShaderResolves(unit.getFilename(), seen);
			}
		}
	}

	private static void assertShaderResolves(String filename, Set<String> seen) throws IOException
	{
		if (!seen.add(filename))
		{
			return;
		}
		String source;
		try (InputStream in = BetterSkyboxPlugin.class.getResourceAsStream(filename))
		{
			assertNotNull(filename, in);
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		for (String line : source.split("\r?\n"))
		{
			if (line.startsWith("#include \""))
			{
				assertShaderResolves(line.substring(10, line.length() - 1), seen);
			}
		}
	}
}
