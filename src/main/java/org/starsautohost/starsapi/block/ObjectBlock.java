package org.starsautohost.starsapi.block;

import java.lang.reflect.Constructor;
import java.math.BigInteger;

import org.starsautohost.starsapi.Util;

/**
 * The ObjectBlock is a multipurpose block with several formats:
 *  - Count
 *  - Map object
 *    * Minefield
 *    * Packet or salvage
 *    * Wormhole
 *    * Mystery Trader
 */
public class ObjectBlock extends Block {

	/**
	 * Used to decode each specific object type
	 */
    public abstract class SubObject {
    	public abstract void decodeObject();
    	public abstract void encodeObject();
    	public abstract String objectString();
    }

    public class CountObject extends SubObject {
        // Common attributes of all types
        public int count = -1;

		@Override
		public void decodeObject() {
	        count = Util.read16(decryptedData, 0);
		}

		@Override
		public void encodeObject() {
	        byte[] res = new byte[2];
	        Util.write16(res, 0, count);
	        setDecryptedData(res, 2);
		}

		@Override
		public String objectString() {
			return "Count: " + count;
		}
    }
    
    public abstract class MapObject extends SubObject {
		// These methods are used decode the MapObject type
		public abstract void decodeMapObject();
		public abstract void encodeMapObject();
		public abstract String mapObjectString();
		
        // All map objects have these
    	public int number;
        public int owner;
        public int objectType;
        public int x, y;

		@Override
		public void decodeObject() {
		    int objectId = Util.read16(decryptedData, 0);
		    number = objectId & 0x01FF;
		    owner = (objectId & 0x1E00) >> 9;
		    objectType = objectId >> 13;

	    	x = Util.read16(decryptedData, 2);
			y = Util.read16(decryptedData, 4);
			
			decodeMapObject();
		}

		@Override
		public void encodeObject() {
			encodeMapObject();
		}

		@Override
		public String objectString() {
			return mapObjectString();
		}
    }
	
    // some of the bytes in minefields differ from player to player inexplicably
    public class MinefieldObject extends MapObject {
        // For minefields
        public long mineCount;
    	
		@Override
		public void decodeMapObject() {
			mineCount = Util.read32(decryptedData, 6);
		}

		@Override
		public void encodeMapObject() {
			Util.write32(decryptedData, 6, mineCount);
		}

		@Override
		public String mapObjectString() {
			return "Minefield";
		}

		public void setMinefieldVisible(int playerMask) {
		    Util.write16(decryptedData, 14, Util.read16(decryptedData, 14) | playerMask);
		}

		// 0 standard
		// 1 heavy
		// 2 speed bump
		public int getMinefieldType() {
		    return Util.read8(decryptedData[12]);
		}
		
		public boolean isMinefieldDetonating() {
		    return decryptedData[13] == 1;	
		}
    }
    
    public class PacketSalvageObject extends MapObject {

		@Override
		public void decodeMapObject() {
			// TODO Auto-generated method stub
		}

		@Override
		public void encodeMapObject() {
			// TODO Auto-generated method stub
		}

		@Override
		public String mapObjectString() {
			return "Packet";
		}
    }
    
    public class WormholeObject extends MapObject {
        // For wormholes
        public int wormholeId;
        public int targetId;
        // 16 bits, 1 per player
        public int beenThroughBits;
        // 16 bits, 1 per player
        public int canSeeBits;
    	
		@Override
		public void decodeMapObject() {
			wormholeId = Util.read16(decryptedData, 0) % 4096;
			beenThroughBits = Util.read16(decryptedData, 8);
			canSeeBits = Util.read16(decryptedData, 10);
			targetId = Util.read16(decryptedData, 12) % 4096;
			//System.out.println(wormholeId+" "+targetId+" "+x+" "+y);
			//System.out.println(Integer.toBinaryString(beenThrough));
			//System.out.println(Integer.toBinaryString(canSee));
		}
		
		@Override
		public void encodeMapObject() {
			// TODO Auto-generated method stub
		}
		
		@Override
		public String mapObjectString() {
			return "Wormhole#"+number+" "+x+","+y+" -> " + ",ID:"+wormholeId+",Dest:"+targetId+ " "+beenThroughBits+" "+canSeeBits;
		}
		
		
		public boolean isWormholeBeenThrough(int playerMask) {
	        return isWormhole() && ((Util.read16(decryptedData, 10) & playerMask) != 0);
		}

		public void setWormholeBeenThrough(int playerMask) {
		    Util.write16(decryptedData, 10, Util.read16(decryptedData, 10) | playerMask);
		}

		public void setWormholeVisible(int playerMask) {
		    Util.write16(decryptedData, 8, Util.read16(decryptedData, 8) | playerMask);
		}
    }
    
    public class MysterTraderObject extends MapObject {
        // For Mystery Trader (MT)
        public int xDest, yDest;
        public int warp;
        // 16 bits, 1 for each potential player. Player 1 is lowest bit (I think),
        // Player 16 is highest
        public int metBits;
        // Each bit here represents a specific item that the MT is carrying
        public int itemBits;
        public int turnNo;
        
