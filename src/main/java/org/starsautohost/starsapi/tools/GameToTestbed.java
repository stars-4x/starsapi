package org.starsautohost.starsapi.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.starsautohost.starsapi.block.BattlePlanBlock;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
import org.starsautohost.starsapi.block.CountersBlock;
import org.starsautohost.starsapi.block.DesignBlock;
import org.starsautohost.starsapi.block.FileFooterBlock;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.FleetBlock;
import org.starsautohost.starsapi.block.FleetNameBlock;
import org.starsautohost.starsapi.block.MessagesFilterBlock;
import org.starsautohost.starsapi.block.ObjectBlock;
import org.starsautohost.starsapi.block.PartialFleetBlock;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.block.PlanetBlock;
import org.starsautohost.starsapi.block.PlanetsBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.block.ProductionQueueBlock;
import org.starsautohost.starsapi.block.WaypointBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class GameToTestbed {

    private String filenameBase;
    private int playerMask = 0; // which players we have M files for current turn 
    private int gameTurn;
    private long gameId;
    private byte unknownHeaderFlagBits; // required to make games work
    private int numPlanets;
    private int numPlayers;
    private Map<String, List<Block>> files = new HashMap<String, List<Block>>();
    private PlayerInfo[] players = new PlayerInfo[16];
    private Map<Integer, PlanetInfo> planets = new TreeMap<Integer, PlanetInfo>();
    private Map<Integer, ObjectBlock> objects = new TreeMap<Integer, ObjectBlock>();
    
    private static class PlayerInfo {
        PlayerBlock playerBlock;
        DesignInfo[] shipDesigns = new DesignInfo[16];
        Map<Integer, FleetInfo> fleets = new TreeMap<Integer, FleetInfo>();
        DesignInfo[] starbaseDesigns = new DesignInfo[10];
        List<BattlePlanBlock> battlePlans = new ArrayList<BattlePlanBlock>();
        
        // during post-processing
//        PlayerBlock partialPlayerBlock;
        int prt;
        boolean hasNrse;
        DesignBlock[] shipDesignBlocks = new DesignBlock[16];
        DesignBlock[] starbaseDesignBlocks = new DesignBlock[16];
        DesignBlock[] fullShipDesignBlocks = new DesignBlock[16];
        DesignBlock[] fullStarbaseDesignBlocks = new DesignBlock[16];
        int planetCount;
    }
    
    private class PlanetInfo {
        PlanetBlock bestHst;
        PartialPlanetBlock bestM;
        PartialPlanetBlock bestH;
        PlanetBlock definitive;
        List<Block> productionQueue = new ArrayList<Block>();
        PartialPlanetBlock best;
        int bestTurn;
        PartialPlanetBlock bestWithEnvironment;
        int bestWithEnvironmentTurn;
        PartialPlanetBlock bestWithStarbase;
        int bestWithStarbaseTurn;

        public void consider(PartialPlanetBlock block, int turn) {
            if (definitive != null) return;
            if (block.hasStarbase && (bestWithStarbase == null || bestWithStarbaseTurn < turn)) {
                bestWithStarbase = block;
                bestWithStarbaseTurn = turn;
            }
            if (block.canSeeEnvironment() && (bestWithEnvironment == null || bestWithEnvironmentTurn < turn)) {
                bestWithEnvironment = block;
                bestWithEnvironmentTurn = turn;
            }
            if (best == null || bestTurn < turn) {
                best = block;
                bestTurn = turn;
                return;
            }
            if (bestTurn == turn) {
                if (block.weirdBit) best.weirdBit = true; // ???
                if (block.isInUseOrRobberBaron && !best.isInUseOrRobberBaron) {
                    best = block;
                    bestTurn = turn;
                } else if (block.canSeeEnvironment() && !best.canSeeEnvironment()) {
                    best = block;
                    bestTurn = turn;
                } else if (block.canSeeEnvironment() == best.canSeeEnvironment() && block.hasStarbase && !best.hasStarbase) {
                    best = block;
                    bestTurn = turn;
                }
            }
        }
        
        public PlanetBlock mergeHst() throws Exception {
            if (bestHst == null) mergeInternal();
            return bestHst;
        }

        public PartialPlanetBlock mergeM() throws Exception {
            if (bestM == null) mergeInternal();
            return bestM;
        }

        public PartialPlanetBlock mergeH() throws Exception {
            if (bestH == null) mergeInternal();
            return bestH;
        }

        public void mergeInternal() throws Exception {
            if (definitive != null) {
                bestHst = definitive;
                bestM = (PartialPlanetBlock) Block.copy(definitive);
                bestM.convertToPartialPlanetForMFile();
                bestH = (PartialPlanetBlock) Block.copy(definitive);
                bestH.convertToPartialPlanetForHFile(gameTurn);
                return;
            }
            boolean mergedLatest = false;
            boolean mergedLatestWithStarbase = false;
            PartialPlanetBlock res;
            int turn;
            if (!best.canSeeEnvironment() && bestWithEnvironment != null) {
                res = bestWithEnvironment;
                turn = bestWithEnvironmentTurn;
                if (res.owner != best.owner || res.hasStarbase != best.hasStarbase || res.starbaseDesign != best.starbaseDesign) {
                    res.owner = best.owner;
                    res.weirdBit = best.weirdBit;
                    res.hasStarbase = best.hasStarbase;
                    res.starbaseDesign = best.starbaseDesign;
                    mergedLatest = true;
                }
            } else {
                res = best;
                turn = bestTurn;
            }
            // Starbases are tricky since they may be missing because destroyed, or because cloaked.
            // We use later data even if no starbase; only add in starbase data at least as new.
            if (!res.hasStarbase && bestWithStarbase != null && turn <= bestWithStarbaseTurn) {
                res.hasStarbase = true;
                res.starbaseDesign = bestWithStarbase.starbaseDesign;
                if (turn < bestWithStarbaseTurn) mergedLatestWithStarbase = true;
            }
            if (mergedLatest || mergedLatestWithStarbase) {
                System.out.println("Warning: merging planet data for planet #" + res.planetNumber);
                System.out.println("Result: " + printPlanet(res));
                if (mergedLatest) System.out.println("Merged in: " + printPlanet(best));
                if (mergedLatestWithStarbase) System.out.println("Merged in: " + printPlanet(bestWithStarbase));
            }
            res.encode();
            bestM = (PartialPlanetBlock) Block.copy(res);
            bestM.convertToPartialPlanetForMFile();
            bestH = (PartialPlanetBlock) Block.copy(res);
            bestH.convertToPartialPlanetForHFile(gameTurn);
            res = (PartialPlanetBlock) Block.copy(res);
            bestHst = PartialPlanetBlock.convertToPlanetBlockForHstFile(res);
        }
    }
    
    static String printPlanet(PartialPlanetBlock block) {
        String res = "Turn:" + block.turn;
        if (block.owner >= 0) res += ", Owner:" + block.owner;
        if (block.canSeeEnvironment()) res += ", Environment";
        if (block.hasStarbase) res += ", Starbase:"+ block.starbaseDesign;
        return res;
    }
    
    private class DesignInfo {
        int player;
        DesignBlock bestResult;
        DesignBlock latestInMFile;
        int latestInMFileTurn = -1;
        DesignBlock latestFullInMFile;
        int latestFullInMFileTurn = -1;
        List<DesignBlock> inHFile = new ArrayList<DesignBlock>();

        public DesignInfo(int player) {
            this.player = player;
        }

        public void consider(DesignBlock block, boolean isMFile, int turn) {
            if (isMFile) {
                if (turn > latestInMFileTurn) {
                    latestInMFile = block;
                    latestInMFileTurn = turn;
                }
                if (block.isFullDesign) {
                    if (turn == latestInMFileTurn) {
                        latestInMFile = block;
                        latestInMFileTurn = turn;
                    }
                    if (turn > latestFullInMFileTurn) {
                        latestFullInMFile = block;
                        latestFullInMFileTurn = turn;
                    }
                }
            } else {
                inHFile.add(block);
            }
        }
        
        public DesignBlock merge() {
            if (bestResult != null) return bestResult;
            else {
                bestResult = mergeInternal();
                // built/remaining is more information than should be revealed
                if ((playerMask & (1 << player)) == 0) {
                    bestResult.totalBuilt = 0;
                    bestResult.totalRemaining = 0;
                }
                bestResult.encode();
                return bestResult;
            }
        }
        
        private DesignBlock mergeInternal() {
            if (latestInMFile != null) {
                if (latestInMFile.isFullDesign) return latestInMFile;
                if (latestFullInMFile != null && DesignBlock.isCompatible(latestInMFile, latestFullInMFile)) {
                    return latestFullInMFile;
                }
                for (DesignBlock hFileBlock : inHFile) {
                    if (hFileBlock.isFullDesign && DesignBlock.isCompatible(latestInMFile, hFileBlock)) {
                        return hFileBlock;
                    }
                }
                return latestInMFile;
            }
            return inHFile.get(0);
        }

    }
    
    private class FleetInfo {
        FleetBlock definitive;
        List<Block> waypointsAndNameBlocks = new ArrayList<Block>();
        PartialFleetBlock bestPartial;
        List<Block> mergedFullResult;
        byte kind;
        
        public void consider(PartialFleetBlock block) throws Exception {
            byte blockKind = block.kindByte;
            if (blockKind == PartialFleetBlock.FULL_KIND) {
                throw new Exception("Unexpected full fleet info");
            }
            if (blockKind > kind) {
                block = (PartialFleetBlock) Block.copy(block);
                block.convertToPartialFleet();
                block.encode();
                bestPartial = block;
                kind = blockKind;
            }
        }
        
        public List<Block> mergeFull(DesignBlock[] designs) throws Exception {
            if (mergedFullResult != null) return mergedFullResult;
            mergedFullResult = new ArrayList<Block>();
            if (definitive != null) {
                mergedFullResult.add(definitive);
                mergedFullResult.addAll(waypointsAndNameBlocks);
                return mergedFullResult;
            } else {
                PartialFleetBlock block = (PartialFleetBlock) Block.copy(bestPartial);
                FleetBlock fleetBlock = FleetBlock.convertToFleetBlockForHstFile(block, designs);
                mergedFullResult.add(fleetBlock);
                mergedFullResult.add(WaypointBlock.waypointZeroForFleet(fleetBlock));
                return mergedFullResult;
            }
        }
        
        public PartialFleetBlock mergePartial(DesignBlock[] designs) throws Exception {
            if (bestPartial != null) {
                return bestPartial;
            } else {
                bestPartial = (PartialFleetBlock) Block.copy(definitive);
                bestPartial.convertToPartialFleet();
                bestPartial.mass = definitive.calculateMass(designs);
                bestPartial.unknownBitsWithWarp = 16;
                if (waypointsAndNameBlocks.isEmpty() || !(waypointsAndNameBlocks.get(0) instanceof WaypointBlock)) {
                    bestPartial.encode();
                    return bestPartial;
                }
                WaypointBlock waypointBlock = (WaypointBlock) waypointsAndNameBlocks.get(0);
                if (waypointBlock.warp > 10) {
                    // try to distinguish "gating" from "stopped"
                    bestPartial.unknownBitsWithWarp = 0;
                    bestPartial.encode();
                    return bestPartial;
                }
                if (waypointBlock.warp == 0) {
                    bestPartial.encode();
                    return bestPartial;
                }
                int deltaX = waypointBlock.x - bestPartial.x;
                int deltaY = waypointBlock.y - bestPartial.y;
                int largest = Math.max(Math.abs(deltaX), Math.abs(deltaY));
                if (largest > 125) {
                    deltaX = deltaX * 125 / largest;
                    deltaY = deltaY * 125 / largest;
                }
                deltaX += 127;
                deltaY += 127;
                bestPartial.deltaX = deltaX;
                bestPartial.deltaY = deltaY;
                bestPartial.warp = waypointBlock.warp;
                bestPartial.unknownBitsWithWarp = 16;
                bestPartial.encode();
                return bestPartial;
            }
        }
    }
        
    public static void main(String[] args) throws Exception {
        new GameToTestbed().run(args);
    }

    public void run(String[] args) throws Exception {
        String xyFilename = null;
        for (String filename : args) {
            List<Block> blocks;
            try {
                blocks = new Decryptor().readFile(filename);
                files.put(filename, blocks);
            } catch (Exception e) {
                System.out.println("Unable to parse file " + filename + ": " + e);
                e.printStackTrace(System.out);
                return;
            }
            if (isProblem(filename, blocks)) return;
            if (checkXYFile(blocks)) {
                if (xyFilename != null) {
                    System.out.println("Found multiple XY files: " + xyFilename + ", " + filename);
                    return;
                } else {
                    xyFilename = filename;
                }
            }
        }
        if (xyFilename == null) {
            System.out.println("No XY file given");
            return;
        }
        if (!xyFilename.toLowerCase().endsWith(".xy")) {
            System.out.println("Surprising XY filename without .XY: " + xyFilename);
            return;
        }
        filenameBase = xyFilename.substring(0, xyFilename.length() - 3);
        checkGameIdsAndYearsAndPlayers(files);
        
        for (Map.Entry<String, List<Block>> entry : files.entrySet()) {
            List<Block> blocks = entry.getValue();
            new FileProcessor().process(blocks);
        }
        
        postProcess();
        
        createHstFile();
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            if ((playerMask & (1 << playerNumber)) == 0) {
                createMFile(playerNumber);
                createHFile(playerNumber);
            }
        }
    }

    private static boolean isProblem(String filename, List<Block> blocks) {
        if (blocks == null || blocks.size() == 0) {
            System.out.println(filename + " does not parse into block list");
            return true;
        }
        if (blocks.get(0).typeId != BlockType.FILE_HEADER) {
            System.out.println(filename + " does not start with header block");
            return true;
        }
        FileHeaderBlock headerBlock = (FileHeaderBlock)blocks.get(0);
        int fileType = headerBlock.fileType;
        if (fileType == 0) {
            // XY file
            return false;
        }
        if (fileType == 1) {
            // X file, ignored
            return false;
        }
        if (fileType == 2) {
            // HST file
            System.out.println(filename + " is already a HST file");
            return true;
        }
        if (fileType != 4 && fileType != 3) {
            System.out.println(filename + " is not of expected type");
            return true;
        }
        if (fileType == 3) return false;
        return checkProblemHFile(filename, blocks);
    }
    
    private static boolean checkProblemHFile(String filename, List<Block> blocks) {
        // structure of H file
        if (blocks.size() < 4) {
            System.out.println(filename + " does not have expected structure");
            return true;
        }
        if (blocks.get(1).typeId != BlockType.COUNTERS) {
            System.out.println(filename + " does not have expected structure");
            return true;
        }
        CountersBlock counters = (CountersBlock)blocks.get(1);
        int numPlanets = counters.planetCount;
        if (blocks.size() < 3 + numPlanets) {
            System.out.println(filename + " does not have expected structure");
            return true;
        }
        for (int i = 0; i < numPlanets; i++) {
            if (blocks.get(2 + i).typeId != BlockType.PARTIAL_PLANET) {
                System.out.println(filename + " does not have expected structure");
                return true;
            }
        }
        for (int i = 2 + numPlanets; i < blocks.size(); i++) {
            if (blocks.get(i).typeId == BlockType.PARTIAL_PLANET) {
                System.out.println(filename + " does not have expected structure");
                return true;
            }
        }
        return false;
    }
    
    private boolean checkXYFile(List<Block> blocks) throws Exception {
        FileHeaderBlock headerBlock = (FileHeaderBlock) blocks.get(0);
        if (headerBlock.fileType != 0) return false;
        gameId = headerBlock.gameId;
        if (blocks.size() != 3) {
            throw new Exception("Unexpected structure of XY file");
        }
        if (!(blocks.get(1) instanceof PlanetsBlock)) {
            throw new Exception("Unexpected structure of XY file");
        }
        PlanetsBlock planetsBlock = (PlanetsBlock) blocks.get(1);
        numPlanets = planetsBlock.getNumPlanets();
        numPlayers = planetsBlock.getNumPlayers();
        return true;
    }

    private void checkGameIdsAndYearsAndPlayers(Map<String, List<Block>> files) throws Exception {
        gameTurn = -1;
        int[] latestTurnByPlayer = new int[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            latestTurnByPlayer[i] = -1;
        }
        for (List<Block> blocks : files.values()) {
            for (Block block : blocks) {
                if (block instanceof FileHeaderBlock) {
                    FileHeaderBlock headerBlock = (FileHeaderBlock) blocks.get(0);
                    if (headerBlock.gameId != gameId) {
                        throw new Exception("Mismatched game ids");
                    }
                    if (headerBlock.fileType != 3) break;
                    if (headerBlock.turn > latestTurnByPlayer[headerBlock.playerNumber]) {
                        latestTurnByPlayer[headerBlock.playerNumber] = headerBlock.turn;
                    }
                    if (headerBlock.turn > gameTurn) {
                        gameTurn = headerBlock.turn;
                        unknownHeaderFlagBits = headerBlock.unknownBits;
                    }
                }
            }
        }
        if (gameTurn == -1) {
            throw new Exception("No M file found");
        }
        playerMask = 0;
        for (int i = 0; i < numPlayers; i++) {
            if (latestTurnByPlayer[i] == gameTurn) {
                playerMask |= 1 << i;
            }
        }
    }

    private class FileProcessor {
        private int observer = -1;
        private int fileTurn = -1;
        private boolean isMFile = false;
        private PlayerInfo thisPlayerInfo = null;
        private List<Block> mostRecentPlanetOrFleetBlocksList = null;
        private int shipDesignIndex = 0;
        private int starbaseDesignIndex = 0;
        private List<Integer> shipDesignOwner = new ArrayList<Integer>();
        private List<Integer> starbaseDesignOwner = new ArrayList<Integer>();
        private int[][] latestStarbaseYearPlusOne = new int[16][10];

        void process(List<Block> blocks) throws Exception {
            for (Block block : blocks) {
                if (block instanceof FileHeaderBlock) {
                    FileHeaderBlock headerBlock = (FileHeaderBlock)block;
                    observer = headerBlock.playerNumber;
                    fileTurn = headerBlock.turn;
                    if (headerBlock.fileType == 3) {
                        isMFile = true;
                    } else if (headerBlock.fileType != 4) {
                        // not an H file or an M file
                        return;
                    }
                    thisPlayerInfo = null;
                    mostRecentPlanetOrFleetBlocksList = null;
                } else if (block instanceof PlayerBlock) {
                    PlayerBlock playerBlock = (PlayerBlock)block;
                    processPlayerBlock(playerBlock);
                } else if (block instanceof PartialPlanetBlock) {
                    // include PlanetBlock as an extension of PartialPlanetBlock
                    PartialPlanetBlock pblock = (PartialPlanetBlock)block;
                    processPlanetBlock(pblock);
                } else if (block instanceof DesignBlock) {
                    DesignBlock designBlock = (DesignBlock)block;
                    processDesignBlock(designBlock);
                } else if (isMFile && fileTurn == gameTurn) {
                    if (block instanceof PartialFleetBlock) {
                        // include FleetBlock as an extension of PartialFleetBlock
                        PartialFleetBlock fblock = (PartialFleetBlock)block;
                        processFleetBlock(fblock);
                    } else if (block instanceof ObjectBlock) {
                        ObjectBlock oblock = (ObjectBlock)block;
                        if (oblock.isCounter()) continue;
                        processObjectBlock(oblock);
                    } else if (block instanceof BattlePlanBlock) {
                        BattlePlanBlock bpblock = (BattlePlanBlock)block;
                        processBattlePlanBlock(bpblock);
                    } else if (block instanceof WaypointBlock || block instanceof FleetNameBlock || block instanceof ProductionQueueBlock) {
                        // include WaypointTaskBlock as an extension of WaypointBlock
                        if (mostRecentPlanetOrFleetBlocksList != null) {
                            mostRecentPlanetOrFleetBlocksList.add(block);
                        }
                    }
                }
            }
        }

        private void processPlayerBlock(PlayerBlock playerBlock) {
            PlayerInfo playerInfo = players[playerBlock.playerNumber];
            if (playerInfo == null) {
                playerInfo = new PlayerInfo();
                players[playerBlock.playerNumber] = playerInfo;
            }
            if (isMFile && fileTurn == gameTurn && playerBlock.playerNumber == observer) {
                playerInfo.playerBlock = playerBlock;
                thisPlayerInfo = playerInfo;
            } else if ((playerMask & (1 << playerBlock.playerNumber)) != 0) {
                // it's a player we'll see definitively, no need to set playerBlock here
            } else if (playerBlock.fullDataFlag == true) {
                // CAs see redacted "full data"; also dead players get full data
                if (playerInfo.playerBlock == null || playerInfo.playerBlock.fullDataFlag == false || playerInfo.playerBlock.hasEnvironmentInfoOnly()) {
                    playerInfo.playerBlock = playerBlock;
                }
            } else if (playerInfo.playerBlock == null) {
                playerInfo.playerBlock = playerBlock;
            }
            Integer playerNumberObj = Integer.valueOf(playerBlock.playerNumber);
            for (int i = 0; i < playerBlock.shipDesigns; i++) {
                shipDesignOwner.add(playerNumberObj);
            }
            for (int i = 0; i < playerBlock.starbaseDesigns; i++) {
                starbaseDesignOwner.add(playerNumberObj);
            }
        }
        
        private void processBattlePlanBlock(BattlePlanBlock bpblock) {
            thisPlayerInfo.battlePlans.add(bpblock);
        }

        private void processPlanetBlock(PartialPlanetBlock pblock) throws Exception {
            int planetNumber = pblock.planetNumber;
            PlanetInfo planetInfo = planets.get(planetNumber);
            if (planetInfo == null) {
                planetInfo = new PlanetInfo();
                planets.put(planetNumber, planetInfo);
            }
            if (isMFile && fileTurn == gameTurn && pblock.typeId == BlockType.PLANET) {
                planetInfo.definitive = (PlanetBlock)pblock;
                mostRecentPlanetOrFleetBlocksList = planetInfo.productionQueue;
            } else {
                int turn = fileTurn;
                if (pblock.turn > -1) turn = pblock.turn;
                planetInfo.consider(pblock, turn);
                if (pblock.hasStarbase) {
                    int last = latestStarbaseYearPlusOne[pblock.owner][pblock.starbaseDesign];
                    if (turn + 1 > last) latestStarbaseYearPlusOne[pblock.owner][pblock.starbaseDesign] = turn + 1;
                }
            }
        }

        private void processDesignBlock(DesignBlock designBlock) {
            int player;
            DesignInfo[] designs;
            if (designBlock.isStarbase) {
                player = starbaseDesignOwner.get(starbaseDesignIndex++);
                designs = players[player].starbaseDesigns;
            } else {
                player = shipDesignOwner.get(shipDesignIndex++);
                designs = players[player].shipDesigns;
            }
            if (((playerMask & (1 << player)) != 0) && (!isMFile || fileTurn < gameTurn)) {
                // we'll see the definitive one elsewhere
                return;
            }
            DesignInfo designInfo = designs[designBlock.designNumber];
            if (designInfo == null) {
                designInfo = new DesignInfo(player);
                designs[designBlock.designNumber] = designInfo;
            }
            if (designBlock.isStarbase && !isMFile) {
                int turn = latestStarbaseYearPlusOne[player][designBlock.designNumber] - 1;
                designInfo.consider(designBlock, true, turn);
                designInfo.consider(designBlock, false, fileTurn);
            } else {
                designInfo.consider(designBlock, isMFile, fileTurn);
            }
        }
        
        private void processFleetBlock(PartialFleetBlock fleetBlock) throws Exception {
            FleetInfo fleetInfo = players[fleetBlock.owner].fleets.get(fleetBlock.fleetNumber);
            if (fleetInfo == null) {
                fleetInfo = new FleetInfo();
                players[fleetBlock.owner].fleets.put(fleetBlock.fleetNumber, fleetInfo);
            }
            if (isMFile && fileTurn == gameTurn && fleetBlock.typeId == BlockType.FLEET) {
                fleetInfo.definitive = (FleetBlock) fleetBlock;
                mostRecentPlanetOrFleetBlocksList = fleetInfo.waypointsAndNameBlocks;
            } else {
                fleetInfo.consider(fleetBlock);
            }
        }

        private void processObjectBlock(ObjectBlock object) throws Exception {
            if (object.isCounter()) return;
            ObjectBlock formerBlock = objects.get(object.getObjectId());
            if (object.isWormhole()) {
                if (formerBlock == null) {
                    object = (ObjectBlock) Block.copy(object);
                    object.setWormholeVisible(0xFFFF);
                    if (object.isWormholeBeenThrough(playerMask)) {
                        object.setWormholeBeenThrough(0xFFFF);
                    }
                    object.encode();
                    objects.put(object.getObjectId(), object);
                } else {
                    if (formerBlock.isWormholeBeenThrough(playerMask)) return;
                    if (object.isWormholeBeenThrough(playerMask)) {
                        object = (ObjectBlock) Block.copy(object);
                        object.setWormholeVisible(0xFFFF);
                        object.setWormholeBeenThrough(0xFFFF);
                        object.encode();
                        objects.put(object.getObjectId(), object);
                    }
                }
            } else {
                if (formerBlock != null) return;
                if (object.isMinefield()) {
                    object = (ObjectBlock) Block.copy(object);
                    object.setMinefieldVisible(0xFFFF);
                    object.encode();
                }
                objects.put(object.getObjectId(), object);
            }
        }
    }

    private void postProcess() throws Exception {
        createRacesForMissingPlayers();
        setPlanetCountsAndPrtsFromPlanetsAndMinefields();
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            postProcessDesignsForPlayer(playerInfo);
            if (!playerInfo.playerBlock.fullDataFlag || playerInfo.playerBlock.hasEnvironmentInfoOnly()) {
                setPrtAndNrseBasedOnDesigns(playerInfo);
                playerInfo.playerBlock.makeBeefyFullData(playerInfo.prt, playerInfo.hasNrse);
            }
            playerInfo.playerBlock.fleets = playerInfo.fleets.size();
            playerInfo.playerBlock.planets = 0;
            setPlayerRelations(playerNumber, playerInfo);
            playerInfo.playerBlock.encode();
            playerInfo.playerBlock.setData(playerInfo.playerBlock.getDecryptedData(), playerInfo.playerBlock.size);
//            playerInfo.partialPlayerBlock = (PlayerBlock) (Block.copy(playerInfo.playerBlock));
//            playerInfo.partialPlayerBlock.fullDataFlag = false;
            if (playerInfo.battlePlans == null || playerInfo.battlePlans.isEmpty()) {
                playerInfo.battlePlans = BattlePlanBlock.defaultBattlePlansForPlayer(playerNumber);
            }
        }
    }

    private void setPrtAndNrseBasedOnDesigns(PlayerInfo playerInfo) {
        for (DesignBlock designBlock : playerInfo.shipDesignBlocks) {
            if (designBlock == null) continue;
            if (designBlock.hasNrse()) playerInfo.hasNrse = true;
            if (playerInfo.prt < 0) {
                int thisDesignPrt = designBlock.getPrt();
                if (thisDesignPrt >= 0) playerInfo.prt = thisDesignPrt;
                else if (thisDesignPrt == -2) playerInfo.prt = -2;
            }
        }
        if (playerInfo.prt < 0) {
            for (DesignBlock designBlock : playerInfo.starbaseDesignBlocks) {
                if (designBlock == null) continue;
                int thisDesignPrt = designBlock.getPrt();
                if (thisDesignPrt >= 0) {
                    playerInfo.prt = thisDesignPrt;
                    break;
                }
            }
        }
        if (playerInfo.prt == -2) playerInfo.prt = PlayerBlock.PRT.IS; // either IS or SD
        if (playerInfo.prt == -1) playerInfo.prt = PlayerBlock.PRT.JOAT; // anything
    }

    private void setPlanetCountsAndPrtsFromPlanetsAndMinefields() throws Exception {
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            playerInfo.planetCount = 0;
            playerInfo.prt = -1;
        }
        for (PlanetInfo planetInfo : planets.values()) {
            int owner = planetInfo.mergeHst().owner;
            if (owner >= 0 && owner < 16) {
                players[owner].planetCount++;
                if (planetInfo.mergeM().canSeeEnvironment() && planetInfo.mergeM().popEstimate == 0) {
                    players[owner].prt = PlayerBlock.PRT.AR;
                }
            }
        }
        for (ObjectBlock object : objects.values()) {
            if (object.isMinefield()) {
                if (object.getMinefieldType() == 1 || object.isMinefieldDetonating()) {
                    players[object.owner].prt = PlayerBlock.PRT.SD;
                } else if (object.getMinefieldType() == 2) {
                    if (players[object.owner].prt == -1) players[object.owner].prt = -2;
                }
            }
        }
    }

    private void createRacesForMissingPlayers() {
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            if (playerInfo == null) {
                playerInfo = new PlayerInfo();
                playerInfo.playerBlock = PlayerBlock.createUnknownRacePlayerBlock(playerNumber);
                players[playerNumber] = playerInfo;
            }
        }
    }

    private void postProcessDesignsForPlayer(PlayerInfo playerInfo) throws Exception {
        playerInfo.playerBlock.shipDesigns = 0;
        playerInfo.playerBlock.starbaseDesigns = 0;
        for (int i = 0; i < 16; i++) {
            DesignInfo designInfo = playerInfo.shipDesigns[i];
            if (designInfo != null) {
                DesignBlock block = designInfo.merge();
                playerInfo.shipDesignBlocks[i] = block;
                if (block.isFullDesign) {
                    playerInfo.fullShipDesignBlocks[i] = block;
                } else {
                    block = (DesignBlock) Block.copy(block);
                    block.convertToFullDesignForHstFile();
                    playerInfo.fullShipDesignBlocks[i] = block;
                }
                playerInfo.playerBlock.shipDesigns++;
            }
        }
        for (int i = 0; i < 10; i++) {
            DesignInfo designInfo = playerInfo.starbaseDesigns[i];
            if (designInfo != null) {
                DesignBlock block = designInfo.merge();
                playerInfo.starbaseDesignBlocks[i] = block;
                if (block.isFullDesign) {
                    playerInfo.fullStarbaseDesignBlocks[i] = block;
                } else {
                    block = (DesignBlock) Block.copy(block);
                    block.convertToFullDesignForHstFile();
                    playerInfo.fullStarbaseDesignBlocks[i] = block;
                }
                playerInfo.playerBlock.starbaseDesigns++;
            }
        }
    }

    private void setPlayerRelations(int playerNumber, PlayerInfo playerInfo) {
        if ((playerMask & (1 << playerNumber)) != 0) return;
        playerInfo.playerBlock.playerRelations = new byte[numPlayers];
        for (int otherPlayerNumber = 0; otherPlayerNumber < numPlayers; otherPlayerNumber++) {
            if (otherPlayerNumber == playerNumber) {
                playerInfo.playerBlock.playerRelations[otherPlayerNumber] = 0;
            } else if ((playerMask & (1 << otherPlayerNumber)) != 0) {
                // reciprocate with known players
                playerInfo.playerBlock.playerRelations[otherPlayerNumber] = players[otherPlayerNumber].playerBlock.getPlayerRelationsWith(playerNumber);
            } else {
                // friends with all others
                playerInfo.playerBlock.playerRelations[otherPlayerNumber] = 1;
            }
        }
    }

    private void createHstFile() throws Exception {
        String hstFileName = filenameBase + ".HST";
        File hstFile = new File(hstFileName);
        if (hstFile.exists()) throw new Exception (hstFileName + " already exists");
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(FileHeaderBlock.headerForHstFile(gameId, gameTurn, unknownHeaderFlagBits));
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            if (playerNumber == 0) {
                playerInfo.playerBlock.planets = numPlanets;
            } else {
                playerInfo.playerBlock.planets = 0;
            }
            playerInfo.playerBlock.encode();
            blocks.add(playerInfo.playerBlock);
        }
        for (int planetNumber = 0; planetNumber < numPlanets; planetNumber++) {
            PlanetInfo planetInfo = planets.get(planetNumber);
            if (planetInfo != null) {
                blocks.add(planetInfo.mergeHst());
                blocks.addAll(planetInfo.productionQueue);
            } else {
                blocks.add(PartialPlanetBlock.createEmptyPlanetForHstFile(planetNumber));
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 16; i++) {
                if (playerInfo.fullShipDesignBlocks[i] != null) {
                    blocks.add(playerInfo.fullShipDesignBlocks[i]);
                }
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (FleetInfo fleetInfo : playerInfo.fleets.values()) {
                blocks.addAll(fleetInfo.mergeFull(playerInfo.shipDesignBlocks));
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 10; i++) {
                if (playerInfo.fullStarbaseDesignBlocks[i] != null) {
                    blocks.add(playerInfo.fullStarbaseDesignBlocks[i]);
                }
            }
        }
        if (!objects.isEmpty()) {
            ObjectBlock counter = new ObjectBlock();
            counter.count = objects.size();
            counter.encode();
            blocks.add(counter);
            for (ObjectBlock object : objects.values()) {
                blocks.add(object);
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            blocks.addAll(playerInfo.battlePlans);
        }
        blocks.add(FileFooterBlock.zeroFileFooterBlockForHstOrMFile());
        new Decryptor().writeBlocks(hstFileName, blocks);
    }
    
    private void createMFile(int thisPlayerNumber) throws Exception {
        String mFileName = filenameBase + ".M" + (thisPlayerNumber + 1);
        File mFile = new File(mFileName);
        if (mFile.exists()) return;
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(FileHeaderBlock.headerForMFile(gameId, gameTurn, unknownHeaderFlagBits, thisPlayerNumber));
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            if (playerNumber == thisPlayerNumber) {
                playerInfo.playerBlock.planets = planets.size();
                playerInfo.playerBlock.encode();
                blocks.add(playerInfo.playerBlock);
            } else {
                playerInfo.playerBlock.planets = 0;
                playerInfo.playerBlock.encode();
                blocks.add(playerInfo.playerBlock);
//                playerInfo.partialPlayerBlock.planets = 0;
//                playerInfo.partialPlayerBlock.encode();
//                blocks.add(playerInfo.partialPlayerBlock);
            }
        }
        for (PlanetInfo planetInfo : planets.values()) {
            PartialPlanetBlock partialBlock = planetInfo.mergeM();
            if (partialBlock.owner == thisPlayerNumber) {
                blocks.add(planetInfo.mergeHst());
            } else {
                blocks.add(partialBlock);
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 16; i++) {
                if (playerInfo.shipDesignBlocks[i] != null) {
                    if (playerNumber == thisPlayerNumber) {
                        blocks.add(playerInfo.fullShipDesignBlocks[i]);
                    } else {
                        blocks.add(playerInfo.shipDesignBlocks[i]);
                    }
                }
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (FleetInfo fleetInfo : playerInfo.fleets.values()) {
                if (playerNumber == thisPlayerNumber) {
                    blocks.addAll(fleetInfo.mergeFull(playerInfo.shipDesignBlocks));
                } else {
                    PartialFleetBlock partialBlock = fleetInfo.mergePartial(playerInfo.shipDesignBlocks);
                    blocks.add(partialBlock);
                }
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 10; i++) {
                if (playerInfo.starbaseDesignBlocks[i] != null) {
                    if (playerNumber == thisPlayerNumber) {
                        blocks.add(playerInfo.fullStarbaseDesignBlocks[i]);
                    } else {
                        blocks.add(playerInfo.starbaseDesignBlocks[i]);
                    }
                }
            }
        }
        if (!objects.isEmpty()) {
            ObjectBlock counter = new ObjectBlock();
            counter.count = objects.size();
            counter.encode();
            blocks.add(counter);
            for (ObjectBlock object : objects.values()) {
                blocks.add(object);
            }
        }
        PlayerInfo playerInfo = players[thisPlayerNumber];
        blocks.addAll(playerInfo.battlePlans);
        blocks.add(FileFooterBlock.zeroFileFooterBlockForHstOrMFile());
        new Decryptor().writeBlocks(mFileName, blocks);
    }

    private void createHFile(int thisPlayerNumber) throws Exception {
        String hFileName = filenameBase + ".H" + (thisPlayerNumber + 1);
        File hFile = new File(hFileName);
        if (hFile.exists()) return;
        List<Block> blocks = new ArrayList<Block>();
        blocks.add(FileHeaderBlock.headerForHFile(gameId, gameTurn, unknownHeaderFlagBits, thisPlayerNumber));
        CountersBlock countersBlock = new CountersBlock();
        countersBlock.planetCount = planets.size();
        countersBlock.fleetCount = players[thisPlayerNumber].fleets.size(); // ???
        countersBlock.encode();
        blocks.add(countersBlock);
        for (PlanetInfo planetInfo : planets.values()) {
            blocks.add(planetInfo.mergeH());
        }
        blocks.add(MessagesFilterBlock.newEmptyMessagesFilterBlock());
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            if (playerNumber == thisPlayerNumber) continue;
            PlayerInfo playerInfo = players[playerNumber];
            playerInfo.playerBlock.planets = playerInfo.planetCount;
            playerInfo.playerBlock.encode();
            blocks.add(playerInfo.playerBlock);
//            playerInfo.partialPlayerBlock.planets = playerInfo.planetCount;
//            playerInfo.partialPlayerBlock.encode();
//            blocks.add(playerInfo.partialPlayerBlock);
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 16; i++) {
                if (playerInfo.shipDesignBlocks[i] != null) {
                    blocks.add(playerInfo.shipDesignBlocks[i]);
                }
            }
        }
        for (int playerNumber = 0; playerNumber < numPlayers; playerNumber++) {
            PlayerInfo playerInfo = players[playerNumber];
            for (int i = 0; i < 10; i++) {
                if (playerInfo.starbaseDesignBlocks[i] != null) {
                    blocks.add(playerInfo.starbaseDesignBlocks[i]);
                }
            }
        }
        blocks.add(FileFooterBlock.emptyFileFooterBlockForHFile());
        new Decryptor().writeBlocks(hFileName, blocks);
    }

}
