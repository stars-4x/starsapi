package org.starsautohost.starsapi.tools;

import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
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
            if (block.typeId == BlockType.FILE_HEADER) System.out.println(block);
            else System.out.println(block.typeId + ": " + Util.bytesToString(block.getDecryptedData(), 0, block.size));
        }
    }

}
