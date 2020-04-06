package com.chocohead.cassette;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

class CassetteWriter {
	private final OutputStream out;
	private final int bitsPerFrame;
	private long framesWritten, bitsWritten;
	private byte bitQueue, queueHead = 7;

	public CassetteWriter(OutputStream out, int bitsPerFrame) {
		this.out = out;
		this.bitsPerFrame = bitsPerFrame;
	}

	public void writeHeader() {
		//TODO
	}

	private static byte computeUnpack(long number) {
		int bitsUsed = Long.SIZE - Long.numberOfLeadingZeros(number);

		for (byte i = 0; i < Constants.SIZE_TABLE.length; i++) {
			if (bitsUsed <= Constants.SIZE_TABLE[i]) return i;
		}

		//This would require having more digits than what a long can store, which isn't possible
		throw new IllegalStateException("Long value " + number + " couldn't fit in 64 bits?");
	}

	public void writeLong(long value) throws IOException {
		byte leadingBits = computeUnpack(value);
		writeBit((leadingBits & 0b100) >>> 2);
		writeBit((leadingBits & 0b010) >>> 1);
		writeBit(leadingBits & 0b001);

		writeLong(value, Constants.SIZE_TABLE[leadingBits]);
	}

	public void writeFile(String name, Path file) throws IOException {
		writeFile(name, file, false);
	}

	public void writeFile(String fileName, Path file, boolean negative) throws IOException {
		assert fileName.equals(fileName.replace('\\', '/')); //No accidents
		long fileSize = Files.size(file);

		if (negative) fileName += Constants.NAK;
		writeString(fileName);
		//System.out.println("Name ends at " + (framesWritten + bitsWritten / 8) + " - " + bitsWritten % 8);

		writeLong(fileSize);
		//System.out.println("Size ends at " + (framesWritten + bitsWritten / 8) + " - " + bitsWritten % 8);
		drainToByte();

		//System.out.println("Blob byte starts at " + (framesWritten + bitsWritten / 8));
		try (InputStream in = Files.newInputStream(file)) {
			bitsWritten = Math.addExact(bitsWritten, Math.multiplyExact(IOUtils.copyLarge(in, out), 8));
		}

		calculateFrames();
	}

	private void writeBit(int bit) throws IOException {
		assert bit == 1 || bit == 0;
		bitQueue |= bit << queueHead;
		bitsWritten++;

		if (queueHead == 0) {
			out.write(bitQueue);
			queueHead = 7;
			bitQueue = 0;
		} else {
			queueHead--;
		}
	}

	private void writeLong(long value, byte bits) throws IOException {
		/*while (--bits >= 0) {//Not especially quick, but it gets the job done
			writeBit((int) (value >> bits) & 0b1);
		}*/
		writeLong(value, bits, (byte) 0);
	}

	private void writeLong(long value, byte bits, byte offset) throws IOException {
		assert 0 <= bits && bits <= 64;
		assert 0 <= offset && offset <= 64 - bits;
		byte shift = (byte) (bits + offset); //This won't overflow

		while (queueHead < 7 && --shift >= 0) {
		//for (int head = queueHead; --shift >= 0 && head < 7; head++) {
			writeBit((int) (value >>> shift) & 0b1);
		}

		while (shift >= 8) {
			out.write((int) (value >>> (shift -= Byte.SIZE)));
			bitsWritten += 8;
		}

		while (--shift >= 0) {
			writeBit((int) (value >>> shift) & 0b1);
		}
	}

	public void writeString(String value) throws IOException {
		byte[] contents = value.getBytes(StandardCharsets.US_ASCII);
		assert value.equals(new String(contents, StandardCharsets.US_ASCII));

		if (ArrayUtils.contains(contents, (byte) Constants.ETX)) {
			throw new IllegalArgumentException("Written string " + value + " contains end marker");
		}

		contents = Arrays.copyOf(contents, contents.length + 1);
		contents[contents.length - 1] = Constants.ETX;

		//The contents get written as 7 bits, so the 64 bits of a long gives room for 9
		for (int pos = 0, longs = contents.length / 9; pos < longs; pos++) {
			long bits = 0;

			for (int i = 8 * 7, index = pos * 9; i >= 0; i -= 7, index++) {
				assert 0 <= index && index < contents.length;
				//System.out.print("Writing " + (value + Constants.ETX).charAt(index));
				bits |= (long) (contents[index] & 0x7F) << i;
				//System.out.println(", bits now " + Long.toBinaryString(bits));
			}

			writeLong(bits, (byte) 63);
		}

		//Will be between 0 and 8 (inclusively) so will not overflow
		byte remainingWidth = (byte) (contents.length % 9);

		if (remainingWidth > 0) {
			assert 0 < remainingWidth && remainingWidth < 9;
			long bits = 0;

			for (int index = contents.length - 1, shift = 0; shift < remainingWidth; shift++, index--) {
				assert 0 <= index && index < contents.length;
				//System.out.print("Writing " + (value + Constants.ETX).charAt(index));
				bits |= (long) (contents[index] & 0x7F) << shift * 7;
				//System.out.println(", bits now " + Long.toBinaryString(bits));
			}

			writeLong(bits, (byte) (remainingWidth * 7)); //remainingWidth is < 9 so this will be <= 56
		}
	}

	private void drainToByte() throws IOException {
		/*for (int head = queueHead; head < 7; head++) {
			writeBit(0); //Could just do while (queueHead != 7) but this is safer
		}*/
		while (queueHead != 7) writeBit(0);

		calculateFrames();
	}

	private void calculateFrames() {
		framesWritten = Math.addExact(framesWritten, bitsWritten / bitsPerFrame);

		bitsWritten %= bitsPerFrame;
		assert 0 <= bitsWritten && bitsWritten < bitsPerFrame;
	}

	public long getFinalSize() throws IOException {
		calculateFrames();

		if (bitsWritten != 0) {//Ensure we don't write a partial final frame
			assert bitsPerFrame <= Byte.MAX_VALUE; //So long as this is true we won't overflow
			writeLong(0, (byte) (bitsPerFrame - bitsWritten));

			calculateFrames();
		}

		assert bitsWritten == 0;
		return framesWritten;
	}
}