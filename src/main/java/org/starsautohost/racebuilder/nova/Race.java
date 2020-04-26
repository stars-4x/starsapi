package org.starsautohost.racebuilder.nova;

/*
 * Modified from Stars Nova project
 */
public class Race {
    public EnvironmentTolerance GravityTolerance        = new GravityTolerance();
    public EnvironmentTolerance RadiationTolerance      = new RadiationTolerance();
    public EnvironmentTolerance TemperatureTolerance    = new TemperatureTolerance();

    //public TechLevel researchCosts = new TechLevel(0);
    public int[] researchCosts = {100,100,100,100,100,100};
    public int getResearchCost(TechLevel.ResearchField field){
    	return researchCosts[field.getValue()];
    }
    
    
    public RacialTraits traits = new RacialTraits(); // Collection of all the race's traits, including the primary.

    public String pluralName;
    public String name;
    public String password;
    //public RaceIcon Icon = new RaceIcon();

    // These parameters affect the production rate of each star (used in the
    // Star class Update method).
    public int factoryBuildCost = 10;        // defined in the Race Designer as the amount of Resourcesrequired to build one factory
    public int colonistsPerResource = 1000;
    public int factoryProduction = 10;    // defined in the Race Designer as the amount of resources produced by 10 factories
    public int operableFactories = 10;

    public int mineBuildCost = 5;
    public int mineProductionRate = 10;   // defined in the Race Designer as the amount of minerals (kT) mined by every 10 mines
    public int operableMines = 10;

    public String LeftoverPointTarget;

    // Growth goes from 3 to 20 and is not normalized here.
    public double GrowthRate = 15;

    // required for searializable class
    public Race() 
    { 
    }

    /// <summary>
    /// Constructor for Race. 
    /// Reads all the race data in from an xml formatted save file.
    /// </summary>
    /// <param name="fileName">A nova save file containing a race.</param>
    /*
    public Race(string fileName)
    {
        XmlDocument xmldoc = new XmlDocument();
        boolean waitForFile = false;
        double waitTime = 0; // seconds
        do
        {
            try
            {
                using (FileStream fileStream = new FileStream(fileName, FileMode.Open, FileAccess.Read))
                {
                    xmldoc.Load(fileName);
                    XmlNode xmlnode = xmldoc.DocumentElement;
                    LoadRaceFromXml(xmlnode);
                }
                waitForFile = false;
            }
            catch (System.IO.IOException)
            {
                // IOException. Is the file locked? Try waiting.
                if (waitTime < Global.TotalFileWaitTime)
                {
                    waitForFile = true;
                    System.Threading.Thread.Sleep(Global.FileWaitRetryTime);
                    waitTime += 0.1;
                }
                else
                {
                    // Give up, maybe something else is wrong?
                    throw;
                }
            }
        } 
        while (waitForFile);
    }
    */
    
    /// <summary>
    /// Calculate this race's Habitability for a given star.
    /// </summary>
    /// <param name="star">The star for which the Habitability is being determined.</param>
    /// <returns>The normalized habitability of the star (-1 to +1).</returns>
    /// <remarks>
    /// This algorithm is taken from the Stars! Technical FAQ:
    /// http://www.starsfaq.com/advfaq/contents.htm
    ///
    /// Return the hab value of this star for the specified race (in the range
    /// -1 to +1 where 1 = 100%). Note that the star environment values are
    /// percentages of the total range.
    ///
    /// The full equation (from the Stars! Technical FAQ) is: 
    ///
    /// Hab% = SQRT[(1-g)^2+(1-t)^2+(1-r)^2]*(1-x)*(1-y)*(1-z)/SQRT[3] 
    ///
    /// Where g, t,and r (stand for gravity, temperature, and radiation)are given
    /// by Clicks_from_center/Total_clicks_from_center_to_edge and where x,y, and z
    /// are:
    ///
    /// x=g-1/2 for g>1/2
    /// x=0 for g less than 1/2 
    /// y=t-1/2 for t>1/2
    /// y=0 for t less than 1/2 
    /// z=r-1/2 for r>1/2
    /// z=0 for r less than 1/2.
    /// </remarks>
    public double habValue(Star star)
    {
        double r = normalizeHabitalityDistance(RadiationTolerance, star.Radiation);
        double g = normalizeHabitalityDistance(GravityTolerance, star.Gravity);
        double t = normalizeHabitalityDistance(TemperatureTolerance, star.Temperature);

        if (r > 1 || g > 1 || t > 1)
        {
            // currently not habitable
            int result = 0;
            int maxMalus = getMaxMalus();
            if (r > 1)
            {
                result -= getMalusForEnvironment(RadiationTolerance, star.Radiation, maxMalus);
            }
            if (g > 1)
            {
                result -= getMalusForEnvironment(GravityTolerance, star.Gravity, maxMalus);
            }
            if (t > 1)
            {
                result -= getMalusForEnvironment(TemperatureTolerance, star.Temperature, maxMalus);
            }
            return result / 100.0;
        }

        double x = 0;
        double y = 0;
        double z = 0;

        if (g > 0.5)
        {
            x = g - 0.5;
        }
        if (t > 0.5)
        {
            y = t - 0.5;
        }
        if (r > 0.5)
        {
            z = r - 0.5;
        }

        double h = Math.sqrt(
                        ((1 - g) * (1 - g)) + ((1 - t) * (1 - t)) + ((1 - r) * (1 - r))) * (1 - x) * (1 - y) * (1 - z)
                             / Math.sqrt(3.0);
        return h;
    }

