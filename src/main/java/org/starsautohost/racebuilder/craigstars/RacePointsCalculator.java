package org.starsautohost.racebuilder.craigstars;

import java.util.HashMap;
import java.util.Map;

public class RacePointsCalculator {

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
    
    private static Race race;
    private static int TTCorrectionFactor;
    private static int numIterationsGrav;
    private static int numIterationsRad;
    private static int numIterationsTemp;
    private static Hab testPlanetHab = new Hab();
    
    /**
     * Get the Advantage points for a race
     * 
     * @return The advantage points for this race, negative means an invalid race.
     */
    public static int getAdvantagePoints(Race race) {
        RacePointsCalculator.race = race;
        
        // start off with some constant
        int points = Consts.raceStartingPoints;

        int habPoints = (int) (getHabRangePoints() / 2000);

        int growthRateFactor = (int) (race.getGrowthRate() * 100 + 0.5); // use raw growth rate, otherwise
                                                               // HEs pay for GR at 2x
        float grRate = growthRateFactor;

        // update the points based on growth rate
        if (growthRateFactor <= 5) {
            points += (6 - growthRateFactor) * 4200;
        } else if (growthRateFactor <= 13) {
            switch (growthRateFactor) {
            case 6:
                points += 3600;
                break;
            case 7:
                points += 2250;
                break;
            case 8:
                points += 600;
                break;
            case 9:
                points += 225;
                break;
            }
            growthRateFactor = growthRateFactor * 2 - 5;
        } else if (growthRateFactor < 20) {
            growthRateFactor = (growthRateFactor - 6) * 3;
        } else {
            growthRateFactor = 45;
        }

        points -= (int) (habPoints * growthRateFactor) / 24;

        // give points for off center habs
        int numImmunities = 0;
        for (int habType = 0; habType < 3; habType++) {
            if (race.isImmune(habType)) {
                numImmunities++;
            } else {
                points += Math.abs(race.getHabCenter(habType) - 50) * 4;
            }
        }

        // multiple immunities are penalized extra
        if (numImmunities > 1) {
            points -= 150;
        }

        // determine factory costs
        int operationPoints = race.getNumFactories();
        int productionPoints = race.getFactoryOutput();

        if (operationPoints > 10 || productionPoints > 10) {
            operationPoints -= 9;
            if (operationPoints < 1) {
                operationPoints = 1;
            }
            productionPoints -= 9;
            if (productionPoints < 1) {
                productionPoints = 1;
            }

            // HE penalty, 2 for all PRTs execpt 3 for HE
            int factoryProductionCost = 2;
            if (race.getPRT() == PRT.HE) {
                factoryProductionCost = 3;
            }

            productionPoints *= factoryProductionCost;

            // additional penalty for two- and three-immune
            if (numImmunities >= 2) {
                points -= ((productionPoints * operationPoints) * grRate) / 2;
            } else {
                points -= ((productionPoints * operationPoints) * grRate) / 9;
            }
        }

        // pop efficiency
        int popEfficiency = race.getColonistsPerResource() / 100;
        if (popEfficiency > 25)
            popEfficiency = 25;

        if (popEfficiency <= 7)
            points -= 2400;
        else if (popEfficiency == 8)
            points -= 1260;
        else if (popEfficiency == 9)
            points -= 600;
        else if (popEfficiency > 10)
            points += (popEfficiency - 10) * 120;

        // factory points (AR races have very simple points)
        if (race.getPRT() == PRT.AR) {
            points += 210;
        } else {
            productionPoints = 10 - race.getFactoryOutput();
            int costPoints = 10 - race.getFactoryCost();
            operationPoints = 10 - race.getNumFactories();
            int tmpPoints = 0;

            if (productionPoints > 0) {
                tmpPoints = productionPoints * 100;
            } else {
                tmpPoints = productionPoints * 121;
            }

            if (costPoints > 0) {
                tmpPoints += costPoints * costPoints * -60;
            } else {
                tmpPoints += costPoints * -55;
            }

            if (operationPoints > 0) {
                tmpPoints += operationPoints * 40;
            } else {
                tmpPoints += operationPoints * 35;
            }

            // limit low factory points
            int llfp = 700;
            if (tmpPoints > llfp) {
                tmpPoints = (tmpPoints - llfp) / 3 + llfp;
            }

            if (operationPoints <= -7) {
                if (operationPoints < -11) {
                    if (operationPoints < -14) {
                        tmpPoints -= 360;
                    } else {
                        tmpPoints += (operationPoints + 7) * 45;
                    }
                } else {
                    tmpPoints += (operationPoints + 6) * 30;
                }
            }

            if (productionPoints <= -3) {
                tmpPoints += (productionPoints + 2) * 60;
            }

            points += tmpPoints;

            if (race.isFactoriesCostLess()) {
                points -= 175;
            }

            // mines
            productionPoints = 10 - race.getMineOutput();
            costPoints = 3 - race.getMineCost();
            operationPoints = 10 - race.getNumMines();
            tmpPoints = 0;

            if (productionPoints > 0) {
                tmpPoints = productionPoints * 100;
            } else {
                tmpPoints = productionPoints * 169;
            }

            if (costPoints > 0) {
                tmpPoints -= 360;
            } else {
                tmpPoints += costPoints * (-65) + 80;
            }

            if (operationPoints > 0) {
                tmpPoints += operationPoints * 40;
            } else {
                tmpPoints += operationPoints * 35;
            }

            points += tmpPoints;
        }

        // prt and lrt point costs
        points += prtPointCost.get(race.getPRT());

        // too many lrts
        int badLRTs = 0;
        int goodLRTs = 0;

        // figure out how many bad vs good lrts we have.
        for (LRT lrt : race.getLRTs()) {
            if (lrtPointCost.get(lrt) >= 0) {
                badLRTs++;
            } else {
                goodLRTs++;
            }
            points += lrtPointCost.get(lrt);
        }

        if (goodLRTs + badLRTs > 4) {
            points -= (goodLRTs + badLRTs) * (goodLRTs + badLRTs - 4) * 10;
        }
        if (badLRTs - goodLRTs > 3) {
            points -= (badLRTs - goodLRTs - 3) * 60;
        }
        if (goodLRTs - badLRTs > 3) {
            points -= (goodLRTs - badLRTs - 3) * 40;
        }

        // No Advanced scanners is penalized in some races
        if (race.getLRTs().contains(LRT.NAS)) {
            if (race.getPRT() == PRT.PP) {
                points -= 280;
            } else if (race.getPRT() == PRT.SS) {
                points -= 200;
            } else if (race.getPRT() == PRT.JoaT) {
                points -= 40;
            }
        }

        // Techs
        //
        // Figure out the total number of Extra's, offset by the number of Less's 
        int techcosts = 0;
        for (int i = 0; i < 6; i++) {
            ResearchCostLevel rc = race.getResearchCost().getAtIndex(i);
            if (rc == ResearchCostLevel.Extra) {
                techcosts--;
            } else if (rc == ResearchCostLevel.Less) {
                techcosts++;
            }
        }

        // if we have more less's then extra's, penalize the race
        if (techcosts > 0) {
            points -= (techcosts * techcosts) * 130;
            if (techcosts >= 6) {
                points += 1430; // already paid 4680 so true cost is 3250
            } else if (techcosts == 5) {
                points += 520; // already paid 3250 so true cost is 2730
            }
        } else if (techcosts < 0) {
            // if we have more extra's, give the race a bonus that increases as
            // we have more extra's
            int[] scienceCost = new int[] { 150, 330, 540, 780, 1050, 1380 };
            points += scienceCost[(-techcosts) - 1];
            if (techcosts < -4 && race.getColonistsPerResource() < 1000) {
                points -= 190;
            }
        }

        if (race.isTechsStartHigh()) {
            points -= 180;
        }

        // ART races get penalized extra for have cheap energy because it gives them such a boost
        if (race.getPRT() == PRT.AR && race.getResearchCost().getEnergy() == ResearchCostLevel.Less) {
            points -= 100;
        }

        return points / 3;

    }
    
