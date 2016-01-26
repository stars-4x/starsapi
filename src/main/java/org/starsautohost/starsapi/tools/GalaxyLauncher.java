package org.starsautohost.starsapi.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

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

	public static File getMapFile(File dir, String gameName) throws Exception{
		String original = gameName+".MAP";
		File map = new File(dir,gameName+".MAP");
		if (map.exists()) return map;
		File f = new File(dir,gameName+".map");
		if (f.exists()) return f;		
		f = new File(dir.getParentFile(),gameName+".MAP");
		if (f.exists()) return f;
		f = new File(dir.getParentFile(),gameName+".map");
		if (f.exists()) return f;
		String error = "Could not find "+map.getAbsolutePath()+"\n";
		error += "Export this file from Stars! (Only needs to be done one time pr game)";
		throw new Exception(error);
	}
}
