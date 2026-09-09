package com.gpuskybox;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import com.gpuskybox.template.Template;
import static org.lwjgl.opengl.GL33C.*;

/**
 * Draws a cubemap sky as a fullscreen pass before the scene.
 * Faces are looked up first in ~/.runelite/gpu-skybox/&lt;name&gt;/ and then in the bundled resources.
 */
@Slf4j
class SkyboxRenderer
{
	static final Shader PROGRAM = new Shader()
		.add(GL_VERTEX_SHADER, "sky_vert.glsl")
		.add(GL_FRAGMENT_SHADER, "sky_frag.glsl");

	private static final String[] FACES = {"px", "nx", "py", "ny", "pz", "nz"};
	private static final File CUSTOM_DIR = new File(RuneLite.RUNELITE_DIR, "gpu-skybox");
	static final int TEXTURE_UNIT = 2;

	private int program;
	private int texture;
	private int horizonColor;

	private int uniSkyProj;
	private int uniCubemap;
	private int uniFogColor;
	private int uniHorizonBlend;
	private int uniFogTint;
	private int uniBrightness;
	private int uniRotation;

	private final long startNanos = System.nanoTime();

	void initProgram(Template template) throws ShaderException
	{
		program = PROGRAM.compile(template);
		uniSkyProj = glGetUniformLocation(program, "skyProj");
		uniCubemap = glGetUniformLocation(program, "cubemap");
		uniFogColor = glGetUniformLocation(program, "fogColor");
		uniHorizonBlend = glGetUniformLocation(program, "horizonBlend");
		uniFogTint = glGetUniformLocation(program, "fogTint");
		uniBrightness = glGetUniformLocation(program, "brightness");
		uniRotation = glGetUniformLocation(program, "rotation");
	}

	void shutdownProgram()
	{
		glDeleteProgram(program);
		program = 0;
	}

	/**
	 * Uploads the cubemap selected in the config. On failure the previous texture is dropped and false is returned.
	 */
	boolean loadCubemap(GpuSkyboxConfig config)
	{
		String name = config.skyboxTexture() == GpuSkyboxConfig.SkyboxTexture.CUSTOM
			? config.skyboxCustomName().trim()
			: config.skyboxTexture().dir;
		freeTexture();
		if (name.isEmpty())
		{
			// the text field fires a config change per keystroke, so stay quiet here
			return false;
		}

		BufferedImage[] faces = loadFaces(name);
		if (faces == null)
		{
			return false;
		}

		horizonColor = averageHorizonColor(faces);

		texture = glGenTextures();
		glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, texture);
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

		log.info("Loaded skybox '{}' ({}x{} per face)", name, faces[0].getWidth(), faces[0].getHeight());
		return true;
	}

	void freeTexture()
	{
		if (texture != 0)
		{
			glDeleteTextures(texture);
			texture = 0;
		}
	}

	boolean isReady()
	{
		return program != 0 && texture != 0;
	}

	/**
	 * Average colour of the horizon band of the loaded cubemap, packed as 0xRRGGBB. Used as the fog colour so
	 * terrain fog and the sky fade meet in the same tone instead of the client's flat sky colour.
	 */
	int getHorizonColor()
	{
		return horizonColor;
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
	 * Expects the scene FBO to be bound and cleared. Leaves depth test, cull face and blend as they were.
	 *
	 * @param skyProj  projection * pitch * yaw, without camera translation
	 * @param quadVao  a VAO whose attribute 0 is a fullscreen quad in clip space (-1..1), drawn as a 4-vertex fan
	 */
	void draw(float[] skyProj, int sky, int quadVao, GpuSkyboxConfig config)
	{
		double elapsedMinutes = (System.nanoTime() - startNanos) / 60e9;
		float rotationDeg = (float) ((config.skyboxRotation() + elapsedMinutes * config.skyboxRotationSpeed()) % 360.0);

		glDisable(GL_DEPTH_TEST);
		glDepthMask(false);
		glDisable(GL_CULL_FACE);

		glUseProgram(program);
		glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT);
		glBindTexture(GL_TEXTURE_CUBE_MAP, texture);
		glUniform1i(uniCubemap, TEXTURE_UNIT);
		glUniformMatrix4fv(uniSkyProj, false, skyProj);
		glUniform4f(uniFogColor, (sky >> 16 & 0xFF) / 255f, (sky >> 8 & 0xFF) / 255f, (sky & 0xFF) / 255f, 1f);
		glUniform1f(uniHorizonBlend, config.skyboxHorizonBlend() / 100f);
		glUniform1f(uniFogTint, config.skyboxFogTint() / 100f);
		glUniform1f(uniBrightness, config.skyboxBrightness() / 100f);
		glUniform1f(uniRotation, (float) Math.toRadians(rotationDeg));

		glBindVertexArray(quadVao);
		glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
		glBindVertexArray(0);

		glActiveTexture(GL_TEXTURE0);
		glDepthMask(true);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_CULL_FACE);
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
				log.warn("Skybox '{}' atlas is {}x{}, expected 4x2 faces", name, atlas.getWidth(), atlas.getHeight());
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
				log.warn("Skybox '{}' is missing {}.png (and has no skybox.png atlas)", name, FACES[i]);
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
			try (InputStream in = SkyboxRenderer.class.getResourceAsStream("skybox/" + name + "/" + file + ".png"))
			{
				return in == null ? null : ImageIO.read(in);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed to read skybox image {}", custom, ex);
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
