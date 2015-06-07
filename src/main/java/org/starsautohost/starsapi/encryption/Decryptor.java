package org.starsautohost.starsapi.encryption;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.BlockType;
import org.starsautohost.starsapi.block.FileHeaderBlock;
import org.starsautohost.starsapi.block.PlanetsBlock;


/**
 * @author raptor
 * 
 * Algorithms ruthlessly taken from the decompiled StarsHostEditor 0.3 .NET 
 * DLL.  Thanks to all those who did the hard work in a disassembler to 
 * figure these out.
 * 
 * Note:   Requires JRE 1.6 or later
 * Note 2: Java does not have unsigned data types which made some of this work
 *         a little trickier.  (see Util.ubyteToInt)
 */
public class Decryptor   
{
	private static int BLOCK_HEADER_SIZE = 2;  // bytes
	private static int BLOCK_MAX_SIZE = 1024;  // bytes
	
	/**
	 * The first 64 prime numbers, after '2' (so all are odd). These are used 
	 * as starting seeds to the random number generator.
	 * 
	 * IMPORTANT:  One number here is not prime (279).  I thought it should be 
	 * replaced with 269, which is prime.  StarsHostEditor 0.3 decompiled source
	 * uses 279 and it turns out that an analysis of the stars EXE with a hex editor
	 * also shows a primes table with 279.  Fun!
	 */
    private static final int[] primes = new int[] { 
    		3, 5, 7, 11, 13, 17, 19, 23, 
    		29, 31, 37, 41, 43, 47, 53, 59,
    		61, 67, 71, 73, 79, 83, 89, 97,
    		101, 103, 107, 109, 113, 127, 131, 137,
    		139, 149, 151, 157, 163, 167, 173, 179,
    		181, 191, 193, 197, 199, 211, 223, 227,
    		229, 233, 239, 241, 251, 257, 263, 279,
    		271, 277, 281, 283, 293, 307, 311, 313 
    };


	private StarsRandom random = null;
    
	
	/**
	 * Initialize the decryption system by seeding and initializing a
	 * random number generator
	 * @param block 
	 * 
	 * @throws Exception
	 */
	private void initDecryption(FileHeaderBlock fileHeaderBlock) throws Exception {
		int salt = fileHeaderBlock.encryptionSalt;
		
		// Use two prime numbers as random seeds.
		// First one comes from the lower 5 bits of the salt
		int index1 = salt & 0x1F;
		// Second index comes from the next higher 5 bits
		int index2 = (salt >> 5) & 0x1F;
		
		// Adjust our indexes if the highest bit (bit 11) is set
		// If set, change index1 to use the upper half of our primes table
		if((salt >> 10) == 1)
			index1 += 32;
		// Else index2 uses the upper half of the primes table
		else
			index2 += 32;
		
		// Determine the number of initialization rounds from 4 other data points
		// 0 or 1 if shareware (I think this is correct, but may not be - so far
		// I have not encountered a shareware flag
		int part1 = fileHeaderBlock.shareware ? 1 : 0;
		
		// Lower 2 bits of player number, plus 1
		int part2 = (fileHeaderBlock.playerNumber & 0x3) + 1;
		
		// Lower 2 bits of turn number, plus 1
		int part3 = (fileHeaderBlock.turn & 0x3) + 1;
		
		// Lower 2 bits of gameId, plus 1
		int part4 = ((int) fileHeaderBlock.gameId & 0x3) + 1;
		
		// Now put them all together, this could conceivably generate up to 65 
		// rounds  (4 * 4 * 4) + 1
		int rounds = (part4 * part3 * part2) + part1;
		
		// Now initialize our random number generator
		random = new StarsRandom(primes[index1], primes[index2], rounds);

		// DEBUG
//		System.out.println(random);
	}


