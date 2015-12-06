package org.starsautohost.starsapi.items;

public class Ship {
    public int mass;
    public int armor;
    public int fuel;
    public int engineCount;
    public int slotCount;
    
    @Override
    public String toString() {
        return "Ship [mass=" + mass + ", armor=" + armor + ", fuel=" + fuel + ", engineCount=" + engineCount + ", slotCount=" + slotCount + "]";
    }
}