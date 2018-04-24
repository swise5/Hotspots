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

public class TweetTextAnalysis extends SimState {

	String dataDir = "/Users/swise/Projects/GISAgents/wildfires/peakForAnalysis"; // #WILDFIRE
	String fileType = ".tsv";
	public Date startTime = null;

	String[] stopwords = new String[] { "a", "about", "above", "after",
			"again", "against", "all", "am", "an", "and", "any", "are",
			"aren't", "as", "at", "be", "because", "been", "before", "being",
			"below", "between", "both", "but", "by", "can't", "cannot",
			"could", "couldn't", "did", "didn't", "do", "does", "doesn't",
			"doing", "don't", "down", "during", "each", "few", "for", "from",
			"further", "had", "hadn't", "has", "hasn't", "have", "haven't",
			"having", "he", "he'd", "he'll", "he's", "her", "here", "here's",
			"hers", "herself", "him", "himself", "his", "how", "how's", "i",
			"i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "isn't",
			"it", "it's", "its", "itself", "let's", "me", "more", "most",
			"mustn't", "my", "myself", "no", "nor", "not", "of", "off", "on",
			"once", "only", "or", "other", "ought", "our", "ours", "ourselves",
			"out", "over", "own", "same", "shan't", "she", "she'd", "she'll",
			"she's", "should", "shouldn't", "so", "some", "such", "than",
			"that", "that's", "the", "their", "theirs", "them", "themselves",
			"then", "there", "there's", "these", "they", "they'd", "they'll",
			"they're", "they've", "this", "those", "through", "to", "too",
			"under", "until", "up", "very", "was", "wasn't", "we", "we'd",
			"we'll", "we're", "we've", "were", "weren't", "what", "what's",
			"when", "when's", "where", "where's", "which", "while", "who",
			"who's", "whom", "why", "why's", "with", "won't", "would",
			"wouldn't", "you", "you'd", "you'll", "you're", "you've", "your",
			"yours", "yourself", "yourselves" };
	
	public TweetTextAnalysis(long seed) {
		super(seed);
	}
	
	public void start(){
		super.start();
		try{
			
			File folder = new File(dataDir);
			File[] hourFiles = folder.listFiles();
			int numEntries = 0;
			ArrayList <String> strings = new ArrayList <String> ();
			
			HashSet <String> arrayStopwords = new HashSet <String> ();
			for(String stopword: stopwords){
				arrayStopwords.add(stopword);
			}
			
			ArrayList <String> times = new ArrayList <String> ();

	/*		String [] targetTerms = new String [] {"#waldocanyonfire", "#highparkfire", "#flagstafffire", 
					"#cofire", "#cofires", "#waldofire", "#pyramidmtnfire", "#pineridgefire", "#weberfire", 
					"#boulderfire", "#woodlandheightsfire", "#bisonfire", "#coloradofires", "#treasurefire", 
					"#springerfire", "#nmfire", "#lastchancefire", "#littlesandfire", "#coloradowildfires", 
					"#estesparkfire", "#pyramidfire", "#wyfire", "#millvillefire"};
	*/
	//		String [] targetTerms = new String [] {"colorado", "#colorado", "waldo", "#waldo", "wildfire", "#wildfire"};
	
			String [] targetTerms = new String [] {"colorado", "waldo", "co", "boulder", "denver", "flagstaff", "colo", "estes", "manitou", "bouldercolorado"};
			HashMap <String, ArrayList <Integer>> termCounts = new HashMap <String, ArrayList <Integer>> ();
			for(String term: targetTerms){
				ArrayList <Integer> termCount = new ArrayList <Integer> ();
				for(int i = 0; i < (hourFiles.length / 24); i++){
					termCount.add(0);
				}
				termCounts.put(term, termCount);
			}
			
			HashMap <String, Integer> words = new HashMap <String, Integer> ();
			HashMap <String, Integer> hashtags = new HashMap <String, Integer> ();
			// go through each of the relevant files and read in the data
			
			int index = 0;
			int countCoords = 0, countOther = 0;

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

					String fullThing = "";

					String[] bits = s.split("\t"); // split into columns

					if(!bits[0].equals("coords")) countCoords++; // only interested in geotagged data?
					else
						countOther++;

					
					String text = bits[bits.length - 1];
					String parts[] = text.split("(?<!\\w)'|[\\s,.?\"!][\\s,.?\"'!]*");
					for(String part: parts){
						String word = part.toLowerCase();
						word = word.replaceAll("&amp;", "&");
						word = word.replaceAll("&lt;", "<");
						word = word.replaceAll("&gt;", ">");
						word = word.replaceAll("[^\\w#]", "");
						if(word.startsWith("http")) continue;
						if(word.length() < 2) continue;
						if(arrayStopwords.contains(word)) continue;

//						fullThing += word + " ";
						else if(word.startsWith("#")){
							try{
								hashtags.put(word, words.get(word)+1);
							} catch(Exception e){
								hashtags.put(word, 1);
							}
						}
						try{
							words.put(word, words.get(word)+1);
						} catch (Exception e){
							words.put(word, 1);
						}
						
						if(termCounts.containsKey(word)){
							int num = termCounts.get(word).get(index/24);
							termCounts.get(word).set(index/24, num + 1);
						}
						numEntries++;
					}
//					strings.add(fullThing);

 
				}
				
				// clean up after yourself
				fstream.close();
				index++;
	
				// print out times and stuff
				if(index % 24 == 0){
//					System.out.println(f.getName());
					System.out.println(countCoords + "\t" + countOther + "\t" + f.getName().split("\\.")[0]);
					countCoords = 0;
					countOther = 0;
					times.add(f.getName().split("\\.")[0]);
				}
			}
			
