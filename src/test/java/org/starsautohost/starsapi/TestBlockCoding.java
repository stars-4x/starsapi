package org.starsautohost.starsapi;

import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.encryption.Decryptor;

public class TestBlockCoding {

	public static void main(String[] args) throws Exception {

		// This is a .x file with a production queue change block 
		String hexChars = 
				"10204a334a33314e6b0b602a0100a0d00140112433f1044513bf612675ad0732"
				+ "f3ccb3aca2427437b4aa5a408947e2ed85eea601782cad8786032a95fb7ccae6"
				+ "63181e5be020eab3301fc5c036f5e9c3afe4936a3d0625b09f748ef373f920e2"
				+ "4c60a38e577be2d14f0000";
		
		// Block data includes blocks 8,9,29,0
		byte[] fileBytes = Util.hexToByteArray(hexChars.toUpperCase());
		

		List<Block> blocks = new Decryptor().readFileBytes(fileBytes);
		
		for(Block block: blocks) {
			verify(block);
		}
	}
	
	private static void verify(Block block) throws Exception {
		System.out.println("Verifying block: " + block.typeId);
		
		byte[] origData = Arrays.copyOf(block.getDecryptedData(), block.size);
		block.encode();
		byte[] newData = Arrays.copyOf(block.getDecryptedData(), block.size);
		
		if (!Arrays.equals(origData, newData)) {
			System.out.println("WARNING: Block was changed by decoding and encoding");
			System.out.println("OLD: " + Util.bytesToString(origData, 0, block.size));
			System.out.println("NEW: " + Util.bytesToString(newData, 0, block.size));
		}
	}
}
