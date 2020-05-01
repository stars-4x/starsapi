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
	public static byte[] fileToBytes(String filename) throws IOException {
		File file = new File(filename);
		FileInputStream fileInputStream = new FileInputStream(file);
		
		byte[] bytes = new byte[(int) file.length()];
		
		fileInputStream.read(bytes);
		fileInputStream.close();
		
		return bytes;
	}
	
	// Hex values representing 4-bit nibbles
	private static char[] hexDigits = "0123456789ABCDEF".toCharArray(); 

	// All of these characters are found in the stars26jrc4 binary at offset 
	// 000B:DD8A
	private static String encodesOneNibble = " aehilnorst";  // 0-A indexed
	private static String encodesB = "ABCDEFGHIJKLMNOP";
	private static String encodesC = "QRSTUVWXYZ012345";
	private static String encodesD = "6789bcdfgjkmpquv";
	private static String encodesE = "wxyz+-,!.?:;'*%$";

	/**
	 * Encodes a String of text using Stars! text encoding and return the hex-
	 * encoded String
	 * 
	 * @param text
	 * @return
	 */
	public static String encodeHexStarsString(String text) {
		StringBuilder hexChars = new StringBuilder();

		// Loop through each hex char; should be all capitalized
		for (int i = 0; i < text.length(); i++) {
			char thisChar = text.charAt(i);

			// Check for bad-value...
			if (thisChar > 255) 
				thisChar = '?';

			// If this character is one that only will be encoded with 1 nibble
			int index = encodesOneNibble.indexOf(thisChar);
			if (index >= 0) {
				hexChars.append(hexDigits[index]);
				continue;
			}
			else {
				index = encodesB.indexOf(thisChar);
				if (index >= 0) {
					hexChars.append('B');
					hexChars.append(hexDigits[index]);
					continue;
				}
				else {
					index = encodesC.indexOf(thisChar);
					if (index >= 0) {
						hexChars.append('C');
						hexChars.append(hexDigits[index]);
						continue;
					}
					else {
						index = encodesD.indexOf(thisChar);
						if (index >= 0) {
							hexChars.append('D');
							hexChars.append(hexDigits[index]);
							continue;
						}
						else {
							index = encodesE.indexOf(thisChar);
							if (index >= 0) {
								hexChars.append('E');
								hexChars.append(hexDigits[index]);
								continue;
							}
							// Otherwise, 3-nibble encoded
							else {
								hexChars.append('F');
								// Just encode the ASCII char, but swap the nibbles
								hexChars.append(hexDigits[(thisChar & 0x0F)]);
								hexChars.append(hexDigits[(thisChar & 0xF0) >> 4]);
							}
						}
					}
				}
			}
		}

		return hexChars.toString();
	}

	/**
	 * Encode the given text as a Stars! encoded string
	 * 
	 * @param s
	 * @return
	 */
	public static byte[] encodeStarsString(String s) {
		String hexChars = encodeHexStarsString(s);

		// Require multiple of 2 bytes and append an 'F' to make it so
		if (hexChars.length() % 2 != 0) 
			hexChars = hexChars + "F";

		// Convert byte size to a hex string
		String byteSizeHex = byteToHex((byte) (hexChars.length()/2));

		// Add the byte size as a header to the data
		hexChars = byteSizeHex + hexChars;

		byte[] res =  hexToByteArray(hexChars);

		return res;
	}

	/**
	 * Decode a sequence of hex characters that are ASCII encoded
	 * 
	 * @param hexChars
	 * @param byteSize
	 * @return
	 */
	public static String decodeHexAscii(String hexChars, int byteSize) {
		StringBuffer result = new StringBuffer();

		// Keep track of what byte we're at for certain checks
		int atByteIndex = -1;

		// Loop through each hex character and decode the text
		for (int t = 0; t < 2*byteSize; t+=2) {  // Skip every 2 nibbles
			// Every 2 nibbles is the start of a new byte
			atByteIndex = t / 2;  // Integer division expected

			char thisNibble = hexChars.charAt(t);
			char nextNibble = hexChars.charAt(t+1);

			// The encoded text is the direct ASCII value of the swapped
			// nibbles
			int parsed = Integer.parseInt("" + thisNibble + nextNibble, 16);
			char theChar = (char) (parsed & 0xff);

			result.append(theChar);

			// We've already hit the last byte, ignore the rest (junk memory)
			if (atByteIndex >= byteSize - 1)
				break;
		}

		return result.toString();
	}

	/**
	 * Decode a sequence of hex characters containing Stars-encoded text
	 *  
	 * @param hexChars
	 * @return
	 */
	public static String decodeHexStarsString(String hexChars, int byteSize)
	{
		StringBuffer result = new StringBuffer();

		// Keep track of what byte we're at for certain checks
		int atByteIndex = -1;

		// Loop through each hex character and decode the text depending on
		// what the hex value is. 1 Nibble (4 bits) is represented by one char
		//	    for (int t = 0; t < hexChars.length(); t++) {
		for (int t = 0; t < 2*byteSize; t++) {
			// Every 2 nibbles is the start of a new byte
			atByteIndex = t / 2;  // Integer division expected

			char thisNibble = hexChars.charAt(t);

			// 0-A is 1-Nibble (4-bits) encoded text
			if (thisNibble <= 'A') {  // ASCII math FTW
				int charIndex = Integer.parseInt(""+thisNibble, 16);
				// This nibble is just an index in a char array
				result.append(encodesOneNibble.charAt(charIndex));
			}

			// Three-nibble encoded text starts with an 'F'
			else if (thisNibble == 'F') {
				// We've already hit the last byte, no decodeable 3-nibble
				// chars are left (probably just junk remaining)
				if (atByteIndex >= byteSize - 1)
					continue;

				char nextNibble = hexChars.charAt(t + 1);
				char nextNextNibble = hexChars.charAt(t + 2);

				// The encoded text is the direct ASCII value of the swapped
				// nibbles
				int parsed = Integer.parseInt("" + nextNextNibble + nextNibble, 16);
				char theChar = (char) (parsed & 0xff);

				result.append(theChar);

				// Advance passed the two characters we decoded
				t += 2;
			}

			// Otherwise, the next hex value is B,C,D, or E, and text is
			// 2-nibble encoded
			else
			{
				char nextNibble = hexChars.charAt(t+1);
				int charIndex = Integer.parseInt(""+nextNibble, 16);

				if (thisNibble == 'B')
					result.append(encodesB.charAt(charIndex));
				else if (thisNibble == 'C')
					result.append(encodesC.charAt(charIndex));
				else if (thisNibble == 'D')
					result.append(encodesD.charAt(charIndex));
				else if (thisNibble == 'E')
					result.append(encodesE.charAt(charIndex));

				// Advance passed the character we decoded
				t++;
			}
		}

		return result.toString();
	}

	/**
	 * Decode a byte array containing Stars-encoded text.
	 * 
	 * This is the general case for most (all?) text encoded byte arrays, other
	 * than messages between users.
	 * 
	 * @param res
	 * @return
	 */
	public static String decodeStarsString(byte[] res) {
		// First byte is string byte size
		int byteSize = res[0];

		// Convert byte array to hex string, stripping off first byte
		byte[] textBytes = subArray(res, 1);
		String hexChars = byteArrayToHex(textBytes);

		// Decode hex string into original text
		String decoded = decodeHexStarsString(hexChars, byteSize);

		return decoded;
	}

	/**
	 * Convert a byte to its 2-nibble hexadecimal representation
	 * 
	 * @param b
	 * @return
	 */
	public static String byteToHex(byte b) {
		int i = (b & 0xff);  // Java has only signed types - force to int

		// Convert
		String hex = Integer.toHexString(i).toUpperCase();

		// Zero pad to 2 chars
		if(hex.length() == 1)
			hex = "0" + hex;

		return hex;
	}

	/**
	 * Convert a byte array into its hexadecimal representation
	 * 
	 * @param bytes
	 * @return
	 */
	public static String byteArrayToHex(byte[] bytes) {
		StringBuilder hexChars = new StringBuilder();

		// Convert each byte to its equivalent hex value
		for (int i = 0; i < bytes.length; i++) {
			String hex = byteToHex(bytes[i]);
			hexChars.append(hex);
		}

		return hexChars.toString();
	}

	/**
	 * Convert a string of hexadecimal characters to a byte equivalents. Every
	 * two hex chars are one byte.
	 *  
	 * @param hexChars
	 * @return
	 */
	public static byte[] hexToByteArray(String hexChars) {
		// Create byte array to hold header and message
		byte[] res = new byte[(hexChars.length() / 2)];

		// Convert hex characters to byte array
		for (int i = 0; i < res.length; i++) { // Jump every 2
			char firstChar = hexChars.charAt(2*i);
			char secondChar = hexChars.charAt(2*i+1);

			// Convert the two hex characters into its byte representation
			byte b = (byte)((charToNibble(firstChar) << 4) | (charToNibble(secondChar)));

			// Save it to the array
			res[i] = b;
		}

		return res;
	}

	/** 
	 * Convert a hex character to its equivalent ASCII character representation;
	 * expects capitalized ASCII chars
	 * @param ch
	 * @return
	 */
	private static byte charToNibble(char ch) {
		// Convert ASCII Hex char to number
		if (ch >= '0' && ch <= '9') 
			return (byte)(ch - '0');

		if (ch >= 'A' && ch <= 'F') 
			return (byte)(ch - 'A' + 10);

		throw new IllegalArgumentException();
	}

	/**
	 * Returns a sub array of the given input using start and end indexes
	 * 
	 * @param input
	 * @param startIdx
	 * @param endIdx
	 * @return
	 */
	public static byte[] subArray(byte[] input, int startIdx, int endIdx) {
		int size = endIdx - startIdx + 1;

		byte[] output = new byte[size];
		System.arraycopy(input, startIdx, output, 0, size);

		return output;
	}

	/**
	 * Returns a sub array starting at the given index until the end of the 
	 * input array
	 * 
	 * @param input
	 * @param startIdx
	 * @return
	 */
	public static byte[] subArray(byte[] input, int startIdx) {
		return subArray(input, startIdx, input.length-1);
	}
}
