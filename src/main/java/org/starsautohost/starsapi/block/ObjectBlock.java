package org.starsautohost.starsapi.block;

import java.math.BigInteger;

import org.starsautohost.starsapi.Util;

public class ObjectBlock extends Block {
    // some of the bytes in minefields differ from player to player inexplicably
    
    public int count = -1;
    public int number;
    public int owner;
    public int type; // 0 = minefield, 1 = packet/salvage, 2 = wormhole, 3 = MT
    public int x, y;
    
    //For MT
    public int xDest, yDest;
    public int warp;
    public int metBits;
    public int itemBits;
    public int turnNo;
    //For minefields
    public long mineCount;
    //For wormholes
    public int wormholeId;
    public int targetId;
    
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

	public boolean isMinefield() {
	    return type == 0;
	}

	public boolean isPacketOrSalvage() {
	    return type == 1;
	}

	public boolean isWormhole() {
	    return type == 2;
	}
	
	public boolean isMT() {
	    return type == 3;
	}
	
	public boolean isWormholeBeenThrough(int playerMask) {
        return type == 2 && ((Util.read16(decryptedData, 10) & playerMask) != 0);
	}

	public void setWormholeBeenThrough(int playerMask) {
	    Util.write16(decryptedData, 10, Util.read16(decryptedData, 10) | playerMask);
	}

	public void setWormholeVisible(int playerMask) {
	    Util.write16(decryptedData, 8, Util.read16(decryptedData, 8) | playerMask);
	}

	public void setMinefieldVisible(int playerMask) {
	    Util.write16(decryptedData, 14, Util.read16(decryptedData, 14) | playerMask);
	}

	// 0 standard
	// 1 heavy
	// 2 speed bump
	public int getMinefieldType() {
	    return Util.read8(decryptedData[12]);
	}
	
	public boolean isMinefieldDetonating() {
	    return decryptedData[13] == 1;
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
	    if (isMT()){
	    	x = Util.read16(decryptedData, 2);
			y = Util.read16(decryptedData, 4);
			xDest = Util.read16(decryptedData, 6);
			yDest = Util.read16(decryptedData, 8);
			warp = decryptedData[10] % 16;
			metBits = Util.read16(decryptedData, 12);
			itemBits = Util.read16(decryptedData, 14);
			turnNo = Util.read16(decryptedData, 16);
			//System.out.println(toStringMt());
			//System.out.println(getMetPlayers());
			//System.out.println(toString());
	    }
	    else if (isMinefield()){
	    	x = Util.read16(decryptedData, 2);
			y = Util.read16(decryptedData, 4);
			mineCount = Util.read32(decryptedData, 6);
	    }
	    else if (isWormhole()){
	    	x = Util.read16(decryptedData, 2);
			y = Util.read16(decryptedData, 4);
			wormholeId = Util.read16(decryptedData, 0) % 4096;
			targetId = Util.read16(decryptedData, 12) % 4096;
			int beenThrough = Util.read16(decryptedData, 8);
			int canSee = Util.read16(decryptedData, 10);
			//System.out.println(wormholeId+" "+targetId+" "+x+" "+y);
			//System.out.println(Integer.toBinaryString(beenThrough));
			//System.out.println(Integer.toBinaryString(canSee));
	    }
	}
	
	public double getDeltaX(){
		double dx = xDest-x;
		double dy = Math.abs(yDest-y);
		return dx/dy; //(Math.abs(dx)+Math.abs(dy));
	}
	public double getDeltaY(){
		double dx = Math.abs(xDest-x);
		double dy = yDest-y;
		return -dy/dx; //(Math.abs(dx)+Math.abs(dy));
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
	
	public String getMTPartName() {
		BigInteger i = new BigInteger(""+itemBits);
		if (i.testBit(0)) return "Multi Cargo Pod";
		if (i.testBit(1)) return "Multi Function Pod";
		if (i.testBit(2)) return "Langston Shield";
		if (i.testBit(3)) return "Mega Poly Shell";
		if (i.testBit(4)) return "Alien Miner";
		if (i.testBit(5)) return "Hush-a-Boom";
		if (i.testBit(6)) return "Anti Matter Torpedo";
		if (i.testBit(7)) return "Multi Contained Munition";
		if (i.testBit(8)) return "Mini Morph";
		if (i.testBit(9)) return "Enigma Pulsar";
		if (i.testBit(10)) return "Genesis Device";
		if (i.testBit(11)) return "Jump Gate";
		if (i.testBit(12)) return "Ship";
		if (itemBits == 0) return "Research";
		return "";
	}
	
	public String getMetPlayers(){
		String s = "";
		BigInteger i = new BigInteger(""+metBits);
		for (int t = 0; t < 16; t++){
			if (i.testBit(t)) s += "Player #"+(t+1)+"\n";
		}
		return s;
	}
	public String toStringMt(){
		return "MT#"+number+" "+x+","+y+" -> "+xDest+","+yDest+" "+warp+" "+metBits+" "+itemBits+" "+turnNo+" "+getMTPartName();
	}
}
