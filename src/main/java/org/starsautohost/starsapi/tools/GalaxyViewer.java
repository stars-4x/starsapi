package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;

/**
 * 
 * @author Runar Holen
 *
 * TODO:
 * -Fleets, more info, etc
 * -Make an animation class displaying progress based on subdirectories (2400,2401,2402 etc)
 * -Finally, make an android app to display galaxy when on the move (with dropbox-integration to get files?)
 */
public class GalaxyViewer extends JFrame implements ActionListener, ChangeListener, KeyListener{

	private static final long serialVersionUID = 1L;
	private Parser p;
	private HashMap<Integer, String> planetNames = new HashMap<Integer, String>();
	private HashMap<Integer, Point> planetCoordinates = new HashMap<Integer, Point>();
	private int maxX = 0, maxY = 0;
	private Settings settings;
	protected Vector<Integer> friends = new Vector<Integer>();
	
	//UI
	protected RPanel universe = new RPanel();
	private JButton hw = new JButton("HW");
	private JTextField search = new JTextField();
	private JCheckBox names = new JCheckBox("Names",true);
	private JSlider zoom = new JSlider(25, 600, 100);
	private JButton help = new JButton("Help");
	private JCheckBox colorize = new JCheckBox("Colorize",false);
	private HashMap<Integer,Color> colors = new HashMap<Integer, Color>();
	private boolean animatorFrame;
	
	public static void main(String[] args) throws Exception{
		try{
			Settings settings = Settings.init();
			new GalaxyViewer(settings,false);
		}catch(Exception ex){
			ex.printStackTrace();
			System.err.println(ex.toString());
			JOptionPane.showMessageDialog(null, ex.toString());
			System.exit(0);
		}
	}
	
	protected static class Settings{
		protected int playerNr = 0;
		protected String directory = ".";
		protected String gameName = "";
		protected String getGameName(){
			return gameName.toUpperCase();
		}
		public static Settings init() throws Exception{
			File f = new File("galaxyviewer.ini");
			if (f.getAbsoluteFile().getParentFile().getName().equals("bin")) f = new File("..","galaxyviewer.ini");
			Settings settings;
			if (f.exists()){
				settings = new Settings();
				BufferedReader in = new BufferedReader(new FileReader(f));
				while(true){
					String s = in.readLine();
					if (s == null) break;
					if (s.contains("=") == false) continue;
					String[] el = s.split("=",-1);
					if (el[0].equalsIgnoreCase("PlayerNr")) settings.playerNr = Integer.parseInt(el[1].trim())-1;
					if (el[0].equalsIgnoreCase("GameName")) settings.gameName = el[1].trim();
					if (el[0].equalsIgnoreCase("GameDir")) settings.directory = el[1].trim();
				}
				in.close();
			}
			else settings = new Settings();
			
			JTextField pNr = new JTextField(""+(settings.playerNr+1));
			JTextField gName = new JTextField(settings.gameName);
			JTextField dir = new JTextField(""+settings.directory);
			
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(3,2));
			p.add(new JLabel("Player nr")); p.add(pNr);
			p.add(new JLabel("Game name")); p.add(gName);
			p.add(new JLabel("Game directory")); p.add(dir);
			gName.setToolTipText("Do not include file extensions");
			String[] el = {"Ok","Cancel"};
			int ok = JOptionPane.showOptionDialog(null,p,"Choose settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,el,el[0]);
			if (ok != 0) System.exit(0);
			settings.playerNr = Integer.parseInt(pNr.getText().trim())-1;
			settings.directory = dir.getText().trim();
			settings.gameName = gName.getText().trim();
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("PlayerNr="+(settings.playerNr+1)+"\n");
			out.write("GameName="+settings.gameName+"\n");
			out.write("GameDir="+settings.directory+"\n");
			out.flush(); out.close();
			return settings;
		}
	}
	
