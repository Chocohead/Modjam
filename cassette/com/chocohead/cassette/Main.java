package com.chocohead.cassette;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.io.IOUtils;

import com.chocohead.cassette.Cassette.Builder;
import com.chocohead.cassette.CassetteReader.CassetteFile;

public final class Main {
	public static void main(String[] args) throws IOException {
		if (args == null || args.length == 0 || args[1].isEmpty()) {
			args = new String[] {"help"};
		}

		switch (args[0].toLowerCase(Locale.ENGLISH)) {
		case "write":
			write(args.length < 2 ? new String[] {"help"} : Arrays.copyOfRange(args, 1, args.length));
			break;

		case "contents":
			contents(args.length < 2 ? new String[] {"help"} : Arrays.copyOfRange(args, 1, args.length));
			break;

		case "extract":
			extract(args.length < 2 ? new String[] {"help"} : Arrays.copyOfRange(args, 1, args.length));
			break;

		default:
			System.err.println("Unknown command: \"" + args[0] + '"');
		case "help":
			System.out.println("Available commands:");
			System.out.println("\twrite - Write a cassette");
			System.out.println("\tcontents - Logs the contents of a cassette");
			System.out.println("\textract - Open a cassette");
			System.out.println("\thelp - Print this message");
			break;
		}
	}

