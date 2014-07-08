package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PlanetsBlock extends Block {
	
	public int planetsSize;

	public PlanetsBlock() {
		typeId = BlockType.PLANETS;
	}

	@Override
	public void decode() throws Exception {
		if(!hasDecryptedData)
			throw new Exception("Cannot decode without decrypted data being set!");

		// Planet size is determined by swapping bytes 10 and 11
		// and concatenating their bits
		planetsSize = Util.read16(decryptedData, 10);
	}

	@Override
	public byte[] encode() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

}
