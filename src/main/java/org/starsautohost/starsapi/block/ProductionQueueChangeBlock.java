package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class ProductionQueueChangeBlock extends ProductionQueue {

	int planetId;
	
	public ProductionQueueChangeBlock() {
		typeId = BlockType.PRODUCTION_QUEUE_CHANGE;
	}

	@Override
	public void decode() {
		planetId = Util.read16(decryptedData, 0);
		
		// The rest is queue data, decode it starting at byte 2
		decodeQueue(2);
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
			s += queueItem.itemType + "\n";
		}
		
		return s;
	}
}
