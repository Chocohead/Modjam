package com.chocohead.cassette;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Predicate;

public final class PathFilter implements PathMatcher, Predicate<Path> {
	public static PathFilter positive(String globPattern) {
        return positive(FileSystems.getDefault(), globPattern);
    }

	public static PathFilter positive(FileSystem fileSystem, String globPattern) {
        return new PathFilter(true, fileSystem, globPattern);
    }

	public static PathFilter negative(String globPattern) {
		return negative(FileSystems.getDefault(), globPattern);
    }

	public static PathFilter negative(FileSystem fileSystem, String globPattern) {
        return new PathFilter(false, fileSystem, globPattern);
    }

	private final PathMatcher matcher;
	private final boolean positive;
	private final String description;

	private PathFilter(boolean positive, FileSystem fileSystem, String globPattern) {
		this(fileSystem.getPathMatcher("glob:".concat(globPattern)), positive, (positive ? "in" : "out") + ':' + globPattern);
	}

	private PathFilter(PathMatcher matcher, boolean positive, String description) {
		this.matcher = matcher;
		this.positive = positive;
		this.description = description;
	}

	@Override
	public boolean matches(Path path) {
		return positive == matcher.matches(path);
	}

	@Override
	public boolean test(Path path) {
		return matches(path);
	}

	@Override
	public PathFilter and(Predicate<? super Path> other) {
		return new PathFilter(path -> positive == matcher.matches(path) && other.test(path), true, description + " && " + other.toString());
	}

	@Override
	public PathFilter or(Predicate<? super Path> other) {
		return new PathFilter(path -> positive == matcher.matches(path) || other.test(path), true, description + " || " + other.toString());
	}

	@Override
	public PathFilter negate() {
		return new PathFilter(matcher, !positive, "!(" + description + ')');
	}

	@Override
	public String toString() {
		return description;
	}
}