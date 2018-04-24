package swiselib.agents;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.geo.GeomVectorField;
import sim.util.geo.MasonGeometry;

/**
 * An agent that exists in space
 * @author swise
 *
 */
public class SpatialAgent extends MasonGeometry implements Steppable {
	
	private static final long serialVersionUID = 1L;

	protected GeomVectorField space = null;
	GeometryFactory geometryFactory = new GeometryFactory();
	
	@Override
	public void step(SimState state) {}
		
	public SpatialAgent(){ super(); }
	public SpatialAgent(Coordinate c){ super((new GeometryFactory()).createPoint(c)); }
	public SpatialAgent(Point p){ super(p); }
	

}