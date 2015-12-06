package org.starsautohost.starsapi.block;

public class FileFooterBlock extends Block {

	public FileFooterBlock() {
		typeId = BlockType.FILE_FOOTER;
	}

	@Override
	public void decode() throws Exception {
		// TODO Auto-generated method stub

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
