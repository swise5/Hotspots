package visualizations;

import hotspots.sim.Hotspots;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.io.geo.ShapeFileImporter;
import sim.util.geo.MasonGeometry;
import swiselib.disasters.Wildfire;

public class Viz extends SimState {

	private static final long serialVersionUID = 1L;
	
	String dataDir = "/Users/swise/Projects/GISAgents/wildfires/partitioned"; // #WILDFIRE
	/* "/Users/swise/Projects/GISAgents/Sandy/sandyREMERGED"; */
	String fileType = ".tsv";

	public int grid_width =	600; // #WILDFIRE
	/*	//600; // NY  
		//400; // Area
		800; // USA
		*/
	public int grid_height = 450;
	public Date startTime = null;

	GeomVectorField baseLayer = new GeomVectorField(grid_width, grid_height);
	GeomVectorField objectLayer = new GeomVectorField(grid_width, grid_height);
	GeomVectorField objectLayer2 = new GeomVectorField(grid_width, grid_height);
	// #WILDFIRE ~~~~~~~
	public GeomVectorField fireLayer = new GeomVectorField(grid_width, grid_height);
	public GeomVectorField roadLayer = new GeomVectorField(grid_width, grid_height);
	public ArrayList <GeomVectorField> firePoints = new ArrayList <GeomVectorField>();
	// ~~~~~~~~~~~~~~~~~
	
	Envelope MBR = null;
	
	double [] valenceVals = new double [11];
	ArrayList <Double> valences = new ArrayList <Double> ();

	public Viz(long seed) {
		super(seed);
	}

