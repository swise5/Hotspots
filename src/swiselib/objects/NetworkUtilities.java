package swiselib.objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.geo.AttributeValue;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;
import swiselib.objects.network.ListEdge;
import swiselib.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

import ec.util.MersenneTwisterFast;

/**
 * Reads the roadLayer geometries into a MASON network and cleans up the resulting product so that
 * the road network consists only of a large, fully-connected component.
 */
public class NetworkUtilities{

	/**
	 * A helper function for networkCleanup()
	 * 
	 * @param c - the location whose node we are seeking
	 * @param field - the field containing the GeoNodes
	 * @return the GeoNode closest to the given Coordinate
	 */
	static GeoNode getNode(Coordinate c, Continuous2D field, GeomVectorField fields, HashMap <MasonGeometry, GeoNode []> linkToNodeMapping, 
			HashMap <MasonGeometry, ListEdge> geomToEdgeMapping, Network network, double resolution, GeometryFactory fa){
		Double2D loc = new Double2D(c.x, c.y);

		Bag b = field.getObjectsWithinDistance(loc, resolution);
		if(b == null || b.size() == 0){
						
			GeoNode node = new GeoNode(fa.createPoint(c));
			field.setObjectLocation(node, loc);
			
			// CHECK TO SEE IF IT BISECTS A ROAD
//			Bag edges = fields.getTouchingObjects(node);
			Bag edges = fields.getObjectsWithinDistance(node.geometry, resolution);
			if(edges != null && edges.size() > 0){
				for(Object o: edges){
					
//			        if(((MasonGeometry)o).getIntegerAttribute("index") == 11753)
//		        		System.out.println("foundya");

					// for each edge, break the edge down around this point
					MasonGeometry mg = (MasonGeometry) o;
					GeoNode [] nodes = linkToNodeMapping.get(mg);
					LineString myLine = (LineString) mg.getGeometry();
					
					network.removeEdge(geomToEdgeMapping.get(mg));

					GeoNode nStart = nodes[0];
					GeoNode nEnd = nodes[1];

					// determine which coordinates go on either side of the point
					ArrayList <Coordinate> front = new ArrayList <Coordinate> (), back = new ArrayList <Coordinate> ();
					int index = 0;
					
					LineString mySegment = null;
					Coordinate lastCoord = myLine.getCoordinateN(0);
					front.add(lastCoord);

					for(int i = 1; i < myLine.getCoordinates().length; i++){
						Coordinate coord = myLine.getCoordinateN(i);
						mySegment = fa.createLineString(new Coordinate[] {lastCoord, coord});

						if(node.geometry.distance(mySegment) < resolution){
							index = i;
							i = myLine.getCoordinates().length;
							front.add(node.geometry.getCoordinate());
							lastCoord = node.geometry.getCoordinate();
						}
						else{
							front.add(coord);
							lastCoord = coord;
						}
					}

					back.add(node.geometry.getCoordinate());
					for(int i = index; i < myLine.getCoordinates().length - 1; i++){
						Coordinate coord = myLine.getCoordinateN(i);
						back.add(coord);
					}
					back.add(nEnd.geometry.getCoordinate());
										
					// create the new geometries
					LineString frontLine = fa.createLineString(
							CoordinateArrays.removeRepeatedPoints(front.toArray(new Coordinate [front.size()])));
					LineString backLine = fa.createLineString(
							CoordinateArrays.removeRepeatedPoints(back.toArray(new Coordinate [back.size()])));
					MasonGeometry newFront = new MasonGeometry(frontLine);
					mg.geometry = backLine;

        			for(String attr: mg.getAttributes().keySet()){
        				Object value = ((AttributeValue)mg.getAttribute(attr)).getValue();
        				if(value instanceof String)
        					newFront.addStringAttribute(attr, (String)value);
        				else if(value instanceof Integer)
        					newFront.addIntegerAttribute(attr, (Integer)value);
        				else if(value instanceof Double)
        					newFront.addDoubleAttribute(attr, (Double)value);
        				else
        					newFront.addAttribute(attr, value);
        			}

					
					
			        fields.addGeometry(newFront);
			        PointMoveTo newPoint = new PointMoveTo();
			        newPoint.setCoordinate(backLine.getCoordinate());
			        mg.geometry.apply(newPoint);
			        mg.geometry.geometryChanged();
			        
			        linkToNodeMapping.put(newFront, new GeoNode [] {nStart, node});
			        linkToNodeMapping.put(mg, new GeoNode [] {node, nEnd});

			        // create the edge and add it to the structure
			        Edge edFrontEd = new Edge(nStart, node, newFront);
			        ListEdge edFront = new ListEdge(edFrontEd, nStart.getGeometry().distance(node.getGeometry()));

			        Edge edBackEd = new Edge(node, nEnd, mg);
			        ListEdge edBack = new ListEdge(edBackEd, nEnd.getGeometry().distance(node.getGeometry()));

					// add the geometries to the network
			        
			        testEdgeForIssues(edFront);
			        testEdgeForIssues(edBack);
			        
		        	network.addEdge(edFront);
		        	network.addEdge(edBack);
		        	
		        	geomToEdgeMapping.put(newFront, edFront);
		        	geomToEdgeMapping.put(mg, edBack);
		        	
		        	

		        	return node; // ONLY RUN IT ONCE
				}
			}
			
			return node;
		}
		else
			return (GeoNode)b.get(0);
	}

