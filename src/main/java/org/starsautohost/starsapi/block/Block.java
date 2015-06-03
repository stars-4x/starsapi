package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;


/**
 * Basic holder for a file block
 */
public abstract class Block {
    private static int BLOCK_PADDING = 4;      // bytes

	public int typeId = BlockType.UNKNOWN_BAD;
	public int size = 0;
	public boolean encrypted = true;
	
	/**
	 * This holds the original block data, padded to a multiple of 4
	 */
	protected byte[] data;
	
	
	/**
	 * Used to enforce proper object usage
	 */
	protected boolean hasData = false;
	protected boolean hasDecryptedData = false;
	
	/**
	 * This holds the decrypted block data, padded to a multiple of 4
	 */
	protected byte[] decryptedData;
	
	
	public Block() {}
	
	/**
	 * Decode the data in this block, essentially deserializing it from Stars!
	 * file types
	 */
	public abstract void decode() throws Exception;
	
	
	/**
	 * Encode the data in this block, serializing it into Stars! file data format.
	 * The result is stored as the decrypted data of the block.
	 */
	public abstract void encode() throws Exception;


	/**
	 * @return the raw byte data from a Stars! file
	 * @throws Exception 
	 */
	public byte[] getData() throws Exception {
		if(!hasData)
			throw new Exception("Raw byte data has not been set!");
			
		return data;
	}

	/**
	 * @param data the raw byte data from a Stars! file
	 * @param size the size of the raw byte data or the decrypted data
	 */
	public void setData(byte[] data, int size) {
		this.size = size;
		if (data.length >= pad(size)) {
		    this.data = data;
		} else {
		    this.data = new byte[pad(size)];
		    System.arraycopy(data, 0, this.data, 0, size);
		}
		this.hasData = true;
	}
	
	public static int pad(int size) {
	    return (size + (BLOCK_PADDING-1)) & ~(BLOCK_PADDING-1);
	}

	/**
	 * @return the decrypted byte data
	 * @throws Exception 
	 */
	public byte[] getDecryptedData() throws Exception {
	    if (!encrypted) return getData();
		if (!hasDecryptedData)
			throw new Exception("Decrypted byte data has not been set!");
		
		return decryptedData;
	}

	/**
	 * @param decryptedData the data decrypted from the raw byte data 
	 */
	public void setDecryptedData(byte[] decryptedData, int size) {
	    this.size = size;
        if (decryptedData.length >= pad(size)) {
            this.decryptedData = decryptedData;
        } else {
            this.decryptedData = new byte[pad(size)];
            System.arraycopy(decryptedData, 0, this.decryptedData, 0, size);
        }
		hasDecryptedData = true;
	}
	

	/**
	 * Output this block for debugging
	 */
	@Override
	public String toString() {
		String s = "=> Block type: " + typeId + "; size: " + size + "\n";
		
		if(size > 0) {
			s += "-- Original Block Data --\n";
			s += Util.bytesToString(data, 0, size) + "\n";
			
			if(encrypted) {
				s += "-- Decrypted Block Data --\n";
				s += Util.bytesToString(decryptedData, 0, size) + "\n";
			}
		}

		return s;
	}
}
