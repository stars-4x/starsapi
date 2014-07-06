package org.starsautohost.starsapi.encryption;

/**
 * Stars Random Number Generator
 * 
 * This needs to be seeded and is used for decryption.
 * 
 * Each new random number uses new seeds based on previous seeds and
 * some possibly random constants
 */
public class StarsRandom {

	// We'll use 'long' for our seeds to avoid signed-integer problems
	private long seedA;
	private long seedB;
	
	private int rounds;
	
	public StarsRandom(int prime1, int prime2, int initRounds) {
		seedA = prime1;
		seedB = prime2;
		rounds = initRounds;
		
		// Now initialize a few rounds
		for(int i = 0; i < rounds; i++)
			nextRandom();
	}
	
	/**
	 * Get the next random number with this seeded generator
	 * 
	 * @return
	 */
	public long nextRandom() {
		// First, calculate new seeds using some constants
		long seedApartA = (seedA % 53668) * 40014;
		long seedApartB = (seedA / 53668) * 12211;  // integer division OK
		long newSeedA = seedApartA - seedApartB;
		
		long seedBpartA = (seedB % 52774) * 40692;
		long seedBpartB = (seedB / 52774) * 3791;
		long newSeedB = seedBpartA - seedBpartB;
		
		// If negative add a whole bunch (there's probably some weird bit math
		// going on here that the disassembler didn't make obvious)
		if(newSeedA < 0)
			newSeedA += 0x7fffffab;

		if(newSeedB < 0)
			newSeedB += 0x7fffff07;
		
		// Set our new seeds
		seedA = newSeedA;
		seedB = newSeedB;
		
		// Generate "random" number.  This will fit into an unsigned 32bit integer
		// We use 'long' because...  java...
		long randomNumber = seedA - seedB;
		if(seedA < seedB)
			randomNumber += 0x100000000l;  // 2^32

		// DEBUG
//		System.out.println("seed1: " + seedA + "; seed2: " + seedB);
//		System.out.println("rand: " + randomNumber);
		
		// Now return our random number
		return randomNumber;
	}
	
	
	@Override
	public String toString() {
		String s = "Random Number Generator:\n";
		
		s += "Seed 1: " + seedA + "\n";
		s += "Seed 2: " + seedB + "\n";
		s += "Rounds: " + rounds + "\n";
		
		return s;
	}
}
