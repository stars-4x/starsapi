package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PlayerBlock extends Block {
	
	public static boolean ignoreParseErrors = false;
	
    public static class PRT {
        public static int HE = 0;
        public static int SS = 1;
        public static int WM = 2;
        public static int CA = 3;
        public static int IS = 4;
        public static int SD = 5;
        public static int PP = 6;
        public static int IT = 7;
        public static int AR = 8;
        public static int JOAT = 9;
    }
    
    public int playerNumber; 
    public int shipDesignCount;
    public int planets;
    public int fleets;
    public int starbaseDesignCount;
    public int logo;
    public boolean fullDataFlag;
    public byte[] fullDataBytes;
    public byte[] playerRelations = new byte[0]; // 0 neutral, 1 friend, 2 enemy

    public String nameSingular;
    public String namePlural;
    
    public byte byte7 = 1;
    
    /**
     * The in-game Block 6 struct *for race files* is exactly this size. 
     * The checksum of this block requires the internals be aligned at the 
     * proper spots (e.g race names)
     */
    public byte[] raceFileStructData = new byte[192];  
    
    
	public PlayerBlock() {
		typeId = BlockType.PLAYER;
	}

	@Override
	public void decode() throws Exception {
	    playerNumber = decryptedData[0] & 0xFF;
	    shipDesignCount = decryptedData[1] & 0xFF;
	    planets = (decryptedData[2] & 0xFF) + ((decryptedData[3] & 0x03) << 8);
	    if ((decryptedData[3] & 0xFC) != 0) {
	        throw new Exception("Unexpected player values " + this);
	    }
	    fleets = (decryptedData[4] & 0xFF) + ((decryptedData[5] & 0x03) << 8);
	    starbaseDesignCount = ((decryptedData[5] & 0xF0) >> 4);
        if ((decryptedData[5] & 0x0C) != 0) {
            throw new Exception("Unexpected player values " + this);
        }
	    logo = ((decryptedData[6] & 0xFF) >> 3);
	    fullDataFlag = (decryptedData[6] & 0x04) != 0;
        if ((decryptedData[6] & 0x03) != 3) {
            throw new Exception("Unexpected player values " + this);
        }
        byte7 = decryptedData[7];
        // TODO maybe AI doesn't have this?
//        if (decryptedData[7] != 1) {
//            throw new Exception("Unexpected player values " + this);
//        }
        int index = 8;
	    if (fullDataFlag) {
	        fullDataBytes = new byte[0x68];
	        System.arraycopy(decryptedData, 8, fullDataBytes, 0, 0x68);
	        index = 0x70;
	        int playerRelationsLength = decryptedData[index] & 0xFF;
	        playerRelations = new byte[playerRelationsLength];
            System.arraycopy(decryptedData, index + 1, playerRelations, 0, playerRelationsLength);
            index += 1 + playerRelationsLength;
	    }

	    int namesStart = index;  // Save for later
	    
	    // Decode the singular name
	    byte[] nameBytesSingular = new byte[32];
	    int singularNameLength = decryptedData[index] & 0xFF;
	    System.arraycopy(decryptedData, index, nameBytesSingular, 0, singularNameLength+1);
	    
	    nameSingular = Util.decodeStarsString(nameBytesSingular);
	    
	    index += (singularNameLength + 1);
	    
	    // Decode plural name (if exist)
	    byte[] nameBytesPlural = new byte[32];
	    int pluralNameLength = decryptedData[index] & 0xFF;
	    System.arraycopy(decryptedData, index, nameBytesPlural, 0, pluralNameLength+1);
	    
	    namePlural = Util.decodeStarsString(nameBytesPlural);
	    
	    index += pluralNameLength+1;
	    // If no plural name skip another byte because of 16-bit alignment
	    if(pluralNameLength == 0)
	    	index++;

	    /*
	     * Now fill out the properly aligned race file struct data that is 
	     * needed for checksums
	     */
	    // First copy in everything up until the singular name
	    System.arraycopy(decryptedData, 0, raceFileStructData, 0, namesStart);
	    
	    // Now do the singular/plural names which are the final two 16-word (32 byte) 
	    // chunks in the struct data (which is 192 bytes)
	    byte[] decodedSingularNameBytes = nameSingular.getBytes();
	    System.arraycopy(decodedSingularNameBytes, 0, raceFileStructData, 128, decodedSingularNameBytes.length);
	    
	    byte[] decodedPluralNameBytes = namePlural.getBytes();
	    System.arraycopy(decodedPluralNameBytes, 0, raceFileStructData, 160, decodedPluralNameBytes.length);
	    
	    
	    if (index != size) {
	    	String error = "Unexpected player data size: " + this;
	    	error += "\nIndex: "+index+", size: "+size;
	    	error += "\nPlayer nr: "+playerNumber+", "+ nameSingular;
	    	error += "\nBlock size: "+(decryptedData != null ? decryptedData.length : 0);
	    	if (ignoreParseErrors){
	    		System.out.println(error);
	    		System.out.println("Continuing anyway");
	    	}
	    	else throw new Exception(error);
	    }
	    //System.out.println("Index: "+index);
	}

	@Override
	public void encode() throws Exception {
		// Encode singular/plural race names
		byte[] nameSingularBytes = Util.encodeStarsString(nameSingular);
		byte[] namePluralBytes = Util.encodeStarsString(namePlural);
				
		int nameBytesOffset = 8;
		if (fullDataFlag)
	    	nameBytesOffset = 8 + 0x68 + 1 + playerRelations.length;

		// Initialize decrypted data array	
		byte[] res = new byte[nameBytesOffset + nameSingularBytes.length + namePluralBytes.length];
	    
		// Encode
	    res[0] = (byte)playerNumber;
        res[1] = (byte)shipDesignCount;
        res[2] = (byte)(planets & 0xFF);
        res[3] = (byte)(planets >> 8);
        res[4] = (byte)(fleets & 0xFF);
        res[5] = (byte)((starbaseDesignCount << 4) + (fleets >> 8));
        res[6] = (byte)((logo << 3) + 7);
        res[7] = byte7;

        // Copy in fullData and playerRelations if exist
	    if (fullDataFlag) {
            System.arraycopy(fullDataBytes, 0, res, 8, fullDataBytes.length);
            res[0x70] = (byte)playerRelations.length;
            System.arraycopy(playerRelations, 0, res, 0x71, playerRelations.length);
	    } 
        
        // Copy in names bytes
        System.arraycopy(nameSingularBytes, 0, res, nameBytesOffset, nameSingularBytes.length);
        System.arraycopy(namePluralBytes, 0, res, nameBytesOffset + nameSingularBytes.length, namePluralBytes.length);
        
        setDecryptedData(res, res.length);
	}

	// CAs see this
	public boolean hasEnvironmentInfoOnly() {
	    if (!fullDataFlag) return false;
	    for (int i = 0; i < 8; i++) {
	        if (fullDataBytes[i] != 0) return false;
	    }
        for (int i = 17; i < 0x68; i++) {
            if (fullDataBytes[i] != 0) return false;
        }
        return true;
	}
	
	public byte getPlayerRelationsWith(int player) {
	    if (player >= playerRelations.length) {
	        return 0;
	    } else {
	        return playerRelations[player];
	    }
	}
	
	public void makeBeefyFullData(int prt, boolean nrse) {
        boolean hasEnvironmentInfo = hasEnvironmentInfoOnly();
	    fullDataFlag = true;
	    if (fullDataBytes == null) fullDataBytes = new byte[0x68];
	    if (!hasEnvironmentInfo) {
	        for (int i = 8; i < 17; i++) {
	            fullDataBytes[i] = (byte)0xFF; //tri-immune
	        }
	    }
        fullDataBytes[17] = 3; // growth rate, this is the point mine...
        for (int i = 18; i < 24; i++) {
            fullDataBytes[i] = 26; // tech
        }
        fullDataBytes[54] = 7; // pop efficiency
        if (prt == PRT.AR) {
            fullDataBytes[55] = 10;
            fullDataBytes[56] = 10;
            fullDataBytes[57] = 10;
            fullDataBytes[58] = 10;
            fullDataBytes[59] = 5;
            fullDataBytes[60] = 10;
        } else {
            fullDataBytes[55] = 15; // factories
            fullDataBytes[56] = 5;
            fullDataBytes[57] = 25;
            fullDataBytes[58] = 25; // mines
            fullDataBytes[59] = 2;
            fullDataBytes[60] = 25;
        }
        // #61 is "spend leftover points on"
        fullDataBytes[62] = 0; // expensive tech
        fullDataBytes[63] = 0;
        fullDataBytes[64] = 0;
        fullDataBytes[65] = 0;
        fullDataBytes[66] = 0;
        fullDataBytes[67] = 0; // end of tech
        fullDataBytes[68] = (byte)prt;
        // #69 is always 0...
        fullDataBytes[70] = nrse ? (byte)141 : 13; // lrts (IFE, ISB, ARM; make this 141 with NRSE)
        fullDataBytes[71] = 32; // lrts (RS)
        fullDataBytes[73] = (byte)128; // G-box checked; +32 is tech-starts-with-3
        fullDataBytes[74] = (byte)255; // all MT items
        fullDataBytes[75] = 15; // all MT items
	}

	public void setTech(int energy, int weapons, int propulsion, int construction, int electronics, int biotech) {
	    fullDataBytes[18] = (byte)energy;
        fullDataBytes[19] = (byte)weapons;
        fullDataBytes[20] = (byte)propulsion;
        fullDataBytes[21] = (byte)construction;
        fullDataBytes[22] = (byte)electronics;
        fullDataBytes[23] = (byte)biotech;
	}
	
	public void setMtMask(int mtMask) {
	    fullDataBytes[74] = (byte)(mtMask >> 8);
	    fullDataBytes[75] = (byte)(mtMask & 0xFF);
	}
	
	public static PlayerBlock createUnknownRacePlayerBlock(int playerNumber) {
	    // does it need a distinct logo? homeworld?
	    PlayerBlock block = new PlayerBlock();
	    block.playerNumber = playerNumber;
	    block.makeBeefyFullData(PRT.JOAT, false);
	    	
        block.nameSingular = "P" + playerNumber;
        block.namePlural = "P" + playerNumber;
        
	    return block;
	}

	public byte[] getFullDataBytes() {
		return fullDataBytes;
	}

	public void setFullDataFlag(boolean b) {
		fullDataFlag = b;
	}
}
