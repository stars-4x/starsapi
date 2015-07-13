package org.starsautohost.starsapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author raptor
 *
 *	Various utility methods to supplement the Stars! decryption process
 */
public class Util {

	/**
	 * Convert unsigned byte to integer.  This is because Java doesn't have unsigned types
	 * and so a normal integer cast will preserve negative values
	 * 
	 * @param b
	 * @return
	 */
	public static int read8(byte b) {
		return b & 0xFF;
	}
	
	
	/**
	 * Read a 16 bit little endian integer from a byte array
	 * 
	 * @param data
	 * @param startIndex
	 * @return
	 */
	public static int read16(byte[] data, int offset) {
		return read8(data[offset+1]) << 8 | read8(data[offset]);
	}
	
	/**
     * Write a 16 bit little endian integer into a byte array
     * 
     * @param data
     * @param startIndex
     * @return
     */
    public static void write16(byte[] data, int offset, int value) {
        data[offset + 1] = (byte)((value >> 8) & 0xFF);
        data[offset] = (byte)(value & 0xFF);
    }

	
	/**
	 * Read a 32 bit little endian integer from a byte array
	 * 
	 * @param data
	 * @param startIndex
	 * @return
	 */
	public static long read32(byte[] data, int offset) {
		return read8(data[offset+3]) << 24 | 
				read8(data[offset+2]) << 16 | 
				read8(data[offset+1]) << 8 | 
				read8(data[offset]);
	}
	
    public static void write32(byte[] data, int offset, long value) {
        data[offset + 3] = (byte)((value >> 24) & 0xFF);
        data[offset + 2] = (byte)((value >> 16) & 0xFF);
        data[offset + 1] = (byte)((value >> 8) & 0xFF);
        data[offset] = (byte)(value & 0xFF);
    }

	public static long readN(byte[] data, int offset, int byteLen) {
	    if (byteLen == 0) return 0;
	    else if (byteLen == 1) return Util.read8(data[offset]);
	    else if (byteLen == 2) return Util.read16(data, offset);
	    else if (byteLen == 4) return Util.read32(data, offset);
	    else throw new IllegalArgumentException("Unexpected byteLen " + byteLen);
	}

    public static int writeN(byte[] data, int offset, long value) {
        if (value < 0) throw new IllegalArgumentException();
        else if (value == 0) return 0;
        else if (value < 256) {
            data[offset] = (byte)value;
            return 1;
        } else if (value < 65536) {
            Util.write16(data, offset, (int) value);
            return 2;
        } else {
            Util.write32(data, offset, value);
            return 4;
        }
    }

	
	/**
	 * For debugging!
	 * 
	 * @param bytes
	 * @param offset
	 * @param size
	 * @return
	 */
	public static String bytesToString(byte[] bytes, int offset, int size) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = offset; i < offset + size; i++) {
			sb.append(Util.read8(bytes[i]));
			sb.append(" ");
		}
		
		return sb.toString();
	}
	
	
	/**
	 * Reads in the given filename and returns a byte array of its contents
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public byte[] fileToBytes(String filename) throws IOException {
		File file = new File(filename);
		FileInputStream fileInputStream = new FileInputStream(file);
		
		byte[] bytes = new byte[(int) file.length()];
		
		fileInputStream.read(bytes);
		fileInputStream.close();
		
		return bytes;
	}
}
