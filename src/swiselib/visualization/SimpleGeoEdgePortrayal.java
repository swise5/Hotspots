package swiselib.visualization;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;

import sim.portrayal.DrawInfo2D;
import sim.portrayal.network.SimpleEdgePortrayal2D;

/**
 * A simple portrayal to draw the edges of a network whose nodes are elements within a GeomVectorField.
 * 
 * @author swise
 *
 */
public class SimpleGeoEdgePortrayal extends SimpleEdgePortrayal2D {
	
	public SimpleGeoEdgePortrayal(Paint edgepaint){
		fromPaint = edgepaint;
		toPaint = edgepaint;
	}
	
    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
    {
    if (!(info instanceof GeomEdgeDrawInfo))
        throw new RuntimeException("Expected this to be an GeomEdgeDrawInfo: " + info);
    GeomEdgeDrawInfo e = (GeomEdgeDrawInfo) info;
    
    final int scaling = getScaling();
    
    Line2D.Double preciseLine = new Line2D.Double();
   
    double startXd = e.draw.x;
    double startYd = e.draw.y;
    final double endXd = e.secondPoint.x;
    final double endYd = e.secondPoint.y;
    final double midXd = ((startXd+endXd) / 2);
    final double midYd = ((startYd+endYd) / 2);     
    final int startX = (int)startXd;
    final int startY = (int)startYd;
    final int endX = (int)endXd;
    final int endY = (int)endYd;
    final int midX = (int)midXd;
    final int midY = (int)midYd;
    
    // shape == SHAPE_LINE
        {
        if (fromPaint == toPaint)
            {
            graphics.setPaint (fromPaint);
            double width = getBaseWidth();
            if (info.precise || width != 0.0)
                { 
                double scale = info.draw.width;
                if (scaling == SCALE_WHEN_SMALLER && info.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                    scale = 1;

                Stroke oldstroke = graphics.getStroke();
                double weight = 1;//getPositiveWeight(object, e);
                graphics.setStroke(new BasicStroke((float)(width * weight * scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));  // duh, can't reset a stroke, have to make it new each time :-(
                preciseLine.setLine(startXd, startYd, endXd, endYd);
                graphics.draw(preciseLine);
                graphics.setStroke(oldstroke);
                }
            else graphics.drawLine (startX, startY, endX, endY);
            }
        else
            {
            graphics.setPaint( fromPaint );
            double width = getBaseWidth();
            if (info.precise || width != 0.0)
                { 
                double scale = info.draw.width;
                if (scaling == SCALE_WHEN_SMALLER && info.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
                    scale = 1;

                Stroke oldstroke = graphics.getStroke();
                double weight = 1;//getPositiveWeight(object, e);
                graphics.setStroke(new BasicStroke((float)(width * weight * scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));  // duh, can't reset a stroke, have to make it new each time :-(
                preciseLine.setLine(startXd, startYd, midXd, midYd); 
                graphics.draw(preciseLine); 
                graphics.setPaint(toPaint);
                preciseLine.setLine(midXd, midYd, endXd, endYd); 
                graphics.draw(preciseLine); 
                graphics.setStroke(oldstroke);
                }
            else
                {
                graphics.drawLine(startX,startY,midX,midY);
                graphics.setPaint( toPaint );
                graphics.drawLine(midX,midY,endX,endY);
                }
            }
        }
            

    }

public boolean hitObject(Object object, DrawInfo2D range)
    {
    if (!(range instanceof GeomEdgeDrawInfo))
        throw new RuntimeException("Expected this to be an GeomEdgeDrawInfo: " + range);
    GeomEdgeDrawInfo e = (GeomEdgeDrawInfo) range;

    final int scaling = getScaling();
    
    double startXd = e.draw.x;
    double startYd = e.draw.y;
    final double endXd = e.secondPoint.x;
    final double endYd = e.secondPoint.y;
    
    double weight = 1;//getPositiveWeight(object, e);
    double width = getBaseWidth();

    final double SLOP = 5;  // allow some imprecision -- click 6 away from the line

    double scale = range.draw.width;
    if (scaling == SCALE_WHEN_SMALLER && range.draw.width >= 1 || scaling == NEVER_SCALE)  // no scaling
            scale = 1;

        Line2D.Double line = new Line2D.Double( startXd, startYd, endXd, endYd );
        if (width == 0)
            return (line.intersects(range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2));
        else
            return new BasicStroke((float)(width * weight * scale), BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER).createStrokedShape(line).intersects(
                range.clip.x - SLOP, range.clip.y - SLOP, range.clip.width + SLOP*2, range.clip.height + SLOP*2);

    }
	
}