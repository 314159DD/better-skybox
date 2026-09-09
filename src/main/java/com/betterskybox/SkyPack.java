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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The sky pack: the 21 cubemap skies and the NASA star map, 35 MB of PNG that would not fit in a hub jar.
 * Turning the setting on downloads the release asset once and unpacks it into
 * {@link CubemapLoader#PACK_DIR}, where the loader looks first; turning it off leaves the files alone, so
 * removing the pack is deleting that folder.
 * <p>
 * One instance for the plugin ({@code @Singleton}). Every field is read and written on the client thread; the
 * download itself runs on RuneLite's scheduled executor and comes back through {@link ClientThread}.
 */
@Slf4j
@Singleton
class SkyPack
{
	/** The release asset, built by {@code tools/build_sky_pack.py}. */
	static final String PACK_URL =
		"https://github.com/314159DD/better-skybox/releases/download/sky-pack-v1/better-skybox-sky-pack.zip";

	@Inject
	private OkHttpClient httpClient;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ScheduledExecutorService executor;

	/** Whether the pack is on disk, read every frame by {@link SkyPass}, so the marker is not stat'ed per frame. */
	private boolean installed;
	/** One download at a time: a second toggle while this is set does nothing. */
	private boolean downloading;

	/** Reads the marker once. Call from {@link SkyPass#init} and never off the client thread. */
	void refresh()
	{
		installed = CubemapLoader.packInstalled();
	}

	boolean installed()
	{
		return installed;
	}

	/**
	 * The Download sky pack setting changed. Turning it on with the pack missing starts the one download;
	 * turning it off, or on again while it runs or after it is done, changes nothing.
	 *
	 * @param onInstalled run on the client thread once the pack is unpacked, so the skies get another attempt
	 */
	void onToggled(boolean on, Runnable onInstalled)
	{
		if (!on || installed || downloading)
		{
			return;
		}
		downloading = true;
		message("downloading the sky pack (about 35 MB), the skies come in when it lands");
		executor.execute(() -> download(onInstalled));
	}

	/** Executor thread: fetch, unpack, mark, and hand the result back to the client thread either way. */
	private void download(Runnable onInstalled)
	{
		try
		{
			File zip = File.createTempFile("better-skybox-sky-pack", ".zip");
			try
			{
				fetch(zip);
				unpack(zip);
			}
			finally
			{
				zip.delete();
			}
			// last, so a torn download cannot leave the pack looking complete
			Files.write(CubemapLoader.PACK_MARKER.toPath(), new byte[0]);
			log.info("Sky pack installed in {}", CubemapLoader.PACK_DIR);
			clientThread.invokeLater(() -> finished(onInstalled));
		}
		catch (IOException ex)
		{
			log.warn("Sky pack download failed", ex);
			clientThread.invokeLater(() -> failed(ex.toString()));
		}
	}

	private void fetch(File to) throws IOException
	{
		try (Response response = httpClient.newCall(new Request.Builder().url(PACK_URL).build()).execute())
		{
			if (!response.isSuccessful())
			{
				throw new IOException("HTTP " + response.code() + " from " + PACK_URL);
			}
			try (InputStream in = response.body().byteStream())
			{
				Files.copy(in, to.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}

	/**
	 * Unpacks into {@link CubemapLoader#PACK_DIR}. Every entry goes through {@link #packEntry} first and one
	 * that does not belong aborts the whole thing, so a doctored zip cannot write outside that folder.
	 */
	private static void unpack(File zip) throws IOException
	{
		try (ZipFile archive = new ZipFile(zip))
		{
			for (Enumeration<? extends ZipEntry> entries = archive.entries(); entries.hasMoreElements(); )
			{
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory())
				{
					continue;
				}
				if (!packEntry(entry.getName()))
				{
					throw new IOException("sky pack holds an entry it should not: " + entry.getName());
				}
				File out = new File(CubemapLoader.PACK_DIR, entry.getName());
				out.getParentFile().mkdirs();
				try (InputStream in = archive.getInputStream(entry))
				{
					Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	/**
	 * Whether a zip entry may be written: a .png or .json exactly one folder deep, {@code <sky>/px.png}. That
	 * rules out an absolute path, a Windows path, a drive letter and any number of {@code ..} segments, which
	 * is how an entry would otherwise land outside the pack folder.
	 */
	static boolean packEntry(String name)
	{
		if (name.indexOf('\\') >= 0 || name.indexOf(':') >= 0)
		{
			return false;
		}
		String[] parts = name.split("/", -1);
		if (parts.length != 2 || parts[0].isEmpty() || parts[0].equals(".") || parts[0].equals(".."))
		{
			return false;
		}
		return parts[1].endsWith(".png") || parts[1].endsWith(".json");
	}

	/** Client thread. */
	private void finished(Runnable onInstalled)
	{
		downloading = false;
		installed = true;
		message("sky pack installed, the cubemap skies and the real star map are available");
		onInstalled.run();
	}

	/** Client thread. */
	private void failed(String reason)
	{
		downloading = false;
		message("sky pack download failed: " + reason);
	}

	private void message(String text)
	{
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.CONSOLE)
			.value("Better Skybox: " + text)
			.build());
	}
}
