package com.betterskybox;

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
 * Loads a cubemap by folder name. Faces are looked up first in ~/.runelite/better-skybox/&lt;name&gt;/ and then in the
 * bundled resources under skybox/&lt;name&gt;/, as six px nx py ny pz nz .png files or one 4x2 skybox.png atlas.
 */
@Slf4j
final class CubemapLoader
{
	private static final String[] FACES = {"px", "nx", "py", "ny", "pz", "nz"};
	private static final File CUSTOM_DIR = new File(RuneLite.RUNELITE_DIR, "better-skybox");

	static class Cubemap
	{
		int texture;
		/** Average colour of the horizon band of the side faces, packed 0xRRGGBB. */
		int horizonColor;
	}

	private CubemapLoader()
	{
	}

	/** Uploads the cubemap on the given texture unit, or returns null when the images are missing or broken. */
	static Cubemap upload(String name, int textureUnit)
	{
		BufferedImage[] faces = loadFaces(name);
		if (faces == null)
		{
			return null;
		}

		Cubemap cubemap = new Cubemap();
		cubemap.horizonColor = averageHorizonColor(faces);
		cubemap.texture = glGenTextures();
		glActiveTexture(GL_TEXTURE0 + textureUnit);
		glBindTexture(GL_TEXTURE_CUBE_MAP, cubemap.texture);
		for (int i = 0; i < 6; i++)
		{
			BufferedImage face = toIntArgb(faces[i]);
			int[] pixels = ((DataBufferInt) face.getRaster().getDataBuffer()).getData();
			glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA8, face.getWidth(), face.getHeight(), 0,
				GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixels);
		}
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
		glGenerateMipmap(GL_TEXTURE_CUBE_MAP);
		glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
		glActiveTexture(GL_TEXTURE0);

		log.info("Loaded cubemap '{}' ({}x{} per face)", name, faces[0].getWidth(), faces[0].getHeight());
		return cubemap;
	}

	private static int averageHorizonColor(BufferedImage[] faces)
	{
		long r = 0, g = 0, b = 0, n = 0;
		for (int i : new int[]{0, 1, 4, 5}) // px, nx, pz, nz: the side faces
		{
			BufferedImage face = faces[i];
			int y0 = (int) (face.getHeight() * 0.48);
			int y1 = (int) (face.getHeight() * 0.52);
			for (int y = y0; y < y1; y++)
			{
				for (int x = 0; x < face.getWidth(); x++)
				{
					int rgb = face.getRGB(x, y);
					r += rgb >> 16 & 0xFF;
					g += rgb >> 8 & 0xFF;
					b += rgb & 0xFF;
					n++;
				}
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
			if (atlas.getHeight() < face * 2)
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
		File custom = new File(new File(CUSTOM_DIR, name), file + ".png");
		try
		{
			if (custom.isFile())
			{
				return ImageIO.read(custom);
			}
			try (InputStream in = CubemapLoader.class.getResourceAsStream("skybox/" + name + "/" + file + ".png"))
			{
				return in == null ? null : ImageIO.read(in);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed to read cubemap image {}", custom, ex);
			return null;
		}
	}

	private static BufferedImage toIntArgb(BufferedImage src)
	{
		if (src.getType() == BufferedImage.TYPE_INT_ARGB)
		{
			return src;
		}
		BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		dst.getGraphics().drawImage(src, 0, 0, null);
		return dst;
	}
}
