/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the Developer class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import ARMSR.Subdivision;           // the subdivsion class

/**
 * The developer (aggregate class) for ARMSR
 */

public class Developer {

	private static int nextID = 0; // A class variable to keep track of what 
	                               //   developer this is

	private ArrayList subs;       // A list of all subs belonging to the Developer
	private int type;   // The type of developer
	
	private int developerID;        // The id for this instance

	// This constructor requires nothing ahead of time
	public Developer()	
	{
		// This sets the individual developer ID
		this.developerID = nextID++;

		//		System.out.println("DeveloperID is " + developerID + "\n");

	}

	// This constructor requires the type ahead of time
	public Developer(int t)	
	{
		// This sets the individual developer ID
		this.developerID = nextID++;

		//		System.out.println("DeveloperID is " + developerID + "\n");

		// specify the developer type
		type = t;
	}

	// accessor methods
	public void addSub(Subdivision s) {
		if (subs != null) {
			subs.add(s);
		}
		else {
			subs = new ArrayList();
			subs.add(s);
		}
	}

	public ArrayList getSubs() {
		return subs;
	}

	public int getType() {
		return type;
	}

	// canDevelop
	// checks to make sure the developer has the resources to develop, i.e.
	//   all of their current developments are full
	public boolean canDevelop() {

		// the return value
		boolean canDevelop = true;

		// check to make sure that we have subdivisions first, 
		//    if not we can always develop
		if (subs != null) {

			// loop through all the subs owned by the developer if any are not
			//   full the developer can't develop
			for (int i=0; i<subs.size(); i++) {
				
				// get the current sub
				Subdivision currentSub = (Subdivision)subs.get(i);
				
				// if it isn't full then set the the returnFlag to false
				if (! (currentSub.isFull())) {
					// found a non-full sub
					canDevelop = false;			

					// check for errors
					//					System.out.println("Non - full sub");
				}
			}
		}
		
		// This is a check to see if its working right
		if (canDevelop) {
			//			System.out.println("All subs full");
		}

		//		System.out.println("canDevelop is " + canDevelop);
		
		// return the flag
		return canDevelop;
	}
}
