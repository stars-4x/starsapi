package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.Vector;

import javax.swing.*;

public class GalaxyAnimator extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private Settings settings;
	private Loader loader;
	private Player player = new Player();
	private JButton previous = new JButton("<<<");
	private JButton next = new JButton(">>>");
	private JButton play = new JButton("Play");
	private JLabel year = new JLabel();
	private JPanel content = new JPanel();
	private int currentNr = -1;
	
	public static void main(String[] args){
		try{
			Settings settings = Settings.init();
			new GalaxyAnimator(settings);
		}catch(Exception ex){
			ex.printStackTrace();
			System.err.println(ex.toString());
			JOptionPane.showMessageDialog(null, ex.toString());
			System.exit(0);
		}
	}
		
	private static class Settings{
		protected String directory = ".";
		protected String gameName = "";
		protected String getGameName(){
			return gameName.toUpperCase();
		}
		public static Settings init() throws Exception{
			File f = new File("galaxyanimator.ini");
			if (f.getAbsoluteFile().getParentFile().getName().equals("bin")) f = new File("..","galaxyanimator.ini");
			Settings settings;
			if (f.exists()){
				settings = new Settings();
				BufferedReader in = new BufferedReader(new FileReader(f));
				while(true){
					String s = in.readLine();
					if (s == null) break;
					if (s.contains("=") == false) continue;
					String[] el = s.split("=",-1);
					if (el[0].equalsIgnoreCase("GameName")) settings.gameName = el[1].trim();
					if (el[0].equalsIgnoreCase("GameDir")) settings.directory = el[1].trim();
				}
				in.close();
			}
			else settings = new Settings();
			JTextField gName = new JTextField(settings.gameName);
			JTextField dir = new JTextField(""+settings.directory);
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(3,2));
			p.add(new JLabel("Game name")); p.add(gName);
			p.add(new JLabel("Game directory")); p.add(dir);
			gName.setToolTipText("Do not include file extensions");
			String[] el = {"Ok","Cancel"};
			int ok = JOptionPane.showOptionDialog(null,p,"Choose settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,el,el[0]);
			if (ok != 0) System.exit(0);
			settings.directory = dir.getText().trim();
			settings.gameName = gName.getText().trim();
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("GameName="+settings.gameName+"\n");
			out.write("GameDir="+settings.directory+"\n");
			out.flush(); out.close();
			return settings;
		}
	}
	
	public GalaxyAnimator(Settings settings) throws Exception{
		super("Stars GalaxyAnimator");
		this.settings = settings;
		if (settings.gameName.equals("")) throw new Exception("GameName not defined in settings.");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		File dir = new File(settings.directory);
		File map = new File(dir,settings.getGameName()+".MAP");
		if (map.exists() == false){
			String error = "Could not find "+map.getAbsolutePath()+"\n";
			error += "Export this file from Stars! (Only needs to be done one time pr game)";
			throw new Exception(error);
		}
		Vector<File> years = new Vector<File>();
		for (File f : dir.listFiles()){
			if (f.isDirectory() && isInteger(f.getName())) years.addElement(f);
		}
		if (years.size() <= 1) throw new Exception(years.size()+" years found.\nEach year should be a subdirectory with the year as directory name.");
		
		loader = new Loader(years);
		//UI:
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel south = new JPanel();
		south.setLayout(new GridLayout(1,4));
		year.setFont(year.getFont().deriveFont(Font.BOLD, 20));
		south.add(year);
		south.add(previous);
		south.add(play);
		south.add(next);
		cp.add(content,BorderLayout.CENTER);
		cp.add(south,BorderLayout.SOUTH);
		previous.addActionListener(this);
		next.addActionListener(this);
		play.addActionListener(this);
		setVisible(true);
		setExtendedState(getExtendedState()|JFrame.MAXIMIZED_BOTH );
	}

	private boolean isInteger(String s) {
		try{
			Integer.parseInt(s);
			return true;
		}catch(NumberFormatException ex){
			return false;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == next) setViewer(currentNr+1);
		else if (e.getSource() == previous) setViewer(currentNr-1);
		else if (e.getSource() == play){
			if (player.playing == false && currentNr == loader.viewers.length - 1) setViewer(0);
			player.startOrStop();
		}
	}
	
	public boolean setViewer(int nr){
		if (nr >= loader.viewers.length) return false;
		if (nr < 0) return false;
		this.currentNr = nr;
		content.removeAll();
		GalaxyViewer gv = loader.viewers[nr];
		if (gv == null){
			content.setLayout(new GridBagLayout());
			content.add(new JLabel("Frame "+nr+" not loaded."));
		}
		else{
			content.setLayout(new BorderLayout());
			content.add(gv.universe,BorderLayout.CENTER);
			gv.universe.setSize(content.getSize());
			gv.universe.zoomToFillGalaxy();
		}
		year.setText(loader.years.elementAt(nr).getName());
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				content.updateUI();
			}
		});
		return true;
	}
	
	private class Player extends Thread{
		private boolean playing = false;
		private Player(){
			start();
		}
		public void startOrStop() {
			playing = !playing;
			play.setText(playing?"Stop":"Start");
		}
		public void run(){
			while(true){
				while(playing == false){
					try{Thread.sleep(10);}catch(InterruptedException ex){}
				}
				try{Thread.sleep(1200);}catch(InterruptedException ex){}
				if (playing){
					boolean ok = setViewer(currentNr+1);
					if (ok == false) startOrStop();
				}
			}
		}
	}
	
	private class Loader extends Thread{
		private Vector<File> years;
		private GalaxyViewer[] viewers;
		
		public Loader(Vector<File> years) {
			this.years = years;
			viewers = new GalaxyViewer[years.size()];
			start();
		}
		public void run(){
			try{
				for (int t = 0; t < years.size(); t++){
					GalaxyViewer.Settings s = new GalaxyViewer.Settings();
					s.playerNr = -1;
					s.gameName = settings.gameName;
					s.directory = years.elementAt(t).getAbsolutePath();
					GalaxyViewer gv = new GalaxyViewer(s,true);
					viewers[t] = gv;
					if (t == 0) setViewer(0);
				}
			}catch(final Exception ex){
				ex.printStackTrace();
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run() {
						JOptionPane.showMessageDialog(GalaxyAnimator.this, ex.toString());
					}
				});
			}
		}
	}
}
