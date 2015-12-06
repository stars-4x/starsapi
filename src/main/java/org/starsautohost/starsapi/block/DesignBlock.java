package org.starsautohost.starsapi.block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.items.Items;

public class DesignBlock extends Block {
    public boolean isFullDesign;
    public boolean isTransferred;
    public boolean isStarbase;
    public int designNumber;
    public int hullId;
    public int pic;
    public int mass; // for full designs, this is calculated
    public int fuelCapacity; // calculated
    public int armor;
    public int slotCount;
    public int turnDesigned;
    public long totalBuilt;
    public long totalRemaining;
    public List<Slot> slots = new ArrayList<Slot>();
    
    public byte[] nameBytes;
    
//    public 
    
	public DesignBlock() {
		typeId = BlockType.DESIGN;
	}

	@Override
	public void decode() throws Exception {
	    if ((decryptedData[0] & 3) != 3) {
	        throw new Exception("Unexpected design first byte: " + this);
	    }
        if ((decryptedData[0] & 0xF8) != 0) {
            throw new Exception("Unexpected design first byte: " + this);
        }
        isFullDesign = (decryptedData[0] & 0x04) == 0x04;
        if ((decryptedData[1] & 0x02) != 0) {
            throw new Exception("Unexpected design second byte: " + this);
        }
        if ((decryptedData[1] & 0x01) != 0x01) {
            throw new Exception("Unexpected design second byte: " + this);
        }
        isTransferred = (decryptedData[1] & 0x80) == 0x80;
        isStarbase = (decryptedData[1] & 0x40) == 0x40;
        designNumber = (decryptedData[1] & 0x3C) >> 2;
        hullId = decryptedData[2] & 0xFF;
        fuelCapacity = Items.ships[hullId].fuel;
        pic = decryptedData[3] & 0xFF;
        int index; 
        if (isFullDesign) {
            if (isStarbase) mass = 0;
            else mass = Items.ships[hullId].mass;
            armor = Util.read16(decryptedData, 4); 
            slotCount = decryptedData[6] & 0xFF;
            turnDesigned = Util.read16(decryptedData, 7);
            totalBuilt = Util.read32(decryptedData, 9);
            totalRemaining = Util.read32(decryptedData, 13);
            index = 17;
            slots.clear();
            for (int i = 0; i < slotCount; i++) {
                Slot slot = new Slot();
                slot.category = Util.read16(decryptedData, index);
                index += 2;
                slot.itemId = Util.read8(decryptedData[index++]);
                slot.count = Util.read8(decryptedData[index++]);
                slots.add(slot);
                if (slot.count > 0) {
                    int key = (slot.category << 8) | (slot.itemId & 0xFF);
                    mass += slot.count * Items.itemMasses.get(key);
                    if (slot.category == Items.TechCategory.Mechanical.getMask()) {
                        if (slot.itemId == 5) fuelCapacity += slot.count * 250;
                        if (slot.itemId == 6) fuelCapacity += slot.count * 500;
                    }
                    if (slot.category == Items.TechCategory.Electrical.getMask()) {
                        if (slot.itemId == 16) fuelCapacity += slot.count * 200;
                    }
                }
            }
        } else {
            mass = Util.read16(decryptedData, 4);
            index = 6;
        }
        int nameLen = decryptedData[index];
        nameBytes = new byte[1 + nameLen];
        System.arraycopy(decryptedData, index, nameBytes, 0, 1 + nameLen);
        index += 1 + nameLen;
        if (index != size) {
            throw new Exception("Unexpected design size: " + this);
        }
	}
	
	public void calculateMassAndFuelCapacity() {
        if (isStarbase) {
            if (isFullDesign) mass = 0;
            fuelCapacity = 0;
        } else {
            if (isFullDesign) mass = Items.ships[hullId].mass;
            fuelCapacity = Items.ships[hullId].fuel;
        }
        if (isFullDesign) {
            for (Slot slot : slots) {
                if (slot.count > 0) {
                    int key = (slot.category << 8) | (slot.itemId & 0xFF);
                    mass += slot.count * Items.itemMasses.get(key);
                    if (slot.category == Items.TechCategory.Mechanical.getMask()) {
                        if (slot.itemId == 5) fuelCapacity += slot.count * 250;
                        if (slot.itemId == 6) fuelCapacity += slot.count * 500;
                    }
                    if (slot.category == Items.TechCategory.Electrical.getMask()) {
                        if (slot.itemId == 16) fuelCapacity += slot.count * 200;
                    }
                }
            }
        }
	}

	@Override
	public void encode() {
	    calculateMassAndFuelCapacity();
	    byte[] data;
	    if (isFullDesign) {
	        data = new byte[4 + 13 + 4*slotCount + nameBytes.length];
	    } else {
	        data = new byte[6 + nameBytes.length];
	    }
	    if (isFullDesign) data[0] = 7;
	    else data[0] = 3;
	    data[1] = 1;
	    data[1] |= designNumber << 2;
	    if (isTransferred) data[1] |= 0x80;
	    if (isStarbase) data[1] |= 0x40;
	    data[2] = (byte)hullId;
	    data[3] = (byte)pic;
	    int index;
	    if (isFullDesign) {
	        Util.write16(data, 4, armor);
	        data[6] = (byte)slotCount;
	        Util.write16(data, 7, turnDesigned);
            Util.write32(data, 9, totalBuilt);
            Util.write32(data, 13, totalRemaining);
            index = 17;
            for (Slot slot : slots) {
                Util.write16(data, index, slot.category);
                index += 2;
                data[index++] = (byte)slot.itemId;
                data[index++] = (byte)slot.count;
            }
	    } else {
	        Util.write16(data, 4, mass);
	        index = 6;
	    }
        System.arraycopy(nameBytes, 0, data, index, nameBytes.length);
        setDecryptedData(data, data.length);
        setData(data, data.length);
        encrypted = false;
	}
	
	public static boolean isCompatible(DesignBlock block1, DesignBlock block2) {
        if (block1.isTransferred != block2.isTransferred) return false;
        if (block1.hullId != block2.hullId) return false;
        if (block1.pic != block2.pic) return false;
        if (block1.mass != block2.mass) return false;
        if (block1.isFullDesign && block2.isFullDesign) {
            if (block1.armor != block2.armor) return false;
            if (block1.slotCount != block2.slotCount) return false;
            // ignore construction counts
            for (int i = 0; i < block1.slotCount; i++) {
                DesignBlock.Slot slot1 = block1.slots.get(i);
                DesignBlock.Slot slot2 = block2.slots.get(i);
                if (slot1.count != slot2.count) return false;
                if (slot1.count == 0) continue;
                if (slot1.category != slot2.category) return false;
                if (slot1.itemId != slot2.itemId) return false;
            }
            if (!Arrays.equals(block1.nameBytes, block2.nameBytes)) return false;
        }
        return true;
    }
	
	public static class Slot {
	    public int category;
	    public int itemId;
	    public int count;

	    @Override
        public String toString() {
            return "Slot [category=" + category + ", itemId=" + itemId + ", count=" + count + "]";
        }
	}

}
