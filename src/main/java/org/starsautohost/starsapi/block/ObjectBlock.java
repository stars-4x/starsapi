package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class ObjectBlock extends Block {
    // some of the bytes in minefields differ from player to player inexplicably
    
    public int count = -1;
    public int number;
    public int owner;
    public int type; // 0 = minefield, 1 = packet/salvage, 2 = wormhole, 3 = MT
    
	public ObjectBlock() {
		typeId = BlockType.OBJECT;
	}

	public boolean isCounter() {
	    return count >= 0;
	}
	
	public int getObjectId() {
	    int res = number;
	    res += owner << 9;
	    res += type << 13;
	    return res;
	}
	
	public boolean isWormhole() {
	    return type == 2;
	}
	
	public boolean isWormholeBeenThrough(int playerMask) {
        return type == 2 && ((Util.read16(decryptedData, 10) & playerMask) != 0);
	}

	public void setWormholeBeenThrough(int playerMask) {
	    Util.write16(decryptedData, 10, Util.read16(decryptedData, 10) | playerMask);
	}
	
	@Override
	public void decode() {
	    if (size == 2) {
	        count = Util.read16(decryptedData, 0);
	        return;
	    }
	    int objectId = Util.read16(decryptedData, 0);
	    number = objectId & 0x01FF;
	    owner = (objectId & 0x1E00) >> 9;
	    type = objectId >> 13;
	}

	@Override
	public void encode() {
	    if (isCounter()) {
	        byte[] res = new byte[2];
	        Util.write16(res, 0, count);
	        setDecryptedData(res, 2);
	        return;
	    }
	}

}
