/**
 ** AStar.java
 **
 ** Copyright 2011 by Andrew Crooks, Sarah Wise, Mark Coletti, and
 ** George Mason University.
 **
 ** Licensed under the Academic Free License version 3.0
 **
 ** See the file "LICENSE" for more information
 **
 ** $Id: AStar.java 679 2012-06-24 21:30:28Z mcoletti $
 **/
package swiselib.objects;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.planargraph.DirectedEdgeStar;
import com.vividsolutions.jts.planargraph.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import sim.field.network.Edge;
import sim.field.network.Network;
import sim.util.geo.GeomPlanarGraphDirectedEdge;
import swiselib.objects.network.GeoNode;

/**
 * a path-planning object used by the Agent to move around the environment
 * 
 * @author swise
 *
 */
@SuppressWarnings("restriction")
public class AStar
{

	/**
	 * Finds a path between the start and goal nodes within the given network
	 * 
	 * @param start - the node from which to start
	 * @param goal - the goal node
	 * @param network - the network in which the path should exist
	 * @return either an ordered list of the connecting edges or else null
	 */
	public ArrayList<Edge> astarPath(GeoNode start, GeoNode goal, Network network)
   {

        // initial check
        if (start == null || goal == null){
            System.out.println("Error: invalid node provided to AStar");
            return null;
        }

        // if they're the same place, the path is empty but certainly exists
        if(start == goal)
        	return new ArrayList<Edge> ();

        // containers for the metainformation about the Nodes relative to the
        // A* search
        HashMap<GeoNode, AStarNodeWrapper> foundNodes =
            new HashMap<GeoNode, AStarNodeWrapper>();

        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
        AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
        foundNodes.put(start, startNode);
        foundNodes.put(goal, goalNode);

        startNode.gx = 0;
        startNode.hx = heuristic(start, goal);
        startNode.fx = heuristic(start, goal);

        // A* containers: nodes to be investigated, nodes that have been investigated
        ArrayList<AStarNodeWrapper> closedSet = new ArrayList<AStarNodeWrapper>(),
            openSet = new ArrayList<AStarNodeWrapper>();
        openSet.add(startNode);


        while (openSet.size() > 0)
        { // while there are reachable nodes to investigate

            AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
            if (x.node == goal)
            { // we have found the shortest possible path to the goal!
                // Reconstruct the path and send it back.
                return reconstructPath(goalNode);
            }
            openSet.remove(x); // maintain the lists
            closedSet.add(x);

            // check all the edges out from this Node
//            for (Object o : network.getEdgesOut(x.node))
           for (Object o : network.getEdges(x.node, null))
           {
            	Edge l = (Edge) o;
                GeoNode next = null;
                next = (GeoNode) l.getOtherNode(x.node);
                
                // get the A* meta information about this Node
                AStarNodeWrapper nextNode;
                if (foundNodes.containsKey(next))
                {
                    nextNode = foundNodes.get(next);
                } else
                {
                    nextNode = new AStarNodeWrapper(next);
                    foundNodes.put(next, nextNode);
                }

                if (closedSet.contains(nextNode)) // it has already been considered
                {
                    continue;
                }

                // otherwise evaluate the cost of this node/edge combo
                double tentativeCost = x.gx + length(l);
                boolean better = false;

                if (!openSet.contains(nextNode))
                {
                    openSet.add(nextNode);
                    nextNode.hx = heuristic(next, goal);
                    better = true;
                } else if (tentativeCost < nextNode.gx)
                {
                    better = true;
                }

                // store A* information about this promising candidate node
                if (better)
                {
                    nextNode.cameFrom = x;
                    nextNode.edgeFrom = l;
                    nextNode.gx = tentativeCost;
                    nextNode.fx = nextNode.gx + nextNode.hx;
                }
            }
        }

//        System.out.println("A* Problem: graph has only " + closedSet.size() + " nodes associated with it");
        return null;
    }