	public void start() {
		super.start();
		try {
			
			ValenceMeasure vm = new ValenceMeasure();
			
//			URL baseLayerUrl = Viz.class.getResource("../data/misc/mergedWorldUS/newWorldWithDetailedUS.shp");
//			URL baseLayerUrl = Viz.class.getResource("../data/misc/worldWithDetailedUS/newWorldWithDetailedUS.shp");
/*			URL baseLayerUrl = Viz.class.getResource("/Users/swise/Data/GIS/mergedWorldUS/newWorldWithDetailedUS.shp");
			ShapeFileImporter.read(baseLayerUrl, baseLayer);
			MBR = baseLayer.getMBR();
			
			// Resize the boundaries of the simulation according to preference

			
			//MBR.init(-80, -71, 39, 44); // NY
			//MBR.init(-85, -70, 30, 45); // Area
			 
//			MBR.init(-125, -65, 25, 50); // USA
//			MBR.init(-110, -100, 36, 42); // #WILDFIRE
*/
			MBR = new Envelope();
			MBR.init(-105.316, -104.291, 38.523, 39.224); // #WILDFIRE COSprings

	/*		// Update all layers to reflect this new MBR
			
			objectLayer.setMBR(MBR);
			objectLayer2.setMBR(MBR);
*/
			// --- DATA INPUT ---

			// set up the structures to open and process the data
			
			File folder = new File(dataDir);
			File[] hourFiles = folder.listFiles();
			int timestep = 0;
			GeometryFactory gf = new GeometryFactory();
			
			long numcoords = 0, numlocations = 0;

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

					// OPTIONAL: don't process things we're not going to add to the layer
					if(!bits[0].equals("coords")) continue; // only interested in geotagged data?
					//if(!bits[0].equals("location")) continue; // only interested in geocoded data?
					
					// OPTIONAL: if there is too much data, take some subsample of it 
					//if(random.nextDouble() > .25) continue;
					
					// extract the start time of the simulation, if we haven't already
					if(startTime == null){
						String time = bits[3];
						SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						startTime = df.parse(time);
					}

					// create a MasonGeometry to hold the point information
					final MasonGeometry mg = new MasonGeometry(
//							gf.createPoint(new Coordinate(Double.parseDouble(bits[1]), 
//														Double.parseDouble(bits[2]))));
							gf.createPoint(new Coordinate(Double.parseDouble(bits[2]), 
									Double.parseDouble(bits[1]))));

					// calculate the valence information and add it to the MasonGeometry as an attribute
					int valence = vm.getValence(bits[bits.length - 1]);
					//TEMPvalenceVals[valence+5]++;
					TEMPvalenceVals[Math.min(10, Math.max(0, valence))]++;
//					if(valence == -2)
						System.out.println(bits[bits.length - 1]);
					mg.addAttribute("Valence", valence);

					// OPTIONAL: don't add things if they won't be displayed
					if(!MBR.contains(mg.geometry.getCoordinate())) continue;
					
					// COMPOSITE_IMAGE_OPTION: add the geometries to the layer directly and immediately
					//objectLayer.addGeometry(mg);

					// DISPLAY_BY_TIME_OPTION: schedule the dots to appear and disappear at the
					// appropriate times. Remember to process correctly and add to the correct layer!
					if (bits[0].equals("coords")) {
						numcoords++;
						schedule.scheduleOnce(timestep, 1, new Steppable() {
							public void step(SimState state) {
								valences.add((double)mg.getIntegerAttribute("Valence")); // record the valence!
								objectLayer.addGeometry(mg); // LAYER 1
							}
						});
						
					}
					else if(bits[0].equals("location")){
						numlocations++;
						schedule.scheduleOnce(timestep, 1, new Steppable() {
							public void step(SimState state) {
								valences.add((double)mg.getIntegerAttribute("Valence")); // record the valence!
								objectLayer2.addGeometry(mg); // LAYER 2
							}
						});						
					}

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

				// OPTIONAL: a status read-out
			//	System.out.println(timestep + " of " + hourFiles.length);
			}

			// OPTIONAL: a status read-out
			System.out.println("Coords:\t" + numcoords + "\nLocations:\t" + numlocations);

			// COMPOSITE_IMAGE_OPTION: make sure that the MBR is correctly positioned!
			//MBR.init(-80, -71, 39, 44); // NY
			//MBR.init(-85, -70, 30, 45); // Area
			//MBR.init(-125, -65, 25, 50); // USA
			
/*
			// DISPLAY_BY_TIME_OPTION: set up the system to sweep out the old points every step
			schedule.scheduleRepeating(0, 0, new Steppable() {
				public void step(SimState state) {
					objectLayer.clear();
					objectLayer.setMBR(MBR);
					
					objectLayer2.clear();
					objectLayer2.setMBR(MBR);	
					
					valenceVals = new double [11];
					valences = new ArrayList <Double> ();
				}
			});
*/			
			// DISPLAY_BY_TIME_OPTION: set up the system to refresh the MBR every step
			schedule.scheduleRepeating(0, 2, new Steppable() {

				@Override
				public void step(SimState state) {
					//MBR.init(-80, -71, 39, 44); // NY
					//MBR.init(-85, -70, 30, 45); // Area
					//MBR.init(-125, -65, 25, 50); // USA
					//MBR.init(-110, -100, 36, 42); // #WILDFIRE
					MBR.init(-105.316, -104.291, 38.523, 39.224); // #WILDFIRE COSprings

					objectLayer.setMBR(MBR);
					objectLayer2.setMBR(MBR);
					objectLayer.clipEnvelope = MBR;//roadLayer.clipEnvelope;//null;//new Envelope(roadLayer.clipEnvelope.getMinX(), roadLayer.clipEnvelope.getMaxX(), roadLayer.clipEnvelope.getMinY(), roadLayer.clipEnvelope.getMaxY());
				}
				
			});

			// #WILDFIREEEEEEEEEEE
			FireStepper fs = new FireStepper();
			File fireFolder = new File("/Users/swise/Dissertation/Colorado/data/fires_denverPost/shapefiles");
			File [] fireFiles = fireFolder.listFiles();
			// THE WORST KLUDGE?!?!? It's likely
			int index = 0;
			int [] vals = new int [] {13*24, 13*24 + 22, 14*24 + 23, 16*24 + 1, 16*24 + 23, 18*24 + 1, 20*24 + 22, 22*24 + 20};
			for(File f: fireFiles){
				if(!f.getName().endsWith(".shp")) continue;
				GeomVectorField fireFile = new GeomVectorField(grid_width, grid_height);
				ShapeFileImporter.read(f.toURL(), fireFile);
				firePoints.add(fireFile);
				String fireFileName = f.getName();
				/*
				int month = Integer.parseInt(fireFileName.substring(4, 5));
				int day = Integer.parseInt(fireFileName.substring(6, 7));
				int hour = 0, minute = 0;
				if(fireFileName.length() > 12){
					hour = Integer.parseInt(fireFileName.substring(8,9));
					minute = Integer.parseInt(fireFileName.substring(10,11));
				}

				int steppity = (day - startTime.getDay()) * 24 */ 
				schedule.scheduleOnce(vals[index], fs);
				index++;
			}
			
//			URL roadUrl = Viz.class.getResource("/Users/swise/Dissertation/Colorado/data/osm_extracts/CoSpringsLocalRoads.shp");
			File roadFile = new File("/Users/swise/Dissertation/Colorado/data/osm_extracts/CoSpringsLocalRoads.shp");
			ShapeFileImporter.read(roadFile.toURL(), roadLayer);
//			ShapeFileImporter.read(roadUrl, roadLayer);
			roadLayer.setMBR(MBR);
						
			// make sure when simulation is over, we shut it down
			schedule.scheduleOnce( timestep, new Steppable(){
				@Override
				public void step(SimState state) {
					state.finish();
				}
			}
			);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public class FireStepper implements Steppable {

		@Override
		public void step(SimState state) {
			if(firePoints.size() <= 0) return;
			fireLayer.clear();
			GeomVectorField fire = firePoints.remove(0);
			for(Object o: fire.getGeometries()){
				fireLayer.addGeometry((MasonGeometry)o);
			}
			fireLayer.setMBR(MBR);
		}
		
	}
	
	public static void main(String[] args)
    {
		doLoop(Viz.class, args);
		System.exit(0);
    }

}