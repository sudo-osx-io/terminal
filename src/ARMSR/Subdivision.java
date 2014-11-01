/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the Subdivision class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import cern.jet.random.Uniform;
import java.awt.Color;       // For colors
import uchicago.src.sim.space.*;  // for object2dgrid

/**
 * The subdvision (aggregate class) for ARMSR
 */

public class Subdivision extends Owner {

	// some constants needed to set the subdivision type
	public static final int COUNTRY = 0;
	public static final int REMNANT = 1;
	public static final int HORT = 2;	
	public static final int DONTDEVELOP = 3;
	public static final int SPLIT = 4;

	// some water constants needed to determine sub type
	// and the possibility of incorporating a cell into a lot
	public static final double waterSubThreshold = 0.01;  //  1% of water in sub to be "present"
	public static final double waterCellThreshold = 0.9; // max 90% of water in cell to be in lot
	public static final double forestThreshold = 10.0; // threshold for forest (in acres)
	public static final double forestSmallThreshold = 0.1; // threshold for forest (in %, for small subs)
	public static final double rollingThreshold = 40.0; // threshold for "rollingness" (in m)

	// global counts of the number of each type
	private static int countryCount = 0;
	private static int remnantCount = 0;
	private static int hortCount = 0;

	// Quality spaces
	private static QualitySpace forestSpace = null;
	private static QualitySpace roadSpace = null;
	private static QualitySpace soilSpace = null;
	private static QualitySpace waterSpace = null;
	private static QualitySpace elevationSpace = null;

	private static int nextID = 0; // A class variable to keep track of what 
	                               //   farm this is

	private ArrayList cells;       // A list of all cells belonging to the subdvision
	private ArrayList lots;        // the lots the cells belong to
	
	private ArrayList lotsForSale; // lots that are for sale
	
	private java.awt.Color color;  // The color of all cells belonging to this 
                                   //  aggregate
	private int subID;             // The id for this instance

	private int type;              // this is the subdivision type

	private Object2DGrid world;    // this is the world of the sub

	private int maxY;              // what is the smallest and 
    private int minY;              //  largest X & Y values assigned
	private int maxX;              //  to our subdivision
	private int minX;

	private int minLotSize;        // this is the min lot size for a sub
	                               //  its determined by its type

	// The only constructor requires you to know ahead of time the Subdivison
	//    cells that are going to be in the Subdivison
	public Subdivision(ArrayList list, Object2DGrid w)
	{
		// This specifies what cells the farm owns
		cells = list;

		// set the world to the current world
		world = w;
		
		//determine the bounds for the subdivision
		determineBounds();
		
		// This sets the individual sub ID
		this.subID = nextID++;

		//		System.out.println("SubID is " + subID + "\n");
	}

	//set the qualityspaces
	public static void setQualitySpaces(QualitySpace r, QualitySpace s, QualitySpace f, QualitySpace w, QualitySpace e) {
		roadSpace = r;
		soilSpace = s;
		forestSpace = f;
		waterSpace = w;
		elevationSpace = e;
	}


	// getAvgWaterForest
	// this returns the cumulative area of forest or water
	// NOT CURRENTLY USED
	public double getAvgWaterForest() {

		// the return value
		double avgWaterForest = 0.0;
		double areaWater = 0.0;
		double areaForest = 0.0;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			// get the forest for the current cell
			areaWater = waterSpace.getValueAt(x,y);
			areaForest = forestSpace.getValueAt(x,y);
			
			// because it's cumulative,
			// choose whatever is the highest value from both
			// (assumes that they cover the same area within cell
			if (areaWater >= areaForest ) {
				avgWaterForest += areaWater;
				//check calculation
				//System.out.println("areaWater is " + areaWater); 
			}
			else {
				avgWaterForest += areaForest;
				//check calculation
				//System.out.println("areaForest is " + areaForest); 
			}

			//check calculation
			//System.out.println("avgWaterForest is " + avgWaterForest);
		}
		// calculate proportion of area in relation to size of sub
		avgWaterForest /= cells.size();

