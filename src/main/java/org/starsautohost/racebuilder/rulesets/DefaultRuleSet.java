package org.starsautohost.racebuilder.rulesets;

import org.starsautohost.racebuilder.craigstars.Race;

public class DefaultRuleSet extends RuleSet {

	@Override
	public String getName(){
		return "Default Stars! ruleset.";
	}
	
	public int addToPoints(Race r, int points){
		return points;
	}

	@Override
	public String getDescription() {
		String s = "<html><font color=blue>The default Stars! ruleset</font></html>\n";
		s += "The habitat page still have some bugs when you\n";
		s += "adjust the habitat values left or right.";
		return s;
	}

}
