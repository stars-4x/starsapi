package org.starsautohost.starsapi.block;

public class PlayerBlock extends Block {
    public int playerNumber;
    public int shipDesigns;
    public int planets;
    public int fleets;
    public int starbaseDesigns;
    public int logo;
    public boolean fullDataFlag;
    public byte[] fullDataBytes;
    public byte[] playerRelationsBytes;
    public byte[] nameBytes;
    
	public PlayerBlock() {
		typeId = BlockType.PLAYER;
	}

	@Override
	public void decode() throws Exception {
	    playerNumber = decryptedData[0] & 0xFF;
	    shipDesigns = decryptedData[1] & 0xFF;
	    planets = (decryptedData[2] & 0xFF) + ((decryptedData[3] & 0x03) << 8);
	    if ((decryptedData[3] & 0xFC) != 0) {
	        throw new Exception("Unexpected player values " + this);
	    }
	    fleets = (decryptedData[4] & 0xFF) + ((decryptedData[5] & 0x03) << 8);
	    starbaseDesigns = ((decryptedData[5] & 0xF0) >> 4);
        if ((decryptedData[5] & 0x0C) != 0) {
            throw new Exception("Unexpected player values " + this);
        }
	    logo = ((decryptedData[6] & 0xFF) >> 3);
	    fullDataFlag = (decryptedData[6] & 0x04) != 0;
        if ((decryptedData[6] & 0x03) != 3) {
            throw new Exception("Unexpected player values " + this);
        }
        if (decryptedData[7] != 1) {
            throw new Exception("Unexpected player values " + this);
        }
        int index = 8;
	    if (fullDataFlag) {
	        fullDataBytes = new byte[0x68];
	        System.arraycopy(decryptedData, 8, fullDataBytes, 0, 0x68);
	        index = 0x70;
	        int playerRelationsLength = 1 + decryptedData[index] & 0xFF;
	        playerRelationsBytes = new byte[playerRelationsLength];
            System.arraycopy(decryptedData, index, playerRelationsBytes, 0, playerRelationsLength);
            index += playerRelationsLength;
	    }
	    int namesStart = index;
	    int singularNameLength = decryptedData[index++] & 0xFF;
	    index += singularNameLength;
	    int pluralNameLength = decryptedData[index++] & 0xFF;
	    index += pluralNameLength;
	    nameBytes = new byte[index - namesStart];
	    System.arraycopy(decryptedData, namesStart, nameBytes, 0, nameBytes.length);
	    if (index != size) {
	        throw new Exception("Unexpected player data size: " + this);
	    }
	}

	@Override
	public void encode() throws Exception {
	    if (fullDataFlag) {
	        throw new Exception("Unimplemented encoding of full player data");
	    }
	    byte[] res = new byte[8 + nameBytes.length];
	    res[0] = (byte)playerNumber;
	    res[1] = (byte)shipDesigns;
	    res[2] = (byte)(planets & 0xFF);
	    res[3] = (byte)(planets >> 8);
        res[4] = (byte)(fleets & 0xFF);
        res[5] = (byte)((starbaseDesigns << 4) + (fleets >> 8));
        res[6] = (byte)((logo << 3) + 3);
        res[7] = 1;
        System.arraycopy(nameBytes, 0, res, 8, nameBytes.length);
        setDecryptedData(res, res.length);
	}

}
