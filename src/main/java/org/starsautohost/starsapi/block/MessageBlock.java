package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class MessageBlock extends Block {

	int unknownWord0;  // Stays the same between sender and receiver, I think
	int unknownWord2;  
	
    // player number 0-15 (displayed as 1-16)
	int senderId;
	
	// 0 = everyone, otherwise its the player number +1 (1-16)
	int receiverId;
	
	// 3 - reply, 4 - normal message sent
	int unknownWord8;
	
	String message;

	public MessageBlock() {
		typeId = BlockType.MESSAGE;
	}

	@Override
	public void decode() {
		unknownWord0 = Util.read16(decryptedData, 0);
		unknownWord2 = Util.read16(decryptedData, 2);
		senderId   = Util.read16(decryptedData, 4);
		receiverId = Util.read16(decryptedData, 6);
		unknownWord8 = Util.read16(decryptedData, 8);
		
		// All the rest of the bytes are for the message
		byte[] messageData = Util.subArray(decryptedData, 10);
		message      = decodeStarsMessage(messageData);
	}

	@Override
	public void encode() {
		// Encode message first so we know how big the byte array will be
		byte[] messageData = encodeStarsMessage(message);
		
		// Allocate
        byte[] res = new byte[10 + messageData.length];

        // Write members
        Util.write16(res, 0, unknownWord0);
        Util.write16(res, 2, unknownWord2);
        Util.write16(res, 4, senderId);
        Util.write16(res, 6, receiverId);
        Util.write16(res, 8, unknownWord8);
        
        // Copy in message data
        System.arraycopy(messageData, 0, res, 10, messageData.length);
        
        // Save as decrypted data
        setDecryptedData(res, res.length);
	}
	
	@Override
	public String toString() {
		String s = "MessageBlock: ";
		s += unknownWord0 + " ";
		s += unknownWord2 + " ";
		s += senderId + " ";
		s += receiverId + " ";
		s += unknownWord8 + " ";
		
		s += message;
		s += "\n";
		
		return s;
	}
	


	/**
	 * Encode the given text into Stars! message format.
	 * 
	 * This format can use two encodings: Stars! text encoding or ASCII,
	 * depending on which is larger.
	 * 
	 * @param text
	 * @return
	 */
	public static byte[] encodeStarsMessage(String text) {
		// Encode string using Stars! encoding
		String hexChars = Util.encodeHexStarsString(text);

		// Require multiple of 2 bytes and append an 'F' to make it so
		if (hexChars.length() % 2 != 0) 
			hexChars = hexChars + "F";

		int starsEncByteLen = hexChars.length() / 2;

		// Header is the byte length as 10 bits
		int header = starsEncByteLen & 0x3ff;

		// Get ASCII encoding info
		byte[] textBytes = text.getBytes();
		int asciiByteLen = textBytes.length;  

		// If ASCII results in a shorter byte size than Stars! encoding, ASCII
		// will be used instead. Modify the header and hexChars accordingly
		if(asciiByteLen < starsEncByteLen) {  // Compare nibble counts
			// Header should really be the ASCII length
			header = asciiByteLen & 0x3ff;  // 10 bits

			// Now invert all bits which puts all 1's in the top 6 bits to tell
			// the decoder if ASCII was used
			header = ~(header) & 0xffff;  // 16 bits

			// Use ASCII chars instead
			hexChars = Util.byteArrayToHex(textBytes);
		}

		// Build header as hex nibbles, 2 bytes total
		String lowerByte = Util.byteToHex((byte) (header & 0xff));
		String upperByte = Util.byteToHex((byte) ((header & 0xff00) >> 8));

		// Prepend header to message, as 4 hex characters
		hexChars = (lowerByte + upperByte) + hexChars;

		// Debugging
		// Messages require a multiple of 4 bytes (8 nibbles) because of the
		// encryption. Append dummy values to test
//		int padLen = hexChars.length() % 8;
//		for(int i = 0; i < padLen; i++) 
//			// Stars! normally just leaves junk memory as padding at the end of
//			// the array. 
//			// A '6' adds 'n' (Stars!) or 'f' (ASCII). Useful for debugging
//			hexChars = hexChars.concat("6");  

		// Create byte array to hold header and message
		byte[] res = Util.hexToByteArray(hexChars);

		return res;
	}

	/**
	 * Decode a byte array from a Stars! message.
	 * 
	 * @param res
	 * @return
	 */
	public static String decodeStarsMessage(byte[] res) {
		// First 2 bytes contain message header
		int header = Util.read16(res, 0);

		// Lower 10 bits, this means 1023 max message bytesize!
		int byteSize = (header & 0x3ff);   
		int asciiIndicator = header >> 10; // Upper 6 bits

		// If the top 6 bits are all 1's, then the entire message is ASCII
		// encoded instead of Stars! encoded and the bytesize bits are inverted
		boolean useAscii = false;
		if(asciiIndicator == 0x3f) {
			useAscii = true;
			byteSize = (~byteSize & 0x3ff);
		}

		// Convert byte array to hex string, stripping off first 2 header bytes
		byte[] textBytes = Util.subArray(res, 2);
		String hexChars = Util.byteArrayToHex(textBytes);

		String decoded = "Error decoding message";
		if(useAscii)
			decoded = Util.decodeHexAscii(hexChars, byteSize);
		else
			decoded = Util.decodeHexStarsString(hexChars, byteSize);

		return decoded;
	}
}
