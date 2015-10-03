package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PartialFleetBlock extends Block {
    public static final int PARTIAL_KIND = 3, PICK_POCKET_KIND = 4, FULL_KIND = 7;
    
    public int fleetNumber;
    public int owner;
    public byte byte2, byte3, byte5, byte6, byte7;
    public byte kindByte; // 3 for most partial, 4 for robber baron, 7 for full
    public int x, y;
    public int shipTypes;
    public int[] shipCount = new int[16];
    public long ironium, boranium, germanium, population, fuel;
    public byte[] fullFleetBytes = new byte[0];
    public int deltaX, deltaY, unknownBitsWithWarp, warp; // partial fleet data
    public long mass; // partial fleet data
    
	public PartialFleetBlock() {
		typeId = BlockType.PARTIAL_FLEET;
	}
	
	public int getFleetIdAndOwner() {
	    return fleetNumber | (owner << 9);
	}

	public void convertToPartialFleet() {
	    typeId = BlockType.PARTIAL_FLEET;
	    kindByte = PARTIAL_KIND;
	    ironium = 0;
	    boranium = 0;
	    germanium = 0;
	    population = 0;
	    fuel = 0;
	    fullFleetBytes = new byte[0];
	    // unknownBitsWithWarp = 16;
	}

	public void convertToPartialFleetWithMinerals() {
	    typeId = BlockType.PARTIAL_FLEET;
	    if (kindByte == FULL_KIND) kindByte = PICK_POCKET_KIND;
	    population = 0;
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
	    if (kindByte != FULL_KIND && kindByte != PICK_POCKET_KIND && kindByte != PARTIAL_KIND) {
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
        if (kindByte == FULL_KIND || kindByte == PICK_POCKET_KIND) {
            int contentsLengths = Util.read16(decryptedData, index);
            int iLength = contentsLengths & 0x03;
            iLength = 4 >> (3 - iLength);
            int bLength = (contentsLengths & 0x0C) >> 2;
            bLength = 4 >> (3 - bLength);
	        int gLength = (contentsLengths & 0x30) >> 4;
            gLength = 4 >> (3 - gLength);
            int popLength = (contentsLengths & 0xC0) >> 6;
            popLength = 4 >> (3 - popLength);
            int fuelLength = contentsLengths >> 8;
            fuelLength = 4 >> (3 - fuelLength);
            index += 2;
            ironium = Util.readN(decryptedData, index, iLength);
            index += iLength;
            boranium = Util.readN(decryptedData, index, bLength);
            index += bLength;
            germanium = Util.readN(decryptedData, index, gLength);
            index += gLength;
            population = Util.readN(decryptedData, index, popLength);
            index += popLength;
            fuel = Util.readN(decryptedData, index, fuelLength);
            index += fuelLength;
        }
        if (kindByte == FULL_KIND) {
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
            mass = Util.read32(decryptedData, index);
            index += 4;
            if (index != size) {
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
        if (kindByte == PICK_POCKET_KIND || kindByte == FULL_KIND) {
            len += getContentLength();
        }
	    if (kindByte == FULL_KIND) len += fullFleetBytes.length;
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
        if (kindByte == FULL_KIND || kindByte == PICK_POCKET_KIND) {
            int contentsLengthIndex = index;
            index += 2;
            int iLength = Util.writeN(res, index, ironium);
            index += iLength;
            if (iLength == 4) iLength = 3;
            int bLength = Util.writeN(res, index, boranium);
            index += bLength;
            if (bLength == 4) bLength = 3;
            int gLength = Util.writeN(res, index, germanium);
            index += gLength;
            if (gLength == 4) gLength = 3;
            int popLength = Util.writeN(res, index, population);
            index += popLength;
            if (popLength == 4) popLength = 3;
            int fuelLength = Util.writeN(res, index, fuel);
            index += fuelLength;
            if (fuelLength == 4) fuelLength = 3;
            byte igbpopByte = (byte)(iLength | (bLength << 2) | (gLength << 4) | (popLength << 6));
            res[contentsLengthIndex] = igbpopByte;
            res[contentsLengthIndex + 1] = (byte)fuelLength;
        }
        if (kindByte == FULL_KIND) {
            System.arraycopy(fullFleetBytes, 0, res, index, fullFleetBytes.length);
            index += fullFleetBytes.length;
        } else {
            res[index++] = (byte)deltaX;
            res[index++] = (byte)deltaY;
            res[index] = (byte)(unknownBitsWithWarp + warp);
            index++;
            res[index++] = 0;
            Util.write32(res, index, mass);
            index += 4;
        }
        setDecryptedData(res, res.length);
        setData(res, res.length);
        encrypted = false;
	}
	
	private int getContentLength() {
	    return 2 + byteLengthForInt(ironium) + byteLengthForInt(boranium) + byteLengthForInt(germanium)
	            + byteLengthForInt(population) + byteLengthForInt(fuel);
	}
	
	private static int byteLengthForInt(long n) {
	    if (n == 0) return 0;
	    if (n < 256) return 1;
	    if (n < 65536) return 2;
	    return 4;
	}

}
