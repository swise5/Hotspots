package swiselib.agents;

import sim.field.network.Edge;
import sim.util.geo.MasonGeometry;
import swiselib.objects.network.ListEdge;
import swiselib.objects.network.GeoNode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.linearref.LengthIndexedLine;
import com.vividsolutions.jts.planargraph.Node;

/**
 * An Agent that moves through the network constrained by the presence of others in its space,
 * so that it is affected by traffic
 * 
 * @author swise
 *
 */
public class TrafficAgent extends MobileAgent {

	private static final long serialVersionUID = 1L;
	
	public double myLastSpeed = -1; // recordkeeping
	public double minSpeed = -1;
	
	public TrafficAgent(){ super(); }
	public TrafficAgent(Coordinate c){ super((new GeometryFactory()).createPoint(c)); }
	public TrafficAgent(Point p){ super(p); }

	/**
	 * 
	 * @param time - a positive amount of time, representing the period of time agents 
	 * 				are allocated for movement
	 * @param obstacles - set of spaces which are obstacles to the agent
	 * @return the amount of time left after moving, negated if the movement failed
	 */
	protected double move(double time, double mySpeed, double resolution){
		
		// if we're at the end of the edge and we have more edges, move onto the next edge
		if(arrived() ){
			
			// clean up any edge we leave
			if(edge != null && edge.getClass().equals(ListEdge.class))
				((ListEdge)edge).removeElement(this);

			// if we have arrived and there is no other edge in the path, we have finished our journey: 
			// reset the path and return the remaining time
			if(goalPoint == null && path.size() == 0 && (currentIndex <= startIndex || currentIndex >= endIndex )){
				path = null;
				return time;
			}
			
			// make sure that there is another edge in the path
			if(path.size() > 0) { 

				// take the next edge
				Edge newEdge = path.remove(path.size() - 1);				
				edge = newEdge;

				// make sure it's open
				// if it's not, return an error!
				if(((MasonGeometry)newEdge.info).getStringAttribute("open").equals("CLOSED")){
					updateLoc(node.geometry.getCoordinate());
					edge = newEdge;
					path = null;
					return -1;
				}				
					
				// change our positional node to be the Node toward which we're moving
				node = (GeoNode) edge.getOtherNode(node);
				
				// format the edge's geometry so that we can move along it conveniently
				LineString ls = (LineString)((MasonGeometry)edge.info).geometry;

				// set up the segment and coordinates
				segment = new LengthIndexedLine(ls);
				startIndex = segment.getStartIndex();
				endIndex = segment.getEndIndex();
				currentIndex = segment.project(this.geometry.getCoordinate());
				
				
				// if that was the last edge and we have a goal point, resize the expanse
				if(path.size() == 0 && goalPoint != null){ 
					double goalIndex = segment.project(goalPoint);
					if(currentIndex < goalIndex)
						endIndex = goalIndex;
					else
						startIndex = goalIndex;
				}
				
				// make sure we're moving in the correct direction along the Edge
				if(node == edge.to()){
					direction = 1;
					currentIndex = Math.max(currentIndex, startIndex);
				} else {
					direction = -1;
					currentIndex = Math.min(currentIndex, endIndex);
				}

				if(edge.getClass().equals(ListEdge.class))
					((ListEdge)edge).addElement(this);

			}
						

		}
		
		// otherwise, we're on an Edge and moving forward!

		// set our speed
		double speed;
		if(edge != null && edge.getClass().equals(ListEdge.class)){
			
			// Each car has a certain amount of space: wants to preserve a following distance. 
			// If the amount of following distance is less than 20 meters (~ 6 car lengths) it'll slow
			// proportionately
			double val = Math.min(1, ((ListEdge)edge).lengthPerElement() / 20); 
			speed = Math.max( mySpeed * val, minSpeed);
		}
		else
			speed = mySpeed;

		myLastSpeed = speed;
		
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

		// don't overshoot if we're on the last bit!
		if(goalPoint != null && path.size() == 0){
			double idealIndex = segment.indexOf(goalPoint);
			if((direction == 1 && idealIndex <= currentIndex) || (direction == -1 && idealIndex >= currentIndex)){
				currentIndex = idealIndex;
				time = 0;
				startIndex = endIndex = currentIndex;
			}
		}

		updateLoc(segment.extractPoint(currentIndex));
		
		if(path.size() == 0 && arrived()){
			path = null;
		}
		return time;
	}
	
	/**
	 * 
	 * @param resolution
	 * @return 1 for success, -1 for failure
	 */
	public int navigate(double resolution){
		if(path != null){
			double time = 1;//speed;
			while(path != null && time > 0){
				time = move(time, speed, resolution);
			}
			
			if(segment != null)
				updateLoc(segment.extractPoint(currentIndex));				

			if(time < 0){
				return -1;
			}
			else
				return 1;
		}
		return -1;
	}
}
