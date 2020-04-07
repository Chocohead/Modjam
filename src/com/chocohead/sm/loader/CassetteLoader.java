package com.chocohead.sm.loader;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Permission;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.cassette.Cassette;
import com.chocohead.cassette.CassetteReader.CassetteFile;
import com.chocohead.mm.api.ClassTinkerers;

@PreMixinClassloaded
class CassetteLoader {
	@PreMixinClassloaded
	private static class CassetteSlot extends URLStreamHandler {
		private static final boolean DEBUG = Boolean.getBoolean("chocohead.sm.slot.debug");
		private final Map<String, byte[]> tracks;

		public static URL engauge(String host, Map<String, byte[]> holes) {
			try {
				return new URL("salts_mill", host, -1, "/", new CassetteSlot(holes));
			} catch (MalformedURLException e) {
				throw new RuntimeException("Problem enguaging track head", e);
			}
		}

		public CassetteSlot(Map<String, byte[]> tracks) {
			if (DEBUG) for (String name : tracks.keySet()) PreLoader.LOGGER.info("Know of " + name);
			this.tracks = tracks;
		}

		@Override
		protected URLConnection openConnection(URL url) throws IOException {
			byte[] track = tracks.get(url.getPath());
			if (DEBUG) PreLoader.LOGGER.info((track != null ? "Succeeded" : "Tried") + " to load from " + url.getPath() + " (part of " + url + ')');
			return track != null ? new URLConnection(url) {
				@Override
				public Permission getPermission() throws IOException {
					return null;
				}

				@Override
				public void connect() throws IOException {
					throw new UnsupportedOperationException();
				}

				@Override
				public InputStream getInputStream() throws IOException {
					return new ByteArrayInputStream(track);
				}
			} : null;
		}
	}

	public static void loadCassettes(ResourceLoader loader) {
		Path dir = FabricLoader.getInstance().getGameDirectory().toPath().resolve("cassettes");
		PreLoader.LOGGER.debug("Loading cassettes from {}", dir);

		if (!Files.exists(dir)) {
			try {
				Files.createDirectory(dir);
			} catch (IOException e) {
				throw new RuntimeException("Could not create cassette slot at " + dir, e);
			}
		}

		if (!Files.isDirectory(dir)) {
			throw new RuntimeException("Cassette slot at " + dir + " is not a directory!");
		}

		Path[] wavs;
		try {
			wavs = Files.walk(dir, 1).filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".wav")).toArray(Path[]::new);
		} catch (IOException e) {
			throw new RuntimeException("Exception while searching for mods in '" + dir + "'!", e);
		}
		PreLoader.LOGGER.debug("Found {} cassettes", wavs.length);

		for (Path wav : wavs) {
			PreLoader.LOGGER.debug("Loading {}", wav);
			Builder<String, byte[]> tracks = ImmutableMap.builder();

			try {
				Cassette.readCompletely(wav, reader -> {
					for (long i = 0, files = reader.readLong(); i < files; i++) {
						CassetteFile file = reader.readFile();
						if (file.negative) continue; //Not today thank you

						tracks.put('/' + file.name, file.contents);
					}
				});
			} catch (EOFException e) {//If the file runs short early it's probably not intact
				throw new UncheckedIOException("Error reading " + wav + ", likely a corrupt download (ie try download it again)", e);
			} catch (IOException e) {//Otherwise it's just some other general reading problem
				throw new UncheckedIOException("Error reading " + wav, e);
			}

			PreLoader.LOGGER.debug("Successfully loaded {}, adding to classpath", wav);
			Map<String, byte[]> cassette = tracks.build();

			boolean success = ClassTinkerers.addURL(CassetteSlot.engauge(wav.getFileName().toString(), cassette));
			if (!success) throw new AssertionError("Failed to insert cassette!"); //A most terrible problem

			loader.giveFiles(cassette);
		}

		loader.complete();
	}
}