package swiselib.disasters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

import ec.util.MersenneTwisterFast;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomGridField;
import sim.field.grid.DoubleGrid2D;
import sim.field.grid.Grid2D;
import sim.field.grid.IntGrid2D;
import sim.util.IntBag;
import swiselib.objects.PopSynth;

/**
 * 
 * @author swise
 *
 * Operationalized from Alexandridis et al. 2008
 * vegetation has been clustered into three categories numbered from 1 to 3
 * 		1 is for agricultural areas
 * 		2 is for shrubs
 * 		3 is for pine trees
 * the density of vegetation has been scaled into three categories numbered from 1 to 3
 * 		1 is for sparse vegetation 
 * 		3 is for dense vegetation
 * 
 */
public class Wildfire implements Steppable {

	public GeomGridField burning;
	GeomGridField elevation;
	GeomGridField vegetation;
	GeomGridField impermeability;
	
	double precision = 5; // in meters, as in Alexandridis
	double windspeed = 1;//2.6; //m/s!
	double sqrt2 = Math.sqrt(2);

	public static int STATE_no_fuel = 0; // The cell contains no forest fuel. Cannot be burned.
	public static int STATE_vulnerable = 1; // The cell contains forest fuel that has not ignited.
	public static int STATE_burning = 2; // The cell contains forest fuel that is burning.
	public static int STATE_burned = 3; // The cell contained forest fuel that has been burned down.

	//
	// PARAMETERS
	//
	// http://landcover.usgs.gov/classes.php
	public static HashMap <Integer, Double> veg_density = new HashMap<Integer, Double>() {{ 
        put(11, -0.4); put(12, -0.4); put(13, -0.4); put(14, -0.4); put(15, -0.4); put(16, -0.4); put(17, -0.4); 
        put(21, -0.4); put(22, -0.4); put(23, -0.4); put(24, -0.4); put(31, -0.4); put(32, -0.4); put(33, -0.4); 
        put(51, -0.4); put(52, -0.4); put(53, -0.4); put(54, -0.4); put(62, -0.4); put(71, -0.4); put(72, -0.4);
        put(73, -0.4); put(74, -0.4); put(75, -0.4); put(76, -0.4); put(77, -0.4); put(83, -0.4); put(84, -0.4); 
        put(90, -0.4); put(91, -0.4); put(92, -0.4); put(93, -0.4); put(94, -0.4); put(95, -0.4);
        
        put(61, 0.); put(81, 0.); put(82, 0.); put(85, 0.);
        
        put(41, 0.3); put(42, 0.3); put(43, 0.3);      
    }};	

    public static HashMap <Integer, Double> veg_type = new HashMap<Integer, Double>() {{ 
        put(11, -0.3); put(12, -0.3); put(13, -0.3); put(14, -0.3); put(15, -0.3); put(16, -0.3); put(17, -0.3); 
        put(21, -0.3); put(22, -0.3); put(23, -0.3); put(24, -0.3); put(31, -0.3); put(32, -0.3); put(33, -0.3); 
        put(51, -0.3); put(52, -0.3); put(53, -0.3); put(54, -0.3); put(62, -0.3); put(71, -0.3); put(72, -0.3);
        put(73, -0.3); put(74, -0.3); put(75, -0.3); put(76, -0.3); put(77, -0.3); put(83, -0.3); put(84, -0.3); 
        put(90, -0.3); put(91, -0.3); put(92, -0.3); put(93, -0.3); put(94, -0.3); put(95, -0.3);
        
        put(41, 0.); put(43, 0.); put(61, 0.); put(81, 0.); put(82, 0.); put(85, 0.);     

        put(42, 0.4); 
    }};

    double p_h = 0.58; // the constant probability that a cell adjacent to a burning cell containing a 
    				   //given type of vegetation and density will catch fire at the next time step under 
    				   //no wind and flat terrain;
//    double a = 0.01;//0.078; // a slope-dependent constant that can be adjusted from experimental data
    double a = 0.078; // a slope-dependent constant that can be adjusted from experimental data
    double c1 = 0.045; // experimentally determined wind direction/velocity constants
    double c2 = 0.131; // " "
	
    public Coordinate initialPoint = null;
	public ArrayList <FireTile> fires = new ArrayList <FireTile> ();
	public int numBurned = 0;

	public GeometryFactory fa = new GeometryFactory();
	public Geometry extent = fa.createPoint(new Coordinate(0,0));
	
	MersenneTwisterFast myRandom;

	/**
	 * Initialize from the vegetation file
	 * @param vegetation
	 */
	public Wildfire(GeomGridField vegetation, GeomGridField elevation, GeomGridField impermeable, Envelope MBR){
		this.vegetation = vegetation;
		this.elevation = elevation;
		this.impermeability = impermeable;
		
		burning = new GeomGridField();
		burning.setMBR(vegetation.getMBR());
		burning.setGrid(new IntGrid2D(vegetation.getGridWidth(), vegetation.getGridHeight(), this.STATE_vulnerable));
		burning.setMBR(MBR);

		
		precision = vegetation.getPixelHeight() * 1000;
		
		double radius = 6371000;
		double dLat = Math.toRadians(MBR.getMaxX() - MBR.getMinX());
		double dLon = Math.toRadians(0); // we're calling them the same for this purpose
		double lat1 = Math.toRadians(MBR.getMinX());
		double lat2 = Math.toRadians(MBR.getMaxX());
		
		double a_prime = Math.sin(dLat/2.) * Math.sin(dLat/2.) + 
			Math.sin(dLon/2.) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
		double c_prime = 2 * Math.atan2(Math.sqrt(a_prime), Math.sqrt(1-a_prime));
		precision = radius * c_prime / vegetation.getGrid().getWidth();

		myRandom = new MersenneTwisterFast(12345);
	}
	