	/**
	 * Finds a path between the start and a set of goal nodes, within the given network
	 * 
	 * @param start - the node from which to start
	 * @param goal - the set of goal nodes
	 * @param network - the network in which the path should exist
	 * @return either an ordered list of the connecting edges or else null
	 */
	public ArrayList<Edge> astarPath(GeoNode start, ArrayList <GeoNode> goal, Network network)
	   {

	        // initial check
	        if (start == null || goal == null || goal.size() == 0)
	        {
	            System.out.println("Error: invalid nodeset provided to AStar");
	            return null;
	        }
	        else if(goal.contains(start))
	        	return null;

	        // containers for the metainformation about the Nodes relative to the
	        // A* search
	        HashMap<GeoNode, AStarNodeWrapper> foundNodes =
	            new HashMap<GeoNode, AStarNodeWrapper>();

	        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
	        foundNodes.put(start, startNode);
	        startNode.gx = 0;

	        ArrayList <AStarNodeWrapper> goalNodes = new ArrayList <AStarNodeWrapper> ();
	        double minVal = Double.MAX_VALUE;
	        for(GeoNode n: goal){
	        	AStarNodeWrapper goalNode = new AStarNodeWrapper(n); 
	        	goalNodes.add(goalNode);
		        foundNodes.put(n, goalNode);
		        
		        double hval = heuristic(start, n);
		        if(hval < minVal)
		        	minVal = hval;
	        }
	        startNode.hx = minVal;
	        startNode.fx = minVal;


	        // A* containers: nodes to be investigated, nodes that have been investigated
	        ArrayList<AStarNodeWrapper> closedSet = new ArrayList<AStarNodeWrapper>(),
	            openSet = new ArrayList<AStarNodeWrapper>();
	        openSet.add(startNode);


	        while (openSet.size() > 0)
	        { // while there are reachable nodes to investigate

	            AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
	            if (goal.contains(x.node))
	            { // we have found the shortest possible path to the goal!
	                // Reconstruct the path and send it back.
	                return reconstructPath(foundNodes.get(x.node));
	            }
	            openSet.remove(x); // maintain the lists
	            closedSet.add(x);

	            // check all the edges out from this Node
	            for (Object o : network.getEdgesOut(x.node)) // TODO: make sure this is still ok
	            {
	            	Edge l = (Edge) o;
	                GeoNode next = null;
	                next = (GeoNode) l.getOtherNode(x.node);
	                
	                // get the A* meta information about this Node
	                AStarNodeWrapper nextNode;
	                if (foundNodes.containsKey(next))
	                {
	                    nextNode = foundNodes.get(next);
	                } else
	                {
	                    nextNode = new AStarNodeWrapper(next);
	                    foundNodes.put(next, nextNode);
	                }

	                if (closedSet.contains(nextNode)) // it has already been considered
	                {
	                    continue;
	                }

	                // otherwise evaluate the cost of this node/edge combo
	                double tentativeCost = x.gx + length(l);
	                boolean better = false;

	                if (!openSet.contains(nextNode))
	                {
	                    openSet.add(nextNode);
	                    minVal = Double.MAX_VALUE;
	                    for(GeoNode n: goal){
	                    	double hval = heuristic(next, n); 
	                    	if(hval < minVal) minVal = hval;
	                    }
	                    nextNode.hx = minVal;
	                    better = true;
	                } else if (tentativeCost < nextNode.gx)
	                {
	                    better = true;
	                }

	                // store A* information about this promising candidate node
	                if (better)
	                {
	                    nextNode.cameFrom = x;
	                    nextNode.edgeFrom = l;
	                    nextNode.gx = tentativeCost;
	                    nextNode.fx = nextNode.gx + nextNode.hx;
	                }
	            }
	        }

	        System.out.println("A* ERROR: No path found. Graph has only " + closedSet.size() + " nodes associated with it");
	        return null;
	    }

	/**
	 * Finds a path between the start and a set of goal nodes, within the given network
	 * 
	 * @param start - the node from which to start
	 * @param goal - the set of goal nodes
	 * @param network - the network in which the path should exist
	 * @return either an ordered list of the connecting edges or else null
	 */
	public ArrayList<Edge> astarWeightedPath(GeoNode start, GeoNode goal, Network network, 
			HashSet <Object> weighted, double weight)
	   {

	        // initial check
	        if (start == null || goal == null)
	        {
	            System.out.println("Error: invalid nodeset provided to AStar");
	        }

	        // set up the containers for the result
	        ArrayList<GeomPlanarGraphDirectedEdge> result =
	            new ArrayList<GeomPlanarGraphDirectedEdge>();

	        // containers for the metainformation about the Nodes relative to the
	        // A* search
	        HashMap<GeoNode, AStarNodeWrapper> foundNodes =
	            new HashMap<GeoNode, AStarNodeWrapper>();
	        
	        AStarNodeWrapper startNode = new AStarNodeWrapper(start);
	        AStarNodeWrapper goalNode = new AStarNodeWrapper(goal);
	        foundNodes.put(start, startNode);
	        foundNodes.put(goal, goalNode);

	        startNode.gx = 0;
	        startNode.hx = heuristic(start, goal);
	        startNode.fx = heuristic(start, goal);


	        // A* containers: nodes to be investigated, nodes that have been investigated
//	        ArrayList<AStarNodeWrapper> closedSet = new ArrayList<AStarNodeWrapper>(),
//	            openSet = new ArrayList<AStarNodeWrapper>();
	        HashSet <AStarNodeWrapper> closedSet = new HashSet<AStarNodeWrapper>(),
		            openSet = new HashSet<AStarNodeWrapper>();
	        openSet.add(startNode);


	        while (openSet.size() > 0)
	        { // while there are reachable nodes to investigate

	            AStarNodeWrapper x = findMin(openSet); // find the shortest path so far
	            if (x.node == goal)
	            { // we have found the shortest possible path to the goal!
	                // Reconstruct the path and send it back.
	                return reconstructPath(foundNodes.get(x.node));
	            }
	            openSet.remove(x); // maintain the lists
	            closedSet.add(x);

	            // check all the edges out from this Node
	            for (Object o : network.getEdgesOut(x.node))
	            {
	            	Edge l = (Edge) o;
	                GeoNode next = null;
	                next = (GeoNode) l.getOtherNode(x.node);
	                
	                // get the A* meta information about this Node
	                AStarNodeWrapper nextNode;
	                if (foundNodes.containsKey(next))
	                {
	                    nextNode = foundNodes.get(next);
	                } else
	                {
	                    nextNode = new AStarNodeWrapper(next);
	                    foundNodes.put(next, nextNode);
	                }

	                if (closedSet.contains(nextNode)) // it has already been considered
	                {
	                    continue;
	                }

	                // otherwise evaluate the cost of this node/edge combo
	                double edge_factor = 1, node_factor = 1;
	                if(weighted.contains(l))
	                	edge_factor = weight;
	                if(weighted.contains(next))
	                	node_factor = weight;
	                double tentativeCost = node_factor * (x.gx + edge_factor * length(l));
	                boolean better = false;

	                if (!openSet.contains(nextNode))
	                {
	                    openSet.add(nextNode);
	                    nextNode.hx = heuristic(next, goal);
	                    better = true;
	                } else if (tentativeCost < nextNode.gx)
	                {
	                    better = true;
	                }

	                // store A* information about this promising candidate node
	                if (better)
	                {
	                    nextNode.cameFrom = x;
	                    nextNode.edgeFrom = l;
	                    nextNode.gx = tentativeCost;
	                    nextNode.fx = nextNode.gx + nextNode.hx;
	                }
	            }
	        }

	        System.out.println("A* Problem: graph has only " + closedSet.size() + " nodes associated with it");
	        return null;
	    }
	
