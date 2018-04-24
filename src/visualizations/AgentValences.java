package visualizations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.MasonGeometry;
import swiselib.disasters.Wildfire;

public class AgentValences extends SimState {

	String userTweets = "/Users/swise/Dissertation/Colorado/twitterapi/allCoUserHistories.txt";
	String fout = "/Users/swise/Dissertation/Colorado/twitterapi/testout.txt";
	ValenceMeasure vm ;
	
	public AgentValences(long seed) {
		super(seed);
		vm = new ValenceMeasure();
	}

	public void start() {
		super.start();
		try {

			// go through each of the relevant files and read in the data

			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(userTweets);

			// Open the file for output
//			BufferedWriter w = new BufferedWriter(new FileWriter(fout));
			
			// write the output file header
			String freqMeasure = "raw";
			if(vm.use_count && vm.use_freq) freqMeasure = "ERROR";
			else if(vm.use_count) freqMeasure = "count";
			else if(vm.use_freq) freqMeasure = "freq";
			//w.write(vm.lexicon + "\t" + vm.use_negations + "\t" + freqMeasure + "\t" + vm.use_stemmer + "\n");
			System.out.print(vm.lexicon + "\t" + vm.use_negations + "\t" + freqMeasure + "\t" + vm.use_stemmer + "\n");
			
			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));
			String s;

			// set up date information
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss ZZZZZ yyyy");
			Date start = new SimpleDateFormat("yyyy MM dd").parse("2012 06 22");
			Date end =   new SimpleDateFormat("yyyy MM dd").parse("2012 07 1");

			// set up objects to help output information
			String currentUser = "";
			String timeList = "";
			String valueList = "";
			ArrayList <Integer> valuesThisBin = new ArrayList <Integer> ();
			int lastBin = -1;
			
			String errorString = "";
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {

				if(s.startsWith("ERROR ")){
					errorString += s + "\n";
					continue;
				}
				
				// getting the information
				String[] bits = s.split("\t"); // split into columns
				
				if(bits.length != 7){
					continue; // there's a problem here, grossss
				}
				
				String username = bits[0];
				
				// recordkeeping
				if(!currentUser.equals(username)){
					
					// finish up list
					int total = 0;
					for(Integer i: valuesThisBin){
						total += i;
					}
					timeList += lastBin + "\n";
					valueList += (((double)total)/valuesThisBin.size()) + "\n";
					valuesThisBin = new ArrayList <Integer> ();

					// output those results!
					System.out.print(currentUser + "\n" + timeList + valueList);
					currentUser = username;
					timeList = "";
					valueList = "";
				}
				
				// getting the time information
				String time_str = bits[4];
				Date currentTime = dateFormat.parse(time_str);
				int timeBin = bin(start, end, 60 * 60, currentTime);
				if(lastBin != timeBin && valuesThisBin.size() > 0){
					int total = 0;
					for(Integer i: valuesThisBin){
						total += i;
					}
					timeList += lastBin + "\t";
					valueList += (((double)total)/valuesThisBin.size()) + "\t";
					valuesThisBin = new ArrayList <Integer> ();
				}
				lastBin = timeBin;
				
				// get the tweet information
				String text = bits[6];
				int valence = vm.getValence(text);
				valuesThisBin.add(valence);
				/*
				// getting the location
				double lat, lon;
				String coords_str = bits[5];
				String location = bits[3];
				
				if(!coords_str.equals("")){
					String [] location_bits = coords_str.split(",");
					lon = Double.parseDouble(location_bits[0].split(":")[1]);
					lat = Double.parseDouble(location_bits[1].split(":")[1]);
				}
				else if(location.startsWith("�T: ")){
					location.replace("�T: ", "");
					String [] location_temp = location.split(",");
					lon = Double.parseDouble(location_temp[0]);
					lat = Double.parseDouble(location_temp[1]);
				}
				else {
					lon = -104.8035;
					lat = 38.8735;
				}
				*/
			}

			fstream.close();
//			w.flush();
//			w.close();
			System.out.print(errorString);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	static int bin(Date start, Date end, int periodInSeconds, Date time){
		long myTimeDelta = time.getTime() - start.getTime(); 
		int givenBins = (int) (myTimeDelta / (periodInSeconds * 1000.));
		return givenBins;
	}
	
	
	public static void main(String[] args) {
		doLoop(AgentValences.class, args);
		System.exit(0);
	}

}