	static void testEdgeForIssues(ListEdge e){
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				Coordinate c1 = ls.getCoordinateN(0);
				Coordinate c2 = ls.getCoordinateN(ls.getNumPoints()-1);
				GeoNode g1 = (GeoNode) e.getFrom();
				GeoNode g2 = (GeoNode) e.getTo();
				if(c1.distance(g1.geometry.getCoordinate()) > 1)
					System.out.println("found you");
				if(c2.distance(g2.geometry.getCoordinate()) > 1)
					System.out.println("found you");
	}
	
	static void testNetworkForIssues(Network n){
		System.out.println("testing");
		for(Object o: n.allNodes){
			GeoNode node = (GeoNode) o;
			for(Object p: n.getEdgesOut(node)){
				sim.field.network.Edge e = (sim.field.network.Edge) p;
				LineString ls = (LineString)((MasonGeometry)e.info).geometry;
				Coordinate c1 = ls.getCoordinateN(0);
				Coordinate c2 = ls.getCoordinateN(ls.getNumPoints()-1);
				GeoNode g1 = (GeoNode) e.getFrom();
				GeoNode g2 = (GeoNode) e.getTo();
				if(c1.distance(g1.geometry.getCoordinate()) > 1)
					System.out.println("found you");
				if(c2.distance(g2.geometry.getCoordinate()) > 1)
					System.out.println("found you");
			}
		}
	}

	static GeoNode checkForNode(Coordinate c, Continuous2D field, double resolution){
		Double2D loc = new Double2D(c.x, c.y);
		Bag b = field.getObjectsWithinDistance(loc, resolution);
		if(b == null || b.size() == 0)
			return null;
		else
			return (GeoNode) b.get(0);
	}
	
