package org.starsautohost.starsapi.tools;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.starsautohost.starsapi.Util;
import org.starsautohost.starsapi.block.*;
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
	private MapFileData map;
	
	private Settings settings;
	protected Vector<Integer> friends = new Vector<Integer>();
	private int bigFleetCounter = -1;
	private Vector<PlayerInfo> sortedPlayers = new Vector<PlayerInfo>();
	private HashMap<Point,RFleetInfo> enemyFleetInfo = new HashMap<Point,RFleetInfo>();
	private HashMap<Point,RFleetInfo> friendlyFleetInfo = new HashMap<Point,RFleetInfo>();
	private HashMap<Point,Integer> totalFleetCount = new HashMap<Point,Integer>();
	private HashMap<Integer,String> playerNames = new HashMap<Integer,String>();
	private HashMap<Integer,JCheckBox> playerFilterMap = new HashMap<Integer, JCheckBox>();
	private HashMap<Integer,Integer> totalFleetCountByPlayer = new HashMap<Integer, Integer>();
	
	//UI
	protected RPanel universe = new RPanel();
	private JButton hw = new JButton("HW");
	private JTextField search = new JTextField();
	private JCheckBox names = new JCheckBox("Names",false);
	private JSlider zoomSlider = new JSlider(25, 600, 100);
	private JButton help = new JButton("Help");
	private JCheckBox colorize = new JCheckBox("Colorize",false);
	private JCheckBox showFleets = new JCheckBox("Fleets",false);
	private JButton gotoBigFleets = new JButton("Go to big enemy fleets");
	private JButton showFilters = new JButton("Show filters");
	private JTextField massFilter = new JTextField();
	private JCheckBox nubians = new JCheckBox("Nubians",true);
	private JCheckBox battleships = new JCheckBox("Battleships",true);
	private JCheckBox others = new JCheckBox("Others",true);
	private JCheckBox showMt = new JCheckBox("MT",true);
	private JCheckBox showMinefields = new JCheckBox("MF",false);
	private JCheckBox showWormholes = new JCheckBox("WH",false);
	private JButton allPlayers = new JButton("All");
	private JButton noPlayers = new JButton("None");
	
	private HashMap<Integer,Color> colors = new HashMap<Integer, Color>();
	private HashMap<Integer,Color> weakColors = new HashMap<Integer,Color>();
	private JLabel info = new JLabel();
	private boolean animatorFrame, paintVoronoi;
	private Vector<JCheckBox> playerFilters = new Vector<JCheckBox>();
	private JPanel filterPanel = new JPanel();
	private static HashMap<String,TexturePaint> bufferedPatterns = new HashMap<String,TexturePaint>();
	private static BufferedImage wormholeImage = getWormholeImage();
	private int year = -1;
	
	public static void main(String[] args) throws Exception{
		try{
			Settings settings = new Settings();
			settings.showNow();
			new GalaxyViewer(settings,false,false);
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
			return gameName;
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
	
	public GalaxyViewer(Settings settings, boolean animatorFrame, boolean paintVoronoi) throws Exception{
		super("Stars GalaxyViewer");
		PlayerBlock.ignoreParseErrors = true;
		this.settings = settings;
		this.animatorFrame = animatorFrame;
		this.paintVoronoi = paintVoronoi;
		if (settings.gameName.equals("")) throw new Exception("GameName not defined in settings.");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		filterPanel.setLayout(new BorderLayout());
		File dir = new File(settings.directory);
		File map = GalaxyLauncher.getMapFile(dir,settings.getGameName());
		Vector<File> mFiles = new Vector<File>();
		Vector<File> hFiles = new Vector<File>();
		for (File f : dir.listFiles()){
			String filename = f.getName();
			if (hasExtension(filename, "map")) continue;
			if (hasExtension(filename, "hst")) continue;
			if (fileNameStarts(f.getName(), settings.getGameName()+".m")) mFiles.addElement(f);
			else if (fileNameStarts(f.getName(), settings.getGameName()+".h")) hFiles.addElement(f);
			else if (fileNameStarts(f.getName(), settings.getGameName()+".xy")) hFiles.addElement(f);
		}
		if (mFiles.size() == 0) throw new Exception("No M-files found matching game name.");
		if (hFiles.size() == 0){
			System.out.println("Warning: No H-files found matching game name.");
			//throw new Exception("No H-files found matching game name.");
		}
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
		
		JPanel s = createPanel(0,hw,new JLabel("Search: "),search,names,zoomSlider,colorize,showFleets,showFilters,showMt,showMinefields,showWormholes,gotoBigFleets);
		JPanel south = new JPanel(); south.setLayout(new BorderLayout());
		south.add(info,BorderLayout.NORTH);
		south.add(filterPanel,BorderLayout.CENTER);
		south.add(s,BorderLayout.SOUTH);
		filterPanel.setVisible(false);
		filterPanel.add(createPanel(0, new JLabel("Mass-filter: "),massFilter,new JLabel("  "),nubians,battleships,others),BorderLayout.CENTER);
		search.setPreferredSize(new Dimension(100,-1));
		massFilter.setPreferredSize(new Dimension(75,-1));
		cp.add(south,BorderLayout.SOUTH);
		hw.addActionListener(this);
		names.addActionListener(this);
		zoomSlider.addChangeListener(this);
		colorize.addActionListener(this);
		showFleets.addActionListener(this);
		gotoBigFleets.addActionListener(this);
		showFilters.addActionListener(this);
		nubians.addActionListener(this);
		battleships.addActionListener(this);
		others.addActionListener(this);
		showMt.addActionListener(this);
		showMinefields.addActionListener(this);
		showWormholes.addActionListener(this);
		allPlayers.addActionListener(this);
		noPlayers.addActionListener(this);
		
		search.addKeyListener(this);
		massFilter.addKeyListener(this);
		hw.addKeyListener(this);
		names.addKeyListener(this);
		zoomSlider.addKeyListener(this);
		colorize.addKeyListener(this);
		showFleets.addKeyListener(this);
		gotoBigFleets.addKeyListener(this);
		showFilters.addKeyListener(this);
		nubians.addKeyListener(this);
		battleships.addKeyListener(this);
		others.addKeyListener(this);
		showMt.addKeyListener(this);
		showMinefields.addKeyListener(this);
		showWormholes.addKeyListener(this);
		allPlayers.addKeyListener(this);
		noPlayers.addKeyListener(this);
		
		setSize(800,600);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screen.width-getWidth())/2, (screen.height-getHeight())/2);
		setVisible(animatorFrame == false);
		if (animatorFrame){
			names.setSelected(false);
			showFleets.setSelected(false);
			showMt.setSelected(false);
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
	
	public static class MapFileData{
		public HashMap<Integer, String> planetNames = new HashMap<Integer, String>();
		public HashMap<Integer, Point> planetCoordinates = new HashMap<Integer, Point>();
		public HashMap<Point, Integer> planetNrs = new HashMap<Point, Integer>();
		public int maxX = 0, maxY = 0;
	}
	
	private void parseMapFile(File f) throws Exception{
		map = parseMapFileData(f);
	}
	
	public static MapFileData parseMapFileData(File f) throws Exception{
		MapFileData map = new MapFileData();
		BufferedReader in = new BufferedReader(new FileReader(f));
		in.readLine();
		while(true){
			String s = in.readLine();
			if (s == null) break;
			if (s.trim().equals("")) continue;
			String[] el = s.split("\t");
			int id = Integer.parseInt(el[0])-1;
			map.planetNames.put(id,el[3]);
			int x = Integer.parseInt(el[1]);
			int y = Integer.parseInt(el[2]);
			if (x > map.maxX) map.maxX = x;
			if (y > map.maxY) map.maxY = y;
			map.planetCoordinates.put(id,new Point(x,y));
			map.planetNrs.put(new Point(x,y),id);
		}
		in.close();
		return map;
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
	        if (xyFilename == null && filesIn.size() > 0){
	        	File first = filesIn.firstElement();
	        	File ff = new File(first.getParentFile().getParentFile(),first.getName().split("\\.")[0]+".xy");
	        	if (ff.exists()){
	        		System.out.println("Parsing xy-file in parent directory");
	        		List<Block> blocks = new Decryptor().readFile(ff.getAbsolutePath());
					System.out.println(blocks.size()+" blocks to parse");
					files.put(ff.getAbsolutePath(),blocks);
					if (isProblem(ff.getAbsolutePath(), blocks)) throw new Exception("An error occured");
					if (checkXYFile(blocks) == false) throw new Exception("Internal error reading xy-file.");
		            xyFilename = ff.getAbsolutePath();
	        	}
	        	else{
	        		throw new Exception("No XY file given (ant not in parent directory)"+"\n"+"Tried directory: "+ff.getParentFile().getAbsolutePath()+"\n"+ff.getName());
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
	        
	        for (int t = 0; t < players.length; t++){
	        	PlayerInfo pi = players[t];
	        	if (pi != null && pi.playerBlock != null){
		        	if (settings.playerNr >= 0){ //Not set if called from GalaxyAnimator
		        		PlayerBlock player = players[settings.playerNr].playerBlock;
		        		if (player != null){
							if (player.getPlayerRelationsWith(pi.playerBlock.playerNumber) == 1) friends.addElement(pi.playerBlock.playerNumber);
						}
		        	}
	        		String s = Util.decodeBytesForStarsString(pi.playerBlock.nameBytes);
	        		s = s.split(" ")[0];
	        		playerNames.put(pi.playerBlock.playerNumber,s);
	        		JCheckBox pf = new JCheckBox(s,true);
	        		pf.addKeyListener(GalaxyViewer.this);
	        		pf.addActionListener(GalaxyViewer.this);
	        		playerFilterMap.put(pi.playerBlock.playerNumber,pf);
	        		playerFilters.addElement(pf);
	           	}
			}		
			calculateFleetInfo();
			int size = playerFilters.size();
			if (playerFilters.size() > 2){
        		size += 2;
        	}
			Component[] c = new Component[size];
			for (int t = 0; t < playerFilters.size(); t++) c[t] = playerFilters.elementAt(t);
			if (allPlayers != null) c[c.length-2] = allPlayers;
			if (noPlayers != null) c[c.length-1] = noPlayers;
			filterPanel.add(createPanel(0, c),BorderLayout.NORTH);
		}
		private void calculateFleetInfo(){
			totalFleetCount.clear();
			enemyFleetInfo.clear();
			friendlyFleetInfo.clear();
			totalFleetCountByPlayer.clear();
			Vector<PlayerInfo> v = new Vector<PlayerInfo>();
			for (PlayerInfo pi : players){
				if (pi != null) v.addElement(pi);
			}
			sortPlayers(v);
			sortedPlayers = v;
			
			//Count total fleets in each x,y-point
			for (PlayerInfo pi : v){
				JCheckBox playerFilter = playerFilterMap.get(pi.playerBlock.playerNumber);
				if (playerFilter != null && playerFilter.isSelected() == false) continue;
				for (Integer fleetId : pi.fleets.keySet()){
					FleetInfo fi = pi.fleets.get(fleetId);
					PartialFleetBlock f = fi.definitive!=null?fi.definitive:fi.bestPartial;
					Point p = new Point(f.x,f.y);
					int shipsAdded = 0;
					if (isEnemy(pi.playerBlock.playerNumber)){ //Info will be merged for each point in space! :-)
						RFleetInfo info = enemyFleetInfo.get(p);
						if (info == null){
							info = new RFleetInfo(p, 0, 0);
							enemyFleetInfo.put(p,info);
						}
						shipsAdded = info.add(pi,f);
					}
					else{ //Info will be merged for each point in space! :-)
						RFleetInfo info = friendlyFleetInfo.get(p);
						if (info == null){
							info = new RFleetInfo(p, 0, 0);
							friendlyFleetInfo.put(p,info);
						}
						shipsAdded = info.add(pi,f);
					}
					Integer i = totalFleetCount.get(p);
					if (i == null) i = 0;
					totalFleetCount.put(p,i+shipsAdded);
					i = totalFleetCountByPlayer.get(pi.playerBlock.playerNumber);
					if (i == null) i = 0;
					totalFleetCountByPlayer.put(pi.playerBlock.playerNumber, i+shipsAdded);
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
				Color col = distinctColors.get(t);
				colors.put(t,col);
				col = new Color(col.getRed(),col.getGreen(),col.getBlue(),128);
				weakColors.put(t,col);
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
	        
	        if (showMinefields.isSelected()){ //First, paint minefields (low priority
	        	//BufferedImage mines = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
	        	for (Integer id : p.objects.keySet()){
					ObjectBlock o = p.objects.get(id);
					if (o.isMinefield()){
						int x = convertX(o.x);
						int y = convertY(o.y);
						double r = Math.sqrt(o.mineCount);
						x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
						y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
						int rx = (int)(r*zoom/100.0);
						int ry = (int)(r*zoom/100.0);
						
						TexturePaint tp = null;
						
						if (o.owner == settings.playerNr){
							if (o.isMinefieldDetonating()){
								tp = getMinefieldPattern(0, new Color(255,0,255));
							}
							else{
								tp = getMinefieldPattern(o.getMinefieldType(), Color.white);
							}
						}
						else{
							if (friends.contains(o.owner)){
								if (o.isMinefieldDetonating()){
									tp = getMinefieldPattern(0, new Color(255,0,255));
								}
								else{
									tp = getMinefieldPattern(o.getMinefieldType(), Color.yellow);
								}
							}
							else{
								if (o.isMinefieldDetonating()){
									tp = getMinefieldPattern(0, new Color(255,0,155));
								}
								else{
									tp = getMinefieldPattern(o.getMinefieldType(), Color.red);
								}
							}
							
						}
						if (tp != null){
							g.setPaint(tp);
							g.fillOval(x-rx, y-ry, rx*2, ry*2);
							g.setPaint(null);
						}
					}
				}
	        }
	        
	        if (paintVoronoi){ //Show Voronoi colors! :-D
	        	g.setStroke(new BasicStroke(1f));
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				
				int totalPoints = 0;
	        	Vector[] points = new Vector[16];
	        	for (int t = 0; t < points.length; t++) points[t] = new Vector<Point>();
	        	int minx = Integer.MAX_VALUE;
	        	int maxx = 0;
	        	int miny = Integer.MAX_VALUE;
	        	int maxy = 0;
	        	for (Integer id : map.planetNames.keySet()){
					String name = map.planetNames.get(id);
					Point p = map.planetCoordinates.get(id);
					PartialPlanetBlock planet = getPlanet(id, -2);
					if (planet == null || planet.owner < 0) continue;
					Color col = getColor(planet.owner);
					col = new Color(col.getRed(),col.getGreen(),col.getBlue(),128);
					int x = convertX(p.x);
					int y = convertY(p.y);
					x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
					y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
					if (x > maxx) maxx = x;
					if (x < minx) minx = x;
					if (y > maxy) maxy = y;
					if (y < miny) miny = y;
			        points[planet.owner].addElement(new Point(x,y));
			        totalPoints++;
	        	}
	        	System.out.println("Calculating Voronoi for "+totalPoints+" planets.");
	        	long time = System.currentTimeMillis();
	        	int maxDistance = (int)(60.0 * zoom / 100.0);
	        	BufferedImage[] map = new BufferedImage[16];
	        	Graphics2D[] gs = new Graphics2D[16];
	        	for (int t = 0; t < map.length; t++){
	        		map[t] = new BufferedImage(getWidth(),getHeight(),BufferedImage.TYPE_BYTE_BINARY);
	        		gs[t] = map[t].createGraphics();
	        		gs[t].setColor(Color.white);
	        	}
	        	for (int owner = 0; owner < points.length; owner++){
	        		for (int t = 0; t < points[owner].size(); t++){
	        			Point p = (Point)points[owner].elementAt(t);
	        			gs[owner].fillOval(p.x-maxDistance, p.y-maxDistance, maxDistance*2, maxDistance*2);
	        		}
	        	}
	        	System.out.println("Precalculations: "+(System.currentTimeMillis()-time));
	        	
	        	//HashMap<int,Vector<>>>
	        	int count0 = 0, count1 = 0, countx = 0;
				for (int x = 0; x < getWidth(); x++) {
					int maxX = x + maxDistance;
					int minX = x - maxDistance;
					if (maxX < minx) continue;
					if (minX > maxx) continue;
					for (int y = 0; y < getHeight(); y++) {
						int maxY = y + maxDistance;
						int minY = y - maxDistance;
						if (maxY < miny) continue;
						if (minY > maxy) continue;
						
						int count = 0;
						Color col = null;
						boolean[] b = new boolean[16];
						for (int t = 0; t < 16; t++){
							int i = map[t].getRGB(x,y);
							if (i == -1){
								b[t] = true;
								count++;
								col = GalaxyViewer.this.weakColors.get(t);
							}
						}
						if (count == 0){
							count0++;
							continue;
						}
						else if (count > 1){
							Double oldDistance = null;
							for (int owner = 0; owner < points.length; owner++){
								if (b[owner] == false) continue; //Not one of this owners planets are in range of current point
								for (int t = 0; t < points[owner].size(); t++) {
									Point p = (Point)points[owner].elementAt(t);
									Color c = weakColors.get(owner);
									//if (p.x > maxX || p.x < minX) continue;
									//if (p.y > maxY || p.y < minY) continue;
									double distance = Voronoi.distance(p.x, x, p.y, y);
									if (distance < maxDistance){
										if (oldDistance == null || distance < oldDistance){
											col = c;
											oldDistance = distance;
										}
									}
								}
							}
							countx++;
						}
						else{
							count1++;
						}
						if (col != null){
							g.setColor(col);
							g.drawLine(x,y,x,y);
						}
					}
				}
				g.setStroke(new BasicStroke(0.1f));
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				System.out.println("Voronoi time: "+(System.currentTimeMillis()-time)); //16,5, 18,7
				System.out.println("Count0:"+count0);
				System.out.println("Count1:"+count1);
				System.out.println("Countx:"+countx);
	        }
	        
			for (Integer id : map.planetNames.keySet()){
				String name = map.planetNames.get(id);
				Point p = map.planetCoordinates.get(id);
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
			else if (showFleets.isSelected()){ //Paint total fleet count (top left)
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
						Integer i = totalFleetCountByPlayer.get(pi.playerBlock.playerNumber);
						if (i == null) i = 0;
						n = n.split(" ")[0]; //Due to some bug in decoding, name separator is not found, so just splitting on ' ' for now.
						n += " ("+i+")";
						g.drawString(n,5,yy);
					}
					yy += 14;
				}
			}
			
			//Then, paint fleets!
			if (showFleets.isSelected()){
				g.setFont(g.getFont().deriveFont((float)10));
				
				int minimumMass = 0;
				if (isInteger(massFilter.getText().trim())){
					minimumMass = Integer.parseInt(massFilter.getText().trim());
				}
				//First pass, paint fleet graphics and small fleet numbers
				HashMap<Point,Color> painted = new HashMap<Point,Color>();
				for (PlayerInfo pi : sortedPlayers){
					JCheckBox playerFilter = playerFilterMap.get(pi.playerBlock.playerNumber);
					if (playerFilter != null && playerFilter.isSelected() == false) continue;
					for (Integer fleetId : pi.fleets.keySet()){
						FleetInfo fi = pi.fleets.get(fleetId);
						PartialFleetBlock f = fi.definitive!=null?fi.definitive:fi.bestPartial;
						Point p = new Point(f.x,f.y);
						if (minimumMass > 0){
							if (isEnemy(pi.playerBlock.playerNumber)){
								RFleetInfo i = enemyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
							else{
								RFleetInfo i = friendlyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
						}
						Integer i = totalFleetCount.get(p);
						if (i == null || i == 0) continue;
						Color col = getColor(pi.playerBlock.playerNumber);
						g.setColor(col);
						int x = convertX(p.x);
						int y = convertY(p.y);
						x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
						y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
						if (map.planetNrs.get(p) != null){ //Fleet at orbit
							//PartialPlanetBlock planet = getPlanet(map.planetNrs.get(p), -1);
							if (painted.get(p) == null || painted.get(p).equals(g.getColor()) == false){
								int rad = 10;				
								if (col.equals(Color.green)) col = Color.white;
						        g.drawOval(x-rad/2, y-rad/2, rad, rad);
						        if (i < 250){
						        	int stringWidth = g.getFontMetrics().stringWidth(""+i);
						        	makeTransparent(g);
						        	g.drawString(""+i, x-stringWidth/2, y-6);
						        	makeOpaque(g);
						        	painted.put(p,g.getColor());
						        }
							}
						}
						else{
							Polygon fleet = getFleetShape(f,x,y);
							if (fleet != null){
								makeTransparent(g);
								g.fillPolygon(fleet);
								makeOpaque(g);
								if (i < 250){
									if (painted.get(p) == null || painted.get(p).equals(g.getColor()) == false){
										int stringWidth = g.getFontMetrics().stringWidth(""+i);
										int xx = x-stringWidth/2;
										int yy = y-6;
										makeTransparent(g);
										g.drawString(""+i, xx, yy);
										makeOpaque(g);
										painted.put(p,g.getColor());
									}
								}
							}
						}
					}		
				}
				//Second pass, paint large fleet numbers
				painted.clear();
				for (PlayerInfo pi : sortedPlayers){
					JCheckBox playerFilter = playerFilterMap.get(pi.playerBlock.playerNumber);
					if (playerFilter != null && playerFilter.isSelected() == false) continue;
					for (Integer fleetId : pi.fleets.keySet()){
						FleetInfo fi = pi.fleets.get(fleetId);
						PartialFleetBlock f = fi.definitive!=null?fi.definitive:fi.bestPartial;
						Point p = new Point(f.x,f.y);
						if (minimumMass > 0){
							if (isEnemy(pi.playerBlock.playerNumber)){
								RFleetInfo i = enemyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
							else{
								RFleetInfo i = friendlyFleetInfo.get(p);
								if (i != null && i.totalMass < minimumMass) continue;
							}
						}
						Integer i = totalFleetCount.get(p);
						if (i == null || i == 0) continue;
						if (i < 250) continue;
						Color col = getColor(pi.playerBlock.playerNumber);
						g.setColor(col);
						int x = convertX(p.x);
						int y = convertY(p.y);
						x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
						y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
						
						if (i >= 1000) g.setFont(g.getFont().deriveFont(Font.BOLD,(float)14));
						else if (i >= 500) g.setFont(g.getFont().deriveFont(Font.BOLD,(float)12));
						else g.setFont(g.getFont().deriveFont(Font.BOLD, (float)10));
						
						int stringWidth = g.getFontMetrics().stringWidth(""+i);
						int xx = x-stringWidth/2;
						int yy = y-6;
						
						Color old = g.getColor();
						if (painted.get(p) == null){
							int rr = 100, gg = 100, bb = 100;
							if (i >= 1000) g.setColor(new Color(rr,gg,bb,200));
							else if (i >= 500) g.setColor(new Color(rr,gg,bb,110));
							else g.setColor(new Color(rr,gg,bb,50));
							int fs = g.getFont().getSize();
							g.fillRect(xx-3, yy-fs, stringWidth+6, fs+2);
							g.setColor(old);
							painted.put(p,old);
							g.drawString(""+i, xx, yy);
							g.setFont(g.getFont().deriveFont(Font.PLAIN,(float)10)); //Reset font
						}
					}
				}
			}
			
			for (Integer id : p.objects.keySet()){
				ObjectBlock o = p.objects.get(id);
				if (o.isWormhole() && showWormholes.isSelected()){
					int x = convertX2(o.x,xOffset);
					int y = convertY2(o.y,yOffset);
					g.drawImage(wormholeImage, x - 4, y - 4, null);
					g.drawString(""+o.wormholeId, x-2, y-6);
					for (Integer id2 : p.objects.keySet()){
						if (id == id2) continue;
						ObjectBlock b = p.objects.get(id2);
						if (b.isWormhole() && o.targetId == b.wormholeId){
							int xx = convertX2(b.x,xOffset);
							int yy = convertY2(b.y,yOffset);
							g.setColor(new Color(255,0,255));
							g.drawLine(x, y, xx, yy);
						}
					}
				}
				if (o.isMT() && showMt.isSelected()){ //Paint MT
					//System.out.println(o.toString());
					g.setFont(g.getFont().deriveFont((float)10));
					Color col = new Color(155,155,255);
					g.setColor(col);
					int x = convertX(o.x);
					int y = convertY(o.y);
					/*
					double ddx = (double)o.x / (double)o.xDest;
					double ddy = (double)o.y / (double)o.yDest;
					double dx = ddx / (ddx+ddy);
					double dy = ddy / (ddx+ddy);
					int xd = (int)(100*dx);
					int yd = (int)(100*dy);
					*/
					int xd = convertX(o.xDest);
					int yd = convertY(o.yDest);
					x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
					y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
					xd = (int)(xOffset + xd*zoom/100.0 - mariginX*zoom/100.0);
					yd = (int)(yOffset + yd*zoom/100.0 - mariginY*zoom/100.0);
					double dx = o.getDeltaX();
					double dy = o.getDeltaY();
					Polygon mt = getFleetShape(dx,dy,x,y);
					if (mt != null){
						System.out.println(dx+" "+dy);
						System.out.println("ABC: "+o.x+","+o.y+" -> "+o.xDest+","+o.yDest+"    "+x+","+y+" -> "+xd+","+yd);
						g.fillPolygon(mt);
						g.setStroke(new BasicStroke(0.1f));
						g.drawLine(x,y,xd,yd);
						//Do NOT comment in the following unless it is determined that part name may be revealed.
						//String type = o.getMTPartName();
						//int stringWidth = g.getFontMetrics().stringWidth(type);
						//makeTransparent(g);
						//g.drawString(type, x-stringWidth/2, y-10);
						//makeOpaque(g);
					}
				}
			}
		}

		private void makeOpaque(Graphics2D g) {
			Color old = g.getColor();
			Color c = new Color(old.getRed(),old.getGreen(),old.getBlue());
			g.setColor(c);
		}

		private void makeTransparent(Graphics2D g) {
			Color old = g.getColor();
			Color c = new Color(old.getRed(),old.getGreen(),old.getBlue(),128);
			g.setColor(c);
		}

		private Polygon getFleetShape(PartialFleetBlock f, int x, int y) {
			double dx = (double)(f.deltaX-127);
			double dy = -(double)(f.deltaY-127);
			return getFleetShape(dx, dy, x, y);
		}
		private Polygon getFleetShape(double dx, double dy, int x, int y){
			int[] xPoints, yPoints;
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
			return map.maxY-y+10;
		}
		
		private int convertX2(int x, int xOffset){
			x = convertX(x);
			x = (int)(xOffset + x*zoom/100.0 - mariginX*zoom/100.0);
			return x;
		}
		
		private int convertY2(int y, int yOffset){
			y = convertY(y);
			y = (int)(yOffset + y*zoom/100.0 - mariginY*zoom/100.0);
			return y;
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
			double ySize = map.maxY-1000;
			zoom = 100.0 * getHeight() / ySize;
			GalaxyViewer.this.zoomSlider.setValue((int)zoom);
			Point center = new Point((map.maxX-1000)/2+1000,(map.maxY-1000)/2+1000);
			centerOnPoint(center);
			repaint();
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == hw){
			PartialPlanetBlock planet = getPlanet(-1, settings.playerNr);
			if (planet != null){
				Point p = map.planetCoordinates.get(planet.planetNumber);
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
		else if (e.getSource() == showMt){
			repaint();
		}
		else if (e.getSource() == showMinefields){
			repaint();
		}
		else if (e.getSource() == showWormholes){
			repaint();
		}
		else if (e.getSource() == gotoBigFleets){
			gotoBigFleet();
		}
		else if (e.getSource() == showFilters){
			filterPanel.setVisible(!filterPanel.isVisible());
			showFilters.setText(filterPanel.isVisible()?"Hide filters":"Show filters");
		}
		else if (e.getSource() == nubians || e.getSource() == battleships || e.getSource() == others){
			p.calculateFleetInfo();
			repaint();
		}
		else if (e.getSource() == allPlayers || e.getSource() == noPlayers){
			for (JCheckBox b : playerFilters){
				b.setSelected(e.getSource() == allPlayers);
			}
			p.calculateFleetInfo();
			repaint();
		}
		else{
			for (JCheckBox cb : playerFilters){
				if (e.getSource() == cb){
					p.calculateFleetInfo();
					repaint();
					break;
				}
			}
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
				for (Integer id : map.planetNames.keySet()){
					String name = map.planetNames.get(id).toLowerCase();
					if (name.startsWith(s)){
						universe.centerOnPoint(map.planetCoordinates.get(id));
						return;
					}
				}
				//Second pass: Contains
				for (Integer id : map.planetNames.keySet()){
					String name = map.planetNames.get(id).toLowerCase();
					if (name.contains(s)){
						universe.centerOnPoint(map.planetCoordinates.get(id));
						return;
					}
				}
			}
		}
	}
	
	/**
	 * 12 first taken for warcraft answer at http://gamedev.stackexchange.com/questions/46463/how-can-i-find-an-optimum-set-of-colors-for-10-players
	 */
	public static List<Color> pickColors(int num){
		List<Color> v = new ArrayList<Color>();
		v.add(new Color(255,3,3));
		v.add(new Color(0,66,255));
		v.add(new Color(28,230,185));
		v.add(new Color(84,0,129));
		v.add(new Color(255,252,1));
		v.add(new Color(254,138,14));
		v.add(new Color(32,192,0));
		v.add(new Color(229,91,176));
		v.add(new Color(149,150,151));
		v.add(new Color(126,191,241));
		v.add(new Color(16,98,70));
		v.add(new Color(78,42,4));
		v.add(new Color(255,255,255));
		v.add(new Color(187,115,20));
		v.add(new Color(255,0,0));
		v.add(new Color(255,0,0));
		return v;
	}
	
	/* Old way
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
	*/
	
	
	/**
	 * Check if file has the proper extension in a non case-sensitive way
	 * @param fileName
	 * @param extension
	 * @return
	 */
	public static boolean hasExtension(String fileName, String extension){
		if(fileName.toUpperCase().endsWith(extension.toUpperCase()))
			return true;
		
		return false;
	}
	
	/**
	 * Check if file has the given name in a non case-sensitive way
	 * @param fileName
	 * @param testName
	 * @return
	 */
	public static boolean fileNameStarts(String fileName, String testName){
		if(fileName.toLowerCase().startsWith(testName.toLowerCase()))
			return true;
		
		return false;
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
			Vector<RFleetInfo> v = new Vector<RFleetInfo>();
			for (Point p : enemyFleetInfo.keySet()){
				RFleetInfo i = enemyFleetInfo.get(p);
				v.addElement(i);
			}
			Collections.sort(v);
			if (v.size() == 0) info.setText("No enemy fleets detected.");
			else{
				int nr = Math.min(10,v.size());
				bigFleetCounter = (bigFleetCounter + 1) % nr;
				RFleetInfo i = v.elementAt(bigFleetCounter);
				if (zoomSlider.getValue() < 200) GalaxyViewer.this.zoomSlider.setValue(200);
				universe.centerOnPoint(i.p);
				DecimalFormat d = new DecimalFormat("###,###");
				String location = "("+i.p.x+","+i.p.y+")";
				Integer planetId = map.planetNrs.get(i.p);
				if (planetId != null){
					location = map.planetNames.get(planetId);
				}
				info.setText("("+(bigFleetCounter+1)+"/"+nr+") "+i.shipCount+" enemy ships at "+location+" with a total mass of "+d.format(i.totalMass));
			}
		}
	}
	
	private class RFleetInfo implements Comparable<RFleetInfo>{
		Point p;
		long shipCount;
		long totalMass;
		Vector<PartialFleetBlock> fleets = new Vector<PartialFleetBlock>();
		private RFleetInfo(Point p, long shipCount, long totalMass){
			this.p = p;
			this.shipCount = shipCount;
			this.totalMass = totalMass;
		}
		public int add(PlayerInfo pi, PartialFleetBlock f) {
			fleets.add(f);
			int thisCount = 0;
			for (int t = 0; t < f.shipCount.length; t++){
				if (f.shipCount[t] == 0) continue;
				if (pi.shipDesignBlocks[t].hullId == 29){ //28 seems to be sml
					if (nubians.isSelected() == false) continue;
				}
				else if (pi.shipDesignBlocks[t].hullId == 9){
					if (battleships.isSelected() == false) continue;
				}
				else{
					if (others.isSelected() == false) continue;
				}
				thisCount += f.shipCount[t];
			}
			shipCount += thisCount;
			totalMass += f.mass;
			return thisCount;
		}
		@Override
		public int compareTo(RFleetInfo o) {
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
	
	protected TexturePaint getMinefieldPattern(int type, Color col){
		String key = type+"_"+col.getRGB();
		TexturePaint tp = bufferedPatterns.get(key);
		if (tp != null) return tp;
		BufferedImage img = new BufferedImage(8,8,BufferedImage.TYPE_INT_ARGB);
		int rgb = col.getRGB();
		if (type == 0){
			img.setRGB(4, 0, rgb);
			img.setRGB(2, 2, rgb);
			img.setRGB(6, 2, rgb);
			img.setRGB(0, 4, rgb);
			img.setRGB(2, 6, rgb);
			img.setRGB(6, 6, rgb);
		}
		else if (type == 1){ //Heavy
			img.setRGB(0, 0, rgb);
			img.setRGB(1, 2, rgb);
			img.setRGB(3, 2, rgb);
			img.setRGB(5, 4, rgb);
			img.setRGB(4, 6, rgb);
			img.setRGB(2, 7, rgb);
		}
		else if (type == 2){ //Bumpy
			img.setRGB(0, 0, rgb);
			img.setRGB(3, 1, rgb);
			img.setRGB(6, 2, rgb);
			img.setRGB(1, 4, rgb);
			img.setRGB(2, 5, rgb);
			img.setRGB(5, 7, rgb);
		}
		tp = new TexturePaint(img, new Rectangle(0, 0, 8, 8));
		bufferedPatterns.put(key, tp);
		return tp;
	}
	
	private static BufferedImage getWormholeImage(){
		BufferedImage i = new BufferedImage(9, 9, BufferedImage.TYPE_INT_ARGB);
		int[][] image = {
				{0,1,1,1,0,2,0,0,0},
				{0,0,0,1,1,0,2,0,1},
				{0,2,2,1,1,1,2,0,1},
				{2,0,1,0,3,0,1,0,1},
				{0,1,1,3,3,3,1,1,0},
				{1,0,1,0,3,0,1,0,2},
				{1,0,2,1,1,1,2,2,0},
				{1,0,2,0,1,0,0,0,0},
				{0,0,0,2,0,1,1,1,0},
		};
		for (int y = 0; y < 9; y++){
			for (int x = 0; x < 9; x++){
				switch(image[y][x]){
				case 1:
					i.setRGB(x, y, new Color(130,0,130).getRGB());
					break;
				case 2:
					i.setRGB(x, y, new Color(255,0,255).getRGB());
					break;
				case 3:
					i.setRGB(x, y, Color.black.getRGB());
					break;
				}
			}
		}
		return i;
	}

	/**
	 * Used in galaxyanimator when only planet info is relevant
	 * Will free up memory.
	 */
	public void removeSomeInfo() {
		enemyFleetInfo.clear();
		friendlyFleetInfo.clear();
		totalFleetCount.clear();
		totalFleetCountByPlayer.clear();
		p.files.clear();
	}

	public int getYear() {
		return 2400+p.getGameTurn();
	}
}
