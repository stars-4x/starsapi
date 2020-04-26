package org.starsautohost.racebuilder;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import org.starsautohost.racebuilder.nova.Race;
import org.starsautohost.racebuilder.nova.TechLevel.ResearchField;

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
		int i = 0;
		for (ResearchField rf : ResearchField.values()){
			int val = r.getResearchCost(rf);
			JRadioButton[] b = techs[i];
			if (val == 175 || val == 150) b[0].setSelected(true);
			else if (val == 100) b[1].setSelected(true);
			else b[2].setSelected(true);
			i++;
		}
		startAt3.setSelected(r.hasTrait("ExtraTech"));
		settingRace = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (settingRace) return;
		Race r = rb.getRace();
		int i = 0;
		for (ResearchField rf : ResearchField.values()){
			JRadioButton[] b = techs[i];
			int val = 100;
			if (b[0].isSelected()) val = 175;
			else if (b[2].isSelected()) val = 50;
			r.researchCosts[i] = val;
			i++;
		}
		if (startAt3.isSelected()) r.traits.add("ExtraTech");
		rb.raceChanged();
	}
}
