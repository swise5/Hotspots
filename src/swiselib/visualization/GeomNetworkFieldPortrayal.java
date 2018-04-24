package swiselib.visualization;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.planargraph.GraphComponent;
import com.vividsolutions.jts.planargraph.DirectedEdge;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import swiselib.objects.network.GeoNode;


import sim.display.GUIState;
import sim.field.SparseField2D;
import sim.field.continuous.Continuous2D;
import sim.field.geo.GeomVectorField;
import sim.field.network.Edge;
import sim.field.network.Network;
import sim.portrayal.*;
import sim.portrayal.geo.GeomInfo2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.portrayal.inspector.TabbedInspector;
import sim.portrayal.network.EdgeDrawInfo2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;
import sim.portrayal.network.SpatialNetwork2D;
import sim.util.Bag;
import sim.util.Double2D;
import sim.util.Properties;
import sim.util.geo.GeomPlanarGraphEdge;
import sim.util.geo.MasonGeometry;

/**
 * Portrays a MASON network whose nodes are elements within a GeomVectorField.
 * 
 * @author swise
 *
 */
public class GeomNetworkFieldPortrayal extends FieldPortrayal2D
{

	Network network;
	
    public void setField(Object field, Network net)
        {
        if (field instanceof GeomVectorField ) super.setField(field);
        else throw new RuntimeException("Invalid field for FieldPortrayal2D: " + field);
        network = net;
        }
        

	/**
	 * Returns the appropriate Portrayal. See the class header for more
	 * information on the implementation of this method.
	 */
    @Override
	public Portrayal getPortrayalForObject(Object obj)
	{
		// return the portrayal-for-all if any
		if (portrayalForAll != null)
        {
            return portrayalForAll;
        }

		MasonGeometry mg = (MasonGeometry) obj;
		Geometry geometry = mg.getGeometry();
		Object user = mg.getUserData();

		Portrayal tmp;

		// we don't check for null values of obj, so this is simpler than the
		// one in FieldPortrayal

		if (user != null && user instanceof Portrayal)
        {
            return (Portrayal) user;
        }
		if (portrayalForNonNull != null)
        {
            return portrayalForNonNull;
        }
		if ((portrayals != null /* && !portrayals.isEmpty() */) && // a little efficiency -- avoid making weak keys etc.
				((tmp = ((Portrayal) (portrayals.get(user)))) != null))
        {
            return tmp;
        }
		if ((portrayals != null /* && !portrayals.isEmpty() */) && // a little efficiency -- avoid making weak keys etc.
				((tmp = ((Portrayal) (portrayals.get(geometry)))) != null))
        {
            return tmp;
        }
		if (user != null && (classPortrayals != null /* && !classPortrayals.isEmpty() */) && // a little efficiency -- avoid making weak keys etc.
		 ((tmp = ((Portrayal) (classPortrayals.get(user.getClass())))) != null))
        {
            return tmp;
        }
		if (geometry != null && (classPortrayals != null /* && !classPortrayals.isEmpty() */) && // a little efficiency -- avoid making weak keys etc.
		 ((tmp = ((Portrayal) (classPortrayals.get(geometry.getClass())))) != null))
        {
            return tmp;
        }
		if (portrayalForRemainder != null)
        {
            return portrayalForRemainder;
        }
        
		return getDefaultPortrayal();
	}

	private static final long serialVersionUID = 8409421628913847667L;

	/** The underlying portrayal */
	GeomPortrayal defaultPortrayal = new GeomPortrayal();

	/** Default constructor */
	public GeomNetworkFieldPortrayal() {
		super();
		setImmutableField(false);
	}

	/** Constructor which sets the field's immutable flag */
	public GeomNetworkFieldPortrayal(boolean immutableField) {
		super();
		setImmutableField(immutableField);
	}

	/** Return the underlying portrayal */
    @Override
	public Portrayal getDefaultPortrayal()
	{
		return defaultPortrayal;
	}

	/** Caches immutable fields. */
	BufferedImage buffer = null;

	RenderingHints hints = null;