	private static void write(String... args) throws IOException {
		Builder cassetteBuilder = new Builder();
		Path destination = null;
		boolean safely = false;
		Path currentDirectory = null;
		boolean negative = false;
		PathFilter filter = null;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase(Locale.ENGLISH)) {
			case "-t":
			case "to":
				if (++i >= args.length) {
					System.err.println("Destination flag specified without path");
					return;
				} else if (destination != null) {
					System.err.println("Multiple destinations provided");
					return;
				} else {
					destination = Paths.get(args[i].endsWith(".wav") ? args[i] : args[i].concat(".wav"));

					if (safely && Files.exists(destination)) {
						System.out.println("Aborting as destination \"" + destination + "\" already exists");
						return;
					}

					continue;
				}

			case "-s":
			case "safely":
				if (destination != null && Files.exists(destination)) {
					System.out.println("Aborting as destination \"" + destination + "\" already exists");
					return;
				} else {
					safely = true;
					continue;
				}

			case "-d":
			case "directory":
				if (++i >= args.length) {
					System.err.println("Directory flag specified without directory");
					return;
				} else {
					if (currentDirectory != null) {
						if (!negative) {
							if (filter != null) {
								cassetteBuilder.addDirectory(currentDirectory, filter);
								filter = null;
							} else {
								cassetteBuilder.addDirectory(currentDirectory);
							}
						} else {
							if (filter != null) {
								cassetteBuilder.addNegativeDirectory(currentDirectory, filter);
								filter = null;
							} else {
								cassetteBuilder.addNegativeDirectory(currentDirectory);
							}
						}
					}

					currentDirectory = Paths.get(args[i]);
					negative = false;
					assert filter == null;

					if (!Files.isDirectory(currentDirectory)) {
						System.err.println("Given directory \"" + currentDirectory + "\" is not a directory");
						return;
					}

					continue;
				}

			case "-nd":
			case "negdirectory":
				if (++i >= args.length) {
					System.err.println("Negative directory flag specified without directory");
					return;
				} else {
					if (currentDirectory != null) {
						if (!negative) {
							if (filter != null) {
								cassetteBuilder.addDirectory(currentDirectory, filter);
								filter = null;
							} else {
								cassetteBuilder.addDirectory(currentDirectory);
							}
						} else {
							if (filter != null) {
								cassetteBuilder.addNegativeDirectory(currentDirectory, filter);
								filter = null;
							} else {
								cassetteBuilder.addNegativeDirectory(currentDirectory);
							}
						}
					}

					currentDirectory = Paths.get(args[i]);
					negative = true;
					assert filter == null;

					if (!Files.isDirectory(currentDirectory)) {
						System.err.println("Given directory \"" + currentDirectory + "\" is not a directory");
						return;
					}

					continue;
				}

			case "-f":
			case "filter":
				if (currentDirectory == null) {
					System.err.println("Filter specified before directory");
					return;
				} else if (++i >= args.length) {
					System.err.println("Filter flag specified without filter");
					return;
				} else {
					PathFilter newFilter = PathFilter.positive(args[i]);
					filter = filter == null ? newFilter : filter.and(newFilter);
					continue;
				}

			case "-nf":
			case "negfilter":
				if (currentDirectory == null) {
					System.err.println("Negative filter specified before directory");
					return;
				} else if (++i >= args.length) {
					System.err.println("Negative filter flag specified without filter");
					return;
				} else {
					PathFilter newFilter = PathFilter.negative(args[i]);
					filter = filter == null ? newFilter : filter.and(newFilter);
					continue;
				}

			default:
				System.err.println("Unknown write flag: \"" + args[i] + '"');
			case "-h":
			case "help":
				System.out.println("Available flags:");
				System.out.println("\t[-t, to] <path> - Write to the given file name");
				System.out.println("\t[-s, safely] - Avoid replacing the destination file if it already exists");
				System.out.println("\t[-d, directory] <dir> - Add the given directory as somewhere to copy from");
				System.out.println("\t[-nd, negdirectory] <dir> - Add the given directory as somewhere to copy from negatively");
				System.out.println("\t[-f, filter] <filter> - Filter to apply to the (current) directory's contents");
				System.out.println("\t[-nf, negfilter] <filter> - Negative filter to apply to the (current) directory's contents");
				System.out.println("\t[-h, help] - Print this message");
				return;
			}
		}

		if (destination == null) {
			System.err.println("No output file supplied!");
			return;
		}

		if (currentDirectory != null) {
			if (!negative) {
				if (filter != null) {
					cassetteBuilder.addDirectory(currentDirectory, filter);
				} else {
					cassetteBuilder.addDirectory(currentDirectory);
				}
			} else {
				if (filter != null) {
					cassetteBuilder.addNegativeDirectory(currentDirectory, filter);
				} else {
					cassetteBuilder.addNegativeDirectory(currentDirectory);
				}
			}
		}

		cassetteBuilder.copy(destination).write();
	}

	private static void contents(String... args) throws IOException {
		Path origin = null;
		boolean viewNegatives = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase(Locale.ENGLISH)) {
			case "-f":
			case "from":
				if (++i >= args.length) {
					System.err.println("Origin flag specified without path");
					return;
				} else if (origin != null) {
					System.err.println("Multiple origin files provided");
					return;
				} else {
					origin = Paths.get(args[i]);

					if (Files.notExists(origin)) {
						System.err.println("Origin file \"" + origin + "\" does not exist");
						return;
					}

					continue;
				}

			case "-n":
			case "negs":
				viewNegatives = true;
				continue;

			default:
				System.err.println("Unknown extract flag: \"" + args[i] + '"');
			case "-h":
			case "help":
				System.out.println("Available flags:");
				System.out.println("\t[-f, from] <path> - Cassette file to view contents of");
				System.out.println("\t[-n, negs] - Log negative files");
				System.out.println("\t[-h, help] - Print this message");
				return;
			}
		}

		final boolean doViewNegatives = viewNegatives;
		Cassette.readCompletely(origin, reader -> {
			for (long i = 0, files = reader.readLong(); i < files; i++) {
				CassetteFile file = reader.scanFile();
				if (file.negative && !doViewNegatives) continue; //Don't need this

				System.out.print(file.name + "\t[" + file.size + " bytes]");
				if (file.negative) System.out.print(" (n)");
				System.out.println();
			}
		});
	}

	private static void extract(String... args) throws IOException {
		Path origin = null;
		Path destination = null;
		boolean clearing = false;
		boolean keepNegatives = false;

		for (int i = 0; i < args.length; i++) {
			switch (args[i].toLowerCase(Locale.ENGLISH)) {
			case "-f":
			case "from":
				if (++i >= args.length) {
					System.err.println("Origin flag specified without path");
					return;
				} else if (origin != null) {
					System.err.println("Multiple origin files provided");
					return;
				} else {
					origin = Paths.get(args[i]);

					if (Files.notExists(origin)) {
						System.err.println("Origin file \"" + origin + "\" does not exist");
						return;
					}

					continue;
				}

			case "-t":
			case "to":
				if (++i >= args.length) {
					System.err.println("Destination flag specified without directory");
					return;
				} else if (destination != null) {
					System.err.println("Multiple destinations provided");
					return;
				} else {
					destination = Paths.get(args[i]);

					if (Files.exists(destination) && !Files.isDirectory(destination)) {
						System.err.println("Output path \"" + destination + "\" is not a directory");
						return;
					}

					continue;
				}

			case "-c":
			case "clear":
				clearing = true;
				continue;

			case "-kn":
			case "keepnegs":
				keepNegatives = true;
				continue;

			default:
				System.err.println("Unknown extract flag: \"" + args[i] + '"');
			case "-h":
			case "help":
				System.out.println("Available flags:");
				System.out.println("\t[-f, from] <path> - Cassette file to extract");
				System.out.println("\t[-t, to] <dir> - Directory to extract into");
				System.out.println("\t[-c, clear] - Clear the output directory if it is not empty");
				System.out.println("\t[-kn, keepnegs] - Keep negative files");
				System.out.println("\t[-h, help] - Print this message");
				return;
			}
		}

		if (clearing && Files.exists(destination)) {
			Files.walkFileTree(destination, new SimpleFileVisitor<Path>() {
				private FileVisitResult visit(Path path) {
					try {
						Files.delete(path);
						return FileVisitResult.CONTINUE;
					} catch (IOException e) {
						throw new UncheckedIOException("Unable to clear file in output directory when requested: " + path, e);
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					return visit(file);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
					return visit(dir);
				}
			});
		} else if (Files.notExists(destination)) {
			Files.createDirectories(destination);
		}

		final Path solidDestination = destination;
		final boolean doKeepNegatives = keepNegatives;
		Cassette.readCompletely(origin, reader -> {
			for (long i = 0, files = reader.readLong(); i < files; i++) {
				CassetteFile file = reader.readFile();
				if (file.negative && !doKeepNegatives) continue; //Don't need this

				Path extractionTarget = solidDestination.resolve(file.name);
				Files.createDirectories(extractionTarget.getParent());
				try (OutputStream out = Files.newOutputStream(extractionTarget)) {
					if (file.size > Short.MAX_VALUE) {
						IOUtils.writeChunked(file.contents, out); //We'll be good and buffer it
					} else {
						out.write(file.contents); //Small enough that it makes no difference
					}
				} catch (Exception e) {
					System.err.println("Error extracting " + file.name + " to " + extractionTarget);
					e.printStackTrace();
				}
			}
		});
	}
}