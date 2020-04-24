package org.starsautohost.starsapi.block;

import java.util.Vector;

import org.starsautohost.starsapi.Util;

public class ProductionQueueChangeBlock extends Block {
	
	public class QueueItem {
		int itemId;
		int count;
		int completePercent;
		int unknownBits;
	}

	int planetId;
	Vector<QueueItem> queueItems = new Vector<QueueItem>();
	
	public ProductionQueueChangeBlock() {
		typeId = BlockType.PRODUCTION_QUEUE_CHANGE;
	}

	@Override
	public void decode() {
		planetId = Util.read16(decryptedData, 0);
		
		// Every 4 bytes from here on out is queue data
		for(int i = 2; i <= decryptedData.length - 4; i+=4) {
			int chunk1 = Util.read16(decryptedData, i); // Read 2
			int chunk2 = Util.read16(decryptedData, i+2); // Read 2
			
			// Build up a QueueItem
			QueueItem item = new QueueItem();

			item.itemId 	= chunk1 >> 10;   // Top 6 - but only uses 4?
			item.count 	  	= chunk1 & 0x3FF; // Bottom 10 bits

			item.completePercent = chunk2 >> 4;  // Top 12 bits
			item.unknownBits     = chunk2 & 0xF; // Bottom 4 bits
			
			// Add to list
			queueItems.add(item);
		}
	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString() {
		String s = "ProductionQueueChangeBlock: ";
		
		s += "Planet ID: " + planetId + "\n";
		s += "Queue items (itemId, count, complete %, unknown):\n";
		
		for(QueueItem queueItem: queueItems) {
			s += queueItem.itemId + "\t";
			s += queueItem.count + "\t";
			s += queueItem.completePercent + "\t";	
			s += queueItem.unknownBits + "\n";
		}
		
		return s;
	}
}
