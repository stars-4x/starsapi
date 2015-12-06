package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PartialFleetBlock extends Block {
    public static final int PARTIAL_KIND = 3, PICK_POCKET_KIND = 4, FULL_KIND = 7;
    
    public int fleetNumber;
    public int owner;
    public byte byte2, byte3, byte5;
    public byte kindByte; // 3 for most partial, 4 for robber baron, 7 for full
    public int positionObjectId;
    public int x, y;
    public int shipTypes;
    public int[] shipCount = new int[16];
    public long ironium, boranium, germanium, population, fuel;
    public int deltaX, deltaY, unknownBitsWithWarp, warp; // partial fleet data
    public long mass; // partial fleet data
    // follows is full fleet data
    public int damagedShipTypes;
    public int[] damagedShipInfo = new int[16];
    public int battlePlan;
    public int waypointCount;
    
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
	    // unknownBitsWithWarp = 16;
	}

	public void convertToPartialFleetWithMinerals() {
	    typeId = BlockType.PARTIAL_FLEET;
	    if (kindByte == FULL_KIND) kindByte = PICK_POCKET_KIND;
	    population = 0;
	    // unknownBitsWithWarp = 16;
	}
	
    public static FleetBlock convertToFleetBlockForHstFile(PartialFleetBlock block, DesignBlock[] designs) throws Exception {
        FleetBlock res;
        if (block.typeId == BlockType.FLEET) {
            res = (FleetBlock)block;
        } else {
            block.typeId = BlockType.FLEET;
            block.waypointCount = 1; 
            if (block.kindByte != FULL_KIND) {
                block.kindByte = FULL_KIND;
                block.fuel = 0;
                for (int i = 0; i < 16; i++) {
                    int count = block.shipCount[i];
                    if (count > 0) {
                        block.fuel += designs[i].fuelCapacity * count;
                    }
                }
            }
            block.encode();
            res = new FleetBlock();
            res.setDecryptedData(block.getDecryptedData(), block.size);
            res.decode();
        }
        return res;
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
        positionObjectId = Util.read16(decryptedData, 6);
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
            damagedShipTypes = Util.read16(decryptedData, index);
            index += 2;
            mask = 1;
            for (int bit = 0; bit < 16; bit++) {
                if ((damagedShipTypes & mask) != 0) {
                    damagedShipInfo[bit] = Util.read16(decryptedData, index);
                    index += 2;
                }
                mask <<= 1;
            }
            battlePlan = Util.read8(decryptedData[index++]);
            waypointCount = Util.read8(decryptedData[index++]);
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
        }
        if (index != size) {
            throw new Exception("Unexpected trailing data in fleet: " + this);
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
	    if (kindByte == FULL_KIND) {
	        len += 4;
	        mask = 1;
	        for (int bit = 0; bit < 16; bit++) {
	            if ((damagedShipTypes & mask) != 0) {
	                len += 2;
	            }
	            mask <<= 1;
	        }
	    } else {
	        len += 8;
	    }
	    byte[] res = new byte[len];
	    res[0] = (byte)(fleetNumber & 0xFF);
	    res[1] = (byte)((owner << 1) + ((fleetNumber & 0x0100) >> 8));
	    res[2] = byte2;
	    res[3] = byte3;
	    res[4] = kindByte;
	    res[5] = byte5;
        Util.write16(res, 6, positionObjectId);
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
            Util.write16(res, index, damagedShipTypes);
            index += 2;
            mask = 1;
            for (int bit = 0; bit < 16; bit++) {
                if ((damagedShipTypes & mask) != 0) {
                    Util.write16(res, index, damagedShipInfo[bit]);
                    index += 2;
                }
                mask <<= 1;
            }
            res[index++] = (byte)battlePlan;
            res[index++] = (byte)waypointCount;
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
	
	public long calculateMass(DesignBlock[] designs) {
	    if (kindByte != FULL_KIND) return mass;
	    long res = 0;
	    for (int i = 0; i < 16; i++) {
	        int count = shipCount[i];
	        if (count > 0) res += count * designs[i].mass;
	    }
	    res += ironium + boranium + germanium + population;
	    return res;
	}

}
