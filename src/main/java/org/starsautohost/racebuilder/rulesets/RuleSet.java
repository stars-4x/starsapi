package org.starsautohost.racebuilder.rulesets;

import org.starsautohost.racebuilder.craigstars.Race;

public abstract class RuleSet {

	public abstract int addToPoints(Race r, int points);
	public abstract String getName();
	public abstract String getDescription();
	
	@Override
	public String toString(){
		return getName();
	}
	
}
