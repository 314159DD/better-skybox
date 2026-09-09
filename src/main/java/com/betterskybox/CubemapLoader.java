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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import static org.lwjgl.opengl.GL33C.*;

/**
 * Loads a cubemap by folder name, as six px nx py ny pz nz .png files or one 4x2 skybox.png atlas. Faces are
 * looked up in the downloaded sky pack ~/.runelite/better-skybox/pack/&lt;name&gt;/, then in a hand-made
 * ~/.runelite/better-skybox/&lt;name&gt;/, then in the bundled resources under skybox/&lt;name&gt;/, which hold
 * the debug sky and nothing else: the 21 skies and the star map are 35 MB and ship in the pack.
 * <p>
 * The load has two halves: {@link #decode} is pure CPU and runs off the client thread, {@link #upload} is nothing
 * but GL and runs on it.
 */
@Slf4j
final class CubemapLoader
{
	private static final String[] FACES = {"px", "nx", "py", "ny", "pz", "nz"};
	/** Hand-made sky folders, one per name. */
	static final File CUSTOM_DIR = new File(RuneLite.RUNELITE_DIR, "better-skybox");
	/** Where the sky pack unpacks to, so removing it is deleting one folder. */
	static final File PACK_DIR = new File(CUSTOM_DIR, "pack");
	/** Written once an extraction has been verified; its absence is what "not installed" means. */
	static final File PACK_MARKER = new File(PACK_DIR, ".complete");
	/**
	 * Folders searched for a face, in order, before the jar: the pack first, so a sky it ships is the one the
	 * area table and the config enum name, whatever else is on disk.
	 */
	static final File[] SEARCH_DIRS = {PACK_DIR, CUSTOM_DIR};

	static class Cubemap
	{
		int texture;
		/** Average colour of the horizon band of the side faces, packed 0xRRGGBB. */
		int horizonColor;
	}

	/** One decoded cubemap, ready to upload. Holds no GL handle, so it can be built on any thread. */
	static final class Faces
	{
		final String name;
		/** Width and height of every face; cubemap faces must be square and all the same size. */
		final int size;
		/** px nx py ny pz nz, each {@code size * size} pixels packed 0xAARRGGBB. */
		final int[][] pixels;
		/** Average colour of the horizon band of the side faces, packed 0xRRGGBB. */
		final int horizonColor;

		Faces(String name, int size, int[][] pixels, int horizonColor)
		{
			this.name = name;
			this.size = size;
			this.pixels = pixels;
			this.horizonColor = horizonColor;
		}
	}

	private CubemapLoader()
	{
	}

	/** Whether the sky pack is on disk. Without it only the debug sky loads, and only the procedural sky draws. */
	static boolean packInstalled()
	{
		return PACK_MARKER.isFile();
	}

	/** Reads and decodes the six faces, or returns null when the images are missing, broken or mismatched. */
	static Faces decode(String name)
	{
		BufferedImage[] images = loadFaces(name);
		if (images == null)
		{
			return null;
		}

		int size = images[0].getWidth();
		int[][] pixels = new int[6][];
		for (int i = 0; i < 6; i++)
		{
			BufferedImage face = images[i];
			if (face.getWidth() != size || face.getHeight() != size)
			{
				log.warn("Cubemap '{}' face {} is {}x{}, expected {}x{}", name, FACES[i], face.getWidth(),
					face.getHeight(), size, size);
				return null;
			}
			pixels[i] = toIntArgb(face, size);
		}
		return new Faces(name, size, pixels, averageHorizonColor(pixels, size));
	}

	/** Uploads decoded faces on the given texture unit. Client thread only. */
	static Cubemap upload(Faces faces, int textureUnit)
	{
		Cubemap cubemap = new Cubemap();
		cubemap.horizonColor = faces.horizonColor;
		cubemap.texture = glGenTextures();
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap.texture);
		for (int i = 0; i < 6; i++)
		{
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA8, faces.size, faces.size, 0,
				GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, faces.pixels[i]);
		}
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
		glActiveTexture(GL_TEXTURE0);

		log.info("Loaded cubemap '{}' ({}x{} per face)", faces.name, faces.size, faces.size);
		return cubemap;
	}

	/** Mean of the 4 percent band around the middle of the four side faces, packed 0xRRGGBB. */
	private static int averageHorizonColor(int[][] pixels, int size)
	{
		long r = 0, g = 0, b = 0, n = 0;
		for (int i : new int[]{0, 1, 4, 5}) // px, nx, pz, nz: the side faces
		{
			int[] face = pixels[i];
			int end = (int) (size * 0.52) * size;
			for (int p = (int) (size * 0.48) * size; p < end; p++)
			{
				int argb = face[p];
				r += argb >> 16 & 0xFF;
				g += argb >> 8 & 0xFF;
				b += argb & 0xFF;
				n++;
			}
		}
		return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
	}

	/**
	 * Six faces from either a 4x2 atlas {@code skybox.png} (row 1: px nz nx pz, row 2: py ny) or six face files.
	 */
	private static BufferedImage[] loadFaces(String name)
	{
		BufferedImage atlas = loadImage(name, "skybox");
		if (atlas != null)
		{
			int face = atlas.getWidth() / 4;
			if (face == 0 || atlas.getHeight() < face * 2)
			{
				log.warn("Cubemap '{}' atlas is {}x{}, expected 4x2 faces", name, atlas.getWidth(), atlas.getHeight());
				return null;
			}
			return new BufferedImage[]{
				atlas.getSubimage(0, 0, face, face),          // px
				atlas.getSubimage(face * 2, 0, face, face),   // nx
				atlas.getSubimage(0, face, face, face),       // py
				atlas.getSubimage(face, face, face, face),    // ny
				atlas.getSubimage(face * 3, 0, face, face),   // pz
				atlas.getSubimage(face, 0, face, face),       // nz
			};
		}

		BufferedImage[] faces = new BufferedImage[6];
		for (int i = 0; i < 6; i++)
		{
			faces[i] = loadImage(name, FACES[i]);
			if (faces[i] == null)
			{
				log.warn("Cubemap '{}' is missing {}.png (and has no skybox.png atlas)", name, FACES[i]);
				return null;
			}
		}
		return faces;
	}

	private static BufferedImage loadImage(String name, String file)
	{
		File onDisk = null;
		try
		{
			for (File dir : SEARCH_DIRS)
			{
				onDisk = new File(new File(dir, name), file + ".png");
				if (onDisk.isFile())
				{
					return ImageIO.read(onDisk);
				}
			}
			try (InputStream in = CubemapLoader.class.getResourceAsStream("skybox/" + name + "/" + file + ".png"))
			{
				return in == null ? null : ImageIO.read(in);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed to read cubemap image {}", onDisk, ex);
			return null;
		}
	}

	/** Copies into a fresh buffer: an atlas subimage shares its parent's raster, which GL must not see. */
	private static int[] toIntArgb(BufferedImage src, int size)
	{
		BufferedImage dst = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = dst.getGraphics();
		graphics.drawImage(src, 0, 0, null);
		graphics.dispose();
		return ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
	}
}