    /// <summary>
    /// Calculate this race's Habitability for a given star report.
    /// </summary>
    /// <param name="report">The star report for which the Habitability is being determined.</param>
    /// <returns>The normalized habitability of the star (-1 to +1).</returns>
    /*
    public double habitalValue(StarIntel report)
    {
        Star star = new Star();
        star.Gravity = report.Gravity;
        star.Radiation = report.Radiation;
        star.Temperature = report.Temperature;
        
        return habValue(star);
    }
    */

    public int getAdvantagePoints() throws Exception
    {
        RaceAdvantagePointCalculator calculator = new RaceAdvantagePointCalculator();
        return calculator.calculateAdvantagePoints(this);
    }

    public int getLeftoverAdvantagePoints() throws Exception
    {
        int advantagePoints = getAdvantagePoints();
        advantagePoints = Math.max(0, advantagePoints); // return Advantage Points only if >= 0
        advantagePoints = Math.min(50, advantagePoints); // return not more than 50
        return advantagePoints;
    }

    private int getMaxMalus()
    {
        int maxMalus = 15;
        if (hasTrait("TT"))
        {
            maxMalus = 30;
        }
        return maxMalus;
    }

    private int getMalusForEnvironment(EnvironmentTolerance tolerance, int starValue, int maxMalus)
    {
        if (starValue > tolerance.getMaximumValue())
        {
            return Math.min(maxMalus, starValue - tolerance.getMaximumValue());
        }
        else if (starValue < tolerance.getMinimumValue())
        {
            return Math.min(maxMalus, tolerance.getMinimumValue() - starValue);
        }
        else
        {
            return 0;
        }
    }
    
    /// <summary>
    /// Clicks_from_center / Total_clicks_from_center_to_edge .
    /// </summary>
    /// <param name="tol"></param>
    /// <param name="starValue"></param>
    /// <returns></returns>
    private double normalizeHabitalityDistance(EnvironmentTolerance tol, int starValue)
    {
        if (tol.isImmune())
        {
            return 0.0;
        }

        int minv = tol.getMinimumValue();
        int maxv = tol.getMaximumValue();
        int span = Math.abs(maxv - minv);
        double totalClicksFromCenterToEdge = span / 2;
        double centre = minv + totalClicksFromCenterToEdge;
        double clicksFromCenter = Math.abs(centre - starValue);
        return clicksFromCenter / totalClicksFromCenterToEdge;
    }
    
    /// <summary>
    /// Calculate the number of resources this race requires to construct a factory.
    /// </summary>
    /// <returns>The number of resources this race requires to construct a factory.</returns>
    public Resources getFactoryResources()
    {
        int factoryBuildCostGerm = hasTrait("CF") ? 3 : 4;
        return new Resources(0, 0, factoryBuildCostGerm, factoryBuildCost);
    }

    /// <summary>
    /// Calculate the number of resources this race requires to construct a mine.
    /// </summary>
    public Resources getMineResources()
    {
        return new Resources(0, 0, 0, mineBuildCost);
    }

    /// <summary>
    /// Determine if this race has a given trait.
    /// </summary>
    /// <param name="trait">A string representing a primary or secondary trait. 
    /// See AllTraits.TraitKeys for examples.</param>
    /// <returns>true if this race has the given trait.</returns>
    public boolean hasTrait(String trait)
    {
        if (trait.equals(traits.getPrimary().code))
        {
            return true;
        }

        if (traits == null)
        {
            return false;
        }
        return this.traits.contains(trait);
    }

