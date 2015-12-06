package org.starsautohost.starsapi.block;

import java.util.Arrays;

import org.starsautohost.starsapi.Util;

/**
 * This class will parse the header block and fill out all header data.
 * 
 * Yes.  I deliberately left out getters/setters. 
 */
public class FileHeaderBlock extends Block {
	
	// Header data
	public byte[] magicNumberData = new byte[] { 74, 51, 74, 51 } ;
	public String magicNumberString = "J3J3";
	
	public long gameId;
	public int versionData = 10848; // 2.6JRC4
	public int versionMajor = 2;
	public int versionMinor = 83;
	public int versionIncrement = 0;
	public int turn;
	public int playerNumber;  // zero-indexed
	public int encryptionSalt = 8; // just a number seen in a real file
	public int fileType;
	public byte unknownBits;
	public boolean turnSubmitted;
	public boolean hostUsing;
	public boolean multipleTurns;
	public boolean gameOver;
	public boolean shareware;

	
	public FileHeaderBlock() {
		typeId = BlockType.FILE_HEADER;
	}


	/**
	 * Parse the given block data according to rules found at:
	 *    http://wiki.starsautohost.org/wiki/FileHeaderBlock
	 *    
	 * Bytes are offset 2 as the type and size have already been parsed off.
	 * E.g. the magic number is bytes 0-3 
	 * @throws Exception 
	 */
	@Override
	public void decode() throws Exception {
		if(!hasData)
			throw new Exception("Cannot decode without data being set!");
		
		magicNumberData = Arrays.copyOfRange(data, 0, 4);
		magicNumberString = new String(magicNumberData);
		
		// Game id is 4 bytes (swapped)
		gameId = Util.read32(data, 4);
		
		// Version data block is two bytes (swapped)
		versionData = Util.read16(data, 8);
		versionMajor = versionData >> 12;         // First 4 bits
		versionMinor = (versionData >> 5) & 0x7F; // Middle 7 bits
		versionIncrement = versionData & 0x1F;    // Last 5 bits
		
		// Turn is next two bytes (swapped)
		turn = Util.read16(data, 10);
		
		// Player data next, 2 bytes swapped
		int playerData = Util.read16(data, 12);
		encryptionSalt = playerData >> 5;  // First 11 bits
		playerNumber = playerData & 0x1F;  // Last 5 bits
		
		// File type is next byte
		fileType = Util.read8(data[14]);
		
		// Flags use the last byte of the file header   The bits are used like so:
		//   UUU43210
		// Where 'U' is unused. and 43210 correspond to the bit shifts below
		int flags = Util.read8(data[15]);
		unknownBits = (byte)((flags >> 5) & 0x07);
		turnSubmitted = (flags & 1) > 0;
		hostUsing =     (flags & (1 << 1)) > 0;
		multipleTurns = (flags & (1 << 2)) > 0;
		gameOver =      (flags & (1 << 3)) > 0;
		shareware =     (flags & (1 << 4)) > 0;		
	}


	@Override
	public void encode() {
		byte[] data = new byte[16];
		System.arraycopy(magicNumberData, 0, data, 0, 4);
		Util.write32(data, 4, gameId);
		Util.write16(data, 8, versionData);
		Util.write16(data, 10, turn);
		Util.write16(data, 12, encryptionSalt << 5 | (playerNumber & 0x1F));
		data[14] = (byte)fileType;
		data[15] = (byte)(unknownBits << 5);
		if (turnSubmitted) data[15] |= 1;
		if (hostUsing) data[15] |= 2;
		if (multipleTurns) data[15] |= 4;
		if (gameOver) data[15] |= 8;
		if (shareware) data[15] |= 16;
        setDecryptedData(data, data.length);
        setData(data, data.length);
        encrypted = false;
	}

	public static FileHeaderBlock headerForHstFile(long gameId, int turn, byte unknownBits) {
	    FileHeaderBlock res = new FileHeaderBlock();
	    res.gameId = gameId;
	    res.turn = turn;
	    res.unknownBits = unknownBits;
	    res.playerNumber = 31;
	    res.fileType = 2;
	    res.encode();
	    return res;
	}

	public static FileHeaderBlock headerForMFile(long gameId, int turn, byte unknownBits, int playerNumber) {
	    FileHeaderBlock res = new FileHeaderBlock();
	    res.gameId = gameId;
	    res.turn = turn;
        res.unknownBits = unknownBits;
	    res.playerNumber = playerNumber;
	    res.fileType = 3;
	    res.encode();
	    return res;
	}

	public static FileHeaderBlock headerForHFile(long gameId, int turn, byte unknownBits, int playerNumber) {
	    FileHeaderBlock res = new FileHeaderBlock();
	    res.gameId = gameId;
	    res.turn = turn;
        res.unknownBits = unknownBits;
	    res.playerNumber = playerNumber;
	    res.fileType = 4;
	    res.encode();
	    return res;
	}

	@Override
	public String toString() {
		// This is about the most inefficient way to do this
		String s = super.toString();
		
		s += "FileHeaderBlock:\n";
		s += "Magic Number Data: " + Util.bytesToString(magicNumberData, 0, 4) + "\n";
		s += "Magic Number String: " + magicNumberString + "\n";
		s += "Game ID: " + Integer.toHexString((int)gameId) + "\n";
		s += "Version: " + versionMajor + "." + versionMinor + "." + versionIncrement + "\n"; 
		s += "Turn: " + turn + "; Year: " + (2400 + turn) + "\n";
		s += "Player Number: " + playerNumber + "; Displayed as: " + (playerNumber + 1) + "\n";
		s += "Encryption Salt: " + Integer.toHexString(encryptionSalt) + "\n";
		s += "File Type: " + fileType + "\n";
		s += "Flags:\n";
		s += "  Turn Submitted: " + turnSubmitted + "\n";
		s += "  File in use by Host: " + hostUsing + "\n";
		s += "  Multiple Turns in file: " + multipleTurns + "\n";
		s += "  GameOver: " + gameOver + "\n";
		s += "  Shareware: " + shareware + "\n";
		
		return s;
	}
}
