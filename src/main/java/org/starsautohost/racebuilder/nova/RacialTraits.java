package org.starsautohost.racebuilder.nova;

import java.util.Vector;

/*
 * Modified from Stars Nova project
 */
public class RacialTraits extends TraitList{
	
	
     private TraitEntry primaryTrait = AllTraits.getData().primary.get("JOAT"); // Start with some default primary trait

     /// <summary>
     /// Default constructor.
     /// </summary>
     public RacialTraits()
     {
     }

     /// <summary>
     /// Loop through all of a races traits, starting with the primary trait.
     /// </summary>
     /// <returns>Each of the race's traits, begining with the PrimaryTrait, 
     /// followed by any lesser traits.</returns>
     public Vector<TraitEntry> getEnumerator()
     {
    	 Vector<TraitEntry> v = new Vector<TraitEntry>();
         v.add(this.primaryTrait);
    	 for (TraitEntry trait : getValues())
         {
             v.add(trait);
         }
    	 return v;
     }

     /// <summary>
     /// Check if the racial traits contains a particular trait.
     /// </summary>
     /// <param name="trait"></param>
     /// <returns>True if trait is the race's PrimaryTrait or a lesser trait.</returns>
     public boolean contains(String trait)
     {
         if (this.primaryTrait.code.equals(trait))
         {
             return true;
         }
         return super.contains(trait);
     }

     /// <summary>
     /// Control access to the primary trait. It can be read as a public property. 
     /// It can be set using the SetPrimary() accessor function passing either a 
     /// TraitEntry or a String containing one of the primary trait codes.
     /// </summary>
     /// <remarks>Did not use a set method as I needed to overload depending on 
     /// whether a TraitEntry or a String is used to set the Primary racial trait.</remarks>
     public TraitEntry getPrimary()
     {
         return this.primaryTrait;
     }

     /// <summary>
     /// Control access to the primary trait. It can be read as a public property. 
     /// It can be set using the SetPrimary() accessor function passing either a 
     /// TraitEntry or a String containing one of the primary trait codes.
     /// </summary>
     /// <param name="primaryTrait">The new primary trait.</param>
     public void SetPrimary(TraitEntry primaryTrait)
     {
         this.primaryTrait = primaryTrait;
     }

     /// <summary>
     /// Control access to the primary trait. It can be read as a public property. 
     /// It can be set using the SetPrimary() accessor function passing either a 
     /// TraitEntry or a String containing one of the primary trait codes.
     /// </summary>
     /// <param name="primaryTrait">The new primary trait.</param>
     public void setPrimary(String primaryTrait) throws Exception
     {
         for (TraitEntry trait : AllTraits.getData().primary.getValues())
         {
             if (trait.code.equals(primaryTrait) || trait.name.equals(primaryTrait))
             {
                 this.primaryTrait = trait;
                 return;
             }
         }

         throw new Exception("The primaryTrait \"" + primaryTrait + "\" is not recognised. Failed to set primary trait.");
     }
 }
