package org.starsautohost.starsapi.block;

public class WaypointDeleteBlock extends WaypointChangeTaskBlock{

	public WaypointDeleteBlock() {
		typeId = BlockType.WAYPOINT_DELETE;
	}

	@Override
	public void decode() {
		//System.out.println(super.toStringOld());
		fleetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 1) << 8);
		wayPointNr = (decryptedData[2]&0xff);
	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}

}