		/*
		// check calculation
		System.out.println("# of cells is " + cells.size() );
		System.out.println("proportional avgWaterForest is " + avgWaterForest);
		*/

		return avgWaterForest;
	}

	// getArea
	// this returns the area of a natural feature
	public double getArea(QualitySpace q) {

		// the return value
		double area = 0.0;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			// get the forest for the current cell and add it to previous value
			area += q.getValueAt(x,y);
			
			//check calculation
			//System.out.println("area is " + area); 
		}

		return area;
	}

	// getAvgLevel
	// this returns the average level of the quality space for everything
	public double getAvgLevel(QualitySpace q) {

		// the return value
		double level = 0.0;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			// get the forest for the current cell
			level += q.getValueAt(x,y);
		//check calculation
		//System.out.println("level is " + level); 

		}


		// take the aggregate of all fractions of coverage and divide
		//  by the number of cells
		level /= cells.size();
		//System.out.println("cells.size is " + cells.size()); 
		//System.out.println("average level is " + level); 

		// return the average level for the whole subdivision
		return level;
	}

	// getMinLevel
	// this returns the minimum level of the quality space for everything
	public double getMinLevel(QualitySpace q) {

		// the return value
		double level = 9999999.9;
		double value = 0.0;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			//get current value
			value = q.getValueAt(x,y);

			// if value is less than level, update level
			if ( value < level ) {
				//	System.out.println("value " + value + " is less than level " + level );
				level = value;
			}
		}

		// return the mimimum level for the whole subdivision
		return level;
	}


	// getMaxLevel
	// this returns the maximum level of the quality space for everything
	public double getMaxLevel(QualitySpace q) {

		// the return value
		double level = -0.1;
		double value = -0.2;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			//get current value
			value = q.getValueAt(x,y);

			// if value is greater than level, update level
			if ( value > level ) {
				//System.out.println("value " + value + " is greater than level " + level );
				level = value;
			}
		}

		// return the max level for the whole subdivision
		return level;
	}

	// calculate the amount of development, right now this is proxied by
	//   the amount of road in the subdivision
	public double getCalculatedDevelopment() {
		return getAvgLevel(roadSpace);
	}

	// isRolling
	// This returns true if the difference between the highest point and
	//  the lowest point is greater than threshold
	public boolean isRolling() {

		// get the min and max levels
		double min = getMinLevel(elevationSpace);
		double max = getMaxLevel(elevationSpace);

		//System.out.println("Min elev is " + min + ", and Max elev is " + max);
	   
		// subtract them and see if the difference is great enough
		if ( (max - min) > rollingThreshold ) {
			//System.out.println("isRolling is true");
			return true;
		}
		else {
			//System.out.println("isRolling is false");
			return false;
		}
	}

	// isNotProductive: CURRENTLY NOT USED
	// This returns true if more than a quarter of the farm has less than .75
	//  soilquality
	public boolean isNotProductive() {

		// the number of acres less than 0.75
		int nCells=0;

		// loop through each cell
		for (int i=0;i<cells.size();i++) {
			// get the current cell
			Cell currentCell = (Cell)cells.get(i);

			// get the x and y values for the current value
			int x = currentCell.getX();
			int y = currentCell.getY();

			// get the level for the current cell
			if (soilSpace.getValueAt(x,y) < 0.75 ) {
				nCells++;
			}
		}

		// if the number of cells is greater than a quarter of the farm
		// then the farm is unproductive
		if (nCells > (0.25 * cells.size())) {
			return true;
		}
		else {
			return false;
		}
	}

	// determineSubType
	// This function will look at the cells owned by the sub and then determine
	//   the appropriate tyep for it
	public int determineSubType() {

		// the return type
		int returnSubType = -1;

		// the subdivision type is determined by the geography of the
		//   land associated with the subdivision
		if (cells.size()>=160) {

			//check to see if error, print error message and exit program
			int subSize = (int) cells.size();
			if ( subSize < 160 ) {
				System.out.println("subSize is smaller than 160!");
				System.exit(-1);
			}

			// if there is water (>= 1% sub area)
			// or more than threshold forest or rollingness
			if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) || 
				 ( getArea(forestSpace) >= forestThreshold ) || 
				 ( isRolling() == true ) ) {

				//make sure these conditions are met
				if ( getArea(forestSpace) < forestThreshold && 
					 getAvgLevel(waterSpace) < waterSubThreshold 
					 && isRolling()==false ) {

					System.out.println("1: should be hort or country!");
					System.exit(-1);
					
				}
				
				// randomly decide between HORT and REMNANT
				double p = Model.getUniformDoubleFromTo(0.0, 1.0);

				//print out p and make sure it matches what happens next.
				//System.out.println("p is " + p);

				if (p < 0.5) {
					returnSubType = HORT;

					// remove forest cover
				}
				else {
					returnSubType = REMNANT;
				}
			}
			else {
				//make sure it's not mistaken
				if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) || 
					 ( getArea(forestSpace) >= forestThreshold ) ||
					 ( isRolling() == true ) ) {
					System.out.println("2: should be hort or remnant!");
					System.exit(-1);
				}

				// randomly decide between HORT and COUNTRY
				double p = Model.getUniformDoubleFromTo(0.0, 1.0);

				//print out p and make sure it matches what happens next.
				//System.out.println("p is " + p);

				if (p < 0.5) {
					returnSubType = HORT;
				}
				else {
					returnSubType = COUNTRY;
				}
			}
		}

		// This decision process will look like the previous one till
		//  we have adjacency in
		else if (cells.size()>=80) {

			//check to see if error, print error message and exit program
			int subSize = (int) cells.size();
			if ( subSize < 80 || subSize > 160 ) {
				System.out.println("subSize is smaller than 80 or larger than 160!");
				System.exit(-1);
			}

			// if there is water
			// or rolling hills or more than threshold forest
			if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) || 
				 ( getArea(forestSpace) >= forestThreshold ) || 
				 ( isRolling() == true ) ) {

				//make sure these conditions are met
				if ( ( getArea(forestSpace) < forestThreshold ) && 
					 ( getAvgLevel(waterSpace) < waterSubThreshold ) && 
					 ( isRolling()==false ) ) {
					
					System.out.println("forest and water are not at appropriate level!");
					System.exit(-1);
					
				}

				// randomly decide between HORT and REMNANT
				double p = Model.getUniformDoubleFromTo(0.0, 1.0);
				if (p < 0.5) {

					//check on this value
					//System.out.println("p is " + p);

					returnSubType = HORT;
				}
				else {
					returnSubType = REMNANT;
				}
			}
			else {
				//make sure it's not mistaken
				if ( ( getArea(forestSpace) >= forestThreshold ) || 
					 ( getAvgLevel(waterSpace) >= waterSubThreshold ) ||
					 ( isRolling() == true ) ) {
					System.out.println("3: should be hort or remnant!");
					System.exit(-1);
				}

				// randomly decide between HORT and COUNTRY
				double p = Model.getUniformDoubleFromTo(0.0, 1.0);

				//System.out.println("p is " + p);

				if (p < 0.5) {
					returnSubType = HORT;
				}
				else {
					returnSubType = COUNTRY;
				}
			}
		}
		else { // if smaller than 80 cells
			//check to see if error, print error message and exit program
			int subSize = (int) cells.size();
			if ( subSize > 80 ) {
				System.out.println("subSize is larger than 80!");
				System.exit(-1);
			}



			// if there is a road 
			if ( getMaxLevel(roadSpace) > 0 ) {  

				//System.out.println("Max road level is > 0");

				//make sure that this condition is met
				if ( getMaxLevel(roadSpace) == 0 ) {  
					System.out.println("should not be developed!");
					System.exit(-1);
				}

				// if there is water
				// or rolling hills or more than threshold forest ( 10 acres )
				if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) ||
					 ( getAvgLevel(forestSpace) >= forestSmallThreshold ) || 
					 ( isRolling() == true) ) {
					
					//make sure this condition is met
					if ( ( getAvgLevel(forestSpace) < forestSmallThreshold ) &&
						 getAvgLevel(waterSpace) < waterSubThreshold && isRolling()==false ) {
						System.out.println("4: should be hort or country!");
						System.exit(-1);
					}

					// randomly decide between HORT and REMNANT
					double p = Model.getUniformDoubleFromTo(0.0, 1.0);

					//check on this value
					//System.out.println("p is " + p);
					
					if (p < 0.5) {
						returnSubType = HORT;
					}
					else {
						returnSubType = REMNANT;
					}
				}
				else {

					//make sure it's not supposed to be something else
					if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) ||
						 ( getAvgLevel(forestSpace) >= forestSmallThreshold ) || 
						 ( isRolling()==true) ) {
						System.out.println("5: should be hort or remnant!");
						System.exit(-1);
					}

					// randomly decide between HORT and COUNTRY
					double p = Model.getUniformDoubleFromTo(0.0, 1.0);

					//check on this value
					//	System.out.println("p is " + p);

					if (p < 0.5) {
						returnSubType = HORT;
					}
					else {
						returnSubType = COUNTRY;
					}
				}
			}
			else {
				//make sure it shouldn't be developed
				if ( getMaxLevel(roadSpace) > 0  ) {  
					System.out.println("should be developed!");
					System.exit(-1);
				}

				// if there is water
				// or rolling hills or more than 10 acres forest
				if ( ( getAvgLevel(waterSpace) >= waterSubThreshold ) || 
					 ( getAvgLevel(forestSpace) >= forestSmallThreshold ) || 
					 ( isRolling()==true ) ) {
					//make sure this condition is met
					if ( ( getAvgLevel(forestSpace) < forestSmallThreshold ) &&
						 getAvgLevel(waterSpace) < waterSubThreshold && isRolling()==false ) {
						System.out.println("6: should be hort or country!");
						System.exit(-1);
					}

					// randomly decide between HORT and REMNANT
					double p = Model.getUniformDoubleFromTo(0.0, 1.0);

					//check on this value
					//	System.out.println("p is " + p);

					if (p < 0.5) {
						returnSubType = HORT;
					}
					else {
						returnSubType = REMNANT;
					}
				}

				else {
				returnSubType = DONTDEVELOP;
				}
			}
		}

		// return the return type
		return returnSubType;

		// This is the old determine subtype that is random
		/*
		// determines the subdivision type, right now this is set randomly 
		// in the future it will be driven by demand or something else
		double p = Model.getUniformDoubleFromTo(0.0, 1.0);
		double third = 1.0/3.0;
		int returnType = -1;

		if (p < third) {
			returnType = COUNTRY;
		} else if (p < (2 * third) ) {
			returnType = REMNANT;
		}
		else {
			returnType = HORT;
		}
		
		//System.out.println("p is " + p + " type is " + returnType + "\n");

		return returnType;
		*/
	}


	

	// accessor methods
	public ArrayList getCells() {
		return cells;
	}

	public java.awt.Color getColor() {
		return color;
	}

	// this method return the minlotsize
	public int getMinLotSize() {
		return minLotSize;
	}

	public int getType() {
		return type;
	}

	// setType
	// setting the type involves changing the color and incrementing counts
	//  as well
	public void setType(int t) {

		// check to see if its a country
		if (t == COUNTRY) {
			
			// set the type and increment the counts
			type = COUNTRY;
			countryCount++;

			// country subs are red
			color = java.awt.Color.red;
			
			// set the minLotSize to 1
			minLotSize = 1;

			//System.out.println("color is " + this.getColor() + "\n");
			
		}
		
		// check to see if its hort
		else if (t == HORT) {

			// set the type and increment the counts
			type = HORT;
			hortCount++;

			// hort subs are blue
			color = java.awt.Color.blue;

			// set the minLotSize to 2
			minLotSize = 2;

			//System.out.println("color is " + this.getColor() + "\n");

		}

		// check to see if its remnant
		else if (t == REMNANT) {

			// set the type and increment the counts
			type = REMNANT;
			remnantCount++;
			
			// remnant subs are green
			color = java.awt.Color.green;

			// set the minLotSize to 3
			minLotSize = 3;

			//System.out.println("color is " + this.getColor() + "\n");

		}
		
		// uncomment this if you want it to print out what type each sub is
		//		System.out.println("CountryCount is " + countryCount + " remnantCount is " + remnantCount + " hortCount is " + hortCount);
	}

	// isFull
	// returns true if 75% of the lots are owned by residents
	public boolean isFull() {

		// first make sure we have lots at all, if we don't then we aren't full
		if (lots != null && lotsForSale != null) {

			// first get the count of lots
			int nLots = lots.size();
			int nLotsForSale = lotsForSale.size();

			// calculate fraction occupied
			// if nlots is 0 and lots for sale is 0 then all are occupied
			double occupied = 0;
			if ((nLots == 0) && (nLotsForSale == 0)) {
				occupied = 1;
			}
			// else it is the ratio of these numbers
			else {
				occupied = 1.0 - (double)nLotsForSale / (double)nLots;
			}

			// set the threshold
			double threshold = .75;

			// uncomment this and the lines below to make sure check is correct
			//			System.out.println("nLots is " + nLots + " nLotsForSale is " + nLotsForSale);

			// if there are more than 1-threshold lots for sale its not full
			if (occupied >= threshold) {
				//				System.out.println("Is full");
				return true;
			}
			else {
				//				System.out.println("Is not full");
				return false;
			}
		}
		else {

			return false;
		}
	}

	// this determines the upper left and lower right corners of our
	//  subdivision
	private void determineBounds() {

		// the current cell
		Cell currentCell;

		// get the first cell so we can use that to set the values
		currentCell = (Cell)cells.get(0);
		
		// set the initial values
		minX = maxX = currentCell.getX();
		minY = maxY = currentCell.getY();

		// loop through all the cells
		for (int i=1; i<cells.size(); i++) {		

			// get the current cell
			currentCell = (Cell)cells.get(i); 

			// store the x, y coords
			int currentX = currentCell.getX();
			int currentY = currentCell.getY();

			// now check to see if its outside our current bounding box
			if (currentX < minX) {
				minX = currentX;
			}
			if (currentY < minY) {
				minY = currentY;
			}
			if (currentX > maxX) {
				maxX = currentX;
			}
			if (currentY > maxY) {
				maxY = currentY;
			}
		}

		//		System.out.println("MaxX, MaxY is " + maxX + "," + maxY);
		//System.out.println("MinX, MinY is " + minX + "," + minY);

	}

	//create a lot
	private void createLot(ArrayList cil) {
		// create the lot
		Lot newLot = new Lot(cil, this);

		// add new lot to this lot list and set it for sale
		lots.add(newLot);
		lotsForSale.add(newLot);

		// set the cells to point to this lot
		for (int i=0; i<cil.size(); i++) {
			
			// get the currentCell
			Cell currentCell = (Cell)cil.get(i);

			currentCell.setLot(newLot);

			// set the lot to point to the township that owns the cell
			newLot.setTownship(currentCell.getTownship());

			// if the Subdivision is country or hort clear the lot of forest
			if ( (type == COUNTRY) || (type == HORT) ) {

				//				double value = forestSpace.getValueAt(currentCell.getX(), currentCell.getY());
				// System.out.println(" Value is " + value);

				// clear the forest
				clearForest(currentCell);
				
				// check to make sure its cleared
				if (forestSpace.getValueAt(currentCell.getX(), 
										 currentCell.getY()) > 0 ) {

					System.out.println("Forest not cleared when it should be!");
					System.exit(-1);
						
				}

				//				value = forestSpace.getValueAt(currentCell.getX(), currentCell.getY());
				// System.out.println(" New Value is " + value);
			}
		}
	}

	// this method clears the forest from a cell
	private void clearForest(Cell c) {

		// get the coordinates of the cell
		int x = c.getX();
		int y = c.getY();

		// set the value in the forest space to 0
		forestSpace.putValueAt(x,y,0);
	}

	// this returns the lot from a cell if that lot is in this subdivision
	private Lot getValidLotFromCell (Cell c) {
		
		Lot returnLot = null;

		// get the lot
		returnLot = c.getLot();

		if (returnLot != null) {
			if (!(lots.contains(returnLot))) {
				returnLot = null;
			}
		}

		return returnLot;
		
	}

	// this takes a list of cells and finds an adjacent lot
	private Lot findAdjacentLot(ArrayList cil) {

		// a flag to indicate we found a lot
		int found = 0;
		Lot foundLot = null;

		// loop through all the cells
		for (int i=0; i<cil.size() && found == 0; i++) {
			
			// get the current cell, and its coords
			Cell currentCell = (Cell)cil.get(i);
			int x = currentCell.getX();
			int y = currentCell.getY();
			
			// examine its neighbors to see if there is a lot
			// check the right
			if ((x+1) < world.getSizeX()) {
				Cell examineCell = (Cell)world.getObjectAt(x+1, y);

				// see if there is a valid lot
				if (getValidLotFromCell(examineCell) != null) {
					foundLot = getValidLotFromCell(examineCell);
					found = 1;
				}
			}

			// check below
			if ((y+1) < world.getSizeY()) {
				Cell examineCell = (Cell)world.getObjectAt(x, y+1);

				// see if there is a valid lot
				if (getValidLotFromCell(examineCell) != null) {
					foundLot = getValidLotFromCell(examineCell);
					found = 1;
				}
			}

			// check the left
			if ((x-1) > 0) {
				Cell examineCell = (Cell)world.getObjectAt(x-1, y);

				// see if there is a valid lot
				if (getValidLotFromCell(examineCell) != null) {
					foundLot = getValidLotFromCell(examineCell);
					found = 1;
				}
			}
			
			// check above
			if ((y-1) > 0) {
				Cell examineCell = (Cell)world.getObjectAt(x, y-1);

				// see if there is a valid lot
				if (getValidLotFromCell(examineCell) != null) {
					foundLot = getValidLotFromCell(examineCell);
					found = 1;
				}
			}
		}
		
		return foundLot;
	}

	// this adds cells to a lot
	private void addToLot(Lot l, ArrayList cil) {
		
		// add the new cells to the lot
		l.addToCells(cil);

		// set the cells to point to this lot
		for (int i=0; i<cil.size(); i++) {
			
			// get the currentCell
			Cell currentCell = (Cell)cil.get(i);

			currentCell.setLot(l);
		}
	}

	// This procedure takes the cell list and breaks it up into lots
	public void divideIntoLots() {
		
		// find the boundaries of our subdivision
		determineBounds();
		
		// check to see if we already have lots if so delete them
		if (lots != null || lotsForSale != null) {
			lots = null;
			lotsForSale = null;
		}
		
		
		// create the lists
		lots = new ArrayList();
		lotsForSale = new ArrayList();
		ArrayList assignedCells = new ArrayList();
		
		// Now we loop through the world trying to create lots of at 
		//   least minLotSize
		for (int x=minX; x <= maxX; x++) {
			for (int y=minY; y <= maxY; y++) {
				
				// get the current cell
				Cell initialCell = (Cell)world.getObjectAt(x, y);

				// First check to see if we can start a lot with this
				//   cell, make sure its in the sub and that its not 
				//   already assigned
				if ((cells.contains(initialCell)) && 
					(!assignedCells.contains(initialCell))) {
					
					// create an arraylist to keep track of these cells
					ArrayList cellsInLot = new ArrayList();

					// reset the counter of cells in this lot
					double nCellsInLot = 0;
				
					// add this cell to the lot
					cellsInLot.add(initialCell);
					assignedCells.add(initialCell);					

					// increment the nCellsInLot counter
					nCellsInLot += 1.0 - initialCell.getWater();
					
					// set up local x, y trackers
					int currentX = x;
					int currentY = y;
					int lastX = x;
					int lastY = y;
					
					// set up a flag to indicate there are no more cells
					//   to look at
					int noMore = 4;

					while ((nCellsInLot < minLotSize)  && (noMore > 0)) {
						
						// look right
						if (noMore == 4) {
							currentX=lastX+1;
							currentY=lastY;
						}
						
						// look down
						if (noMore == 3) {
							currentX = lastX;
							currentY = lastY+1;
						}
						
						// look left
						if (noMore == 2) {
							currentX = lastX-1;
							currentY = lastY;
						}
						
						// look up
						if (noMore == 1) {
							currentX = lastX;
							currentY = lastY-1;
						}
						
						// make sure the current x, y coords are inside the world
						if (currentX < 0 || currentX > world.getSizeX() ||
							currentY < 0 || currentY > world.getSizeY() ) {
							noMore--;
						}				
						else {
							// get the current cell
							Cell currentCell = (Cell)world.getObjectAt(currentX, currentY);
							
							// check to see if its in the subdivision and not already assigned

							if ((cells.contains(currentCell)) && 
								!(assignedCells.contains(currentCell))) {
								
								// add it to the lot and to the assigned cells
								//   list
								cellsInLot.add(currentCell);
								assignedCells.add(currentCell);
								
								// increment the nCellsInLot counter
								nCellsInLot += 1.0 - currentCell.getWater();

								// update the last variables
								lastX = currentX;
								lastY = currentY;
								
								// reset the noMore flag
								noMore = 4;
								
							}
							else {
								noMore--;
							}
						}
					}
					
					// if we have enough for a lot create it
					if (nCellsInLot >= minLotSize) {
						createLot(cellsInLot);
					}

					// if we have cells but can't assign them to a lot, find another
					//   lot to assign them to
					else {
						if (cellsInLot.size() > 0) {
							// check whether it has found an adjacent lot or not
							// if it has, then add cells to adjacent lot
							if (findAdjacentLot(cellsInLot) !=null) {
								addToLot(findAdjacentLot(cellsInLot), cellsInLot);
							}
							// if there is no adjacent lot, 
							// leave the cells unbuilt
							// remove cells from list
							else {
								for ( int n = 0; n < cellsInLot.size(); n++ ) {
									cellsInLot.remove(n);
								}
								cellsInLot = null;
							}
						}
					}
				}
			}
		}   
		
	}


	// This method returns all the lots the subdivision has for sale
	public ArrayList getLotsForSale() {
		return lotsForSale;
	}

	// This method transfer the lot from the subdivision to the resident
	public void sellLot(Lot l, Resident r) {

		// remove the lot from the forSale list
		lotsForSale.remove(l);

		// set the owner of the lot to the resident
		l.setOwnedBy(r);

		// tell the resident that it owns the lot
		r.setLot(l);

		// tell the resident what township they are in
		r.setTownship(l.getTownship());
		
		// tell the township the resident moved in
		Township t = l.getTownship();
		t.addResident(r);
	}

	// This method sets a lot for sale
	public void forSale(Lot l) {
		lotsForSale.add(l);
	}

	// find out what township the sub is in
	public Township getTownship() {

		Township currentTownship = null;

		if (cells.size()>0) {
			// get the first cell in the list of cells
			Cell firstCell = (Cell)cells.get(0);
		
			// get their township
			currentTownship = firstCell.getTownship();
		}

		return currentTownship;
	}
}
