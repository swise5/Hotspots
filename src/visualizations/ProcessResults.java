package visualizations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import sim.engine.SimState;
import sim.field.grid.DoubleGrid2D;

public class ProcessResults extends SimState {

	private static final long serialVersionUID = 1L;

	String fileType = ".txt";

	int totalAgents = 78611;
	
	public ProcessResults(long seed) {
		super(seed);
	}

/*	public void start() {
		super.start();
		try {
			String foutDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/processed/speeds";
			String dataDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/speeds";//-1";

			// go through each of the relevant files and read in the data

			File folder = new File(dataDir);
			File[] runFiles = folder.listFiles();

			HashMap <String, ArrayList <ArrayList <Double>>> runMeans = new HashMap <String, ArrayList <ArrayList <Double>>> (),
					runMaxes = new HashMap <String, ArrayList <ArrayList <Double>>> (),
					runMins = new HashMap <String, ArrayList <ArrayList <Double>>> (); 
			
			int count = 0;
			for (File f : runFiles) {

				String filename = f.getName();
				if (!filename.endsWith(fileType)) // only specific kinds of files
					continue;

				// the group of runs to which this result belongs
				String runGroup = filename.substring(6, filename.lastIndexOf("_"));

				// Open the file as an input stream
				FileInputStream fstream;
				fstream = new FileInputStream(f.getAbsoluteFile());

				// Convert our input stream to a BufferedReader
				BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

				String s;// = d.readLine(); // get rid of the header (MAYBE)

				ArrayList <Double> maxes = new ArrayList <Double> (), means = new ArrayList <Double> (), 
						mins = new ArrayList <Double> ();
				
				// read in the file line by line
				while ((s = d.readLine()) != null) {

					String[] bits = s.split("\t"); // split into columns
					double max = 0, min = Double.MAX_VALUE, mean = 0;

					for(int i = 1; i < bits.length; i++){
						double bit = Double.parseDouble(bits[i]);
						if(bit < min) min = bit;
						if(bit > max) max = bit;
						mean += bit;
					}
					if(bits.length > 1)
						mean /= (bits.length - 1);
					else
						mean = 0;
					
					if(min == Double.MAX_VALUE)
						min = 0;
					
					maxes.add(max);
					mins.add(min);
					means.add(mean);
				}

				if(!runMeans.containsKey(runGroup)){
					runMeans.put(runGroup, new ArrayList <ArrayList <Double>> ());
					runMins.put(runGroup, new ArrayList <ArrayList <Double>> ());
					runMaxes.put(runGroup, new ArrayList <ArrayList <Double>> ());
				}
					
				runMeans.get(runGroup).add(means);
				runMins.get(runGroup).add(mins);
				runMaxes.get(runGroup).add(maxes);
				
				fstream.close();
				
				count++;
				System.out.println(count);
			}

			for(String runGroup: runMeans.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_means.txt"));

				ArrayList <ArrayList <Double>>runMean = runMeans.get(runGroup);
				
				for(int i = 0; i < runMean.size(); i++){
					String myString = "";
					for(Double d: runMean.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}

			for(String runGroup: runMins.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_mins.txt"));

				ArrayList <ArrayList <Double>>runMin = runMins.get(runGroup);
				
				for(int i = 0; i < runMin.size(); i++){
					String myString = "";
					for(Double d: runMin.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}
			
			for(String runGroup: runMaxes.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_maxes.txt"));

				ArrayList <ArrayList <Double>> runMax = runMaxes.get(runGroup);
				
				for(int i = 0; i < runMax.size(); i++){
					String myString = "";
					for(Double d: runMax.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}

			for(String runGroup: runMaxes.keySet()){
				System.out.println(runGroup + "\t" + runMaxes.get(runGroup).size());
			}

		} catch (Exception e) {}
	}

/*
	
 // SENTIMENTS
	public void start() {
		super.start();
		try {

			String foutDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/processed/sentiments";
			String dataDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/sentiments";//-1";

			// go through each of the relevant files and read in the data

			File folder = new File(dataDir);
			File[] runFiles = folder.listFiles();

			HashMap <String, ArrayList <ArrayList <Double>>> runMeans = new HashMap <String, ArrayList <ArrayList <Double>>> (),
					runMaxes = new HashMap <String, ArrayList <ArrayList <Double>>> (),
					runMins = new HashMap <String, ArrayList <ArrayList <Double>>> (); 
			
			int count = 0;
			for (File f : runFiles) {

				String filename = f.getName();
				if (!filename.endsWith(fileType)) // only specific kinds of files
					continue;

				// the group of runs to which this result belongs
				String runGroup = filename.substring(9, filename.lastIndexOf("_"));

				// Open the file as an input stream
				FileInputStream fstream;
				fstream = new FileInputStream(f.getAbsoluteFile());

				// Convert our input stream to a BufferedReader
				BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

				String s;// = d.readLine(); // get rid of the header (MAYBE)

				ArrayList <Double> maxes = new ArrayList <Double> (), means = new ArrayList <Double> (), 
						mins = new ArrayList <Double> ();
				
				// read in the file line by line
				while ((s = d.readLine()) != null) {

					String[] bits = s.split("\t"); // split into columns
					double max = 0, min = Double.MAX_VALUE, mean = 0;
					int numAgentRecords = Integer.parseInt(bits[1]);
					for(int i = 2; i < bits.length; i++){
						double bit = Math.min(10, Double.parseDouble(bits[i]));
						if(bit < min) min = bit;
						if(bit > max) max = bit;
						mean += bit;
					}
					if(numAgentRecords < totalAgents)
						min = 0;
					mean /= totalAgents;//(bits.length - 1);
					
					maxes.add(max);
					mins.add(min);
					means.add(mean);
				}

				if(!runMeans.containsKey(runGroup)){
					runMeans.put(runGroup, new ArrayList <ArrayList <Double>> ());
					runMins.put(runGroup, new ArrayList <ArrayList <Double>> ());
					runMaxes.put(runGroup, new ArrayList <ArrayList <Double>> ());
				}
					
				runMeans.get(runGroup).add(means);
				runMins.get(runGroup).add(mins);
				runMaxes.get(runGroup).add(maxes);
				
				fstream.close();
				
				count++;
				System.out.println(count);
			}

			for(String runGroup: runMeans.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_means.txt"));

				ArrayList <ArrayList <Double>>runMean = runMeans.get(runGroup);
				
				for(int i = 0; i < runMean.size(); i++){
					String myString = "";
					for(Double d: runMean.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}

			for(String runGroup: runMins.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_mins.txt"));

				ArrayList <ArrayList <Double>>runMin = runMins.get(runGroup);
				
				for(int i = 0; i < runMin.size(); i++){
					String myString = "";
					for(Double d: runMin.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}
			
			for(String runGroup: runMaxes.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + "_maxes.txt"));

				ArrayList <ArrayList <Double>> runMax = runMaxes.get(runGroup);
				
				for(int i = 0; i < runMax.size(); i++){
					String myString = "";
					for(Double d: runMax.get(i))
						myString += d + "\t";
					w.write(myString + "\n");
				}
				
				w.close();
			}


		} catch (Exception e) {}
	}

	/*
	 * 
	 */
  // HEATMAPS
	public void start() {
		super.start();
		try {

			String foutDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/processed/heatmaps";
			String dataDir = "/Users/swise/Dissertation/Colorado/finalLegionResults/nofire/heatmaps";//-1";

			// go through each of the relevant files and read in the data

			File folder = new File(dataDir);
			File[] runFiles = folder.listFiles();

			HashMap <String, DoubleGrid2D> differentRuns = new HashMap <String, DoubleGrid2D> (); 
			
			for (File f : runFiles) {

				String filename = f.getName();
				if (!filename.endsWith(fileType)) // only specific kinds of files
					continue;

				// the group of runs to which this result belongs
				String runGroup = filename.substring(7, filename.lastIndexOf("_"));

				// Open the file as an input stream
				FileInputStream fstream;
				fstream = new FileInputStream(f.getAbsoluteFile());

				// Convert our input stream to a BufferedReader
				BufferedReader d = new BufferedReader(new InputStreamReader(fstream));

				String s = d.readLine(); // get rid of the header (MAYBE)

				DoubleGrid2D myGrid = differentRuns.get(runGroup);
				if(myGrid == null){
					String[] bits = s.split("\t");
					myGrid = new DoubleGrid2D(Integer.parseInt(bits[0]), Integer.parseInt(bits[1]));
					differentRuns.put(runGroup, myGrid);
				}

				int row = 0;
				// read in the file line by line
				while ((s = d.readLine()) != null) {

					String[] bits = s.split("\t"); // split into columns
					int col = 0;
					for(String bit: bits){
						int i = Integer.parseInt(bit);
						myGrid.field[row][col] += i;
						col++;
					}
					row++;
				}

				fstream.close();
			}

			for(String runGroup: differentRuns.keySet()){
				
				// Open the file for output
				BufferedWriter w = new BufferedWriter(new FileWriter(foutDir + "/" + runGroup + ".txt"));
				
				DoubleGrid2D myGrid = differentRuns.get(runGroup);
				
				for(int row = 0; row < myGrid.getWidth(); row++){
					for(int col = 0; col < myGrid.getHeight(); col++){
						w.write(myGrid.field[row][col] + "\t");
					}
					w.newLine();
				}
				
				w.close();

			}

		} catch (Exception e) {}
	}

	public static void main(String[] args) {
		doLoop(ProcessResults.class, args);
		System.exit(0);
	}

}