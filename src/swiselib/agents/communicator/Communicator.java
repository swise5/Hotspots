package swiselib.agents.communicator;

import java.util.ArrayList;

/**
 * Communicator
 * 
 * @author swise
 *
 * interface which determines how objects can exchange information with one another
 *
 */
public interface Communicator{
	
	public ArrayList getInformationSince(double time);
	public void learnAbout(Object o, Information i);
	
}