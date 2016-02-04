package org.starsautohost.starsapi.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

/**
 * @author Runar
 *
 */
public class GetPassword {

	private static char[] simpleChars = "abcdefghijklmnopqrstuvwxyz".toCharArray();
	private static char[] fullChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	private static long start = 0;
	
	public static void main(String[] args) throws Exception{
		if (args.length < 1){
			System.out.println("Usage: <file or dir>");
			System.exit(0);
		}
		new GetPassword(args);
	}
	
	public GetPassword(String[] args) throws Exception{
		File f = new File(args[0]);
		if (f.exists() == false) f = new File("..\\"+args[0]); //Due to bin-catalog, try this too just in case.
		if (f.isDirectory()){
			File[] files = f.listFiles();
			Vector<File> v = new Vector<File>();
			for (File ff : files){
				String name = ff.getName().toLowerCase();
				if (name.matches(".*\\.[m|M].") || name.matches(".*\\.[r|R].")){
					v.addElement(ff);
					System.out.println(v.size()+": "+ff.getName());
				}
			}
			if (v.size() == 0){
				System.out.println("No R or M-files in directory.");
				System.exit(0);
			}
			System.out.println("Enter the number for the file you want.");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String s = in.readLine();
			f = v.elementAt(Integer.parseInt(s)-1);
			in.close();
			System.out.println("Selected file: "+f.getName());
		}
		List<Block> v1 = new Decryptor().readFile(f.getAbsolutePath());
		PlayerBlock pb = null;
		for (Block b : v1){
			if (b instanceof PlayerBlock) pb = (PlayerBlock)b;
		}
		if (pb == null){
			System.out.println("Could not find an instance of PlayerBlock in given file.");
			System.exit(0);
		}
		byte[] b = pb.getDecryptedData();
		int i = ((b[15]&0xff) << 24) | ((b[14]&0xff) << 16) | ((b[13]&0xff) << 8)  | (b[12]&0xff);
		System.out.println("Printing hash value (target for brute force search)");
		System.out.println(i + " " + Integer.toBinaryString(i)+" ("+Integer.toBinaryString(i).length()+")");
		System.out.println("Starting");
		start = System.currentTimeMillis();
		DecryptThread t1 = new DecryptThread(1,simpleChars,i);
		DecryptThread t2 = new DecryptThread(2,fullChars,i);
	}
	
	private class DecryptThread extends Thread{
		private int threadNr;
		private char[] chars;
		private int i;
		
		public DecryptThread(int threadNr, char[] chars, int i){
			this.threadNr = threadNr;
			this.chars = chars;
			this.i = i;
			start();
		}
		
		public void run(){
			for (int t = 1; t <= 16; t++){
				System.out.print(threadNr+" Trying with "+t+" characters.");
				long l = System.currentTimeMillis();
				generatePermutations(chars, t, 0, "", i);
				System.out.println(" "+(System.currentTimeMillis()-l)+" ms");
			}
			System.out.println(threadNr+" Giving up.");
			System.out.println("Total time: "+(System.currentTimeMillis()-start)+" ms");
		}
	}
	
	private static int getHash(String s){
		int val = 0;
		for (int t = 0; t < s.length(); t++){
			int i = (char)s.charAt(t);
			if (t % 2 == 0) val += i;
			else val *= i;
		}
		return val;
	}
	
	private static void generatePermutations(char[] chars, int nr, int depth, String current, int hash){
	    Vector<String> v = new Vector<String>();
		if (depth == nr){
	    	if (hash == getHash(current)){ //getHash(new String(s))){
				System.out.println("RESULT: "+current);
				System.out.println("Total time: "+(System.currentTimeMillis()-start)+" ms");
				v.addElement(current);
				//System.exit(0);
			}
	    	return;
	    }
	    for (int i = 0; i < chars.length; i++){
	        generatePermutations(chars, nr, depth + 1, current + chars[i], hash);
	    }
	    if (v.size() > 0) System.exit(0);
	}
}
