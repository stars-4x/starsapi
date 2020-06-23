package org.starsautohost.starsapi.tools;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

/**
 * A conversion of StarsAI.pl from https://github.com/ricks03/TotalHost/blob/master/scripts/StarsAI.pl
 *
 */
public class StarsHumanToAiChanger {

	private static final String[] aiStatus = {"Human", "Inactive", "CA", "PP", "HE", "IS", "SS", "AR"};
	private static final String[] prts = {"HE","SS","WM","CA","IS","SD","PP","IT","AR","JOAT"};
	
	private static String join(String sep, String[] array){
		String s = array[0];
		for (int t = 1; t < array.length; t++){
			s += sep;
			s += array[t];
		}
		return s;
	}
	
	public static void main(String[] args) throws Exception{
		int debug = 0;
		if (args.length < 3){
			System.out.println("Usage: <Game HST file> <PlayerID 1-16> <new AI status > <output file (optional)>");
			System.out.println("Example:");
			System.out.println("c:\\games\\test.HST 1 Inactive");
			System.out.println("Changes the first player to Inactive");
			System.out.println("Possible Player Status options: "+join(",",aiStatus));
			System.out.println("By default, a new file will be created: <filename>.clean");
			System.out.println("You can create a different file with <filename> <PlayerID 1-16> <new AI status> <newfilename>");
			System.out.println("<filename> <PlayerID 1-16> <new AI status> <filename> will overwrite the original file.");
			System.out.println("As always when using any tool, it's a good idea to back up your file(s).");
			System.exit(0);
		}
		new StarsHumanToAiChanger().change(args[0],Integer.parseInt(args[1]),args[2],args.length>3?args[3]:null);
	}
	
	private void change(String fileName, int playerId, String newAi, String outFileName) throws Exception{
		File f = new File(fileName);
		if (f.exists() == false || f.isDirectory() || f.getName().toLowerCase().endsWith(".hst") == false) throw new Exception("HST file not found");
		if (playerId > 16 || playerId < 1) throw new Exception("Player ID must be between 1 and 16");
		playerId--;
		if (Arrays.asList(aiStatus).contains(newAi) == false) throw new Exception("Undefined player status specified.");
		String baseFile = f.getName();
		//File dir = dirName(f);
		//my ($basefile, $dir, $ext);
		//$basefile = basename($filename);    # mygamename.m1
		//$dir  = dirname($filename);         # c:\stars
		Decryptor d = new Decryptor();
		List<Block> blocks = d.readFile(f.getAbsolutePath());
		for (Block b : blocks){
			if (b instanceof PlayerBlock){
				PlayerBlock pb = (PlayerBlock)b;
				changePlayerBlock(pb,playerId,newAi);
			}
		}
		File newFile = outFileName!=null?new File(f.getParentFile(),outFileName):new File(f.getParentFile(),baseFile+".clean"); 
		d.writeBlocks(newFile.getAbsolutePath(), blocks, false);	  
		System.out.println("File output: "+newFile.getName());
		if (outFileName == null) System.out.println("Don't forget to rename the file.");
	}
	
	private int changePlayerBlock(PlayerBlock pb, int playerId, String newAI) throws Exception{
		int action = 0;
		byte[] decryptedData = pb.getDecryptedData();
        if (pb.playerNumber == playerId) {
          if (pb.fullDataFlag) {
        	  int prt = decryptedData[76]; //HE SS WM CA IS SD PP IT AR JOAT  
        	  System.out.println("Current PRT: "+prts[prt]);
          }
          action = 1;
          // Have to handle the password change differently for human <> inactive
          if (newAI.equals("Human")) {
            if (decryptedData[7] == (byte)225 || decryptedData[7] == (byte)1) { 
              	System.out.println("Already Human");
            } else if (decryptedData[7] == (byte)227 ) {
            	System.out.println("Changing from Human(Inactive) AI to Human");
            	decryptedData[7] = (byte)225;
            	//The bits for the password of an inactive player are the inverse of the 
            	//bits of the password for an active player 
            	//Flip the bits of the password
            	decryptedData[12] = (byte)Util.read8((byte)~decryptedData[12]);
            	decryptedData[13] = (byte)Util.read8((byte)~decryptedData[13]);
            	decryptedData[14] = (byte)Util.read8((byte)~decryptedData[14]);
            	decryptedData[15] = (byte)Util.read8((byte)~decryptedData[15]);
            } else {
            	System.out.println("Changing from AI to Human");
            	decryptedData[7] = (byte)225;
            	//Reset the AI password to blank for human use
            	decryptedData[12] = 0;
            	decryptedData[13] = 0;
            	decryptedData[14] = 0;
            	decryptedData[15] = 0;
            }
          } else if (newAI.equals("Inactive")) {
        	  if (decryptedData[7] == (byte)227 ) {
        		  System.out.println("Already Inactive AI");
        	  } else if (decryptedData[7] == (byte)225 || decryptedData[7] == (byte)1) { 
        		  System.out.println("Changing from Human to Human(Inactive) AI");
        		  decryptedData[7] = (byte)225;
        		  //The bits for the password of an inactive player are the inverse of the 
        		  //bits of the password for an active player 
        		  //Flip the bits of the password
        		  decryptedData[12] = (byte)Util.read8((byte)~decryptedData[12]);
        		  decryptedData[13] = (byte)Util.read8((byte)~decryptedData[13]);
        		  decryptedData[14] = (byte)Util.read8((byte)~decryptedData[14]);
        		  decryptedData[15] = (byte)Util.read8((byte)~decryptedData[15]);
        	  } else {
        		  System.out.println("Changing from Full AI to Human(Inactive) AI");
        		  decryptedData[7] = (byte)227;
        		  //The inverse of a blank password
        		  decryptedData[12] = (byte)255;
        		  decryptedData[13] = (byte)255;
        		  decryptedData[14] = (byte)255;
        		  decryptedData[15] = (byte)255;
        	  }
          } else { 
            //Setting to one of the AIs
            //Set the standard AI password              
            decryptedData[12] = (byte)238;
            decryptedData[13] = (byte)171;
            decryptedData[14] = (byte)77;
            decryptedData[15] = (byte)9;
            System.out.println("Changing to "+newAI+" AI");
            // Use the Expert values for the AIs
            if (newAI.equals("CA"))      {  decryptedData[7] = (byte)111;  System.out.println("Does not expect IFE. Expects TT/OBRM/NAS"); 
            } else if (newAI.equals("PP" )) {  decryptedData[7] = (byte)143;  System.out.println("Expects IFE/TT/OBRM/NAS. The PP AI appears brain dead for non-PP PRTs.");
            } else if (newAI.equals("HE" )) {  decryptedData[7] = (byte)15;   System.out.println("Expects IFE/OBRM.");
            } else if (newAI.equals("IS" )) {  decryptedData[7] = (byte)79;   System.out.println("Does not expect IFE. Expects OBRM/NAS");
            } else if (newAI.equals("SS" )) {  decryptedData[7] = (byte)47;   System.out.println("Expects IFE/ARM.");
            } else if (newAI.equals("AR" )) {  decryptedData[7] = (byte)175;  System.out.println("Expects IFE/TT/ARM/ISB.");
            } 
          } // End of $newAI
        } // End of PlayerId 
        // END OF MAGIC
        return action;
	}	
}
