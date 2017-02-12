package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;
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
	private JButton previous = new JButton("<");
	private JButton next = new JButton(">");
	private JButton first = new JButton("|<<");
	private JButton last = new JButton(">>|");
	private JButton play = new JButton("Play");
	private JButton saveGif = new JButton("Save gif");
	private JLabel year = new JLabel();
	private JPanel content = new JPanel();
	private int currentNr = -1;
	private JButton[] buttons = {first,previous,play,next,last,saveGif};
	
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
		protected int startFrame = 0;
		protected int maxFrames = 1000;
		protected boolean paintVoronoi = false;
		JTextField gName;
		JTextField dir;
		JTextField start;
		JTextField max;
		JCheckBox voronoi;
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
					if (el[0].equalsIgnoreCase("StartFrame")) startFrame = Integer.parseInt(el[1].trim());
					if (el[0].equalsIgnoreCase("MaxFrames")) maxFrames = Integer.parseInt(el[1].trim());
					if (el[0].equalsIgnoreCase("PaintVoronoi")) paintVoronoi = Boolean.parseBoolean(el[1].trim());
				}
				in.close();
			}
			gName = new JTextField(gameName);
			dir = new JTextField(""+directory);
			start = new JTextField(""+startFrame);
			max = new JTextField(""+maxFrames);
			voronoi = new JCheckBox("Paint voronoi (computational expensive)");
			JButton selectDir = new JButton("...");
			selectDir.addActionListener(new SelectDirectory(gName,dir));
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(7,1));
			p.add(createPanel(new JLabel("Game name"),gName));
			p.add(createPanel(new JLabel("Game directory"),GalaxyViewer.createPanel(null, dir, selectDir)));
			p.add(new JLabel("Directory must have subdirectories ala 2400, 2401, 2402 etc (complete set not needed)"));
			p.add(new JLabel("(backupxxx or backup.xxx is also accepted)"));
			p.add(createPanel(new JLabel("Start-frame"),start));
			p.add(createPanel(new JLabel("Max frames"),max));
			p.add(voronoi);
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
			startFrame = Integer.parseInt(start.getText());
			maxFrames = Integer.parseInt(max.getText());
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("GameName="+gameName+"\n");
			out.write("GameDir="+directory+"\n");
			out.write("StartFrame="+startFrame+"\n");
			out.write("MaxFrames="+maxFrames+"\n");
			out.write("PaintVoronoi="+paintVoronoi+"\n");
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
		java.util.List<RFile> years = new Vector<RFile>();
		for (File f : dir.listFiles()){
			if (f.isDirectory()){
				String s = f.getName();
				if (s.toLowerCase().startsWith("backup.")) s = s.substring(7);
				else if (s.toLowerCase().startsWith("backup")) s = s.substring(6);
				if (GalaxyViewer.isInteger(s)){
					years.add(new RFile(f,Integer.parseInt(s)));
				}
			}
		}
		if (years.size() <= 1) throw new Exception(years.size()+" years found.\nEach year should be a subdirectory with the year as directory name.\nbackupxxx or backup.xxx is also accepted");
		Collections.sort(years);
		if (settings.startFrame > 0){
			years = years.subList(settings.startFrame, years.size()-1);
		}
		years = years.subList(0, Math.min(settings.maxFrames,years.size()));
		loader = new Loader(years);
		//UI:
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		JPanel south = new JPanel();
		south.setLayout(new GridLayout(1,7));
		year.setFont(year.getFont().deriveFont(Font.BOLD, 20));
		south.add(year);
		south.add(first);
		south.add(previous);
		south.add(play);
		south.add(next);
		south.add(last);
		south.add(saveGif);
		cp.add(content,BorderLayout.CENTER);
		cp.add(south,BorderLayout.SOUTH);
		previous.addActionListener(this);
		next.addActionListener(this);
		first.addActionListener(this);
		last.addActionListener(this);
		play.addActionListener(this);
		saveGif.addActionListener(this);
		saveGif.setToolTipText("NB! Image dimensions will be that of your current window size");
		setVisible(true);
		setExtendedState(getExtendedState()|JFrame.MAXIMIZED_BOTH );
	}

	private class RFile implements Comparable<RFile>{
		private File f;
		private int year;

		private RFile(File f, int year){
			this.f = f;
			this.year = year;
		}

		@Override
		public int compareTo(RFile o) {
			return new Integer(year).compareTo(o.year);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == next) setViewer(currentNr+1);
		else if (e.getSource() == previous) setViewer(currentNr-1);
		else if (e.getSource() == first) setViewer(0);
		else if (e.getSource() == last) setViewer(loader.viewers.length-1);
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
		year.setText(""+loader.viewers[nr].getYear()); //years.get(nr).year+"");
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
		private java.util.List<RFile> years;
		private GalaxyViewer[] viewers;
		
		public Loader(java.util.List<RFile> years) {
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
					s.directory = years.get(t).f.getAbsolutePath();
					GalaxyViewer gv = new GalaxyViewer(s,true,settings.paintVoronoi);
					viewers[t] = gv;
					if (t == 0) setViewer(0);
					else{ //Removing noise in initial play
						gv.universe.setSize(viewers[0].universe.getSize());
					}
					gv.removeSomeInfo();
					setTitle("Stars GalaxyAnimator"+", Loaded "+(t+1)+" / "+years.size()+" frames.");
				}
				setTitle("Stars GalaxyAnimator");
			}catch(final Exception ex){
				ex.printStackTrace();
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run() {
						JOptionPane.showMessageDialog(GalaxyAnimator.this, ex.toString());
					}
				});
			}catch(final OutOfMemoryError err){
				err.printStackTrace();
				SwingUtilities.invokeLater(new Runnable(){
					@Override
					public void run() {
						JOptionPane.showMessageDialog(GalaxyAnimator.this, "An OutOfMemoryError occured.\nTry giving java some more memory to load all the years.\n"+err.toString());
					}
				});
			}
		}
	}
}
