package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */ 
public class TraitEntry{
     public String name;        // e.g. "Hyper Expansion" or "Regenerating Shields" (may contain spaces)
     public String code;        // e.g. "HE" or "RS" (must be unique, all caps, no spaces or punctuation) 
     public String description; // Detailed description (paragraph).

     /// <summary>
     /// Trait constructor. In most instances a string containing the trait code is 
     /// sufficient. Use this when it is necessary to have acess to all the details of a trait.
     /// </summary>
     /// <param name="n">Name e.g. "Hyper Expansion" or "Regenerating Shields" (may contain spaces).</param>
     /// <param name="a">Code e.g. "HE" or "RS" (must be unique, all caps, no spaces or punctuation).</param>
     /// <param name="c">Cost in advantage points, negative cost give more points to buy other things.</param>
     /// <param name="d">Detailed description (paragraph).</param>
     public TraitEntry(String n, String a, String d)
     {
         name = n;
         code = a;
         description = d;
     }

     /// <summary>
     /// Return the TraitKey for a TraitEntry.
     /// </summary>
     @Override
     public int hashCode() {
         int hash = 1;
         for (String key : AllTraits.traitKeys){
             if (key.equals(this.code))
             { 
                 return hash; 
             }
             else
             {
                 ++hash;
             }
         }
         return hash;
     }

     /// <summary>
     /// Return a String representation of the TraitEntry.
     /// </summary>
     /// <returns></returns>
     @Override
     public String toString()
     {
         return name;
     }

     /// <summary>
     /// Test for equality.
     /// </summary>
     /// <param name="a">Trait to compare.</param>
     /// <param name="b">Trait to compare.</param>
     /// <returns>Returns true if the traits are the same trait.</returns>
     @Override
     public boolean equals(Object o){
    	 if (o instanceof TraitEntry == false) return false;
    	 TraitEntry te = (TraitEntry)o;
    	 return code.equals(te.code);
     }

}
