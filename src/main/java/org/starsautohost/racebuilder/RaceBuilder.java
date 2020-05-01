package org.starsautohost.racebuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import org.starsautohost.racebuilder.craigstars.Race;
import org.starsautohost.racebuilder.craigstars.RacePointsCalculator;

public class RaceBuilder extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private JButton back = new JButton("< Back");
	private JButton next = new JButton("Next >");
	private JButton finished = new JButton("Finish");
	private int page = 1;
	private JPanel center = new JPanel();
	private JLabel pointsLeftLabel = new JLabel("0");
	private Page[] pages = new Page[6];
	private Race r;
	
	public static void main(String[] args){
		Font f = new JLabel().getFont().deriveFont((float)18);
		UIManager.put("Button.font", f);
		UIManager.put("RadioButton.font", f);
		UIManager.put("CheckBox.font", f);
		UIManager.put("ComboBox.font", f);
		UIManager.put("TextField.font", f);
		UIManager.put("PasswordField.font", f);
		UIManager.put("Label.font", f);
		UIManager.put("TitledBorder.font", f);
		//UIManager.put("Spinner.font", f);
		Race r = Race.getHumanoid();
		//Some adjustment tests outside of gui:
		//System.out.println(r.getHabLow().getGrav()+" "+r.getHabCenter(0)+" "+r.getHabHigh().getGrav()+" "+r.getAvailablePoints());
		//r.getHabLow().setGrav(14);
		//r.getHabHigh().setGrav(84);
		//System.out.println(r.getHabLow().getGrav()+" "+r.getHabCenter(0)+" "+r.getHabHigh().getGrav()+" "+r.getAvailablePoints());
		
		new RaceBuilder(r);
	}
	
	public RaceBuilder(Race r){
		super();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		construct(r);
	}
	private void construct(Race r){
		this.r = r;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pages[0] = new Page1(this);
		pages[1] = new Page2(this);
		pages[2] = new Page3(this);
		pages[3] = new Page4(this);
		pages[4] = new Page5(this);
		pages[5] = new Page6(this);
		for (Page p : pages){
			p.setRace(r);
		}
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel north = new JPanel();
		north.setLayout(new BorderLayout());
		north.add(new JLabel(),BorderLayout.NORTH);
		JPanel northEast = new JPanel();
		northEast.setLayout(new BorderLayout());
		northEast.add(new JLabel("<html>Advantage<br>Points Left</html>"),BorderLayout.CENTER);
		northEast.add(pointsLeftLabel,BorderLayout.EAST);
		pointsLeftLabel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
		pointsLeftLabel.setFont(pointsLeftLabel.getFont().deriveFont((float)36));
		northEast.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		north.add(northEast,BorderLayout.EAST);
		JPanel south = new JPanel(); south.setLayout(new FlowLayout());
		south.add(back); south.add(next); south.add(finished);
		cp.add(north,BorderLayout.NORTH);
		cp.add(center,BorderLayout.CENTER);
		cp.add(south,BorderLayout.SOUTH);
		center.setLayout(new BorderLayout());
		center.setPreferredSize(new Dimension(600,400));
		setPage(1);
		back.addActionListener(this);
		next.addActionListener(this);
		finished.addActionListener(this);
		raceChanged();
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-getWidth())/2, (screen.height-getHeight())/2);
		setVisible(true);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == back){
			setPage(page-1);
		}
		else if (e.getSource() == next){
			setPage(page+1);
		}
	}
	
	public void setPage(int page){
		if (page <= 1) page = 1;
		if (page >= 6) page = 6;
		this.page = page;
		center.removeAll();
		center.add(pages[page-1],BorderLayout.CENTER);
		setTitle("Race Wizard - Page "+page+" of 6");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				center.updateUI();
			}
		});
	}
	
	public Race getRace(){
		return r;
	}

	public void raceChanged() {
		try{
			int points = RacePointsCalculator.getAdvantagePoints(r);
			pointsLeftLabel.setText(""+points);
		}catch(Exception ex){
			ex.printStackTrace();
			showError(ex);
		}
	}

	public void showError(Exception ex) {
		JOptionPane.showMessageDialog(this, ex.toString());
	}
}
