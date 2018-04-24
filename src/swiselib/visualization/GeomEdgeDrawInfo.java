package swiselib.visualization;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.geo.GeomInfo2D;

/**
 * Used by GeomNetworkFieldPortrayal and SimpleGeoEdgePortrayal to draw MASON networks
 * over GeomVectorFields
 * 
 * @author swise
 *
 */
public class GeomEdgeDrawInfo extends DrawInfo2D
{
	
	public AffineTransform transform;
	public Point2D.Double secondPoint;
	
    public GeomEdgeDrawInfo(DrawInfo2D info, AffineTransform t, Point2D.Double second)
    {
        super(info);
        transform = t;
        secondPoint = second;
    }

}