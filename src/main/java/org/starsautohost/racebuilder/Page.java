package org.starsautohost.racebuilder;

import javax.swing.JPanel;

import org.starsautohost.racebuilder.craigstars.Race;

public abstract class Page extends JPanel{

	protected RaceBuilder rb;
	
	public Page(RaceBuilder rb){
		setLayout(null);
		this.rb = rb;
	}
	
	public abstract void setRace(Race r);
	public void update(){
		rb.raceChanged();
	}
}
