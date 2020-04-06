package com.chocohead.cassette;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;

public class CassetteReader {
	public static class CassetteFile {
		public final String name;
		public final boolean negative;
		public final long size;
		public final byte[] contents; //Don't modify me please :)

		CassetteFile(String name, long size, byte[] contents) {
			this(name, false, size, contents);
		}

		CassetteFile(String name, boolean negative, long size, byte[] contents) {
			this.name = name;
			this.negative = negative;
			this.size = size;
			this.contents = contents;
		}

		@Override
		public String toString() {
			return "CassetteFile<"  + (negative ? '[' + name + ']' : name) + ", " + size + " bytes>";
		}
	}

	private static final int BUFFER_SIZE = 50000;//4096;
	private final InputStream in;
	private final byte[] buffer = new byte[BUFFER_SIZE];
	//private long offset; //Number of frames already into the file
	private int bufferHead, bufferTail;

	CassetteReader(AudioInputStream audio) {
		in = audio;
	}

	public long readLong() throws IOException {
		short leadingBits = readShort(3);
		return readLong(Constants.SIZE_TABLE[leadingBits]);
	}

	public CassetteFile readFile() throws IOException {
		String name = readString();

		boolean negative = !name.isEmpty() && name.charAt(name.length() - 1) == Constants.NAK;
		if (negative) name = name.substring(0, name.length() - 1);

		long size = readLong();
		//System.out.println("Size reading finished " + (offset + bufferHead / 8) + " - " + bufferHead % 8);
		skipEndByte();

		byte[] contents = new byte[Math.toIntExact(size)];
		readBlob(contents);

		return new CassetteFile(name, negative, size, contents);
	}

	public CassetteFile scanFile() throws IOException {
		String name = readString();

		boolean negative = !name.isEmpty() && name.charAt(name.length() - 1) == Constants.NAK;
		if (negative) name = name.substring(0, name.length() - 1);

		long size = readLong();
		skipEndByte();
		skipBytes(size);

		return new CassetteFile(name, negative, size, null);
	}

	private void refillBuffer() throws IOException {
		if (bufferHead == bufferTail) {
			bufferHead = 0;

			//offset += bufferTail / 8;
			bufferTail = Math.multiplyExact(in.read(buffer), 8);
			assert bufferTail != 0;
			if (bufferTail < 0) throw new EOFException("No more data left in file");
		}

		assert bufferHead <= bufferTail;
	}

	private byte readBit() throws IOException {
		refillBuffer();

		/*byte readByte = buffer[bufferHead / 8];
		int shift = 7 - bufferHead++ % 8;
		int bit = readByte >>> shift;
		byte out = (byte) (bit & 0b1);
		return out;*/

		//A byte being right shifted is never going to stop being a byte
		return (byte) (buffer[bufferHead / 8] >>> 7 - bufferHead++ % 8 & 0b1);
	}

	private short readShort(int bits) throws IOException {
		assert bits <= Short.SIZE;
		short value = 0;

		for (int bit = bits - 1; bit >= 0; bit--) {
			value |= readBit() << bit;
		}

		return value;
	}

	private long readLong(int bits) throws IOException {
		if (bits <= Short.SIZE) return readShort(bits) & 0xFFFF;
		long value = 0;

		while (bufferHead % 8 != 0) {
			value |= (long) readBit() << --bits;
		}

		while (bits >= 8) {
			refillBuffer();
			value |= (long) (buffer[bufferHead / 8] & 0xFF) << (bits -= 8);
			bufferHead += 8;
		}

		while (--bits >= 0) {
			value |= readBit() << bits;
		}

		return value;
	}

	private char readChar() throws IOException {
		char value = 0;

		for (int i = 6; i >= 0; i--) {
			value |= readBit() << i;
		}

		return value;
	}

	public String readString() throws IOException {
		StringBuilder builder = new StringBuilder();

		char last;
		do {
			last = readChar();
			builder.append(last);
		} while (last != Constants.ETX);

		//System.out.println("String reading finished " + (offset + bufferHead / 8) + " - " + bufferHead % 8);
		return builder.substring(0, builder.length() - 1);
	}

	private void skipEndByte() {
		if (bufferHead % 8 != 0) {
			bufferHead += 8 - bufferHead % 8;
			assert bufferHead <= bufferTail;
		}
	}

	private void skipBytes(long bytes) throws IOException {
		if (bufferHead % 8 != 0) throw new IllegalStateException("Floating midbyte!");

		while (bytes > 0) {
			try {
				refillBuffer();
			} catch (EOFException e) {
				System.err.println("Ran out of WAV to skip wanting " + bytes + " bytes more");
				throw e;
			}

			assert (bufferTail - bufferHead) % 8 == 0;
			long toSkip = Math.min((bufferTail - bufferHead) / 8, bytes);
			bufferHead += toSkip * 8;
			bytes -= toSkip;
		}
	}

	private void readBlob(byte[] to) throws IOException {
		if (bufferHead % 8 != 0) throw new IllegalStateException("Floating midbyte!");
		assert to.length > 0;

		//System.out.println("Blob byte reading at " + (offset + bufferHead / 8));
		int remaining = to.length;
		do {
			try {
				refillBuffer();
			} catch (EOFException e) {
				System.err.println("Ran out of WAV with " + remaining + " bytes left");
				throw e;
			}

			assert (bufferTail - bufferHead) % 8 == 0;
			int toRead = Math.min((bufferTail - bufferHead) / 8, remaining);
			System.arraycopy(buffer, bufferHead / 8, to, to.length - remaining, toRead);
			bufferHead += toRead * 8;
			remaining -= toRead;
		} while (remaining > 0);
	}

	public void assertDrained() throws IOException {
		if (bufferHead == bufferTail) {
			try {
				refillBuffer();
			} catch (EOFException e) {
				return;
			}
		} else return;

		throw new AssertionError("Expected stream to be finished yet there was at least " + (bufferTail - bufferHead) / 8 + " bytes left!");
	}
}