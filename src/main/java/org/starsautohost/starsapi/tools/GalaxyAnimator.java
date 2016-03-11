package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Vector;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

import org.starsautohost.starsapi.tools.GalaxyViewer.RPanel;
import org.starsautohost.starsapi.tools.GalaxyViewer.SelectDirectory;

public class GalaxyAnimator extends JFrame implements ActionListener{

	private static final long serialVersionUID = 1L;
	private Settings settings;
	private Loader loader;
	private Player player = new Player();
	private JButton previous = new JButton("<<<");
	private JButton next = new JButton(">>>");
	private JButton play = new JButton("Play");
	private JButton saveGif = new JButton("Save gif");
	private JLabel year = new JLabel();
	private JPanel content = new JPanel();
	private int currentNr = -1;
	private JButton[] buttons = {previous,next,play,saveGif};
	
	public static void main(String[] args){
		try{
			Settings settings = new Settings();
			settings.showNow();
			new GalaxyAnimator(settings);
		}catch(Exception ex){
			ex.printStackTrace();
			System.err.println(ex.toString());
			JOptionPane.showMessageDialog(null, ex.toString());
			System.exit(0);
		}
	}
		
	protected static class Settings extends JPanel{
		private static final long serialVersionUID = 1L;
		protected String directory = ".";
		protected String gameName = "";
		JTextField gName;
		JTextField dir;
		private File f;
		
		protected String getGameName(){
			return gameName.toUpperCase();
		}
		public Settings() throws Exception{
			f = new File("galaxyanimator.ini");
			if (f.getAbsoluteFile().getParentFile().getName().equals("bin")) f = new File("..","galaxyanimator.ini");
			if (f.exists()){
				BufferedReader in = new BufferedReader(new FileReader(f));
				while(true){
					String s = in.readLine();
					if (s == null) break;
					if (s.contains("=") == false) continue;
					String[] el = s.split("=",-1);
					if (el[0].equalsIgnoreCase("GameName")) gameName = el[1].trim();
					if (el[0].equalsIgnoreCase("GameDir")) directory = el[1].trim();
				}
				in.close();
			}
			gName = new JTextField(gameName);
			dir = new JTextField(""+directory);
			JButton selectDir = new JButton("...");
			selectDir.addActionListener(new SelectDirectory(gName,dir));
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(3,1));
			p.add(createPanel(new JLabel("Game name"),gName));
			p.add(createPanel(new JLabel("Game directory"),GalaxyViewer.createPanel(null, dir, selectDir)));
			p.add(new JLabel("Directory must have subdirectories ala 2400, 2401, 2402 etc (complete set not needed)"));
			gName.setToolTipText("Do not include file extensions");
			setLayout(new BorderLayout());
			add(p,BorderLayout.CENTER);
		}
		public void showNow() throws Exception{
			String[] el = {"Ok","Cancel"};
			int ok = JOptionPane.showOptionDialog(null,this,"Choose settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,el,el[0]);
			if (ok != 0) System.exit(0);
			update();
		}
		public void update() throws Exception{
			directory = dir.getText().trim();
			gameName = gName.getText().trim();
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("GameName="+gameName+"\n");
			out.write("GameDir="+directory+"\n");
			out.flush(); out.close();
		}
		private JPanel createPanel(Component c1, Component c2){
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(1,2));
			p.add(c1); p.add(c2);
			return p;
		}
	}
	
	public GalaxyAnimator(Settings settings) throws Exception{
		super("Stars GalaxyAnimator");
		this.settings = settings;
		if (settings.gameName.equals("")) throw new Exception("GameName not defined in settings.");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		File dir = new File(settings.directory);
		File map = GalaxyLauncher.getMapFile(dir,settings.getGameName());
		Vector<File> years = new Vector<File>();
		for (File f : dir.listFiles()){
			if (f.isDirectory() && GalaxyViewer.isInteger(f.getName())) years.addElement(f);
		}
		if (years.size() <= 1) throw new Exception(years.size()+" years found.\nEach year should be a subdirectory with the year as directory name.");
		
		loader = new Loader(years);
		//UI:
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel south = new JPanel();
		south.setLayout(new GridLayout(1,5));
		year.setFont(year.getFont().deriveFont(Font.BOLD, 20));
		south.add(year);
		south.add(previous);
		south.add(play);
		south.add(next);
		south.add(saveGif);
		cp.add(content,BorderLayout.CENTER);
		cp.add(south,BorderLayout.SOUTH);
		previous.addActionListener(this);
		next.addActionListener(this);
		play.addActionListener(this);
		saveGif.addActionListener(this);
		saveGif.setToolTipText("NB! Image dimensions will be that of your current window size");
		setVisible(true);
		setExtendedState(getExtendedState()|JFrame.MAXIMIZED_BOTH );
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == next) setViewer(currentNr+1);
		else if (e.getSource() == previous) setViewer(currentNr-1);
		else if (e.getSource() == play){
			if (player.playing == false && currentNr == loader.viewers.length - 1) setViewer(0);
			player.startOrStop();
		}
		else if (e.getSource() == saveGif){
			JFileChooser jfc = new JFileChooser();
			jfc.setSelectedFile(new File("animation.gif"));
			if (jfc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION){
				File f = jfc.getSelectedFile();
				if (f != null){
					new SaveGifThread(f).start();
				}
			}
		}
	}
	
	private class SaveGifThread extends Thread{
		private File f;

		public SaveGifThread(File f) {
			this.f = f;
		}

		public void run(){
			try{
				for (JButton b : buttons) b.setEnabled(false);
				ImageOutputStream output = new FileImageOutputStream(f);
				GifSequenceWriter gif = new GifSequenceWriter(output, BufferedImage.TYPE_INT_RGB, 1000, true);
				for (int t = 0; t < loader.viewers.length; t++){
					setViewer(t);
					Thread.sleep(100);
					RPanel p = loader.viewers[t].universe;
					BufferedImage i = new BufferedImage(p.getWidth(), p.getHeight(), BufferedImage.TYPE_INT_RGB);
					Graphics2D gr = i.createGraphics();
					p.paint(gr);
					gr.setColor(Color.blue);
					gr.setFont(gr.getFont().deriveFont((float)30));
					gr.drawString(year.getText(), 10, i.getHeight()-20);
					gif.writeToSequence(i);
				}
				gif.close();
			    output.close();
			    JOptionPane.showMessageDialog(GalaxyAnimator.this, f.getAbsolutePath()+" created");
			}catch(Exception ex){
				ex.printStackTrace();
				JOptionPane.showMessageDialog(GalaxyAnimator.this, ex.toString());
			}catch(OutOfMemoryError err){
				err.printStackTrace();
				JOptionPane.showMessageDialog(GalaxyAnimator.this, "Increase available memory for java"+"\n"+err.toString());
			}
			for (JButton b : buttons) b.setEnabled(true);
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
					else{ //Removing noise in initial play
						gv.universe.setSize(viewers[0].universe.getSize());
					}
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
