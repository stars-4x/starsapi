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
	 * 
	 * @param b
	 * @return
	 */
	public static int ubyteToInt(byte b) {
		return b & 0xFF;
	}
	
	
	/**
	 * For debugging!
	 * 
	 * @param bytes
	 * @param startIndex
	 * @param size
	 * @return
	 */
	public static String bytesToString(byte[] bytes, int startIndex, int size) {
		StringBuilder sb = new StringBuilder();
		
		for(int i = startIndex; i < startIndex + size; i++) {
			sb.append(Util.ubyteToInt(bytes[i]));
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
