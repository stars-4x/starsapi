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
	
	// creates an empty hull with, in the case of ships, the Trans-Star 10 engine
	public void convertToFullDesignForHstFile() {
	    if (isFullDesign) return;
	    isFullDesign = true;
	    armor = Items.ships[hullId].armor;
	    slotCount = Items.ships[hullId].slotCount;
	    slots.clear();
	    if (!isStarbase) {
	        Slot engineSlot = new Slot();
	        engineSlot.category = Items.TechCategory.Engine.getMask();
	        engineSlot.itemId = 9; // Trans-Star 10
	        engineSlot.count = Items.ships[hullId].engineCount;
	        slots.add(engineSlot);
	    } else {
	        slots.add(new Slot());
	    }
	    for (int i = 1; i < slotCount; i++) {
	        slots.add(new Slot());
	    }
	    encode();
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
	
	public int getPrt() {
	    if (isTransferred) return -1;
	    if (hullId == 3) return PlayerBlock.PRT.IS; // Super Freighter
	    if (hullId == 8) return PlayerBlock.PRT.WM; // Battle Cruiser
        if (hullId == 10) return PlayerBlock.PRT.WM; // Dreadnought
        if (hullId == 12) return PlayerBlock.PRT.SS; // Rogue
        if (hullId == 14) return PlayerBlock.PRT.HE; // Mini-Colony Ship
        if (hullId == 18) return PlayerBlock.PRT.SS; // Stealth Bomber
        if (hullId == 25) return PlayerBlock.PRT.IS; // Fuel Transport
        if (hullId == 27) return PlayerBlock.PRT.SD; // Mini Mine Layer
        if (hullId == 28) return PlayerBlock.PRT.SD; // Super Mine Layer
        if (hullId == 31) return PlayerBlock.PRT.HE; // Meta Morph
        if (hullId == 36) return PlayerBlock.PRT.AR; // Death Star
        if (!isFullDesign) return -1;
        boolean isOrSd = true;
        for (Slot slot : slots) {
            if (slot.count == 0) continue;
            if (slot.category == Items.TechCategory.Orbital.getMask()) {
                if (slot.itemId == 1 || slot.itemId == 4 || slot.itemId == 5 || slot.itemId == 6) return PlayerBlock.PRT.IT;
                if (slot.itemId == 7 || slot.itemId == 8 || slot.itemId == 10 || slot.itemId == 11) return PlayerBlock.PRT.PP;
                if (slot.itemId == 13 || slot.itemId == 14 || slot.itemId == 15) return PlayerBlock.PRT.PP;
            } else if (slot.category == Items.TechCategory.BeamWeapon.getMask()) {
                if (slot.itemId == 2) return PlayerBlock.PRT.IS;
                if (slot.itemId == 14) return PlayerBlock.PRT.WM;
                if (slot.itemId == 16) return PlayerBlock.PRT.WM;
            } else if (slot.category == Items.TechCategory.Bomb.getMask()) {
                if (slot.itemId == 9) return PlayerBlock.PRT.CA;
            } else if (slot.category == Items.TechCategory.MiningRobot.getMask()) {
                if (slot.itemId == 7) return PlayerBlock.PRT.CA;
            } else if (slot.category == Items.TechCategory.MineLayer.getMask()) {
                if (slot.itemId != 1 && slot.itemId != 7) return PlayerBlock.PRT.SD;
                if (slot.itemId == 7) isOrSd = true;
            } else if (slot.category == Items.TechCategory.Mechanical.getMask()) {
                if (slot.itemId == 1) return PlayerBlock.PRT.AR;
            } else if (slot.category == Items.TechCategory.Electrical.getMask()) {
                if (slot.itemId == 0 || slot.itemId == 3) return PlayerBlock.PRT.SS;
                if (slot.itemId == 8 || slot.itemId == 11) return PlayerBlock.PRT.IS;
                if (slot.itemId == 13) return PlayerBlock.PRT.HE;
                if (slot.itemId == 14) return PlayerBlock.PRT.SD;
                if (slot.itemId == 15) return PlayerBlock.PRT.IS;
                if (slot.itemId == 16) return PlayerBlock.PRT.IT;
            } else if (slot.category == Items.TechCategory.Shield.getMask()) {
                if (slot.itemId == 3) return PlayerBlock.PRT.IS;
                if (slot.itemId == 4) return PlayerBlock.PRT.SS;
            } else if (slot.category == Items.TechCategory.Scanner.getMask()) {
                if (slot.itemId == 5 || slot.itemId == 6 || slot.itemId == 14) return PlayerBlock.PRT.SS;
            } else if (slot.category == Items.TechCategory.Armor.getMask()) {
                if (slot.itemId == 6) return PlayerBlock.PRT.IS;
                if (slot.itemId == 7) return PlayerBlock.PRT.SS;
            }
        }
        if (isOrSd) return -2;
        return -1;
	}

	public boolean hasNrse() {
	    if (isTransferred) return false;
	    if (!isFullDesign) return false;
	    for (Slot slot : slots) {
	        if (slot.count == 0) continue;
	        if (slot.category == Items.TechCategory.Engine.getMask()) {
	            if (slot.itemId == 7) return true;
	        }
	    }
	    return false;
	}

	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    sb.append(isStarbase ? "Starbase" : "Ship");
	    sb.append("Design " + designNumber);
	    if (isTransferred) sb.append(", Transferred");
	    sb.append(", Hull " + hullId);
	    sb.append(", Pic " + pic);
	    sb.append(", Mass " + mass);
	    if (isFullDesign) {
	        sb.append(", Armor " + armor);
	        sb.append(", Count " + totalRemaining + "/" + totalBuilt);
	        for (Slot slot : slots) {
                if (slot.count > 0) {
                    sb.append("\n");
                    sb.append(slot);
                }
	        }
	    }
	    return sb.toString();
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
