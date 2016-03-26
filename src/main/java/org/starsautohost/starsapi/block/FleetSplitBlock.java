package org.starsautohost.starsapi.block;

public class FleetSplitBlock extends Block {

	public int fleetNumber;
	
	public FleetSplitBlock() {
		typeId = BlockType.FLEET_SPLIT;
	}

	@Override
	public void decode() {
		fleetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 1) << 8);
		//System.out.println("Split: "+fleetNumber+" "+size); //+" "+toString());
	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}

}
