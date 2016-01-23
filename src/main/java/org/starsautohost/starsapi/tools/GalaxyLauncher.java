package org.starsautohost.starsapi.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class GalaxyLauncher extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private JTabbedPane tabs = new JTabbedPane();
	private JButton launch = new JButton("Launch tool");
	private GalaxyViewer.Settings gvs;
	private GalaxyAnimator.Settings gas;
	
	public static void main(String[] args){
		try{
			new GalaxyLauncher();
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(null, ex.toString());
		}
	}
	
	public GalaxyLauncher() throws Exception{
		setTitle("Galaxy Tools");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		gvs = new GalaxyViewer.Settings();
		gas = new GalaxyAnimator.Settings();
		tabs.add("GalaxyViewer", gvs);
		tabs.add("GalaxyAnimator", gas);
		setLayout(new BorderLayout());
		add(tabs,BorderLayout.CENTER);
		add(launch,BorderLayout.SOUTH);
		launch.addActionListener(this);
		pack();
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-getWidth())/2, (screen.height-getHeight())/2);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		try{
			if (e.getSource() == launch){
				if (tabs.getSelectedIndex() == 0){
					gvs.update();
					dispose();
					new GalaxyViewer(gvs,false);
				}
				else if (tabs.getSelectedIndex() == 1){
					gas.update();
					dispose();
					new GalaxyAnimator(gas);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, ex.toString());
		}
	}
}