    /// <summary>
    /// The maximum planetary population for this race.
    /// </summary>
    public int getMaxPopulation(){
        int maxPop = Global.NominalMaximumPlanetaryPopulation;
        if (hasTrait("HE"))
        {
            maxPop = (int)(maxPop * Global.PopulationFactorHyperExpansion);
        }
        if (hasTrait("JOAT"))
        { 
            maxPop = (int)(maxPop * Global.PopulationFactorJackOfAllTrades);
        }
        if (hasTrait("OBRM")) 
        {
            maxPop = (int)(maxPop * Global.PopulationFactorOnlyBasicRemoteMining);
        }
        return maxPop;
    }

    /// <summary>
    /// Get the starting population for this race.
    /// </summary>
    /// <returns>The starting population.</returns>
    /// <remarks>
    /// TODO (priority 4) - Implement starting populations for races with two starting planets.
    /// </remarks>
    /*
    public int GetStartingPopulation()
    {
        int population = Global.StartingColonists;
        
        if (GameSettings.Data.AcceleratedStart)
        {
            population = Global.StartingColonistsAcceleratedBBS;
        }

        if (hasTrait("LSP"))
        {
            population = (int)(population * Global.LowStartingPopulationFactor);
        }

        return population;
    }
    */

    // Quick and dirty way to clone a race but has the big advantage
    // of picking up XML changes automagically
    /*
    public Race Clone()
    {
        XmlDocument doc = new XmlDocument();
        XmlElement ele = ToXml(doc);
        Race ret = new Race();
        ret.LoadRaceFromXml(ele);
        return ret;
    }
    */

    /// <summary>
    /// Save: Serialize this Race to an <see cref="XmlElement"/>.
    /// </summary>
    /// <param name="xmldoc">The parent <see cref="XmlDocument"/>.</param>
    /// <returns>An <see cref="XmlElement"/> representation of the Race.</returns>
    /*
    public XmlElement ToXml(XmlDocument xmldoc)
    {
        XmlElement xmlelRace = xmldoc.CreateElement("Race");

        xmlelRace.AppendChild(GravityTolerance.ToXml(xmldoc, "GravityTolerance"));
        xmlelRace.AppendChild(RadiationTolerance.ToXml(xmldoc, "RadiationTolerance"));
        xmlelRace.AppendChild(TemperatureTolerance.ToXml(xmldoc, "TemperatureTolerance"));
        // Tech
        xmlelRace.AppendChild(ResearchCosts.ToXml(xmldoc));

        // Type; // Primary Racial Trait.
        Global.SaveData(xmldoc, xmlelRace, "PRT", Traits.Primary.Code);
        // Traits
        foreach (TraitEntry trait in Traits)
        {
            if (AllTraits.Data.Primary.Contains(trait.Code))
            {
                continue; // Skip the PRT, just add LRTs here.
            }
            Global.SaveData(xmldoc, xmlelRace, "LRT", trait.Code);
        }

        // MineBuildCost
        Global.SaveData(xmldoc, xmlelRace, "MineBuildCost", MineBuildCost.ToString(System.Globalization.CultureInfo.InvariantCulture));

        // Plural Name
        if (!string.IsNullOrEmpty(PluralName))
        {
            Global.SaveData(xmldoc, xmlelRace, "PluralName", PluralName);
        }
        // Name
        if (!string.IsNullOrEmpty(Name))
        {
            Global.SaveData(xmldoc, xmlelRace, "Name", Name);
        }
        // Password 
        if (!string.IsNullOrEmpty(Password))
        {
            Global.SaveData(xmldoc, xmlelRace, "Password", Password);
        }
        // RaceIconName
        if (!string.IsNullOrEmpty(Icon.Source))
        {
            Global.SaveData(xmldoc, xmlelRace, "RaceIconName", Icon.Source);
        }
        // Factory Build Cost
        Global.SaveData(xmldoc, xmlelRace, "FactoryBuildCost", FactoryBuildCost.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // ColonistsPerResource
        Global.SaveData(xmldoc, xmlelRace, "ColonistsPerResource", ColonistsPerResource.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // FactoryProduction
        Global.SaveData(xmldoc, xmlelRace, "FactoryProduction", FactoryProduction.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // OperableFactories
        Global.SaveData(xmldoc, xmlelRace, "OperableFactories", OperableFactories.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // MineProductionRate
        Global.SaveData(xmldoc, xmlelRace, "MineProductionRate", MineProductionRate.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // OperableMines
        Global.SaveData(xmldoc, xmlelRace, "OperableMines", OperableMines.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // MaxPopulation
        Global.SaveData(xmldoc, xmlelRace, "MaxPopulation", MaxPopulation.ToString(System.Globalization.CultureInfo.InvariantCulture));
        // GrowthRate
        Global.SaveData(xmldoc, xmlelRace, "GrowthRate", GrowthRate.ToString(System.Globalization.CultureInfo.InvariantCulture));

        // LeftoverPointTarget
        if ("".Equals(LeftoverPointTarget) || LeftoverPointTarget == null)
        {
            LeftoverPointTarget = "Surface minerals";
        }
        Global.SaveData(xmldoc, xmlelRace, "LeftoverPoints", LeftoverPointTarget.ToString(System.Globalization.CultureInfo.InvariantCulture));

        return xmlelRace;
    }
    */

