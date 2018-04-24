package swiselib.objects;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sim.util.distribution.*;
import sim.field.geo.GeomVectorField;
import sim.field.network.Network;
import sim.io.geo.ShapeFileImporter;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import swiselib.objects.network.GeoNode;
import swiselib.objects.network.ListEdge;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;

import ec.util.MersenneTwisterFast;

/**
 * Population Synthesis Method
 * 
 * @author swise
 *
 * DEMOGRAPHIC PARAMETERS RECORDED HERE FOR POSTERITY: AS THEY EXISTED IN 2011 CENSUS
 *
GEOID10: County Subdivision identifier; a concatenation of 2010 Census state FIPS
NAMELSAD10: 2010 Census name and the translated legal/statistical area description code for county subdivision
ALAND10: 2010 Census land area (square meters)
AWATER10: 2010 Census water area (square meters)
INTPTLAT10: 2010 Census latitude of the internal point
INTPTLON10: 2010 Census longitude of the internal point

DP0010001: Total Population

DP0010002: Total Population: Under 5 years
DP0010003: Total Population: 5 to 9 years
DP0010004: Total Population: 10 to 14 years
DP0010005: Total Population: 15 to 19 years
DP0010006: Total Population: 20 to 24 years
DP0010007: Total Population: 25 to 29 years
DP0010008: Total Population: 30 to 34 years
DP0010009: Total Population: 35 to 39 years
DP0010010: Total Population: 40 to 44 years
DP0010011: Total Population: 45 to 49 years
DP0010012: Total Population: 50 to 54 years
DP0010013: Total Population: 55 to 59 years
DP0010014: Total Population: 60 to 64 years
DP0010015: Total Population: 65 to 69 years
DP0010016: Total Population: 70 to 74 years
DP0010017: Total Population: 75 to 79 years
DP0010018: Total Population: 80 to 84 years
DP0010019: Total Population: 85 years and over

DP0010020: Total Population: Male
DP0010021: Total Population: Male: Under 5 years
DP0010022: Total Population: Male: 5 to 9 years
DP0010023: Total Population: Male: 10 to 14 years
DP0010024: Total Population: Male: 15 to 19 years
DP0010025: Total Population: Male: 20 to 24 years
DP0010026: Total Population: Male: 25 to 29 years
DP0010027: Total Population: Male: 30 to 34 years
DP0010028: Total Population: Male: 35 to 39 years
DP0010029: Total Population: Male: 40 to 44 years
DP0010030: Total Population: Male: 45 to 49 years
DP0010031: Total Population: Male: 50 to 54 years
DP0010032: Total Population: Male: 55 to 59 years
DP0010033: Total Population: Male: 60 to 64 years
DP0010034: Total Population: Male: 65 to 69 years
DP0010035: Total Population: Male: 70 to 74 years
DP0010036: Total Population: Male: 75 to 79 years
DP0010037: Total Population: Male: 80 to 84 years
DP0010038: Total Population: Male: 85 years and over

DP0010039: Total Population: Female
DP0010040: Total Population: Female: Under 5 years
DP0010041: Total Population: Female: 5 to 9 years
DP0010042: Total Population: Female: 10 to 14 years
DP0010043: Total Population: Female: 15 to 19 years
DP0010044: Total Population: Female: 20 to 24 years
DP0010045: Total Population: Female: 25 to 29 years
DP0010046: Total Population: Female: 30 to 34 years
DP0010047: Total Population: Female: 35 to 39 years
DP0010048: Total Population: Female: 40 to 44 years
DP0010049: Total Population: Female: 45 to 49 years
DP0010050: Total Population: Female: 50 to 54 years
DP0010051: Total Population: Female: 55 to 59 years
DP0010052: Total Population: Female: 60 to 64 years
DP0010053: Total Population: Female: 65 to 69 years
DP0010054: Total Population: Female: 70 to 74 years
DP0010055: Total Population: Female: 75 to 79 years
DP0010056: Total Population: Female: 80 to 84 years
DP0010057: Total Population: Female: 85 years and over

...

DP0130001: Households: Total
DP0130002: Households: Total: Family households (families)
DP0130003: Households: Total: Family households (families): With own children under 18 years
DP0130004: Households: Total: Husband-wife family
DP0130005: Households: Total: Husband-wife family: With own children under 18 years
DP0130006: Households: Total: Male householder, no wife present
DP0130007: Households: Total: Male householder, no wife present: With own children under 18 years
DP0130008: Households: Total: Female householder, no husband present
DP0130009: Households: Total: Female householder, no husband present: With own children under 18 years
DP0130010: Households: Total: Nonfamily households
DP0130011: Households: Total: Nonfamily households: Householder living alone
DP0130012: Households: Total: Nonfamily households: Householder living alone: Male
DP0130013: Households: Total: Nonfamily households: Householder living alone: Male: 65 years and over
DP0130014: Households: Total: Nonfamily households: Householder living alone: Female
DP0130015: Households: Total: Nonfamily households: Householder living alone: Female: 65 years and over

 */
public class PopSynth {
	
	String censusTractFilename = "/Users/swise/Dissertation/Colorado/data/Tract_2010Census_DP1/tinyArea.shp";
	String roadsFilename = "/Users/swise/Dissertation/Colorado/data/osm_extracts/tinyRoads.shp";
	String travelToWorkFilename = "/Users/swise/Dissertation/Colorado/data/census/countyTravelToWork.csv";
	String socialMediaUsageFilename = "/Users/swise/Dissertation/Colorado/data/PewTwitterUsageStats.txt";
	
	Network roadNetwork;
	MersenneTwisterFast random = new MersenneTwisterFast(1234);

	GeometryFactory gf = new GeometryFactory();
	
	public static double resolution = 5;// // the granularity of the simulation 

	public static int familyWeight = 10;
	public static int friendWeight = 5;
	public static int acquaintenceWeight = 1;
	public static int maxNumCoworkers = 35;
	
	/**
	 * A structure to hold information about the generated individuals
	 */
	class Agent extends MasonGeometry {
		
		int age;
		int sex;
		Point home;
		Point work;
		HashMap <Agent, Integer> socialTies; // range: 0-10 with 10 being strongest
		ArrayList <Agent> socialMediaTies;
		
		public Agent(int a, int s){
			age = a;
			sex = s;
			socialTies = new HashMap <Agent, Integer> ();
			socialMediaTies = null; // set to null until we decide it's active
			this.addStringAttribute("ID", "" + random.nextLong()); // HIGHLY UNLIKELY to collide
		}
		
		public void addContact(Agent a, int weight){
			if(socialTies.containsKey(a))
				socialTies.put(a, weight + socialTies.get(a));
			else
				socialTies.put(a, weight);
		}
		
		public void addMediaContact(Agent a){
			if(socialMediaTies == null)
				socialMediaTies = new ArrayList <Agent> ();
			socialMediaTies.add(a);
		}
		
		/**
		 * Set simple social distance to another agent based on age, sex, and distance between homes
		 * @param a
		 * @return
		 */
		public double getSocialDistance(Agent a){
			double similarity = 0;
			if(a.sex != sex) similarity += 2;
			double ageDiff = 10 * (a.age - age)/18; 
			if(ageDiff > 0)
				similarity += ageDiff;
			else
				similarity -= ageDiff;
			
			if(home != null){
				double distance = home.distance(a.home);
				similarity += distance * 1000;
			}
			
			return Math.max(0, similarity);
		}
		
		public boolean equals(Object o){
			if(! (o instanceof Agent)) return false;
			return ((Agent)o).getStringAttribute("ID").equals(this.getStringAttribute("ID"));
		}
		
		public int hashCode(){
			return this.getStringAttribute("ID").hashCode();
		}
	}
	
	double [] ageSexConstraints = null;
	