	/**
	 * Decrypt the given block.
	 * 
	 * The first call to this will be the File Header Block which will
	 * be used to initialize the decryption system
	 * 
	 * @param block
	 * @throws Exception
	 */
	private void decryptBlock(Block block) throws Exception {
		// If it's a header block, it's unencrypted and will be used to 
		// initialize the decryption system.  We have to decode it first
		if(block.typeId == BlockType.FILE_HEADER) {
			block.encrypted = false;
			block.decode();
			
			initDecryption((FileHeaderBlock) block);
			
			return;
		}

		byte[] encryptedData = block.getData();
		
		byte[] decryptedData = new byte[encryptedData.length];
		
		// Now decrypt, processing 4 bytes at a time
		for(int i = 0; i < encryptedData.length; i+=4) {
			// Swap bytes:  4 3 2 1
			long chunk = (Util.read8(encryptedData[i+3]) << 24)
					| (Util.read8(encryptedData[i+2]) << 16)
					| (Util.read8(encryptedData[i+1]) << 8)
					| Util.read8(encryptedData[i]);
			
//			System.out.println("chunk  : " + Integer.toHexString((int)chunk));
			
			// XOR with a random number
			long decryptedChunk = chunk ^ random.nextRandom();
//			System.out.println("dechunk: " + Integer.toHexString((int)decryptedChunk));
			
			// Write out the decrypted data, swapped back
			decryptedData[i] =  (byte) (decryptedChunk & 0xFF);
			decryptedData[i+1] =  (byte) ((decryptedChunk >> 8)  & 0xFF);
			decryptedData[i+2] =  (byte) ((decryptedChunk >> 16)  & 0xFF);
			decryptedData[i+3] =  (byte) ((decryptedChunk >> 24)  & 0xFF);
		}
		
		block.setDecryptedData(decryptedData, block.size);
	}

	/**
     * Encrypt the given block.
     * 
     * The first call to this will be the File Header Block which will
     * be used to initialize the encryption system
     * 
     * @param block
     * @throws Exception
     */
    private void encryptBlock(Block block) throws Exception {
        // If it's a header block, it's unencrypted and will be used to 
        // initialize the decryption system.  We have to decode it first
        if(block.typeId == BlockType.FILE_HEADER) {
            block.encrypted = false;
            block.decode();
            
            initDecryption((FileHeaderBlock) block);
            
            return;
        }
        
        block.encrypted = true;

        byte[] decryptedData = block.getDecryptedData();
        
        byte[] encryptedData = new byte[decryptedData.length];
        
        // Now encrypt, processing 4 bytes at a time
        for(int i = 0; i < decryptedData.length; i+=4) {
            // Swap bytes:  4 3 2 1
            long chunk = (Util.read8(decryptedData[i+3]) << 24)
                    | (Util.read8(decryptedData[i+2]) << 16)
                    | (Util.read8(decryptedData[i+1]) << 8)
                    | Util.read8(decryptedData[i]);
            
//          System.out.println("chunk  : " + Integer.toHexString((int)chunk));
            
            // XOR with a random number
            long encryptedChunk = chunk ^ random.nextRandom();
//          System.out.println("dechunk: " + Integer.toHexString((int)decryptedChunk));
            
            // Write out the decrypted data, swapped back
            encryptedData[i] =  (byte) (encryptedChunk & 0xFF);
            encryptedData[i+1] =  (byte) ((encryptedChunk >> 8)  & 0xFF);
            encryptedData[i+2] =  (byte) ((encryptedChunk >> 16)  & 0xFF);
            encryptedData[i+3] =  (byte) ((encryptedChunk >> 24)  & 0xFF);
        }
        
        block.setData(encryptedData, block.size);
    }

	
	/**
	 * This will detect and return a block with its type, size, and block of the 
	 * given block from the given data
	 * 
	 * Details of a header block bitwise: XXXXXXXX YYYYYYZZ
     *   (XXXXXXXX is a first byte, YYYYYYZZ is a second byte) 
     *   
	 * Where:
     *   YYYYYY is a block type.
     *   ZZXXXXXXXX is a block size. 
	 * 
	 * @param offset
	 * @param fileBytes
	 * @return
	 * @throws Exception 
	 */
	private Block parseBlock(byte[] fileBytes, int offset) throws Exception {
		// We have to do a bitwise AND with 0xFF to convert from unsigned byte to int
		int header = Util.read16(fileBytes, offset);

		int typeId = header >> 10;
		int size = header & 0x3FF;
		
		if(size > BLOCK_MAX_SIZE)
			throw new Exception("Bad block size: " + size + "; typeId: " + typeId);
		
		// We must have a padded byte array because decryption works on 4
		// bytes at a time
		byte[] data = new byte[Block.pad(size)];
		
		// Now copy the block data from the file byte array
		System.arraycopy(fileBytes, offset + 2, data, 0, size);
		
		// This will create the appropriate Block-type object according to the typeId
		Class<? extends Block> blockClass = BlockType.getBlockClass(typeId);
		
		Block block = blockClass.newInstance();
		block.setData(data, size);
		
		return block;
	}

