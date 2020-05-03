package org.starsautohost.racebuilder.rulesets;

import org.starsautohost.racebuilder.craigstars.*;

public class TestRuleSet extends RuleSet{

	@Override
	public int addToPoints(Race r, int points) {
		if (r.getPRT() == PRT.CA) points += 50;
		else if (r.getPRT() == PRT.PP) points += 350;
		else if (r.getPRT() == PRT.AR) points += 450;
		else points += 300;
		if (r.getLRTs().contains(LRT.BET)) points += 80;
		if (r.getLRTs().contains(LRT.GR)) points += 20;
		if (r.getLRTs().contains(LRT.UR)) points += 20;
		if (r.getLRTs().contains(LRT.MA)) points += 20;
		return points;
	}

	@Override
	public String getName() {
		return "Test: Superraces";
	}

	@Override
	public String getDescription() {
		String s = "<html><font color=blue>Just a test ruleset to illustrate the point.</font></html>\n";
		s += "AR gets +450 points.\n";
		s += "PP gets +350 points.\n";
		s += "CA gets +50 points.\n";
		s += "All other races gets +300 points.\n";
		s += "\n";
		s += "Bleeding edge gives an additional 80 points.\n";
		s += "GR, UR and MA gives 20 points each.\n";
		return s;
	}

}