    /**
     * Compute the hab range advantage points for this race by generating test planets for a variety
     * of ranges and using the habitability of those planets
     * 
     * @return The advantage points for this race's habitability range
     */
    private static long getHabRangePoints() {
        boolean totalTerraforming;
        double temperatureSum, gravitySum;
        long radiationSum, planetDesirability;
        int terraformOffsetSum, tmpHab;
        int[] terraformOffset = new int[3];

        // setup the starting values for each hab type, and the widths
        // for those
        Hab testHabStart = new Hab();
        Hab testHabWidth = new Hab();
        //int[] testHabStart = new int[3];
        //int[] testHabWidth = new int[3];

        double points = 0.0;
        totalTerraforming = race.getLRTs().contains(LRT.TT);

        terraformOffset[0] = terraformOffset[1] = terraformOffset[2] = 0;
        
        // set the number of iterations for each hab type.  If we're immune it's just
        // 1 because all the planets in that range will be the same.  Otherwise we loop
        // over the entire hab range in 11 equal divisions (i.e. for Humanoids grav would be 15, 22, 29, etc. all the way to 85)
        if (race.isImmuneGrav()) {
            numIterationsGrav = 1;
        } else {
            numIterationsGrav = 11;
        }
        if (race.isImmuneTemp()) {
            numIterationsTemp = 1;
        } else {
            numIterationsTemp = 11;
        }
        if (race.isImmuneRad()) {
            numIterationsRad = 1;
        } else {
            numIterationsRad = 11;
        }

        // We go through 3 main iterations.  During each the habitability of the test planet
        // varies between the low and high of the hab range for each hab type.  So for a humanoid
        // it goes (15, 15, 15), (15, 15, 22), (15, 15, 29), etc.   Until it's (85, 85, 85)
        // During the various loops the TTCorrectionFactor changes to account for the race's ability
        // to terrform.
        for (int loopIndex = 0; loopIndex < 3; loopIndex++) {

            // each main loop gets a different TTCorrectionFactor
            if (loopIndex == 0)
                TTCorrectionFactor = 0;
            else if (loopIndex == 1)
                TTCorrectionFactor = totalTerraforming ? 8 : 5;
            else
                TTCorrectionFactor = totalTerraforming ? 17 : 15;
            

            // for each hab type, set up the starts and widths
            // for this outer loop
            for (int habType = 0; habType < 3; habType++) {
                // if we're immune, just make the hab values some middle value
                if (race.isImmune(habType)) {
                    testHabStart.setAtIndex(habType,  50);
                    testHabWidth.setAtIndex(habType, 11);

                } else {
                    // start at the minimum hab range
                    testHabStart.setAtIndex(habType, race.getHabLow().getAtIndex(habType) - TTCorrectionFactor);

                    // don't go below 0, that doesnt' make sense for a hab range
                    if (testHabStart.getAtIndex(habType) < 0) {
                        testHabStart.setAtIndex(habType, 0);
                    }

                    // get the high range for this hab type
                    tmpHab = race.getHabHigh().getAtIndex(habType) + TTCorrectionFactor;

                    // don't go over 100, that doesn't make sense
                    if (tmpHab > 100)
                        tmpHab = 100;

                    // figure out the width for this hab type's starting range
                    testHabWidth.setAtIndex(habType, tmpHab - testHabStart.getAtIndex(habType));
                }
            }

            // 3 nested for loops, one for each hab type.  The number of iterations is 11 for non immune habs, or 1 for immune habs
            // this starts iterations for the first hab (gravity)
            gravitySum = 0.0;
            for (int iterationGrav = 0; iterationGrav < numIterationsGrav; iterationGrav++) {
                tmpHab = getPlanetHabForHabIndex(iterationGrav, 0, loopIndex, numIterationsGrav, testHabStart.getGrav(), testHabWidth.getGrav(), terraformOffset);
                testPlanetHab.setGrav(tmpHab);

                // go through iterations for temperature
                temperatureSum = 0.0;
                for (int iterationTemp = 0; iterationTemp < numIterationsTemp; iterationTemp++) {
                    tmpHab = getPlanetHabForHabIndex(iterationTemp, 1, loopIndex, numIterationsTemp, testHabStart.getTemp(), testHabWidth.getTemp(), terraformOffset);
                    testPlanetHab.setTemp(tmpHab);

                    // go through iterations for radiation
                    radiationSum = 0;
                    for (int iterationRad = 0; iterationRad < numIterationsRad; iterationRad++) {
                        tmpHab = getPlanetHabForHabIndex(iterationRad, 2, loopIndex, numIterationsRad, testHabStart.getRad(), testHabWidth.getRad(), terraformOffset);
                        testPlanetHab.setRad(tmpHab);

                        planetDesirability = race.getPlanetHabitability(testPlanetHab);

                        terraformOffsetSum = terraformOffset[0] + terraformOffset[1] + terraformOffset[2];
                        if (terraformOffsetSum > TTCorrectionFactor) {
                            // bring the planet desirability down by the difference between the terraformOffsetSum and the TTCorrectionFactor
                            planetDesirability -= terraformOffsetSum - TTCorrectionFactor;
                            // make sure the planet isn't negative in desirability
                            if (planetDesirability < 0)
                                planetDesirability = 0;
                        }
                        planetDesirability *= planetDesirability;
                        
                        // modify the planetDesirability by some factor based on which main loop we're going through
                        switch (loopIndex) {
                        case 0:
                            planetDesirability *= 7;
                            break;
                        case 1:
                            planetDesirability *= 5;
                            break;
                        default:
                            planetDesirability *= 6;
                        }

                        radiationSum += planetDesirability;
                    }
                    
                    // The radiationSum is the sum of the planetDesirability for each iteration in numIterationsRad
                    // if we're immune to radiation it'll be the same very loop, so *= by 11
                    if (!race.isImmuneRad()) {
                        radiationSum = (radiationSum * testHabWidth.getRad()) / 100;
                    } else {
                        radiationSum *= 11;
                    }

                    temperatureSum += radiationSum;
                }

                // The tempSum is the sum of the radSums
                // if we're immune to radiation it'll be the same very loop, so *= by 11
                if (!race.isImmuneTemp()) {
                    temperatureSum = (temperatureSum * testHabWidth.getTemp()) / 100;
                } else {
                    temperatureSum *= 11;
                }

                gravitySum += temperatureSum;
            }
            if (!race.isImmuneGrav()) {
                gravitySum = (gravitySum * testHabWidth.getGrav()) / 100;
            } else {
                gravitySum *= 11;
            }

            points += gravitySum;
        }

        return (long) (points / 10.0 + 0.5);
    }