	/** Handles hit-testing and drawing of the underlying geometry objects. */
    @Override
	protected void hitOrDraw(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
	{
		if (field == null)
        {
            return;
        }

		// If we're drawing (and not inspecting), re-fresh the buffer if the
		// associated field is immutable.
		if (graphics != null && immutableField && !info.precise)
		{

			GeomVectorField geomField = (GeomVectorField) field;
			double x = info.clip.x;
			double y = info.clip.y;
			boolean dirty = false;

			// make a new buffer? or did the user change the zoom? Or change the
			// rendering hints?
			if (buffer == null || buffer.getWidth() != info.clip.width || buffer.getHeight() != info.clip.height
					|| hints == null || !hints.equals(graphics.getRenderingHints()))
			{
				hints = graphics.getRenderingHints();
				buffer = new BufferedImage((int) info.clip.width, (int) info.clip.height, BufferedImage.TYPE_INT_ARGB);
				dirty = true;
			}

			// handles the case for scrolling
			if (geomField.drawX != x || geomField.drawY != y)
			{
				dirty = true;
			}

			// save the origin of the drawn region for later
			geomField.drawX = x;
			geomField.drawY = y;

			// re-draw into the buffer
			if (dirty)
			{
				clearBufferedImage(buffer);
				Graphics2D newGraphics = (Graphics2D) buffer.getGraphics();
				newGraphics.setRenderingHints(hints);
				hitOrDraw2(newGraphics, new DrawInfo2D(info, -x, -y), putInHere);
				newGraphics.dispose();
			}

			// draw buffer on screen
			graphics.drawImage(buffer, (int) x, (int) y, null);
		}
		else if (graphics == null) // we're just hitting
		{
			hitOrDraw2(graphics, info, putInHere);
		}
		else
			// might as well clear the buffer -- likely we're doing precise drawing
		{
			// do regular MASON-style drawing
			buffer = null;
			hitOrDraw2(graphics, info, putInHere);
		}
	}

	/** Clears the BufferedImage by setting all the pixels to RGB(0,0,0,0) */
	void clearBufferedImage(BufferedImage image)
	{
		int len = image.getHeight() * image.getWidth();
		WritableRaster raster = image.getRaster();
		int[] data = new int[len];
		for (int i = 0; i < len; i++)
        {
            data[i] = 0;
        }
		raster.setDataElements(0, 0, image.getWidth(), image.getHeight(), data);
	}

	/**
	 * Helper function which performs the actual hit-testing and drawing for
	 * both immutable fields and non-immutable fields.
	 * 
	 * <p>
	 * The objects in the field can either use GeomPortrayal or any
	 * SimplePortrayal2D for drawing.
	 * 
	 */
	@SuppressWarnings({ "restriction", "deprecation" })
	void hitOrDraw2(Graphics2D graphics, DrawInfo2D info, Bag putInHere)
	{
		GeomVectorField geomField = (GeomVectorField) field;
		if (geomField == null)
        {
            return;
        }

		geomField.updateTransform(info);
		GeomInfo2D gInfo = new GeomInfo2D(info, geomField.worldToScreen);

		final double xScale = info.draw.width / geomField.getFieldWidth();
		final double yScale = info.draw.height / geomField.getFieldHeight();
		GeomEdgeDrawInfo newinfo = new GeomEdgeDrawInfo(new DrawInfo2D(info.gui, info.fieldPortrayal, new Rectangle2D.Double(0, 0, xScale, yScale), info.clip),
				geomField.worldToScreen, new Point2D.Double(0,0));
		newinfo.fieldPortrayal = this;

		// ADDING SCW ///////
		
		Bag nodes = network.getAllNodes();
		HashMap edgemap = new HashMap();

		//////////////////////

        for(int x=0;x<nodes.numObjs;x++)
        {
        	// set up the start node
        	Object node = nodes.objs[x];
        	Bag edges = network.getEdgesOut(node);
        	
        	MasonGeometry geo1;
        	if(node instanceof MasonGeometry) geo1 = (MasonGeometry) node;
        	else if(node instanceof GeoNode) geo1 = ((GeoNode)node);
        	else continue;
        	Point locStart = geo1.getGeometry().getCentroid();
        	Coordinate c1 = geo1.getGeometry().getCoordinate();
        	if (locStart == null) continue;
        	
        	locStart.apply(geomField.jtsTransform);
        	locStart.geometryChanged();
			newinfo.draw.x =   locStart.getX() ;
			newinfo.draw.y =  locStart.getY() ;

            for(int y=0;y<edges.numObjs;y++)
            {
            	Edge edge = (Edge)edges.objs[y];
                   
            	Object node2 = edge.getOtherNode(node);
            	MasonGeometry geo2;
            	if(node2 instanceof MasonGeometry) geo2 = (MasonGeometry) node2;
            	else if(node2 instanceof GeoNode) geo2 = ((GeoNode)node2);
            	else continue;
            	Point locStop = geo2.getGeometry().getCentroid();
            	Coordinate c2 = geo2.getGeometry().getCoordinate();
            	if (locStop == null) continue;

            	// only include the edge if we've not included it already.
            	if (network.isDirected()){
            		if (edgemap.containsKey(edge)) continue;
            		
                }
            	edgemap.put(edge, edge);
            	GeometryFactory gf = new GeometryFactory();
            	LineString ls;
            	if(edge.info instanceof GeomPlanarGraphEdge){
            		ls = ((GeomPlanarGraphEdge) edge.info).getLine();
            	}
            	else if(edge.info instanceof LineString){
            		ls = (LineString) edge.info;
            	}
            	else
            		ls = gf.createLineString(new Coordinate[] {c1, c2});
            	MasonGeometry mg = new MasonGeometry(ls);
            	
            	Portrayal p = getPortrayalForObject(edge);
            	SimplePortrayal2D portrayal = (SimplePortrayal2D) p;

    			if (graphics == null)
    			{
    				if (portrayal.hitObject(mg, info))
                    {
                        putInHere.add(new LocationWrapper(mg, ls.getCentroid(), this));
                    }
    			}
    			else
    			{
    				if (portrayal instanceof GeomPortrayal)
                    {
                        portrayal.draw(mg, graphics, gInfo);
                    }
    				else
    				{
    					System.out.println("GAHHH");
    				}
    			}
            }
        }
		
	}

	/** Sets the underlying field, after ensuring its a GeomVectorField. */
    @Override
	public void setField(Object field)
	{
		if (field instanceof GeomVectorField)
        {
            super.setField(field);
        }  // sets dirty field already
		else
        {
            throw new RuntimeException("Invalid field for GeomNetworkFieldPortrayal: " + field);
        }
	}

	HashMap<Object, LocationWrapper> selectedWrappers = new HashMap<Object, LocationWrapper>();

    @Override
	public boolean setSelected(LocationWrapper wrapper, boolean selected)
	{
		if (wrapper == null)
        {
            return true;
        }
		if (wrapper.getFieldPortrayal() != this)
        {
            return true;
        }

		Object obj = wrapper.getObject();
		boolean b = getPortrayalForObject(obj).setSelected(wrapper, selected);
		if (selected)
		{
			if (b == false)
            {
                return false;
            }
			selectedWrappers.put(obj, wrapper);
		}
		else
        {
            selectedWrappers.remove(obj);
        }
		return true;
	}

    String edgeLocation(Edge edge)
    {
    // don't use toString, too much info
            
    if (edge == null)
        return "(Null)";
    else if (edge.owner() == null) 
        return "(Unowned)" + edge.from() + " --> " + edge.to();
    else if (edge.owner().isDirected())
        return edge.from() + " --> " +edge.to();
    else 
        return edge.from() + " <-> " + edge.to();
    }
            
            
// The easiest way to make an inspector which gives the location of my objects
public LocationWrapper getWrapper(Edge edge)
    {
    final GeomVectorField field = (GeomVectorField)this.field;
    return new LocationWrapper( edge.info, edge, this )
        {
        public String getLocationName()
            {
            Edge edge = (Edge)getLocation();
            if (field != null && network != null)
                {  
                // do I still exist in the field?  Check the from() value
                Bag b = network.getEdgesOut(edge.from());
                // if (b != null)  // no longer necessary
                for(int x=0;x<b.numObjs;x++)
                    if (b.objs[x] == edge)
                                            
                        return edgeLocation(edge);
                }
            return "Gone.  Was: " + edgeLocation(edge);
            }
        };
    }
}
