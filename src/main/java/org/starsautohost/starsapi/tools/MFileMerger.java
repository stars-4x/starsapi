package org.starsautohost.starsapi.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.starsautohost.starsapi.block.BattleBlock;
import org.starsautohost.starsapi.block.BattleContinuationBlock;
import org.starsautohost.starsapi.block.BattlePlanBlock;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
import org.starsautohost.starsapi.block.DesignBlock;
import org.starsautohost.starsapi.block.EventsBlock;
import org.starsautohost.starsapi.block.FileFooterBlock;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.FleetNameBlock;
import org.starsautohost.starsapi.block.MessageBlock;
import org.starsautohost.starsapi.block.ObjectBlock;
import org.starsautohost.starsapi.block.PartialFleetBlock;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.block.PlayerScoresBlock;
import org.starsautohost.starsapi.block.ProductionQueueBlock;
import org.starsautohost.starsapi.block.WaypointBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class MFileMerger {
    private static boolean wantsHelp(String[] args) {
        if (args.length == 0) return true;
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }
    
    public static void main(String[] args) throws Exception {
        new MFileMerger().run(args);
    }
    
    private int playerMask = 0; // for setting wormhole been-through 
    private int actualTurn;
    private Map<String, List<Block>> files = new HashMap<String, List<Block>>();
    private PlayerBlock[] players = new PlayerBlock[16];
    private DesignInfo[][] shipDesigns = new DesignInfo[16][16];
    private DesignInfo[][] starbaseDesigns = new DesignInfo[16][10];
    private Map<Integer, PlanetInfo> planets = new TreeMap<Integer, PlanetInfo>();
    private int numPlanets;
    private List<PartialPlanetBlock> planetBlocks;
    @SuppressWarnings("unchecked")
    private Map<Integer, PartialFleetBlock>[] fleets = new Map[16];
    {
        for (int i = 0; i < 16; i++) {
            fleets[i] = new TreeMap<Integer, PartialFleetBlock>();
        }
    }
    private Map<Integer, ObjectBlock> objects = new TreeMap<Integer, ObjectBlock>();
    
    private void run(String[] args) throws Exception {
        if (wantsHelp(args)) {
            System.out.println("Usage: java -jar MFileMerger.jar file...");
            System.out.println();
            System.out.println("All M files supplied on the command line will have their data augmented");
            System.out.println("with the data on each planet, player, design, fleet, minefield, packet,");
            System.out.println("salvage, or wormhole from any of the files.");
            System.out.println();
            System.out.println("Backups of each input M file will be retained with suffix .backup-m#.");
            return;
        }
        
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
        }
        checkGameIdsAndYearsAndPlayers(files);
        
        for (List<Block> blocks : files.values()) {
            new FileProcessor().process(blocks);
        }
        
        postProcess();
        
        for (String filename : args) {
            new FileOutProcessor(filename).process();
        }
    }

    private void postProcess() throws Exception {
        numPlanets = planets.size();
        planetBlocks = new ArrayList<PartialPlanetBlock>();
        for (PlanetInfo planetInfo : planets.values()) {
            PartialPlanetBlock pblock = planetInfo.merge();
            pblock.encode();
            planetBlocks.add(pblock);
        }
        for (int player = 0; player < 16; player++) {
            if (players[player] == null) continue;
            players[player].shipDesigns = 0;
            players[player].starbaseDesigns = 0;
            players[player].planets = 0;
            players[player].fleets = fleets[player].size();
            for (int designNumber = 0; designNumber < 16; designNumber++) {
                DesignInfo designInfo = shipDesigns[player][designNumber];
                if (designInfo != null) {
                    players[player].shipDesigns++;
                }
            }
            for (int designNumber = 0; designNumber < 10; designNumber++) {
                DesignInfo designInfo = starbaseDesigns[player][designNumber];
                if (designInfo != null) {
                    players[player].starbaseDesigns++;
                }
            }
            players[player].encode();
        }
    }
    
    private class FileOutProcessor {
        String filename;
        List<Block> blocks;
        PlayerBlock[] players = MFileMerger.this.players.clone();
        DesignInfo[][] shipDesigns = MFileMerger.this.shipDesigns.clone();
        DesignInfo[][] starbaseDesigns = MFileMerger.this.starbaseDesigns.clone();
        Map<Integer, List<Block>> planets = new TreeMap<Integer, List<Block>>();
        Map<Integer, List<Block>> fleets = new TreeMap<Integer, List<Block>>();
        Map<Integer, ObjectBlock> objects = new TreeMap<Integer, ObjectBlock>();
        List<Block> currentBlocks = null;
        int shipDesignIndex = 0;
        int starbaseDesignIndex = 0;
        List<Integer> shipDesignOwner = new ArrayList<Integer>();
        List<Integer> starbaseDesignOwner = new ArrayList<Integer>();

        public FileOutProcessor(String filename) {
            this.filename = filename;
            this.blocks = files.get(filename);
            for (PartialPlanetBlock pblock : MFileMerger.this.planetBlocks) {
                planets.put(pblock.planetNumber, Collections.<Block>singletonList(pblock));
            }
            for (int i = 0; i < 16; i++) {
                for (PartialFleetBlock fblock : MFileMerger.this.fleets[i].values()) {
                    fleets.put(fblock.getFleetIdAndOwner(), Collections.<Block>singletonList(fblock));
                }
            }
            objects.putAll(MFileMerger.this.objects);
        }
        
        void process() throws Exception {
            copy(filename, backupFilename(filename));
            setUpBlocks();
            List<Block> newBlocks = new ArrayList<Block>();
            boolean foundLastTurn = false;
            boolean didPlayers = false;
            boolean didPlanets = false;
            boolean didShipDesigns = false;
            boolean didFleets = false;
            boolean didStarbaseDesigns = false;
            boolean didObjects = false;
            for (Block block : blocks) {
                if (block instanceof FileHeaderBlock) {
                    FileHeaderBlock headerBlock = (FileHeaderBlock)block;
                    if (headerBlock.turn == actualTurn) foundLastTurn = true;
                }
                if (!foundLastTurn) {
                    newBlocks.add(block);
                    continue;
                }
                if (block instanceof PlayerBlock) {
                    if (!didPlayers) doPlayers(newBlocks);
                    didPlayers = true;
                } else if (block instanceof PartialPlanetBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                } else if (block instanceof DesignBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                    if (!didShipDesigns) doShipDesigns(newBlocks);
                    didShipDesigns = true;
                    if (((DesignBlock)block).isStarbase) {
                        if (!didFleets) doFleets(newBlocks);
                        didFleets = true;
                        if (!didStarbaseDesigns) doStarbaseDesigns(newBlocks);
                        didStarbaseDesigns = true;
                    }
                } else if (block instanceof PartialFleetBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                    if (!didShipDesigns) doShipDesigns(newBlocks);
                    didShipDesigns = true;
                    if (!didFleets) doFleets(newBlocks);
                    didFleets = true;
                } else if (block instanceof PlayerScoresBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                    if (!didShipDesigns) doShipDesigns(newBlocks);
                    didShipDesigns = true;
                    if (!didFleets) doFleets(newBlocks);
                    didFleets = true;
                    if (!didStarbaseDesigns) doStarbaseDesigns(newBlocks);
                    didStarbaseDesigns = true;
                    newBlocks.add(block);
                } else if (block instanceof ObjectBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                    if (!didShipDesigns) doShipDesigns(newBlocks);
                    didShipDesigns = true;
                    if (!didFleets) doFleets(newBlocks);
                    didFleets = true;
                    if (!didStarbaseDesigns) doStarbaseDesigns(newBlocks);
                    didStarbaseDesigns = true;
                    if (!didObjects) doObjects(newBlocks);
                    didObjects = true;
                } else if (block instanceof BattlePlanBlock || block instanceof FileFooterBlock) {
                    if (!didPlanets) doPlanets(newBlocks);
                    didPlanets = true;
                    if (!didShipDesigns) doShipDesigns(newBlocks);
                    didShipDesigns = true;
                    if (!didFleets) doFleets(newBlocks);
                    didFleets = true;
                    if (!didStarbaseDesigns) doStarbaseDesigns(newBlocks);
                    didStarbaseDesigns = true;
                    if (!didObjects) doObjects(newBlocks);
                    didObjects = true;
                    newBlocks.add(block);
                } else if (block instanceof WaypointBlock || block instanceof FleetNameBlock || block instanceof ProductionQueueBlock) {
                    // skip
                } else if (block instanceof FileHeaderBlock || block instanceof BattleBlock || block instanceof BattleContinuationBlock || block instanceof EventsBlock || block instanceof MessageBlock) {
                    newBlocks.add(block);
                } else {
                    throw new Exception("Unexpected block: " + block);
                }
            }
            new Decryptor().writeBlocks(filename, newBlocks);
        }
        
        private void doPlayers(List<Block> newBlocks) {
            for (int i = 0; i < 16; i++) {
                if (players[i] != null) newBlocks.add(players[i]);
            }
        }

        private void doPlanets(List<Block> newBlocks) {
            for (List<Block> blocks : planets.values()) {
                newBlocks.addAll(blocks);
            }
        }

        private void doShipDesigns(List<Block> newBlocks) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    if (shipDesigns[i][j] != null) {
                        newBlocks.add(shipDesigns[i][j].block);
                    }
                }
            }
        }

        private void doFleets(List<Block> newBlocks) {
            for (List<Block> blocks : fleets.values()) {
                newBlocks.addAll(blocks);
            }
        }

        private void doStarbaseDesigns(List<Block> newBlocks) {
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 10; j++) {
                    if (starbaseDesigns[i][j] != null) {
                        newBlocks.add(starbaseDesigns[i][j].block);
                    }
                }
            }
        }

        private void doObjects(List<Block> newBlocks) throws Exception {
            if (objects.size() == 0) return;
            ObjectBlock counter = new ObjectBlock();
            counter.count = objects.size();
            counter.encode();
            newBlocks.add(counter);
            newBlocks.addAll(objects.values());
        }

        void setUpBlocks() throws Exception {
            boolean foundLastTurn = false;
            for (Block block : blocks) {
                if (block instanceof FileHeaderBlock) {
                    FileHeaderBlock headerBlock = (FileHeaderBlock)block;
                    if (headerBlock.turn == actualTurn) foundLastTurn = true;
                }
                if (!foundLastTurn) {
                    continue;
                }

                if (block instanceof PlayerBlock) {
                    PlayerBlock playerBlock = (PlayerBlock)block;
                    processPlayerBlock(playerBlock);
                }
                if (block instanceof PartialPlanetBlock) {
                    PartialPlanetBlock pblock = (PartialPlanetBlock)block;
                    processPlanetBlock(pblock);
                }
                if (block instanceof ProductionQueueBlock) {
                    currentBlocks.add(block);
                }
                if (block instanceof DesignBlock) {
                    DesignBlock designBlock = (DesignBlock) block;
                    processDesignBlock(designBlock);
                }
                // include FleetBlock as an extension of PartialFleetBlock
                if (block instanceof PartialFleetBlock) {
                    PartialFleetBlock fblock = (PartialFleetBlock)block;
                    currentBlocks = new ArrayList<Block>();
                    currentBlocks.add(fblock);
                    fleets.put(fblock.getFleetIdAndOwner(), currentBlocks);
                }
                if (block instanceof WaypointBlock || block instanceof FleetNameBlock) {
                    currentBlocks.add(block);
                }
                if (block instanceof ObjectBlock) {
                    ObjectBlock oblock = (ObjectBlock)block;
                    if (oblock.isCounter()) continue;
                    if (oblock.isWormhole() && oblock.isWormholeBeenThrough(playerMask)) {
                        oblock.setWormholeBeenThrough(playerMask);
                        // no need to encode
                    }
                    objects.put(oblock.getObjectId(), oblock);
                }
            }
        }

        private void processDesignBlock(DesignBlock designBlock) {
            int player;
            DesignInfo[][] designMatrix = null;
            if (designBlock.isStarbase) {
                player = starbaseDesignOwner.get(starbaseDesignIndex++);
                if (MFileMerger.this.starbaseDesigns[player][designBlock.designNumber].compatibilityIssue) {
                    designMatrix = starbaseDesigns;
                }
            } else {
                player = shipDesignOwner.get(shipDesignIndex++);
                if (MFileMerger.this.shipDesigns[player][designBlock.designNumber].compatibilityIssue) {
                    designMatrix = shipDesigns;
                }
            }
            if (designMatrix == null) return;
            DesignInfo designInfo = new DesignInfo(player);
            designMatrix[player][designBlock.designNumber] = designInfo;
            designInfo.consider(designBlock);
        }

        private void processPlanetBlock(PartialPlanetBlock pblock) {
            if (MFileMerger.this.planets.get(pblock.planetNumber).compatibilityIssue || pblock.isInUseOrRobberBaron || pblock.hasSurfaceMinerals) {
                currentBlocks = new ArrayList<Block>();
                currentBlocks.add(pblock);
                planets.put(pblock.planetNumber, currentBlocks);
            }
        }

        private void processPlayerBlock(PlayerBlock playerBlock) throws Exception {
            if (playerBlock.fullDataFlag) {
                playerBlock.planets = numPlanets;
                players[playerBlock.playerNumber] = playerBlock;
                playerBlock.encode();
            }
            Integer playerNumberObj = Integer.valueOf(playerBlock.playerNumber);
            for (int i = 0; i < playerBlock.shipDesigns; i++) {
                shipDesignOwner.add(playerNumberObj);
            }
            for (int i = 0; i < playerBlock.starbaseDesigns; i++) {
                starbaseDesignOwner.add(playerNumberObj);
            }
        }
    }

    
    private static String backupFilename(String filename) {
        if (filename.matches("^.*\\.[mM][0-9][0-9]*$")) {
            return filename.replaceAll("\\.([mM][0-9][0-9]*)$", ".backup-$1");
        } else {
            return filename + ".backup-m";
        }
    }

    private class FileProcessor {
        private PartialFleetBlock lastFleetBlock;
        private int waypointCount = 0;
        private int shipDesignIndex = 0;
        private int starbaseDesignIndex = 0;
        private List<Integer> shipDesignOwner = new ArrayList<Integer>();
        private List<Integer> starbaseDesignOwner = new ArrayList<Integer>();

        void process(List<Block> blocks) throws Exception {
            boolean foundLatestTurn = false;
            for (Block block : blocks) {
                if (block instanceof FileHeaderBlock) {
                    FileHeaderBlock headerBlock = (FileHeaderBlock)block;
                    if (headerBlock.turn == actualTurn) foundLatestTurn = true;
                }
                if (!foundLatestTurn) continue;
                if (block instanceof PlayerBlock) {
                    PlayerBlock playerBlock = (PlayerBlock)block;
                    processPlayerBlock(playerBlock);
                }
                // include PlanetBlock as an extension of PartialPlanetBlock
                if (block instanceof PartialPlanetBlock) {
                    PartialPlanetBlock pblock = (PartialPlanetBlock)block;
                    processPlanetBlock(pblock);
                }
                if (block instanceof DesignBlock) {
                    DesignBlock designBlock = (DesignBlock)block;
                    processDesignBlock(designBlock);
                }
                // include FleetBlock as an extension of PartialFleetBlock
                if (block instanceof PartialFleetBlock) {
                    PartialFleetBlock fblock = (PartialFleetBlock)block;
                    lastFleetBlock = fblock;
                    waypointCount = 0;
                    processFleetBlock(fblock);
                }
                // include WaypointTaskBlock as an extension of WaypointBlock
                if (block instanceof WaypointBlock) {
                    waypointCount++;
                    if (waypointCount == 2) {
                        WaypointBlock wblock = (WaypointBlock)block;
                        processWaypointBlock(lastFleetBlock, wblock);
                    }
                }
                if (block instanceof ObjectBlock) {
                    ObjectBlock oblock = (ObjectBlock)block;
                    if (oblock.isCounter()) continue;
                    processObjectBlock(oblock);
                }
            }
        }
        
        private void processPlanetBlock(PartialPlanetBlock pblock) throws Exception {
            pblock = (PartialPlanetBlock) Block.copy(pblock);
            int planetNumber = pblock.planetNumber;
            PlanetInfo planetInfo = planets.get(planetNumber);
            if (planetInfo == null) {
                planetInfo = new PlanetInfo();
                planets.put(planetNumber, planetInfo);
            }
            pblock.convertToPartialPlanetForMFile();
            planetInfo.consider(pblock);
        }

        private void processPlayerBlock(PlayerBlock playerBlock) throws Exception {
            playerBlock = (PlayerBlock) Block.copy(playerBlock);
            if (players[playerBlock.playerNumber] == null) {
                playerBlock.fullDataFlag = false;
                playerBlock.planets = 0;
                playerBlock.fleets = 0;
                players[playerBlock.playerNumber] = playerBlock;
            }
            Integer playerNumberObj = Integer.valueOf(playerBlock.playerNumber);
            for (int i = 0; i < playerBlock.shipDesigns; i++) {
                shipDesignOwner.add(playerNumberObj);
            }
            playerBlock.shipDesigns = 0;
            for (int i = 0; i < playerBlock.starbaseDesigns; i++) {
                starbaseDesignOwner.add(playerNumberObj);
            }
            playerBlock.starbaseDesigns = 0;
        }

        private void processDesignBlock(DesignBlock designBlock) {
            int player;
            DesignInfo[][] designMatrix;
            if (designBlock.isStarbase) {
                player = starbaseDesignOwner.get(starbaseDesignIndex++);
                designMatrix = starbaseDesigns;
            } else {
                player = shipDesignOwner.get(shipDesignIndex++);
                designMatrix = shipDesigns;
            }
            DesignInfo designInfo = designMatrix[player][designBlock.designNumber];
            if (designInfo == null) {
                designInfo = new DesignInfo(player);
                designMatrix[player][designBlock.designNumber] = designInfo;
            }
            designInfo.consider(designBlock);
        }
        
        private void processFleetBlock(PartialFleetBlock fleetBlock) throws Exception {
            PartialFleetBlock savedBlock = fleets[fleetBlock.owner].get(fleetBlock.fleetNumber);
            if (savedBlock == null || (savedBlock.mass == 0 && fleetBlock.mass > 0)) {
                fleetBlock = (PartialFleetBlock) Block.copy(fleetBlock);
                fleetBlock.convertToPartialFleet();
                if (fleetBlock.mass == 0) fleetBlock.unknownBitsWithWarp = 16;
                fleetBlock.encode();
                fleets[fleetBlock.owner].put(fleetBlock.fleetNumber, fleetBlock);
            }
        }
        
        private void processWaypointBlock(PartialFleetBlock fleetBlock, WaypointBlock waypointBlock) {
            // use second waypoint to set motion of fleet
            PartialFleetBlock savedBlock = fleets[fleetBlock.owner].get(fleetBlock.fleetNumber);
            if (savedBlock == null || savedBlock.mass > 0) return;
            if (waypointBlock.warp == 0 || waypointBlock.warp > 10) return;
            int deltaX = waypointBlock.x - savedBlock.x;
            int deltaY = waypointBlock.y - savedBlock.y;
            int largest = Math.max(Math.abs(deltaX), Math.abs(deltaY));
            if (largest > 125) {
                deltaX = deltaX * 125 / largest;
                deltaY = deltaY * 125 / largest;
            }
            deltaX += 127;
            deltaY += 127;
            savedBlock.deltaX = deltaX;
            savedBlock.deltaY = deltaY;
            savedBlock.warp = waypointBlock.warp;
            savedBlock.unknownBitsWithWarp = 16;
            savedBlock.encode();
        }
        
        private void processObjectBlock(ObjectBlock object) throws Exception {
            if (object.isCounter()) return;
            if (object.isWormhole() && object.isWormholeBeenThrough(playerMask)) {
                object = (ObjectBlock) Block.copy(object);
                object.setWormholeBeenThrough(playerMask);
                // no need to encode
            }
            objects.put(object.getObjectId(), object);
        }
    }
    
    private void checkGameIdsAndYearsAndPlayers(Map<String, List<Block>> files) throws Exception {
        if (files.isEmpty()) {
            throw new Exception("No M files given");
        }
        long gameId = 0;
        int turn = 0;
        boolean first = true;
        for (List<Block> blocks : files.values()) {
            FileHeaderBlock headerBlock = null;
            for (Block block : blocks) {
                if (!(block instanceof FileHeaderBlock)) continue;
                headerBlock = (FileHeaderBlock)block;
            }
            if (first) {
                gameId = headerBlock.gameId;
                turn = headerBlock.turn;
                playerMask |= 1 << headerBlock.playerNumber;
            } else {
                if (gameId != headerBlock.gameId) {
                    throw new Exception("Game ID does not match");
                }
                if (turn != headerBlock.turn) {
                    throw new Exception("Latest turn in file does not match");
                }
            }
        }
        actualTurn = turn;
    }

    public static void copy(String src, String dst) throws Exception {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);

            // Transfer bytes from in to out
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } finally {
            if (in != null) try { in.close(); } catch (IOException e) { };
            if (out != null) out.close();
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
        if (fileType != 3) {
            System.out.println(filename + " is not an M file");
            return true;
        }
        return false;
    }
    
    private static class PlanetInfo {
        PartialPlanetBlock best;
        PartialPlanetBlock bestWithEnvironment;
        PartialPlanetBlock bestWithStarbase;
        boolean compatibilityIssue;
        
        PartialPlanetBlock merge() {
            PartialPlanetBlock res = best;
            // Starbases are tricky since they may be missing because destroyed, or because cloaked.
            if (!res.hasStarbase && bestWithStarbase != null) {
                res.hasStarbase = true;
                res.starbaseDesign = bestWithStarbase.starbaseDesign;
            }
            return res;
        }
 
        static boolean isCompatible(PartialPlanetBlock first, PartialPlanetBlock second) {
            if (first == null || second == null) return true;
            if (first.owner != second.owner) return false;
            if (first.isHomeworld != second.isHomeworld) return false;
            if (first.hasStarbase && second.hasStarbase && (first.starbaseDesign != second.starbaseDesign)) return false;
            if (first.hasEnvironmentInfo && second.hasEnvironmentInfo) {
                if (first.ironiumConc != second.ironiumConc) return false;
                if (first.boraniumConc != second.boraniumConc) return false;
                if (first.germaniumConc != second.germaniumConc) return false;
                if (first.gravity != second.gravity) return false;
                if (first.temperature != second.temperature) return false;
                if (first.radiation != second.radiation) return false;
                if (first.isTerraformed != second.isTerraformed) return false;
                if (first.isTerraformed) {
                    if (first.origGravity != second.origGravity) return false;
                    if (first.origTemperature != second.origTemperature) return false;
                    if (first.origRadiation != second.origRadiation) return false;
                }
                if (first.owner >= 0) {
                    if (first.estimatesShort != second.estimatesShort) return false;
                }
            }
            return true;
        }
        
        void checkCompatibility(PartialPlanetBlock block) {
            if (!isCompatible(block, best) || !isCompatible(block, bestWithStarbase) || !isCompatible(block, bestWithEnvironment)) {
                System.out.println("Warning: planet conflict, planet " + block.planetNumber);
                compatibilityIssue = true;
            }
        }
        
        void consider(PartialPlanetBlock block) throws Exception {
            checkCompatibility(block);
            if (block.hasStarbase && bestWithStarbase == null) {
                bestWithStarbase = block;
            }
            if (block.hasEnvironmentInfo && bestWithEnvironment == null) {
                bestWithEnvironment = block;
            }
            if (best == null) {
                best = block;
                return;
            }
            if (block.weirdBit) best.weirdBit = true; // ???
            if (block.hasEnvironmentInfo && !best.hasEnvironmentInfo) {
                best = block;
            } else if (block.hasEnvironmentInfo == best.hasEnvironmentInfo && block.hasStarbase && !best.hasStarbase) {
                best = block;
            }
        }
    }
    
    private static class DesignInfo {
        final int player;
        DesignBlock block;
        boolean compatibilityIssue;
        
        DesignInfo(int player) {
            this.player = player;
        }
        
        static boolean isCompatible(DesignBlock block1, DesignBlock block2) {
            if (block1.isTransferred != block2.isTransferred) return false;
            if (block1.hullId != block2.hullId) return false;
            if (block1.pic != block2.pic) return false;
            if (!block1.isFullDesign && !block2.isFullDesign) {
                if (block1.mass != block2.mass) return false;
            }
            // TODO mass of full designs vs not-full designs
            if (block1.isFullDesign && block2.isFullDesign) {
                if (!Arrays.equals(block1.nameBytes, block2.nameBytes)) return false;
                if (block1.fullBytes.length != block2.fullBytes.length) return false;
                for (int i = 0; i < block1.fullBytes.length; i++) {
                    // skip weird counting and other bytes, just consider armor and slots
                    if (i >= 3 && i <= 12) continue;
                    if (block1.fullBytes[i] != block2.fullBytes[i]) return false;
                }
            }
            return true;
        }
        
        void consider(DesignBlock block) {
            if (this.block == null) {
                this.block = block;
            } else {
                if (!isCompatible(this.block, block)) {
                    compatibilityIssue = true;
                    System.out.println("Warning: design conflict, player " + player + " " + (block.isStarbase ? "starbase" : "ship") + " slot " + block.designNumber);
                }
                if (!this.block.isFullDesign && block.isFullDesign) {
                    this.block = block;
                }
            }
        }
    }
}
