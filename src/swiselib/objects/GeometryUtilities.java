package swiselib.objects;

import java.util.ArrayList;
import java.util.HashMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import sim.util.Bag;
import sim.util.geo.MasonGeometry;
import swiselib.objects.network.GeoNode;

public class GeometryUtilities {
	
	/**
	 * 
	 * @param masongeos
	 * @param nodes
	 * @return
	 */
	public static HashMap <MasonGeometry, ArrayList <GeoNode>> mapGeoNodesToMasonGeometries(
			Bag masongeos, Bag nodes){
		
		HashMap <MasonGeometry, ArrayList <GeoNode>> result = 
			new HashMap <MasonGeometry, ArrayList <GeoNode>> ();
		
		HashMap <Geometry, MasonGeometry> geos = new HashMap <Geometry, MasonGeometry> ();
		for(Object o: masongeos){
			if( !(o instanceof MasonGeometry)){
				System.out.println("Error: object " + o + " is not a MasonGeometry and will not be mapped.");
				continue;				
			}
			MasonGeometry mg = (MasonGeometry) o;
			geos.put(mg.getGeometry(), mg);
			result.put(mg, new ArrayList <GeoNode> ());
		}
		
		for(Object o: nodes){
			if( !(o instanceof GeoNode)){
				System.out.println("Error: object " + o + " is not a GeoNode and will not be mapped.");
				continue;
			}
			GeoNode gn = (GeoNode) o;
			Geometry gn_n = gn.geometry;
			for(Geometry g: geos.keySet()){
				if(g.contains(gn_n)){
					result.get(geos.get(g)).add(gn);
					break;
				}
			}
			
		}
		
		return result;
	}
	
	public static double cleanAngle(double angle){
		while(angle > 360) angle -= 360;
		while(angle < 0) angle += 360;
		return angle;
	}
	
	public static double toAngle(Coordinate from, Coordinate to){
		return cleanAngle(Math.toDegrees(Math.atan((to.x - from.x) / (to.y - from.y))));
	}
}