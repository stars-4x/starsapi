package org.starsautohost.starsapi.block;

import java.io.ByteArrayOutputStream;

import org.starsautohost.starsapi.Util;

public class PartialPlanetBlock extends Block {
    public int planetNumber;
    public int owner; // -1 for no owner
    public boolean isHomeworld;
    public boolean isInUseOrRobberBaron; // inhabited or remote mined???
    public boolean hasEnvironmentInfo;
    public boolean bitWhichIsOffForRemoteMiningAndRobberBaron;
    public boolean weirdBit; // I see this on or off somewhat randomly
    public boolean hasRoute;
    public boolean hasSurfaceMinerals;
    public boolean hasArtifact;
    public boolean hasInstallations;
    public boolean isTerraformed;
    public boolean hasStarbase;
    public byte[] preEnvironmentBytes;
    public int ironiumConc, boraniumConc, germaniumConc;
    public int gravity, temperature, radiation;
    public int origGravity, origTemperature, origRadiation;
    public int estimatesShort;
    public long ironium, boranium, germanium, population, fuel;
    public byte[] installationsBytes;
    public int starbaseDesign;
    public byte[] starbaseBytes;
    public int routeShort;
    
    public int turn = -1;
    
	public PartialPlanetBlock() {
		typeId = BlockType.PARTIAL_PLANET;
	}

