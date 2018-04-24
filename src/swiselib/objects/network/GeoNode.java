package swiselib.objects.network;

import sim.util.geo.MasonGeometry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.planargraph.Node;

/**
 * a structure used by the road network to inform Agent movement
 * 
 * @author swise
 *
 */

public class GeoNode extends MasonGeometry {

//	MasonGeometry mg;
	Node n;
	/*
	public boolean equals(Object o){
		if(!(o instanceof GeoNode)) return false;
		return ((GeoNode)o).geometry.equals(this.geometry) && ((GeoNode)o).n.equals(this.n);
	}
	
	public int hashCode(){
		return (n.toString() + geometry.toString()).hashCode();
	}
	*/
	public GeoNode(Geometry g) {
		super();
		this.geometry = g;
		n = new Node(g.getCoordinate());
	}
	
	public Node getNode(){ return n; }
}