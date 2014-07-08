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
