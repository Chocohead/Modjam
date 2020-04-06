package com.chocohead.cassette;

import java.util.function.Predicate;

final class Constants {
	public static final byte[] SIZE_TABLE = {1, 2, 4, 8, 16, 32, 48, 64};
	public static final char ETX = 0b11; //ETX = End of text
	public static final char NAK = 0b10101; //NAK = Negative acknowledgement

	public static <T> Predicate<T> alwaysTrue() {
		return thing -> true;
	}
}