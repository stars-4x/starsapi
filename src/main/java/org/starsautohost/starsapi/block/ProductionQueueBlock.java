package org.starsautohost.starsapi.block;

public class ProductionQueueBlock extends ProductionQueue {
	
	/**
	 * Note that this block does not have a planet ID with it like the
	 * {@link ProductionQueueChangeBlock}. It comes directly after the 
	 * {@link PartialPlanetBlock} with its ID. 
	 */
	public ProductionQueueBlock() {
		typeId = BlockType.PRODUCTION_QUEUE;
	}

	@Override
	public void decode() {
		// The whole block queue data, decode it starting at byte 0
		decodeQueue(0);
	}

	@Override
	public void encode() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString() {
		String s = "ProductionQueueBlock: ";
		
		s += "Queue items (itemId, count, complete %, itemType):\n";
		
		for(QueueItem queueItem: queueItems) {
			s += queueItem.itemId + "\t";
			s += queueItem.count + "\t";
			s += queueItem.completePercent + "\t";	
			s += queueItem.itemType + "\n";
		}
		
		return s;
	}
}
