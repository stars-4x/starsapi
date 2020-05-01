package org.starsautohost.racebuilder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.starsautohost.racebuilder.craigstars.Hab;
import org.starsautohost.racebuilder.craigstars.Race;
import org.starsautohost.racebuilder.nova.Gravity;
import org.starsautohost.racebuilder.nova.Temperature;

public class Page4 extends Page implements ChangeListener{

	SliderPanel gravityPanel = new SliderPanel("Gravity",0,new Color(128,128,255));
	SliderPanel temperaturePanel = new SliderPanel("Temperature",1,new Color(255,128,128));
	SliderPanel radiationPanel = new SliderPanel("Radiation",2,new Color(128,255,128));
	JSpinner growthRate = new JSpinner(new SpinnerNumberModel(15,1,20,1));
	private boolean settingRace = false;
	
	public Page4(RaceBuilder rb){
		super(rb);
		add(gravityPanel);
		gravityPanel.setBounds(20,20,580,80);
		add(temperaturePanel);
		temperaturePanel.setBounds(20,100,580,80);
		add(radiationPanel);
		radiationPanel.setBounds(20,180,580,80);
		growthRate.setFont(growthRate.getFont().deriveFont((float)16));
		growthRate.addChangeListener(this);
		JPanel p = Page5.createPanel("Maximum colonist growth rate per year: ", growthRate, "");
		add(p);
		p.setBounds(120,300,450,30);
	}
	
	private class SliderPanel extends JPanel implements ActionListener{
		JButton left = new JButton("<");
		JButton right = new JButton(">");
		JButton extend = new JButton("<< >>");
		JButton shrink = new JButton(">> <<");
		int min = 0, max = 100;
		int start = 0, end = 100;
		Slider slider = new Slider();
		JLabel[] explanation = new JLabel[3];
		Color color;
		int mode;
		
		JCheckBox immune = new JCheckBox();
		private SliderPanel(String text, int mode, Color color){
			this.color = color;
			this.mode = mode;
			JLabel l = new JLabel(text+" "); 
			l.setFont(l.getFont().deriveFont((float)15));
			l.setHorizontalAlignment(SwingConstants.RIGHT);
			immune.setText("Immune to "+text);
			immune.setFont(immune.getFont().deriveFont((float)15));
			left.setMargin(new Insets(0,0,0,0));
			right.setMargin(new Insets(0,0,0,0));
			extend.setMargin(new Insets(0,0,0,0));
			shrink.setMargin(new Insets(0,0,0,0));
			setLayout(null);
			add(l);
			l.setBounds(0,20,100,25);
			add(left);
			left.setBounds(100,20,30,25);
			add(slider);
			slider.setBounds(130,20,350,25);
			add(right);
			right.setBounds(480,20,30,25);
			add(extend);
			extend.setBounds(100,50,75,25);
			add(immune);
			immune.setBounds(180,50,200,25);
			add(shrink);
			shrink.setBounds(435,50,75,25);
			for (int t = 0; t < explanation.length; t++){
				add(explanation[t] = new JLabel());
				explanation[t].setBounds(510,20+t*18,100,18);
				
			}
			left.addActionListener(this);
			right.addActionListener(this);
			extend.addActionListener(this);
			shrink.addActionListener(this);
			immune.addActionListener(this);
		}
		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == left){
				if (immune.isSelected()) return;
				if (start > min){
					start--;
					end--;
					repaint();
					updateExplanation();
					raceChanged();
				}
			}
			else if (e.getSource() == right){
				if (immune.isSelected()) return;
				if (end < max){
					start++;
					end++;
					repaint();
					updateExplanation();
					raceChanged();
				}
			}
			else if (e.getSource() == shrink){
				if (immune.isSelected()) return;
				if (end-start <= 20) return;
				start++;
				end--;
				repaint();
				updateExplanation();
				raceChanged();
			}
			else if (e.getSource() == extend){
				if (immune.isSelected()) return;
				if (end-start >= max-min) return;
				if (start == min) end+=2;
				else if (end == max) start-=2;
				else{
					start--;
					end++;
				}
				repaint();
				updateExplanation();
				raceChanged();
			}
			else if (e.getSource() == immune){
				raceChanged();
				updateExplanation();
			}
		}
		private void updateExplanation(){
			String s1 = "", s2 = "", s3 = "";
			if (immune.isSelected() == false){
				if (mode == 0) s1 = Gravity.formatWithUnit(start);
				if (mode == 1) s1 = Temperature.formatWithUnit(start);
				if (mode == 2) s1 = start+"mR";
				s2 = "   to";
				if (mode == 0) s3 = Gravity.formatWithUnit(end);
				if (mode == 1) s3 = Temperature.formatWithUnit(end);
				if (mode == 2) s3 = end+"mR";
			}
			explanation[0].setText(s1);
			explanation[1].setText(s2);
			explanation[2].setText(s3);
		}
		private class Slider extends JPanel{
			public Slider(){
				setBackground(Color.black);
			}
			public void paint(Graphics g){
				super.paint(g);
				if (immune.isSelected() == false){
					g.setColor(color);
					//System.out.println(min+" "+max);
					//System.out.println(start+" "+end+" "+getWidth());
					int w = max-min;
					int x = (start-min)*getWidth()/w;
					int y = (end-min)*getWidth()/w;
					//System.out.println(x+" "+y+" "+w);
					g.fillRect(x, 0, y-x, getHeight());
				}
			}
		}
		public void setValues(int minimumValue, int maximumValue) {
			this.start = minimumValue;
			this.end = maximumValue;
			repaint();
			updateExplanation();
		}
	}

	@Override
	public void setRace(Race r) {
		settingRace = true;
		System.out.println(r.getGrowthRate());
		Hab low = r.getHabLow();
		Hab high = r.getHabHigh();
		gravityPanel.setValues(low.getGrav(),high.getGrav());
		gravityPanel.immune.setSelected(r.isImmuneGrav());
		temperaturePanel.setValues(low.getTemp(),high.getTemp());
		temperaturePanel.immune.setSelected(r.isImmuneTemp());
		radiationPanel.setValues(low.getRad(), high.getRad());
		radiationPanel.immune.setSelected(r.isImmuneRad());
		growthRate.setValue((int)(r.getGrowthRate()*100));
		settingRace = false;
	}
	
	private void raceChanged(){
		if (settingRace) return;
		Hab low = rb.getRace().getHabLow();
		Hab high = rb.getRace().getHabHigh();
		low.setGrav(gravityPanel.start);
		high.setGrav(gravityPanel.end);
		rb.getRace().setImmuneGrav(gravityPanel.immune.isSelected());
		low.setTemp(temperaturePanel.start);
		high.setTemp(temperaturePanel.end);
		rb.getRace().setImmuneTemp(temperaturePanel.immune.isSelected());
		low.setRad(radiationPanel.start);
		high.setRad(radiationPanel.end);
		rb.getRace().setImmuneRad(radiationPanel.immune.isSelected());
		rb.getRace().setGrowthRate((float)((int)growthRate.getValue()/100.0));
		rb.raceChanged();
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		raceChanged();
	}
}