	/**
	 * Reads the roadLayer geometries into a MASON network and cleans up the resulting product so that
	 * the road network consists only of a large, fully-connected component.
	 */
	public static Network multipartNetworkCleanup(GeomVectorField roadLayer, Bag roadNodes, double resolution, 
			GeometryFactory fa, MersenneTwisterFast random, double roadWidth){

		// read the geometries from a GeomVectorField into a proper MASON Network
		
		Continuous2D nodesInWorld = new Continuous2D(resolution, roadLayer.fieldWidth, roadLayer.fieldHeight);
		
		GeomVectorField addedRoads = new GeomVectorField();
		
		HashMap <MasonGeometry, GeoNode []> linkToNodeMapping = new HashMap <MasonGeometry, GeoNode []> (); 
		HashMap <MasonGeometry, ListEdge> geometryToEdgeMapping = new HashMap <MasonGeometry, ListEdge> (); 
		
		Bag geometries = roadLayer.getGeometries();
		Network net = new Network(false);
		
		int testIndex = 0;
		
		for(Object o: geometries){
			
			// we're only dealing with LineStrings here!
			if(((MasonGeometry)o).geometry instanceof LineString){
								
		        LineString line = (LineString) ((MasonGeometry)o).geometry;
		        if (line.isEmpty()) continue; // empty geometry
		        
		        Coordinate[] linePts = CoordinateArrays.removeRepeatedPoints(line.getCoordinates());

		        if (linePts.length < 2) continue; // an unconnected point

	        	Coordinate startPt = linePts[0];
		        GeoNode nStart = getNode(startPt, nodesInWorld, addedRoads, linkToNodeMapping, geometryToEdgeMapping, net, resolution, fa);
		        startPt = nStart.getGeometry().getCoordinate();
	        	
		        ArrayList <Coordinate> points = new ArrayList <Coordinate> ();
		        points.add(startPt);
		        Coordinate lastPt = startPt;
		        
//		        if(((MasonGeometry)o).getIntegerAttribute("index") == 11753)
//		        		System.out.println("foundya");
		        
	        	for(int i = 1; i < linePts.length; i++){
	        		Coordinate c = linePts[i];
	        		
	        		if(lastPt.distance(c) == 0) 
	        			continue; // not a different point, in fact
	        		
	        		// check to see if this node already exists in the world
	        		GeoNode thisNode = checkForNode(c, nodesInWorld, resolution);
	        		
	        		// it's the last point, create it regardless
	        		if(thisNode == null && i == linePts.length - 1){ 
	        			thisNode = getNode(c, nodesInWorld, addedRoads, linkToNodeMapping, geometryToEdgeMapping, net, resolution, fa);
	        		}
	        		
	        		// if it's just another point, add it in
	        		if(thisNode == null){
		        		lastPt = c;
		        		points.add(c);	        			
	        		}
	        		
	        		// otherwise, given that we've found a node, create the edge
	        		else {
	        			c = thisNode.getGeometry().getCoordinate();
	        			points.add(c);
	        			
	        			// create the edge
	        			LineString mgLS = fa.createLineString(points.toArray(new Coordinate [points.size()]));
	        			MasonGeometry mg = new MasonGeometry(mgLS);
	        			for(String attr: ((MasonGeometry)o).getAttributes().keySet()){
	        				Object value = ((AttributeValue)((MasonGeometry)o).getAttribute(attr)).getValue();
	        				if(value instanceof String)
	        					mg.addStringAttribute(attr, (String)value);
	        				else if(value instanceof Integer)
	        					mg.addIntegerAttribute(attr, (Integer)value);
	        				else if(value instanceof Double)
	        					mg.addDoubleAttribute(attr, (Double)value);
	        				else
	        					mg.addAttribute(attr, value);
	        			}
	        			
	        			// add all appropriate records
	        			addedRoads.addGeometry(mg);
	    		        linkToNodeMapping.put(mg, new GeoNode [] {nStart, thisNode});

	    		        ListEdge ed = new ListEdge(new Edge(nStart, thisNode, mg), ((LineString)mg.geometry).getLength());
	    	        	net.addEdge(ed);
	    	        	testEdgeForIssues(ed);
	    	        	geometryToEdgeMapping.put(mg, ed);

	    	        	// prep for the new edge
	    	        	points = new ArrayList <Coordinate> ();
	    	        	points.add(c);
	    	        	nStart = thisNode;
	    	        	startPt = c;
	    	        	lastPt = c;
	        		}
	        	}
			}
/*			if(testIndex % 100 == 0)
				System.out.println(testIndex + " of out " + geometries.size());
			testIndex++;
	*/	}
		
	//	testNetworkForIssues(net);
		
		// clean up the MASON network
		
		// get all of the nodes associated with the largest connected subcomponent we can find
		roadNodes = new Bag();
		roadNodes.addAll(getSubcomponents(net, random));
		
		// remove all nodes that are not part of that largest connected subcomponent from the network
		Bag badNodes = new Bag(net.getAllNodes());
		badNodes.removeAll(roadNodes);		
		for(Object o: badNodes){
			net.removeNode(o);
		}
		
	//	testNetworkForIssues(net);
		// save the results of this cleanup to the global variables
		roadNodes = new Bag();
		roadNodes.addAll(net.getAllNodes());
		return(net);
	}
	
