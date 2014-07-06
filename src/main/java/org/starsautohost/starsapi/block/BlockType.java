package org.starsautohost.starsapi.block;

import java.util.HashMap;

/**
 * Taken from http://wiki.starsautohost.org/wiki/Technical_Information
 */
public class BlockType {
	
	/**
	 * Here are all the Block Type IDs
	 */
	public static int FILE_FOOTER = 0;
	public static int MANUAL_SMALL_LOAD_UNLOAD_TASK = 1;
	public static int MANUAL_MEDIUM_LOAD_UNLOAD_TASK = 2;
	public static int WAYPOINT_DELETE = 3;
	public static int WAYPOINT_ADD = 4;
	public static int WAYPOINT_CHANGE_TASK = 5;
	public static int PLAYER = 6;
	public static int PLANETS = 7;
	public static int FILE_HEADER = 8; // (unencrypted)
	public static int FILE_HASH = 9;
	public static int WAYPOINT_REPEAT_ORDERS = 10;
	public static int UNKNOWN_BLOCK_11 = 11;
	public static int EVENTS = 12;
	public static int PLANET = 13;
	public static int PARTIAL_PLANET = 14;
	public static int UNKNOWN_BLOCK_15 = 15;
	public static int FLEET = 16;
	public static int PARTIAL_FLEET = 17;
	public static int UNKNOWN_BLOCK_18 = 18;
	public static int WAYPOINT_TASK = 19;
	public static int WAYPOINT = 20;
	public static int FLEET_NAME = 21;
	public static int UNKNOWN_BLOCK_22 = 22;
	public static int MOVE_SHIPS = 23;
	public static int FLEET_SPLIT = 24;
	public static int MANUAL_LARGE_LOAD_UNLOAD_TASK = 25;
	public static int DESIGN = 26;
	public static int DESIGN_CHANGE = 27;
	public static int PRODUCTION_QUEUE = 28;
	public static int PRODUCTION_QUEUE_CHANGE = 29;
	public static int BATTLE_PLAN = 30;
	public static int BATTLE = 31; // (content isn't decoded yet)
	public static int COUNTERS = 32;
	public static int MESSAGES_FILTER = 33;
	public static int RESEARCH_CHANGE = 34;
	public static int PLANET_CHANGE = 35;
	public static int CHANGE_PASSWORD = 36;
	public static int FLEETS_MERGE = 37;
	public static int PLAYERS_RELATION_CHANGE = 38;
	public static int BATTLE_CONTINUATION = 39; // (content isn't decoded yet)
	public static int MESSAGE = 40;
	public static int AI_H_FILE_RECORD = 41;
	public static int SET_FLEET_BATTLE_PLAN = 42;
	public static int OBJECT = 43;
	public static int RENAME_FLEET = 44;
	public static int PLAYER_SCORES = 45;
	public static int SAVE_AND_SUBMIT = 46;
	
