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
		message      = Util.decodeBytesForStarsMessage(messageData);
	}

	@Override
	public void encode() {
		// Encode message first so we know how big the byte array will be
		byte[] messageData = Util.encodeTextForStarsMessage(message);
		
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

}
