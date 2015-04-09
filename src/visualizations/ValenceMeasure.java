package visualizations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;


public class ValenceMeasure {

	// *** DATA FILES ***
	public static String afinnFile = "/Users/swise/Dissertation/references/AFINN/AFINN-modified-further.txt",
			 //"/Users/swise/Dissertation/references/AFINN/AFINN-111.txt",
			sentiWordNetFile = "/Users/swise/Dissertation/references/SentiWordNet_3.0.0_20130122.txt",
			redondo = "/Users/swise/Dissertation/resources/redondo.txt",
			emoticonFile = "/Users/swise/Dissertation/references/AFINN/tung_emoticons.txt",
			stopwordsFile = "/Users/swise/Dissertation/references/AFINN/stopwords.txt",
			wordFrequencies = "/Users/swise/Dissertation/resources/coloradoFrequenciesPunct_clean.txt";
	// *** END DATA FILES ***

	// *** OPTIONS ***
	String lexicon = afinnFile;
	boolean use_sqr = false;
	boolean use_negations = false;
	boolean use_freq = false;
	boolean use_count = true;
	boolean use_stemmer = false;
	// *** END OPTIONS ***
	
	// *** OBJECTS ***
	ArrayList <String> stopwords = new ArrayList <String>();
	ArrayList <String> negations = new ArrayList <String>();
    HashMap <String, Integer> valence = new HashMap <String, Integer> ();
    HashMap <String, Integer> emoticons = new HashMap <String, Integer> ();
    HashMap <String, Double> frequencies = new HashMap <String, Double> ();
    StringTokenizer tokenizer = null;
    
    SnowballStemmer stemmer;
    // *** END OBJECTS ***

    public ValenceMeasure(){
    	this(false, false, false, false, afinnFile);
    }
    
    /**
     * Constructor
     */
    public ValenceMeasure(boolean freq, boolean count, boolean stems, boolean neg, String lex){
    	this.use_freq = freq;
    	this.use_count = count;
    	this.use_stemmer = stems;
    	this.use_negations = neg;
    	this.lexicon = lex;

		try {

			// *** VALENCES ***

			// open the valence file
			FileInputStream fstream;
			fstream = new FileInputStream(lexicon);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));
			String s;

			// AFINN
			if (lexicon.equals(afinnFile))
				while ((s = d.readLine()) != null) {
					String[] bits = s.split("\t");
					valence.put(bits[0], Integer.parseInt(bits[1]) + 5);
				}

			// SENTIWORDNET
			else if (lexicon.equals(sentiWordNetFile))
				while ((s = d.readLine()) != null) {
					if (s.startsWith("#"))
						continue;
					String[] bits = s.split("\t");
					String [] words = bits[4].split(" ");
					int value = 5 + (int) (5 * (Double.parseDouble(bits[2]) - Double.parseDouble(bits[3])));
					for(int i = 0; i < words.length; i++){
						String [] info = words[i].split("#");
						String word = info[0];
						if(Integer.parseInt(info[1]) == 1 && !valence.containsKey(word))
							valence.put(word, value);						
					}
				}
			
			else if (lexicon.equals(redondo)){				
				while ((s = d.readLine()) != null) {
					String[] bits = s.split("\t");
					valence.put(bits[0], (int)Double.parseDouble(bits[2]));
				}
			}

			// *** END VALENCES ***

			// *** FREQUENCIES ***
			
			fstream = new FileInputStream(wordFrequencies);
			d = new BufferedReader(new InputStreamReader(fstream));
			while ((s = d.readLine()) != null) {
				String[] bits = s.split("\t");
//				frequencies.put(bits[0], Double.parseDouble(bits[1]));
				frequencies.put(bits[1], Double.parseDouble(bits[0]));
			}

			
			// *** END FREQUENCIES ***
			
			// *** STOPWORDS ***
			fstream = new FileInputStream(stopwordsFile);
			d = new BufferedReader(new InputStreamReader(fstream));
			while ((s = d.readLine()) != null) {
				stopwords.add(s);
			}
			// *** END STOPWORDS ***