	/**
	 * Takes a network and returns a large connected subcomponent
	 * @param network - the network of nodes
	 * @return a bag of all the nodes in the largest connected component found
	 */
	static Bag getSubcomponents(Network network, MersenneTwisterFast random){
		
		// record-keeping
		ArrayList <Bag> subcomponents = new ArrayList <Bag> ();
		int biggestIndex = -1;
		int biggestSize = -1;
		
		Bag nodes = new Bag(network.getAllNodes());
//		int count = nodes.size() / 50;
		int count = (int) Math.max(1, Math.log10(nodes.size()));
		
		// pass over the list of nodes, selecting a node at random from the remaining pool
		// and searching until all of the nodes connected to it have been found
		for(int i = 0; i < count; i++){
			
			// select a node at random from those which remain
			GeoNode n = ((GeoNode) nodes.get(random.nextInt(nodes.size())));
			
			HashSet closedSet = new HashSet();
			ArrayList openSet = new ArrayList();
			Bag found = new Bag();
			
			openSet.add(n); // add the root node for consideration
			
			// so long as there are more nodes whose connections have not yet been
			// fully explored, continue looking
			while(openSet.size() > 0){
				
				GeoNode nprime = (GeoNode) openSet.remove(openSet.size() - 1);
				found.add(nprime);
				closedSet.add(nprime);
				
				// consider all connected nodes
				for(Object o: network.getEdges(nprime, null)){
					
					Edge e = (Edge) o;
					GeoNode n2 = ((GeoNode)e.getOtherNode(nprime));
					
					// if this is a new node, add it
					if(! (closedSet.contains(n2) || openSet.contains(n2))){
						openSet.add(n2);
					}
				}
			}
			
			// store this newly found connected component
			subcomponents.add(found);
			if(closedSet.size() > biggestSize){
				biggestSize = closedSet.size();
				biggestIndex = i;
			}
			
			// clean up and, conditionally, terminate the search
			nodes.removeAll(found);
			int s = nodes.size();
			if(s == 0 || s < biggestSize) 
				i = count;
		}

		return(subcomponents.get(biggestIndex));
	}
	
	/**
	 * Given a subgraph of a network and a GeoNode which is not yet part of the subgraph, adds a
	 * set of nodes and edges which most efficiently connects the GeoNode to the subgraph
	 * @param subgraph - the smaller network
	 * @param all - the network which contains all of subgraph and target
	 * @param target - the GeoNode to be incorporated into subgraph
	 * @return a network which contains all of subgraph and a path to target
	 */
	public static ArrayList <Edge> findLinkTo(Network subgraph, Network all, GeoNode target, AStar astar){
		
		//astarPath(Node start, ArrayList <Node> goal, Network network)
		ArrayList <GeoNode> ns = new ArrayList <GeoNode> ();
		for(Object o: subgraph.getAllNodes()){
			ns.add((GeoNode)o);
		}

		return astar.astarPath(target, ns, all);
	}
	
