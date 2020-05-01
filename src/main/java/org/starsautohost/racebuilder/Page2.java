package org.starsautohost.racebuilder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import org.starsautohost.racebuilder.craigstars.*;

public class Page2 extends Page implements ActionListener{

	JRadioButton r1 = new JRadioButton("Hyper-Expansion");
	JRadioButton r2 = new JRadioButton("Super Stealth");
	JRadioButton r3 = new JRadioButton("War Monger");
	JRadioButton r4 = new JRadioButton("Claim Adjuster");
	JRadioButton r5 = new JRadioButton("Inner-Strength");
	JRadioButton r6 = new JRadioButton("Space Demolition");
	JRadioButton r7 = new JRadioButton("Packet Physics");
	JRadioButton r8 = new JRadioButton("Interstellar Traveler");
	JRadioButton r9 = new JRadioButton("Alternate Reality");
	JRadioButton r10 = new JRadioButton("Jack of All Trades");
	JRadioButton[] buttons = {r1,r2,r3,r4,r5,r6,r7,r8,r9,r10};
	private boolean settingRace = false;
	
	public Page2(RaceBuilder rb){
		super(rb);
		JPanel p = new JPanel();
		p.setBorder(BorderFactory.createTitledBorder("Primary Racial Trait"));
		add(p);
		p.setLayout(null);
		p.setBounds(0,0,550,180);
		ButtonGroup bg = new ButtonGroup();
		for (int t = 0; t < buttons.length; t++){
			bg.add(buttons[t]);
			p.add(buttons[t]);
			int x = t >=5 ? 250 : 20;
			int y = (t % 5) * 30 + 30;
			buttons[t].setBounds(x, y, 200, 25);
			buttons[t].addActionListener(this);
		}
	}

	@Override
	public void setRace(Race r) {
		settingRace = true;
		int index = r.getPRT().getIndex();
		//int index = AllTraits.getIndex(r.traits.getPrimary().code);
		buttons[index].setSelected(true);
		settingRace = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try{
			if (settingRace) return;
			for (int t = 0; t < buttons.length; t++) {
				if (buttons[t].isSelected()){
					rb.getRace().setPRT(PRT.fromIndex(t));
				}
			}
			rb.raceChanged();
		}catch(Exception ex){
			ex.printStackTrace();
			rb.showError(ex);
		}
	}
	
}