    /**
     * Get the planet hab value (grav, temp or rad) for an iteration of the loop
     * 
     * @param iterIndex The index of the iteration loop (1 through 11 usually)
     * @param habType The index of the loop
     * @param loopIndex The type of hab for the main outer loop
     * @param TTCorrectionFactor The Total Terraforming Correction Factor
     * @param numIterations The numIterations[HabType] for this loop
     * @param testHabStart The testHabStart for the habType
     * @param testHabWidth The testHabWidth for the habType
     * @param terraformOffset The terraformOffset array to set for the habIndex, this happens to account for the race terraforming in the future (I think)
     * @return The Hab value for the test planet, based on the habIndex, habType,
     *         TTCorrectionFactor, etc.
     */
    private static int getPlanetHabForHabIndex(int iterIndex, int habType, int loopIndex, int numIterations, int testHabStart, int testHabWidth,
                                        int[] terraformOffset) {
        int tmpHab = 0;
        
        // on the first iteration just use the testHabStart we already defined
        // if we're on a subsequent loop move the hab value along the habitable range of this race
        if (iterIndex == 0 || numIterations <= 1) {
            tmpHab = testHabStart;
        } else {
            tmpHab = (testHabWidth * iterIndex) / (numIterations - 1) + testHabStart;
        }

        // if we on a main loop other than the first one, do some
        // stuff with the terraforming correction factor
        if (loopIndex != 0 && !race.isImmune(habType)) {
            int offset = race.getHabCenter(habType) - tmpHab;
            if (Math.abs(offset) <= TTCorrectionFactor) {
                offset = 0;
            } else if (offset < 0) {
                offset += TTCorrectionFactor;
            } else {
                offset -= TTCorrectionFactor;
            }
            
            // we set this terraformOffset value for later use
            // when we do the summing
            terraformOffset[habType] = offset;
            tmpHab = race.getHabCenter(habType) - offset;
        }

        return tmpHab;
    }
    
    public static void main(String[] args){
    	Race r = Race.getHumanoid();
    	int i = RacePointsCalculator.getAdvantagePoints(r);
    	System.out.println("Points: "+i);
    }
}


