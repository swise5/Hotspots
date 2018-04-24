package swiselib.objects.network;

import java.util.ArrayList;

import sim.field.network.Edge;
import sim.util.geo.MasonGeometry;

public class MultiPartEdge extends Edge {

	public MasonGeometry geometry;
	public ArrayList [] elements;
	public Integer [] direction;
	
	public MultiPartEdge(Object from, Object to, MasonGeometry mg, int numElements, Integer [] directions){
		super(from, to, mg);
		this.geometry = mg;
		elements = new ArrayList [numElements];
		for(int i = 0; i < numElements; i++)
			elements[i] = new ArrayList ();
		this.direction = directions;
	}

	
	public ArrayList addObject(MasonGeometry object, int direction){
		
		return null;
	}
	
}