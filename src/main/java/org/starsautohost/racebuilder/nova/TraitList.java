package org.starsautohost.racebuilder.nova;

import java.util.Collection;
import java.util.HashMap;

public class TraitList {
	
	HashMap<String,TraitEntry> hm = new HashMap<String,TraitEntry>();
     /// <summary>
     /// Add a new trait to the race's collection of traits.
     /// </summary>
     /// <param name="new_trait">A TraitEntry, such as those in SecondaryTraits.</param>
     public void add(TraitEntry new_trait)
     {
         hm.put(new_trait.code, new_trait);
     }

     /// <summary>
     /// Add a new trait to the race's collection of traits.
     /// </summary>
     /// <param name="newTrait">The code or short name of a trait such as IS for Improved Starbases. These are defined in AllTraits.</param>
     public void add(String newTrait)
     {
         for (TraitEntry trait : AllTraits.getData().secondary.getValues())
         {
        	 if (trait.code.equals(newTrait))
             {
                 hm.put(trait.code, trait);
             }
         }
     }

     public Collection<TraitEntry> getValues() {
		return hm.values();
	}

	/// <summary>
     /// Remove a trait from the race's collection of traits.
     /// </summary>
     /// <param name="traitToRemove">The trait to remove.</param>
     public void remove(TraitEntry traitToRemove)
     {
         hm.remove(traitToRemove.code);
     }

     /// <summary>
     /// Remove a trait from the race's collection of traits.
     /// </summary>
     /// <param name="traitToRemove">The code (short name) for the trait to remove, as defined in AllTraits.</param>
     public void remove(String traitToRemove)
     {
         hm.remove(traitToRemove);
     }

     /// <summary>
     /// Check if the racial traits contains a particular trait.
     /// </summary>
     /// <param name="trait"></param>
     /// <returns></returns>
     public boolean contains(String trait)
     {
         return hm.get(trait) != null;
     }

     /// <summary>
     /// Allow array type indexing to a TraitList.
     /// </summary>
     /// <param name="index">The code (short name) for the trait, as defined in AllTraits.</param>
     /// <returns>The TraitEntry for the given trait code.</returns>
     public TraitEntry get(String key){
     {
         return hm.get(key);
     }
 }
}
