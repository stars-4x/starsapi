package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class FileFooterBlock extends Block {

	public int checksum;
	
	public FileFooterBlock() {
		typeId = BlockType.FILE_FOOTER;
	}

	@Override
	public void decode() throws Exception {
		// .h# files have no checksum in their footer
		if(size >= 2)
			checksum = Util.read16(data, 0);
		
        encrypted = false;
	}

	@Override
	public void encode() throws Exception {
		// TODO Auto-generated method stub
		
	}

	public static FileFooterBlock zeroFileFooterBlockForHstOrMFile() {
	    FileFooterBlock res = new FileFooterBlock();
	    res.setDecryptedData(new byte[] { 0, 0 }, 2);
	    return res;
	}
	
	public static FileFooterBlock emptyFileFooterBlockForHFile() {
	    FileFooterBlock res = new FileFooterBlock();
	    res.setDecryptedData(new byte[0], 0);
	    return res;
	}

}