			// *** EMOTICONS ***
			fstream = new FileInputStream(emoticonFile);
			d = new BufferedReader(new InputStreamReader(fstream));
			while ((s = d.readLine()) != null) {
				String[] bits = s.split("\t");
				emoticons.put(bits[0], Integer.parseInt(bits[1]));
			}
			// *** END EMOTICONS ***

			// *** NEGATION WORDS ***
			negations.add("not");
			negations.add("cannot");
			negations.add("never");
			negations.add("can't"); 
			negations.add("won't");
			negations.add("ain't");
			negations.add("don't");
			negations.add("didn't");
			// *** END NEGATION WORDS***
			
			stemmer = (SnowballStemmer) englishStemmer.class.newInstance();
		} catch (Exception e) {
			System.err.println("File input error");
		}
	}

	/**
	 * Calculate the sentiment orientation of the given string
	 * 
	 * @param s
	 *            - the target string
	 * @return the sentiment orientation (int between 0 and 10)
	 */
	int getValence(String s) {

		double value = 0.;
		double counts = 0.;
		double freqSum = 0.;

		String raw = s.replaceAll("&amp;", "&");
		raw = raw.replaceAll("&lt;", "<");
		raw = raw.replaceAll("&gt;", "<");

		// go through each of the words and calculate their sentiment value
		//String[] bits = raw.split("\\W"); // split on non-word characters
		String[] bits = raw.split("(?<!\\w)'|[\\s,.?\"!][\\s,.?\"'!]*"); // split with ' and -, etc
		
		for (int i = 0; i < bits.length; i++) {
			
			String bit = bits[i];
			String word = bit.toLowerCase(); // convert all to lower case
			if(word.startsWith("http://")){
				i++; // the rest of the url should be dropped
				continue;
			}
			
			// stem it
			stemmer.setCurrent(word);
			stemmer.stem();
			if(use_stemmer)
				word = stemmer.getCurrent();
			
			// do not consider stopwords
			if (stopwords.contains(word))
				continue; 

			// determine the valence of the word (assume it's neutral)
			double val = 5;
			if (valence.containsKey(word)) { // check if this is a sentiment-laden word
				val = valence.get(word);
			}
			else // don't consider the contributions of unlisted words, they're not interesting enough
				continue;
			
			 if(use_negations && i > 0 && negations.contains(bits[i-1])) // process negations
			 		val = 10 - val;
			
			double freq = 0.;
			if (frequencies.containsKey(word))
				freq = frequencies.get(word);
			else
				freq = .00001;

			if(use_freq){
				val *= freq;
				freqSum += freq;
			}
			else if(use_count)
				freqSum++;
			else
				val -= 5;
			
			value += val;


			// calculate with the appropriate weighting function
			// if(use_sqr)
			// 		value += val * Math.abs(val); // squared but without un-negating negatives
			// else
			// value += val;
			// if(val != 0) // only count words with sentiment
			// 		counts++;
		}

		// include sentiment value from emoticons
		bits = raw.split("\\s"); // split on whitespace characters
		for (String bit : bits) {
			if (emoticons.containsKey(bit)) { // check if this is an emoticon
				double val = emoticons.get(bit);
				
				if(use_freq){
					val *= .0001;
					freqSum += .01;
				}
				else if(use_count)
					freqSum++;
				else
					val -= 5;

				// calculate with the appropriate weighting function
				if (use_sqr)
					value += val * Math.abs(val); // squared but without
													// un-negating negatives
				else
					value += val;
				counts++;
			}
		}

		// if(use_sqr)
		// counts *= counts;

		// if(counts == 0) return 5; // no information either way

		if(use_freq && freqSum == 0) return 5; // no information either way
		else if(use_freq || use_count) value /= freqSum;
		else value += 5;
		// normalize the value of the sentiment score and return it
		// if(counts > 0)
		// value = (int)((double)value/counts);
		return (int) Math.max(0, Math.min(10, value));
	}
}