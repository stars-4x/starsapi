package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PartialFleetBlock extends Block {
    public int fleetNumber;
    public int owner;
    public byte byte2, byte3, byte5, byte6, byte7;
    public byte kindByte; // 3 for most partial, 4 for robber baron, 7 for full
    public int x, y;
    public int shipTypes;
    public int[] shipCount = new int[16];
    public byte[] contentsBytes = new byte[0];
    public byte[] fullFleetBytes = new byte[0];
    public int deltaX, deltaY, unknownBitsWithWarp, warp, mass; // partial fleet data
    
	public PartialFleetBlock() {
		typeId = BlockType.PARTIAL_FLEET;
	}
	
	public int getFleetIdAndOwner() {
	    return fleetNumber | (owner << 9);
	}

	public void convertToPartialFleet() {
	    typeId = BlockType.PARTIAL_FLEET;
	    kindByte = 3;
	    contentsBytes = new byte[0];
	    fullFleetBytes = new byte[0];
	    // unknownBitsWithWarp = 16;
	}
	
	@Override
	public void decode() throws Exception {
	    fleetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 1) << 8);
	    owner = decryptedData[1] >> 1;
	    if (owner < 0 || owner >= 16) throw new Exception("Unexpected owner: " + this);
	    byte2 = decryptedData[2];
	    byte3 = decryptedData[3];
	    kindByte = decryptedData[4];
	    if (kindByte != 7 && kindByte != 4 && kindByte != 3) {
	        throw new Exception("Unexpected byte[4]: " + this);
	    }
        byte5 = decryptedData[5];
        boolean shipCountTwoBytes = false;
        if ((byte5 & 8) == 0) {
            shipCountTwoBytes = true;
        }
        byte6 = decryptedData[6];
        byte7 = decryptedData[7];
	    x = Util.read16(decryptedData, 8);
        y = Util.read16(decryptedData, 10);
        shipTypes = Util.read16(decryptedData, 12);
        int index = 14;
        int mask = 1;
        for (int bit = 0; bit < 16; bit++) {
            if ((shipTypes & mask) != 0) {
                if (shipCountTwoBytes) {
                    shipCount[bit] = Util.read16(decryptedData, index);
                    index += 2;
                } else {
                    shipCount[bit] = Util.read8(decryptedData[index]);
                    index += 1;
                }
            }
            mask <<= 1;
        }
        if ((kindByte & 4) != 0) {
            int contentsLengths = Util.read16(decryptedData, index);
            int iLength = contentsLengths & 0x03;
            int bLength = (contentsLengths & 0x0C) >> 2;
            int gLength = (contentsLengths & 0x30) >> 4;
            int popLength = (contentsLengths & 0xC0) >> 6;
            int fuelLength = contentsLengths >> 8;
            int contentsBytesLength = 2;
            contentsBytesLength += 4 >> (3 - iLength);
            contentsBytesLength += 4 >> (3 - bLength);
            contentsBytesLength += 4 >> (3 - gLength);
            contentsBytesLength += 4 >> (3 - popLength);
            contentsBytesLength += 4 >> (3 - fuelLength);
            contentsBytes = new byte[contentsBytesLength];
            System.arraycopy(decryptedData, index, contentsBytes, 0, contentsBytesLength);
            index += contentsBytesLength;
        }
        if (kindByte == 7) {
            fullFleetBytes = new byte[size - index];
            System.arraycopy(decryptedData, index, fullFleetBytes, 0, size - index);
        } else {
            deltaX = Util.read8(decryptedData[index++]);
            deltaY = Util.read8(decryptedData[index++]);
            warp = decryptedData[index] & 15;
            unknownBitsWithWarp = decryptedData[index] & 0xF0;
            index++;
            if (decryptedData[index] != 0) {
                throw new Exception("Unexpected extra information in warp: " + this);
            }
            index++;
            mass = Util.read16(decryptedData, index);
            index += 2;
            if (decryptedData[index++] != 0 || decryptedData[index++] != 0 || index != size) {
                throw new Exception("Unexpected trailing data in partial fleet: " + this);
            }
        }
	}

	@Override
	public void encode() {
	    boolean shipCountTwoBytes = false;
	    if ((byte5 & 8) == 0) {
	        shipCountTwoBytes = true;
	    }
	    int len = 14;
        int mask = 1;
        for (int bit = 0; bit < 16; bit++) {
            if ((shipTypes & mask) != 0) {
                if (shipCountTwoBytes) {
                    len += 2;
                } else {
                    len += 1;
                }
            }
            mask <<= 1;
        }
	    len += contentsBytes.length;
	    if (kindByte == 7) len += fullFleetBytes.length;
	    else len += 8;
	    byte[] res = new byte[len];
	    res[0] = (byte)(fleetNumber & 0xFF);
	    res[1] = (byte)((owner << 1) + ((fleetNumber & 0x0100) >> 8));
	    res[2] = byte2;
	    res[3] = byte3;
	    res[4] = kindByte;
	    res[5] = byte5;
	    res[6] = byte6;
	    res[7] = byte7;
	    Util.write16(res, 8, x);
        Util.write16(res, 10, y);
        Util.write16(res, 12, shipTypes);
        mask = 1;
        int index = 14;
        for (int bit = 0; bit < 16; bit++) {
            if ((shipTypes & mask) != 0) {
                if (shipCountTwoBytes) {
                    Util.write16(res, index, shipCount[bit]);
                    index += 2;
                } else {
                    res[index] = (byte)(shipCount[bit]);
                    index += 1;
                }
            }
            mask <<= 1;
        }
        System.arraycopy(contentsBytes, 0, res, index, contentsBytes.length);
        index += contentsBytes.length;
        if (kindByte == 7) {
            System.arraycopy(fullFleetBytes, 0, res, index, fullFleetBytes.length);
            index += fullFleetBytes.length;
        } else {
            res[index++] = (byte)deltaX;
            res[index++] = (byte)deltaY;
            res[index] = (byte)(unknownBitsWithWarp + warp);
            index++;
            res[index++] = 0;
            Util.write16(res, index, mass);
            index += 2;
            Util.write16(res, index, 0);
        }
        setDecryptedData(res, res.length);
        setData(res, res.length);
        encrypted = false;
	}

}
