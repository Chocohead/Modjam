package com.chocohead.cassette;

import java.io.IOException;
import java.nio.file.Paths;

public class CTest {
	public static void main(String[] args) throws IOException {
		Cassette.writeDirectory(Paths.get("Test.wav"), Paths.get(args[0]));

		Cassette.readCompletely(Paths.get("Test.wav"), reader -> {
			for (long i = 0, files = reader.readLong(); i < files; i++) {
				System.out.println(reader.readFile());
			}
		});
	}
}