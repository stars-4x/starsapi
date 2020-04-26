package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class Global{

    // Colonists
    public static final int     ColonistsPerKiloton                     = 100;
    public static final double  LowStartingPopulationFactor             = 0.7;
    public static final double  BaseCrowdingFactor                      = 16.0 / 9.0; // Taken from the Stars technical faq.
    public static final int     StartingColonists                       = 25000;
    public static final int     StartingColonistsAcceleratedBBS         = 100000;
    public static final int     NominalMaximumPlanetaryPopulation       = 1000000; // use Race.MaxPopulation to get the maximum for a particular race.
    public static final double  PopulationFactorHyperExpansion          = 0.5;
    public static final double  GrowthFactorHyperExpansion              = 2;
    public static final double  PopulationFactorJackOfAllTrades         = 1.2;
    public static final double  PopulationFactorOnlyBasicRemoteMining   = 1.1;

    // Combat
    public static final int MaxWeaponRange  = 7; // Doom/Armegeddon on station.
    public static final int MaxDefenses     = 100;
    
    // Environment
    public static final double GravityMinimum       = 0; // FIXME (priority 3) - Stars! gravity range is 0.2 - 6.0 with 1.0 in the middle! Will need to revise all current race builds once changed.
    public static final double GravityMaximum       = 8;
    public static final double RadiationMinimum     = 0;
    public static final double RadiationMaximum     = 100;
    public static final double TemperatureMinimum   = -200;
    public static final double TemperatureMaximum   = 200;

    // Production constants
    public static final int ColonistsPerOperableFactoryUnit     = 10000;
    public static final int FactoriesPerFactoryProductionUnit   = 10;
    public static final int ColonistsPerOperableMiningUnit      = 10000;
    public static final int MinesPerMineProductionUnit          = 10;

    public static final int DefenseIroniumCost = 5;
    public static final int DefenseBoraniumCost = 5;
    public static final int DefenseGermaniumCost = 5;
    public static final int DefenseEnergyCost = 15;
     
    // Research constants
    public static final int DefaultResearchPercentage = 10;

    // Format
    public static final int ShipIconNumberingLength = 4;
     
    // Turn data
    public static final int StartingYear = 2100;
    public static final int DiscardFleetReportAge = 1;
    
    // Limits
    public static final int MaxFleetAmount              = 512;
    public static final int MaxDesignsAmount            = 16;
    public static final int MaxStarbaseDesignsAmount    = 10;
    
    // Defaults
    public static final int Nobody = 0x00000000; // As an empire Id cannot be 0, it is used for no owner.
    public static final int Everyone = Nobody;
    public static final int None = Nobody;
    public static final int Unset = -10000;

    // System
    public static final double TotalFileWaitTime = 8.0; // (s) Maximum time to wait for a file to become available.
    public static final int FileWaitRetryTime = 100; // (ms) Time to wait before trying again to access the file.

}

