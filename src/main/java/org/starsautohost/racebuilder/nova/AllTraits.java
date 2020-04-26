package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class AllTraits {
     public TraitList all = new TraitList();
     public TraitList primary = new TraitList();
     public TraitList secondary = new TraitList();

     private static final Object padlock = new Object();
     private static AllTraits instance;

     /// <summary>
     /// Private constructor to prevent anyone else creating instances of this class.
     /// </summary>
     private AllTraits()
     {
         for (TraitEntry trait : PrimaryTraits.traits)
         {
             all.add(trait);
             primary.add(trait);
         }

         for (TraitEntry trait : SecondaryTraits.traits)
         {
             all.add(trait);
             secondary.add(trait);
         }
     }

     /// <summary>
     /// Provide a mechanism of accessing the single instance of this class that we
     /// will create locally. Creation of the data is thread-safe.
     /// </summary>
     public static AllTraits getData(){
         if (instance == null) {
             synchronized (padlock){
                 if (instance == null)
                 {
                     instance = new AllTraits();
                 }
             }
         }
         return instance;
     }
     
	public static void setData(AllTraits value){
		instance = value;
	}
     public final int numberOfPrimaryRacialTraits = 10;
     public final int numberOfSecondaryRacialTraits = 14;
   
     /// <summary>
     /// Provide a list of all trait keys. 
     /// These can be used to index AllTraits.Data.All or in a foreach loop.
     /// </summary>
     public static String[] traitKeys = 
     {
         // 10 PRTs
         "HE", "SS", "WM", "CA", "IS", "SD", "PP", "IT", "AR", "JOAT",
         // 14 LRTs
         "IFE", "TT", "ARM", "ISB", "GR", "UR", "MA", "NRS", "CE", "OBRM", "NAS", "LSP", "BET", "RS"
     };

     public static int getIndex(String code) {
 		for (int t = 0; t < traitKeys.length; t++){
 			if (code.equals(traitKeys[t])) return t;
 		}
    	return -1;
 	 }
     public static int getSecondaryIndex(String code){
    	int index = getIndex(code);
    	if (index == -1) return -1;
    	return index - 10;
     }
     
     /// <summary>
     /// Provide a list of all the trait names. This can be used to get the printable name of a trait.
     /// </summary>
     public static String[] traitString = 
     {
         // 10 PRTs
         "Hyper Expansion", 
         "Super Stealth", 
         "War Monger", 
         "Claim Adjuster", 
         "Inner Strength", 
         "Space Demolition", 
         "Packet Pysics", 
         "Interstellar Traveler", 
         "Artificial Reality", 
         "Jack of all Trades",
         // 14 LRTs
         "Improved Fuel Efficiency", 
         "Total Terraforming", 
         "Advanced Remote Mining", 
         "Improved Star Bases", 
         "Generalised Research", 
         "Ultimate Recycling", 
         "Mineral Alchemy", 
         "No Ram Scoop Engines", 
         "Cheap Engines",
         "Only Basic Remote Mining", 
         "No Advanced Scanners", 
         "Low Starting Population", 
         "Bleeding Edge Technology", 
         "Regenerating Shields"
     };

	
}
