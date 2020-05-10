package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class ProductionQueueChangeBlock extends ProductionQueue {

	int planetId;
	
	public ProductionQueueChangeBlock() {
		typeId = BlockType.PRODUCTION_QUEUE_CHANGE;
	}

	@Override
	public void decode() {
		planetId = Util.read16(decryptedData, 0) & 0x7FF;  // 11 Bits to match other blocks
		
		// The rest is queue data, decode it starting at byte 2
		decodeQueue(2);
	}

	@Override
	public void encode() {
		byte[] queueBytes = encodeQueue();
		
		byte[] res = new byte[2 + queueBytes.length];
		
		// Add planet ID
		Util.write16(res, 0, planetId & 0x7FF);
		
        // Copy in queue data
        System.arraycopy(queueBytes, 0, res, 2, queueBytes.length);
        
        // Save as decrypted data
        setDecryptedData(res, res.length);
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
