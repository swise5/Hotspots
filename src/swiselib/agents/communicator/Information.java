package swiselib.agents.communicator;

/**
 * Information
 * 
 * @author swise
 *
 * Basic class which defines structure for information, the time it was gained, the source of it, and its 
 * valence.sentiment value.
 */
public class Information {
	long timeRecorded = -1;
	Object sourceOfInfo = null;
	Object info;
	int valence; // a value between 0 and 10, 10 being highest valence and 0 being lowest valence
	
	/**
	 * Constructor 
	 * 
	 * @param o - the information itself
	 * @param time
	 * @param source
	 * @param valence
	 */
	public Information(Object o, long time, Object source, int valence){
		this.timeRecorded = time;
		this.sourceOfInfo = source;
		this.info = o;
		this.valence = valence;
	}
	
	// getters and setters
	public long getTime(){ return timeRecorded; }
	public Object getInfo(){ return info; }
	public Object getSource(){ return sourceOfInfo; }
	public int getValence(){ return valence; }
}
