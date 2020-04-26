package org.starsautohost.starsapi.block;

public class DesignChangeBlock extends DesignBlock {

	public boolean delete = false;
	public int designToDelete = -1;
	public boolean bitNotSet = false;
	
	public DesignChangeBlock() {
		typeId = BlockType.DESIGN_CHANGE;
	}

	/**
	 * Based on DesignBlock, but with 2 extra bytes
	 */
	@Override
	public void decode() throws Exception{
		//System.out.println(toRawBlockString());
		if (decryptedData[0] % 16 == 0){ //I think this is correct? I have encountered 64 and 0 so far.
			delete = true;
			designToDelete = decryptedData[1] % 16;
			isStarbase = (decryptedData[1] >> 4) % 2 == 1;
		}
		else{
			byte[] b = decryptedData;
			byte[] bb = new byte[decryptedData.length-2];
			for (int t = 0; t < bb.length; t++){
				bb[t] = b[t+2];
			}
			if ((bb[1] & 0x01) != 0x01) {
	            bb[1] |= 0x01;
	            bitNotSet = true;
	        }
			decryptedData = bb;
			size = size-2;
			super.decode(); //Use the decoding in DesignBlock.java
			decryptedData = b;
			size = size+2;
		}
	}

	@Override
	public String toString(){
		if (delete) return "Delete design "+designToDelete;
		else if (bitNotSet) return "BitNotSet. "+super.toString();
		return super.toString();
	}
	
	@Override
	public void encode() throws Exception{
		throw new Exception("encode() not implemented for DesignChangeBlock.");
	}

}
