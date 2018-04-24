package swiselib.visualization;

import com.vividsolutions.jts.geom.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import sim.display.GUIState;
import sim.portrayal.*;
import sim.portrayal.geo.GeomInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.inspector.TabbedInspector;
import sim.util.Properties;
import sim.util.geo.MasonGeometry;
import sim.util.gui.ColorMap;

/**
 * Portrays a filled polygon with user-defined outline color
 * 
 * @author swise
 *
 */
public class AttributePolyPortrayal extends GeomPortrayal
{

	private static final long serialVersionUID = 1L;
	String attribute = null;
	ColorMap cm = null;
	
	/** How to paint each object */
	public Paint outlinePaint;

	/** Default constructor creates filled, gray objects with a scale of 1.0 */
	public AttributePolyPortrayal() {
		this(Color.GRAY, Color.BLACK, 1.0, true);
	}

	public AttributePolyPortrayal(Paint paint) {
		this(paint, Color.BLACK, 1.0, true);
	}

	public AttributePolyPortrayal(Paint paint, Paint outline, boolean filled) {
		this(paint, outline, 1.0, filled);
	}

	public AttributePolyPortrayal(Paint paint, Paint outline, double scale, boolean filled) {
		this.paint = paint;
		this.outlinePaint = outline;
		this.scale = scale;
		this.filled = filled;
	}    

	public AttributePolyPortrayal(ColorMap colormap, String attribute, Paint outline, boolean filled) {
		this.cm = colormap;
		this.attribute = attribute;
		this.paint = null;
		this.outlinePaint = outline;
		this.scale = 1.0;
		this.filled = filled;
	}    

	public AttributePolyPortrayal(ColorMap colormap, String attribute, Paint outline, boolean filled, double scale) {
		this.cm = colormap;
		this.attribute = attribute;
		this.paint = null;
		this.outlinePaint = outline;
		this.scale = scale;
		this.filled = filled;
	}    

	/**
	 * Draw a JTS geometry object. The JTS geometries are converted to Java
	 * general path objects, which are then drawn using the native Graphics2D
	 * methods.
	 */
    @Override
	public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
	{
		GeomInfo2D gInfo;
		if (info instanceof GeomInfo2D)
        {
            gInfo = (GeomInfo2D) info;
        }
		else
        {
            gInfo = new GeomInfo2D(info, new AffineTransform());
        }

		MasonGeometry gm = (MasonGeometry) object;
		Geometry geometry = gm.getGeometry();

		if(!gm.hasAttribute(attribute)){
			return;
		}
		
		if (geometry.isEmpty())
        {
            return;
        }

		if (paint != null)
        {
            graphics.setPaint(paint);
        }
		else if(cm != null){
			int myval = gm.getIntegerAttribute(attribute);
			graphics.setPaint(cm.getColor(myval));
		}

		// don't have cached shape or the transform changed, so need to build
		// the shape
		if ((gm.isMovable) || (gm.shape == null) || (!gm.transform.equals(gInfo.transform)))
		{
			gm.transform.setTransform(gInfo.transform);
			if (geometry instanceof Point)
			{
				Point point = (Point) geometry;
				double offset = 3 * scale / 2.0; // used to center point
				Ellipse2D.Double ellipse = new Ellipse2D.Double(point.getX() - offset, point.getY() - offset,
						3 * scale, 3 * scale);

				GeneralPath path = (GeneralPath) (new GeneralPath(ellipse).createTransformedShape(gInfo.transform));
				gm.shape = path;
			}
			else if (geometry instanceof LineString)
			{
				gm.shape = drawGeometry(geometry, gInfo, false);
				filled = false;
			}
			else if (geometry instanceof Polygon)
            {
                gm.shape = drawPolygon((Polygon) geometry, gInfo, filled);
            }
			else if (geometry instanceof MultiLineString)
			{
				// draw each LineString individually
				MultiLineString multiLine = (MultiLineString) geometry;
				for (int i = 0; i < multiLine.getNumGeometries(); i++)
				{
					GeneralPath p = drawGeometry(multiLine.getGeometryN(i), gInfo, false);
					if (i == 0)
                    {
                        gm.shape = p;
                    }
					else
                    {
                        gm.shape.append(p, false);
                    }
				}
				filled = false;
			}
			else if (geometry instanceof MultiPolygon)
			{
				// draw each Polygon individually
				MultiPolygon multiPolygon = (MultiPolygon) geometry;
				for (int i = 0; i < multiPolygon.getNumGeometries(); i++)
				{
					GeneralPath p = drawPolygon((Polygon) multiPolygon.getGeometryN(i), gInfo, filled);
					if (i == 0)
                    {
                        gm.shape = p;
                    }
					else
                    {
                        gm.shape.append(p, false);
                    }
				}
			}
			else
            {
                throw new UnsupportedOperationException("Unsupported JTS type for draw()" + geometry);
            }
		}

		// now draw it!
		if (filled)
        {
            graphics.fill(gm.shape);
            graphics.setPaint(outlinePaint);
            graphics.draw(gm.shape);
        }
		else
        {
            graphics.draw(gm.shape);
        }
	}



	/**
	 * Helper function for drawing a JTS polygon.
	 * 
	 * <p>
	 * Polygons have two sets of coordinates; one for the outer ring, and
	 * optionally another for internal ring coordinates. Draw the outer ring
	 * first, and then draw each internal ring, if they exist.
	 * */
	GeneralPath drawPolygon(Polygon polygon, GeomInfo2D info, boolean fill)
	{
		GeneralPath p = drawGeometry(polygon.getExteriorRing(), info, fill);

		for (int i = 0; i < polygon.getNumInteriorRing(); i++)
		{ // fill for internal rings will always be false as they are literally
			// "holes" in the polygon
			p.append(drawGeometry(polygon.getInteriorRingN(i), info, false), false);
		}
		return p;
	}

	/**
	 * Helper function to draw a JTS geometry object. The coordinates of the JTS
	 * geometry are converted to a native Java GeneralPath which is used to draw
	 * the object.
	 */
	GeneralPath drawGeometry(Geometry geom, GeomInfo2D info, boolean fill)
	{
		Coordinate coords[] = geom.getCoordinates();
		GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, coords.length);
		path.moveTo((float) coords[0].x, (float) coords[0].y);

		for (int i = 1; i < coords.length; i++)
        {
            path.lineTo((float) coords[i].x, (float) coords[i].y);
        }

		path.transform(info.transform);

		return path;
	}


}
