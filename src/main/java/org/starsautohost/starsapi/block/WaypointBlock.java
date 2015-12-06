package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class WaypointBlock extends Block {
    public int x;
    public int y;
    public int positionObject;
    public int warp;
    public int unknownBitsWithWarp;
    public int positionObjectType;
    
	public WaypointBlock() {
		typeId = BlockType.WAYPOINT;
	}

	@Override
	public void decode() {
	    x = Util.read16(decryptedData, 0);
        y = Util.read16(decryptedData, 2);
        positionObject = Util.read16(decryptedData, 4);
        warp = (decryptedData[6] & 0xFF) >> 4;
        unknownBitsWithWarp = decryptedData[6] & 0x0F;
        positionObjectType = decryptedData[7] & 0xFF;
	}

	@Override
	public void encode() {
	    byte[] data = new byte[8];
	    Util.write16(data, 0, x);
	    Util.write16(data, 2, y);
	    Util.write16(data, 4, positionObject);
	    data[6] = (byte)((warp << 4) | unknownBitsWithWarp);
	    data[7] = (byte)positionObjectType;
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
}
