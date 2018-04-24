package swiselib.agents;

import java.util.ArrayList;
import java.util.HashMap;

import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import sim.util.geo.PointMoveTo;
import swiselib.objects.AStar;
import swiselib.objects.GeometryUtilities;
import swiselib.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;


/**
 * MobileAgents can move around the environment on a network
 * @author swise
 *
 */
public class MobileAgent extends SpatialAgent {
	
	// network functionality
	protected GeoNode node;
	protected Coordinate goalPoint = null, startPoint = null;
	protected AStar pathfinder = new AStar();
	protected int direction = 1;
	protected Edge edge;
	protected LengthIndexedLine segment = null;
	protected double startIndex = 0., endIndex = 0., currentIndex = 0.;
	protected ArrayList<Edge> path = null;

	// attributes
	protected double speed = 0;
	protected double size = 1;
	
	/**
	 * Constructors
	 */
	public MobileAgent(){ super(); }
	public MobileAgent(Coordinate c){ super((new GeometryFactory()).createPoint(c)); }
	public MobileAgent(Point p){ super(p); }

	public HashMap <String, Object> getPositionalInformation(){
		HashMap <String, Object> results = new HashMap <String, Object> ();
		results.put("node", node);
		results.put("direction", direction);
		results.put("edge", edge);
		results.put("segment", segment);
		results.put("startIndex", startIndex);
		results.put("endIndex", endIndex);
		results.put("currentIndex", currentIndex);
		return results;
	}
	
	public void setPositionalInformation(HashMap <String, Object> position){
		node = (GeoNode) position.get("node");
		direction = (Integer) position.get("direction");
		edge = (Edge) position.get("edge");
		segment = (LengthIndexedLine) position.get("segment");
		startIndex = (Double) position.get("startIndex");
		endIndex = (Double) position.get("endIndex");
		currentIndex = (Double) position.get("currentIndex");
	}
	
	/**
	 * 
	 * @param time - a positive amount of time, representing the period of time agents 
	 * 				are allocated for movement
	 * @param obstacles - set of spaces which are obstacles to the agent
	 * @return the amount of time left after moving, negated if the movement failed
	 */
	protected double move(double time, HashMap <GeomVectorField, Double> obstacles){
		
		// if we're at the end of the edge and we have more edges, move onto the next edge
		if(arrived()){
			
			// make sure that there is another edge in the path
			if(path.size() > 0){ 
			
				// take the next edge
				edge = path.remove(path.size() - 1);
				
				// format the edge's geometry so that we can move along it conveniently
				LineString ls = (LineString)((MasonGeometry)edge.info).geometry;
				
				segment = new LengthIndexedLine(ls);
				startIndex = segment.getStartIndex();
				endIndex = segment.getEndIndex();

				// make sure we're moving in the correct direction along the Edge
				if(node == edge.from()){
					direction = 1;
					currentIndex = startIndex;
				} else {
					direction = -1;
					currentIndex = endIndex;
				}
				
				// change our positional node to be the Node toward which we're moving
				node = (GeoNode) edge.getOtherNode(node);
			}

			// if there is no other edge in the path, we have finished our journey: reset the path
			// and return the remaining time
			else if(currentIndex <= startIndex || currentIndex >= endIndex){
				path = null;
				return time;
			}
		}
		
		// otherwise, we're on an Edge and moving forward!

		// we can't move into an obstacle, so check to make sure that our proposed new position
		// doesn't crash into something else

		// construct a new current index which reflects the speed and direction of travel
		double proposedCurrentIndex = currentIndex + time * speed * direction;
		
		// great! It works! Move along!
		currentIndex = proposedCurrentIndex;
		
		if( direction < 0 ){
			if(currentIndex < startIndex){
				time = (startIndex - currentIndex) / speed; // convert back to time
				currentIndex = startIndex;
			}
			else
				time = 0;
		}
		else if(currentIndex > endIndex){
			time = (currentIndex - endIndex) / speed; // convert back to time
			currentIndex = endIndex;
		}
		else
			time = 0;

		updateLoc(segment.extractPoint(currentIndex));
		
		return time;
	}
	
	/**
	 * @return whether the MobileAgent has arrived at the end of the Edge
	 */
	protected boolean arrived(){
		if( (direction > 0 && currentIndex >= endIndex) || 
				(direction < 0 && currentIndex <= startIndex)) return true;
		return false;
	}
	
	/**
	 * Change the position of the MobileAgent in the space in which it is embedded
	 * @param c - the new position of the MobileAgent
	 */
	protected void updateLoc(Coordinate c){
		PointMoveTo p = new PointMoveTo();
		p.setCoordinate(c);
		geometry.apply(p);
		geometry.geometryChanged();
	}

}