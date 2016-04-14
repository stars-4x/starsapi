package org.starsautohost.starsapi.block;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.starsautohost.starsapi.Util;

public class WaypointBlock extends Waypoint{
    //public int x; //Now defined in Waypoint
    //public int y; //Now defined in Waypoint
    public int positionObject;
    //public int warp; //Now defined in Waypoint
    public int waypointTask; //1-Transport ... 9 - Transfer (will have additional bytes if > 0, usually 10 bytes)
    public int positionObjectType;
    public Vector<Byte> additionalBytes = new Vector<Byte>();
    
	public WaypointBlock() {
		typeId = BlockType.WAYPOINT;
	}

	@Override
	public void decode() {
	    x = Util.read16(decryptedData, 0);
        y = Util.read16(decryptedData, 2);
        positionObject = Util.read16(decryptedData, 4);
        warp = (decryptedData[6] & 0xFF) >> 4;
        waypointTask = decryptedData[6] & 0x0F;
        positionObjectType = decryptedData[7] & 0xFF;
        for (int t = 8; t < decryptedData.length; t++){
        	additionalBytes.add(decryptedData[t]);
        }
	}

	@Override
	public void encode() {
	    byte[] data = new byte[8+additionalBytes.size()];
	    Util.write16(data, 0, x);
	    Util.write16(data, 2, y);
	    Util.write16(data, 4, positionObject);
	    data[6] = (byte)((warp << 4) | waypointTask);
	    data[7] = (byte)positionObjectType;
	    for (int t = 8; t < data.length; t++){
	    	data[t] = additionalBytes.elementAt(t-8);
	    }
        setDecryptedData(data, data.length);
        setData(data, data.length);
        encrypted = false;
	}

	public static WaypointBlock waypointZeroForFleet(FleetBlock fleet) {
	    WaypointBlock res = new WaypointBlock();
	    res.x = fleet.x;
	    res.y = fleet.y;
	    res.positionObject = fleet.positionObjectId;
	    if (res.positionObject == 0x0FFFF) {
	        res.positionObject = 0;
	        res.positionObjectType = 20;
	    } else {
	        res.positionObjectType = 17;
	    }
	    res.encode();
	    return res;
	}

	/**
	 * @param mBlocks If multiple turns in one m-file, should be only last turns blocks.
	 */
	public static HashMap<Integer,Vector<Waypoint>> getFleetBlocks(List<Block> mBlocks) {
		HashMap<Integer,Vector<Waypoint>> waypoints = new HashMap<Integer,Vector<Waypoint>>();
		Vector<WaypointBlock> v = new Vector<WaypointBlock>();
		for (Block b : mBlocks){
			if (b instanceof WaypointBlock){
				WaypointBlock wp = (WaypointBlock)b;
				v.addElement(wp);
			}
		}
		for (Block b : mBlocks){
			if (b instanceof FleetBlock){
				FleetBlock f = (FleetBlock)b;
				Vector<Waypoint> vv = waypoints.get(f.fleetNumber);
				if (vv == null){
					vv = new Vector<Waypoint>();
					waypoints.put(f.fleetNumber,vv);
				}
				for (int t = 0; t < f.waypointCount; t++){	
					vv.addElement(v.remove(0));
				}
			}
		}
		return waypoints;
	}
}
