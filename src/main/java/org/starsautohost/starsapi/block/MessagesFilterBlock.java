package org.starsautohost.starsapi.block;

public class MessagesFilterBlock extends Block {

	public MessagesFilterBlock() {
		typeId = BlockType.MESSAGES_FILTER;
	}

	@Override
	public void decode() {
		// TODO Auto-generated method stub

	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}

	public static MessagesFilterBlock newEmptyMessagesFilterBlock() {
	    MessagesFilterBlock block = new MessagesFilterBlock();
	    block.setDecryptedData(new byte[49], 49);
	    return block;
	}
	
}
