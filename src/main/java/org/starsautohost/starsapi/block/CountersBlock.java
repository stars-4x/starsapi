package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class CountersBlock extends Block {
    public int planetCount;
    public int fleetCount;
    
	public CountersBlock() {
		typeId = BlockType.COUNTERS;
	}

	@Override
	public void decode() throws Exception {
	    if (!hasDecryptedData) throw new Exception("Cannot decode without decrypted data being set!");
	    if (size != 4) throw new Exception("Expected 4 bytes in counters block: " + this);
	    planetCount = Util.read16(decryptedData, 0);
	    fleetCount = Util.read16(decryptedData, 2);
	    if (planetCount >= 256 * 8) throw new Exception("Planet count too high: " + this);
        if (fleetCount >= 256 * 8) throw new Exception("Fleet count too high: " + this);
	}

	@Override
	public void encode() {
	    byte[] decryptedData = new byte[4];
	    Util.write16(decryptedData, 0, planetCount);
        Util.write16(decryptedData, 2, fleetCount);
        setDecryptedData(decryptedData, 4);
	}
}

