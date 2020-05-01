package org.starsautohost.starsapi.block;

import java.util.Vector;

import org.starsautohost.starsapi.Util;

public abstract class ProductionQueue extends Block {

	public class QueueItem {
		public int itemId;
		public int count;
		public int completePercent;
		public int itemType;  // 2 - standard (mine/factory etc), 4 - custom (ship/starbase design)
	}
	
	/*
	 * If itemType == 2 these are the item IDs:
		0 Mines (Auto Build)
		1 Factories (Auto Build)
		2 Defenses (Auto Build)
		3 Alchemy (Auto Build)
		4 Min Terraform (Auto Build)
		5 Max Terraform (Auto Build)
		6 Mineral Packets (Auto Build)
		7 Factory
		8 Mine
		9 Defenses
		11 Mineral Alchemy
		12 UNKNOWN - Some sort of invisible item, always has non-zero completion. maybe excess resources?
		14 Ironium Mineral Packet
		15 Boranium Mineral Packet
		16 Germanium Mineral Packet
		17 Mixed Mineral Packet
		27 Planetary Scanner
	 */

	// Order of QueueItems is the order in Production Queue
	public Vector<QueueItem> queueItems = new Vector<QueueItem>();
	
	/**
	 * Decode a list of queue items
	 * 
	 * @param startIdx The index in the byte array where the queue starts
	 */
	protected void decodeQueue(int startIdx) {
		// Every 4 bytes from here on out is queue data
		for(int i = startIdx; i <= decryptedData.length - 4; i+=4) {
			int chunk1 = Util.read16(decryptedData, i); // Read 2
			int chunk2 = Util.read16(decryptedData, i+2); // Read 2
			
			// Build up a QueueItem
			QueueItem item = new QueueItem();

			item.itemId 	= chunk1 >> 10;   // Top 6 - but only uses 4?
			item.count 	  	= chunk1 & 0x3FF; // Bottom 10 bits

			item.completePercent = chunk2 >> 4;  // Top 12 bits
			item.itemType     = chunk2 & 0xF; // Bottom 4 bits
			
			// Add to list
			queueItems.add(item);
		}
	}
}
