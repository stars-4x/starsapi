package org.starsautohost.starsapi.tools;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.DesignChangeBlock;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.FleetBlock;
import org.starsautohost.starsapi.block.PlanetBlock;
import org.starsautohost.starsapi.block.Waypoint;
import org.starsautohost.starsapi.block.WaypointAddBlock;
import org.starsautohost.starsapi.block.WaypointBlock;
import org.starsautohost.starsapi.block.WaypointChangeTaskBlock;
import org.starsautohost.starsapi.block.WaypointDeleteBlock;
import org.starsautohost.starsapi.encryption.Decryptor;
import org.starsautohost.starsapi.tools.GalaxyViewer.MapFileData;

public class PlayerState {

	private List<Block> mBlocks, xBlocks;
	private MapFileData map;
	private HashMap<Integer,FleetBlock> fleetBlocks = new HashMap<Integer,FleetBlock>();
	private HashMap<Integer,PlanetBlock> planetBlocks = new HashMap<Integer,PlanetBlock>();
	private HashMap<Integer,DesignChangeBlock> designChanges = new HashMap<Integer,DesignChangeBlock>();
	private HashMap<Integer,Vector<Waypoint>> waypoints = new HashMap<Integer,Vector<Waypoint>>();
	private FileHeaderBlock mFileHeader = null;
	
	public PlayerState(File xFile, File mFile, MapFileData map) throws Exception{
		this.map = map;
		mBlocks = new Decryptor().readFile(mFile.getAbsolutePath());
		xBlocks = new Decryptor().readFile(xFile.getAbsolutePath());
		waypoints = WaypointBlock.getFleetBlocks(mBlocks);
		System.out.println(waypoints.size());
		for (Block b : mBlocks){
			if (b instanceof FleetBlock){
				FleetBlock fleet = (FleetBlock)b;
				fleetBlocks.put(fleet.fleetNumber, fleet);
			}
			if (b instanceof PlanetBlock){
				PlanetBlock planet = (PlanetBlock)b;
				planetBlocks.put(planet.planetNumber, planet);
			}
			if (b instanceof FileHeaderBlock){
				FileHeaderBlock fh = (FileHeaderBlock)b;
				mFileHeader = fh;
			}
		}
		for (Block b : xBlocks){
			if (b instanceof WaypointChangeTaskBlock){
				WaypointChangeTaskBlock task = (WaypointChangeTaskBlock)b;
				Vector<Waypoint> v = waypoints.get(task.fleetNumber);
				if (task instanceof WaypointDeleteBlock){
					v.removeElementAt(task.wayPointNr);
				}
				else if (task instanceof WaypointAddBlock){
					v.add(task.wayPointNr,task);
				}
				else{
					v.set(task.wayPointNr,task);
				}
				//System.out.println(task);
			}
			else if (b instanceof DesignChangeBlock){
				DesignChangeBlock d = (DesignChangeBlock)b;
				designChanges.put(d.designNumber,d);
				//System.out.println(d);
			}
			else{
				//System.out.println(b.getClass().getName());
			}
		}
	}
	
	/**
	 * If file is not ok, will return a String with the error message.
	 * Else returns null
	 */
	public String sanitize(){
		System.out.println("Starting sanitizing x-file for turn "+mFileHeader.turn);
		for (Integer fleetId : waypoints.keySet()){
			Vector<Waypoint> tasks = waypoints.get(fleetId);
			System.out.println("# "+(fleetId+1)+": "+tasks.size());
			for (int t = 1; t < tasks.size(); t++){
				Waypoint task = tasks.elementAt(t);
				int oldx = tasks.elementAt(t-1).x;
				int	oldy = tasks.elementAt(t-1).y;
				
				if (task.x == oldx){
					if (task.y == oldy) continue; //Same location
					System.out.println("Checking vertical movement for fleet # "+(fleetId+1));
					if (task.warp > 4){ //Vertical movement detected!
						if (Math.abs(task.y-oldy) <= 16) continue; //Assume interception of enemy fleet at high warp.
						Point p1 = new Point(oldx,oldy);
						Point p2 = new Point(task.x,task.y);
						if (map.planetNrs.get(p1) != null && map.planetNrs.get(p2) != null){
							System.out.println("Planets at both locations. Ok.");
						}
						else{
							return "Vertical movement detected for fleet # "+(fleetId+1)+" at warp "+task.warp;
						}
					}
				}
			}
		}
		for (Integer designId : designChanges.keySet()){
			DesignChangeBlock d = designChanges.get(designId);
			//System.out.println(d.toString());
			//System.out.println(d.toStringOld());
			if (d.colonizerModuleBug){
				System.out.println("Empty colonizer module bug on new fleet design # "+(d.designNumber+1));
				return "Empty colonizer module bug on new fleet design # "+(d.designNumber+1);
			}
		}
		return null;
	}
}
