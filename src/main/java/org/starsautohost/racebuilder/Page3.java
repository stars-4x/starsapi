package org.starsautohost.racebuilder;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import org.starsautohost.racebuilder.nova.*;

public class Page3 extends Page implements ActionListener{

	JLabel headerLabel = new JLabel("Lesser Racial Traits");
	JCheckBox lt1 = new JCheckBox("Improved Fuel Efficency");
	JCheckBox lt2 = new JCheckBox("Total Terraforming");
	JCheckBox lt3 = new JCheckBox("Advanced Remote Mining");
	JCheckBox lt4 = new JCheckBox("Improved Starbases");
	JCheckBox lt5 = new JCheckBox("Generalized Research");
	JCheckBox lt6 = new JCheckBox("Ultimate Recycling");
	JCheckBox lt7 = new JCheckBox("Mineral Alchemy");
	JCheckBox lt8 = new JCheckBox("No Ram Scoop Engines");
	JCheckBox lt9 = new JCheckBox("Cheap Engines");
	JCheckBox lt10 = new JCheckBox("Only Basic Remote Mining");
	JCheckBox lt11 = new JCheckBox("No Advanced Scanners");
	JCheckBox lt12 = new JCheckBox("Low Starting Population");
	JCheckBox lt13 = new JCheckBox("Bleeding Edge Technology");
	JCheckBox lt14 = new JCheckBox("Regenerating Shields");
	JCheckBox[] boxes = {lt1,lt2,lt3,lt4,lt5,lt6,lt7,lt8,lt9,lt10,lt11,lt12,lt13,lt14};
	private boolean settingRace = false;
	
	public Page3(RaceBuilder rb){
		super(rb);
		add(headerLabel);
		headerLabel.setBounds(200,30,200,20);
		for (int t = 0; t < boxes.length; t++){
			add(boxes[t]);
			int x = t >=7 ? 300 : 20;
			int y = (t % 7) * 30 + 30+30;
			boxes[t].setBounds(x, y, 280, 25);
			boxes[t].addActionListener(this);
		}
	}

	@Override
	public void setRace(Race r) {
		settingRace = true;
		for (int t = 0; t < boxes.length; t++) boxes[t].setSelected(false);
		for (TraitEntry t : r.traits.getValues()){
			int index = AllTraits.getSecondaryIndex(t.code);
			boxes[index].setSelected(true);
		}
		settingRace  = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (settingRace) return;
		for (int t = 0; t < boxes.length; t++){
			String key = AllTraits.traitKeys[t+10];
			if (boxes[t].isSelected()){
				rb.getRace().traits.add(key);
			}
			else rb.getRace().traits.remove(key);
		}
		//System.out.println(rb.getRace().traits.getValues().toString());
		rb.raceChanged();
	}
}