	@Override
	public void decode() throws Exception {
	    planetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 7) << 8);
	    owner = (decryptedData[1] & 0xF8) >> 3;
	    if (owner == 31) owner = -1;
	    else if (owner >= 16) throw new Exception("Unexpected owner: " + this);
	    int flags = Util.read16(decryptedData, 2);
	    if ((flags & 0x0078) != 0) {
	        throw new Exception("Unexpected planet flags: " + this);
	    }
	    if ((flags & 0x0100) != 0x0100) {
	        throw new Exception("Unexpected planet flags: " + this);
	    }
	    isHomeworld = (flags & 0x80) != 0;
	    isInUseOrRobberBaron = (flags & 0x04) != 0;
	    hasEnvironmentInfo = (flags & 0x02) != 0;
	    bitWhichIsOffForRemoteMiningAndRobberBaron = (flags & 0x01) != 0;
	    weirdBit = (flags & 0x8000) != 0;
	    hasRoute = (flags & 0x4000) != 0;
	    hasSurfaceMinerals = (flags & 0x2000) != 0;
	    hasArtifact = (flags & 0x1000) != 0;
	    hasInstallations = (flags & 0x0800) != 0;
	    isTerraformed = (flags & 0x0400) != 0;
	    hasStarbase = (flags & 0x0200) != 0;
	    if (!bitWhichIsOffForRemoteMiningAndRobberBaron && !hasSurfaceMinerals && !isInUseOrRobberBaron) {
            throw new Exception("Unexpected planet flags for not data[2] & 1: " + this);
	    }
	    if (isInUseOrRobberBaron && typeId == BlockType.PARTIAL_PLANET && bitWhichIsOffForRemoteMiningAndRobberBaron) {
	        throw new Exception("Did not expect data[2] & 5 in partial planet: " + this);
	    }
        if (!isInUseOrRobberBaron && typeId == BlockType.PLANET) {
            throw new Exception("Expected data[2] & 4 in planet: " + this);
        }
        int index = 4;
        if (canSeeEnvironment()) {
            int preEnvironmentLengthByte = Util.read8(decryptedData[4]);
            if ((preEnvironmentLengthByte & 0xC0) != 0) {
                throw new Exception("Unexpected bits at data[3]: " + this);
            }
            int preEnvironmentLength = 1;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x30) >> 4;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x0C) >> 2;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x03);
            preEnvironmentBytes = new byte[preEnvironmentLength];
            System.arraycopy(decryptedData, 4, preEnvironmentBytes, 0, preEnvironmentLength);
            index += preEnvironmentLength;
            ironiumConc = Util.read8(decryptedData[index++]);
            boraniumConc = Util.read8(decryptedData[index++]);
            germaniumConc = Util.read8(decryptedData[index++]);
            gravity = Util.read8(decryptedData[index++]);
            temperature = Util.read8(decryptedData[index++]);
            radiation = Util.read8(decryptedData[index++]);
            if (isTerraformed) {
                origGravity = Util.read8(decryptedData[index++]);
                origTemperature = Util.read8(decryptedData[index++]);
                origRadiation = Util.read8(decryptedData[index++]);
            }
            if (owner >= 0) {
                estimatesShort = Util.read16(decryptedData, index);
                index += 2;
            }
        }
        if (hasSurfaceMinerals) {
            int contentsLengths = Util.read8(decryptedData[index]);
            int iLength = contentsLengths & 0x03;
            iLength = 4 >> (3 - iLength);
            int bLength = (contentsLengths & 0x0C) >> 2;
            bLength = 4 >> (3 - bLength);
            int gLength = (contentsLengths & 0x30) >> 4;
            gLength = 4 >> (3 - gLength);
            int popLength = (contentsLengths & 0xC0) >> 6;
            popLength = 4 >> (3 - popLength);
            index += 1;
            ironium = Util.readN(decryptedData, index, iLength);
            index += iLength;
            boranium = Util.readN(decryptedData, index, bLength);
            index += bLength;
            germanium = Util.readN(decryptedData, index, gLength);
            index += gLength;
            population = Util.readN(decryptedData, index, popLength);
            index += popLength;
        }
        if (hasInstallations) {
            installationsBytes = new byte[8];
            System.arraycopy(decryptedData, index, installationsBytes, 0, 8);
            index += 8;
        }
        if (hasStarbase) {
            if (typeId == BlockType.PARTIAL_PLANET) {
                byte starbaseByte = decryptedData[index++];
                if ((starbaseByte & 0xF0) != 0) {
                    throw new Exception("Unexpected starbase byte: " + this);
                }
                starbaseDesign = starbaseByte;
            } else {
                starbaseBytes = new byte[4];
                System.arraycopy(decryptedData, index, starbaseBytes, 0, 4);
                index += 4;
                starbaseDesign = starbaseBytes[0] & 0x0F;
            }
        }
        if (hasRoute && typeId == BlockType.PLANET) {
            routeShort = Util.read16(decryptedData, index);
            index += 2;
        }
        if (index != size && index + 2 != size) {
            throw new Exception("Unexpected planet data: " + this);
        }
        if (index + 2 == size) {
            turn = Util.read16(decryptedData, index);
        }
	}

    public boolean canSeeEnvironment() {
        return hasEnvironmentInfo || ((hasSurfaceMinerals || isInUseOrRobberBaron) && !bitWhichIsOffForRemoteMiningAndRobberBaron);
    }

	
    public void convertToPartialPlanetForMFile() throws Exception {
        convertToPartialPlanetForHFile(-1);
    }
    
	public void convertToPartialPlanetForHFile(int turn) throws Exception {
	    this.typeId = BlockType.PARTIAL_PLANET;
	    if (canSeeEnvironment()) hasEnvironmentInfo = true;
	    isInUseOrRobberBaron = false;
	    bitWhichIsOffForRemoteMiningAndRobberBaron = true;
	    // hasRoute = false;
	    hasSurfaceMinerals = false;
	    hasArtifact = false;
	    hasInstallations = false;
	    if (turn != -1) this.turn = turn;
	    this.encode();
	}

    public void convertToPartialPlanetForMFileWithMinerals() throws Exception {
        this.typeId = BlockType.PARTIAL_PLANET;
        // hasRoute = false;
        hasArtifact = false;
        hasInstallations = false;
        this.turn = -1;
        if (isInUseOrRobberBaron || hasSurfaceMinerals) {
            isInUseOrRobberBaron = true;
            bitWhichIsOffForRemoteMiningAndRobberBaron = false;
            population = 0;
            // hasSurfaceMinerals = false;
        } else {
            isInUseOrRobberBaron = false;
            bitWhichIsOffForRemoteMiningAndRobberBaron = true;
            hasSurfaceMinerals = false;
        }
        this.encode();
    }
    
	@Override
	public void encode() throws Exception {
	    // NOTE only encodes partial planet, possibly with minerals
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    bout.write(planetNumber & 0xFF);
	    bout.write((planetNumber >> 8) + (owner << 3));
	    int flag1 = 0;
	    if (isHomeworld) flag1 = flag1 | 0x80;
	    if (isInUseOrRobberBaron) flag1 = flag1 | 0x04;
	    if (hasEnvironmentInfo) flag1 = flag1 | 0x02;
	    if (bitWhichIsOffForRemoteMiningAndRobberBaron) flag1 = flag1 | 0x01;
	    bout.write(flag1);
	    int flag2 = 1;
	    if (weirdBit) flag2 = flag2 | 0x80;
	    if (hasRoute) flag2 = flag2 | 0x40;
	    if (hasSurfaceMinerals) flag2 = flag2 | 0x20;
	    if (isTerraformed) flag2 = flag2 | 0x04;
	    if (hasStarbase) flag2 = flag2 | 0x02;
	    bout.write(flag2);
        if (canSeeEnvironment()) {
	        if (preEnvironmentBytes != null) bout.write(preEnvironmentBytes); 
	        bout.write(ironiumConc);
	        bout.write(boraniumConc);
	        bout.write(germaniumConc);
	        bout.write(gravity);
	        bout.write(temperature);
	        bout.write(radiation);
            if (isTerraformed) {
                bout.write(origGravity);
                bout.write(origTemperature);
                bout.write(origRadiation);
            }
            if (owner >= 0) {
                bout.write(estimatesShort & 0xFF);
                bout.write(estimatesShort >> 8);
            }
        }
        if (hasSurfaceMinerals) {
            byte[] res = new byte[getContentLength()];
            int index = 1;
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
            byte igbpopByte = (byte)(iLength | (bLength << 2) | (gLength << 4) | (popLength << 6));
            res[0] = igbpopByte;
            bout.write(res);
        }
        if (hasStarbase) {
            bout.write(starbaseDesign);
        }
        if (turn >= 0) {
            bout.write(turn & 0xFF);
            bout.write(turn >> 8);
        }
        byte[] bytes = bout.toByteArray();
        setDecryptedData(bytes, bytes.length);
        setData(bytes, bytes.length);
        encrypted = false;
	}
	
    private int getContentLength() {
        return 1 + byteLengthForInt(ironium) + byteLengthForInt(boranium) + byteLengthForInt(germanium)
                + byteLengthForInt(population);
    }
    
    private static int byteLengthForInt(long n) {
        if (n == 0) return 0;
        if (n < 256) return 1;
        if (n < 65536) return 2;
        return 4;
    }

}
