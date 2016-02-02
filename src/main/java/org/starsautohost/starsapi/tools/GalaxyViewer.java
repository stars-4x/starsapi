package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.Block;
import org.starsautohost.starsapi.block.PartialFleetBlock;
import org.starsautohost.starsapi.block.PartialPlanetBlock;
import org.starsautohost.starsapi.block.PlayerBlock;
import org.starsautohost.starsapi.encryption.Decryptor;
import org.starsautohost.starsapi.tools.GameToTestbed.FleetInfo;
import org.starsautohost.starsapi.tools.GameToTestbed.PlanetInfo;
import org.starsautohost.starsapi.tools.GameToTestbed.PlayerInfo;

/**
 * 
 * @author Runar Holen (platon79 on SAH-forums)
 *
 * TODO:
 * -More info, etc
 * -Finally, make an android app to display galaxy when on the move (with dropbox-integration to get files?)
 */
public class GalaxyViewer extends JFrame implements ActionListener, ChangeListener, KeyListener{

	private static final long serialVersionUID = 1L;
	private Parser p;
	private HashMap<Integer, String> planetNames = new HashMap<Integer, String>();
	private HashMap<Integer, Point> planetCoordinates = new HashMap<Integer, Point>();
	private HashMap<Point, Integer> planetNrs = new HashMap<Point, Integer>();
	private int maxX = 0, maxY = 0;
	private Settings settings;
	protected Vector<Integer> friends = new Vector<Integer>();
	private int bigFleetCounter = -1;
	private Vector<PlayerInfo> sortedPlayers = new Vector<PlayerInfo>();
	private HashMap<Point,EnemyFleetInfo> enemyFleetInfo = new HashMap<Point,EnemyFleetInfo>();
	private HashMap<Point,FriendlyFleetInfo> friendlyFleetInfo = new HashMap<Point,FriendlyFleetInfo>();
	private HashMap<Point,Integer> totalFleetCount = new HashMap<Point,Integer>();
	private HashMap<Integer,String> playerNames = new HashMap<Integer,String>();
	
	//UI
	protected RPanel universe = new RPanel();
	private JButton hw = new JButton("HW");
	private JTextField search = new JTextField();
	private JCheckBox names = new JCheckBox("Names",false);
	private JSlider zoomSlider = new JSlider(25, 600, 100);
	private JButton help = new JButton("Help");
	private JCheckBox colorize = new JCheckBox("Colorize",false);
	private JCheckBox showFleets = new JCheckBox("Show fleets",false);
	private JTextField massFilter = new JTextField();
	private JButton gotoBigFleets = new JButton("Go to big enemy fleets");
	private HashMap<Integer,Color> colors = new HashMap<Integer, Color>();
	private JLabel info = new JLabel();
	private boolean animatorFrame;
	
	public static void main(String[] args) throws Exception{
		try{
			Settings settings = new Settings();
			settings.showNow();
			new GalaxyViewer(settings,false);
		}catch(Exception ex){
			ex.printStackTrace();
			System.err.println(ex.toString());
			JOptionPane.showMessageDialog(null, ex.toString());
			System.exit(0);
		}
	}
	
	protected static class Settings extends JPanel{
		private static final long serialVersionUID = 1L;
		protected int playerNr = 0;
		protected String directory = ".";
		protected String gameName = "";
		protected JTextField pNr, gName, dir;
		private File f;
		
