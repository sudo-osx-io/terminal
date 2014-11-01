/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the Lot class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import cern.jet.random.Uniform;
import java.awt.Color;       // For colors

/**
 * The lot (aggregate class) for ARMSR
 */

public class Lot {

	private static int nextID = 0; // A class variable to keep track of what 
	                               //   farm this is

	private ArrayList cells;    // A list of all cells belonging to the subdvision
	private java.awt.Color color;  // The color of all cells belonging to this 
                                   //  aggregate
	private int lotID;             // The id for this instance
	private Subdivision sub;       // This is the subdivision that owns the lot
	private Owner ownedBy;         // Lots are owned by residents
	private Township township;     // the township the lot is in

	// The only constructor requires you to know ahead of time the Lot
	//    cells and the subdivision
	public Lot(ArrayList list, Subdivision s)
	{
		// This specifies what cells the farm owns
		cells = list;
		
		// This sets the individual sub ID
		this.lotID = nextID++;

		// Set the subdivision
		sub = s;

		// The color for the lot is the color of the subdivision
		color = s.getColor();

		//		System.out.println("SubID is " + subID + "\n");
	}

	// this method sets the owned by parameter and makes sure all the cells
	//   are owned by the same owner
	public void setOwnedBy(Owner o) {

		// set the lot's owned by
		ownedBy = o;

		// set the owned by for the cells
		for (int i=0; i<cells.size(); i++) {
			Cell currentCell = (Cell)cells.get(i);

			currentCell.setOwnedBy(o);
		}
	}

	// accessor methods
	public ArrayList getCells() {
		return cells;
	}

	// this adds an array list of cells to the cell list
	public void addToCells(ArrayList cl) {

		cells.addAll(cl);		
	}

	public java.awt.Color getColor() {
		return color;
	}

	public Owner getOwnedBy() {
		return ownedBy;
	}

	public Subdivision getSub() {
		return sub;
	}

	public Township getTownship() {
		return township;
	}

	public void setTownship(Township t) {
		township = t;
	}

	// this calculates the average forest coverage by the cells below
	public double getCalculatedAvgForest() {

		// variable for the result
		double avgForest=0.0;

		// loop through all the cells in the lot
		for (int i=0; i<cells.size(); i++) {
			
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get its forest value and add it to the total
			avgForest += currentCell.getForest();
			//check that it's actually returning some value
			//System.out.println("Forest is " + avgForest);
		}

		// divide by the number of cells to create an average
		avgForest /= cells.size();
		//Check it's averaging appropriately
		//System.out.println("cells are " + cells.size() + " and avgForest is " + avgForest);

		return avgForest;
	}

	// calculate the panoramic view this is 1 if there is any cell in a lot
	//  that has more than .5 of its Moore neighbors at least 50 ft lower
	public double getCalculatedPanoramicView() {

		// a return variable
		double panoramicView = 0.0;

		// loop through all the cells in the lot
		for (int i=0; i<cells.size(); i++) {
			
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// test if own elevation is at least 50ft higher than
			// most of its neighboring cells
			if ( currentCell.testPanoramicView() ) {
				panoramicView = 1.0;
			}
		}
		//Check it's assigning panoramic view appropriately
		//System.out.println("Panoramic view is " + panoramicView);
		return panoramicView;

	}

	// calculate the relief, this is the difference between the highest and
	//   lowest points in the lot
	public double getCalculatedRelief() {

		// a return variable
		double relief = 0.0;
		
		// max and min elevations
		double max = 0.0;
		double min = 1.0;

		// loop through all the cells in the lot
		for (int i=0; i<cells.size(); i++) {
			
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the maximum elevation
			if ( currentCell.getElevation() > max ) {
				max = currentCell.getElevation();
				//Check how it varies
				//System.out.println("maxElevation is " + max);
			}

			// get the minimum elevation
			if ( currentCell.getElevation() < min ) {
				min = currentCell.getElevation();
				//Check how it varies
				//System.out.println("minElevation is " + min);
			}
		}
		
		// calculate the relief
		relief = max - min;
		//System.out.println("relief is " + relief);

		return relief;

	}

	// this calculates the average water coverage by the cells below
	public double getCalculatedAvgWater() {

		// variable for the result
		double avgWater=0.0;

		// loop through all the cells in the lot
		for (int i=0; i<cells.size(); i++) {
			
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get its forest value and add it to the total
			avgWater += currentCell.getWater();
			//check that it's actually returning some value
			//System.out.println("Water is " + avgWater);
		}

		// divide by the number of cells to create an average
		avgWater /= cells.size();
		//Check it's averaging appropriately
		//System.out.println("cells are " + cells.size() + " and avgWater is " + avgWater);

		return avgWater;
	}

}