	/**
	 * Some blocks have more data at the end of the block (like PLANETS).  
	 * Detect this, parse the data, and return the size of the data.
	 * 
	 * Requires decryption to have been done on the block data
	 * 
	 * @param offset
	 * @param fileBytes
	 * @param block
	 * @return
	 */
	private int postProcessBlock(byte[] fileBytes, int offset, Block block) {
		int size = 0;
		
		if(block.typeId == BlockType.PLANETS) {
			PlanetsBlock planetsBlock = (PlanetsBlock) block;
			
			// There are 4 bytes per planet
			size = planetsBlock.planetsSize * 4;
			planetsBlock.planetsDataSize = size;
			
			planetsBlock.planetsData = Arrays.copyOfRange(fileBytes, offset, offset + size);
		}
		
		return size;
	}
	

	/**
	 * Read in a Stars! file to decrypt it.  This returns a List of all blocks found
	 * within.  Each encrypted block will be decrypted.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public List<Block> readFile(String filename) throws Exception {
		
		// Read in the full file to a byte array...  we have the RAM
		File file = new File(filename);
		InputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));
		ByteArrayOutputStream bout = new ByteArrayOutputStream((int) file.length());
		try {
		    byte[] buf = new byte[8192];
		    int r;
		    while ((r = fileInputStream.read(buf)) > 0) {
		        bout.write(buf, 0, r);
		    }
		} finally {
		    fileInputStream.close();
		}
		byte[] fileBytes = bout.toByteArray();
		
		// Round 1: Block-parsing
		List<Block> blockList = new ArrayList<Block>();
		
		// Index where we start to read the next block
		int offset = 0;
		
		while(offset < fileBytes.length) {
			// Initial parse of our block
			Block block = parseBlock(fileBytes, offset);
			
			// Do the decryption!
			decryptBlock(block);
			
			// Decode!
			try {
			    block.decode();
			} catch (RuntimeException e) {
			    System.out.println("Error (" + e + ") decoding: " + block);
			    throw e;
			}

			// Advance our read index
			offset = offset + block.size + BLOCK_HEADER_SIZE;
			
			// Check to see if we need to grab even more data before the next block
			int dataSize = postProcessBlock(fileBytes, offset, block);
			
			// Advance the index again
			offset = offset + dataSize;

			// DEBUG
			//System.out.println(block);
			
			// Store block for later parsing
			blockList.add(block);
		}

		return blockList;
	}

    /**
     * Write a block to an output stream.  Assume already encrypted if needed.
     */
    private void writeBlock(OutputStream out, Block block) throws Exception {
        int header = (block.typeId << 10) | block.size;
        out.write(header & 0xFF);
        out.write((header >> 8) & 0xFF);
        out.write(block.getData(), 0, block.size);
        if(block.typeId == BlockType.PLANETS) {
            PlanetsBlock planetsBlock = (PlanetsBlock) block;
            out.write(planetsBlock.planetsData);
        }       
    }

    /**
     * Write blocks to an output stream.  Encrypt as needed.
     */
    public void writeBlocks(OutputStream out, List<Block> blocks) throws Exception {
        for (Block block : blocks) {
            block.encode();
            encryptBlock(block);
            writeBlock(out, block);
        }
    }

    /**
     * Write blocks to an file.  Encrypt as needed.
     */
    public void writeBlocks(String filename, List<Block> blocks) throws Exception {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
        try {
            writeBlocks(out, blocks);
        } finally {
            out.close();
        }
    }
}


