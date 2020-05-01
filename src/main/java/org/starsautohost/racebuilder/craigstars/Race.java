package org.starsautohost.racebuilder.craigstars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Race { //extends AbstractCSObject {

    /**
     * The 'cost' in points of each LRT. A good LRT costs negative, a bad LRT is positive
     */
    private static Map<LRT, Integer> lrtPointCost;
    private static Map<PRT, Integer> prtPointCost;

    static {
        lrtPointCost = new HashMap<LRT, Integer>();
        prtPointCost = new HashMap<PRT, Integer>();

        lrtPointCost.put(LRT.IFE, -235);
        lrtPointCost.put(LRT.TT, -25);
        lrtPointCost.put(LRT.ARM, -159);
        lrtPointCost.put(LRT.ISB, -201);
        lrtPointCost.put(LRT.GR, 40);
        lrtPointCost.put(LRT.UR, -240);
        lrtPointCost.put(LRT.MA, -155);
        lrtPointCost.put(LRT.NRSE, 160);
        lrtPointCost.put(LRT.CE, 240);
        lrtPointCost.put(LRT.OBRM, 255);
        lrtPointCost.put(LRT.NAS, 325);
        lrtPointCost.put(LRT.LSP, 180);
        lrtPointCost.put(LRT.BET, 70);
        lrtPointCost.put(LRT.RS, 30);

        prtPointCost.put(PRT.HE, -40);
        prtPointCost.put(PRT.SS, -95);
        prtPointCost.put(PRT.WM, -45);
        prtPointCost.put(PRT.CA, -10);
        prtPointCost.put(PRT.IS, 100);
        prtPointCost.put(PRT.SD, 150);
        prtPointCost.put(PRT.PP, -120);
        prtPointCost.put(PRT.IT, -180);
        prtPointCost.put(PRT.AR, -90);
        prtPointCost.put(PRT.JoaT, 66);
    }

    private String name;
    private String pluralName;
    private String password;
    private PRT prt;
    private Set<LRT> lrts = new HashSet<LRT>();
    private Hab habLow = new Hab();
    private Hab habHigh = new Hab();
    private float growthRate;
    private int colonistsPerResource;
    private int factoryOutput;
    private int factoryCost;
    private int numFactories;
    private boolean factoriesCostLess;
    private int mineOutput;
    private int mineCost;
    private int numMines;
    private boolean techsStartHigh;
    private SpendLeftoverPointsOn spendLeftoverPointsOn;
    private boolean immuneGrav;
    private boolean immuneTemp;
    private boolean immuneRad;
    private ResearchCost researchCost = new ResearchCost();

    // computed values
    private int[] habCenter;
    private int[] habWidth;

    public Race() {
        super();
        init();
    }

    @Override
    public String toString() {
        return String.format("<Race: %s (%s) PRT: %s, LRTs: %s, Hab(%s -> %s immune: (%s %s %s)), rc: %s>", name, pluralName, prt, lrts, habLow, habHigh,
                             immuneGrav, immuneTemp, immuneRad, researchCost);
    }

    /**
     * Initialize computed values, like habCenter and habWidth
     */
    private void init() {
        habCenter = new int[3];
        habWidth = new int[3];
        for (int i = 0; i < 3; i++) {
            habCenter[i] = (habHigh.getAtIndex(i) + habLow.getAtIndex(i)) / 2;
            habWidth[i] = (habHigh.getAtIndex(i) - habLow.getAtIndex(i)) / 2;
        }
    }

    /**
     * Get a copy of a humanoid Race
     * 
     * @return A Humanoid race
     */
    public static Race getHumanoid() {
        Race race = new Race();
        race.setName("Humanoid");
        race.setPluralName("Humanoids");

        race.prt = PRT.JoaT;
        race.habLow = new Hab(15, 15, 15);
        race.habHigh = new Hab(85, 85, 85);
        race.growthRate = .15f;
        race.colonistsPerResource = 1000;
        race.factoryOutput = 10;
        race.factoryCost = 10;
        race.numFactories = 10;
        race.factoriesCostLess = false;
        race.mineOutput = 10;
        race.mineCost = 5;
        race.numMines = 10;
        race.techsStartHigh = false;
        race.immuneGrav = false;
        race.immuneTemp = false;
        race.immuneRad = false;
        race.spendLeftoverPointsOn = SpendLeftoverPointsOn.SurfaceMinerals;
        race.researchCost = new ResearchCost(ResearchCostLevel.Standard, ResearchCostLevel.Standard, ResearchCostLevel.Standard, ResearchCostLevel.Standard,
                                             ResearchCostLevel.Standard, ResearchCostLevel.Standard);

        race.init();
        return race;
    }

    /**
     * @return Return the center point of this hab, i.e. for hab 25 to 75 the center is 50 hab 60 to
     *         100 the center is 80
     */
    public int getHabCenter(int index) {
        return habCenter[index];
    }

    /**
     * Return whether this race is immune to a specific hab, by index
     * 
     * @param index The index of the hab, 0 == gravity, 1 == temp, 2 == radiation
     * @return Whether this race is immune to the specific hab type.
     */
    public boolean isImmune(int index) {
        if (index == 0) {
            return immuneGrav;
        } else if (index == 1) {
            return immuneTemp;
        } else if (index == 2) {
            return immuneRad;
        }
        throw new IndexOutOfBoundsException("Habitabiilty index must be between 0 and 2, was: " + index);
    }

    /**
     * Get the habitability of this race for a given planet's hab value
     * 
     * @param planetHabData The Hab value for a planet.
     * @return The habiability of this race to that planet, with 100 being the best
     */
    public long getPlanetHabitability(Hab planetHabData) {
        long planetValuePoints = 0, redValue = 0, ideality = 10000;
        int habValue, habCenter, habUpper, habLower, fromIdeal, habRadius, poorPlanetMod, habRed, tmp;
        for (int habType = 0; habType < 3; habType++) {
            habValue = planetHabData.getAtIndex(habType);
            habCenter = this.habCenter[habType];
            habLower = this.habLow.getAtIndex(habType);
            habUpper = this.habHigh.getAtIndex(habType);

            if (isImmune(habType))
                planetValuePoints += 10000;
            else {
                if (habLower <= habValue && habUpper >= habValue) {
                    /* green planet */
                    fromIdeal = Math.abs(habValue - habCenter) * 100;
                    if (habCenter > habValue) {
                        habRadius = habCenter - habLower;
                        fromIdeal /= habRadius;
                        tmp = habCenter - habValue;
                    } else {
                        habRadius = habUpper - habCenter;
                        fromIdeal /= habRadius;
                        tmp = habValue - habCenter;
                    }
                    poorPlanetMod = ((tmp) * 2) - habRadius;
                    fromIdeal = 100 - fromIdeal;
                    planetValuePoints += fromIdeal * fromIdeal;
                    if (poorPlanetMod > 0) {
                        ideality *= habRadius * 2 - poorPlanetMod;
                        ideality /= habRadius * 2;
                    }
                } else {
                    /* red planet */
                    if (habLower <= habValue)
                        habRed = habValue - habUpper;
                    else
                        habRed = habLower - habValue;

                    if (habRed > 15)
                        habRed = 15;

                    redValue += habRed;
                }
            }
        }

        if (redValue != 0) {
            return -redValue;
        }

        planetValuePoints = (long) (Math.sqrt((double) planetValuePoints / 3) + 0.9);
        planetValuePoints = planetValuePoints * ideality / 10000;

        return planetValuePoints;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPluralName() {
        return pluralName;
    }

    public void setPluralName(String pluralName) {
        this.pluralName = pluralName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    public PRT getPRT() {
        return prt;
    }

    public void setPRT(PRT prt) {
        this.prt = prt;
    }

    public Set<LRT> getLRTs() {
        return lrts;
    }

    public void setLRTs(Set<LRT> lrts) {
        this.lrts = lrts;
    }

    public Hab getHabLow() {
        return habLow;
    }

    public void setHabLow(Hab habLow) {
        this.habLow = habLow;
        init();
    }

    public Hab getHabHigh() {
        return habHigh;
    }

    public void setHabHigh(Hab habHigh) {
        this.habHigh = habHigh;
        init();
    }

    public float getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(float growthRate) {
        this.growthRate = growthRate;
    }

    public int getColonistsPerResource() {
        return colonistsPerResource;
    }

    public void setColonistsPerResource(int colonistsPerResource) {
        this.colonistsPerResource = colonistsPerResource;
    }

    public int getFactoryOutput() {
        return factoryOutput;
    }

    public void setFactoryOutput(int factoryOutput) {
        this.factoryOutput = factoryOutput;
    }

    public int getFactoryCost() {
        return factoryCost;
    }

    public void setFactoryCost(int factoryCost) {
        this.factoryCost = factoryCost;
    }

    public int getNumFactories() {
        return numFactories;
    }

    public void setNumFactories(int numFactories) {
        this.numFactories = numFactories;
    }

    public boolean isFactoriesCostLess() {
        return factoriesCostLess;
    }

    public void setFactoriesCostLess(boolean factoriesCostLess) {
        this.factoriesCostLess = factoriesCostLess;
    }

    public int getMineOutput() {
        return mineOutput;
    }

    public void setMineOutput(int mineOutput) {
        this.mineOutput = mineOutput;
    }

    public int getMineCost() {
        return mineCost;
    }

    public void setMineCost(int mineCost) {
        this.mineCost = mineCost;
    }

    public int getNumMines() {
        return numMines;
    }

    public void setNumMines(int numMines) {
        this.numMines = numMines;
    }

    public boolean isTechsStartHigh() {
        return techsStartHigh;
    }

    public void setTechsStartHigh(boolean techsStartHigh) {
        this.techsStartHigh = techsStartHigh;
    }

    public SpendLeftoverPointsOn getSpendLeftoverPointsOn() {
        return spendLeftoverPointsOn;
    }

    public void setSpendLeftoverPointsOn(SpendLeftoverPointsOn spendLeftoverPointsOn) {
        this.spendLeftoverPointsOn = spendLeftoverPointsOn;
    }

    public boolean isImmuneGrav() {
        return immuneGrav;
    }

    public void setImmuneGrav(boolean immuneGrav) {
        this.immuneGrav = immuneGrav;
    }

    public boolean isImmuneTemp() {
        return immuneTemp;
    }

    public void setImmuneTemp(boolean immuneTemp) {
        this.immuneTemp = immuneTemp;
    }

    public boolean isImmuneRad() {
        return immuneRad;
    }

    public void setImmuneRad(boolean immuneRad) {
        this.immuneRad = immuneRad;
    }

    public ResearchCost getResearchCost() {
        return researchCost;
    }

    public void setResearchCost(ResearchCost researchCost) {
        this.researchCost = researchCost;
    }

	public int getAvailablePoints() {
		return RacePointsCalculator.getAdvantagePoints(this);
	}

}
