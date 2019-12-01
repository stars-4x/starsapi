package org.starsautohost.starsapi;

public class TestTextEncoding {

	/**
	 * Text can be encoded in two ways:
	 * 
	 * 1. As a Stars! String
	 * 2. As a message, in two formats:
	 *    a. encoded using Stars! encoding
	 *    b. encoded using ASCII
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Simple
		runTest("abcdefABCDEF1234567890");
		
		// Default 2-nibble chars or less
		String defaultChars = " aehilnorstABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789bcdfgjkmpquvwxyz+-,!.?:;'*%$";
		runTest(defaultChars);
		
		// Everything I could type
		String all = "`1234567890-=~!@#$%^&*()_+\n" +
				"qwertyuiop[]\\QWERTYUIOP{}|\n" +
				"asdfghjkl;'ASDFGHJKL:\"\n" +
				"zxcvbnm,./ZXCVBNM<>?\n" +
				" \t\n" + // SPACE and TAB
				"";
		runTest(all);
		
		// Tests to verify half-byte operation using known sequences that are 1-nibble encoded
		String a = "a";
		runTest(a);
		String eee = "eee";
		runTest(eee);
		String iiiii = "iiiii";
		runTest(iiiii);
		String ooooooo = "ooooooo";
		runTest(ooooooo);
		String rrrrrrrrr = "rrrrrrrrr";
		runTest(rrrrrrrrr);
		
		// Force ASCII encoding by using characters known to be worse in Stars! encoding
		String ascii = "~!@#$%^&*()_+";
		runTest(ascii);
		
		// Do length test with ASCII
		String longMessageAscii = "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789\r\n"
				+ "01234567890123456789012345678901234567890123456789"
				;  // This should be 518 bytes and use 10 header bits
		System.out.println("Original:   " + longMessageAscii);
		runMessageTest(longMessageAscii);
		
		// Do length test with Stars! encoding
		String longMessage = "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				+ "aeio01234567890123456789012345678901234567890123456789"
				;  // This should be 520 bytes and use 10 header bits
		System.out.println("Original:   " + longMessage);
		runMessageTest(longMessage);
	}
	
	
	// Run both tests
	private static void runTest(String theString) {
		System.out.println("Original:   " + theString);
		
		runStringTest(theString);
		runMessageTest(theString);
	}
	
	private static void runMessageTest(String theString) {
		// Run test as though it was a message
		byte[] encoded = Util.encodeTextForStarsMessage(theString);
		String decoded = Util.decodeBytesForStarsMessage(encoded);
		
		if(decoded.equals(theString))
			System.out.println("PASSED msg");
		else
			System.out.println("FAILED msg: " + decoded);
	}
	
	private static void runStringTest(String theString) {
		// Run encode-decode test as though it was a general Stars! string
		byte[] encoded = Util.encodeTextForStarsString(theString);
		String decoded = Util.decodeBytesForStarsString(encoded);
		
		if(decoded.equals(theString))
			System.out.println("PASSED str");
		else
			System.out.println("FAILED str: " + decoded);
	}
	
}