    /// <summary>
    /// Load a Race from an xml document.
    /// </summary>
    /// <param name="xmlnode">An XmlNode, see Race constructor for generation.</param>
    /*
    public void LoadRaceFromXml(XmlNode xmlnode)
    {
        while (xmlnode != null)
        {
            try
            {
                switch (xmlnode.Name.ToLower())
                {
                    case "root":
                        xmlnode = xmlnode.FirstChild;
                        continue;
                    case "race":
                        xmlnode = xmlnode.FirstChild;
                        continue;
                    case "gravitytolerance":
                        GravityTolerance.FromXml(xmlnode);
                        break;
                    case "radiationtolerance":
                        RadiationTolerance.FromXml(xmlnode);
                        break;
                    case "temperaturetolerance":
                        TemperatureTolerance.FromXml(xmlnode);
                        break;
                    case "tech":
                        ResearchCosts = new TechLevel(xmlnode);
                        break;

                    case "lrt":
                        Traits.Add(xmlnode.FirstChild.Value);
                        break;

                    case "minebuildcost":
                        MineBuildCost = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "prt":
                        Traits.SetPrimary(xmlnode.FirstChild.Value);
                        break;
                    case "pluralname":
                        if (xmlnode.FirstChild != null)
                        {
                            PluralName = xmlnode.FirstChild.Value;
                        }
                        break;
                    case "name":
                        if (xmlnode.FirstChild != null)
                        {
                            Name = xmlnode.FirstChild.Value;
                        }
                        break;
                    case "password":
                        if (xmlnode.FirstChild != null)
                        {
                            Password = xmlnode.FirstChild.Value;
                        }
                        break;

                    // TODO (priority 5) - load the RaceIcon
                    case "raceiconname":
                        if (xmlnode.FirstChild != null)
                        {
                            Icon.Source = xmlnode.FirstChild.Value;
                        }
                        break;

                    case "factorybuildcost":
                        FactoryBuildCost = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "colonistsperresource":
                        ColonistsPerResource = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "factoryproduction":
                        FactoryProduction = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "operablefactories":
                        OperableFactories = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "mineproductionrate":
                        MineProductionRate = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "operablemines":
                        OperableMines = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "growthrate":
                        GrowthRate = int.Parse(xmlnode.FirstChild.Value, System.Globalization.CultureInfo.InvariantCulture);
                        break;
                    case "leftoverpoints":
                        this.LeftoverPointTarget = xmlnode.FirstChild.Value;
                        break;

                    default: break;
                }
            }
            catch (Exception e)
            {
                Report.FatalError(e.Message + "\n Details: \n" + e);
            }

            xmlnode = xmlnode.NextSibling;
        }

        // if an old version of the race file is loaded and there is no leftover point target then select standard leftover point target.
        if ("".Equals(LeftoverPointTarget) || LeftoverPointTarget == null)
        {
            this.LeftoverPointTarget = "Surface minerals";
        }
    }
    */

    public int LowerHab(int habIndex)
    {
        switch (habIndex)
        {
            case 0:
                return GravityTolerance.getMinimumValue();
            case 1:
                return TemperatureTolerance.getMinimumValue();
            case 2:
                return RadiationTolerance.getMinimumValue();
        }
        return 0;
    }

    public int UpperHab(int habIndex)
    {
        switch (habIndex)
        {
            case 0:
                return GravityTolerance.getMaximumValue();
            case 1:
                return TemperatureTolerance.getMaximumValue();
            case 2:
                return RadiationTolerance.getMaximumValue();
        }
        return 0;
    }

    public int CenterHab(int habIndex)
    {
        switch (habIndex)
        {
            case 0:
                return GravityTolerance.getOptimumLevel();
            case 1:
                return TemperatureTolerance.getOptimumLevel();
            case 2:
                return RadiationTolerance.getOptimumLevel();
        }
        return 0;
    }

    public boolean IsImmune(int habIndex)
    {
        switch (habIndex)
        {
            case 0:
                return GravityTolerance.isImmune();
            case 1:
                return TemperatureTolerance.isImmune();
            case 2:
                return RadiationTolerance.isImmune();
        }
        return false;
    }
}
