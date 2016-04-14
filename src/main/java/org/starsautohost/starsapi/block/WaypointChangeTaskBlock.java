package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class WaypointChangeTaskBlock extends Waypoint {

	public int fleetNumber;
	public int wayPointNr; // 0 for move via diamond-select, 1 for first waypoint, 2 for next etc 
	public int unknownByte3;
	//public int x,y; //Now defined in Waypoint
	public int target; //See getTarget()
	//public int warp; //Now defined in Waypoint
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
		wayPointNr = (decryptedData[2]&0xff);
		unknownByte3 = (decryptedData[3]&0xff);
		targetType = (decryptedData[11]&0xff) % 16;
		unknownBitsWithTargetType = (decryptedData[11]&0xff) >> 4;
		if (size > 12) subTaskIndex = decryptedData[12]&0xff;
	}

	@Override
	public void encode() {
		byte[] data = new byte[subTaskIndex>0?13:12];
		Util.write16(data, 0, fleetNumber);
		data[2] = (byte)wayPointNr;
		data[3] = (byte)unknownByte3;
		Util.write16(data, 4, x);
	    Util.write16(data, 6, y);
	    Util.write16(data, 8, target); //This must be checked
	    data[10] = (byte)((warp << 4) | waypointTask);
	    data[11] = (byte)((unknownBitsWithTargetType << 4) | targetType);
	    if (data.length > 12) data[12] = (byte)subTaskIndex;
	    setDecryptedData(data, data.length);
        setData(data, data.length);
        size = data.length;
        encrypted = false;

	}

	
	public String debug(){
		String s = typeId==BlockType.WAYPOINT_CHANGE_TASK?"WPC#":"WPA#";
		s += fleetNumber+": "+x+","+y+" MoveType "+wayPointNr+" Warp "+warp+" Target "+target+" Type "+targetType;
		s += " Task "+waypointTask+" TT "+subTaskIndex;
		s += " ("+unknownByte3+" "+unknownBitsWithTargetType+")";
		return s;
	}
		
	public String toString(){
		if (typeId == BlockType.WAYPOINT_DELETE) return "MOVE DELETE "+wayPointNr+": #"+(fleetNumber+1);
		String type = typeId == BlockType.WAYPOINT_ADD ? "ADD" : "CHANGE";		
		return "MOVE "+type+" "+wayPointNr+": #"+(fleetNumber+1)+" to "+x+","+y;
	}
}
