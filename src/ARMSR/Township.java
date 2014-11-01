/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the Township class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import cern.jet.random.Uniform;
import java.awt.Color;       // For colors
import uchicago.src.sim.space.*;  // for object2dgrid

/**
 * The subdvision (aggregate class) for ARMSR
 */

public class Township {

	private static int nextID = 0; // A class variable to keep track of what 
	                               //   subdivision this is

	private ArrayList cells;       // A list of all cells belonging to the Township
	private ArrayList residentsList;   // The residents of a township

	private static ArrayList subList;   // The remnant subs of a township

	
	private int townshipID;        // The id for this instance

	private int zonedMinLotSize;       // The zoned minimum lot size

	// The only constructor requires nothing ahead of time
	public Township()	
	{
		// This sets the individual township ID
		this.townshipID = nextID++;

		//		System.out.println("TownshipID is " + townshipID + "\n");

	}

	// accessor methods
	// set and get zoned minimum lot size
	public void setZonedMinLotSize(int z) {
		zonedMinLotSize = z;
	}

	public int getZonedMinLotSize() {
		return zonedMinLotSize;
	}

	public void addCell(Cell c) {
		if (cells != null) {
			cells.add(c);
		}
		else {
			cells = new ArrayList();
			cells.add(c);
		}
	}

	public ArrayList getCells() {
		return cells;
	}


	//set the subList
	public static void setSubList(ArrayList s) {
		subList = s;
	}


	public void addResident(Resident r) {
		if (residentsList != null) {
			residentsList.add(r);
		}
		else {
			residentsList = new ArrayList();
			residentsList.add(r);
		}
		// confirm that resident is in the list
		if (!(residentsList.contains(r))) {
				System.out.println("Resident was not added to township!");
				System.exit (-1);
		}			

		//		System.out.println("Adding resident to township " + townshipID + " count at " + residentsList.size());
	}

	public void removeResident(Resident r) {
		if (residentsList.contains(r)) {
			residentsList.remove(r);
			//System.out.println("Removing resident to township " + townshipID + " count at " + residentsList.size());

			//make sure resident is no longer in township
			if (residentsList.contains(r)) {
				System.out.println("Resident is still there!");
				System.exit (-1);
			}
		}
	}

	// returns true if township owns cell and false otherwise
	public boolean townshipOwnsCell(Cell c) {
		if (cells.contains(c)) {
			return true;
		}
		else {
			return false;
		}
	}

	// this loops through all the residents and adds two to the count for all
	//   high income individuals and 1 for all low income
	public int calculateIncome() {
		
		// aggregate variable of return
		int income=0;

		// loop through all residents
		if (residentsList != null) {
			for(int i=0; i<residentsList.size(); i++) {
				
				// get the current resident
				Resident currResident = (Resident)residentsList.get(i);
				
				// if the resident is high income add 2, else 1
				if (currResident.getIncome() == Resident.INCOME_HIGH) {
					income+=2;
				}
				else {
					income++;
				}
			}
		}

		//		System.out.println("Township " + townshipID + " has income " + income);

		return income;
	}

	// This method calculates the average forest coverage per cell
	public double calculateAvgForest() {
		double avgForest =0.0;

		// go through all the cells in the township
		for (int i=0; i < cells.size(); i++) {

			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// add the forest value to the sum
			avgForest+=currentCell.getForest();
			//System.out.println("Forest coverage is " + avgForest);
		}

		// take the average for the whole township
		avgForest/=cells.size();
		//System.out.println("cells are " + cells.size() + " and avgForest is " + avgForest);
		
		return avgForest;
	}


	// this loops through all the subdivisions and counts how many there
	// are of each type
	public int countSubs(int type) {
		
		// counter
		int counter=0;

		// loop through all subs
		if (subList != null) {
			for(int i=0; i<subList.size(); i++) {
				
				// get the current sub
				Subdivision currSub = (Subdivision) subList.get(i);
				
				// if the sub is of the type specified AND it's in the
				// current township, add it to the count
				if (currSub.getType() == type && (Township) currSub.getTownship() == this ) {
					counter = counter + 1;
				}
			}
		}

		//System.out.println("Township " + townshipID + " has " + counter + " " + type + " subs");

		return counter;
	}

}
