package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class DesignBlock extends Block {
    public boolean isFullDesign;
    public boolean isTransferred;
    public boolean isStarbase;
    public int designNumber;
    public int hullId;
    public int pic;
    public int mass;
    public byte[] fullBytes;
    public byte[] nameBytes;
    
//    public 
    
	public DesignBlock() {
		typeId = BlockType.DESIGN;
	}

	@Override
	public void decode() throws Exception {
	    if ((decryptedData[0] & 3) != 3) {
	        throw new Exception("Unexpected design first byte: " + this);
	    }
        if ((decryptedData[0] & 0xF8) != 0) {
            throw new Exception("Unexpected design first byte: " + this);
        }
        isFullDesign = (decryptedData[0] & 0x04) == 0x04;
        if ((decryptedData[1] & 0x02) != 0) {
            throw new Exception("Unexpected design second byte: " + this);
        }
        if ((decryptedData[1] & 0x01) != 0x01) {
            throw new Exception("Unexpected design second byte: " + this);
        }
        isTransferred = (decryptedData[1] & 0x80) == 0x80;
        isStarbase = (decryptedData[1] & 0x40) == 0x40;
        designNumber = (decryptedData[1] & 0x3C) >> 2;
        hullId = decryptedData[2] & 0xFF;
        pic = decryptedData[3] & 0xFF;
        int index; 
        if (isFullDesign) {
            // TODO calculate mass from slots
            int slotCount = decryptedData[6] & 0xFF;
            fullBytes = new byte[3 + 10 + 4*slotCount];
            System.arraycopy(decryptedData, 4, fullBytes, 0, fullBytes.length);
            index = 4 + fullBytes.length;
        } else {
            mass = Util.read16(decryptedData, 4);
            index = 6;
        }
        int nameLen = decryptedData[index];
        nameBytes = new byte[1 + nameLen];
        System.arraycopy(decryptedData, index, nameBytes, 0, 1 + nameLen);
        index += 1 + nameLen;
        if (index != size) {
            throw new Exception("Unexpected design size: " + this);
        }
	}

	@Override
	public void encode() {
	    // unimplemented, not yet changing design blocks
	}

}
