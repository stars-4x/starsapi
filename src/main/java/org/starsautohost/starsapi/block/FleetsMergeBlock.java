package org.starsautohost.starsapi.block;

import java.util.Vector;

public class FleetsMergeBlock extends Block {

	public int fleetNumber;
	public Vector<Integer> fleetsToMerge = new Vector<Integer>();
	
	public FleetsMergeBlock() {
		typeId = BlockType.FLEETS_MERGE;
	}

	@Override
	public void decode() {
		fleetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 1) << 8);
		for (int t = 2; t < size; t += 2){
			int merge = (decryptedData[t] & 0xFF) + ((decryptedData[t+1] & 1) << 8);
			fleetsToMerge.addElement(merge);
		}
	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}

}
