package org.starsautohost.racebuilder.nova;
/*
 * Modified from Stars Nova project
 */
public class SecondaryTraits{
     private static final String ImprovedFuelEfficiencyDescription = "This gives you the Fuel Mizer and Galaxy Scoop engines and increases your starting propulsion tech by 1 level. All engines use 15% less fuel";
     private static final String TotalTerraformingDescription = "Allows you to terraform by investing solely in Biotechnology  You may terraform a variable up to 30%.  Terraforming costs 30% less.";
     private static final String AdvancedRemoteMiningDescription = "Gives you three additional mining hulls and two new robots. You will start the game with two Midget Miners. Do not pick Only Basic Remote Mining with this option (or you will get no benefit from this selection).";
     private static final String ImprovedStarBasesDescription = "Gives you two new starbase designs. The Stardock allows you to build light ships. The Ultra Station is a formidable weapons platform. Your starbases cost 20% less and are 20% cloaked.";
     private static final String GeneralisedResearchDescription = "Your race takes a holistic approach to research, Only half of the resources dedicated to research will be applied to the current field of research. However, 15% of the total will be applied to all other fields.";
     private static final String UltimateRecyclingDescription = "When you scarp a fleet at a starbase you recover 90% of the minerals and some of the resources. The resources are available for use the following year. Scrapping a fleet at a planet gives half the starbase amount.";
     private static final String MineralAlchemyDescription = "Allows you turn resources into minerals four times more efficiently than other races. This may be performed at any planet you own.";
     private static final String CheapFactoriesDescription = "Factories cost 1kT less Germanium to build.";
     private static final String NoRamScoopEnginesDescription = "No engines which travel at warp 5 or greater burning no fuel will be available. However, the Interspace 10 engine will be available. This drive will travel at warp 10 without taking damage.";
     private static final String CheapEnginesDecription = "You can throw together engines at half cost. However, at speeds greater than wap 6 there is a 10% chance the engines will not engage. You start with a propulsion one level higher than you would otherwise.";
     private static final String OnlyBasicRemoteMiningDescription = "The only mining ship available to you will be the Mini-Miner.This trait overrides Advanced Remote Mining. Your maximum population per planet is increased by 10%.";
     private static final String NoAdvancedScannersDecription = "No planet penetrating scanners will be available to you. However, conventional scanners will have their range doubled.";
     private static final String LowStartingPopulationDescription = "You start with 30% fewer colonists.";
     private static final String BleedingEdgeTechnologyDescription = "New techs initially cost twice as much to build. As soon as you exceed all of the tech requirements by one level the cost drops back to normal. Miniaturization occurs at 5% a level and pegs at 80%.";
     private static final String RegeneratingShieldsDescription = "All shields are 40% stronger than the listed rating. Shields regenerate 10% of maxium strength after every round of battle. However, your armor will only be 50% of its rated strength.";
     private static final String ExtraCostStartLevel4Description = "All Techs with 75% extra Costs start at Tech Level 3, for JOAT at Tech Level 4.";

     //I swapped the order here from nova to resemble stars race wizard
     public static final TraitEntry[] traits = new TraitEntry[]{
         new TraitEntry("Improved Fuel Efficiency", "IFE", ImprovedFuelEfficiencyDescription), 
         new TraitEntry("Total Terraforming", "TT", TotalTerraformingDescription),
         new TraitEntry("Advanced Remote Mining", "ARM", AdvancedRemoteMiningDescription),
         new TraitEntry("Improved Star Bases", "ISB", ImprovedStarBasesDescription),
         new TraitEntry("Generalised Research", "GR", GeneralisedResearchDescription),
         new TraitEntry("Ultimate Recycling", "UR", UltimateRecyclingDescription),
         new TraitEntry("Mineral Alchemy", "MA", MineralAlchemyDescription),
         new TraitEntry("No Ram Scoop Engines", "NRS", NoRamScoopEnginesDescription),
         new TraitEntry("Cheap Engines", "CE", CheapEnginesDecription),
         new TraitEntry("Only Basic Remote Mining", "OBRM", OnlyBasicRemoteMiningDescription),
         new TraitEntry("No Advanced Scanners", "NAS", NoAdvancedScannersDecription),
         new TraitEntry("Low Starting Population", "LSP", LowStartingPopulationDescription),
         new TraitEntry("Bleeding Edge Technology", "BET", BleedingEdgeTechnologyDescription),
         new TraitEntry("Regenerating Shields", "RS", RegeneratingShieldsDescription),
         //These are not normal PRT's
         new TraitEntry("Cheap Factories", "CF", CheapFactoriesDescription), // This is not a normal LRT!
         new TraitEntry("Extra Tech", "ExtraTech", ExtraCostStartLevel4Description) // This is not a normal LRT!
   };
}
