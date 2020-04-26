package org.starsautohost.racebuilder;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.starsautohost.racebuilder.nova.Race;

public class Page5 extends Page implements ActionListener, ChangeListener{

	JSpinner resources = new JSpinner(new SpinnerNumberModel(1000,700,2500,100));
	JSpinner factories1 = new JSpinner(new SpinnerNumberModel(10,5,15,1));
	JSpinner factories2 = new JSpinner(new SpinnerNumberModel(10,5,25,1));
	JSpinner factories3 = new JSpinner(new SpinnerNumberModel(10,5,25,1));
	JCheckBox germanium = new JCheckBox("Factories cost 1 kT less of Germanium to build");
	JSpinner mines1 = new JSpinner(new SpinnerNumberModel(10,5,25,1));
	JSpinner mines2 = new JSpinner(new SpinnerNumberModel(10,2,15,1));
	JSpinner mines3 = new JSpinner(new SpinnerNumberModel(10,5,25,1));
	JSpinner[] spinners = {resources,factories1,factories2,factories3,mines1,mines2,mines3};
	private boolean settingRace = false;
	
	public Page5(RaceBuilder rb){
		super(rb);
		JComponent[] components = new JComponent[8];
		int x = 5;
		int w = 600;
		int h = 30;
		JPanel p1 = createPanel("One resource is generated each year for every ",resources," colonists.");
		add(p1);
		p1.setBounds(x,20,w,h);
		JPanel p2 = createPanel("Every 10 factories produce ",factories1," resources each year.");
		add(p2);
		p2.setBounds(x,60,w,h);
		JPanel p3 = createPanel("Factories require ",factories2," resources to build.");
		add(p3);
		p3.setBounds(x,90,w,h);
		JPanel p4 = createPanel("Every 10,000 colonists may operate up to ",factories3," factories.");
		add(p4);
		p4.setBounds(x,120,w,h);
		add(germanium);
		germanium.setBounds(x,150,w,h);
		JPanel p5 = createPanel("Every 10 mines produce up to ",mines1," of each mineral every year.");
		add(p5);
		p5.setBounds(x,190,w,h);
		JPanel p6 = createPanel("Mines require ",mines2," resources to build.");
		add(p6);
		p6.setBounds(x,220,w,h);
		JPanel p7 = createPanel("Every 10,000 colonists may operate up to ",mines3," mines.");
		add(p7);
		p7.setBounds(x,250,w,h);
		for (int t = 0; t < spinners.length; t++){
			spinners[t].addChangeListener(this);
		}
		germanium.addActionListener(this);
	}

	protected static JPanel createPanel(String s1, JSpinner sp, String s2) {
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout(FlowLayout.LEFT));
		p.add(new JLabel(s1));
		p.add(sp);
		p.add(new JLabel(s2));
		return p;
	}

	@Override
	public void setRace(Race r) {
		settingRace = true;
		resources.setValue(r.colonistsPerResource);
		factories1.setValue(r.factoryProduction);
		factories2.setValue(r.factoryBuildCost);
		factories3.setValue(r.operableFactories);
		mines1.setValue(r.mineProductionRate);
		mines2.setValue(r.mineBuildCost);
		mines3.setValue(r.operableMines);
		germanium.setSelected(r.hasTrait("CF"));
		settingRace = false;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		if (settingRace) return;
		Race r = rb.getRace();
		r.colonistsPerResource = (Integer)resources.getValue();
		r.factoryProduction = (Integer)factories1.getValue();
		r.factoryBuildCost = (Integer)factories2.getValue();
		r.operableFactories = (Integer)factories3.getValue();
		r.mineProductionRate = (Integer)mines1.getValue();
		r.mineBuildCost = (Integer)mines2.getValue();
		r.operableMines = (Integer)mines3.getValue();
		if (germanium.isSelected()) r.traits.add("CF");
		else r.traits.remove("CF");
		rb.raceChanged();
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		stateChanged(null);
	}	
}