	public PopSynth(){

		// read in data
		GeomVectorField field = readInVectors(censusTractFilename);
		GeomVectorField roads = readInVectors(roadsFilename);
		roadNetwork = NetworkUtilities.multipartNetworkCleanup(roads, new Bag(), resolution, gf, random, 0);

		HashMap <MasonGeometry, ArrayList <GeoNode>> nodesTractMapping = getNodesTractMapping(field, roadNetwork);
		
		// construct the houses into which individuals are to be slotted
		HashMap <MasonGeometry, ArrayList<Point>> houses = generateHouses(field, roadNetwork);
		

		ArrayList<ArrayList<Agent>> allHouseholds = new ArrayList <ArrayList<Agent>> ();
		HashMap <String, ArrayList<ArrayList<Agent>>> householdsPerCounty = 
				new HashMap <String, ArrayList<ArrayList<Agent>>> (); 

		HashMap <MasonGeometry, String> tractToCountyMapping = new HashMap <MasonGeometry, String> ();

		for (Object o : field.getGeometries()) {

			MasonGeometry tract = (MasonGeometry) o;
			
			if(!nodesTractMapping.containsKey(tract))
				continue;
			
			String tractName = tract.getStringAttribute("GEOID10");
			String countyName = tractName.substring(0, 5);

			tractToCountyMapping.put(tract, countyName);

			if(!householdsPerCounty.containsKey(countyName))
				householdsPerCounty.put(countyName, new ArrayList <ArrayList<Agent>>());
		}			


		//
		// Generate the households per census tract
		//

		// iterate through the census tracts and generate the households
		// residing in each
		ArrayList <Agent> allIndividuals = new ArrayList <Agent> ();
		for (Object o : field.getGeometries()) {
			MasonGeometry tract = (MasonGeometry) o;

			if(!nodesTractMapping.containsKey(tract))
				continue;
							
			// don't bother to generate population for unlinked areas
			if (houses.get(tract) == null || houses.get(tract).size() == 0)
				continue;

			// generate the individuals and assemble them into households
			ArrayList<Agent> individuals = generateIndividuals(tract);
			if (individuals == null)
				continue;
			
			ArrayList<ArrayList<Agent>> households = generateHouseholds(tract, (ArrayList <Agent>)individuals.clone());
			
			assignHouseholdsToHouses(households, houses.get(tract));

			ArrayList <Agent> noAssignedHome = new ArrayList <Agent> ();
			for(Agent a: individuals){
				if(a.home == null)
					noAssignedHome.add(a);
			}
			
			ArrayList <ArrayList<Agent>> emptyHouseholds = new ArrayList <ArrayList <Agent>> ();
			for(ArrayList <Agent> household: households){
				if(household.size() == 0) 
					emptyHouseholds.add(household);
			}
			households.removeAll(emptyHouseholds);
			
			individuals.removeAll(noAssignedHome);
			
			allIndividuals.addAll(individuals);

			householdsPerCounty.get(tractToCountyMapping.get(tract)).addAll(households);
			allHouseholds.addAll(households);
			System.out.println("Finished with " + tract.getStringAttribute("GEOID10"));
		}
				
		ArrayList <Agent> socialMediaUsers = getSocialMediaUsers(allIndividuals);

		System.out.println("Finished with picking social media users");

		//
		// Assign individuals to workplaces
		//

		generateWorkplaces(field, roadNetwork, tractToCountyMapping, householdsPerCounty);
		System.out.println("Finished with generating workplaces");
		
		System.gc();
		
		//
		// Create friendship-based social ties
		//

		allIndividuals = new ArrayList <Agent> ();
		for(ArrayList <Agent> household: allHouseholds){
			allIndividuals.addAll(household);
		}
		
		sociallyCluster(allIndividuals, acquaintenceWeight);

		System.out.println("Finished with social clustering");
		
		sociallyMediaCluster(socialMediaUsers, acquaintenceWeight, 15);

		System.out.println("Finished with social media clustering");
		
		//
		// Write out the findings
		//

		writeOutAggregated(allHouseholds);
	}

	/**
	 * Map the road network to the census tracts
	 * 
	 * @param field
	 * @param roadNetwork
	 * @return
	 */
	HashMap <MasonGeometry, ArrayList <GeoNode>> getNodesTractMapping (GeomVectorField field, Network roadNetwork){

		HashMap <MasonGeometry, ArrayList <GeoNode>> nodesTractMapping = new HashMap <MasonGeometry, ArrayList <GeoNode>> ();
	
		for(Object o: roadNetwork.getAllNodes()){ // go through nodes one by one
			
			GeoNode node = (GeoNode) o;
			MasonGeometry tract = getCovering(node, field);
			// no need to consider further nodes which are not within any tract
			if(tract == null)
				continue;
			if(!nodesTractMapping.containsKey(tract))
				nodesTractMapping.put(tract, new ArrayList <GeoNode> ());
			nodesTractMapping.get(tract).add(node);
		}
		
		return nodesTractMapping;
	}