		protected String getGameName(){
			return gameName.toUpperCase();
		}
		public Settings() throws Exception{
			f = new File("galaxyviewer.ini");
			if (f.getAbsoluteFile().getParentFile().getName().equals("bin")) f = new File("..","galaxyviewer.ini");
			if (f.exists()){
				BufferedReader in = new BufferedReader(new FileReader(f));
				while(true){
					String s = in.readLine();
					if (s == null) break;
					if (s.contains("=") == false) continue;
					String[] el = s.split("=",-1);
					if (el[0].equalsIgnoreCase("PlayerNr")) playerNr = Integer.parseInt(el[1].trim())-1;
					if (el[0].equalsIgnoreCase("GameName")) gameName = el[1].trim();
					if (el[0].equalsIgnoreCase("GameDir")) directory = el[1].trim();
				}
				in.close();
			}
			pNr = new JTextField(""+(playerNr+1));
			gName = new JTextField(gameName);
			dir = new JTextField(""+directory);
			JButton selectDir = new JButton("...");
			selectDir.addActionListener(new SelectDirectory(gName,dir));
			JPanel p = new JPanel();
			p.setLayout(new GridLayout(3,2));
			p.add(new JLabel("Player nr")); p.add(pNr);
			p.add(new JLabel("Game name")); p.add(gName);
			p.add(new JLabel("Game directory")); p.add(createPanel(null,dir,selectDir));
			setLayout(new BorderLayout());
			add(p, BorderLayout.CENTER);
			gName.setToolTipText("Do not include file extensions");
		}
		public void showNow() throws Exception{
			String[] el = {"Ok","Cancel"};
			int ok = JOptionPane.showOptionDialog(null,this,"Choose settings",JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null,el,el[0]);
			if (ok != 0) System.exit(0);
			update();
		}
		public void update() throws Exception{
			if (isInteger(pNr.getText().trim()) == false) throw new Exception("Specify a player nr");
			playerNr = Integer.parseInt(pNr.getText().trim())-1;
			directory = dir.getText().trim();
			gameName = gName.getText().trim();
			BufferedWriter out = new BufferedWriter(new FileWriter(f));
			out.write("PlayerNr="+(playerNr+1)+"\n");
			out.write("GameName="+gameName+"\n");
			out.write("GameDir="+directory+"\n");
			out.flush(); out.close();
		}
	}
	
