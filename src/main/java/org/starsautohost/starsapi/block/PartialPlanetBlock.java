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
    public byte[] surfaceMineralsAndPopulationBytes;
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
            throw new Exception("Unexpected planet flags for not data[3] & 1: " + this);
	    }
	    if (isInUseOrRobberBaron && typeId == BlockType.PARTIAL_PLANET && (bitWhichIsOffForRemoteMiningAndRobberBaron || !hasSurfaceMinerals)) {
	        throw new Exception("Did not expect data[3] & 4 without mining-or-baron bit off in partial planet: " + this);
	    }
        if (!isInUseOrRobberBaron && typeId == BlockType.PLANET) {
            throw new Exception("Expected data[3] & 4 in planet: " + this);
        }
        int index = 4;
        if (hasEnvironmentInfo || ((hasSurfaceMinerals || isInUseOrRobberBaron) && !bitWhichIsOffForRemoteMiningAndRobberBaron)) {
            int preEnvironmentLengthByte = Util.read8(decryptedData[4]);
            if ((preEnvironmentLengthByte & 0xC0) != 0) {
                throw new Exception("Unexpected bits at data[4]: " + this);
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
            int surfaceMineralsAndPopulationLengthByte = Util.read8(decryptedData[index]);
            int popBits = (surfaceMineralsAndPopulationLengthByte & 0xC0) >> 6;
            int gBits = (surfaceMineralsAndPopulationLengthByte & 0x30) >> 4;
            int bBits = (surfaceMineralsAndPopulationLengthByte & 0x0C) >> 2;
            int iBits = (surfaceMineralsAndPopulationLengthByte & 0x03);
            int surfaceMineralsAndPopulationLength = 1;
            surfaceMineralsAndPopulationLength += 4 >> (3 - iBits);
            surfaceMineralsAndPopulationLength += 4 >> (3 - bBits);
            surfaceMineralsAndPopulationLength += 4 >> (3 - gBits);
            surfaceMineralsAndPopulationLength += 4 >> (3 - popBits);
            surfaceMineralsAndPopulationBytes = new byte[surfaceMineralsAndPopulationLength];
            System.arraycopy(decryptedData, index, surfaceMineralsAndPopulationBytes, 0, surfaceMineralsAndPopulationLength);
            index += surfaceMineralsAndPopulationLength;
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

	public void convertToPartialPlanetForHFile(int turn) throws Exception {
	    this.typeId = BlockType.PARTIAL_PLANET;
	    isInUseOrRobberBaron = false;
	    bitWhichIsOffForRemoteMiningAndRobberBaron = true;
	    hasRoute = false;
	    hasSurfaceMinerals = false;
	    hasArtifact = false;
	    hasInstallations = false;
	    if (turn != -1) this.turn = turn;
	    this.encode();
	}
	
	@Override
	public void encode() throws Exception {
	    // NOTE only encodes partial planet for H file merge
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    bout.write(planetNumber & 0xFF);
	    bout.write((planetNumber >> 8) + (owner << 3));
	    int flag1 = 1;
	    if (isHomeworld) flag1 = flag1 | 0x80;
	    if (hasEnvironmentInfo) flag1 = flag1 | 0x02;
	    bout.write(flag1);
	    int flag2 = 1;
	    if (weirdBit) flag2 = flag2 | 0x80;
	    if (isTerraformed) flag2 = flag2 | 0x04;
	    if (hasStarbase) flag2 = flag2 | 0x02;
	    bout.write(flag2);
	    if (hasEnvironmentInfo) {
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
}
