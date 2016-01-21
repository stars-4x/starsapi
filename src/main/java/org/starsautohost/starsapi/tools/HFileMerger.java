package org.starsautohost.starsapi.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
import org.starsautohost.starsapi.block.CountersBlock;
import org.starsautohost.starsapi.block.DesignBlock;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class HFileMerger {
    private static boolean wantsHelp(String[] args) {
        if (args.length == 0) return true;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("-help") || arg.equalsIgnoreCase("--help")) {
                return true;
            }
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("-h")) return true;
        return false;
    }
    
    public static void main(String[] args) throws Exception {
        if (wantsHelp(args)) {
            System.out.println("Usage: java -jar HFileMerger.jar file...");
            System.out.println();
            System.out.println("All H files supplied on the command line will have their data replaced");
            System.out.println("with the newest data on each planet, player, and design from any of the files.");
            System.out.println();
            System.out.println("M files supplied on the command line will have their data incorporated");
            System.out.println("but will not be changed.  M files are needed for accurately determining");
            System.out.println("the latest ship designs.");
            System.out.println();
            System.out.println("Backups of each input H file will be retained with suffix .backup-h#.");
            return;
        }
        
        new HFileMerger().run(args);
    }
    
    private Map<String, List<Block>> files = new HashMap<String, List<Block>>();
    private Map<Integer, PlanetInfo> planets = new TreeMap<Integer, PlanetInfo>();
    protected PlayerBlock[] players = new PlayerBlock[16];
    private int[][] fleetCount = new int[16][16];
    private DesignInfo[][] shipDesigns = new DesignInfo[16][16];
    private DesignInfo[][] starbaseDesigns = new DesignInfo[16][10];
    private int numPlanets;
    protected List<PartialPlanetBlock> planetBlocks;
    
    public void run(String[] args) throws Exception {
        for (String filename : args) {
            List<Block> blocks;
            try {
                blocks = new Decryptor().readFile(filename);
                files.put(filename, blocks);
            } catch (Exception e) {
                System.out.println("Unable to parse file " + filename + ": " + e);
                return;
            }
            if (isProblem(filename, blocks)) return;
        }
        checkGameIdsAndYears(files);
        
        for (List<Block> blocks : files.values()) {
            new FileProcessor().process(blocks);
        }
        
        postProcess();
        
        for (String filename : args) {
            outProcessFile(filename);
        }
    }

    protected void postProcess() {
        numPlanets = planets.size();
        planetBlocks = new ArrayList<PartialPlanetBlock>();
        for (PlanetInfo planetInfo : planets.values()) {
            PartialPlanetBlock pblock = planetInfo.merge();
            planetBlocks.add(pblock);
            if (pblock.owner >= 0 && players[pblock.owner] != null) players[pblock.owner].planets++;
        }
        for (int player = 0; player < 16; player++) {
            for (int designNumber = 0; designNumber < 16; designNumber++) {
                DesignInfo designInfo = shipDesigns[player][designNumber];
                if (designInfo != null) {
                    players[player].shipDesigns++;
                    designInfo.checkConflict();
                }
            }
            for (int designNumber = 0; designNumber < 10; designNumber++) {
                DesignInfo designInfo = starbaseDesigns[player][designNumber];
                if (designInfo != null) {
                    players[player].starbaseDesigns++;
                    designInfo.checkConflict();
                }
            }
        }
    }
    
    void outProcessFile(String filename) throws Exception {
        List<Block> blocks = files.get(filename);
        FileHeaderBlock headerBlock = (FileHeaderBlock)(blocks.get(0));
        if (headerBlock.fileType != 4) return;
        int filePlayerNumber = headerBlock.playerNumber;
        copy(filename, backupFilename(filename));
        ((CountersBlock)blocks.get(1)).planetCount = numPlanets;
        blocks.get(1).encode();
        List<Block> newBlocks = new ArrayList<Block>();
        newBlocks.add(blocks.get(0));
        newBlocks.add(blocks.get(1));
        for (Block block : planetBlocks) {
            block.encode();
        }
        newBlocks.addAll(planetBlocks);
        boolean donePlayersAndDesigns = false;
        for (int blockNumber = 2; blockNumber < blocks.size(); blockNumber++) {
            Block block = blocks.get(blockNumber);
            if (block.typeId == BlockType.PLAYER) {
                PlayerBlock playerBlock = (PlayerBlock)block;
                players[playerBlock.playerNumber].fleets = fleetCount[filePlayerNumber][playerBlock.playerNumber];
            }
            if (!donePlayersAndDesigns && (block.typeId == BlockType.PLAYER_SCORES || block.typeId == BlockType.FILE_FOOTER)) {
                addPlayersAndDesigns(newBlocks, filePlayerNumber);
                donePlayersAndDesigns = true;
            }
            if (block.typeId != BlockType.PARTIAL_PLANET && block.typeId != BlockType.PLAYER && block.typeId != BlockType.DESIGN) {
                newBlocks.add(block);
            }
        }
        new Decryptor().writeBlocks(filename, newBlocks);
    }

    private static String backupFilename(String filename) {
        if (filename.matches("^.*\\.[hH][0-9][0-9]*$")) {
            return filename.replaceAll("\\.([hH][0-9][0-9]*)$", ".backup-$1");
        } else {
            return filename + ".backup-h";
        }
    }

    private void addPlayersAndDesigns(List<Block> newBlocks, int filePlayerNumber) throws Exception {
        for (int player = 0; player < 16; player++) {
            if (players[player] != null & player != filePlayerNumber) {
                players[player].encode();
                newBlocks.add(players[player]);
            }
        }
        for (int player = 0; player < 16; player++) {
            if (player == filePlayerNumber) continue;
            for (int designNumber = 0; designNumber < 16; designNumber++) {
                DesignInfo designInfo = shipDesigns[player][designNumber];
                if (designInfo != null) {
                    newBlocks.add(designInfo.getBestForObserver(filePlayerNumber));
                }
            }
        }
        for (int player = 0; player < 16; player++) {
            if (player == filePlayerNumber) continue;
            for (int designNumber = 0; designNumber < 10; designNumber++) {
                DesignInfo designInfo = starbaseDesigns[player][designNumber];
                if (designInfo != null) {
                    newBlocks.add(designInfo.getBestForObserver(filePlayerNumber));
                }
            }
        }
    }
    
    protected class FileProcessor {
        private int observer = -1;
        private int fileTurn = -1;
        private boolean isMFile = false;
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
                    isMFile = headerBlock.fileType == 3;
                }
                // include PlanetBlock as an extension of PartialPlanetBlock
                if (block instanceof PartialPlanetBlock) {
                    PartialPlanetBlock pblock = (PartialPlanetBlock)block;
                    processPlanetBlock(pblock);
                }
                if (block instanceof PlayerBlock) {
                    PlayerBlock playerBlock = (PlayerBlock)block;
                    processPlayerBlock(playerBlock);
                }
                if (block instanceof DesignBlock) {
                    DesignBlock designBlock = (DesignBlock)block;
                    processDesignBlock(designBlock);
                }
            }
        }
        
        private void processPlanetBlock(PartialPlanetBlock pblock) throws Exception {
            int planetNumber = pblock.planetNumber;
            PlanetInfo planetInfo = planets.get(planetNumber);
            if (planetInfo == null) {
                planetInfo = new PlanetInfo();
                planets.put(planetNumber, planetInfo);
            }
            int turn = fileTurn;
            if (pblock.turn > -1) turn = pblock.turn;
            pblock.convertToPartialPlanetForHFile(turn);
            planetInfo.considerForLatest(pblock);
            if (pblock.hasStarbase) {
                int last = latestStarbaseYearPlusOne[pblock.owner][pblock.starbaseDesign];
                if (turn + 1 > last) latestStarbaseYearPlusOne[pblock.owner][pblock.starbaseDesign] = turn + 1;
            }
        }

        private void processPlayerBlock(PlayerBlock playerBlock) {
            if (!isMFile) {
                fleetCount[observer][playerBlock.playerNumber] = playerBlock.fleets;
            }
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
            if (designBlock.isStarbase && !isMFile) {
                int turn = latestStarbaseYearPlusOne[player][designBlock.designNumber] - 1;
                designInfo.consider(designBlock, observer, true, turn);
                designInfo.consider(designBlock, observer, false, fileTurn);
            } else {
                designInfo.consider(designBlock, observer, isMFile, fileTurn);
            }
        }
    }
    
    private void checkGameIdsAndYears(Map<String, List<Block>> files) throws Exception {
        long gameId = 0;
        boolean first = true;
        boolean mismatchedTurns = false;
        boolean foundHFile = false;
        int latestHTurn = 0;
        for (List<Block> blocks : files.values()) {
            FileHeaderBlock headerBlock = (FileHeaderBlock)blocks.get(0);
            if (headerBlock.fileType == 4) foundHFile = true;
            else continue;
            if (first) {
                gameId = headerBlock.gameId;
                latestHTurn = headerBlock.turn;
                first = false;
            } else {
                if (gameId != headerBlock.gameId) {
                    throw new Exception("Game ID does not match");
                }
                if (latestHTurn != headerBlock.turn) {
                    if (headerBlock.turn > latestHTurn) {
                        latestHTurn = headerBlock.turn;
                    }
                    mismatchedTurns = true;
                }
            }
        }
        if (!foundHFile) {
            throw new Exception("No H file given");
        }
        for (List<Block> blocks : files.values()) {
            FileHeaderBlock headerBlock = (FileHeaderBlock)blocks.get(0);
            if (headerBlock.fileType != 3) continue;
            if (gameId != headerBlock.gameId) {
                throw new Exception("Game ID does not match");
            }
            for (Block block : blocks) {
                if (block.typeId != BlockType.FILE_HEADER) continue;
                headerBlock = (FileHeaderBlock)block;
                if (headerBlock.turn > latestHTurn) {
                    mismatchedTurns = true;
                }
            }
        }
        if (mismatchedTurns) {
            System.out.println("WARNING: mismatched turns");
            for (Map.Entry<String, List<Block>> entry : files.entrySet()) {
                List<Block> blocks = entry.getValue();
                for (Block block : blocks) {
                    if (block.typeId != BlockType.FILE_HEADER) continue;
                    FileHeaderBlock headerBlock = (FileHeaderBlock)block;
                    String filename = entry.getKey();
                    System.out.println(filename + ": " + (2400 + headerBlock.turn));
                }
            }
        }
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
        if (fileType != 4 && fileType != 3) {
            System.out.println(filename + " is not an H file or an M file");
            return true;
        }
        if (fileType == 3) return false;
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
    
    private static class PlanetInfo {
        PartialPlanetBlock latest;
        PartialPlanetBlock latestWithEnvironment;
        PartialPlanetBlock latestWithStarbase;
        
        PartialPlanetBlock merge() {
            boolean mergedLatest = false;
            boolean mergedLatestWithStarbase = false;
            PartialPlanetBlock res;
            if (!latest.canSeeEnvironment() && latestWithEnvironment != null) {
                res = latestWithEnvironment;
                res.owner = latest.owner;
                res.weirdBit = latest.weirdBit;
                res.hasStarbase = latest.hasStarbase;
                res.starbaseDesign = latest.starbaseDesign;
                mergedLatest = true;
            } else {
                res = latest;
            }
            // Starbases are tricky since they may be missing because destroyed, or because cloaked.
            // We use later data even if no starbase; only add in starbase data at least as new.
            if (!res.hasStarbase && latestWithStarbase != null && res.turn <= latestWithStarbase.turn) {
                res.hasStarbase = true;
                res.starbaseDesign = latestWithStarbase.starbaseDesign;
                if (res.turn < latestWithStarbase.turn) mergedLatestWithStarbase = true;
            }
            
            if (mergedLatest || mergedLatestWithStarbase) {
                System.out.println("Warning: merging planet data for planet #" + res.planetNumber);
                System.out.println("Result: " + printPlanet(res));
                if (mergedLatest) System.out.println("Merged in: " + printPlanet(latest));
                if (mergedLatestWithStarbase) System.out.println("Merged in: " + printPlanet(latestWithStarbase));
            }
            
            return res;
        }
        
        static String printPlanet(PartialPlanetBlock block) {
            String res = "Turn:" + block.turn;
            if (block.owner >= 0) res += ", Owner:" + block.owner;
            if (block.canSeeEnvironment()) res += ", Environment";
            if (block.hasStarbase) res += ", Starbase:"+ block.starbaseDesign;
            return res;
        }
        
        void considerForLatest(PartialPlanetBlock block) {
            if (block.hasStarbase && (latestWithStarbase == null || latestWithStarbase.turn < block.turn)) {
                latestWithStarbase = block;
            }
            if (block.canSeeEnvironment() && (latestWithEnvironment == null || latestWithEnvironment.turn < block.turn)) {
                latestWithEnvironment = block;
            }
            if (latest == null || latest.turn < block.turn) {
                latest = block;
                return;
            }
            if (latest.turn == block.turn) {
                if (block.weirdBit) latest.weirdBit = true; // ???
                if (block.canSeeEnvironment() && !latest.canSeeEnvironment()) {
                    latest = block;
                } else if (block.canSeeEnvironment() == latest.canSeeEnvironment() && 
                        block.hasStarbase && !latest.hasStarbase) {
                    latest = block;
                }
            }
        }
    }
    
    private static class DesignInfo {
        final int player;
        DesignBlock latestInMFile;
        int latestInMFileTurn = -1;
        DesignBlock latestFullInMFile;
        int latestFullInMFileTurn = -1;
        DesignBlock[] latestInHFile = new DesignBlock[16];
        
        DesignInfo(int player) {
            this.player = player;
        }
        
        void checkConflict() {
            if (latestInMFile != null) {
                if (latestInMFile.isFullDesign) return;
                if (latestFullInMFile != null) {
                    if (DesignBlock.isCompatible(latestInMFile, latestFullInMFile)) {
                        return;
                    } else {
                        latestFullInMFile = null;
                        latestFullInMFileTurn = -1;
                    }
                } else {
                    checkCompatibleDesignArray(latestInHFile, latestInMFile, true);
                    return;
                }
            }
            DesignBlock first = checkCompatibleDesignArray(latestInHFile, null, false); 
            checkCompatibleDesignArray(latestInHFile, first, true);
        }

        private DesignBlock checkCompatibleDesignArray(DesignBlock[] blocks, DesignBlock mustBeCompatibleWith, boolean mustBeFull) {
            DesignBlock first = null;
            int firstObserver = -1;
            for (int i = 0; i < 16; i++) {
                DesignBlock block = blocks[i];
                if (block == null) continue;
                if (mustBeFull && !block.isFullDesign) continue;
                if (mustBeCompatibleWith != null && !DesignBlock.isCompatible(mustBeCompatibleWith, block)) continue;
                if (first == null) {
                    first = block;
                    firstObserver = i;
                } else {
                    if (!DesignBlock.isCompatible(first, block)) {
                        System.out.println("Warning: design conflict, player " + player + " " + (first.isStarbase ? "starbase" : "ship") + " slot " + first.designNumber + ", observers " + firstObserver + "/" + i);
                    }
                }
            }
            return first;
        }
        
        DesignBlock getBestForObserver(int observer) {
            DesignBlock bestMaybeNotFull = null;
            if (latestInMFile != null) {
                bestMaybeNotFull = latestInMFile;
            } else if (latestInHFile[observer] != null) {
                bestMaybeNotFull = latestInHFile[observer];
            } else {
                for (DesignBlock block : latestInHFile) {
                    if (block != null) {
                        bestMaybeNotFull = block;
                        break;
                    }
                }
            }
            if (bestMaybeNotFull == null) return null;
            if (bestMaybeNotFull.isFullDesign) return bestMaybeNotFull;
            if (latestFullInMFile != null && DesignBlock.isCompatible(bestMaybeNotFull, latestFullInMFile)) {
                return latestFullInMFile;
            } else if (latestInHFile[observer] != null && latestInHFile[observer].isFullDesign && DesignBlock.isCompatible(bestMaybeNotFull, latestInHFile[observer])) {
                return latestInHFile[observer];
            } else {
                for (DesignBlock block : latestInHFile) {
                    if (block != null && block.isFullDesign && DesignBlock.isCompatible(block, bestMaybeNotFull)) {
                        return block;
                    }
                }
            }
            return bestMaybeNotFull;
        }
        
        void consider(DesignBlock block, int observer, boolean isMFile, int turn) {
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
                latestInHFile[observer] = block;
            }
        }
    }
}
