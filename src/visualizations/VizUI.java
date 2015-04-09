package visualizations;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.text.SimpleDateFormat;
import java.util.Date;


import javax.swing.JFrame;

import sim.display.Console;
import sim.display.Controller;
import sim.display.Display2D;
import sim.display.GUIState;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.portrayal.DrawInfo2D;
import sim.portrayal.FieldPortrayal2D;
import sim.portrayal.geo.GeomPortrayal;
import sim.portrayal.geo.GeomVectorFieldPortrayal;
import sim.util.gui.ColorMap;
import sim.util.gui.SimpleColorMap;
import sim.util.media.chart.HistogramGenerator;
import swise.visualization.AttributePolyPortrayal;
import swise.visualization.FilledPolyPortrayal;

public class VizUI extends GUIState {

	Viz sim = null;
	
	public Display2D display;
	public JFrame displayFrame;

	HistogramGenerator valence;
	double [] valenceVals;

	double agentScale //= .075; // USA
		// = .02; // NY
		// = .035; // Area
//		= .005; // COSprings
		= .003; // COSprings
	
	private GeomVectorFieldPortrayal map = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal agents = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal agents2 = new GeomVectorFieldPortrayal();
	private GeomVectorFieldPortrayal fire = new GeomVectorFieldPortrayal(); // #WILDFIRE
	private GeomVectorFieldPortrayal roads = new GeomVectorFieldPortrayal(); // #WILDFIRE


	public VizUI(SimState state) {
		super(state);
		sim = (Viz) state;
	}
	
	public void start() {
		super.start();
		setupPortrayals();
	}

	/** Loads the simulation from a point */
	public void load(SimState state) {
		super.load(state);
		sim = (Viz) state;
		
		// we now have new grids. Set up the portrayals to reflect that
		setupPortrayals();
	}

	/**
	 * Specify how the various layers will be rendered
	 */
	void setupPortrayals() {

		// ---BACKGROUND---
		map.setField(sim.baseLayer);
		map.setPortrayalForAll(new FilledPolyPortrayal(Color.gray, Color.DARK_GRAY, true));
		
		// ---FIRST AGENT LAYER---
		agents.setField(sim.objectLayer);
		agents.setPortrayalForAll(new AttributePolyPortrayal(
				// DISPLAY_BY_TIME_OPTION: opaque
				//new SimpleColorMap(-5, 5, Color.RED, Color.GREEN), "Valence", new Color(0,0,0,0), true, agentScale));
				// COMPOSITE_IMAGE_OPTION: translucent
				//new SimpleColorMap(-5, 5, new Color(255, 0, 0, 50), new Color(0, 255, 0, 50)), "Valence", new Color(0,0,0,0), true, agentScale));
//				new SimpleColorMap(-5, 5, new Color(255, 0, 0, 50), new Color(0, 255, 0, 50)), "Valence", Color.black, true, agentScale));
				new SimpleColorMap(0, 10, new Color(255, 0, 0, 100), new Color(0, 255, 0, 100)), "Valence", new Color(0,0,0,0), true, agentScale));
		// simple red dot
		//agents.setPortrayalForAll(new GeomPortrayal(Color.red, agentScale, true)); 
	

		// ---SECOND AGENT LAYER---

		agents2.setField(sim.objectLayer2);
		agents2.setPortrayalForAll(new AttributePolyPortrayal(
				// DISPLAY_BY_TIME_OPTION: opaque
				//new SimpleColorMap(-5, 5, Color.RED, Color.GREEN), "Valence", new Color(0,0,0,0), true, agentScale));
				// COMPOSITE_IMAGE_OPTION: translucent
				new SimpleColorMap(-5, 5, new Color(255, 0, 0, 50), new Color(0, 255, 0, 10)), "Valence", new Color(0,0,0,0), true, agentScale));
		// simple yellow dot
		//agents2.setPortrayalForAll(new GeomPortrayal(Color.yellow, agentScale, true));

		// ---#WILDFIRE---
		
		fire.setField( sim.fireLayer );
		fire.setImmutableField(false);
		fire.setPortrayalForAll(new GeomPortrayal(new Color(153, 52, 4, 150),//new Color(255,0,0), 
				true));
		
		roads.setField(sim.roadLayer);
		roads.setPortrayalForAll(new GeomPortrayal(Color.DARK_GRAY,false));


		// ---HISTOGRAM OF VALENCES---
		valenceVals = new double [11];
		valence.removeAllSeries();
		valence.addSeries(valenceVals, 11, "Valence", null);
		valence.setDomainAxisRange(-5, 5);
		valence.setRangeAxisRange(0, 2000);
		state.schedule.scheduleRepeating(0, 10000, new ValenceTracker());

		// reset stuff
		// reschedule the displayer
		display.reset();
		display.setBackdrop(Color.black);

		// redraw the display
		display.repaint();
	}

