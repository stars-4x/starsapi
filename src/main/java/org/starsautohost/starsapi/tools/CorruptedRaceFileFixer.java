package org.starsautohost.starsapi.tools;

import java.util.ArrayList;
import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.FileFooterBlock;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class CorruptedRaceFileFixer {

	public static void main(String[] args) throws Exception {
		// PUT FULL FILE NAME AND PATH HERE
		String fullpath = "/path/to/corrupted/race/file.r1";

		// First verify the file is corrupt
		Decryptor decryptor = new Decryptor();
		List<Block> blocks = decryptor.readFile(fullpath);

		FileHeaderBlock block8 = (FileHeaderBlock) blocks.get(0);  // header
		PlayerBlock     block6 = (PlayerBlock)     blocks.get(1);  // player
		FileFooterBlock block0 = (FileFooterBlock) blocks.get(2);  // footer

		int fileChecksum = block0.checksum;
		int calcChecksum = Util.checkSumRaceFile(block6.raceFileStructData);

		System.out.println("== Checksums ==");
		System.out.println("From file:\t" + hexConvert(fileChecksum));
		System.out.println("Calculated:\t" + hexConvert(calcChecksum));

		// Verify
		if(fileChecksum == calcChecksum) {
			System.out.println("Race file does not appear corrupted.");
			return;
		}

		// Uh oh, problem. Show some stats
		System.out.println("== Corrupted! ==");
		checksumDiff(fileChecksum, calcChecksum);

		// Fix file by replacing footer with calculated checksum
		FileFooterBlock block0new = new FileFooterBlock();
		block0new.checksum = calcChecksum;
		block0new.encode();

		// Add back to a list
		List<Block> newBlocks = new ArrayList<Block>();
		newBlocks.add(block8);
		newBlocks.add(block6);
		newBlocks.add(block0new);  // New footer!

		// And write out to our new file
		String[] tokens = fullpath.split("\\.(?=[^\\.]+$)");
		String basename = tokens[0];
		String extension = tokens[1];

		String newFullpath = basename + "_fixed." + extension;
		decryptor.writeBlocks(newFullpath, newBlocks, false);
		
		System.out.println("== New file ==");
		System.out.println("Name:\n   " + newFullpath);
	}


	public static void checksumDiff(int checksum1, int checksum2) {
		int diff = checksum1 ^ checksum2;
		int byte1 = (diff >> 8 ) & 0xff;  // 16 bit LE
		int byte2 = (diff & 0xff);
		String c1 = byte1 == 0 ? "" : Character.toString((char)byte1);
		String c2 = byte2 == 0 ? "" : Character.toString((char)byte2);
		System.out.println("Difference:\t" + hexConvert(diff) + 
				", dec: " + diff + ", bytes: " + byte1 + " (" + c1 + ") " +
				byte2 + " (" + c2 + ") ");
	}


	public static String hexConvert(int num)
	{
		return "0x" + Integer.toHexString(num & 0xFFFF);
	}	

}
