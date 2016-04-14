package org.starsautohost.starsapi.tools;

import java.awt.Point;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.starsautohost.starsapi.block.*;
import org.starsautohost.starsapi.encryption.Decryptor;
import org.starsautohost.starsapi.tools.GalaxyViewer.MapFileData;

public class PlayerState {

	protected List<Block> mBlocks, lastMBlocks, xBlocks;
	private MapFileData map;
	public HashMap<Integer,FleetBlock> fleetBlocks = new HashMap<Integer,FleetBlock>();
	private HashMap<Integer,PlanetBlock> planetBlocks = new HashMap<Integer,PlanetBlock>();
	private HashMap<Integer,DesignChangeBlock> designChangesShips = new HashMap<Integer,DesignChangeBlock>();
	private HashMap<Integer,DesignChangeBlock> designChangesStarbases = new HashMap<Integer,DesignChangeBlock>();
	public HashMap<Integer,Vector<Waypoint>> waypoints = new HashMap<Integer,Vector<Waypoint>>();
	private FileHeaderBlock mFileHeader = null;
	
	public PlayerState(File mFile, File xFile, MapFileData map) throws Exception{
		this.map = map;
		mBlocks = new Decryptor().readFile(mFile.getAbsolutePath());
		lastMBlocks = getLastMBlocks(mBlocks);
		if (mBlocks.size() != lastMBlocks.size()) System.out.println("MBlocks reduced from "+mBlocks.size()+" to "+lastMBlocks.size());
		xBlocks = new Decryptor().readFile(xFile.getAbsolutePath());
		waypoints = WaypointBlock.getFleetBlocks(lastMBlocks);
		System.out.println("Waypoints: "+waypoints.size());
		for (Block b : lastMBlocks){
			if (b instanceof FleetBlock){
				FleetBlock fleet = (FleetBlock)b;
				fleetBlocks.put(fleet.fleetNumber, fleet);
				//System.out.print(fleet.fleetNumber+" ");
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
		int debugFleet = -2; //52; //55; //79; //-1; //55;
		for (Block b : xBlocks){
			//System.out.println(b.getClass().getName());
			if (b instanceof FleetSplitBlock){
				FleetSplitBlock split = (FleetSplitBlock)b;
				int fleetNr = getNextAvailableFleetNr(waypoints);
				if (debugFleet == -1 || debugFleet == split.fleetNumber || debugFleet == fleetNr){
					System.out.println("Split: "+split.fleetNumber+" -> "+fleetNr);
				}
				Vector<Waypoint> v = waypoints.get(split.fleetNumber);
				Vector<Waypoint> clone = new Vector<Waypoint>();
				for (Waypoint w : v){
					clone.addElement(w.cloneWaypoint());
				}
				waypoints.put(fleetNr, clone);
				
			}
			if (b instanceof FleetsMergeBlock){
				FleetsMergeBlock m = (FleetsMergeBlock)b;
				for (int fleetNr : m.fleetsToMerge){
					waypoints.remove(fleetNr);
				}
				if (debugFleet == -1 || debugFleet == m.fleetNumber || m.fleetsToMerge.contains(debugFleet)){
					System.out.println("Merge: "+m.fleetNumber+" "+m.fleetsToMerge);
				}
			}
			if (b instanceof WaypointChangeTaskBlock){
				WaypointChangeTaskBlock task = (WaypointChangeTaskBlock)b;
				Vector<Waypoint> v = waypoints.get(task.fleetNumber);
				if (v == null){
					if (debugFleet == task.fleetNumber){
						System.out.println(task.getClass().getName()+" for "+task.fleetNumber);
					}
					String error = "Error finding waypoints for fleet # "+(task.fleetNumber+1);
					//System.out.println(error);
					throw new Exception(error);
					//System.out.println("# " + (task.fleetNumber+1));
					//continue;
					//v = new Vector<Waypoint>();
					//waypoints.put(task.fleetNumber, v);
				}
				//System.out.print(".");
				if (task instanceof WaypointDeleteBlock){
					if (debugFleet == -1 || debugFleet == task.fleetNumber){
						System.out.println("Delete: "+task.fleetNumber+" ("+task.wayPointNr+")"); //+v.size());
					}
					v.removeElementAt(task.wayPointNr);
				}
				else if (task instanceof WaypointAddBlock){
					if (debugFleet == -1 || debugFleet == task.fleetNumber){
						System.out.println("Add: "+task.fleetNumber);
					}
					v.add(task.wayPointNr,task);
				}
				else{
					if (debugFleet == -1 || debugFleet == task.fleetNumber){
						System.out.println("Change: "+task.fleetNumber);
					}
					v.set(task.wayPointNr,task);
				}
				//System.out.println(task);
			}
			else if (b instanceof DesignChangeBlock){
				DesignChangeBlock d = (DesignChangeBlock)b;
				if (d.delete){
					if (d.isStarbase) designChangesStarbases.remove(d.designToDelete);
					else{
						designChangesShips.remove(d.designToDelete);
						//System.out.println("Deleting design "+d.designToDelete);
						for (int id : fleetBlocks.keySet()){
							FleetBlock f = fleetBlocks.get(id);
							if (f.shipCount[d.designToDelete] > 0){
								boolean onlyThis = true;
								for (int t = 0; t < f.shipCount.length; t++){
									if (t == d.designToDelete) continue;
									if (f.shipCount[t] > 0){
										onlyThis = false;
										break;
									}
								}
								if (onlyThis){
									System.out.println("Deleting fleet "+id+" due to design deletion.");
									waypoints.remove(id);
								}
							}
						}
					}
				}
				else{
					if (d.isStarbase) designChangesStarbases.put(d.designNumber,d);
					else designChangesShips.put(d.designNumber,d);
				}
				//System.out.println(d.toStringOld());
				//System.out.println(d.toString());
				//System.out.println(d.designNumber+" "+d.isStarbase+" "+d.delete+" "+d.designToDelete+" "+designChangesStarbases.size());
			}
			else{
				//System.out.println(b.getClass().getName());
			}
		}
	}
	
	private Vector<Block> getLastMBlocks(List<Block> mBlocks) {
		Vector<Block> latestFileBlocks = new Vector<Block>();
		for (Block b : mBlocks){
			if (b instanceof FileHeaderBlock){
				latestFileBlocks.clear(); //Turns skipped. Start fresh with latest file.
			}
			latestFileBlocks.addElement(b);
		}
		return latestFileBlocks;
	}

	private int getNextAvailableFleetNr(HashMap<Integer, Vector<Waypoint>> waypoints2) throws Exception{
		for (int t = 0; t < 512; t++){ //Get next available fleet number
			Vector<Waypoint> vv = waypoints.get(t);
			if (vv == null) return t;
		}
		throw new Exception("No available fleet nr found.");
	}

	/**
	 * If file is not ok, will return a String with the error message.
	 * Else returns null
	 */
	public String sanitize(){
		System.out.println("Starting sanitizing x-file for turn "+mFileHeader.turn);
		for (Integer fleetId : waypoints.keySet()){
			Vector<Waypoint> tasks = waypoints.get(fleetId);
			//System.out.println("# "+(fleetId+1)+": "+tasks.size());
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
		for (Integer designId : designChangesShips.keySet()){
			DesignChangeBlock d = designChangesShips.get(designId);
			//System.out.println(d.toString());
			//System.out.println(d.toStringOld());
			if (d.colonizerModuleBug){
				System.out.println("Empty colonizer module bug on new fleet design # "+(d.designNumber+1));
				return "Empty colonizer module bug on new fleet design # "+(d.designNumber+1);
			}
		}
		for (Integer designId : designChangesStarbases.keySet()){
			DesignChangeBlock d = designChangesStarbases.get(designId);
			//System.out.println(d.toString());
			//System.out.println(d.toStringOld());
			if (d.spaceDocBug){
				System.out.println("Space Doc bug on new starbase design # "+(d.designNumber+1));
				return "Space Doc bug on new starbase design # "+(d.designNumber+1);
			}
		}
		return null;
	}
}
