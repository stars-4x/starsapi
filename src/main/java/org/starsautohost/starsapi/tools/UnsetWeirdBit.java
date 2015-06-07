package org.starsautohost.starsapi.tools;

import java.util.List;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class UnsetWeirdBit {
    public static void main(String[] args) throws Exception {
        List<Block> blocks = new Decryptor().readFile(args[0]);
        for (Block block : blocks) {
            if (block instanceof PartialPlanetBlock) {
                ((PartialPlanetBlock) block).weirdBit = false;
            }
        }
        new Decryptor().writeBlocks(System.out, blocks);
    }
}