    /** Tracks the agent valences */
    class ValenceTracker implements Steppable {
		public void step(SimState state){
			Viz world = (Viz)state;
			valence.removeAllSeries();
			double [] vals = new double[world.valences.size()];
			for(int i = 0; i < world.valences.size(); i++){
				vals[i] = world.valences.get(i);
			}
			valence.addSeries(vals, 11, "Valence", null);
		}
	}

    /**
     * Set up the display and attach the relevant features
     */
	public void init(Controller c) {
		super.init(c);

		// set up the display
		display = new Display2D((int)(1.5 * sim.grid_width), (int)(1.5 * sim.grid_height), this);

		display.attach(map, "Landscape");
		display.attach(roads, "Roads");
		display.attach(agents2, "Agent2");
		display.attach(agents, "Agents");
		display.attach(fire, "Wildfire");

		// DISPLAY_BY_TIME_OPTION: show a timestamp
		
		// ---TIMESTAMP---
		display.attach(new FieldPortrayal2D()
	    {
		    Font font = new Font("SansSerif", 0, 24);  // keep it around for efficiency
		    SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd HH:mm zzz");
		    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
		        {
		        String s = "";
		        if (state !=null) // if simulation has not begun or has finished, indicate this
		            s = state.schedule.getTimestamp("Before Simulation", "Simulation Finished");
		        graphics.setColor(Color.white);
		        Viz world = (Viz) state;
		        if (state != null && world.startTime != null){
		        	// specify the timestep here
			        Date time = new Date(state.schedule.getSteps() * 3600000 + world.startTime.getTime());
			        s = ft.format(time);	
		        }

		        graphics.drawString(s, (int)info.clip.x + 10, 
		                (int)(info.clip.y + 10 + font.getStringBounds(s,graphics.getFontRenderContext()).getHeight()));

		        }
		    }, "Time");
		
		// ---end Timestamp---
		    
		// ---LEGEND---
		display.attach(new FieldPortrayal2D()
	    {
		    Font font = new Font("SansSerif", 0, 20);  // keep it around for efficiency
		    int [] legendVals = new int [] {-5, -3, 0, 3, 5};
		    SimpleColorMap map = new SimpleColorMap(-5, 5, Color.RED, Color.GREEN);
		    
		    public void draw(Object object, Graphics2D graphics, DrawInfo2D info)
		        {
		        String s = "0";
		        double dx = font.getStringBounds(s,graphics.getFontRenderContext()).getWidth(),
		        	dy = font.getStringBounds(s,graphics.getFontRenderContext()).getHeight();
		        for(int i = 0; i < legendVals.length; i++){

		        	s = "" + legendVals[i];
		        	dx = font.getStringBounds(s,graphics.getFontRenderContext()).getWidth();

		        	int x = (int)(info.clip.x + info.clip.width - 50),
		        		y = (int)(info.clip.y + info.clip.height - dy*i - 30);
		        	
			        graphics.setColor(map.getColor(legendVals[i]));
		        	graphics.fillRect(x, y, 10, (int)dy);		        	
		        	
			        graphics.setColor(Color.white);
			        x = (int)(info.clip.x + info.clip.width - dx - 10);
			        graphics.drawString(s, x, y + (int)dy - 5);
		        }

		        }
		    }, "Legend");
		
		// ---end Legend---
		
		displayFrame = display.createFrame();
		c.registerFrame(displayFrame); // register the frame so it appears in the "Display" list
		displayFrame.setVisible(true);

		// OPTION: display a histogram of the valences
		valence = new HistogramGenerator();
		valence.setTitle("Distribution of Positivity/Negativity Valence");
		valence.setDomainAxisLabel("Valence");
		valence.setRangeAxisLabel("Count");
		JFrame histoFrame = valence.createFrame(this);
		histoFrame.pack();
		controller.registerFrame(histoFrame);
	}

	/** Quits the simulation and cleans up. */
	public void quit() {
		super.quit();

		if (displayFrame != null)
			displayFrame.dispose();
		displayFrame = null; // let gc
		display = null; // let gc
	}

	/** Runs the simulation */
	public static void main(String[] args) {
		VizUI gui = null;
		
		try {
			Viz viz = new Viz(System.currentTimeMillis());
			gui = new VizUI( viz );
		} catch (Exception ex) {
			System.out.println(ex.getStackTrace());
		}

		Console console = new Console(gui);
		console.setVisible(true);
	}

	/** Returns the name of the simulation */
	public static String getName() {
		return "Visualization";
	}

	/** Allows for users to modify the simulation using the model tab */
	public Object getSimulationInspectedObject() {
		return state;
	}

}