	public void writeOutAggregated(ArrayList <ArrayList <Agent>> households){
		String fout = "/Users/swise/Dissertation/Colorado/TESTTINY.txt";
		
		BufferedWriter w;
		try {
			w = new BufferedWriter(new FileWriter(fout));
			
			HashMap <Agent, Agent> agentsToHouseAgents = new HashMap <Agent, Agent> ();
			ArrayList <Agent> houseAgents = new ArrayList <Agent> ();
			
			for (ArrayList<Agent> household: households) {
				
				ArrayList <Agent> employees = new ArrayList <Agent> ();
				
				// first, make a list of everyone who gets their own agents ------------
				for (Agent a : household) {
					if(a.work != null) employees.add(a);
				}
				
				// perhaps there aren't any employed persons in the house - then add the 
				// oldest tenant 
				if(employees.size() == 0){
					int maxAge = -1;
					Agent defaultAgent = null;
					for(Agent a: household){
						if(maxAge < a.age){
							maxAge = a.age;
							defaultAgent = a;
						}
					}
					employees.add(defaultAgent);
				}
				
				// error check - there must be SOMEone!
				if(employees.size() == 0)
					System.out.println("synth pop problem");

				// now generate HouseAgents for each of the relevant agents! ----------
				
				Agent head = employees.get(0);
				if(head == null) 
					continue;
				Agent houseAgent = new Agent(head.age, head.sex);
				houseAgent.home = head.home;
				houseAgent.work = head.work;
				houseAgent.addStringAttribute("ID", head.getStringAttribute("ID"));
				houseAgents.add(houseAgent);
				
				for(Agent b: household)
					agentsToHouseAgents.put(b, houseAgent);
			}
			
			for(ArrayList <Agent> household: households){
				
				HashSet <Agent> connectedHouses = new HashSet <Agent> (),
						connectedMediaHouses = new HashSet <Agent> ();
				
				// socially connect the HouseAgent objects that already exist -------
				for(Agent a: household){
					
					for(Agent b: a.socialTies.keySet()){
						if(household.contains(b)) continue;
						else if(! agentsToHouseAgents.containsKey(b)) 
							continue;
						Agent bHouse = agentsToHouseAgents.get(b);
						connectedHouses.add(bHouse);
					}
					
					if(a.socialMediaTies != null)
					for(Agent b: a.socialMediaTies){
						if(household.contains(b)) continue;
						else if(! agentsToHouseAgents.containsKey(b)) 
							continue;
						Agent bHouse = agentsToHouseAgents.get(b);
						connectedMediaHouses.add(bHouse);
					}
				}
			
				Agent thisHouse = agentsToHouseAgents.get(household.get(0));
				for(Agent a: connectedHouses)
					thisHouse.addContact(a, friendWeight);
				for(Agent a: connectedMediaHouses)
					thisHouse.addMediaContact(a);
			}
			
			
			// WRITE OUT THESE NEW HOUSE AGENTS
			for(Agent a: houseAgents){
				w.write(a.getStringAttribute("ID") + "\t" + a.age + "\t" + a.sex + "\t" + a.home.toString() + "\t");
				long myId = Long.parseLong(a.getStringAttribute("ID"));
				if (a.work != null)
					w.write(a.work.toString() + "\t");
				else
					w.write("\t");

				String contacts = "";
				int numContacts = 0;
				
				for (Agent tie : a.socialTies.keySet()) {
					if (Long.parseLong(tie.getStringAttribute("ID")) < myId){
						contacts += tie.getStringAttribute("ID") + " " + a.socialTies.get(tie) + "\t";
						numContacts++;
					}
				}

				w.write(numContacts + "\t" + contacts);

				
				if (a.socialMediaTies != null) {
					contacts = "";
					numContacts = 0;
					for (Agent tie : a.socialMediaTies) {
						if (Long.parseLong(tie.getStringAttribute("ID")) < myId){
							contacts += tie.getStringAttribute("ID") + "\t";
							numContacts++;
						}
					}
					w.write(numContacts + "\t" + contacts);
				}
				
				w.newLine();
			}

			w.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void assignHouseholdsToHouses(ArrayList <ArrayList <Agent>> households, ArrayList <Point> houses){
		
		ArrayList <Point> assignedHouses = new ArrayList <Point> ();
		
		int numHouses = houses.size();
		if(numHouses < households.size())
			System.out.println("ERROR: not enough housing units given the number of households");
		
		for(ArrayList <Agent> household: households){
			Point assignedHouse = houses.get(random.nextInt(numHouses));
			while(assignedHouses.contains(assignedHouse))
				assignedHouse = houses.get(random.nextInt(numHouses));
			assignedHouses.add(assignedHouse);
			for(Agent a: household)
				a.home = assignedHouse;
		}
	}

	/**
	 * Generate the set of workplaces, given the road network, set of nodes in the tract, number of households,
	 * and the flow of individuals
	 * 
	 * @param field
	 * @param roadNetwork
	 * @param tractToCountyMapping
	 * @param householdsPerCounty
	 */
	void generateWorkplaces(GeomVectorField field, Network roadNetwork, HashMap <MasonGeometry, String> tractToCountyMapping, 
			HashMap <String, ArrayList<ArrayList<Agent>>> householdsPerCounty){
		
		HashMap <String, ArrayList <MasonGeometry>> countyToTractMappingCompare = new HashMap <String, ArrayList <MasonGeometry>> ();
		for(MasonGeometry mg: tractToCountyMapping.keySet()){
			String c = tractToCountyMapping.get(mg);
			if(!countyToTractMappingCompare.containsKey(c))
				countyToTractMappingCompare.put(c, new ArrayList <MasonGeometry>());
			countyToTractMappingCompare.get(c).add(mg);
		}
		
		HashMap <String, ArrayList <MasonGeometry>> countyToTractMapping = new HashMap <String, ArrayList <MasonGeometry>> ();
		for (Object o : field.getGeometries()) {

			MasonGeometry tract = (MasonGeometry) o;
			String tractName = tract.getStringAttribute("GEOID10");
			String countyName = tractName.substring(0, 5);

			if(!countyToTractMapping.containsKey(countyName))
				countyToTractMapping.put(countyName, new ArrayList <MasonGeometry>());
			countyToTractMapping.get(countyName).add(tract);
			
		}			
		
		HashMap <String, HashMap <String, Double>> populationFlows = new HashMap <String, HashMap <String, Double>> ();
		HashMap <String, Double> ratioEmployed = new HashMap <String, Double> ();
		try {
			// Open the tracts file
			FileInputStream fstream = new FileInputStream(travelToWorkFilename);
			
			// Convert our input stream to a BufferedReader
			BufferedReader flowData = new BufferedReader(new InputStreamReader(fstream));
			String s;

			flowData.readLine(); // get rid of header
			while ((s = flowData.readLine()) != null) {
				String[] bits = s.split(",");
				String from = bits[0] + bits[1];
				String to = bits[2].substring(1,3) + bits[3];
				Double count = Double.parseDouble(bits[4]);
				
				// DON'T ADD IT if we're not interested in that area!
				if(!countyToTractMapping.containsKey(from))
					continue;
				
				if(!populationFlows.containsKey(from))
					populationFlows.put(from, new HashMap <String, Double>() );
				populationFlows.get(from).put(to, count);
			}

			// clean up
			flowData.close();
			
		} catch (Exception e) {
			System.err.println("File input error");
		}
		
		// if there was a problem with the input, return without generating flows
		if(populationFlows.keySet().size() == 0) 
			return;
		
		// normalize the flows
		for(String from: populationFlows.keySet()){
			
			// determine the total number of jobs associated with this country
			double totalJobs = 0.;
			for(String to: populationFlows.get(from).keySet())
				totalJobs += populationFlows.get(from).get(to);
			
			// for each of these flows, normalize to a percent of the total population working in that area
			for(String to: populationFlows.get(from).keySet())
				populationFlows.get(from).put(to, (populationFlows.get(from).get(to)/totalJobs));
			
			// determine the proportional number of jobs to generate in this county
			double pop = 0.;
			for(MasonGeometry mg: countyToTractMapping.get(from))
				pop += mg.getIntegerAttribute("DP0010001");
			
			ratioEmployed.put(from, totalJobs/pop);
		}

		// generate a mapping of road nodes to counties, soas to assign individuals to workplaces in the appropriate county
		HashMap <String, ArrayList <GeoNode>> nodesToCountyMapping = new HashMap <String, ArrayList <GeoNode>> ();
		for(Object o: roadNetwork.getAllNodes()){ // go through nodes one by one
			
			GeoNode node = (GeoNode) o;
			MasonGeometry tract = getCovering(node, field);
			String county = tractToCountyMapping.get(tract);
			if(!nodesToCountyMapping.containsKey(county))
				nodesToCountyMapping.put(county, new ArrayList <GeoNode> ());
			nodesToCountyMapping.get(county).add(node);
		}
		
		// go through each county and assign household members to jobs based on job flow
		for(String county: householdsPerCounty.keySet()){
			
			// get the set of households associated with this county
			ArrayList <ArrayList<Agent>> households = new ArrayList <ArrayList<Agent>> (householdsPerCounty.get(county));

			// no need to generate any flows from irrelevant counties
			if(populationFlows.get(county) == null)
				continue;
			
			int pop = 0;
			for(ArrayList <Agent> household: households){
				pop += household.size();
			}
			
			int numJobs = (int)(pop * ratioEmployed.get(county));
				
			ArrayList <Agent> workers = new ArrayList <Agent> ();
			for(ArrayList <Agent> household: households){
				for(Agent a: household)
					if(a.age > 3 && a.work == null) workers.add(a);
			}

			HashMap <String, Double> jobDistribution = populationFlows.get(county);

			int workerSize = workers.size();
			if(workerSize == 0){ // if there are no workers, just go on;
				System.out.println("ERROR: no workers for this tract");
				continue;
			}

			for(int i = 0; i < numJobs; i++){

				String toCounty = getIndex(jobDistribution, random.nextDouble());
				
				// select a random node in the county as a worksite
				ArrayList <GeoNode> countyNodes = nodesToCountyMapping.get(toCounty);
				
				if(countyNodes == null) // if no one here will be involved in the simulation, don't bother to generate them
					continue;

				Point workPoint = gf.createPoint(countyNodes.get(random.nextInt(countyNodes.size())).geometry.getCoordinate());
					
				// select a random worker (who is of age to be employed) 
				boolean unassigned = true;
				while(unassigned){
					
					int workerIndex = random.nextInt(workerSize);
					Agent a = workers.get(workerIndex);
					if(a.age > 3 && a.work == null){
						a.work = workPoint;
						workers.remove(workerIndex);
						unassigned = false;
						workerSize--;
					}
	
				}
				
				// terminate it if there's no one left
				if(workers.size() == 0)
					i = numJobs + 1;
			}
		}
	}
	
	/**
	 * Generate the set of possible houses in the environment, given the residential roads
	 *  
	 * @param field
	 * @param roadNetwork
	 * @return
	 */
	HashMap <MasonGeometry, ArrayList<Point>> generateHouses(GeomVectorField field, Network roadNetwork){
		
		HashMap <MasonGeometry, ArrayList <Point>> result = new HashMap <MasonGeometry, ArrayList <Point>> (); 
		
		HashMap <GeoNode, MasonGeometry> nodesTractMapping = new HashMap <GeoNode, MasonGeometry> ();
		HashMap <MasonGeometry, ArrayList <ListEdge>> edgesTractMapping = new HashMap <MasonGeometry, ArrayList <ListEdge>>(); 
		
		//
		// match all the edges to the areas that completely contain them.
		//
		for(Object o: roadNetwork.getAllNodes()){ // go through nodes one by one
			
			GeoNode node = (GeoNode) o;
			MasonGeometry tract = nodesTractMapping.get(node);
			if(tract == null){
				tract = getCovering(node, field);
				nodesTractMapping.put(node, tract);
			}
			
			// no need to consider further nodes which are not within any tract
			if(tract == null)
				continue;

			// for each of the edges out of this node, consider whether they should be included in the
			// "intra-tract" node list
			for(Object p: roadNetwork.getEdgesOut(node)){
				
				// find the opposite node
				ListEdge edge = (ListEdge) p;
				
				String type = ((MasonGeometry)edge.getInfo()).getStringAttribute("TYPE");
				if(!type.equals("residential"))
					continue;
				
				GeoNode otherNode = (GeoNode) edge.getTo();
				if(otherNode == node) otherNode = (GeoNode) edge.getFrom();

				// determine whether this edge qualifies
				MasonGeometry otherTract = nodesTractMapping.get(otherNode);
				if(otherTract == null){
					otherTract = getCovering(otherNode, field);
					nodesTractMapping.put(otherNode, otherTract);
				}
				
				if(otherTract != null && otherTract == tract) {
					if(edgesTractMapping.get(tract) == null)
						edgesTractMapping.put(tract, new ArrayList <ListEdge> ());
					edgesTractMapping.get(tract).add(edge);
				}
			}
		}

		//
		// go through the tracts and generate points along the roads
		//
		for(Object o: field.getGeometries()){
			MasonGeometry tract = (MasonGeometry) o;
			
			if(!edgesTractMapping.containsKey(tract))
				continue; // there are no roads here, so it's not a relevant target area
			
			double residentialRoadLength = 0;
			ArrayList <ListEdge> residentialRoads = new ArrayList <ListEdge> ();
			for(ListEdge e: edgesTractMapping.get(tract)){

				String type = ((MasonGeometry)e.info).getStringAttribute("TYPE");
				if(type.equals("residential")){
					Geometry gm = ((MasonGeometry)e.info).geometry;
					residentialRoadLength += ((MasonGeometry)e.info).geometry.getLength();
					residentialRoads.add(e);
				}
			}
			
			int numHouses= tract.getIntegerAttribute("DP0180001");
			
			// houses on either side of the road along each of the residential roads
			double houseSpacing = residentialRoadLength / (double) numHouses;
			//houseSpacing = Math.max(.001, houseSpacing);
			
			ArrayList <Point> houses = new ArrayList <Point> ();
			
			// iterate along the roads and set up the houses
			for(ListEdge e: residentialRoads){
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				LengthIndexedLine segment = new LengthIndexedLine(ls);
				double startIndex = segment.getStartIndex();
				double endIndex = segment.getEndIndex();
				
				for(double i = startIndex; i < endIndex; i += houseSpacing){
					Point house1 = gf.createPoint(segment.extractPoint(i));
					houses.add(house1);
				}
			}
			
			result.put(tract, houses);
		}
		
		return result;
	}
	
	/**
	 * Get the tract that completely covers the given geometry 
	 * @param g
	 * @param field
	 * @return
	 */
	MasonGeometry getCovering(MasonGeometry g, GeomVectorField field){
		Bag geos = field.getCoveringObjects(g);
		if(geos == null || geos.size() == 0){
			System.out.println("Geometry Error: no field contains this geometry");
			return null;
		}		
		return (MasonGeometry) geos.get(0);
	}

	/**
	 * @param tract - a census tract with demographic attributes
	 * @return a double array of the ratios of males and females of various age groups. Here, the 
	 * 		array is assumed to be structured so that all the ratios of each age group of one sex are presented,
	 * 		then the next. This could easily be extended to deal with more than two sexes.
	 */
	double [] getAgeSexConstraints(MasonGeometry tract){

		double [] ageSexConstraints = new double [36];		
		double total = 0.;
		
		// read in the age structure for males
		String prefix = "DP00100";
		int index = 0;
		for(int i = 21; i < 39; i++){
			String attribute = prefix + i;
			int value = tract.getIntegerAttribute(attribute);
			ageSexConstraints[index] = value;
			index++;
			total += value;
		}
		
		// read in the age structure for females
		for(int i = 40; i < 58; i++){
			String attribute = prefix + i;
			int value = tract.getIntegerAttribute(attribute);
			ageSexConstraints[index] = value;
			index++;
			total += value;
		}
		
		// convert all to ratios of the total population
		for(int i = 0; i < 36; i++)
			ageSexConstraints[i] /= total;
		
		return ageSexConstraints;
	}
	
	/**
	 * From the demographic information associated with a given census tract, generate a set
	 * of individuals who match these parameters. Report on the fit of this generated population.
	 *  
	 * @param area - the census tract
	 * @return a set of individuals
	 */
	ArrayList <Agent> generateIndividuals(MasonGeometry area){
		
		// get the joint distribution of age and sex constraints on the population
		double [] ageSexConstraints = getAgeSexConstraints(area);
		
		ArrayList <Agent> individuals = new ArrayList <Agent> ();
		
		// get the total number of individuals in the area
		int totalPop = area.getIntegerAttribute("DP0010001");
		if(totalPop == 0)
			return null;
		
		totalPop = Math.min(totalPop, 30);
		// for every individual in the area, generate a representative agent
		for(int i = 0; i < totalPop; i++){
			
			// generate information about this agent
			double val = random.nextDouble();
			int index = getIndex(ageSexConstraints, val);
			int sex = index / 18;
			int age = index % 18;
			
			// create the agent
			Agent a = new Agent(age, sex);
			
			// record this individual
			individuals.add(a);
		}
		
		// print out report on the quality of fit
//		System.out.println("Fit for " + area.getStringAttribute("NAMELSAD10") + ": " + fitIndividuals(individuals, ageSexConstraints));	
//		System.out.println("HOUSEHOLDS: " + area.getIntegerAttribute("DP0120002") + "\tGROUP QUARTERS: " + area.getIntegerAttribute("DP0120014") + "\tTOTAL: " + totalPop);
		this.ageSexConstraints = ageSexConstraints;
		
		return individuals;
	}

	/**
	 * Extract the household type constraints from the provided demographic information
	 * @param tract - the census tract with embedded information
	 * @return a set of household type constraints consistent with the coding provided at
	 * 		http://www.census.gov/prod/cen2010/doc/sf1.pdf
	 */
	double [] getHouseholdConstraints(MasonGeometry tract){
		
		double [] householdConstraints = new double [16];
		
		String prefix = "DP01300";
		for(int i = 1; i < 16; i++){
			String attribute = prefix;
			if(i < 10) attribute += 0;
			attribute += i;
			householdConstraints[i] = tract.getIntegerAttribute(attribute);
		}

		// SET UP TYPES OF HOUSEHOLDS
		
		double [] householdRatios = new double [12];
		
		// husband/wife families
		householdRatios[0] = householdConstraints[4] - householdConstraints[5]; // husband-wife family
		householdRatios[1] = householdConstraints[5]; // husband-wife family, OWN CHILDREN < 18
		
		// male householders
		householdRatios[2] = householdConstraints[6] - householdConstraints[7]; // single male householder
		householdRatios[3] = householdConstraints[7]; // single male householder, OWN CHILDREN < 18
		
		// female householders
		householdRatios[4] = householdConstraints[8] - householdConstraints[9]; // single female householder
		householdRatios[5] = householdConstraints[9]; // single female householder, OWN CHILDREN < 18

		// nonfamily householders
		householdRatios[6] = householdConstraints[10] - householdConstraints[11]; // nonfamily group living
		householdRatios[7] = householdConstraints[12] - householdConstraints[13]; // lone male < 65
		householdRatios[8] = householdConstraints[13]; // lone male >= 65
		householdRatios[9] = householdConstraints[14] - householdConstraints[15]; // lone female < 65
		householdRatios[10] = householdConstraints[15]; // lone female >= 65
		
		for(int i = 0; i < 11; i++){
			householdRatios[i] /= householdConstraints[1];
		}
		
		return householdRatios;
	}
	
	/**
	 * Generate a set of households based on the provided set of individuals and household parameters
	 * @param area - the census area with embedded demographic information
	 * @param individuals - the set of individuals generated from this census area
	 * @return sets of Agents grouped into households
	 */
	ArrayList <ArrayList<Agent>> generateHouseholds(MasonGeometry area, ArrayList <Agent> individuals){
				
		ArrayList <ArrayList<Agent>> allHouseholds = new ArrayList <ArrayList <Agent>> ();
		ArrayList <ArrayList<Agent>> familyHouseholds = new ArrayList <ArrayList <Agent>>();

		
		// read in the constraints on household types
		double [] householdTypeRatios = getHouseholdConstraints(area);
				
		
		// remove group population first (NOTIONAL)
		int popInGroupQuarters = area.getIntegerAttribute("DP0120014");
		
		ArrayList <Agent> groupPopulation = new ArrayList <Agent> ();
		for(int i = 0; i < Math.min(individuals.size(), popInGroupQuarters); i++){
			groupPopulation.add(individuals.remove(random.nextInt(individuals.size())));
		}
		
		// GENERATE THE HOUSEHOLDS
		int numHouseholds = area.getIntegerAttribute("DP0130001");	

		for(int i = 0; i < numHouseholds; i++){
			
			if(individuals.size() == 0) 
				continue;
			
			ArrayList <Agent> household = new ArrayList <Agent> ();
			Agent a;

			//////////////////////////////////////////////////////////////////////////////////////////
			// determine the household type //////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////////////////////

			double val = random.nextDouble();
			int index = getIndex(householdTypeRatios, val);
			
			// given the type, set up the household
			int hh1 = -1, hh2 = -1; // 0 for male, 1 for female
			int numChildren = 0;
			boolean under18 = false;
			boolean over65 = false;
			boolean familyGroup = true;
			
			switch (index) {
			case 0: // husband-wife family
				hh1 = 0; hh2 = 1;
				numChildren = ownChildrenDistribution();
				break;
			case 1: // husband-wife family, OWN CHILDREN < 18
				hh1 = 0; hh2 = 1;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 2: // single male householder WITH FAMILY
				hh1 = 0;
				numChildren = ownChildrenDistribution();
				break;
			case 3: // single male householder, OWN CHILDREN < 18
				hh1 = 0;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 4: // single female householder WITH FAMILY
				hh1 = 1;
				numChildren = ownChildrenDistribution();
				break;
			case 5: // single female householder, OWN CHILDREN < 18
				hh1 = 1;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 6: // nonfamily group living
				familyGroup = false;
				break;
			case 7: // lone male < 65
				hh1 = 0;
				familyGroup = false;
				break;
			case 8: // lone male >= 65
				hh1 = 0;
				familyGroup = false;
				over65 = true;
				break;
			case 9: // lone female < 65
				hh1 = 1;
				familyGroup = false;
				break;
			case 10: // lone female >= 65
				hh1 = 1;
				familyGroup = false;
				over65 = true;
				break;
			}
			
			//////////////////////////////////////////////////////////////////////////////////////////
			// construct the household ///////////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////////////////////

			int spouseAge = -1;
			int numAttempts = 1000;

			// select the Householder /////////////////////////////////////////////

			int attempts = Math.min(numAttempts, individuals.size()); // try at most 100 times to fill this slot
			while(attempts > 0 && hh1 >= 0){
				a = individuals.get(random.nextInt(individuals.size()));
				attempts--;
				if(a.sex == hh1 && a.age > 3){ // basic requirements

					if(over65 && a.age < 13) // if the householder needs to be a senior 
						continue;
					else if(!over65 && a.age >= 13) // if the householder specifically is not a senior
						continue;

					// if the householder needs to have children under 18
					if(under18 && a.age > 14)
						continue;

					household.add(a);
					spouseAge = a.age + (int)(1.5 * random.nextGaussian());	
					individuals.remove(a);
					attempts = -1;
				}
			}

			// add spouse, if appropriate /////////////////////////////////////////////
			attempts = Math.min(100, individuals.size()); // try at most 100 times to fill this slot

			while(attempts > 0 && hh2 >= 0){
				a = individuals.get(random.nextInt(individuals.size()));
				if(a.sex == hh2 && a.age == spouseAge){ // basic requirements						
					household.add(a);
					individuals.remove(a);
					attempts = -1;
				}
				attempts--;
			}

			// add children, if appropriate /////////////////////////////////////////////
			if(numChildren > 0){

				// get rough age parameters for the children of the householder and spouse
				int minAge = 17, maxAge = 0;
				for(Agent member: household){
					if(member.age > maxAge) maxAge = member.age;
					if(member.age < minAge) minAge = member.age;
				}
				// children should be younger than the minimum age of a parent minus 15 years (ASSUMPTION)
				int maxChildAge = minAge - 3;
				// children can be born to parents at no older than 50 (ASSUMPTION)
				int minChildAge = Math.max(0, maxAge - 9);//10);

				attempts = Math.min(numAttempts * numChildren, individuals.size()); // try at most 100 times to fill this slot
				int previousChildAge = -1; // use to try to cluster child ages together
				int fulfilled = 0;
				while(attempts > 0 && fulfilled < numChildren){
					attempts--;
					a = individuals.get(random.nextInt(individuals.size()));
					if(under18 && a.age > 4 && fulfilled == 0) // must have at least one child under 18 to qualify
						continue;
					if(a.age >= minChildAge && a.age <= maxChildAge){ // basic requirements

						// prefer children to be closer in age to one another (normal distribution --> within 5 years, certainly within 10)
						if(Math.abs(a.age - previousChildAge) > Math.abs(random.nextGaussian()))
							continue;

						household.add(a);
						individuals.remove(a);
						fulfilled++;
						previousChildAge = a.age;
					}
				}

			}

			// NON-FAMILY HOUSEHOLD: add roommates who are adults //////////////////////
			if(hh1 == -1 && hh2 == -1 && !familyGroup){

				attempts = Math.min(numAttempts, individuals.size()); // try at most 100 times to fill this slot
				int numMembers = otherAdultsDistribution();
				int fulfilled = 0;

				while(attempts > 0 && fulfilled < numMembers){
					a = individuals.get(random.nextInt(individuals.size()));
					if(a.age > 4){ 
						household.add(a);
						individuals.remove(a);
						fulfilled++;
					}
					attempts--;
				}
			}

			if(household.size() > 0){
				if(familyGroup)
					familyHouseholds.add(household);
				else
					allHouseholds.add(household);
			}
			else {
				i--;
			}

		}

		//////////////////////////////////////////////////////////////////////////////////////////
		// allocated unassigned individuals //////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		int leftoverIndividualsToAllocate = individuals.size();
		while(leftoverIndividualsToAllocate > 0){
			Agent member = individuals.remove(random.nextInt(leftoverIndividualsToAllocate));
			familyHouseholds.get(random.nextInt(familyHouseholds.size())).add(member);
			leftoverIndividualsToAllocate--;
		}
	
		//////////////////////////////////////////////////////////////////////////////////////////
		// set up basic household social networks ////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		for(ArrayList <Agent> household: familyHouseholds){
			int weight = familyWeight;
			for(int i = 0; i < household.size()-1; i++){				
				for(int j = i+1; j < household.size(); j++){
					household.get(i).addContact(household.get(j), weight);
					household.get(j).addContact(household.get(i), weight);					
				}
			}
		}
		
		for(ArrayList <Agent> household: allHouseholds){
			int weight = friendWeight;
			for(int i = 0; i < household.size()-1; i++){				
				for(int j = i+1; j < household.size(); j++){
					household.get(i).addContact(household.get(j), weight);
					household.get(j).addContact(household.get(i), weight);					
				}
			}			
		}
		
		if(groupPopulation.size() > 10)
			sociallyCluster(groupPopulation, acquaintenceWeight);
		else // fully connected
			for(int i = 0; i < groupPopulation.size(); i++){
				Agent a = groupPopulation.get(i);
				for(int j = i + 1; j < groupPopulation.size(); j++){
					Agent b = groupPopulation.get(j);
					a.addContact(b, acquaintenceWeight);
					b.addContact(a, acquaintenceWeight);
				}
			}
		
		//////////////////////////////////////////////////////////////////////////////////////////
		// clean up the structures ///////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		allHouseholds.addAll(familyHouseholds); // combine the two sets of household types
		fitOfHouseholds(area, allHouseholds, familyHouseholds); // report on the fit of households
		if(groupPopulation.size() > 0)
			allHouseholds.add(groupPopulation); // add the group population back in, treating it all as one household
		
		return allHouseholds;
	}
	
	/**
	 * For testing
	 * @param area
	 * @param individuals
	 * @return
	 */
	ArrayList <ArrayList<Agent>> generateSubsetOfHouseholds(MasonGeometry area, ArrayList <Agent> individuals){
		
		ArrayList <ArrayList<Agent>> allHouseholds = new ArrayList <ArrayList <Agent>> ();
		ArrayList <ArrayList<Agent>> familyHouseholds = new ArrayList <ArrayList <Agent>>();

		
		// read in the constraints on household types
		double [] householdTypeRatios = getHouseholdConstraints(area);
						
		// GENERATE THE HOUSEHOLDS
		int numHouseholds = area.getIntegerAttribute("DP0130001");	

		for(int i = 0; i < Math.min(numHouseholds, 10); i++){
			
			if(individuals.size() == 0) 
				continue;
			
			ArrayList <Agent> household = new ArrayList <Agent> ();
			Agent a;

			//////////////////////////////////////////////////////////////////////////////////////////
			// determine the household type //////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////////////////////

			double val = random.nextDouble();
			int index = getIndex(householdTypeRatios, val);
			
			// given the type, set up the household
			int hh1 = -1, hh2 = -1; // 0 for male, 1 for female
			int numChildren = 0;
			boolean under18 = false;
			boolean over65 = false;
			boolean familyGroup = true;
			
			switch (index) {
			case 0: // husband-wife family
				hh1 = 0; hh2 = 1;
				numChildren = ownChildrenDistribution();
				break;
			case 1: // husband-wife family, OWN CHILDREN < 18
				hh1 = 0; hh2 = 1;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 2: // single male householder WITH FAMILY
				hh1 = 0;
				numChildren = ownChildrenDistribution();
				break;
			case 3: // single male householder, OWN CHILDREN < 18
				hh1 = 0;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 4: // single female householder WITH FAMILY
				hh1 = 1;
				numChildren = ownChildrenDistribution();
				break;
			case 5: // single female householder, OWN CHILDREN < 18
				hh1 = 0;
				under18 = true;
				numChildren = ownChildrenDistribution();
				break;
			case 6: // nonfamily group living
				familyGroup = false;
				break;
			case 7: // lone male < 65
				hh1 = 0;
				familyGroup = false;
				break;
			case 8: // lone male >= 65
				hh1 = 0;
				familyGroup = false;
				over65 = true;
				break;
			case 9: // lone female < 65
				hh1 = 1;
				familyGroup = false;
				break;
			case 10: // lone female >= 65
				hh1 = 1;
				familyGroup = false;
				over65 = true;
				break;
			}
			
			//////////////////////////////////////////////////////////////////////////////////////////
			// construct the household ///////////////////////////////////////////////////////////////
			//////////////////////////////////////////////////////////////////////////////////////////

			int spouseAge = -1;
			int numAttempts = 1000;

			// select the Householder /////////////////////////////////////////////

			int attempts = Math.min(numAttempts, individuals.size()); // try at most 100 times to fill this slot
			while(attempts > 0 && hh1 >= 0){
				a = individuals.get(random.nextInt(individuals.size()));
				if(a.sex == hh1 && a.age > 3){ // basic requirements

					if(over65 && a.age < 13) // if the householder needs to be a senior 
						continue;
					else if(!over65 && a.age >= 13) // if the householder specifically is not a senior
						continue;

					// if the householder needs to have children under 18
					if(under18 && a.age > 14)
						continue;

					household.add(a);
					spouseAge = a.age + (int)(1.5 * random.nextGaussian());	
					individuals.remove(a);
					attempts = -1;
				}
				attempts--;
			}

			// add spouse, if appropriate /////////////////////////////////////////////
			attempts = Math.min(100, individuals.size()); // try at most 100 times to fill this slot

			while(attempts > 0 && hh2 >= 0){
				a = individuals.get(random.nextInt(individuals.size()));
				if(a.sex == hh2 && a.age == spouseAge){ // basic requirements						
					household.add(a);
					individuals.remove(a);
					attempts = -1;
				}
				attempts--;
			}

			// add children, if appropriate /////////////////////////////////////////////
			if(numChildren > 0){

				// get rough age parameters for the children of the householder and spouse
				int minAge = 17, maxAge = 0;
				for(Agent member: household){
					if(member.age > maxAge) maxAge = member.age;
					if(member.age < minAge) minAge = member.age;
				}
				// children should be younger than the minimum age of a parent minus 15 years (ASSUMPTION)
				int maxChildAge = minAge - 3;
				// children can be born to parents at no older than 50 (ASSUMPTION)
				int minChildAge = Math.max(0, maxAge - 9);//10);

				attempts = Math.min(numAttempts * numChildren, individuals.size()); // try at most 100 times to fill this slot
				int previousChildAge = -1; // use to try to cluster child ages together
				int fulfilled = 0;
				while(attempts > 0 && fulfilled < numChildren){
					attempts--;
					a = individuals.get(random.nextInt(individuals.size()));
					if(under18 && a.age > 4 && fulfilled == 0) // must have at least one child under 18 to qualify
						continue;
					if(a.age >= minChildAge && a.age <= maxChildAge){ // basic requirements

						// prefer children to be closer in age to one another (normal distribution --> within 5 years, certainly within 10)
						if(Math.abs(a.age - previousChildAge) > Math.abs(random.nextGaussian()))
							continue;

						household.add(a);
						individuals.remove(a);
						fulfilled++;
						previousChildAge = a.age;
					}
				}

			}

			// NON-FAMILY HOUSEHOLD: add roommates who are adults //////////////////////
			if(hh1 == -1 && hh2 == -1 && !familyGroup){

				attempts = Math.min(numAttempts, individuals.size()); // try at most 100 times to fill this slot
				int numMembers = otherAdultsDistribution();
				int fulfilled = 0;

				while(attempts > 0 && fulfilled < numMembers){
					a = individuals.get(random.nextInt(individuals.size()));
					if(a.age > 4){ 
						household.add(a);
						individuals.remove(a);
						fulfilled++;
					}
					attempts--;
				}
			}

			if(household.size() > 0){
				if(familyGroup)
					familyHouseholds.add(household);
				else
					allHouseholds.add(household);
			}
			else {
				i--;
			}

		}

		//////////////////////////////////////////////////////////////////////////////////////////
		// clean up the structures ///////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		allHouseholds.addAll(familyHouseholds); // combine the two sets of household types
		
		ArrayList <Agent> copyOfIndividuals = new ArrayList <Agent> ();
		for(ArrayList <Agent> hs: allHouseholds){
			for(Agent a: hs){
				copyOfIndividuals.add(a);
			}
		}

		return allHouseholds;
	}
	
	/**
	 * Cluster the individuals, giving the generated relationships the given weight
	 * @param individuals
	 * @param weight
	 */
	void sociallyCluster(ArrayList <Agent> individuals, int weight){

		int indexy = -1;

		HashMap <Agent, Integer> assignedFriends = new HashMap <Agent, Integer> ();
		for(Agent a: individuals){
			int numConnections = 3 + 100 - (int) Distributions.nextPowLaw(6, 100, this.random);
			assignedFriends.put(a, numConnections);
			indexy++;
			if(indexy % 100 == 0) System.out.println(indexy + " out of " + individuals.size() + " distrib. assigned...");
		}
		
		indexy = -1;
		for(Agent a: individuals){
			indexy++;
			if(indexy % 100 == 0)
				System.out.println(indexy + " out of " + individuals.size());
			// power law-distributed number of connections, ranging from about 4 to 100
			int numConnections = assignedFriends.get(a) - a.socialTies.size();
			if(numConnections <= 0) continue; // this agent is already all set!
			
			// collect a group of unconnected individuals and weight them according to closeness and similarity
			HashMap <Agent, Double> socialTree = new HashMap <Agent, Double> ();
			
			// get social connections
			for(Agent b: a.socialTies.keySet()){
				for(Agent c: b.socialTies.keySet()){
					if(c == a) continue;  // don't add self
					if(!a.socialTies.containsKey(c) && c.socialTies.size() < assignedFriends.get(c)){
						double distance = a.getSocialDistance(c);
						socialTree.put(c, distance);
					}
				}
			}
			
			// get random connections
			for(int i = socialTree.size(); i < numConnections + 100; i++){
				Agent b = individuals.get(random.nextInt(individuals.size()));
				// don't add self, an existing friend, or someone already under consideration
				if(b == a || a.socialTies.containsKey(b) || socialTree.containsKey(b) || b.socialTies.size() >= assignedFriends.get(b)){
					continue; // don't add self
				}
				double distance = a.getSocialDistance(b);
				socialTree.put(b, distance);
			}

			// add the heaviest weighted individuals!
			for(int i = 0; i < numConnections; i++){
				Agent b = getMin(socialTree);
				if(b == null) continue;
				a.addContact(b, weight);
				b.addContact(a, weight);
				socialTree.remove(b);
			}
			
		}
		
	}
	
	/**
	 * Remember, it's DIRECTED. Following someone does not ensure that they'll follow you back!
	 * 
	 * @param individuals
	 * @param weight
	 * @param averageDegree
	 */
	void sociallyMediaCluster(ArrayList <Agent> individuals, int weight, double averageDegree){
		
		HashMap <Agent, Integer> assignedFriends = new HashMap <Agent, Integer> ();
		for(Agent a: individuals){
			int numConnections = 3 + 100 - (int) Distributions.nextPowLaw(6, 100, this.random);
			assignedFriends.put(a, numConnections);
		}

		for(Agent a: individuals){
			
			// power law-distributed number of connections, ranging from about 4 to 100
			int numConnections = 5 + 100 - (int) Distributions.nextPowLaw(6, 100, this.random)
					- a.socialMediaTies.size(); // don't count beyond what the agent already has!
			
			// collect a group of unconnected individuals and weight them according to closeness and similarity
			HashMap <Agent, Double> socialTree = new HashMap <Agent, Double> ();
			
			// get social connections
			for(Agent b: a.socialTies.keySet()){
				for(Agent c: b.socialTies.keySet()){
					if(c == a) continue;  // don't add self
					if(c.socialMediaTies == null) continue; // don't add non-users!
					if(!a.socialMediaTies.contains(c) && c.socialMediaTies.size() < assignedFriends.get(c)){
						double distance = a.getSocialDistance(c);
						socialTree.put(c, distance);
					}
				}
			}
			
			// get random connections
			for(int i = socialTree.size(); i < numConnections + 100; i++){
				Agent b = individuals.get(random.nextInt(individuals.size()));
				if(b == a) continue; // don't add self
				if(a.socialMediaTies.contains(b) || b.socialMediaTies.size() < assignedFriends.get(b)) continue;
				double distance = a.getSocialDistance(b);
				socialTree.put(b, distance);
			}

			// add the heaviest weighted individuals!
			for(int i = 0; i < numConnections; i++){
				Agent b = getMin(socialTree);
				if(b == null) continue;
				a.addMediaContact(b);
				socialTree.remove(b);
			}
		}
		
	}
	
	Agent getMin(HashMap <Agent, Double> map){
		double minVal = Double.MAX_VALUE;
		Agent best = null;
		for(Agent a: map.keySet()){
			if(map.get(a) < minVal){
				minVal = map.get(a);
				best = a;
			}
		}
		return best;
	}
	
	///////////////////////////////////////////////////////////////
	// UTILITIES
	///////////////////////////////////////////////////////////////

	int ownChildrenDistribution(){ return random.nextInt(2) + 1;}	
	int otherAdultsDistribution(){ return random.nextInt(2) + 2;}
	
	/**
	 * Test the fit between a generated population and the age and sex constraints upon it
	 * 
	 * @param individuals - the list of individuals
	 * @param ageSexConstraints - a double array representing the ideal ratios of population. Here, the 
	 * 		array is assumed to be structured so that all the ratios of each age group of one sex are presented,
	 * 		then the next. This could easily be extended to deal with more than two sexes.
	 *  
	 * @return a Pearson's Chi-squared fit of the results with the expected ratios  
	 */
	double fitIndividuals(ArrayList <Agent> individuals, double [] ageSexConstraints){

		// structures to hold information
		double [] counts = new double [ageSexConstraints.length];
		int ageCategories = ageSexConstraints.length / 2; // CHANGE HERE to increase # sexes
		
		// calculate the distribution based on the set of individuals
		for(Agent a: individuals){
			int index = ageCategories * a.sex + a.age;
			counts[index]++;
		}
		
	//	System.out.println("FIT MEASURE:");
		// calculate Pearson's chi-squared
		double total = 0;
		double totalPop = individuals.size();
		for(int i = 0; i < ageSexConstraints.length; i++){
			double expected = totalPop * ageSexConstraints[i];
			if(expected == 0)
				expected = .001;
				//continue;
			total += Math.pow((counts[i] - expected), 2)/expected;
		//	System.out.println(counts[i] +  "\t" + expected);
		}
		
		return total;
	}
	
	/**
	 * Test how well the generated households compare to the data
	 * 
	 * @param area
	 * @param households
	 * @param familyHouseholds
	 */
	void fitOfHouseholds(MasonGeometry area, ArrayList <ArrayList <Agent>> households, ArrayList <ArrayList<Agent>> familyHouseholds){

		// TEST THE FIT
		int peopleInFamily = 0;
		int peopleInHouseholds= 0;
		int householdsUnder18 = 0;
		int householdsOver65 = 0;
		ArrayList <Agent> individuals = new ArrayList <Agent> ();
		
		for(ArrayList <Agent> h: households){
			if(h.size() == 0)
				System.out.print("EMPTY HOUSE");
			if(familyHouseholds.contains(h)) peopleInFamily += h.size();
			peopleInHouseholds += h.size();
			
			boolean under18 = false;
			boolean over65 = false;
			
			for(Agent ha: h){
				
				// Verbose household output
/*				System.out.print((1 + ha.age) * 5 + " ");
				if(ha.sex == 0)
					System.out.print("M\t");
				else if(ha.sex == 1)
					System.out.print("F\t");
	*/			 
				if(ha.age < 4) under18 = true;
				if(ha.age > 12) over65 = true;
				individuals.add(ha);
			}
			if(under18) householdsUnder18++;
			if(over65) householdsOver65++;
		//	 System.out.println(); // used for verbose output
		}

		/*
		// VERBOSE 
		System.out.println("TRACT: " + area.getStringAttribute("NAMELSAD10"));
		System.out.println("Total Pop: " + individuals.size() + " vs " + area.getIntegerAttribute("DP0120002"));
		System.out.println("Avg In Family: " + (peopleInFamily / (double) familyHouseholds.size()) + " vs " + area.getDoubleAttribute("DP0170001"));
		System.out.println("Avg In Household: " + (peopleInHouseholds / (double) households.size()) + " vs " + area.getDoubleAttribute("DP0160001"));
		System.out.println("Households w/ under 18: " + householdsUnder18 + " vs " + area.getIntegerAttribute("DP0140001"));
		System.out.println("Households w/ over 65: " + householdsOver65 + " vs " + area.getIntegerAttribute("DP0150001"));
		System.out.println("FIT OF HOUSEHOLDS: " + fitIndividuals(individuals, ageSexConstraints));
		System.out.println();
		*/
		
		
		// TAB-DELIMITED
		System.out.print(area.getStringAttribute("NAMELSAD10") + "\t"); // tract
		System.out.print(individuals.size() + "\t" + area.getIntegerAttribute("DP0120002") + "\t" + 
				area.getIntegerAttribute("DP0120014") + "\t"); // total vs in households vs group
		System.out.print(households.size() + "\t" + area.getIntegerAttribute("DP0130001") + "\t"); // number of households + generated households
		System.out.print((peopleInFamily / (double) familyHouseholds.size())  + "\t" + area.getDoubleAttribute("DP0170001") + "\t"); // avg in family vs TRUE
		System.out.print((peopleInHouseholds / (double) households.size())  + "\t" + area.getDoubleAttribute("DP0160001") + "\t"); // avg in household vs TRUE
		System.out.print(householdsUnder18  + "\t" + area.getIntegerAttribute("DP0140001") + "\t"); // households < 18 vs TRUE
		System.out.print(householdsOver65  + "\t" + area.getIntegerAttribute("DP0150001") + "\t"); // households > 65 vs TRUE
		System.out.println(fitIndividuals(individuals, getAgeSexConstraints(area)));
	}

	
	/**
	 * @param vals - a set of ratios which sum to 1
	 * @param val - a value between 0 and 1
	 * @return the bin of vals into which val fits
	 */
	int getIndex(double [] vals, double val){
		double count = 0;
		for(int j = 0; j < vals.length; j++){
			count += vals[j];
			if(val <= count) return j;
		}
		return vals.length - 1;
	}

	/**
	 * 
	 * @param distribution
	 * @param val
	 * @return
	 */
	String getIndex(HashMap <String, Double> distribution, double val){
		double count = 0;
		for(String index: distribution.keySet()){
			count += distribution.get(index);
			if(val <= count) return index;
		}
		return null;
	}

	/**
	 * read in the geometries associated with a shapefile
	 * 
	 * @param filename - the file to read in
	 * @return a GeomVectorField containing the geometries
	 */
	GeomVectorField readInVectors(String filename){
		GeomVectorField field = new GeomVectorField();
		try {
			System.out.print("Reading in file...");
			File file = new File(filename);
			ShapeFileImporter.read(file.toURL(), field);
			System.out.println("done");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return field;
	}
	
	double [] getSumByCol(double [] popMatrix){
		double [] result = new double [18];
		
		for(int i = 0; i < result.length; i++)
			result[i] = popMatrix[i] + popMatrix[i + 18];
		
		return result;
	}
	
	double [] getSumByRow(double [] popMatrix){
		double [] result = new double [2];
		
		for(int i = 0; i < popMatrix.length; i++)
			result[ (int)Math.floor(i / 18)] += popMatrix[i];
		
		return result;
	}
	
	
	ArrayList <Agent> getSocialMediaUsers(ArrayList <Agent> individuals){//double [] ageSexPopConstraints){

		try {

			double[] ageSexPopConstraints = new double[36];
			for (Agent a : individuals) {
				int aIndex = a.age + a.sex * 18;
				ageSexPopConstraints[aIndex]++;
			}

			double[] totalPopSexCounts = getSumByRow(ageSexPopConstraints);
			double[] totalPopAgeCounts = getSumByCol(ageSexPopConstraints);

			// Open the tracts file
			FileInputStream fstream = new FileInputStream(
					socialMediaUsageFilename);

			// Convert our input stream to a BufferedReader
			BufferedReader d = new BufferedReader(
					new InputStreamReader(fstream));

			String s;
			d.readLine(); // header

			// read in sex ratios
			// //////////////////////////////////////////////////////

			d.readLine(); // section header: sex

			double[] sexSocialMediaUsageCounts = new double[2];

			// multiply the percent of internet users by the percent of social media users to get the percent of total pop using 
			s = d.readLine();
			String[] bits = s.split("\t");
			if (bits[0].equals("Men")){ // 
				sexSocialMediaUsageCounts[0] = Integer.parseInt(bits[2])  * totalPopSexCounts[0] * Integer.parseInt(bits[1]) / 10000.;
			}
			s = d.readLine();
			bits = s.split("\t");
			if (bits[0].equals("Women")){
				sexSocialMediaUsageCounts[1] = Integer.parseInt(bits[2]) * totalPopSexCounts[1] * Integer.parseInt(bits[1]) / 10000.;
			}

			// read in age ratios
			// //////////////////////////////////////////////////////

			d.readLine(); // next section header
			int index = 0;
			double[] ageSocialMediaUsageCounts = new double[18];

			while ((s = d.readLine()) != null) {
				bits = s.split("\t");
				int minAgeInGroup = (int) Math.floor(Integer.parseInt(bits[0]) / 5); // get the appropriate bin
				int maxAgeInGroup = (int) Math.floor(Integer.parseInt(bits[1]) / 5); // get the appropriate bin

				// an unconsidered group, apparently
				while (index < minAgeInGroup) {
					ageSocialMediaUsageCounts[index] = 0;
					index++;
				}

				// multiply the percent of internet users by the percent of social media users to get the percent of total pop using 
				double perHundredSocialMedia = Integer.parseInt(bits[3]) * Integer.parseInt(bits[2]) / 10000.;

				for (; index <= maxAgeInGroup; index++) {
					ageSocialMediaUsageCounts[index] = perHundredSocialMedia * totalPopAgeCounts[index];
				}
			}
			while (index < 18) {
				ageSocialMediaUsageCounts[index] = 0;
				index++;
			}

			// IPF
			// //////////////////////////////////////////////////////////////////

			double[] oldRatios = new double[36], newCounts = new double[36];

			// initially set everyone with the same number of people
			double initialVal = (totalPopSexCounts[0] + totalPopSexCounts[1]) / 36;
			for (int i = 0; i < 36; i++)
				newCounts[i] = initialVal;
			boolean finished = false;

			while (!finished) {
				oldRatios = newCounts;
				newCounts = new double[36];

				// sex constraints

				// calculate current sex parameters
				double[] currentSexSizes = new double[2];
				for (int i = 0; i < currentSexSizes.length; i++) {
					for (int j = 0; j < 18; j++) {
						currentSexSizes[i] += oldRatios[j + i * 18];
					}
				}
				// modify the matrix so that it reflects both the existing sex
				// distribution and the true sex distribution
				for (int i = 0; i < currentSexSizes.length; i++) {
					for (int j = 0; j < 18; j++) {
						newCounts[j + i * 18] = (oldRatios[j + i * 18] / currentSexSizes[i])
								* sexSocialMediaUsageCounts[i];
					}
				}

				// age constraints

				// calculate current age parameters
				double[] currentAgeSizes = new double[18];
				for (int i = 0; i < currentAgeSizes.length; i++) {
					for (int j = 0; j < 2; j++) {
						currentAgeSizes[i] += oldRatios[i + j * 18];
					}
				}
				// modify the matrix so that it reflects both the existing sex
				// distribution and the true sex distribution
				for (int i = 0; i < currentAgeSizes.length; i++) {
					for (int j = 0; j < 2; j++) {
						newCounts[i + j * 18] = (oldRatios[i + j * 18] / currentAgeSizes[i])
								* ageSocialMediaUsageCounts[i];
					}
				}

				finished = true; // unless found otherwise
				for (int i = 0; i < 36; i++) {
					if (Math.abs(oldRatios[i] - newCounts[i]) > 5) {
						finished = false;
						break;
					}
				}
			}

			// clean up
			d.close();

			for (int i = 0; i < newCounts.length; i++) {
				newCounts[i] /= ageSexPopConstraints[i];
			}

			ArrayList<Agent> socialMediaUsers = new ArrayList<Agent>();
			for (Agent a : individuals) {
				int aIndex = a.age + a.sex * 18;
				if (random.nextDouble() < newCounts[aIndex]) {
					a.socialMediaTies = new ArrayList<Agent>();
					socialMediaUsers.add(a);
				}
			}

			return socialMediaUsers;
		} catch (Exception e) {
			System.err.println("File input error");
		}

		return null;
	}
	
	public static void main(String [] args){
		PopSynth newpop = new PopSynth();		
	}
}