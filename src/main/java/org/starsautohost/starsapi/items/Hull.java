package org.starsautohost.starsapi.items;

public class Hull {
    public int mass;
    public int armor;
    public int fuel;
    public int engineCount;
    public int slotCount;
    public int[] slotSizes;
    
    @Override
    public String toString() {
        return "Hull [mass=" + mass + ", armor=" + armor + ", fuel=" + fuel + ", engineCount=" + engineCount + ", slotCount=" + slotCount + "]";
    }
}