package com.chocohead.cassette;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public final class Cassette {
	@FunctionalInterface
	private interface CassetteWriterConsumer {
		void accept(CassetteWriter writer) throws IOException;
	}

	public static class Builder {
		private final Path to;
		private final Set<Entry<String, Path>> paths = new LinkedHashSet<>();
		private final Set<Entry<String, Path>> negativePaths = new LinkedHashSet<>();

		Builder() {
			to = null;
		}

		public Builder(Path to) {
			this.to = to;
		}

		public Builder copy(Path to) {
			Builder clone = new Builder(to);

			clone.paths.addAll(paths);
			clone.negativePaths.addAll(negativePaths);

			return clone;
		}

		public Builder addDirectory(Path directory) throws IOException {
			return addDirectory(directory, Constants.alwaysTrue());
		}

		public Builder addDirectory(Path directory, Predicate<Path> filter) throws IOException {
			addDirectory(paths, directory, filter);
			return this;
		}

		public Builder addNegativeDirectory(Path directory) throws IOException {
			return addNegativeDirectory(directory, Constants.alwaysTrue());
		}

		public Builder addNegativeDirectory(Path directory, Predicate<Path> filter) throws IOException {
			addDirectory(negativePaths, directory, filter);
			return this;
		}

		private static void addDirectory(Set<Entry<String, Path>> to, Path directory, Predicate<Path> filter) throws IOException {
			Files.walk(directory).filter(Files::isRegularFile).filter(filter).map(relativiser(directory)).forEach(to::add);
		}

		public void write() throws IOException {
			Cassette.write(to, writer -> {
				writer.writeLong(paths.size() + negativePaths.size());
				for (Entry<String, Path> path : paths) {
					System.out.println("Writing " + path.getKey());
					writer.writeFile(path.getKey(), path.getValue());
				}
				for (Entry<String, Path> path : negativePaths) {
					System.out.println("Writing " + path.getKey());
					writer.writeFile(path.getKey(), path.getValue(), true);
				}
			});
		}
	}

	public static void writeDirectory(Path to, Path directory) throws IOException {
		writeDirectory(to, directory, Constants.alwaysTrue());
	}

	@SuppressWarnings("unchecked") //It is what it is
	public static void writeDirectory(Path to, Path directory, Predicate<Path> filter) throws IOException {
		write(to, Files.walk(directory).filter(Files::isRegularFile).filter(filter).map(relativiser(directory)).toArray(Entry[]::new));
	}

	public static void writeDirectories(Path to, Path... directories) throws IOException {
		writeDirectories(to, Constants.alwaysTrue(), directories);
	}

	@SuppressWarnings("unchecked")
	public static void writeDirectories(Path to, Predicate<Path> filter, Path... directories) throws IOException {
		Entry<String, Path>[] paths;
		try {
			paths = Arrays.stream(directories).flatMap(directory -> {
				try {
					return Files.walk(directory).filter(Files::isRegularFile).filter(filter).map(relativiser(directory));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).toArray(Entry[]::new);
		} catch (UncheckedIOException e) {
			throw e.getCause(); //Don't let exceptions get stuck in the lambdas
		}

		write(to, paths);
	}

	static Function<Path, Entry<String, Path>> relativiser(Path directory) {
		return path -> new SimpleImmutableEntry<>(directory.relativize(path).toString().replace('\\', '/'), path);
	}

	@SafeVarargs
	private static void write(Path to, Entry<String, Path>... from) throws IOException {
		write(to, writer -> {
			writer.writeLong(from.length);
			for (Entry<String, Path> path : from) {
				System.out.println("Writing " + path.getKey());
				writer.writeFile(path.getKey(), path.getValue());
			}
		});
	}

	static void write(Path to, CassetteWriterConsumer writerFiller) throws IOException {
		if (to == null) throw new IllegalArgumentException("Cannot write to null file!");
		AudioFormat format = new AudioFormat(44100, 8, 1, false, false);
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		CassetteWriter writer = new CassetteWriter(out, format.getSampleSizeInBits());
		writerFiller.accept(writer);

		try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(out.toByteArray()), format, writer.getFinalSize())) {
			AudioSystem.write(audio, Type.WAVE, to.toFile());
		}
	}


	@FunctionalInterface
	public interface CassetteReaderConsumer {
		void accept(CassetteReader reader) throws IOException;
	}

	public static void readCompletely(Path origin, CassetteReaderConsumer readerReader) throws IOException {
		try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(origin)))) {
			CassetteReader reader = new CassetteReader(in);

			readerReader.accept(reader);

			reader.assertDrained();
		} catch (UnsupportedAudioFileException e) {
			throw new AssertionError("Missing WAV reader?", e);
		}
	}
}