	// Default
	public static int UNKNOWN_BAD = -1;
	
	
	/**
	 * Here we create a map that maps the block typeId to a class
	 * 
	 * It's the best we can do without tuples or preprocessor definitions
	 */
	private static final HashMap<Integer, Class<? extends Block> > typeToClass;
	static {
		typeToClass = new HashMap<Integer, Class<? extends Block> >();
		typeToClass.put(FILE_FOOTER, FileFooterBlock.class);
		typeToClass.put(FILE_FOOTER, FileFooterBlock.class);
		typeToClass.put(MANUAL_SMALL_LOAD_UNLOAD_TASK, ManualSmallLoadUnloadTaskBlock.class);
		typeToClass.put(MANUAL_MEDIUM_LOAD_UNLOAD_TASK, ManualMediumLoadUnloadTaskBlock.class);
		typeToClass.put(WAYPOINT_DELETE, WaypointDeleteBlock.class);
		typeToClass.put(WAYPOINT_ADD, WaypointAddBlock.class);
		typeToClass.put(WAYPOINT_CHANGE_TASK, WaypointChangeTaskBlock.class);
		typeToClass.put(PLAYER, PlayerBlock.class);
		typeToClass.put(PLANETS, PlanetsBlock.class);
		typeToClass.put(FILE_HEADER, FileHeaderBlock.class);
		typeToClass.put(FILE_HASH, FileHashBlock.class);
		typeToClass.put(WAYPOINT_REPEAT_ORDERS, WaypointRepeatOrdersBlock.class);
		typeToClass.put(UNKNOWN_BLOCK_11, UnknownBlock11.class);
		typeToClass.put(EVENTS, EventsBlock.class);
		typeToClass.put(PLANET, PlanetBlock.class);
		typeToClass.put(PARTIAL_PLANET, PartialPlanetBlock.class);
		typeToClass.put(UNKNOWN_BLOCK_15, UnknownBlock15.class);
		typeToClass.put(FLEET, FleetBlock.class);
		typeToClass.put(PARTIAL_FLEET, PartialFleetBlock.class);
		typeToClass.put(UNKNOWN_BLOCK_18, UnknownBlock18.class);
		typeToClass.put(WAYPOINT_TASK, WaypointTaskBlock.class);
		typeToClass.put(WAYPOINT, WaypointBlock.class);
		typeToClass.put(FLEET_NAME, FleetNameBlock.class);
		typeToClass.put(UNKNOWN_BLOCK_22, UnknownBlock22.class);
		typeToClass.put(MOVE_SHIPS, MoveShipsBlock.class);
		typeToClass.put(FLEET_SPLIT, FleetSplitBlock.class);
		typeToClass.put(MANUAL_LARGE_LOAD_UNLOAD_TASK, ManualLargeLoadUnloadTaskBlock.class);
		typeToClass.put(DESIGN, DesignBlock.class);
		typeToClass.put(DESIGN_CHANGE, DesignChangeBlock.class);
		typeToClass.put(PRODUCTION_QUEUE, ProductionQueueBlock.class);
		typeToClass.put(PRODUCTION_QUEUE_CHANGE, ProductionQueueChangeBlock.class);
		typeToClass.put(BATTLE_PLAN, BattlePlanBlock.class);
		typeToClass.put(BATTLE, BattleBlock.class);
		typeToClass.put(COUNTERS, CountersBlock.class);
		typeToClass.put(MESSAGES_FILTER, MessagesFilterBlock.class);
		typeToClass.put(RESEARCH_CHANGE, ResearchChangeBlock.class);
		typeToClass.put(PLANET_CHANGE, PlanetChangeBlock.class);
		typeToClass.put(CHANGE_PASSWORD, ChangePasswordBlock.class);
		typeToClass.put(FLEETS_MERGE, FleetsMergeBlock.class);
		typeToClass.put(PLAYERS_RELATION_CHANGE, PlayersRelationChangeBlock.class);
		typeToClass.put(BATTLE_CONTINUATION, BattleContinuationBlock.class);
		typeToClass.put(MESSAGE, MessageBlock.class);
		typeToClass.put(AI_H_FILE_RECORD, AIHFileRecordBlock.class);
		typeToClass.put(SET_FLEET_BATTLE_PLAN, SetFleetBattlePlanBlock.class);
		typeToClass.put(OBJECT, ObjectBlock.class);
		typeToClass.put(RENAME_FLEET, RenameFleetBlock.class);
		typeToClass.put(PLAYER_SCORES, PlayerScoresBlock.class);
		typeToClass.put(SAVE_AND_SUBMIT, SaveAndSubmitBlock.class);
	};
	

	public static Class<? extends Block> getBlockClass(int typeId) throws Exception {
		if(typeId < 0 || typeId >= typeToClass.size())
			throw new Exception("Unsupported Block class for type: " + typeId);
		
		return typeToClass.get(typeId);
	}
}
