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
import java.util.HashSet;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.geo.MasonGeometry;
import swiselib.disasters.Wildfire;

public class ProcessValences extends SimState {

	String goldStandard = "/Users/swise/Dissertation/Colorado/goldStandardJun26.txt";
	String testdata = "/Users/swise/Dissertation/Colorado/newTexts.txt";
	String lexicon = "/Users/swise/lexicon.txt";
	String fout = "/Users/swise/Dissertation/Colorado/goldScoresSWN.txt";
	ValenceMeasure vm ;
	
	public ProcessValences(long seed) {
		super(seed);
		vm = new ValenceMeasure();
	}

	public void start() {
		super.start();
		try {

			int num_pos = 0, num_neu = 0, num_neg = 0;
			double corr_percent_pos = 0, corr_percent_neu = 0, corr_percent_neg = 0;
			

			// go through each of the relevant files and read in the data

			// Open the file as an input stream
			FileInputStream fstream;
			fstream = new FileInputStream(goldStandard);
//			fstream = new FileInputStream(lexicon);
//			fstream = new FileInputStream(testdata);

			// Open the file for output
//			BufferedWriter w = new BufferedWriter(new FileWriter(fout));

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(new InputStreamReader(fstream));
			String s;
			d.readLine(); // get rid of the header

			ArrayList <String> wordlist = new ArrayList <String> (vm.valence.keySet());
			
			// read in the file line by line
			while ((s = d.readLine()) != null) {

				String[] bits = s.split("\t"); // split into columns
				
				String gold_valence = bits[0];
				if(gold_valence.equals("negative")) num_neg++;
				else if(gold_valence.equals("positive")) num_pos++;
				else num_neu++;
				
				String text = bits[1];
//				String text = bits[0];
	
				// FOR TESTING
//				String text = s;
//				if(!wordlist.contains(text) && !vm.stopwords.contains(text))
//					w.write(text + "\n");
				
				int valence = vm.getValence(text);
				String my_valence = "";
				if(valence < 5){
					my_valence = "negative";
					if(gold_valence.equals("negative"))
						corr_percent_neg += 1;
				}
				else if(valence > 5){
					my_valence = "positive";
					if(gold_valence.equals("positive"))
						corr_percent_pos += 1;
				}
				else {
					my_valence = "neutral";
					if(gold_valence.equals("neutral"))
						corr_percent_neu += 1;
				}
				
//				String result = valence + "\t" + my_valence + "\t" + gold_valence + "\t" + my_valence.equals(gold_valence) + "\t" + text;
//				w.write(result + "\n");
//				System.out.println(valence + "\t" + gold_valence + "\t" + text);
//				System.out.println(valence + "\t" + text);
			}

			fstream.close();
//			w.close();
			
			String freqMeasure = "raw";
			if(vm.use_count && vm.use_freq) freqMeasure = "ERROR";
			else if(vm.use_count) freqMeasure = "count";
			else if(vm.use_freq) freqMeasure = "freq";
			
			/*
			System.out.println(vm.lexicon + "\tNeg:\t" + vm.use_negations + "\tNorm:\t" + freqMeasure + "\tStemmer:\t" + vm.use_stemmer);
			System.out.println("total\t" + (num_neg + num_neu + num_pos));
			System.out.println("total%\t" + (corr_percent_pos + corr_percent_neu + corr_percent_neg)/(num_neg + num_neu + num_pos));			
			System.out.println("pos\t" + (corr_percent_pos/num_pos));
			System.out.println("neu\t" + (corr_percent_neu/num_neu));
			System.out.println("neg\t" + (corr_percent_neg/num_neg));
			*/
			System.out.print(vm.lexicon + "\t" + vm.use_negations + "\t" + freqMeasure + "\t" + vm.use_stemmer + "\t");
			System.out.print((corr_percent_pos + corr_percent_neu + corr_percent_neg)/(num_neg + num_neu + num_pos) + "\t");			
			System.out.println("\t" + (corr_percent_pos/num_pos) + "\t" + (corr_percent_neu/num_neu) + "\t" + (corr_percent_neg/num_neg));

			
		} catch (Exception e) {
		}

	}

	public static void main(String[] args) {
		doLoop(ProcessValences.class, args);
		System.exit(0);
	}

}