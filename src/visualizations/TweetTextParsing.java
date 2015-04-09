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
import swise.disasters.Wildfire;

public class TweetTextParsing extends SimState {

	String dataDir = "/Users/swise/Projects/GISAgents/wildfires/partitionedTail"; // #WILDFIRE
	String fileType = ".tsv";
	public Date startTime = null;

	public TweetTextParsing(long seed) {
		super(seed);
	}
	
	public void start(){
		super.start();
		try{
			
			ValenceMeasure vm = new ValenceMeasure();

			File folder = new File(dataDir);
			File[] hourFiles = folder.listFiles();
			int timestep = 0;
			long numcoords = 0;
			HashSet <String> texts = new HashSet <String> ();
			HashMap <String, Integer> words = new HashMap <String, Integer> ();
			int numEntries = 0;

			// go through each of the relevant files and read in the data
			
			int [] TEMPvalenceVals = {0,0,0,0,0,0,0,0,0,0,0}; 

			for (File f : hourFiles) {
				
				if (!f.getName().endsWith(fileType)) // we are only interested in, for example, .tsv files
					continue;
				
				// Open the file as an input stream
				FileInputStream fstream;
				fstream = new FileInputStream(f.getAbsoluteFile());

				// Convert our input stream to a BufferedReader
				BufferedReader d = new BufferedReader(new InputStreamReader(
						fstream));
				String s;
				d.readLine(); //get rid of the header
				
				// read in the file line by line
				while ((s = d.readLine()) != null) {

					String[] bits = s.split("\t"); // split into columns

					String text = bits[bits.length - 1];
//					String parts[] = text.split("\\W");
					// String[] bits = raw.split("(?<!\\w)'|[\\s,.?\"!][\\s,.?\"'!]*");
/*					String parts[] = text.split("(?<!\\w)'|[\\s,.?\"!][\\s,.?\"'!]*");
					for(String part: parts){
						String word = part.toLowerCase();
						word = word.replaceAll("&amp;", "&");
						word = word.replaceAll("&lt;", "<");
						word = word.replaceAll("&gt;", "<");
						word = word.replaceAll("\\W", "");
						if(word.startsWith("http")) continue;
						try{
							words.put(word, words.get(word)+1);
						} catch (Exception e){
							words.put(word, 1);
						}
						numEntries++;
					}
					//texts.add(text);
*/
					//if(!bits[0].equals("coords")) continue; // only interested in geotagged data?
					//if(!bits[0].equals("location")) continue; // only interested in geocoded data?
					
					// OPTIONAL: if there is too much data, take some subsample of it 
					//if(random.nextDouble() > .25) continue;
					
					// calculate the valence information
					
					int valence = vm.getValence(bits[bits.length - 1]);
					
					if(valence < 2)
						System.out.println(valence + "\t" + text);
					TEMPvalenceVals[valence]++;

					// if (bits[0].equals("coords")) { numcoords++;}
				}
				
				if(timestep % 24 == 23)
				{
					System.out.print(f.getName() + "\t");

					for(int num: TEMPvalenceVals)
						System.out.print(num + "\t");
					System.out.println();

					TEMPvalenceVals = new int [] {0,0,0,0,0,0,0,0,0,0,0};
				}
				// update the timestep at which to schedule the next set of points 
				timestep++;

				// clean up after yourself
				fstream.close();
			}
			
	/*		String fout = "/Users/swise/wordFrequenciesPunct.txt";
			BufferedWriter w = new BufferedWriter(new FileWriter(fout));
/*			ArrayList <String> textsArray = new ArrayList <String> ();
			for(String text: texts) textsArray.add(text);
			
			for(int i = 0; i < 300; i++){
				w.write(textsArray.remove(random.nextInt(textsArray.size())) + "\n");
			}
	*/		
		/*	w.write(numEntries + "\n");
			for(String key: words.keySet()){
				int count = words.get(key);
				if(count > 2)
					w.write(key + "\t" + count + "\n");
			}
		
			w.close();
			*/
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
    {
		doLoop(TweetTextParsing.class, args);
		System.exit(0);
    }

	
}