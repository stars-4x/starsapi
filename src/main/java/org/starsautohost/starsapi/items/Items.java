package org.starsautohost.starsapi.items;

import static org.starsautohost.starsapi.items.Items.TechCategory.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.starsautohost.starsapi.block.DesignBlock;

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
    public static int ITEM_ARMOR_INDEX = 13;
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
        itemLine.name = parts[2].substring(1, parts[2].length() - 1);
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
    public static Map<Integer, Integer> itemArmors = new HashMap<Integer, Integer>();
    public static Hull[] ships = new Hull[37];  // includes starbases...
    public static Map<String, Integer> itemsByName = new HashMap<String, Integer>();
    public static Map<String, Integer> hullsByName = new HashMap<String, Integer>();
    
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
                    itemsByName.put(itemLine.name, key);
                    if (cat == TechCategory.Armor) {
                        itemArmors.put(key, itemLine.nums[ITEM_ARMOR_INDEX]);
                    } else if ("Croby Sharmor".equals(itemLine.name) || "Langston Shell".equals(itemLine.name)) {
                        itemArmors.put(key, 65);
                    } else if ("Multi Cargo Pod".equals(itemLine.name)) {
                        itemArmors.put(key, 50);
                    }
                } else if (itemLine.category == 15) {
                    Hull ship = new Hull();
                    ship.mass = itemLine.nums[MASS_INDEX];
                    ship.armor = itemLine.nums[ARMOR_INDEX];
                    ship.fuel = itemLine.nums[FUEL_INDEX];
                    ship.engineCount = itemLine.nums[ENGINE_COUNT_INDEX];
                    ship.slotCount = itemLine.nums[SLOT_COUNT_INDEX];
                    ship.slotSizes = new int[ship.slotCount];
                    for (int i = 0; i  < ship.slotCount; i++) {
                        ship.slotSizes[i] = itemLine.nums[ENGINE_COUNT_INDEX + 2*i];
                    }
                    int index = itemLine.nums[ITEM_ID_INDEX];
                    ships[index] = ship;
                    hullsByName.put(itemLine.name, index);
                } else if (itemLine.category == 16) {
                    Hull ship = new Hull();
                    ship.armor = itemLine.nums[ARMOR_INDEX];
                    ship.slotCount = itemLine.nums[SLOT_COUNT_INDEX];
                    int index = itemLine.nums[ITEM_ID_INDEX];
                    ships[index] = ship;
                    hullsByName.put(itemLine.name, index);
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        } finally {
            if (reader != null) reader.close();
            in.close();
        }
    }
    
    static class AbbrevHolder {
        static Map<String, String> itemAbbrev = new HashMap<String, String>();
        static Map<String, String> hullAbbrev = new HashMap<String, String>();
        
        static {
            try {
                initialize();
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
        
        static void initialize() throws IOException {
            InputStream in = Items.class.getResourceAsStream("item-abbrev.properties");
            Properties itemProps = new Properties();
            itemProps.load(in);
            in.close();
            for (Object keyObj : itemProps.keySet()) {
                String key = (String)keyObj;
                String value = itemProps.getProperty(key);
                itemAbbrev.put(condenseString(key), value);
            }
            for (String key : itemsByName.keySet()) {
                itemAbbrev.put(condenseString(key), key);
            }
            Map<String, String> itemsToAdd = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : itemAbbrev.entrySet()) {
                String pluralKey = entry.getKey() + "s";
                if (itemAbbrev.get(pluralKey) == null) {
                    itemsToAdd.put(pluralKey, entry.getValue());
                }
            }
            itemAbbrev.putAll(itemsToAdd);
            in = Items.class.getResourceAsStream("hull-abbrev.properties");
            Properties hullProps = new Properties();
            hullProps.load(in);
            in.close();
            for (Object keyObj : hullProps.keySet()) {
                String key = (String)keyObj;
                String value = hullProps.getProperty(key);
                hullAbbrev.put(condenseString(key), value);
            }
            for (String key : hullsByName.keySet()) {
                hullAbbrev.put(condenseString(key), key);
            }
            Map<String, String> hullsToAdd = new HashMap<String, String>();
            for (Map.Entry<String, String> entry : hullAbbrev.entrySet()) {
                String pluralKey = entry.getKey() + "s";
                if (hullAbbrev.get(pluralKey) == null) {
                    hullsToAdd.put(pluralKey, entry.getValue());
                }
            }
            hullAbbrev.putAll(hullsToAdd);
        }
    }
    
    public static String condenseString(String s) {
        return s.toLowerCase(Locale.ENGLISH).replaceAll("[^0-9a-z]", "");
    }
    
    public static byte getHullIdOfUserString(String s) {
        s = s.trim();
        Integer v = hullsByName.get(s);
        if (v != null) return v.byteValue();
        String name = AbbrevHolder.hullAbbrev.get(condenseString(s));
        if (name == null) throw new IllegalArgumentException("Unknown hull: " + s);
        return hullsByName.get(name).byteValue();
    }
    
    public static DesignBlock.Slot getSlotOfUserString(String s) {
        s = s.trim();
        if (s.isEmpty() || "0".equals(s) || s.startsWith("0 ") || "empty".equalsIgnoreCase(s)) return new DesignBlock.Slot();
        int count = -1;
        int index = s.indexOf(' ');
        if (index > 0) {
            String countString = s.substring(0, index);
            try {
                count = Integer.parseInt(countString);
                s = s.substring(index + 1).trim();
            } catch (NumberFormatException e) {
                // no count
            }
        }
        Integer v = itemsByName.get(s);
        if (v == null) {
            String name = AbbrevHolder.itemAbbrev.get(condenseString(s));
            if (name == null) throw new IllegalArgumentException("Unknown item: " + s);
            v = itemsByName.get(name);
            if (v == null) throw new IllegalArgumentException("Something odd: " + s);
        }
        int vint = v.intValue();
        int category = (vint >> 8);
        int itemId = vint & 0x0F;
        DesignBlock.Slot res = new DesignBlock.Slot();
        res.category = category;
        res.itemId = itemId;
        res.count = count;
        return res;
    }
}
