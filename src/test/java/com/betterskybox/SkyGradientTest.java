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

import com.google.gson.Gson;
import java.io.IOException;
import java.util.regex.Pattern;
import com.betterskybox.ProceduralSkyRenderer.Gradient;
import com.betterskybox.ProceduralSkyRenderer.Keyframe;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

/** The bundled sky_gradient.json parses, its ramps are well formed and evaluate() reads them as intended. */
public class SkyGradientTest
{
	private static final Pattern RGB = Pattern.compile("#[0-9a-fA-F]{6}");

	private static Gradient gradient() throws IOException
	{
		return ProceduralSkyRenderer.loadGradient(new Gson());
	}

	private static Keyframe[][] ramps(Gradient g)
	{
		return new Keyframe[][]{g.zenith, g.horizon, g.sunGlow};
	}

	private static float[] rgb(String hex)
	{
		int packed = Integer.parseInt(hex.substring(1), 16);
		return new float[]{(packed >> 16 & 0xFF) / 255f, (packed >> 8 & 0xFF) / 255f, (packed & 0xFF) / 255f};
	}

	private static float[] evaluate(Keyframe[] keys, float altitude)
	{
		float[] out = new float[3];
		ProceduralSkyRenderer.evaluate(keys, altitude, out);
		return out;
	}

	@Test
	public void everyRampRisesStrictlyThroughRgbHexColours() throws IOException
	{
		for (Keyframe[] keys : ramps(gradient()))
		{
			assertTrue(keys.length >= 2);
			for (int i = 0; i < keys.length; i++)
			{
				assertTrue(keys[i].color, RGB.matcher(keys[i].color).matches());
				if (i > 0)
				{
					assertTrue(keys[i].altitude + " after " + keys[i - 1].altitude, keys[i].altitude > keys[i - 1].altitude);
				}
			}
		}
	}

	@Test
	public void evaluateClampsPastBothEnds() throws IOException
	{
		for (Keyframe[] keys : ramps(gradient()))
		{
			Keyframe first = keys[0];
			Keyframe last = keys[keys.length - 1];
			assertArrayEquals(rgb(first.color), evaluate(keys, first.altitude - 100), 1e-6f);
			assertArrayEquals(rgb(last.color), evaluate(keys, last.altitude + 100), 1e-6f);
		}
	}

	@Test
	public void evaluateHitsAKeyExactly() throws IOException
	{
		for (Keyframe[] keys : ramps(gradient()))
		{
			Keyframe key = keys[keys.length / 2];
			assertArrayEquals(rgb(key.color), evaluate(keys, key.altitude), 1e-6f);
		}
	}

	@Test
	public void evaluateInterpolatesHalfWayBetweenTwoKeys() throws IOException
	{
		for (Keyframe[] keys : ramps(gradient()))
		{
			float[] a = rgb(keys[0].color);
			float[] b = rgb(keys[1].color);
			float[] expected = {(a[0] + b[0]) / 2, (a[1] + b[1]) / 2, (a[2] + b[2]) / 2};
			assertArrayEquals(expected, evaluate(keys, (keys[0].altitude + keys[1].altitude) / 2), 1e-6f);
		}
	}
}