	public GalaxyViewer(Settings settings, boolean animatorFrame) throws Exception{
		super("Stars GalaxyViewer");
		this.settings = settings;
		this.animatorFrame = animatorFrame;
		if (settings.gameName.equals("")) throw new Exception("GameName not defined in settings.");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		File dir = new File(settings.directory);
		File map = new File(dir,settings.getGameName()+".MAP");
		if (map.exists() == false){
			File f = new File(dir.getParentFile(),settings.getGameName()+".MAP");
			if (f.exists()) map = f;
			else{
				String error = "Could not find "+map.getAbsolutePath()+"\n";
				error += "Export this file from Stars! (Only needs to be done one time pr game)";
				throw new Exception(error);
			}
		}
		Vector<File> mFiles = new Vector<File>();
		Vector<File> hFiles = new Vector<File>();
		for (File f : dir.listFiles()){
			if (f.getName().toUpperCase().endsWith("MAP")) continue;
			if (f.getName().toUpperCase().endsWith("HST")) continue;
			if (f.getName().toUpperCase().startsWith(settings.getGameName()+".M")) mFiles.addElement(f);
			else if (f.getName().toUpperCase().startsWith(settings.getGameName()+".H")) hFiles.addElement(f);
		}
		if (mFiles.size() == 0) throw new Exception("No M-files found matching game name.");
		if (hFiles.size() == 0) throw new Exception("No H-files found matching game name.");
		parseMapFile(map);
		Vector<File> files = new Vector<File>();
		files.addAll(mFiles);
		files.addAll(hFiles);
		p = new Parser(files);
		calculateColors();
		
		//UI:
		JPanel cp = (JPanel)getContentPane();
		cp.setLayout(new BorderLayout());
		cp.add(universe,BorderLayout.CENTER);
		JPanel south = createPanel(0,hw,new JLabel("Search: "),search,names,zoom,colorize);
		search.setPreferredSize(new Dimension(100,-1));
		cp.add(south,BorderLayout.SOUTH);
		hw.addActionListener(this);
		names.addActionListener(this);
		zoom.addChangeListener(this);
		search.addKeyListener(this);
		colorize.addActionListener(this);
		setSize(800,600);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-getWidth())/2, (screen.height-getHeight())/2);
		setVisible(animatorFrame == false);
		if (animatorFrame) names.setSelected(false);
	}
	
	private void parseMapFile(File map) throws Exception{
		BufferedReader in = new BufferedReader(new FileReader(map));
		in.readLine();
		while(true){
			String s = in.readLine();
			if (s == null) break;
			if (s.trim().equals("")) continue;
			String[] el = s.split("\t");
			int id = Integer.parseInt(el[0])-1;
			planetNames.put(id,el[3]);
			int x = Integer.parseInt(el[1]);
			int y = Integer.parseInt(el[2]);
			if (x > maxX) maxX = x;
			if (y > maxY) maxY = y;
			planetCoordinates.put(id,new Point(x,y));
		}
		in.close();
	}
	
	protected class Parser extends HFileMerger{
		public Parser(Vector<File> files) throws Exception{
			for (File f : files){
				System.out.println("Parsing "+f.getName());
				List<Block> blocks = new Decryptor().readFile(f.getAbsolutePath());
				System.out.println(blocks.size()+" blocks to parse");
	            new FileProcessor().process(blocks);
	            //printBlockCount(blocks);
			}
			postProcess();
			
			if (settings.playerNr >= 0){ //Not set if called from GalaxyAnimator
				PlayerBlock player = players[settings.playerNr];
				if (player != null){
					for (PlayerBlock pb : players){
						if (pb == null) continue;
						if (player.getPlayerRelationsWith(pb.playerNumber) == 1) friends.addElement(pb.playerNumber);
					}
				}
			}
		}

		private void printBlockCount(List<Block> blocks) {
			HashMap<Class,Integer> count = new HashMap<Class,Integer>();
			for (Block b : blocks){
				Integer i = count.get(b.getClass());
				if (i == null) i = 0;
				count.put(b.getClass(),i+1);
			}
			System.out.println("Block counts:");
			for (Class c : count.keySet()){
				System.out.println(c.getName()+" "+count.get(c));
			}
		}
    }
	
	private void calculateColors(){
		if (animatorFrame){
			List<Color> distinctColors = pickColors(16);
			for (int t = 0; t < distinctColors.size(); t++){
				colors.put(t,distinctColors.get(t));
			}
		}
		else{
			Vector<PlayerBlock> friends = new Vector<PlayerBlock>();
			Vector<PlayerBlock> enemies = new Vector<PlayerBlock>();
			Vector<Color> friendlyColors = new Vector<Color>();
			Vector<Color> enemyColors = new Vector<Color>();
			friendlyColors.addElement(new Color(255,255,0));
			friendlyColors.addElement(new Color(183,197,21));
			friendlyColors.addElement(new Color(197,133,21));
			friendlyColors.addElement(new Color(21,197,125));
			friendlyColors.addElement(new Color(255,255,192));
			friendlyColors.addElement(new Color(21,178,197));
			
			enemyColors.addElement(new Color(255,0,0));
			enemyColors.addElement(new Color(255,92,92));
			enemyColors.addElement(new Color(197,21,133));
			enemyColors.addElement(new Color(255,64,64));
			enemyColors.addElement(new Color(255,128,128));
			enemyColors.addElement(new Color(255,0,255));
			enemyColors.addElement(new Color(145,0,197));
			enemyColors.addElement(new Color(112,34,34));
			enemyColors.addElement(new Color(90,34,102));
			enemyColors.addElement(new Color(230,126,56));
			for (PlayerBlock pb : p.players){
				if (pb == null) continue;
				if (pb.playerNumber == settings.playerNr) continue;
				if (pb.getPlayerRelationsWith(settings.playerNr) == 1) friends.addElement(pb);
				if (friends.contains(pb.playerNumber)) friends.addElement(pb);
				else enemies.addElement(pb);
			}
			for (int t = 0; t < enemies.size(); t++){
				PlayerBlock pb = enemies.elementAt(t);
				if (enemyColors.size() > 0) colors.put(pb.playerNumber, enemyColors.remove(0));
			}
			for (int t = 0; t < friends.size(); t++){
				PlayerBlock pb = friends.elementAt(t);
				if (friendlyColors.size() > 0) colors.put(pb.playerNumber, friendlyColors.remove(0));
			}
		}
	}
	
	private PartialPlanetBlock getPlanet(int id, int hwForPlayer){
		for (PartialPlanetBlock ppb : p.planetBlocks){
			if (ppb.planetNumber == id) return ppb;
			if (ppb.isHomeworld && ppb.owner == hwForPlayer) return ppb;
		}
		return null;
	}
	
	protected class RPanel extends JPanel{
		private static final long serialVersionUID = 1L;
		private double zoom = 100.0;
		private int mariginX = 0;
		private int mariginY = 0;
		
		private RPanel(){
			MyMouseListener mml = new MyMouseListener();
			addMouseListener(mml);
			addMouseMotionListener(mml);
		}
		
		private class MyMouseListener extends MouseAdapter{
			int x = -1, y = -1;
			public void mousePressed(MouseEvent e){
				x = e.getX();
				y = e.getY();
			}
			public void mouseDragged(MouseEvent e){
				int dx = e.getX()-x;
				int dy = e.getY()-y;
				x = e.getX();
				y = e.getY();
				mariginX -= dx*100/zoom;
				mariginY -= dy*100/zoom;
				repaint();
			}
		}
		
		@Override
		public void paint(Graphics g){
			Graphics2D g2d = (Graphics2D)g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());
			for (Integer id : planetNames.keySet()){
				String name = planetNames.get(id);
				Point p = planetCoordinates.get(id);
				PartialPlanetBlock planet = getPlanet(id, -2);
				g.setColor(Color.gray);
				int rad = 3;
				if (planet != null){
					rad = 6;
					if (animatorFrame){
						Color col = colors.get(planet.owner);
						if (col == null) col = Color.gray;
						g.setColor(col);
					}
					else{
						if (planet.owner == settings.playerNr) g.setColor(Color.green);
						else if (colorize.isSelected() && colors.get(planet.owner) != null) g.setColor(colors.get(planet.owner));
						else if (friends.contains(planet.owner)) g.setColor(Color.YELLOW);
						else if (planet.owner >= 0) g.setColor(Color.red);
						else rad = 3;
					}
				}
				int x = convertX(p.x);
				int y = convertY(p.y);
				double virtualWidth = getWidth() * zoom / 100;
				double virtualHeight = getHeight() * zoom / 100;
				int xOffset = (int)(getWidth() - virtualWidth) / 2;
		        int yOffset = (int)(getHeight() - virtualHeight) / 2;
				x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
				y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
		        g.fillOval(x-rad/2, y-rad/2, rad, rad);
				if (names.isSelected()){
					g.setFont(g.getFont().deriveFont((float)10));
					g.setColor(Color.gray);
					int stringWidth = g.getFontMetrics().stringWidth(name);
					g.drawString(name, x-stringWidth/2, y+12);
				}
				if (colorize.isSelected() || animatorFrame){
					int yy = 20;
					g.setFont(g.getFont().deriveFont((float)12));
					for (PlayerBlock pb : GalaxyViewer.this.p.players){
						if (pb == null) continue;
						Color col = colors.get(pb.playerNumber);
						if (animatorFrame && col != null) ; //Ok
						else if (pb.playerNumber == settings.playerNr) col = Color.green;
						else if (col == null) col = Color.red;
						g.setColor(col);
						String n = new String(pb.nameBytes); //Does not work
						n = "Player "+(pb.playerNumber+1);
						g.drawString(n,5,yy);
						//System.out.print(pb.playerNumber+": ");
						//for (int t = 0; t < pb.nameBytes.length; t++){
							//System.out.print((pb.nameBytes[t]&0xff)+" ");
							//System.out.print((pb.nameBytes[t])+" ");
						//}
						//System.out.println();
						yy += 14;
					}
				}
			}
		}

		private int convertX(int x){
			return x - 1000;
		}
		private int convertY(int y){
			return maxY-y+10;
		}
		
		public void centerOnPoint(Point p) {
			double x = (double)convertX(p.x);
			double y = (double)convertY(p.y);
			double w = (double)getWidth();
			double h = (double)getHeight();
			mariginX = (int)((x-(w/2.0)));
			mariginY = (int)((y-(h/2.0)));
			System.out.println("Planet: "+x+" "+y);
			System.out.println("Marigin: "+mariginX+" "+mariginY);
			repaint();
		}

		public void zoomToFillGalaxy() {
			double ySize = maxY-1000;
			zoom = 100.0 * getHeight() / ySize;
			Point center = new Point((maxX-1000)/2+1000,(maxY-1000)/2+1000);
			centerOnPoint(center);
			repaint();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == hw){
			PartialPlanetBlock planet = getPlanet(-1, settings.playerNr);
			if (planet != null){
				Point p = planetCoordinates.get(planet.planetNumber);
				if (p != null) universe.centerOnPoint(p);
			}
		}
		else if (e.getSource() == names){
			repaint();
		}
		else if (e.getSource() == colorize){
			repaint();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		universe.zoom = zoom.getValue();
		repaint();
	}
	
	private JPanel createPanel(int index, Component... components) {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setOpaque(false);
		if (index >= components.length) return p;
		p.add(components[index],BorderLayout.WEST);
		p.add(createPanel(index+1,components),BorderLayout.CENTER);
		return p;
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}
	@Override
	public void keyPressed(KeyEvent e) {
	}
	@Override
	public void keyReleased(KeyEvent e) {
		String s = search.getText().toLowerCase().trim();
		if (s.equals("") == false){
			//First pass: Starts with (So that Ney is prioritized before McCartney)
			for (Integer id : planetNames.keySet()){
				String name = planetNames.get(id).toLowerCase();
				if (name.startsWith(s)){
					universe.centerOnPoint(planetCoordinates.get(id));
					return;
				}
			}
			//Second pass: Contains
			for (Integer id : planetNames.keySet()){
				String name = planetNames.get(id).toLowerCase();
				if (name.contains(s)){
					universe.centerOnPoint(planetCoordinates.get(id));
					return;
				}
			}
		}
	}
	
	public static List<Color> pickColors(int num) {
		List<Color> colors = new ArrayList<Color>();
		if (num < 2)
			return colors;
		float dx = 1.0f / (float) (num - 1);
		for (int i = 0; i < num; i++) {
			colors.add(get(i * dx));
		}
		return colors;
	}

	public static Color get(float x) {
		float r = 0.0f;
		float g = 0.0f;
		float b = 1.0f;
		if (x >= 0.0f && x < 0.2f) {
			x = x / 0.2f;
			r = 0.0f;
			g = x;
			b = 1.0f;
		} else if (x >= 0.2f && x < 0.4f) {
			x = (x - 0.2f) / 0.2f;
			r = 0.0f;
			g = 1.0f;
			b = 1.0f - x;
		} else if (x >= 0.4f && x < 0.6f) {
			x = (x - 0.4f) / 0.2f;
			r = x;
			g = 1.0f;
			b = 0.0f;
		} else if (x >= 0.6f && x < 0.8f) {
			x = (x - 0.6f) / 0.2f;
			r = 1.0f;
			g = 1.0f - x;
			b = 0.0f;
		} else if (x >= 0.8f && x <= 1.0f) {
			x = (x - 0.8f) / 0.2f;
			r = 1.0f;
			g = 0.0f;
			b = x;
		}
		return new Color(r, g, b);
	}
}
