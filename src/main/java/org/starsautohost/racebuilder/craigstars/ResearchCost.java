package org.starsautohost.racebuilder.craigstars;

public class ResearchCost{ // extends AbstractCSObject {

    private ResearchCostLevel energy;
    private ResearchCostLevel weapons;
    private ResearchCostLevel propulsion;
    private ResearchCostLevel construction;
    private ResearchCostLevel electronics;
    private ResearchCostLevel biotechnology;

    public ResearchCost() {
        super();
        energy = ResearchCostLevel.Standard;
        weapons = ResearchCostLevel.Standard;
        propulsion = ResearchCostLevel.Standard;
        construction = ResearchCostLevel.Standard;
        electronics = ResearchCostLevel.Standard;
        biotechnology = ResearchCostLevel.Standard;
    }

    public ResearchCost(ResearchCostLevel energy, ResearchCostLevel weapons, ResearchCostLevel propulsion, ResearchCostLevel construction,
        ResearchCostLevel electronics, ResearchCostLevel biotechnology) {
        super();
        this.energy = energy;
        this.weapons = weapons;
        this.propulsion = propulsion;
        this.construction = construction;
        this.electronics = electronics;
        this.biotechnology = biotechnology;
    }
    
    public ResearchCostLevel getAtIndex(int index) {
        switch(index) {
        case 0:
            return energy;
        case 1:
            return weapons;
        case 2:
            return propulsion;
        case 3:
            return construction;
        case 4:
            return electronics;
        case 5:
            return biotechnology;
        }
        
        throw new IndexOutOfBoundsException("TechField index must be between 0 and 5, was: " + index);
    }

    public void setAtIndex(int index, ResearchCostLevel lvl) {
		switch(index) {
        case 0:
            energy = lvl;
            break;
        case 1:
            weapons = lvl;
            break;
        case 2:
            propulsion = lvl;
            break;
        case 3:
            construction = lvl;
            break;
        case 4:
            electronics = lvl;
            break;
        case 5:
            biotechnology = lvl;
            break;
        default:
			throw new IndexOutOfBoundsException("TechField index must be between 0 and 5, was: " + index);
		}
	}
    
    public ResearchCostLevel getEnergy() {
        return energy;
    }

    public void setEnergy(ResearchCostLevel energy) {
        this.energy = energy;
    }

    public ResearchCostLevel getWeapons() {
        return weapons;
    }

    public void setWeapons(ResearchCostLevel weapons) {
        this.weapons = weapons;
    }

    public ResearchCostLevel getPropulsion() {
        return propulsion;
    }

    public void setPropulsion(ResearchCostLevel propulsion) {
        this.propulsion = propulsion;
    }

    public ResearchCostLevel getConstruction() {
        return construction;
    }

    public void setConstruction(ResearchCostLevel construction) {
        this.construction = construction;
    }

    public ResearchCostLevel getElectronics() {
        return electronics;
    }

    public void setElectronics(ResearchCostLevel electronics) {
        this.electronics = electronics;
    }

    public ResearchCostLevel getBiotechnology() {
        return biotechnology;
    }

    public void setBiotechnology(ResearchCostLevel biotechnology) {
        this.biotechnology = biotechnology;
    }
}
