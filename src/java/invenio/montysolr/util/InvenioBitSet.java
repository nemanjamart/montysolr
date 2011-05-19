package invenio.montysolr.util;

import java.io.*;
import java.util.BitSet;
import java.util.Arrays;
import java.util.zip.*;

/**
 * This file was developed by Jan for Invenio, taken from
 * http://invenio-software
 * .org/cgi-bin/cgit/personal/invenio-jan/tree/modules/miscutil
 * /lib/InvenioBitSet.java?h=lucene-connect
 *
 * @author Jan Iwaszkiewicz
 * @author rca - added fastLoad and fastDump
 *
 */

public class InvenioBitSet extends BitSet {

	private static final long serialVersionUID = 1L;

	public InvenioBitSet() {
		super();
	}

	public ByteArrayOutputStream fastDump() throws IOException {
		// Encode a String into bytes
		byte[] input = this.toByteArray();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		Deflater compressor = new Deflater();
		compressor.setInput(this.toByteArray());
		compressor.finish();
		while (!compressor.finished()) {
			byte[] out = new byte[input.length];
			int len = compressor.deflate(out);
			bout.write(out);
		}
		return bout;
	}

	public static InvenioBitSet fastLoad(String input) throws DataFormatException {
		byte[] bytes = input.getBytes();
		return InvenioBitSet.fastLoad(bytes);
	}

	public static InvenioBitSet fastLoad(byte[] input) throws DataFormatException {
		InvenioBitSet bitSet = new InvenioBitSet();

		// Decompress the bytes
		Inflater decompresser = new Inflater();
		decompresser.setInput(input);
		byte[] result = new byte[input.length];
		decompresser.inflate(result);
		decompresser.end();

		int i = 0;
		for (byte b : result) {
			for (int mask = 0x01; mask != 0x100; mask <<= 1) {
				boolean value = (b & mask) != 0;
				if (value) {
					bitSet.set(i);
				}
				i += 1;
			}
		}

		return bitSet;
	}

	// helper function for debugging
	public static String getHexString(byte[] b) throws Exception {
		String result = "";
		for (int i = 0; i < b.length; i++) {
			result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

	public InvenioBitSet(int nbits) {
		super(nbits);
	}

	// TODO: remove the trailing 8 bytes (added by intbitset format) and test
	public InvenioBitSet(byte[] bytes) {
		this(bytes == null ? 0 : bytes.length * 8);
		for (int i = 0; i < size(); i++) {
			if (isBitOn(i, bytes))
				set(i);
		}
	}

	// convert to a byte array to be used as intbitset in Invenio
	public byte[] toByteArray() {

		if (size() == 0)
			return new byte[0];

		// Find highest bit
		int hiBit = -1;
		for (int i = 0; i < size(); i++) {
			if (get(i))
				hiBit = i;
		}

		// was: int n = (hiBit + 8) / 8;
		// +128 (64 for trailing zeros used in intbitset and 64 to avoid
		// trancating)
		int n = ((hiBit + 128) / 64) * 8;
		byte[] bytes = new byte[n];
		if (n == 0)
			return bytes;

		Arrays.fill(bytes, (byte) 0);
		for (int i = 0; i < n * 8; i++) {
			if (get(i))
				setBit(i, bytes);
		}
		return bytes;
	}

	protected static int BIT_MASK[] =
	// {0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01};
	{ 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80 };

	protected static boolean isBitOn(int bit, byte[] bytes) {
		int size = bytes == null ? 0 : bytes.length * 8;
		if (bit >= size)
			return false;
		return (bytes[bit / 8] & BIT_MASK[bit % 8]) != 0;
	}

	protected static void setBit(int bit, byte[] bytes) {
		int size = bytes == null ? 0 : bytes.length * 8;
		if (bit >= size)
			throw new ArrayIndexOutOfBoundsException("Byte array too small");
		bytes[bit / 8] |= BIT_MASK[bit % 8];
	}

	public static void main(String[] args) throws DataFormatException, IOException {

		int min = 0;
		int max = 5000;
		InvenioBitSet ibs = new InvenioBitSet(max);
		for (int i = 0; i < 500; i++) {
			int r = min + (int) (Math.random() * ((max - min) + 1));
			ibs.set(r);
		}
		ByteArrayOutputStream b = ibs.fastDump();
		InvenioBitSet bs;
		bs = InvenioBitSet.fastLoad(b.toByteArray());
		System.out.println("set lengths: " + ibs.cardinality() + " - " + bs.cardinality());
		System.out.println("Set equals? " + ibs.equals(bs));

	}
}
