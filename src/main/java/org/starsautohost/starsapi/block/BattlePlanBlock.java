package org.starsautohost.starsapi.block;

import java.util.ArrayList;
import java.util.List;

public class BattlePlanBlock extends Block {

	public BattlePlanBlock() {
		typeId = BlockType.BATTLE_PLAN;
	}

	@Override
	public void decode() {
		// TODO Auto-generated method stub

	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}
	
	static final byte[][] DEFAULT_BATTLE_PLANS_BYTES = { 
	    {0, 4, 19, 2, 5, (byte)179, 45, 113, (byte)222, 90}, 
	    {16, 4, 50, 2, 8, (byte)186, 69, 80, (byte)194, (byte)161, (byte)141, 65, (byte)146}, 
	    {32, 3, 67, 2, 8, (byte)188, 30, 30, 91, 50, (byte)215, 38, (byte)146}, 
	    {48, 1, 5, 2, 4, (byte)194, 100, (byte)220, 40}, 
	    {64, 0, 1, 2, 5, (byte)178, 52, (byte)213, (byte)218, 38}
	};

	public static List<BattlePlanBlock> defaultBattlePlansForPlayer(int playerNumber) {
	    List<BattlePlanBlock> blocks = new ArrayList<BattlePlanBlock>();
	    for (byte[] bytes : DEFAULT_BATTLE_PLANS_BYTES) {
	        byte[] clonedBytes = bytes.clone();
	        clonedBytes[0] += playerNumber;
	        BattlePlanBlock block = new BattlePlanBlock();
	        block.setDecryptedData(clonedBytes, clonedBytes.length);
	        blocks.add(block);
	    }
	    return blocks;
	}
}
