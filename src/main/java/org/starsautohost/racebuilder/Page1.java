package org.starsautohost.racebuilder;

import java.awt.Insets;
import java.awt.event.*;

import javax.swing.*;

import org.starsautohost.racebuilder.nova.Race;

public class Page1 extends Page implements ActionListener, KeyListener{

	JLabel label1 = new JLabel("Race Name: ");
	JLabel label2 = new JLabel("Plural Race Name: ");
	JLabel label3 = new JLabel("Password: ");
	JTextField raceName = new JTextField();
	JTextField pluralRaceName = new JTextField();
	JPasswordField password = new JPasswordField();
	JComboBox<String> leftoverPoints = new JComboBox<String>(new String[]{"Surface minerals","Mineral concentrations","Mines","Factories","Defences"});
	JButton image = new JButton("Img1");
	
	public Page1(RaceBuilder rb){
		super(rb);
		label1.setHorizontalAlignment(SwingConstants.RIGHT);
		label2.setHorizontalAlignment(SwingConstants.RIGHT);
		label3.setHorizontalAlignment(SwingConstants.RIGHT);
		add(label1);
		label1.setBounds(20, 20, 200, 25);
		add(raceName);
		raceName.setBounds(220, 20, 200, 25);
		add(label2);
		label2.setBounds(20, 50, 200, 25);
		add(pluralRaceName);
		pluralRaceName.setBounds(220,50,200,25);
		add(label3);
		label3.setBounds(20, 80, 200, 25);
		add(password);
		password.setBounds(220,80,200,25);
		JLabel l = new JLabel("Spend up to 50 leftover advantage points on:");
		add(l);
		l.setBounds(120,300,450,25);
		add(leftoverPoints);
		leftoverPoints.setBounds(120,330,350,25);
		add(image);
		image.setBounds(500,330,60,25);
		image.setMargin(new Insets(0,0,0,0));
		image.addActionListener(this);
		raceName.addKeyListener(this);
		pluralRaceName.addKeyListener(this);
		password.addKeyListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == image){
			
		}
	}

	@Override
	public void setRace(Race r) {
		raceName.setText(r.name);
		pluralRaceName.setText(r.pluralName);
		password.setText(r.password);
	}

	@Override
	public void keyTyped(KeyEvent e) {	
	}
	@Override
	public void keyPressed(KeyEvent e) {	
	}
	@Override
	public void keyReleased(KeyEvent e) {
		rb.getRace().name = this.raceName.getText();
		rb.getRace().pluralName = this.pluralRaceName.getText();
		rb.getRace().password = this.password.getText().toString();
	}
}
