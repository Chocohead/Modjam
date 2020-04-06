package com.chocohead.cassette;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.io.FileUtils;

import com.chocohead.cassette.CassetteReader.CassetteFile;

public class CRWTest {
	static final boolean TRUST_WRITER_SIZE = true;

	public static void main(String[] args) throws IOException {
		Path directory = Paths.get(args[0]);
		File file = new File(args[1]);
		Files.walk(directory).filter(Files::isRegularFile).forEach(path -> {
			AudioFormat format = new AudioFormat(44100, 8, 1, false, false);
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			String name = directory.relativize(path).toString().replace('\\', '/');
			try {
				CassetteWriter writer = new CassetteWriter(out, format.getSampleSizeInBits());
				writer.writeFile(name, path);

				long length = TRUST_WRITER_SIZE ? writer.getFinalSize() : AudioSystem.NOT_SPECIFIED;
				try (AudioInputStream audio = new AudioInputStream(new ByteArrayInputStream(out.toByteArray()), format, length)) {
					AudioSystem.write(audio, Type.WAVE, file);
				}
			} catch (IOException e) {
				throw new UncheckedIOException("Error writing " + name, e);
			}

			try (AudioInputStream in = AudioSystem.getAudioInputStream(file)) {
				CassetteReader reader = new CassetteReader(in);

				CassetteFile cassette = reader.readFile();
				//System.out.println(name + " -> " + cassette);
				assert name.equals(cassette.name): "Name corrupted writing " + name + ": \"" + cassette.name + '"';
				assert !cassette.negative: "Negativeness flipped writing " + name;
				assert file.length() == cassette.size: "Size changed writing " + name + ": \"" + cassette.size + '"';
				assert Arrays.equals(FileUtils.readFileToByteArray(file), cassette.contents): "File corrupted writing " + name;

				reader.assertDrained();
			} catch (UnsupportedAudioFileException e) {
				throw new AssertionError("Missing WAV reader?", e);
			} catch (IOException e) {
				throw new UncheckedIOException("Error reading " + name, e);
			} catch (Exception e) {
				throw new RuntimeException("Error reading " + name, e);
			}
		});

		System.out.println("Test passed");
	}
}