		@Override
		public void decodeMapObject() {
			xDest = Util.read16(decryptedData, 6);
			yDest = Util.read16(decryptedData, 8);
			warp = decryptedData[10] & 0xF;  // Bottom four bits
			metBits = Util.read16(decryptedData, 12);
			itemBits = Util.read16(decryptedData, 14);
			turnNo = Util.read16(decryptedData, 16);
			//System.out.println(toStringMt());
			//System.out.println(getMetPlayers());
			//System.out.println(toString());
		}

		@Override
		public void encodeMapObject() {
			// TODO Auto-generated method stub
		}
		
		@Override
		public String mapObjectString() {
			return "MT#"+number+" "+x+","+y+" -> "+xDest+","+yDest+" "+warp+" "+metBits+" "+itemBits+" "+turnNo+" "+getPartName();
		}


		public double getDeltaX(){
			double dx = xDest-x;
			double dy = Math.abs(yDest-y);
			return dx/dy; //(Math.abs(dx)+Math.abs(dy));
		}
		public double getDeltaY(){
			double dx = Math.abs(xDest-x);
			double dy = yDest-y;
			return -dy/dx; //(Math.abs(dx)+Math.abs(dy));
		}
		
		public String getPartName() {
			BigInteger i = new BigInteger(""+itemBits);
			if (i.testBit(0)) return "Multi Cargo Pod";
			if (i.testBit(1)) return "Multi Function Pod";
			if (i.testBit(2)) return "Langston Shield";
			if (i.testBit(3)) return "Mega Poly Shell";
			if (i.testBit(4)) return "Alien Miner";
			if (i.testBit(5)) return "Hush-a-Boom";
			if (i.testBit(6)) return "Anti Matter Torpedo";
			if (i.testBit(7)) return "Multi Contained Munition";
			if (i.testBit(8)) return "Mini Morph";
			if (i.testBit(9)) return "Enigma Pulsar";
			if (i.testBit(10)) return "Genesis Device";
			if (i.testBit(11)) return "Jump Gate";
			if (i.testBit(12)) return "Ship";
			if (itemBits == 0) return "Research";
			return "";
		}
		
		public String getMetPlayers(){
			String s = "";
			BigInteger i = new BigInteger(""+metBits);
			for (int t = 0; t < 16; t++){
				if (i.testBit(t)) s += "Player #"+(t+1)+"\n";
			}
			return s;
		}
    }
    
    public class UnknownObject extends MapObject {
		@Override
		public void decodeMapObject() {}
		@Override
		public void encodeMapObject() {}
		@Override
		public String mapObjectString() {
			return "Unknown object type: " + typeId;
		}
    }
    
    public SubObject subObject;
    
	public ObjectBlock() {
		typeId = BlockType.OBJECT;
	}
	
	// 0 = minefield, 1 = packet/salvage, 2 = wormhole, 3 = MT
    public class MapObjectType {
    	public static final int MINEFIELD = 0;
    	public static final int PACKET_OR_SALVAGE = 1;
    	public static final int WORMHOLE = 2;
    	public static final int MYSTERY_TRADER = 3;

    	public static final int UNKNOWN_OBJECT_TYPE = -1;
    }
    
	protected Class<? extends MapObject> getSubObjectClass(int type) {
		switch(type) {
		case MapObjectType.MINEFIELD: 
			return MinefieldObject.class; 
		case MapObjectType.PACKET_OR_SALVAGE: 
			return PacketSalvageObject.class; 		
		case MapObjectType.WORMHOLE:
			return WormholeObject.class;
		case MapObjectType.MYSTERY_TRADER:
			return MysterTraderObject.class;
		}
		
		return UnknownObject.class;
	}
	
	public void createSubObject(Class<? extends SubObject> subObjectClass) throws Exception {
		// Weird way you have to create a sub-class
		Constructor<?> ctor = subObjectClass.getDeclaredConstructor(subObjectClass.getEnclosingClass());
		subObject = (SubObject) ctor.newInstance(this);
	}
	
	@Override
	public void decode() throws Exception {
		// Determine which sub-object to create
		Class<? extends SubObject> subObjectClass;
	    
		// A CountObject only has size 2
		if (size == 2) {
	        subObjectClass = CountObject.class;
		}
	    // All others are MapObject
	    else {
	    	// Pre-decode object type
		    int objectType = Util.read16(decryptedData, 0) >> 13;
			subObjectClass = getSubObjectClass(objectType);
	    }
	    
		// Create sub-object and decode
		createSubObject(subObjectClass);
		subObject.decodeObject();
	}

	@Override
	public void encode() {
		subObject.encodeObject();
	}
	
	@Override
	public String toString() {
		String prefix = "ObjectBlock, " + subObject.getClass().getSimpleName() + "; ";

		return prefix + subObject.objectString() + "\n";
	}
	
	public boolean isCounter() {
	    return subObject instanceof CountObject;
	}

	public boolean isMinefield() {
	    return subObject instanceof MinefieldObject;
	}

	public boolean isPacketOrSalvage() {
	    return subObject instanceof PacketSalvageObject;
	}

	public boolean isWormhole() {
	    return subObject instanceof WormholeObject;
	}
	
	public boolean isMysteryTrader() {
	    return subObject instanceof MysterTraderObject;
	}
	
	public int getMapObjectId() {
		// Cast to Map object - it better not be a Count object!
		MapObject mapObject = (MapObject) subObject;
	    
		int res = mapObject.number;
	    res += mapObject.owner << 9;
	    res += mapObject.objectType << 13;
	    
	    return res;
	}
}
