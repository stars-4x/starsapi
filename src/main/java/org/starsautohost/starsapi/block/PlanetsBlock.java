package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PlanetsBlock extends Block {
	
	public int planetsSize;

	
	public int planetsDataSize = 0;
	
	/**
	 * This holds possible other data after the block, like with the PLANETS
	 * block (probably the only case?)
	 */
	public byte[] planetsData;
	

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
	public void encode() throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString() {
		String s = super.toString();
		
		if(planetsDataSize > 0) {
			s += "-- Planets Data --\n";
			s += Util.bytesToString(planetsData, 0, planetsDataSize) + "\n";
		}
		
		return s;
	}
	
	public int getNumPlayers() {
	    return decryptedData[8];
	}

	public int getNumPlanets() {
	    return Util.read16(decryptedData, 10);
	}
}
