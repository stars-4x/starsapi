package org.starsautohost.racebuilder;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.starsautohost.racebuilder.craigstars.*;

public class Page6 extends Page implements ActionListener{

	JRadioButton en1 = new JRadioButton("Costs 75% extra");
	JRadioButton en2 = new JRadioButton("Costs standard amount");
	JRadioButton en3 = new JRadioButton("Costs 50% less");
	JRadioButton wp1 = new JRadioButton("Costs 75% extra");
	JRadioButton wp2 = new JRadioButton("Costs standard amount");
	JRadioButton wp3 = new JRadioButton("Costs 50% less");
	JRadioButton pr1 = new JRadioButton("Costs 75% extra");
	JRadioButton pr2 = new JRadioButton("Costs standard amount");
	JRadioButton pr3 = new JRadioButton("Costs 50% less");
	JRadioButton con1 = new JRadioButton("Costs 75% extra");
	JRadioButton con2 = new JRadioButton("Costs standard amount");
	JRadioButton con3 = new JRadioButton("Costs 50% less");
	JRadioButton el1 = new JRadioButton("Costs 75% extra");
	JRadioButton el2 = new JRadioButton("Costs standard amount");
	JRadioButton el3 = new JRadioButton("Costs 50% less");
	JRadioButton bio1 = new JRadioButton("Costs 75% extra");
	JRadioButton bio2 = new JRadioButton("Costs standard amount");
	JRadioButton bio3 = new JRadioButton("Costs 50% less");
	JRadioButton[] en = {en1,en2,en3};
	JRadioButton[] wp = {wp1,wp2,wp3};
	JRadioButton[] pr = {pr1,pr2,pr3};
	JRadioButton[] con = {con1,con2,con3};
	JRadioButton[] el = {el1,el2,el3};
	JRadioButton[] bio = {bio1,bio2,bio3};
	JRadioButton[][] techs = {en,wp,pr,con,el,bio};
	String[] techStrings = {"Energy Research","Weapons Research","Propulsion Research","Construction Research","Electronics Research","Biotechnology Research"};
	JCheckBox startAt3 = new JCheckBox("All 'Cost 75% extra' research fields start at Tech 3");
	private boolean settingRace = false;
	
	public Page6(RaceBuilder rb){
		super(rb);
		for (int t = 0; t < techs.length; t++){
			JPanel p = new JPanel();
			p.setBorder(BorderFactory.createTitledBorder(techStrings[t]));
			p.setLayout(new GridLayout(3, 1));
			JRadioButton[] bb = techs[t];
			ButtonGroup bg = new ButtonGroup();
			for (int tt = 0; tt < bb.length; tt++){
				bg.add(bb[tt]);
				p.add(bb[tt]);
				bb[tt].addActionListener(this);
			}
			add(p);
			int x = t >= 3 ? 300 : 20;
			int y = (t % 3) * 110; 
			p.setBounds(x, y, 250, 100);
		}
		add(startAt3);
		startAt3.setBounds(20,350,550,25);
		startAt3.addActionListener(this);
	}

	@Override
	public void setRace(Race r) {
		settingRace = true;
		for (int t = 0; t < techs.length; t++){
			JRadioButton[] tech = techs[t];
			ResearchCostLevel rcl = r.getResearchCost().getAtIndex(t);
			if (rcl == ResearchCostLevel.Extra) tech[0].setSelected(true);
			else if (rcl == ResearchCostLevel.Standard) tech[1].setSelected(true);
			else tech[2].setSelected(true);
		}
		
		startAt3.setSelected(r.isTechsStartHigh());
		settingRace = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (settingRace) return;
		Race r = rb.getRace();
		for (int t = 0; t < techs.length; t++){
			JRadioButton[] tech = techs[t];
			if (tech[0].isSelected()) r.getResearchCost().setAtIndex(t,ResearchCostLevel.Extra);
			else if (tech[1].isSelected()) r.getResearchCost().setAtIndex(t,ResearchCostLevel.Standard);
			else r.getResearchCost().setAtIndex(t,ResearchCostLevel.Less);
			
		}
		r.setTechsStartHigh(startAt3.isSelected());
		rb.raceChanged();
	}
}
