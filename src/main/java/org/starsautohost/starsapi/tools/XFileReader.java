package org.starsautohost.starsapi.tools;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.DesignChangeBlock;
import org.starsautohost.starsapi.block.FleetBlock;
import org.starsautohost.starsapi.block.PlanetBlock;
import org.starsautohost.starsapi.block.Waypoint;
import org.starsautohost.starsapi.block.WaypointAddBlock;
import org.starsautohost.starsapi.block.WaypointBlock;
import org.starsautohost.starsapi.block.WaypointChangeTaskBlock;
import org.starsautohost.starsapi.block.WaypointDeleteBlock;
import org.starsautohost.starsapi.block.WaypointTaskBlock;
import org.starsautohost.starsapi.encryption.Decryptor;
import org.starsautohost.starsapi.tools.GalaxyViewer.MapFileData;

/**
 * @author Runar
 *
 */
public class XFileReader {

	private MapFileData map;
	private List<Block> xBlocks, mBlocks;
	private HashMap<Integer,FleetBlock> fleetBlocks = new HashMap<Integer,FleetBlock>();
	private HashMap<Integer,PlanetBlock> planetBlocks = new HashMap<Integer,PlanetBlock>();
	private HashMap<Integer,DesignChangeBlock> designChanges = new HashMap<Integer,DesignChangeBlock>();
	private HashMap<Integer,Vector<Waypoint>> currentWaypoints = new HashMap<Integer,Vector<Waypoint>>();
 	private HashMap<Integer,Vector<Waypoint>> fleetTasks = new HashMap<Integer,Vector<Waypoint>>();
	
	public static void main(String[] args) throws Exception{
		try{
			if (args.length < 1){
				System.out.println("Usage: <xfile>");
				System.out.println("m-file and map-file must exist in same directory as x-file!");
				System.exit(1);
			}
			File xFile = new File(args[0]);
			if (xFile.exists() == false) xFile = new File(new File(".").getParentFile(),args[0]);
			if (xFile.exists() == false) throw new Exception("Could not find x-file "+args[0]);

			String[] el = xFile.getName().split("\\.");
			File mFile = new File(xFile.getParentFile(),el[0]+"."+el[1].replace("x","m").replace("X","M"));
			File mapFile = new File(xFile.getParentFile(),el[0]+"."+el[1].substring(0,1).replace("x","map").replace("X","MAP"));
			if (mFile.exists() == false) throw new Exception("Could not find m-file "+mFile.getAbsolutePath());
			if (mapFile.exists() == false){
				if (xFile.getParentFile() != null){ //In case of old subdirectories, check parent directory for map file.
					File m = new File(xFile.getParentFile().getParentFile(),el[0]+"."+el[1].substring(0,1).replace("x","map").replace("X","MAP"));
					if (m.exists()) mapFile = m;
				}
				if (mapFile.exists() == false) throw new Exception("Could not find map-file "+mapFile.getAbsolutePath());
			}
			MapFileData map = GalaxyViewer.parseMapFileData(mapFile);
			PlayerState s = new PlayerState(mFile,xFile,map);
			String error = s.sanitize();
			if (error != null){
				System.err.println(error);
				System.exit(1);
			}
			System.out.println("X-file OK!");
			System.exit(0);
		}
		catch(Exception ex){
			System.err.println(ex.toString());
			ex.printStackTrace(System.out);
			System.exit(1);
		}
	}
	
	public static void printHistogram(List<Block> list) {
		HashMap<Class<?>,Integer> hm = new HashMap<Class<?>, Integer>();
		for (Block b : list){
			Integer i = hm.get(b.getClass());
			if (i == null) i = 0;
			hm.put(b.getClass(),i+1);
		}
		for (Class<?> c : hm.keySet()){
			System.out.println(c.getName()+"\t"+hm.get(c));
		}
	}
}
