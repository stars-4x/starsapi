package org.starsautohost.starsapi.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.Util;

public class BattlePlanBlock extends Block {
	
	public int ownerPlayerId;
	public int planId;
	
	// Tactics:
	// 0 - Disengage
	// 1 - Disengage if challenged
	// 2 - Minimize damage to self	
	// 3 - Maximize net damage
	// 4 - Maximize damage ratio
	// 5 - Maximize damage
	public int tactic;
	
	public boolean dumpCargo;

	// Targets:
	// 0 - None/Disengage	
	// 1 - Any	
	// 2 - Starbase	
	// 3 - Armed Ships	
	// 4 - Bombers/Freighters	
	// 5 - Unarmed Ships	
	// 6 - Fuel Transports	
	// 7 - Freighters
	public int primaryTarget;
	public int secondaryTarget;
	
	// Attack Who:
	//    0 - Nobody
	//    1 - Enemies
	//    2 - Neutral & Enemies
	//    3 - Everyone
	// 4-19 - Player ID minus 4 
	public int attackWho;

	public String name;
	
	// Inferred
	public boolean deleted;

	
	public BattlePlanBlock() {
		typeId = BlockType.BATTLE_PLAN;
	}

	@Override
	public void decode() {
		int word0 = Util.read16(decryptedData, 0);
		ownerPlayerId = word0 & 0xF;        // Lower 4 bits
		planId =       (word0 >> 4)  & 0xF; // Next 4 bits
		tactic =       (word0 >> 8)  & 0xF; // etc. 
		dumpCargo =   ((word0 >> 15) == 0) ? false : true;  // Top bit

		int word1 = Util.read16(decryptedData, 2);
		primaryTarget = word1 & 0xF; 
		secondaryTarget = (word1 >> 4) & 0xF;
		attackWho = word1 >> 8;  // Upper 8 bits
		
		// Test for a deleted block, if total length is 4 bytes
		if(decryptedData.length == 4)
			deleted = true;
		else
			deleted = false;
		
		// String length is the first byte of the array
		if(!deleted) {
			byte[] nameBytes = Arrays.copyOfRange(decryptedData, 4, decryptedData.length);
			name = Util.decodeStarsString(nameBytes);
		}
	}

	@Override
	public void encode() {
		// Find how long we need by encoding the plan name first
		byte[] nameBytes = Util.encodeStarsString(name);
		
		// Preallocate
		byte[] res = new byte[4 + nameBytes.length];
		
		int dump = dumpCargo ? 1 : 0;
		int word0 = (ownerPlayerId & 0x0F) |
				((planId & 0x0F) << 4) |
				((tactic & 0x0F) << 8) |
				((dump & 0x1)    << 15);
		
		int word1 = (primaryTarget & 0xF) |
				(secondaryTarget & 0xF) << 4 |
				(attackWho & 0xFF) << 8;
		
		// Write data
		Util.write16(res, 0, word0);
		Util.write16(res, 2, word1);
		
        // Copy in message data
		if(nameBytes.length > 0)
			System.arraycopy(nameBytes, 0, res, 4, nameBytes.length);
        
        // Save as decrypted data
        setDecryptedData(res, res.length);
	}

	@Override
	public String toString() {
		String s = "BattlePlanBlock:\n";
		
		s += "(ownerId, planId, tactic, dump, deleted";
		
		if(!deleted)
			s += ", primaryTarget, secondaryTarget, attackWho, name";
		
		s += "):\n";
		
		s += ownerPlayerId + "\t";
		s += planId + "\t";
		s += tactic + "\t";
		s += dumpCargo + "\t";
		s += deleted + "\t";
		
		if(!deleted) {
			s += primaryTarget + "\t";
			s += secondaryTarget + "\t";
			s += attackWho + "\t";
			s += name + "\t";
		}
		s += "\n";
		
		return s;
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