	public GalaxyViewer(Settings settings, boolean animatorFrame) throws Exception{
		super("Stars GalaxyViewer");
		this.settings = settings;
		this.animatorFrame = animatorFrame;
		if (settings.gameName.equals("")) throw new Exception("GameName not defined in settings.");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		File dir = new File(settings.directory);
		File map = GalaxyLauncher.getMapFile(dir,settings.getGameName());
		Vector<File> mFiles = new Vector<File>();
		Vector<File> hFiles = new Vector<File>();
		for (File f : dir.listFiles()){
			if (f.getName().toUpperCase().endsWith("MAP")) continue;
			if (f.getName().toUpperCase().endsWith("HST")) continue;
			if (f.getName().toUpperCase().startsWith(settings.getGameName()+".M")) mFiles.addElement(f);
			else if (f.getName().toUpperCase().startsWith(settings.getGameName()+".H")) hFiles.addElement(f);
			else if (f.getName().toUpperCase().startsWith(settings.getGameName()+".XY")) hFiles.addElement(f);
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
		JPanel s = createPanel(0,hw,new JLabel("Search: "),search,names,zoomSlider,colorize,showFleets,new JLabel("Mass-filter:"),massFilter,gotoBigFleets);
		JPanel south = new JPanel(); south.setLayout(new BorderLayout());
		south.add(info,BorderLayout.NORTH);
		south.add(s,BorderLayout.CENTER);
		search.setPreferredSize(new Dimension(100,-1));
		massFilter.setPreferredSize(new Dimension(75,-1));
		cp.add(south,BorderLayout.SOUTH);
		hw.addActionListener(this);
		names.addActionListener(this);
		zoomSlider.addChangeListener(this);
		colorize.addActionListener(this);
		showFleets.addActionListener(this);
		gotoBigFleets.addActionListener(this);

		search.addKeyListener(this);
		massFilter.addKeyListener(this);
		hw.addKeyListener(this);
		names.addKeyListener(this);
		colorize.addKeyListener(this);
		showFleets.addKeyListener(this);
		gotoBigFleets.addKeyListener(this);
		
		setSize(800,600);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-getWidth())/2, (screen.height-getHeight())/2);
		setVisible(animatorFrame == false);
		if (animatorFrame){
			names.setSelected(false);
			showFleets.setSelected(false);
		}
		else{
			setExtendedState(getExtendedState()|JFrame.MAXIMIZED_BOTH);
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					search.requestFocusInWindow();
					universe.zoomToFillGalaxy();
				}
			});
		}
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
			planetNrs.put(new Point(x,y),id);
		}
		in.close();
	}

	protected class Parser extends GameToTestbed{
		public Parser(Vector<File> filesIn) throws Exception{
			String xyFilename = null;
			for (File f : filesIn){
				System.out.println("Parsing "+f.getName());
				List<Block> blocks = new Decryptor().readFile(f.getAbsolutePath());
				System.out.println(blocks.size()+" blocks to parse");
				files.put(f.getAbsolutePath(),blocks);
				if (isProblem(f.getAbsolutePath(), blocks)) throw new Exception("An error occured");
				if (checkXYFile(blocks)) {
	                if (xyFilename != null) throw new Exception("Found multiple XY files");
	                xyFilename = f.getAbsolutePath();
	            }
			}
	        if (xyFilename == null) throw new Exception("No XY file given");
	        if (!xyFilename.toLowerCase().endsWith(".xy")) throw new Exception("Surprising XY filename without .XY: " + xyFilename);
	        filenameBase = xyFilename.substring(0, xyFilename.length() - 3);
	        checkGameIdsAndYearsAndPlayers(files);
	        for (Map.Entry<String, List<Block>> entry : files.entrySet()) {
	            List<Block> blocks = entry.getValue();
	            new FileProcessor().process(blocks);
	        }
	        postProcess();
	        
	        for (PlayerInfo pi :players){
	        	if (settings.playerNr >= 0){ //Not set if called from GalaxyAnimator
	        		PlayerBlock player = players[settings.playerNr].playerBlock;
	        		if (player != null){
						if (pi == null || pi.playerBlock == null) continue;
						if (player.getPlayerRelationsWith(pi.playerBlock.playerNumber) == 1) friends.addElement(pi.playerBlock.playerNumber);
					}
				}
				String s = Util.decodeBytesForStarsString(pi.playerBlock.nameBytes);
				playerNames.put(pi.playerBlock.playerNumber,s.split(" ")[0]);
			}		
			calculateFleetInfo();
		}
		private void calculateFleetInfo(){
			Vector<PlayerInfo> v = new Vector<PlayerInfo>();
			for (PlayerInfo pi : players){
				if (pi != null) v.addElement(pi);
			}
			sortPlayers(v);
			sortedPlayers = v;
			
			//Count total fleets in each x,y-point
			for (PlayerInfo pi : v){
				for (Integer fleetId : pi.fleets.keySet()){
					FleetInfo fi = pi.fleets.get(fleetId);
					PartialFleetBlock f = fi.definitive!=null?fi.definitive:fi.bestPartial;
					Point p = new Point(f.x,f.y);
					Integer i = totalFleetCount.get(p);
					if (i == null) i = 0;
					int thisCount = 0;
					for (int t = 0; t < f.shipCount.length; t++){
						thisCount += f.shipCount[t];
					}
					i += thisCount;
					totalFleetCount.put(p,i);
					if (isEnemy(pi.playerBlock.playerNumber)){ //Info will be merged for each point in space! :-)
						EnemyFleetInfo info = enemyFleetInfo.get(p);
						if (info == null){
							info = new EnemyFleetInfo(p, 0, 0);
							enemyFleetInfo.put(p,info);
						}
						info.shipCount += thisCount;
						info.totalMass += f.mass;
					}
					else{ //Info will be merged for each point in space! :-)
						FriendlyFleetInfo info = friendlyFleetInfo.get(p);
						if (info == null){
							info = new FriendlyFleetInfo(p, 0, 0);
							friendlyFleetInfo.put(p,info);
						}
						info.shipCount += thisCount;
						info.totalMass += f.mass;
					}
				}
			}
		}
	}
	
	
	
	/*
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
    */
	
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
			for (PlayerInfo pi : p.players){
				if (pi == null) continue;
				PlayerBlock pb = pi.playerBlock;
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
	
	public boolean isEnemy(int playerId) {
		return playerId != settings.playerNr && friends.contains(playerId) == false;
	}

	private PartialPlanetBlock getPlanet(int id, int hwForPlayer){
		PlanetInfo pi = p.planets.get(id);
		if (pi != null){
			PartialPlanetBlock ppb = pi.definitive!=null?pi.definitive:pi.best;
			if (ppb != null) return ppb;
		}
		if (hwForPlayer >= 0){
			for (Integer i : p.planets.keySet()){
				pi = p.planets.get(i);
				PartialPlanetBlock ppb = pi.definitive!=null?pi.definitive:pi.best;
				if (ppb != null){
					if (ppb.isHomeworld && ppb.owner == hwForPlayer) return ppb;
				}
			}
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
			setBackground(Color.black);
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
		public void paint(Graphics gr){
			Graphics2D g = (Graphics2D)gr;
			g.setStroke(new BasicStroke(0.1f));
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(Color.black);
			g.fillRect(0, 0, getWidth(), getHeight());
			double virtualWidth = getWidth() * zoom / 100;
			double virtualHeight = getHeight() * zoom / 100;
			int xOffset = (int)(getWidth() - virtualWidth) / 2;
	        int yOffset = (int)(getHeight() - virtualHeight) / 2;
	        
			for (Integer id : planetNames.keySet()){
				String name = planetNames.get(id);
				Point p = planetCoordinates.get(id);
				PartialPlanetBlock planet = getPlanet(id, -2);
				g.setColor(Color.gray);
				int rad = 3;
				if (planet != null && planet.owner >= 0){
					rad = 5;
					Color col = getColor(planet.owner);
					g.setColor(col);
				}
				int x = convertX(p.x);
				int y = convertY(p.y);
				x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
				y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
		        g.fillOval(x-rad/2, y-rad/2, rad, rad);
				if (names.isSelected()){
					g.setFont(g.getFont().deriveFont((float)10));
					g.setColor(Color.gray);
					int stringWidth = g.getFontMetrics().stringWidth(name);
					g.drawString(name, x-stringWidth/2, y+12);
				}
			}
			if (colorize.isSelected() || animatorFrame){
				int yy = 20;
				g.setFont(g.getFont().deriveFont(Font.PLAIN,(float)12));
				for (PlayerInfo pi : GalaxyViewer.this.p.players){
					if (pi == null) continue;
					PlayerBlock pb = pi.playerBlock;
					if (pb == null) continue;
					Color col = getColor(pb.playerNumber);
					g.setColor(col);
					String n = playerNames.get(pb.playerNumber);
					if (n != null){
						n = n.split(" ")[0]; //Due to some bug in decoding, name separator is not found, so just splitting on ' ' for now.
						n += " ("+pi.planetCount+")";
						g.drawString(n,5,yy);
					}
					yy += 14;
				}
			}
			
			if (showFleets.isSelected()){
				g.setFont(g.getFont().deriveFont((float)10));
				
				//Then, paint fleets!
				int minimumMass = 0;
				if (isInteger(massFilter.getText().trim())){
					minimumMass = Integer.parseInt(massFilter.getText().trim());
				}
				for (PlayerInfo pi : sortedPlayers){
					for (Integer fleetId : pi.fleets.keySet()){
						FleetInfo fi = pi.fleets.get(fleetId);
						PartialFleetBlock f = fi.definitive!=null?fi.definitive:fi.bestPartial;
						Point p = new Point(f.x,f.y);
						if (minimumMass > 0){
							if (isEnemy(pi.playerBlock.playerNumber)){
								EnemyFleetInfo i = enemyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
							else{
								FriendlyFleetInfo i = friendlyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
						}						
						Integer i = totalFleetCount.get(p);
						Color col = getColor(pi.playerBlock.playerNumber);
						g.setColor(col);
						int x = convertX(p.x);
						int y = convertY(p.y);
						x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
						y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
						if (planetNrs.get(p) != null){ //Fleet at orbit
							PartialPlanetBlock planet = getPlanet(planetNrs.get(p), -1);
							int rad = 10;				
							if (col.equals(Color.green)) col = Color.white;
					        g.drawOval(x-rad/2, y-rad/2, rad, rad);
					        int stringWidth = g.getFontMetrics().stringWidth(""+i);
							g.drawString(""+i, x-stringWidth/2, y-6);
						}
						else{
							Polygon fleet = getFleetShape(f,x,y);
							if (fleet != null){
								g.fillPolygon(fleet);
								int stringWidth = g.getFontMetrics().stringWidth(""+i);
								g.drawString(""+i, x-stringWidth/2, y-6);
							}
						}
						
					}
				}
			}
		}

		private Polygon getFleetShape(PartialFleetBlock f, int x, int y) {
			int[] xPoints, yPoints;
			double dx = (double)(f.deltaX-127);
			double dy = -(double)(f.deltaY-127);
			//if (f.deltaX != 0 && f.deltaY != 0) System.out.println(f.deltaX+" "+f.deltaY+" "+dx+" "+dy);
			if (dy < 0 && Math.abs(dy) / Math.abs(dx) >= 2.0){ //Up
				xPoints = new int[]{x-4,x,x+4};
				yPoints = new int[]{y+3,y-3,y+3};
			}
			else if (dy > 0 && Math.abs(dy) / Math.abs(dx) >= 2.0){ //Down
				xPoints = new int[]{x-4,x,x+4};
				yPoints = new int[]{y-3,y+3,y-3};
			}
			else if (dx > 0 && Math.abs(dx) / Math.abs(dy) >= 2.0){ //East
				xPoints = new int[]{x-3,x+3,x-3};
				yPoints = new int[]{y-4,y,y+4};
			}
			else if (dx < 0 && Math.abs(dx) / Math.abs(dy) >= 2.0){ //West
				xPoints = new int[]{x+3,x-3,x+3};
				yPoints = new int[]{y-4,y,y+4};
			}
			else if (dx > 0 && dy > 0){ //South-east
				xPoints = new int[]{x-3,x+3,x+3};
				yPoints = new int[]{y+3,y-3,y+3};
			}
			else if (dx > 0 && dy < 0){ //North-east
				xPoints = new int[]{x-3,x+3,x+3};
				yPoints = new int[]{y-3,y-3,y+3};
			}
			else if (dx < 0 && dy < 0){ //North-west
				xPoints = new int[]{x-3,x-3,x+3};
				yPoints = new int[]{y-3,y+3,y-3};
			}
			else if (dx < 0 && dy > 0){ //South-west
				xPoints = new int[]{x-3,x-3,x+3};
				yPoints = new int[]{y-3,y+3,y+3};
			}
			else{ //Stationary, same as east
				xPoints = new int[]{x-3,x+3,x-3};
				yPoints = new int[]{y-4,y,y+4};
			}
			Polygon p = new Polygon(xPoints, yPoints, 3);
			return p;
		}

		private Color getColor(int playerNumber) {
			if (playerNumber < 0) return Color.gray;
			/*
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
			*/
			Color col = colors.get(playerNumber);
			if ((animatorFrame || colorize.isSelected()) && col != null) ; //Ok
			else if (playerNumber == settings.playerNr) col = Color.green;
			else if (friends.contains(playerNumber)) col = Color.yellow;
			else col = Color.red;
			return col;
		}

		private int convertX(int x){
			return x - 1000;
		}
		private int convertY(int y){
			return maxY-y+10;
		}
		
		public void centerOnPoint(Point p) {
			System.out.println("Trying to center on point "+p);
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
			GalaxyViewer.this.zoomSlider.setValue((int)zoom);
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
			else{
				System.out.println("Did not find hw for player "+settings.playerNr);
			}
		}
		else if (e.getSource() == names){
			repaint();
		}
		else if (e.getSource() == colorize){
			repaint();
		}
		else if (e.getSource() == showFleets){
			repaint();
		}
		else if (e.getSource() == gotoBigFleets){
			gotoBigFleet();
		}
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {
		universe.zoom = zoomSlider.getValue();
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
		if (e.getKeyCode() == KeyEvent.VK_PLUS || e.getKeyChar() == '+'){
			zoomSlider.setValue(Math.min(600, zoomSlider.getValue()+10));
			e.consume();
		}
		else if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyChar() == '-'){
			zoomSlider.setValue(Math.max(0, zoomSlider.getValue()-10));
			e.consume();
		}
	}
	@Override
	public void keyPressed(KeyEvent e) {
	}
	@Override
	public void keyReleased(KeyEvent e) {
		if (e.getSource() == massFilter){
			repaint();
		}
		if (e.getSource() == search){
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
	
	/**
	 * You first, then friends, and then enemies
	 */
	private void sortPlayers(Vector<PlayerInfo> v){
		Collections.sort(v,new Comparator<PlayerInfo>(){
			@Override
			public int compare(PlayerInfo o1, PlayerInfo o2) {
				if (settings.playerNr == -1) return 0;
				if (o1.playerBlock.playerNumber == settings.playerNr) return -2;
				if (o2.playerBlock.playerNumber == settings.playerNr) return 2;
				if (o1.playerBlock.getPlayerRelationsWith(settings.playerNr) == 1) return -1;
				if (o2.playerBlock.getPlayerRelationsWith(settings.playerNr) == 1) return 1;
				return 0;
			}
		});
	}
	
	private void gotoBigFleet(){
		if (showFleets.isSelected() == false) info.setText("You must show fleets to use the goto big fleets function");
		else{
			Vector<EnemyFleetInfo> v = new Vector<EnemyFleetInfo>();
			for (Point p : enemyFleetInfo.keySet()){
				EnemyFleetInfo i = enemyFleetInfo.get(p);
				v.addElement(i);
			}
			Collections.sort(v);
			if (v.size() == 0) info.setText("No enemy fleets detected.");
			else{
				int nr = Math.min(10,v.size());
				bigFleetCounter = (bigFleetCounter + 1) % nr;
				EnemyFleetInfo i = v.elementAt(bigFleetCounter);
				if (zoomSlider.getValue() < 200) GalaxyViewer.this.zoomSlider.setValue(200);
				universe.centerOnPoint(i.p);
				DecimalFormat d = new DecimalFormat("###,###");
				String location = "("+i.p.x+","+i.p.y+")";
				Integer planetId = planetNrs.get(i.p);
				if (planetId != null){
					location = planetNames.get(planetId);
				}
				info.setText("("+(bigFleetCounter+1)+"/"+nr+") "+i.shipCount+" enemy ships at "+location+" with a total mass of "+d.format(i.totalMass));
			}
		}
	}
	
	private class FriendlyFleetInfo{
		Point p;
		long shipCount;
		long totalMass;
		private FriendlyFleetInfo(Point p, long shipCount, long totalMass){
			this.p = p;
			this.shipCount = shipCount;
			this.totalMass = totalMass;
		}
	}
	
	private class EnemyFleetInfo implements Comparable<EnemyFleetInfo>{
		Point p;
		long shipCount;
		long totalMass;
		private EnemyFleetInfo(Point p, long shipCount, long totalMass){
			this.p = p;
			this.shipCount = shipCount;
			this.totalMass = totalMass;
		}
		@Override
		public int compareTo(EnemyFleetInfo o) {
			return new Long(o.totalMass).compareTo(totalMass);
		}
	}
	
	protected static boolean isInteger(String s) {
		try{
			Integer.parseInt(s);
			return true;
		}catch(NumberFormatException ex){
			return false;
		}
	}
	
	protected static JPanel createPanel(Component left, Component center, Component right){
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		if (left != null) p.add(left,BorderLayout.WEST);
		p.add(center,BorderLayout.CENTER);
		if (right != null) p.add(right,BorderLayout.EAST);
		return p;
		
	}
	
	protected static class SelectDirectory implements ActionListener{

		private JTextField name;
		private JTextField dir;

		public SelectDirectory(JTextField name, JTextField dir) {
			this.name = name;
			this.dir = dir;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			File current = new File(dir.getText());
			if (current.exists() == false) current = null;
			JFileChooser jfc = new JFileChooser(current);
			jfc.addChoosableFileFilter(new FileNameExtensionFilter("Stars! MAP-file", "MAP"));
			jfc.setAcceptAllFileFilterUsed(false);
			jfc.showOpenDialog(dir);
			File f = jfc.getSelectedFile();
			if (f != null && f.exists()){
				name.setText(f.getName().split("\\.")[0]);
				dir.setText(f.getParentFile().getAbsolutePath());
			}
		}
	}
}
