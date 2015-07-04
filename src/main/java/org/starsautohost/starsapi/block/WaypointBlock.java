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
	    throw new UnsupportedOperationException();
	}

}
