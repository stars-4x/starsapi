package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class WaypointChangeTaskBlock extends Block {

	public int fleetNumber;
	public int moveType; // 1 for normal move, 0 for move via diamond-select
	public int unknownByte3;
	public int x,y;
	public int target;
	public int warp;
	public int waypointTask;
	public int unknownBitsWithTargetType; //Hm, got "9" in 2503 on some orders. Check?
	public int targetType; //1-planet, 2-fleet, 4-deep space, 8-wormhole/trader/minefield
	public int subTaskIndex; //Is byte[12] if exists
	
	public WaypointChangeTaskBlock() {
		typeId = BlockType.WAYPOINT_CHANGE_TASK;
	}

	/**
	 * Seems to be:
	 * Fleet nr for fleets
	 * Planet nr for planets
	 * 1 if targetType 8
	 * 511 if targetType 4 and wormhole/trader exists?
	 */
	public int getTarget(){
		return target;
	}
	
	@Override
	public void decode() throws Exception{
		fleetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 1) << 8);
		warp = (decryptedData[10]&0xff) >> 4;
		waypointTask = (decryptedData[10]&0xff) % 16;
		x = Util.read16(decryptedData, 4);
		y = Util.read16(decryptedData, 6);
		target = (decryptedData[8] & 0xFF) + ((decryptedData[9] & 1) << 8);
		moveType = (decryptedData[2]&0xff);
		unknownByte3 = (decryptedData[3]&0xff);
		targetType = (decryptedData[11]&0xff) % 16;
		unknownBitsWithTargetType = (decryptedData[11]&0xff) >> 4;
		if (size > 12) subTaskIndex = decryptedData[12]&0xff;
	}

	public String debug(){
		String s = typeId==BlockType.WAYPOINT_CHANGE_TASK?"WPC#":"WPA#";
		s += fleetNumber+": "+x+","+y+" MoveType "+moveType+" Warp "+warp+" Target "+target+" Type "+targetType;
		s += " Task "+waypointTask+" TT "+subTaskIndex;
		s += " ("+unknownByte3+" "+unknownBitsWithTargetType+")";
		return s;
	}
	
	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}

}