	/**
	 * Uses A* to attach unconnected components of a subgraph as efficiently as is possible
	 * 
	 * @param subgraph - the subnetwork of unconnected components
	 * @param all - a more full network, of which subgraph is a subgraph (predictably enough)
	 */
	public static void attachUnconnectedComponents(Network subgraph, Network all){
		
		AStar astar = new AStar();

		// get an initial list of all of the connected components
		ArrayList <Bag> components = connectedComponents(subgraph);
		
		int i = 0, j = 1; // indices to help compare the graphs
		
		// while there are unconnected components and all of the combinations of components have
		// yet to be compared, continue trying to merge components
		while(components.size() > 1 && i < components.size() - 1){

			// the two subcomponents to connect
			Bag component1 = components.get(i);
			Bag component2 = components.get(j);
			
			// the set of nodes contained in both components
//			ArrayList <Object> allNodes = new ArrayList <Object> ();
/*			HashSet <Object> allNodes = new HashSet <Object> ();
			allNodes.addAll(component1);
			allNodes.addAll(component2);
	*/		
			// calculate an A* path n the larger network between random elements of the two components 
//			ArrayList <Edge> es = astar.astarWeightedPath((GeoNode) component1.get(0), (GeoNode) component2.get(0), 
//					all, allNodes, 1.);
			ArrayList <Edge> es = astar.astarPath((GeoNode) component1.get(0), (GeoNode) component2.get(0), all);
			
			// if no such path exists, the components are truly disconnected. We do not attempt
			// to merge them further, and instead increase the indices 
			if(es == null){
				
				// reenter the pair of components so they are not lost
				components.add(i, component1);
				components.add(j, component2);
				
				// if our second component is the last in the set, increase i; otherwise increment j
				if(++j > components.size() - 1) { 
					i++;
					j = i + 1;
				}
				continue; // DO NOT try to add the nonexistant edges to this set!
			}
			
			// otherwise, proceed with merging!
			components.remove(j); // get rid of old version of component 2 FIRST (or it will mess up order)
			components.remove(i); // get rid of old version of component 1

			
			// go through the connecting edges and add their edges to the graph and their nodes to the components
			for(Edge e: es){
				
				Object from = e.from();
				Object to = e.to();
				
				// don't want to re-add component-internal edges
				if( (component1.contains(from) && component1.contains(to)) || 
					(component2.contains(from) && component2.contains(to))) 
						continue;
				else{
					subgraph.addEdge(from, to, e.info);
					
					// add newly found nodes to component1, to which component2 will be added
					// (we forbear combining the two until lest we exclude edges that go directly between
					// nodes from C1 and C2)
					if(! (component1.contains(from) || component2.contains(from))) component1.add(from);
					if(! (component1.contains(to) || component2.contains(to))) component1.add(to);
				}
			}
			
			// merge the two sets of nodes
			component1.addAll(component2);

			// save the component
			components.add(i, component1);			
		}
		
	}
	
	/**
	 * Groups nodes into connected components
	 * @param graph - the network of nodes
	 * @return an ArrayList of Bag of connected nodes
	 */
	public static ArrayList <Bag> connectedComponents(Network graph){
		ArrayList <Bag> subcomponents = new ArrayList <Bag> ();
		Bag nodes = new Bag(graph.getAllNodes());
		
		// create components as long as nodes exist which have not been assigned to a component
		while(nodes.size() > 0){
			
			Bag connected = new Bag(); // the component's nodes
			Bag toExplore = new Bag(); // the nodes yet to explore

			// begin a new component based around the first remaining node
			Object n = nodes.remove(nodes.size() - 1);
			connected.add(n);
			toExplore.add(n);

			// while there are nodes associated with the root node to explore, continue
			// perusing their links and adding associated nodes
			while(toExplore.size() > 0){
				
				n = toExplore.remove(toExplore.size() - 1); // explore the first in the set
				
				// consider all of the nodes to which this newly considered node connects
				for(Object o: graph.getEdges(n, null)){
					
					Edge e = (Edge) o;
					Object other = null;
					if(e.from() == n) other = e.to();
					else other = e.from();
					
					// if we haven't already found and stored this node, add it to the list
					// of nodes to consider in the future
					if(! connected.contains(other)){
						connected.add(other);
						toExplore.add(other);
					}	
				}
				
			}
			
			// remove all of the nodes we've found from the list of unclassified nodes
			nodes.removeAll(connected); 
			
			// store our newly-found component
			subcomponents.add(connected);
		}
		
		return subcomponents;
	}
	
	public static boolean validPath(ArrayList <Edge> path, Network network){

		if(path == null)// || path.size() == 0) // get rid of degenerate paths
			return false;

		for(Edge e: path){
			if(((MasonGeometry)e.info).getStringAttribute("open").equals("CLOSED"))
					return false;
		}
		return true;
	}
		

}
