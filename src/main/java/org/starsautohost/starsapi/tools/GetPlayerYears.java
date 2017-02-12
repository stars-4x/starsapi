package org.starsautohost.starsapi.tools;

import java.io.File;
import java.util.List;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

public class GetPlayerYears {

	public static void main(String[] args) throws Exception{
		if (args.length < 1){
			System.out.println("Usage: <dir>");
			System.out.println("Should contain subdirectories with backup files with hst-files.");
			System.exit(0);
		}
		File dir = new File(args[0]);
		if (dir.isDirectory() == false) System.exit(0);
		for (File d : dir.listFiles()){
			if (d.isDirectory() == false) continue;
			for (File f : d.listFiles()){
				if (f.getName().toLowerCase().contains(".hst")){
					List<Block> blocks = new Decryptor().readFile(f.getAbsolutePath());
					for (Block b : blocks){
						if (b instanceof FileHeaderBlock){
							FileHeaderBlock fh = (FileHeaderBlock)b;
							System.out.println(f.getParentFile().getName()+": "+fh.turn);
						}
					}
				}
			}
		}
	}
}
