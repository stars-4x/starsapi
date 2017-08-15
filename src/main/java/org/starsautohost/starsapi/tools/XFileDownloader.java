package org.starsautohost.starsapi.tools;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class XFileDownloader {

	String gamePage = "";
	String downloadDir = "";
	String[] passwords = new String[16];
	
	public static void main(String[] args) throws Exception{
		XFileDownloader xfd = new XFileDownloader();
		xfd.init();
		xfd.download();
	}

	private String[] getDates(int year) throws Exception{
		String[] dates = new String[16];
		File dir = new File(downloadDir,""+year);
		File f = new File(dir,"xfiles.dates");
		if (f.exists()){
			BufferedReader in = new BufferedReader(new FileReader(f));
			while (true){
				String s = in.readLine();
				if (s == null) break;
				if (s.trim().equals("")) continue;
				String[] el = s.split("=",-1);
				dates[Integer.parseInt(el[0])-1] = el[1];
			}
			in.close();
		}
		return dates;
	}
	
	private void setDates(int year, String[] dates) throws Exception{
		if (new File(downloadDir).exists() == false) throw new Exception("Download dir did not exist");
		File dir = new File(downloadDir,""+year);
		if (dir.exists() == false) dir.mkdir();
		File f = new File(dir,"xfiles.dates");
		BufferedWriter out = new BufferedWriter(new FileWriter(f));
		for (int t = 0; t < dates.length; t++){
			out.write((t+1)+"="+(dates[t]!=null?dates[t]:""));
			out.write("\r\n");
		}
		out.flush();
		out.close();
	}
	
	private void download() throws Exception{
		System.out.println("Reading "+gamePage);
		URL url = new URL(gamePage);
		HttpURLConnection c = (HttpURLConnection)url.openConnection();
		c.setDoInput(true); c.setDoOutput(false);
		StringBuffer sb = new StringBuffer();
		BufferedReader in = new BufferedReader(new InputStreamReader(c.getInputStream()));
		while(true){
			String s = in.readLine();
			if (s == null) break;
			sb.append(s); sb.append("\n");
		}
		in.close(); c.disconnect();
		String s = sb.toString();
		String yearKey = "Playing&nbsp;Year: ";
		int yearIndex = s.indexOf(yearKey)+yearKey.length();
		int year = Integer.parseInt(s.substring(yearIndex).split("<")[0]);
		String[] dates = getDates(year);
		System.out.println("Parsing "+year);
		while (true){
			String key = "<!-- Player Info. -->";
			int i = s.indexOf(key);
			if (i == -1) break;
			s = s.substring(i+key.length());
			i = s.indexOf("Player");
			if (i == -1) throw new Exception("Error loading player info");
			int nr = Integer.parseInt(s.substring(i+6).split("<")[0]);
			i = s.indexOf("<small>");
			String date = s.substring(i+7).split("<")[0];
			if (date.equals("<TIME_UL>")) date = "";
			if (date.equals("") == false){
				if (dates[nr-1].equals(date)){
					System.out.println("No changes for player "+nr);
				}
				else{
					dates[nr-1] = date;
					if (passwords[nr-1] == null || passwords[nr-1].equals("")) throw new Exception("Password not set for player "+nr);
					int ii = gamePage.lastIndexOf("/");
					String gameName = gamePage.substring(ii+1).split("\\.")[0];
					String u = "http://starsautohost.org/cgi-bin/downloadturn.php?file="+gameName+".x"+nr; //+"&password="+passwords[nr-1];
					System.out.println(u);
					File dir = new File(downloadDir,""+year);
					if (dir.exists() == false) dir.mkdir();
					url = new URL(u);
					c = (HttpURLConnection)url.openConnection();
					c.setDoOutput(true); c.setDoInput(true);
					File f = new File(dir,gameName+".x"+nr);
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
					//out.write("password: \""+passwords[nr-1]+"\"\n");
					out.write("password="+passwords[nr-1]);
					out.flush();
					BufferedInputStream is = new BufferedInputStream(c.getInputStream());
					BufferedOutputStream fs = new BufferedOutputStream(new FileOutputStream(f));
					while(true){
						int b = is.read();
						if (b == -1) break;
						fs.write(b);
					}
					is.close();
					fs.flush();
					fs.close();
				}
			}
		}
		setDates(year, dates);
		System.out.println("Finished");
	}

	private void init() throws Exception{
		File f = new File("xfiledownload.ini");
		if (f.exists() == false) f = new File("..\\xfiledownload.ini");
		if (f.exists() == false) throw new Exception("xfildownload.ini not found");
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f),Charset.forName("UTF8")));
		while(true){
			String s = in.readLine();
			if (s == null) break;
			if (s.contains("=") == false) continue;
			if ((int)s.charAt(0) == 65279) s = s.substring(1);
			String key = s.split("=",-1)[0].trim();
			String value = s.split("=",-1)[1].trim();
			if (key.equals("gamepage")) gamePage = value;
			if (key.equals("downloaddir")) downloadDir = value;
			if (key.startsWith("player")){
				int nr = Integer.parseInt(key.substring(6));
				passwords[nr-1] = value;
			}
		}
		in.close();
		if (gamePage.equals("")) throw new Exception("Gamepage not defined");
		if (downloadDir.equals("")) throw new Exception("Downloaddir not defined");
	}
}
