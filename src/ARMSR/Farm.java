/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the farm class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import cern.jet.random.Uniform;
import java.awt.Color;       // For colors

/**
 * The farm (aggregate class) for ARMSR
 */

public class Farm extends Owner {

	private static int nextID;     // A class variable to keep track of what 
	                               //   farm this is

	private ArrayList cells;       // A list of all cells belonging to the farm
	private java.awt.Color color;  // The color of all cells belonging to this 
                                   //  aggregate
	private int farmID;            // The id for this instance

	private boolean forSale;       // If true the farm is for sale

	// The only constructor requires you to know ahead of time the farm 
	//    cells that are going to be in the farm
	public Farm(ArrayList list)
	{
		// This specifies what cells the farm owns
		cells = list;
		
		// This sets the individual farm ID
		this.farmID = nextID++;

		// This sets the color for this farm, it makes them so they are 
		//   a few steps apart but also cycles through the yellow colors
		// the 20 and 230 in here are kind of random constants, something else
		//   might work better
		color = new java.awt.Color(java.awt.Color.yellow.getRGB() + ( ( farmID * 20 ) ) % 230 );

		//		System.out.println("FarmID is " + farmID + "\n");

		// initially all farms aren't for sale
		forSale = false;
	}

	// accessor methods
	public ArrayList getCells() {
		return cells;
	}

	public java.awt.Color getColor() {
		return  color;
	}

	public boolean getForSale() {
		return forSale;
	}

	public void setForSale(boolean v) {
		forSale = v;
	}
}
