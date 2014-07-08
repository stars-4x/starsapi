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
	public byte[] magicNumberData;
	public String magicNumberString;
	
	public long gameId;
	public int versionMajor;
	public int versionMinor;
	public int versionIncrement;
	public int turn;
	public int year;
	public int playerNumber;  // zero-indexed
	public int encryptionSalt;
	public int fileType;
	
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
		int versionData = Util.read16(data, 8);
		versionMajor = versionData >> 12;         // First 4 bits
		versionMinor = (versionData >> 5) & 0x7F; // Middle 7 bits
		versionIncrement = versionData & 0x1F;    // Last 5 bits
		
		// Turn is next two bytes (swapped)
		turn = Util.read16(data, 10);
		year = 2400 + turn;
		
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
		turnSubmitted = (flags & 1) > 0;
		hostUsing =     (flags & (1 << 1)) > 0;
		multipleTurns = (flags & (1 << 2)) > 0;
		gameOver =      (flags & (1 << 3)) > 0;
		shareware =     (flags & (1 << 4)) > 0;		
	}


	@Override
	public byte[] encode() throws Exception {
		return null;
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
		s += "Turn: " + turn + "; Year: " + year + "\n";
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
