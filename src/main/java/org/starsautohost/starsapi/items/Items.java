package org.starsautohost.starsapi.items;

import static org.starsautohost.starsapi.items.Items.TechCategory.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Items {
    public static enum TechCategory {
        Empty(0),
        Engine(1),
        Scanner(2),
        Shield(4),
        Armor(8),
        BeamWeapon(0x10),
        Torpedo(0x20),
        Bomb(0x40),
        MiningRobot(0x80),
        MineLayer(0x100),
        Orbital(0x200),
        Planetary(0x400),
        Electrical(0x800),
        Mechanical(0x1000);
        
        private final int mask;

        private TechCategory(int mask) {
            this.mask = mask;
        }
        
        public int getMask() {
            return mask;
        }
    }
    
    public static TechCategory[] uneditedModCategories = {
        null, Orbital, BeamWeapon, Torpedo,
        Bomb, null /*Terraforming*/, Planetary, MiningRobot,
        MineLayer, Mechanical, Electrical, Shield,
        Scanner, Armor, Engine, null /*ShipHull*/, null /*StarbaseHull*/
    };
    
    public static int ITEM_ID_INDEX = 0;
    public static int MASS_INDEX = 7;
    public static int ARMOR_INDEX = 15;
    public static int FUEL_INDEX = 14;
    public static int SLOT_START_INDEX = 16;
    public static int ENGINE_COUNT_INDEX = 17;
    public static int SLOT_COUNT_INDEX = 48;
    
    public static class ItemLine {
        public int category;
        public int index;
        public String name;
        public int[] nums;
    }
    
    private static ItemLine readItemLine(String line) {
        String[] parts = line.split(",");
        int numsLen = parts.length - 3;
        ItemLine itemLine = new ItemLine();
        itemLine.category = Integer.parseInt(parts[0]);
        itemLine.index = Integer.parseInt(parts[1]);
        itemLine.name = parts[2];
        itemLine.nums = new int[numsLen];
        for (int i = 0; i < numsLen; i++) {
            String part = parts[3 + i];
            if (!part.isEmpty()) {
                itemLine.nums[i] = Integer.parseInt(part);
            }
        }
        return itemLine;
    }
    
    public static Map<Integer, Integer> itemMasses = new HashMap<Integer, Integer>();
    public static Ship[] ships = new Ship[37];  // includes starbases...
    
    static {
        try {
            initialize();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    
    public static void initialize() throws IOException {
        InputStream in = Items.class.getResourceAsStream("UNEDITED.MOD");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
            String line;
            while ((line = reader.readLine()) != null) {
                ItemLine itemLine = readItemLine(line);
                TechCategory cat = uneditedModCategories[itemLine.category];
                if (cat != null) {
                    int key = (cat.getMask() << 8) | ((itemLine.index - 1) & 0xFF);
                    itemMasses.put(key, itemLine.nums[MASS_INDEX]);
                } else if (itemLine.category == 15) {
                    Ship ship = new Ship();
                    ship.mass = itemLine.nums[MASS_INDEX];
                    ship.armor = itemLine.nums[ARMOR_INDEX];
                    ship.fuel = itemLine.nums[FUEL_INDEX];
                    ship.engineCount = itemLine.nums[ENGINE_COUNT_INDEX];
                    ship.slotCount = itemLine.nums[SLOT_COUNT_INDEX];
                    int index = itemLine.nums[ITEM_ID_INDEX];
                    ships[index] = ship;
                } else if (itemLine.category == 16) {
                    Ship ship = new Ship();
                    ship.armor = itemLine.nums[ARMOR_INDEX];
                    ship.slotCount = itemLine.nums[SLOT_COUNT_INDEX];
                    int index = itemLine.nums[ITEM_ID_INDEX];
                    ships[index] = ship;
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } finally {
            if (reader != null) reader.close();
            in.close();
        }
    }
}
