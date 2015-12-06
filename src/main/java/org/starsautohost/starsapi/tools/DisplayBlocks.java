package org.starsautohost.starsapi.tools;

import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
import org.starsautohost.starsapi.block.DesignBlock;
import org.starsautohost.starsapi.block.PartialFleetBlock;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class DisplayBlocks {
    private static boolean wantsHelp(String[] args) {
        if (args.length != 1) return true;
        for (String arg : args) {
            if (arg.equals("-h") || arg.equals("-help") || arg.equals("--help")) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        if (wantsHelp(args)) {
            System.out.println("Usage: java -jar DisplayBlocks.jar file");
            System.out.println();
            System.out.println("Displays the descrypted blocks of the file.");
            return;
        }

        List<Block> blocks = new Decryptor().readFile(args[0]);
        for (Block block : blocks) {
            byte[] origData = Arrays.copyOf(block.getDecryptedData(), block.size);
            block.encode();
            byte[] newData = Arrays.copyOf(block.getDecryptedData(), block.size);
            if (!Arrays.equals(origData, newData)) {
                System.out.println("WARNING: Block was changed by decoding and encoding");
                System.out.println(block.typeId + ": " + Util.bytesToString(origData, 0, block.size));
                System.out.println(block.typeId + ": " + Util.bytesToString(newData, 0, block.size));
            }
        }
        for (Block block : blocks) {
            if (block.typeId == BlockType.FILE_HEADER || block.typeId == BlockType.PLANETS) System.out.println(block);
            else System.out.println(block.typeId + ": " + Util.bytesToString(block.getDecryptedData(), 0, block.size));
            if (block instanceof PartialPlanetBlock || block instanceof PartialFleetBlock || block instanceof DesignBlock) System.out.println(block);
        }
    }

}