    /**
     * Takes the information about the given node n and returns the path that
     * found it.
     * @param n the end point of the path
     * @return an ArrayList of Edges that lead from the
     * given Node to the Node from which the search began
     */
    ArrayList <Edge> reconstructPath(AStarNodeWrapper n)
    {
        ArrayList<Edge> result = new ArrayList<Edge>();
        AStarNodeWrapper x = n;
        while (x.cameFrom != null)
        {
            result.add(x.edgeFrom); // add this edge to the front of the list
            x = x.cameFrom;
        }

//        Collections.reverse(result);
        
        if(result.size() < 1)
        	System.out.println("stuipd path...");
        return result;
    }


    /**
     * Measure of the estimated distance between two Nodes. Takes into account whether either of the 
     * GeoNodes entails a delay
     * @param x
     * @param y
     * @return notional "distance" between the given nodes.
     */
    double heuristic(GeoNode x, GeoNode y)
    {
        Coordinate xnode = x.geometry.getCoordinate();
        Coordinate ynode = y.geometry.getCoordinate();
        int nodeCost = 0;
        if(x.hasAttribute("delay"))
        	nodeCost += x.getIntegerAttribute("delay");
        if(y.hasAttribute("delay"))
        	nodeCost += y.getIntegerAttribute("delay");
        
        return nodeCost + Math.sqrt(Math.pow(xnode.x - ynode.x, 2)
            + Math.pow(xnode.y - ynode.y, 2));
    }

    double length(Edge e)
    {
        Coordinate xnode = ((GeoNode)e.from()).geometry.getCoordinate();
        Coordinate ynode = ((GeoNode)e.to()).geometry.getCoordinate();
        return Math.sqrt(Math.pow(xnode.x - ynode.x, 2)
            + Math.pow(xnode.y - ynode.y, 2));
    }

    /**
     *  Considers the list of Nodes open for consideration and returns the node
     *  with minimum fx value
     * @param set list of open Nodes
     * @return
     */
    AStarNodeWrapper findMin(ArrayList<AStarNodeWrapper> set)
    {
        double min = Double.MAX_VALUE;
        AStarNodeWrapper minNode = null;
        for (AStarNodeWrapper n : set)
        {
            if (n.fx < min)
            {
                min = n.fx;
                minNode = n;
            }
        }
        return minNode;
    }
    
    /**
     *  Considers the list of Nodes open for consideration and returns the node
     *  with minimum fx value
     * @param set list of open Nodes
     * @return
     */
    AStarNodeWrapper findMin(HashSet<AStarNodeWrapper> set)
    {
        double min = Double.MAX_VALUE;
        AStarNodeWrapper minNode = null;
        for (AStarNodeWrapper n : set)
        {
            if (n.fx < min)
            {
                min = n.fx;
                minNode = n;
            }
        }
        return minNode;
    }

    /**
     * A wrapper to contain the A* meta information about the Nodes
     *
     */
    class AStarNodeWrapper
    {

        // the underlying Node associated with the metainformation
        GeoNode node;
        // the Node from which this Node was most profitably linked
        AStarNodeWrapper cameFrom;
        // the edge by which this Node was discovered
        Edge edgeFrom;
        double gx, hx, fx;



        public AStarNodeWrapper(GeoNode n)
        {
            node = n;
            gx = 0;
            hx = 0;
            fx = 0;
            cameFrom = null;
            edgeFrom = null;
        }

        public int hashCode(){
        	return node.hashCode(); 
        }
    }
}