	@Override
	public void step(SimState state) {

		// Update and propagate the fires
		
		ArrayList <FireTile> currentFires = (ArrayList <FireTile>) fires.clone();
		for(FireTile f: currentFires){
			f.step(state);
		}

		
		// Generate fire extent
		
		ArrayList <Coordinate> coords = new ArrayList <Coordinate> ();
		Point lastPoint = null;
		for(int i = 0; i < fires.size(); i++){
			FireTile f = fires.get(i);
			Point p = burning.toPoint(f.x, f.y);
			if(lastPoint == null) lastPoint = p;
			else if(lastPoint.distance(p) < 1000) continue;
			coords.add(new Coordinate(p.getX(), p.getY()));
			lastPoint = p;
		}
		
		// if it's just one point, it's not a proper polygon
		if(coords.size() > 3){
			coords.add(coords.get(0));
			
			Geometry myGeom = fa.createPolygon(fa.createLinearRing(coords.toArray(new Coordinate [] {})), null);

			if(myGeom.isEmpty()) return;
			if(extent.getNumPoints() > 2)
				extent = myGeom.convexHull().union(extent);
			else
				extent = myGeom.convexHull();
		}
		else if(coords.size() > 0)
			extent = fa.createPoint(coords.get(0));
		else
			extent = fa.createPoint(new Coordinate(0,0));
	}
	
	
	
	/**
	 * Start a fire at the given coordinates
	 * @param lat - the latitdue
	 * @param lon - the longitude
	 */
	public void initiateFire(double lat, double lon){
		int x = vegetation.toXCoord(lat), y = vegetation.toYCoord(lon);
		FireTile init = new FireTile(x, y, this);
		((IntGrid2D)burning.getGrid()).field[x][y] = STATE_burning;
		fires.add(init);
		initialPoint = new Coordinate(lat, lon);
	}
	
	/**
	 * A representation of the fires in space
	 */
	class FireTile implements Steppable {

		Wildfire owner;
		int x, y;
		
		public FireTile(int x, int y, Wildfire owner){
			this.x = x;
			this.y = y;
			this.owner = owner;
		}
		
		@Override
		public void step(SimState state) {
			
			int[][] fieldy = ((IntGrid2D)owner.burning.getGrid()).field;
			
			// get this location's current status
			int status = fieldy[x][y];
			
			if(status == STATE_burned || status == STATE_no_fuel) // it cannot burn if there is nothing to burn 
				return;
						
			else if(status == STATE_burning){ // burning progresses to burned
			
				int myIndex = fires.indexOf(this);

				// progress this tile to the next logical step
				fieldy[x][y] = STATE_burned;
				numBurned++;
				
				// go through each of the neighboring tiles and potentially light them on fire
				IntBag xPos = new IntBag(), yPos = new IntBag();
				((IntGrid2D)owner.burning.getGrid()).getNeighborsHexagonalDistance(x, y, 1, false, xPos, yPos);
				int [] xs = xPos.objs, ys = yPos.objs;
				
				for(int i = 0; i < xs.length; i++){ // note: we ourselves are a member of this group, but we're already on fire
											// so nothing can come of it and it's costly to check

					int neighbor_state = fieldy[xs[i]][ys[i]];
					int permeable = ((IntGrid2D)owner.impermeability.getGrid()).field[xs[i]][ys[i]];
					
					if(neighbor_state == STATE_vulnerable && permeable == 0){ // only care if it's unburned but burnable!

						 // the constant probability a cell adjacent to a burning cell w/ type of vegetation 
						 // and density will catch fire
						 
						// fire propagation probability depends on the density + type of vegetation
						int veg = ((IntGrid2D)vegetation.getGrid()).field[xs[i]][ys[i]];
						double p_den = veg_density.get(veg);
						double p_veg = veg_type.get(veg);
						
						// NOT INCLUDED HERE: WIND MODEL
						/*// impact of wind speed
						double c1 = 0, c2 = 0, theta = 0, V = 0; 
						// c1, c2 are constants to be determined and h is the angle between the direction of the 
						// fire propagation and the direction of the wind
						*/double p_w = Math.exp( c1 * windspeed) * Math.exp(windspeed * c2 * (-1 - 1));
						
						
						// impact of slope
						DoubleGrid2D elev = (DoubleGrid2D)elevation.getGrid();
						double theta_s = theta_s(elev.field[x][y], elev.field[xs[i]][ys[i]], !(x == xs[i] || y == ys[i]));
						
						double p_s = Math.exp(a * theta_s);
						
						double p_burn = p_h * (1 + p_veg) * (1 + p_den) * p_s * p_w; 
						
						// NOT INCLUDED HERE: SPOTTING MODEL
						if(myRandom.nextDouble() < p_burn){
							fieldy[xs[i]][ys[i]] = STATE_burning;
							FireTile f = new FireTile(xs[i], ys[i], owner);
							owner.fires.add(myIndex, f);
							myIndex++;
						}
					}
				}
				
				// possibly transmit to everywhere else!
			}
			fires.remove(this);
		}
		
		/**
		 * Calculates the change in slope
		 * @param e1 - elevation of one tile
		 * @param e2 - elevation of other tile
		 * @param diagonal - whether the tiles are diagonal or side-by-side (Moore vs von Neumann)
		 * @return
		 */
		double theta_s(double e1, double e2, boolean diagonal){
			if(diagonal)
				return Math.atan((e1-e2)/precision);
			else
				return Math.atan((e1-e2)/(precision * sqrt2));
		}
		
	}
	
	public GeomGridField getGrid(){ return burning; }
	
}