			// OUTPUT TO R!!!
			int numTerms = Math.min(10, targetTerms.length);
			for(int i = 0; i < numTerms; i++){
				String term = targetTerms[i];
				
				String thisItem = "term" + i + " = c(";
				ArrayList <Integer> counts = termCounts.get(term);
				for(int j = 0; j < counts.size(); j++){
					thisItem += counts.get(j);
					if(j < counts.size() - 1)
						thisItem += ", ";
				}
				thisItem += ")\n";
				
				System.out.print(thisItem);
			}
						
			String timeString = "times = c(\"";
			for(int i = 0; i < times.size(); i++){
				timeString += times.get(i);
				if(i < times.size() -1)
					timeString += "\",\"";
			}
			timeString += "\")\n";

			System.out.print(timeString);
			
			String frameIt = "d <- data.frame(times=as.POSIXlt(times),";
			for(int i = 0; i < numTerms -1; i++){
				frameIt += "term" + i + ", "; 
			}
			frameIt += "term" + (numTerms - 1) +")\n";
			
			
			System.out.print(frameIt);
			
			String plotIt = "ggplot(data=d, aes(times))";
			for(int i = 0; i < numTerms; i++){
				plotIt += "+ geom_line(aes(y =term" + i + "), colour=pal["+ (i+1) +"], size=2)"; 
			}
			
			plotIt += "+ scale_colour_discrete(name=\"Hashtag\") + ylab(\"Number of Usages\") + xlab(\"Date\") + opts(title=expression(\"Comparison of Hashtag Usage Over Time\"))";
			System.out.print(plotIt);
	
			
/*			System.out.println("printin'");
	
			String fout = "/Users/swise/textsForThisPeriod.txt";
			BufferedWriter w = new BufferedWriter(new FileWriter(fout));
			for(String key: strings){
				w.write(key + "\n");
			}
		
			w.close();

/*			String fout = "/Users/swise/wordTermFrequenciesDAILY.txt";
			BufferedWriter w = new BufferedWriter(new FileWriter(fout));
			for(String key: termCounts.keySet()){
				w.write(key);
				for(Integer i: termCounts.get(key))
					w.write("\t" + i);
				w.write("\n");
			}
		
			w.close();

*/		/*	String fout = "/Users/swise/wordFrequencies.txt";
			BufferedWriter w = new BufferedWriter(new FileWriter(fout));
			w.write(numEntries + "\n");
			for(String key: words.keySet()){
				int count = words.get(key);
				if(count > 5)
					w.write(key + "\t" + count + "\n");
			}
		
			w.close();
		
		/*	fout = "/Users/swise/hashTagFrequencies.txt";
			w = new BufferedWriter(new FileWriter(fout));
			w.write(hashtags.size() + "\n");
			for(String key: hashtags.keySet()){
				int count = hashtags.get(key);
				if(count > 2)
					w.write(key + "\t" + count + "\n");
			}
		*/
		//	w.close();
			
			
		} catch (Exception e){}
	}
	
	public static void main(String[] args)
    {
		doLoop(TweetTextAnalysis.class, args);
		System.exit(0);
    }

	
}