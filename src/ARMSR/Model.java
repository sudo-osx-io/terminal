/*$$
 * This is the model that controls the ARMS-R
 *$$*/
package ARMSR;

import java.util.Vector;
import java.util.ArrayList;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.lang.Math.*;
import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.FileDialog;
import javax.swing.JFrame;

import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.text.DateFormat;
import java.sql.Time;

import java.lang.reflect.*;
import java.util.TreeMap;  // for parameter parsing

import uchicago.src.sim.util.Random;
import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.*;
import uchicago.src.sim.space.*;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.util.ProbeUtilities;
import uchicago.src.sim.event.SliderListener;
import uchicago.src.sim.analysis.Plot;

import com.braju.beta.format.*; // for printf
import com.braju.beta.lang.*;
import rlriolo.ioutils.*;

//for the xml parsing of input files
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

// import cern.jet.random.Uniform;
/**
 */

public class Model extends SimModelImpl {

	//  SECTION AAA from Dronification Template
	// for Format.printf and friends
	protected Parameters ps = new Parameters(this).mark();
	protected Parameters p = new Parameters();

	// Generic parameters
	protected String		initialParametersFileName = "";
	// 	private String			initialAgentsFileName = "";
	private String			reportFileName = "report";
	private String			outputDirName =  "./";
	private int				reportFrequency = 999999;
	private int				runNumber = 0;
	private int				stopT = 100;
	private int				rDebug = 0;
	private int				saveRunEndState = 0;
	private long    		seed = 1234567;
	private PrintWriter		reportFile, plaintextReportFile;
	private PrintWriter		changesFile;

	// other utilities
	protected String[] commandLineArgs;
	protected String modelType = "Model";

	//for input file 
	private boolean 	STRICT_FILE_FORMAT = true;
	private Vector 		changesVector;
	private Vector 		agentChangesVector;
	// variables for processing run-time changes that are
	// read in from the input file
	protected int 		numberOfChanges = 0;
	protected int 		nextChangeToDo = 0;
	protected int[] 	changeSteps = new int[64];
	protected int[] 	changeIDs = new int[64];
  	protected ArrayList changeSpecs = new ArrayList(16);
	// END SECTION AAA from Dronification Template

	// set the number of townships this is hardcoded right now to 2
	private int numTownships = 2;

	// declare zoned min lot size, hardcoded for two townships
	private int zonedMinLotSize0;
	private int zonedMinLotSize1;

	// declare demographic characteristics of residents
	private double demogrKids;
	private double demogrIncome;
	private double demogrEnv;

	private int worldXSize;
	private int worldYSize;

	private int farmWidth;
	private int farmHeight;

	// The probability that a farm will be put up for sale at any time step
	private double pFarmSale;

	// The probability that a farm will put a rural lot for sale at any time step
	private double pRuralLotSale;

	private int resPerTurn;

	private int pauseVal;
	protected Schedule schedule;

	// all of the various agent lists
	protected ArrayList cellList;

	private ArrayList farmList;

	private ArrayList ruralLotList;

	private ArrayList subList;

	private ArrayList residentList;

	private ArrayList townshipList;


	// developers
	private Developer countryDeveloper;
	private Developer hortDeveloper;
	private Developer remnantDeveloper;

	// spaces
	protected Object2DGrid world;
	
	//Quality Space details
	private QualitySpace roadSpace;
	private QualitySpace soilSpace;
	private QualitySpace forestSpace;
	private QualitySpace waterSpace;
	private QualitySpace elevationSpace;

	//Map files directory
	private String mapDir;

	//Quality Space files
	public static final String roadMap = "road.txt";
	public static final String soilMap = "soil.txt";
	public static final String forestMap = "forest.txt";
	public static final String waterMap = "water.txt";
	public static final String elevationMap = "elevation.txt";

	// SECTION BBB from dronification template
	// this implements the mapping from aliases to long names.
	private   TreeMap parametersMap;
	public void setupParametersMap () {

		DMSG( 1, "setupParametersMap()" );

		parametersMap = null;
		parametersMap = new TreeMap();
		// generic model parameters
		parametersMap.put( "D", "rDebug" );
		parametersMap.put( "S", "seed" );
		parametersMap.put( "iPFN", "initialParametersFileName" );
		//		parametersMap.put( "iAFN", "initialAgentsFileName" );
		parametersMap.put( "rFN", "reportFileName" );
		parametersMap.put( "T", "stopT" );
		parametersMap.put( "sRES", "saveRunEndState" );
		parametersMap.put( "oDN", "outputDirName" );
		parametersMap.put( "rF", "reportFrequency" );
		parametersMap.put( "rN", "runNumber" );
		// model specific parameters
		parametersMap.put( "X", "worldXSize" );
		parametersMap.put( "Y", "worldYSize" );
		parametersMap.put( "mD", "mapDir" );
		parametersMap.put( "pFS", "pFarmSale" );
		parametersMap.put( "pRLS", "pRuralLotSale" );
		parametersMap.put( "rPT", "resPerTurn" );
		parametersMap.put( "zMLS0", "zonedMinLotSize0" );
		parametersMap.put( "zMLS1", "zonedMinLotSize1" );
		parametersMap.put( "dKids", "demogrKids" );
		parametersMap.put( "dInc", "demogrIncome" );
		parametersMap.put( "dEnv", "demogrEnv" );
	}

	// These are parameters to appear in the gui -- these can be in any order
	//		"initialAgentsFileName,
	private String[] guiParameterNames = { 
		"rDebug","seed","initialParametersFileName", "reportFileName", "stopT",
		"saveRunEndState", 
		// add model specific parameters here:
		"worldXSize", "worldYSize","mapDir", "pFarmSale", "pRuralLotSale", "resPerTurn",
		"zonedMinLotSize0", "zonedMinLotSize1", "demogrKids", "demogrIncome", "demogrEnv"  
	};

	// END SECTION BBB from dronification template


	public Model() {
		
	}

	// this will set up the QualitySpaces
	private void createQualitySpaces() {

		// create the quality spaces
		try {

			// try to read from a file
			String fname = mapDir + roadMap;
			roadSpace = new QualitySpace(fname);

			/*
			// scale the file by dividing all the values by 8191
			roadSpace.scale();
			*/
		}
		catch(IOException e) {

			// couldn't read the file just initialize
			System.out.println("Could not read road.txt, initializing by default!");
			roadSpace = new QualitySpace(worldXSize, worldYSize);

			// this a binary w/ probability of a 1 = 0.5
			roadSpace.initializeBinary(0.5);
		}

		/*
		//check to see what values there are
		//from road.txt
		System.out.println("Road value at 0,0 is " + roadSpace.getValueAt(0,0));
		System.out.println("Road value at 10,10 is " + roadSpace.getValueAt(10,10));
		System.out.println("Road value at 39,39 is " + roadSpace.getValueAt(39,39));
		System.out.println("Road value at 50,50 is " + roadSpace.getValueAt(50,50));
		System.out.println("Road value at 79,79 is " + roadSpace.getValueAt(79,79));
		*/

		// forest space
		try {
			// try to read from a file
			String fname = mapDir + forestMap;
			forestSpace = new QualitySpace(fname);

			/*
			// scale the file by dividing all the values by 8191
			forestSpace.scale();
			*/
		}
		catch(IOException e) {
			// couldn't read the file just initialize
			System.out.println("Could not read forest.txt, initializing by default!");
			forestSpace = new QualitySpace(worldXSize, worldYSize);
			forestSpace.initializeNormalMean(0.5, 0.25, 0, 1);
		}


		/*		//check to see what values there are
		//from forest.txt
		System.out.println("Forest value at 0,0 is " + forestSpace.getValueAt(0,0));
		System.out.println("Forest value at 19,19 is " + forestSpace.getValueAt(19,19));
		System.out.println("Forest value at 39,39 is " + forestSpace.getValueAt(39,39));
		System.out.println("Forest value at 59,59 is " + forestSpace.getValueAt(59,59));
		System.out.println("Forest value at 79,79 is " + forestSpace.getValueAt(79,79));
		*/
		// water space
		try {
			// try to read from a file
			String fname = mapDir + waterMap;
			waterSpace = new QualitySpace(fname);

			/*
			// scale the file by dividing all the values by 8191
			waterSpace.scale();
			*/
		}
		catch(IOException e) {
			// couldn't read the file just initialize
			System.out.println("Could not read water.txt, initializing by default!");
			waterSpace = new QualitySpace(worldXSize, worldYSize);
			waterSpace.initializeNormalMean(0.1, 0.25, 0, 1);
		}
		
		/*
		//check to see what values there are
		//from water.txt
		System.out.println("Water value at 0,0 is " + waterSpace.getValueAt(0,0));
		System.out.println("Water value at 19,19 is " + waterSpace.getValueAt(19,19));
		System.out.println("Water value at 39,39 is " + waterSpace.getValueAt(39,39));
		System.out.println("Water value at 59,59 is " + waterSpace.getValueAt(59,59));
		System.out.println("Water value at 79,79 is " + waterSpace.getValueAt(79,79));
		*/
		
		// elvation space
		try {
			// try to read from a file
			String fname = mapDir + elevationMap;
			elevationSpace = new QualitySpace(fname);

			/*
			// scale the file by dividing all the values by 8191
			elevationSpace.scale();
			*/

		}
		catch(IOException e) {
			// couldn't read the file just initialize
			System.out.println("Could not read elevation.txt, initializing by default!");
			elevationSpace = new QualitySpace(worldXSize, worldYSize);
			elevationSpace.initializeNormalMean(0.5, 0.02, 0, 1);
		}

		/*
		//check to see what values there are
		//from elevation.txt, there should be 1 in top and bottom rows
		System.out.println("Elevation value at 0,0 is " + elevationSpace.getValueAt(0,0));
		System.out.println("Elevation value at 19,19 is " + elevationSpace.getValueAt(19,19));
		System.out.println("Elevation value at 39,39 is " + elevationSpace.getValueAt(39,39));
		System.out.println("Elevation value at 59,59 is " + elevationSpace.getValueAt(59,59));
		System.out.println("Elevation value at 79,79 is " + elevationSpace.getValueAt(79,79));
		*/

		// soil space
		try {
			// try to read from a file
			String fname = mapDir + soilMap;
			soilSpace = new QualitySpace(fname);

			// scale the file by dividing all the values by 8191
			soilSpace.scale();
		}
		catch(IOException e) {
			// couldn't read the file just initialize
			System.out.println("Could not read soil.txt, initializing by default!");
			soilSpace = new QualitySpace(worldXSize, worldYSize);
			soilSpace.initializeNormalMean(0.5, 0.25, 0, 1);
		}

		/*
		//check to see what values there are
		//from soil.txt, there should be 1 in top and bottom rows
		System.out.println("Soil value at 0,0 is " + soilSpace.getValueAt(0,0));
		System.out.println("Soil value at 19,19 is " + soilSpace.getValueAt(19,19));
		System.out.println("Soil value at 39,39 is " + soilSpace.getValueAt(39,39));
		System.out.println("Soil value at 59,59 is " + soilSpace.getValueAt(59,59));
		System.out.println("Soil value at 79,79 is " + soilSpace.getValueAt(79,79));
		*/

		// tell the subdivision class where these are
		Subdivision.setQualitySpaces(roadSpace, soilSpace, forestSpace, waterSpace, elevationSpace);

		// tell the cell class where these are, eventually eliminate the
		//    subdivision knowing this
		Cell.setQualitySpaces(roadSpace, soilSpace, forestSpace, waterSpace, elevationSpace);

		// try to test here if quality spaces have a value in x,y
		//		System.out.println("Getting " + soilSpace.getValueAt(39,39) + " at " + 39 + ", " + 39);
		//System.out.println("Getting " + waterSpace.getValueAt(39,39) + " at " + 39 + ", " + 39);

	}

	// this method creates the townships, it might seem weird to do this first
	//   but since townships are an aggregate class containing cells we need
	//   them before we can create cells
	private void createTownships() {
		
		// create the townships, each township identical right now
		for (int i=0; i<numTownships; i++) {
			Township t = new Township();
			// set the zoned minimum lot size, depending on which township it is
			// this is hardcoded, needs to be more generic
			if ( i == 0 ) {
				t.setZonedMinLotSize(zonedMinLotSize0);
			}
			if ( i == 1) {
				t.setZonedMinLotSize(zonedMinLotSize1);
			}
			townshipList.add(t);
		}

		//test to make sure that all townships are there and that zoning is right
		//first get the size of township list
		//use that value to loop through list, get the zoning assigned and print
		/*
		int ts = (int) townshipList.size();
		for (int i=0; i<ts; ++i) {
			//get the township and zoning
			Township t = (Township)townshipList.get(i);
			int z = (int) t.getZonedMinLotSize();
			System.out.println("Township is " + i + ", zoning is " + z);
		}
*/
		// assign the subList to the Township class
		Township.setSubList(subList);
		
	}

	// this method figures out what township a cell should be in and
	//   adds it to the appropriate one
	private void addCellToTownship(Cell c) {

		// right now I'm going to hardcode in the township boundaries at
		//   equal spacings across the x direction
		//  have to subtract 1 because x values are 0 ordered so this makes 
		//    the size 0-ordered as well -??? I'm not sure this is correct (mzellner).
		int townshipWidth = ( world.getSizeX() - 1 ) / numTownships;

		// the township it belongs to is the integer part of
		//   its x value divided by the township width
		int townshipNumber = (int)(c.getX() / (townshipWidth + 1));

		//		System.out.println("Adding " + c.getX() + " to township " + townshipNumber);

		// get the township
		Township t = (Township)townshipList.get(townshipNumber);

		// add the cell to the township
		t.addCell(c);

		// tell the cell what township its in
		c.setTownship(t);

		// ask the township whether the last cell in its list is this one
		ArrayList cells1 = t.getCells();
		if (!(cells1.contains(c))) {
			System.out.println("Current cell was not added!");
			System.exit (-1);
		}
		
		// ask cell if its township is assigned correctly
		Township t1 = c.getTownship();
		if (t1 != t) {
			System.out.println("Current cell has incorrect township!");
			System.exit (-1);
		}

	}

	// this method fills the world with cells, and then assigns them to a
	//   township
	private void createCells() {
		for (int x = 0; x < world.getSizeX(); x++) {
			for (int y = 0; y < world.getSizeY(); y++) {
				Cell cell = new Cell(world, x, y);
				world.putObjectAt(x,y,cell);
				cellList.add(cell);

				// also add the cell to a township
				addCellToTownship(cell);
			}
		}
	}
	
	private void assignCellsToFarms() {
		// we need to split up the world into different farms
		//  for now all of the farms
		//  are of uniform size except for the ones on the ends
		for (int y = 0; y < world.getSizeY(); y++) {
			for (int x = 0; x < world.getSizeX(); x++) {

				int currDistX = 0;
				double currFarmFitX = 0.0;
				int currDistY = 0;
				double currFarmFitY = 0.0;
				int currFarmWidth = 0;
				int currFarmHeight = 0;

				Cell current = (Cell)world.getObjectAt(x,y);

				// determine if the distance between edge of world and current x,y
				// allows for two or more whole farms
				// if it doesn't, then make one whole farm of that width/height

				currDistX = world.getSizeX() - x;
				currFarmFitX = (double) currDistX/farmWidth;

				if (currFarmFitX >= 2.0) {
					currFarmWidth = farmWidth;
				}
				else {
					currFarmWidth = currDistX;
				}

				currDistY = world.getSizeY() - y;
				currFarmFitY = (double) currDistY/farmHeight;

				if (currFarmFitY >= 2.0) {
					currFarmHeight = farmHeight;
				}
				else {
					currFarmHeight = currDistY;
				}

				// if the Cell belongs to a farm, or if it has 0 width or height, skip it
				if (current.getOwnedBy() != null || currFarmWidth == 0 || currFarmHeight == 0) {
					x++;
				}
				else {
					// make sure all the cells are a member of the farm
					//  and that the farm contains all the cells
					ArrayList listOfFarmCells = new ArrayList();
					for(int indFarmX = 0; indFarmX < currFarmWidth; indFarmX++) {
						for(int indFarmY = 0; indFarmY < currFarmHeight; indFarmY++) {
							current = (Cell)world.getObjectAt(x+indFarmX, y+indFarmY);
							if (current.getOwnedBy() == null) {
								listOfFarmCells.add(current);
							}
							else {
								System.out.println("Tried to assign a cell to two different farms!");
							}
						}
					}
					
					// create a new farm to assign all of these farm cells to
					Farm newFarm = new Farm(listOfFarmCells);

					// go through the list of all farm cells and make sure 
					//  they know who they are owned by
					for (int i=0; i< listOfFarmCells.size(); i++) {
						Cell currCell = (Cell)listOfFarmCells.get(i);
						
						if (currCell == null) {
							System.out.println("currCell is null!");
							System.exit(-1);
						}

						if (newFarm == null) {
							System.out.println("newFarm is null!");
							System.exit(-1);
						}

						currCell.setOwnedBy(newFarm);
					}

					// add the farm to the list of farms
					farmList.add(newFarm);

					// skip over the rest that are owned by this farm
					x += currFarmWidth - 1;
				}
			}
		}
	}

	////////////////////////////////////////////////////////////////
	//	assignDemogResident()
	// This method is called from buildModel to tell Resident class 
	// 	to assign demographic characteristics to residents, according
	// to distribution parameter
	private void assignDemogResident() {
		Resident.setDemographics(demogrKids, demogrIncome, demogrEnv);
	}



	////////////////////////////////////////////////////////////////
	// sellFarms
	// This goes through the list of farms and determines if they
	//   should be for sale, if so it adds them to the farmsForSale list and
	//   sets there forSale flag to true
	private void sellFarms() {
		int farmsSold=0; // Keep track of number of farms put up for sale

		// loop through all of the farms
		for (int i=0; i < farmList.size() ; i++) {
			// generate a random double
			double p = getUniformDoubleFromTo(0.0,1.0);

			// see if this is less than the probability of selling the farm
			if (p < pFarmSale) {

				//	System.out.println("p is " + p + " pFarmSale is " + pFarmSale);

				// get the current farm
				Farm f = (Farm)farmList.get(i);

				// set it for sale
				f.setForSale(true);

				//increment the sale counter
				farmsSold++;
			}
		}
		

		//		System.out.println("New farms for sale: " + farmsSold);
	}
	
	// developFarms
	// this method develops farms into subdivisions
	private void developFarms() { 

		// For debugging sub type distribution
		/*		ArrayList subs = remnantDeveloper.getSubs();
		if (subs!=null) {
			System.out.println(subs.size() + " Remnants");
		}
		subs = hortDeveloper.getSubs();
		if (subs!=null) {
			System.out.println(subs.size() + " Horts");
		}
		subs = countryDeveloper.getSubs();
		if (subs!=null) {
			System.out.println(subs.size() + " Country");
			}*/
		// Use in debugging
		//		System.out.println("*********************developing farms");
		// first make a copy of the farm list so that we can iterate over one
		//   copy while manipulating the main list
		ArrayList copyOfFarmList = (ArrayList)farmList.clone();
		
		// only do this if there is a farm list
		if (copyOfFarmList.size() > 0) {

		//Check to make sure it does shuffle the list
		int numFarms = (int)copyOfFarmList.size();
		Farm firstFarm1 = (Farm)copyOfFarmList.get(0);
		Farm lastFarm1 = (Farm)copyOfFarmList.get(numFarms-1);

		// shuffle the list
		java.util.Collections.shuffle(copyOfFarmList);

		//Now compare after shuffling and print out warning
			Farm firstFarm2 = (Farm)copyOfFarmList.get(0);
			Farm lastFarm2 = (Farm)copyOfFarmList.get(numFarms-1);
			if ( firstFarm2 == firstFarm1 && lastFarm2 == lastFarm1) {
				System.out.println("CAUTION: may not have shuffled list of farms");
			}
		}

		// loop through all of the farms
		for (int i=0; i < copyOfFarmList.size() ; i++) {

			// get the current farm
			Farm f = (Farm)copyOfFarmList.get(i);

			// check to see if the farm is for sale
			if (f.getForSale()) {

				// check that owner is farmer for all cells
				ArrayList listOfFarmCells = new ArrayList ();
				listOfFarmCells = f.getCells();
				
				//go down the list of cells and get owner
				for (int n=0; n < listOfFarmCells.size() ; n ++) {
					Cell c = (Cell)listOfFarmCells.get(n);
					Owner o = c.getOwnedBy();
				
					//check that o matches f
					//if not, exit the program
					if (o != f) {
						System.out.println("cell owner is not f!");
						System.exit(-1);
					}
				}

				// create the new subdivision
				Subdivision sub = new Subdivision(f.getCells(), world);

				// determine and set its type
				int subType = sub.determineSubType();

				// get the appropriate developer and make sure
				//  they can develop
				Developer dev = null;

				if (subType == Subdivision.COUNTRY) {
					dev = countryDeveloper;
					//					System.out.println("Country");
					sub.setType(subType);
				}
				else if (subType == Subdivision.REMNANT) {
					dev = remnantDeveloper;
					//					System.out.println("Remnant");
					sub.setType(subType);
				}
				else if (subType == Subdivision.HORT) {
					dev = hortDeveloper;
					//					System.out.println("Hort");
					sub.setType(subType);
				}

				// but this never gets returned right now
				// this splits the subdivision
				else if (subType == Subdivision.SPLIT) {
					// this should never be printed out right now
					System.out.println("Split");
				}
				
				// skip the farm and don't develop it
				else if (subType == Subdivision.DONTDEVELOP) {
					
					// set the developer to null
					dev = null;
					//	System.out.println("Don't Develop");
				}

				/*
				   //Check here that development type is assigned appropriately,
				   //by comparing to messages above
				   int devType = dev.getType();
				   System.out.println("Type is " + devType);
				*/

				// first find out what township the sub would be in
				Township currentTownship = sub.getTownship();

				// can they develop
				// check to make sure there is a developer, and they can 
				//   develop and the development type isn't dontdevelop
				// also need to check to make sure minLotSize for the sub
				//   excedes the minlotsize for the township
				if (dev!=null && 
					dev.canDevelop() && 
					sub.getMinLotSize() >=
					currentTownship.getZonedMinLotSize() &&
					subType != Subdivision.DONTDEVELOP) {

					// add another check that township zoning for min lot size 
					//is respected
					if (currentTownship.getZonedMinLotSize()<= sub.getMinLotSize()) {


						// if so add the sub to the developer and set its type
						dev.addSub(sub);
						
						//Check that the sub was actually added to the developer's list
						ArrayList checkSubList = dev.getSubs();
						if (!(checkSubList.contains(sub))) {
							System.out.println("Sub was not added to developer's list!");
							System.exit(-1);
						}
						
						// get a list of the cells
						ArrayList subCells = sub.getCells();
						
						// remove the farm from the original list and delete it
						farmList.remove(f);
						f = null;
						
						// Don't forget to set the owned by for the new cells
						//   and place the object in the world
						for (int j=0 ; j < subCells.size() ; j++) {				
							// set the owned by
							Cell newSubCell = (Cell)subCells.get(j);
							newSubCell.setOwnedBy(sub);
						}
						
						// check that owner is subDivision for all cells
						//go down the list of cells and get owner
						for (int d=0; d < subCells.size() ; d ++) {
							Cell c = (Cell)subCells.get(d);
							Owner o = c.getOwnedBy();
							
							//check that o matches sub
							//if not, exit the program
							if (o != sub) {
								System.out.println("cell owner is not sub!");
								System.exit(-1);
							}
						}

						sub.divideIntoLots();
						
						// add the subdivision to the subdivision list
						subList.add(sub);
					}
					
				}
				
				// if the developer can not develop throw away the subdivision
				//   and try next year
				else {

					// reset the subdivsion pointer
					sub = null;

				}
			}
		}
	}

	// this method creates residents and sells them lots
	//  may need to separate these two functions in the future.
	private void sellLots() {
		//Aggregate all lots for sale
		ArrayList globalLotsForSale = new ArrayList();

		// go through all subdivisions getting their lots for sale
		for (int i=0; i < subList.size(); i++) {

			// look at each subdivision in turn
			Subdivision currentSub = (Subdivision)subList.get(i);

			// add all of the lots of the subdivision to the global list
			globalLotsForSale.addAll(currentSub.getLotsForSale());
		}

		//first create the number of residents determined by resPerTurn
		for (int i=0; i < resPerTurn ; i++) {
			Resident r = new Resident();
			
			// generate a list of candidate lots for this resident
			// set numTests to its default or the size of the lotsForSaleList
			int numTests = java.lang.Math.min(10, globalLotsForSale.size());
			ArrayList testLots = new ArrayList();

			for (int j=0; j < numTests; j++) {
				Lot currentLot;
				int trials = 0;
				
				// make sure we don't already have it
				int randomInd = 0;
				do {
					// generate a random number from 0 to the length of the 
					//    lotsForSale list, don't forget size is 1-orderd
					randomInd = getUniformIntFromTo(0, globalLotsForSale.size()-1);
					
					// get the actual lot
					currentLot = (Lot)globalLotsForSale.get(randomInd);

					// increment the number of times we've tried to find a 
					//   new lot
					trials++;


				// check the lot to make sure its of a type the resident
				//   can afford, and that we don't already have it and
				//   that we haven't tried too many times to add a lot
				} while ( ( ( testLots.contains(currentLot) ) ||
							( r.evaluateSub(currentLot.getSub()) <= 0 ) ) &&
						  ( trials < 1000 ) );

				// add the random cell to the testLots if we haven't exceeded
				//  the number of trials
				if (trials < 1000) {
					testLots.add(currentLot);
				}
			}

			// if the size of testLots is 0 then there is nothing the 
			//   resident can afford, thus the resident simply goes away
			if (testLots.size() > 0) {

				//evaluate lots, this calls the resident's evaluate lot method
				//   the resident then places a value on the lot from 0 to 1
				//   assume all values > 0
				double maxValue = -1;
				Lot maxLot = null;
				for (int j=0; j<testLots.size(); j++) {
					
					// evaluate the lot
					double value = r.evaluateLot((Lot)testLots.get(j));
					//System.out.println("lot value is " + value );

					// no lot should return a value of 0 NOT TRUE ANYMORE
					//					if (value <= 0 ) {
					//	System.out.println("Lot returned a value of 0!");
					//	System.out.println("Lot value is " + value);
					//	System.exit(-1);
					//}
					
					// if current lot has greater value than max value then set maxValue to
					//   that lot and maxLot to that object
					if (value > maxValue) {
						maxValue = value;
						maxLot = (Lot)testLots.get(j);
					}
				}
				//	System.out.println("maxValue is " + maxValue );
				

				// now sell the lot, this takes care of the subdivision but we still need to
				//   change our local list
				if (maxLot!=null && r!=null) {
					//transfer lot to resident
					//  but sellLot is handled by a subdivision so we get the subdivision which
					//   owns the lot first and then sell it
					Subdivision s = maxLot.getSub();
					
					s.sellLot(maxLot, r);
					
					// check that the lot sold is not one that the resident wouldn't buy
					int type = s.getType();
					int env = r.getEnvGroup();
					int inc = r.getIncome();
					
					if (type == Subdivision.COUNTRY) {
						if (env == Resident.ENVGROUP_YES && inc == Resident.INCOME_HIGH){
							System.out.println("Tried to buy a lot type they wouldn't buy!");
							System.out.println("Sub type "+ type);
							System.out.println("Env "+ env);
							System.out.println("Inc "+ inc);
							System.out.println("maxValue "+ maxValue);
							System.exit(-1);
						}
					}
					if (type == Subdivision.REMNANT || type == Subdivision.HORT) {
						if (inc == Resident.INCOME_LOW){
							System.out.println("Tried to buy a lot type they wouldn't buy!");
							System.out.println("Sub type "+ type);
							System.out.println("Inc "+ inc);
							System.exit(-1);
						}
					}
					
					// remove the lot from the global for sale list
					globalLotsForSale.remove(maxLot);
					
					//add the resident to the list of all residents
					residentList.add(r);
					
					//check to make sure that the lot's owner is r
					//if not, exit the program
					Owner o = maxLot.getOwnedBy();
					if (o != r) {
						System.out.println("Owner of lot is not r!");
						System.exit(-1);
					}
				}
				else {
					
					// we are here because either the maxLot doesn't exist or the
					//  owner doesn't exist
					// errors in case we try to sell things and they don't exist
					if (maxLot == null) {
						System.out.println("Tried to sell a lot that doesn't exist!");
						System.exit(-1);
					}
					if (r == null) {
						System.out.println("Tried to sell a lot to a resident that doesn't exist!");
						System.exit(-1);
					}
				}
			}
			else {

				// This prints out a message whenever there are no lots 
				//  for the residents to look at
				//				System.out.println("Resident could not afford any available lots or there were no available lots");
			}
		}
	}

	// This handles that some residents want to move out over time
	private void moveOut() {

		// create a copy of the resident list
		ArrayList copyOfResidentList = (ArrayList)residentList.clone();

		// this is the probability an individual resident will move out
		double pMoveOut = 0.000;

		// loop through all the residents and determine which ones
		//   want to move out
		for(int i=0; i < copyOfResidentList.size(); i++) {

			// generate a random probability
			double p = getUniformDoubleFromTo(0.0, 1.0);

			// if that probability is less than the move out prob
			//   then the resident moves out
			if ( p < pMoveOut ) {

				// get the current resident
				Resident currResident = (Resident)copyOfResidentList.get(i);

				// tell the resident to move out
				currResident.moveOut();

				// remove the resident from the lists
				residentList.remove(currResident);
				currResident = null;
			}			
		}

	}

	protected void buildModel() {
		// SECTION DDD
		startReportFile();
		if ( getSeed() == 1234567 || getSeed() == 0) {
			long s = System.currentTimeMillis();
			setSeed( s );
			Format.printf( "\nseed was 1234567 or 0, now ==> s=%d\n", p.add(s) );
		}
		Format.printf( "\nabout to setSeed(%d)\n", p.add( getSeed() ) );
   		Random.setSeed( getSeed() );
		Random.createUniform();
		Random.createNormal( 0.0, 1.0 );
		// END SECTION DDD

		world = new Object2DTorus(worldXSize, worldYSize);
		
		Format.printf( "\n---Creating Townships\n" );
		createTownships();
		Format.printf( "\n---Creating Quality Spaces\n" );
		createQualitySpaces();
		Format.printf( "\n---Creating Cells\n" );
		createCells();
		Format.printf( "\n---Assigning Cells to Farms\n" );
		assignCellsToFarms();
		Format.printf( "\n---Assigning demographic characteristics to Resident class\n" );
		assignDemogResident();

		// SECTION EEE
		// some post-load finishing touches
		applyAnyStoredChanges();
		//		applyAnyStoredAgentChanges();
		getReportFile().flush();
		getPlaintextReportFile().flush();
		// END SECTION EEE
	}
	
	public void checkDistribution() {
   	/*		// test for problems in resident distributions
		// assumes they are all equal

		// go through global list of residents
		//		int no_kids = 0;
		//		int yes_kids = 0;
		//		int low_income = 0;
		//		int high_income = 0;
		//		int env_no = 0;
		//		int env_yes = 0;

		//		for (int i=0; i<residentList.size(); i++) {
		//			Resident r = (Resident)residentList.get(i);

			// calculate the distribution of kids and aggregate in groups
		//			int k = r.getKids();
		//			if ( k < 0.5 ) {
		//				no_kids++;
		//			}
		//			else {
		//				yes_kids++;
		//			}

			// calculate the distribution of income and aggregate in groups
		//			int y = r.getIncome();
		//			if ( y < 0.5 ) {
		//				low_income++;
		//			}
		//			else {
		//				high_income++;
		//			}

			// calculate the distribution of env affiliation and aggregate in groups
		//			int e = r.getEnvGroup();
		//			if ( e < 0.5 ) {
		//				env_no++;
		//			}
		//			else {
		//				env_yes++;
		//			}

		//		}

		// expected value
		//		double expected = (env_no + env_yes)/2.0;
		//		int population = residentList.size ();
		//		double std_dev = java.lang.Math.sqrt( (double) (1.0/12.0)*population);

		// test if the difference between these two groups is more
		// than two std. dev.
		
		//		if ( ( yes_kids > expected + 3 * std_dev ) || ( yes_kids < expected - 3 * std_dev ) ) {
		//			System.out.println("yes_kids is " + yes_kids + " expected is " + expected + " std_dev is " + std_dev);
		//		}
		//		if ( ( low_income > expected + 3 * std_dev ) || ( low_income < expected - 3 * std_dev )) {
		//			System.out.println("low_income is " + low_income + " expected is " + expected + " std_dev is " + std_dev);
		//		}
		//		if ( ( env_yes > expected + 3 * std_dev ) || ( env_yes < expected - 3 * std_dev ) ) {
		//			System.out.println("env_yes is " + env_yes + " expected is " + expected + " std_dev is " + std_dev);
		//		}
*/

	}

	public void step() {
		
		// See if there are any farms for sale
		sellFarms();

		// develop farms into subdivisions
		developFarms();
		
		// sell lots within subdivisions
		sellLots();

		// have some residents move out
		moveOut();

		// checks to make sure the distributtion is okay
		checkDistribution();

	}

	// stepReport
	// each step write out:
	//   time  avgUnhappiness  avgSize
	// Note: put a header line in via the startReportFile (below in this file)
	public void stepReport () {
		// set up a string with the values to write
		String s = Format.sprintf( "%5.0d", p.add( getTickCount() ) );

		// go through each township
		for (int i=0; i<townshipList.size(); i++) {

			// get the current township
			Township t = (Township)townshipList.get(i);

			// calculate the income
			s += Format.sprintf("  %6.0d  ", 
								p.add( t.calculateIncome() ) );

			// calculate the avg forest
			s += Format.sprintf("  %6.3f  ", 
								p.add( t.calculateAvgForest() ) );

			// count the number of country subs
			s += Format.sprintf("  %6.0d  ", 
								p.add( t.countSubs( (int) Subdivision.COUNTRY) ) );

			// count the number of horticultural subs
			s += Format.sprintf("  %6.0d  ", 
								p.add( t.countSubs( (int) Subdivision.HORT) ) );
			// count the number of remnant subs
			s += Format.sprintf("  %6.0d  ", 
								p.add( t.countSubs( (int) Subdivision.REMNANT) ) );
		}

		// write it to the xml and plain text report files
		writeLineToReportFile ( "<stepreport>" + s + "</stepreport>" );
		writeLineToPlaintextReportFile( s );

		getReportFile().flush();
		getPlaintextReportFile().flush();
	}

	// SECTION GGG  ----------------------------------------------------------
	////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////
	// builds the schedule
	// the rest of it is built by GUIModel or BatchModel extensions of this class.
	//
	public void buildSchedule() {
		schedule = new Schedule();
		schedule.scheduleActionAtInterval( 1, this, "applyAnyStoredChanges", Schedule.LAST );
		//		schedule.scheduleActionAtInterval( 1, this, "applyAnyStoredAgentChanges", Schedule.LAST );
	}

	public void processEndOfRun ( ) {
		if ( rDebug > 0 )  
			Format.printf("\n\n===== processEndOfRun =====\n\n" );
		endReportFile();
		//		if ( saveRunEndState == 1 ) saveAgents();
		this.fireStopSim();
	}
	// END SECTION GGG  ----------------------------------------------------------

	public void begin() {
		buildModel();

		buildSchedule();
	}
	
	public void setup() {
		
		Format.printf("Model Setup() called...\n");

		// setup default values
		// agents
		// delete and create the agent list

		// cells
		// delete and create the cell list
		cellList = null;
		cellList = new ArrayList();
		
		// aggreagates
		// delete and create the farm list
		farmList = null;
		farmList = new ArrayList();

		subList = null;
		subList = new ArrayList();

		residentList = null;
		residentList = new ArrayList();

		townshipList = null;
		townshipList = new ArrayList();

		// set up the developers
		countryDeveloper = null;
		remnantDeveloper = null;
		hortDeveloper = null;

		countryDeveloper = new Developer(Subdivision.COUNTRY);
		remnantDeveloper = new Developer(Subdivision.REMNANT);
		hortDeveloper = new Developer(Subdivision.HORT);

		//set the min lot size zoned in each township
		zonedMinLotSize0 = 1;
		zonedMinLotSize1 = 1;

		// set the min and max farm sizes
		farmHeight = 13;
		farmWidth = 13;

		// set the probability of a farm sale
		pFarmSale = 0.1;

		// set the probability of a rural lot sale
		pRuralLotSale = 0.2;

		// set number of residents per turn
		resPerTurn = 10;

		//set the distribution of demographic characteristics
		demogrKids = 0.5;
		demogrIncome = 0.5;
		demogrEnv = 0.05;

		//set the map files directory

		mapDir = "/users/mzellner/Sluce/aag/policyModel/repastCode/sluce/ARMSR/";

		worldXSize = 304;
		worldYSize = 152;

		pauseVal = -1;
		
		world = null;

		schedule = null;
		System.gc();
		
		// SECTION CCC ---------------------------------------
		changesVector = new Vector();
		//		agentChangesVector = new Vector();
		setupParametersMap();

		// this might be kind of kludgy?
		// only process command line arguments if it is the first run
		// if it is the first run then schedule is null,
		// if not then schedule is initialized (and is set to null
		// on the next line)
		if( schedule == null ) 
			processCommandLinePars( commandLineArgs );
		// END SECTION CCC  ---------------------------------------

		schedule = new Schedule(1);

	}
	
	public String[] getInitParam() {
		String[] params = {"worldXSize", "worldYSize", "mapDir", "zonedMinLotSize0", "zonedMinLotSize1", "farmHeight", "farmWidth", "pFarmSale", "pRuralLotSale", "resPerTurn", "demogrKids", "demogrIncome", "demogrEnv", "Pause", "Model"};
		return params;
	}
	
	public String getName() {
		return "ARMSR";
	}
	
	public void setPause(int tick) {
		schedule.scheduleActionAt(tick, this, "pause", Schedule.LAST);
		pauseVal = tick;
	}
	
	public int getPause() {
		return pauseVal;
	}


	// Accessor methods for probability of a farm sale
	public void setPFarmSale(double p) {
		pFarmSale = p;
	}

	public double getPFarmSale() {
		return pFarmSale;
	}
	
	public void setPRuralLotSale(double p) {
		pRuralLotSale = p;
	}

	public double getPRuralLotSale() {
		return pRuralLotSale;
	}
	
	public void setFarmWidth(int wid) {
		farmWidth = wid;
	}

	public int getFarmWidth() {
		return farmWidth;
	}

	public void setFarmHeight(int ht) {
		farmHeight = ht;
	}

	public int getFarmHeight() {
		return farmHeight;
	}
		
	public void setZonedMinLotSize0(int z0) {
		zonedMinLotSize0 = z0;
	}

	public int getZonedMinLotSize0() {
		return zonedMinLotSize0;
	}

	public void setZonedMinLotSize1(int z1) {
		zonedMinLotSize1 = z1;
	}

	public int getZonedMinLotSize1() {
		return zonedMinLotSize1;
	}

	// Accessor methods for residents per turn
	public void setResPerTurn(int r) {
		resPerTurn = r;
	}

	public int getResPerTurn() {
		return resPerTurn;
	}

	// Accessor methods for resident demographic distributions
	public void setDemogrKids(double dk) {
		demogrKids = dk;
	}

	public double getDemogrKids() {
		return demogrKids;
	}

	public void setDemogrIncome(double di) {
		demogrIncome = di;
	}

	public double getDemogrIncome() {
		return demogrIncome;
	}

	public void setDemogrEnv(double de) {
		demogrEnv = de;
	}

	public double getDemogrEnv() {
		return demogrEnv;
	}

	public String getMapDir() {
		return mapDir;
	}
	
	public void setMapDir(String dir) {
		mapDir = dir;
	}
	
	public int getWorldXSize() {
		return worldXSize;
	}
	
	public void setWorldXSize(int size) {
		worldXSize = size;
	}

	public int getWorldYSize() {
		return worldYSize;
	}
	
	public void setWorldYSize(int size) {
		worldYSize = size;
	}
	

	///   SECTION RRR  ----------------------------------------------------------------------
	////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////


	// generic setters/getters

	public String[] getCommandLineArgs () { return commandLineArgs; }
	public void setCommandLineArgs ( String[] arguments ) {
		// for debugging...
		/*
		for( int i = 0; i < arguments.length; i++ ) {
			System.out.println( "setCommandLineArgs[" + i + "] = " + arguments[i] );
		}
		*/
		commandLineArgs = arguments;
	}

	public String getModelType() { return modelType; }
	public void setModelType( String s ) {
		modelType = s;
	}

	public String getInitialParametersFileName () { 
		return initialParametersFileName; }
	public void setInitialParametersFileName ( String s ) {
		initialParametersFileName = s;
	}

	//	public String getInitialAgentsFileName () { 
	//	return initialAgentsFileName; }
	//public void setInitialAgentsFileName ( String s ) {
	//	initialAgentsFileName = s;
	//}

	public String getReportFileName () { return reportFileName; }
	public void setReportFileName ( String s ) {
		reportFileName = s;
	}

	public String getOutputDirName () { return outputDirName; }
	public void setOutputDirName ( String s ) {
		outputDirName = s;
	}

	public int getReportFrequency () { return reportFrequency; }
	public void setReportFrequency ( int i ) {
		reportFrequency = i;
	}

	public int getRunNumber () { return runNumber; }
	public void setRunNumber ( int i ) {
		runNumber = i;
	}

	public int getStopT () { return stopT; }
	public void setStopT ( int i ) {
		stopT = i;
	}

	public int getSaveRunEndState () { return saveRunEndState; }
	public void setSaveRunEndState ( int i ) {
		saveRunEndState = i;
	}

	public int getRDebug () { return rDebug; }
	public void setRDebug ( int i ) {
		if( rDebug == i ) {
			System.out.println( "setRDebug called, but value unchanged, returning" );
			return;
		}
		System.out.println( "setRDebug ( " + i + " ) called" );
		rDebug = i;
		if( modelType.equals( "GUIModel" ) ) {
			updateAllProbePanels();
		}
		if( schedule != null )
			writeChangeToReportFile ( "rDebug", String.valueOf( i ) );
	}

	public PrintWriter getReportFile () { return reportFile; }
	public PrintWriter getPlaintextReportFile () { return plaintextReportFile; }
	public Schedule getSchedule() { return schedule; }

	// --------------------------------------------------------------
	//  RNG related methods
	public long getSeed () { return seed; }
   	// setSeed
	// this calls resetRNGenerators because once you change the seed you invalidate
   	// any previously created distributions
	public void setSeed ( long i ) {
		seed = i;
		resetRNGenerators();
		if( modelType.equals( "GUIModel" ) ) {
			updateAllProbePanels();
		}
		if( schedule != null)
			writeChangeToReportFile ( "seed", String.valueOf( i ) );
	}
	public void resetRNGenerators ( ) {
		if ( rDebug > 0 )
			Format.printf( "\nresetRNGenerators with %d\n", p.add( getSeed() ) );
   		Random.setSeed( getSeed() );
		Random.createUniform();
		Random.createNormal( 0.0, 1.0 );
		//uchicago.src.sim.util.Random.setSeed( seed );
		//uchicago.src.sim.util.Random.createUniform();
		//uchicago.src.sim.util.Random.createNormal( 0.0, 1.0 );
	}
	// NOTE: these are class methods!
	static public int getUniformIntFromTo ( int low, int high ) {
		return Random.uniform.nextIntFromTo( low, high );
	}
	static public double getNormalDouble ( double mean, double var ) {
		return Random.normal.nextDouble ( mean, var );
	}
	static public double getUniformDoubleFromTo( double low, double high ) {
		return Random.uniform.nextDoubleFromTo( low, high );
	}
	// loop until a number between 0 and 1 is generated,
	// if mean and var are set correctly the loop will rarely happen
	static public double getNormalDoubleProb ( double mean, double var ) {
		if ( mean < 0 || mean > 1 ) {
			System.out.println ( "Invalid value set for normal distribution mean" );
			return -1;
		}
		double d = getNormalDouble ( mean, var );
		while ( d < 0 || d > 1 )
			d = getNormalDouble ( mean, var );

		return d;
	}

	// ----------------------------------------------------------------------
	// some generic utilities
	public void updateAllProbePanels() {
		DMSG ( 2, "updateAllProbePanels()" );
		ProbeUtilities.updateProbePanels();

		// kludge...
		// need this in case updateAllProbePanels gets called
		// before the probe panel is created (if it is called
		// before, then a RuntimeException occurs)
		// did have if(schedule != null), but that means panels
		// do not update at all during time=0, so people get confused.
		try {
			ProbeUtilities.updateModelProbePanel();
		}
		catch (RuntimeException e) {
			// ignore exception
			DMSG( 3, "RuntimeException when updating model probe panel, ignoring ..." );
		}
	}

	// captialize first character of s
	protected String capitalize( String s ) {
		char c = s.charAt( 0 );
		char upper = Character.toUpperCase( c );
		return upper + s.substring( 1, s.length() );
	}


	// REPORT FILE PROCESSING ------------------------------
	//
	// Generic methods for writing to the plain and xml report files,
	// for closing the files, etc.
	//
	// startReportFile
	// opens two report files
	// one XML report file and one plaintext report file
	// call writeLineToReportFile to write to XML report file
	// and writeLineToPlaintextReportFile to write to plaintext file
	// calls writeReportFileHeaders to write model-specific headers.

	public PrintWriter startReportFile ( ) {
		if ( rDebug > 0 ) System.out.println( "startReportFile called!" );
		reportFile = null;
		plaintextReportFile = null;

		String fullFileName = reportFileName + Format.sprintf( ".%02d", p.add(runNumber) );
		String xmlFullFileName = reportFileName + ".xml" 
						+ Format.sprintf( ".%02d", p.add(runNumber) );

		// BufferedReader inFile = IOUtils.openFileToRead(initialParametersFileName);

		reportFile = IOUtils.openFileToWrite( outputDirName, xmlFullFileName, "r" );
		plaintextReportFile = IOUtils.openFileToWrite( outputDirName, fullFileName, "r" );

		// the first line you have to write is the XML version line
		// DO NOT WRITE THIS LINE USING writeLineToReportFile()!
		reportFile.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

		writeLineToReportFile( "<reportfile>" );

		writeLineToPlaintextReportFile( "# begin reportfile" );

		//  write the initial parameters to the report file
		writeParametersToReportFile();

		// customize this to match data in the report file
		writeReportFileHeaders();

		return reportFile;
	}

	public void writeLineToReportFile ( String line ) {
		if ( reportFile == null ) {
			DMSG( 3, "report file not opened yet" );
			// click the initialize button to open it! 
			// returning w/o writing to report file ...");
			return;
		}
		else {
			reportFile.println( line );
		}
	}

	public void writeLineToPlaintextReportFile ( String line ) {
		if ( plaintextReportFile == null ) {
			DMSG( 3, "report file not opened yet" );
			// click the initialize button to open it! 
			// returning w/o writing to report file ...");
			return;
		}
		else {
			plaintextReportFile.println( line );
		}
	}

	public void writeChangeToReportFile( String varname, String value ) {
		DMSG( 1, "writeChangeToReportFile(): write change to report file: " 
			+ varname + " changed to " + value );

		writeLineToReportFile( "<change>" );
		writeLineToReportFile( "\t<" + varname + ">" + value 
									+ "</" + varname + ">" );
		writeLineToReportFile( "\t<time>" + getTickCount() + "</time>" );
		writeLineToReportFile( "</change>" );

		writeLineToPlaintextReportFile( "# change:  " + varname + "=" + value );
	}

	/*	public void writeAgentChangeToReportFile( int id, String varname, String value ) {
		DMSG( 1, "writeAgentChangeToReportFile():  writing change to report file for agent " 
			+ id + ": " + varname + " changed to " + value );
		writeLineToReportFile( "<agentchange>" );
		writeLineToReportFile( "\t<id>" + id + "</id>" );
		writeLineToReportFile( "\t<" + varname + ">"
					+ value 
					+ "</" + varname + ">" );
		writeLineToReportFile( "\t<time>" + getTickCount() + "</time>" );
		writeLineToReportFile( "</agentchange>" );

		writeLineToPlaintextReportFile( "# change agent " + id + ":  " + varname + "=" + value );
		}*/


	public void endReportFile ( ) {
		writeLineToReportFile( "</reportfile>" );
		writeLineToPlaintextReportFile( "# end report file" );
		IOUtils.closePWFile( reportFile );
		IOUtils.closePWFile( plaintextReportFile );
	}


	// this iterates through the values stored in the parametersMap, 
	// calls the getter on each parameter, and outputs the 
	// parameter and its value to the report file.
	// this is called right before the model run starts (after all 
	// initial parameters are changed!) so 
	// the initial parameters are in the report file.
	public void writeParametersToReportFile() {
		DMSG( 1, "writeParametersToReportFile()" );

		writeLineToReportFile( "<parameters>" );
		writeLineToPlaintextReportFile( "# begin parameters" );

		ArrayList parameterNames = new ArrayList( parametersMap.values() );
		for( int i = 0; i < parameterNames.size(); i++ ) {
			Method getmethod = null;
			getmethod = findGetMethodFor( (String) parameterNames.get( i ) );

			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );

					writeLineToReportFile( "\t<" + parameterNames.get(i) + ">"
								+ returnVal 
								+ "</" + parameterNames.get(i) + ">" );

					writeLineToPlaintextReportFile( parameterNames.get(i)
									+ "="
									+ returnVal );

				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				Format.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					p.add ( parameterNames.get( i ) ) );
				Format.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}
		writeLineToReportFile( "</parameters>" );
		writeLineToPlaintextReportFile( "# end parameters" );
	}


	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	// parseParametersFile
	//
	public void parseParametersFile() {
		// a klunky way to see if the parameters file exists
		try {
			BufferedReader inFile = 
				IOUtils.openFileToRead(initialParametersFileName );
			IOUtils.closeBRFile( inFile );
		}
		catch(Exception e) { // not an error, just not there!
			Format.printf( "  -- no initialParametersFileName '%s' to parse.\n", 
						   p.add( initialParametersFileName ) );
			return;
		}

		try {
			//setup the input file
			DocumentBuilderFactory myDBF = DocumentBuilderFactory.newInstance();
			DocumentBuilder myDB = myDBF.newDocumentBuilder();
			Document myDocument = myDB.parse(initialParametersFileName);

			if ( rDebug > 0 )
				System.out.println("Parsing parameter file: "+initialParametersFileName);

			NodeList tmpList = myDocument.getElementsByTagName("parameters");
			Element tmpElement = (Element)tmpList.item(0);
			NodeList parameterList = tmpElement.getElementsByTagName("*");

			for(int i = 0; i < parameterList.getLength(); i++) {
				if ( parameterList.item(i).getChildNodes().item(0) == null)
					continue;
				DMSG( 1, "name:  " + parameterList.item(i).getNodeName()
							+ "  value:  "
							+ parameterList.item(i).getChildNodes().item(0).getNodeValue() );
				set( parameterList.item(i).getNodeName(), 
					parameterList.item(i).getChildNodes().item(0).getNodeValue() );
			}

			// process changes
			NodeList parameterChangeList = myDocument.getElementsByTagName( "change" );
			processChangeList( parameterChangeList );

			// process agent changes
			//			NodeList agentParameterChangeList = myDocument.getElementsByTagName( "agentchange" );
			//	processAgentChangeList( agentParameterChangeList );

			DMSG( 1, "Done parsing file:  " + initialParametersFileName );
		}
		catch(Exception e) {
			System.out.println("Exception when parsing parameters file:  "
						+ initialParametersFileName);
			System.out.println("Is the file in the correct format?");
			e.printStackTrace();
		}
	}


	/////////////////////////////////////////////////////////////////////////
	// processCommandLinePars
	// storeParameter
	//
	//
	public void processCommandLinePars ( String[] args ) {

		int r;
		if( args.length > 0 
			&& ( args[0].equals( "--help" ) || args[0].equals( "-h" ) ) ) {
			printProjectHelp();
		}

		for ( int i = 0; i < args.length; ++i ) {
			r = storeParameter( args[i] );
			if( r != 0 )  {
				System.out.println( "Error processing cmdLine par:  " + args[i] );
			}
		}
	}

	// storeParameter
	// format:  parname=value
	// parse out parname, and find method for setParname
	// if not found, return -1
	// otherwise set the value and return 0.
	// to set the value, we have to get the setMethod, and its par type.
	// then convert the string value to the appropriate object, and
	// use invoke to do the setting!

	public int storeParameter ( String line ) {
		int r = 0;
		String pname, pvalue;
		StringTokenizer st = new StringTokenizer( line, "=;," );
		Method setm = null;

		if ( ( pname = st.nextToken() ) == null ) {
			Format.printf("\n** storeParameter -- couldn't find pname on '%s'.\n",
						  p.add( line ) );
			return -1;
		}
		if ( ( pvalue = st.nextToken() ) == null ) {
			Format.printf("\n** storeParameter -- couldn't find value on '%s'.\n",
						  p.add( line ) );
			return -1;
		}
		pname = pname.trim();
		pvalue = pvalue.trim();

		pname = aliasToParameterName ( pname );

		//		Format.printf("-storeParameter: '%s' -> pname '%s' pvalue '%s'.\n",
		//			  p.add(line).add(pname).add(pvalue) );

		// if this is a scheduledChange, create the change
		// and insert it into the changesVector
		if( pname.equals ( "sC" ) ) {
			String changetime = pvalue;
			String changepname, changepvalue;

			if ( ( changepname = st.nextToken() ) == null ) {
				System.out.println( "\n** storeParameter -- couldn't find "
					+ "scheduleChange pname on:  " + line );
				return -1;
			}

			if ( ( changepvalue = st.nextToken() ) == null ) {
				System.out.println( "\n** storeParameter -- couldn't find "
					+ "scheduleChange pvalue on:  " + line );
				return -1;
			}

			changepname = changepname.trim();
			changepvalue = changepvalue.trim();

			changepname = aliasToParameterName ( changepname );

			ChangeObj newChange = new ChangeObj( Integer.parseInt( changetime ),
						changepname, changepvalue );

			DMSG ( 1, "scheduledChange from command line created:  "
				+ "  Time:  " + changetime + "  pname:  "
				+ changepname + "  pvalue:  " + changepvalue );
			changesVector.add ( newChange );

			return 0;
		}

		setm = findSetMethodFor( pname );
		String ptype = getParTypeOfSetMethod( setm );
		try {
			setm.invoke( this, new Object[] { valToObject( ptype, pvalue ) } );
		} catch ( Exception  e ) {
			Format.printf( "\n storeParameter: '%s'='%s' invoke exception!\n", 
						   p.add( pname ).add( pvalue ) );
			e.printStackTrace();
			return -1;
		}

		if( pname.equals ( "initialParametersFileName" ) ) {
			DMSG( 1, "Processing initial parameters file:  " + pvalue );
			parseParametersFile();
		}

		return r;
	}

	// returns the long parameter name if the parameter passed in is
	// an alias.  if it is not an alias, the name sent to it is returned.
	public String aliasToParameterName ( String alias ) {
		// check to see if "alias" is an alias in the parametersMap
		// if it is then "alias" is a valid alias, so set "alias" to the 
		// actual parameter name that is in the map
		if( parametersMap.containsKey( alias ) ) {
			DMSG( 1, "Converting alias " + alias + " to " + parametersMap.get( alias ) );
			alias = (String) parametersMap.get( alias );
		}

		return alias;
	}

	// getParTypeOfSetMethod
	// get type of setPar method parameter
	public String getParTypeOfSetMethod ( Method m ) {
		Class[] parTypes = m.getParameterTypes();
		String s = parTypes[0].getName();
		return s;
	}

	// findGetMethodFor
	// find get<ParName> method for specified parameter name 
	private Method findGetMethodFor( String varname ) {
		String methodname = new String( "get" + capitalize( varname ) );
		Class c = getClass();
		Method[] methods = c.getMethods();
		Method getmethod = null;

		for ( int j = 0; j < methods.length; j++ ) {
			if ( methods[j].getName().equals( methodname ) ) {
				getmethod = methods[j];
				break;
			}
		}
		if ( getmethod == null ) {
			Format.printf( "\n** findGetMethodFor -- couldn't find '%s'\n",
						   p.add( methodname ) );
			return getmethod;
		}

		return getmethod;
	}

	// findSetMethodFor
	// find set<ParName> method for specified parameter name 
	public Method findSetMethodFor ( String pname ) {
		Class c = this.getClass();
		Method[] methods = c.getMethods();
		int nf = methods.length;
		String setmethodname = "set" + capitalize( pname );
		String mname;
		Method method = null;
		for ( int i = 0; i < nf; ++i ) {
			mname = methods[i].getName();
			if ( mname.equals( setmethodname ) ) {
				method = methods[i];
				break;
			}
		}
		if ( method == null ) {
			Format.printf( "\n** findSetMethodFor -- couldn't fine '%s'\n",
						   p.add( setmethodname ) );
			return method;
		}
		return method;
	}

	// valToObject
	// return value stored in object of appropriate type
	private Object valToObject( String type, String val ) {
		if ( type.equals( "int" ) ) {
		  return Integer.valueOf( val );
		} else if ( type.equals( "double" ) ) {
		  return Double.valueOf( val );
		} else if ( type.equals( "float" ) ) {
		  return Float.valueOf( val );
		} else if ( type.equals( "long" ) ) {
		  return Long.valueOf(val);
		} else if ( type.equals( "boolean" ) ) {
		  return Boolean.valueOf(val);
		} else if ( type.equals( "java.lang.String" ) ) {
		  return val;
		} else {
		  throw new IllegalArgumentException( "illegal type" );
		}
	}

	public String skipCommentLines ( BufferedReader inFile ) {
		String line;
		while ( ( line = IOUtils.readBRLine ( inFile ) ) != null ) {
			if ( line.charAt(0) != '#' )
				break;
		}
		return line;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	// applyAnyStoredChanges
	// look through all of the changes, if any have time of this time 
	// step execute the change
	public void applyAnyStoredChanges () {
		if ( rDebug > 1 ) 
			System.out.println( "applyAnyStoredChanges called at time step: " 
						   + getTickCount() );

		for( int i = 0; i < changesVector.size(); i++ ) {
			ChangeObj tmpObj = (ChangeObj) changesVector.get( i );
			if( tmpObj.time == getTickCount() ) {
				if ( rDebug > 0 )
					System.out.println( "applyAnyStoredChanges():  Changing " 
								   + tmpObj.varname + " to " +tmpObj.value );
				set( tmpObj.varname, tmpObj.value );
			}
		}
	}

	// applyAnyStoredAgentChanges
	// look through all of the changes, if any have time of this time step 
	// execute the change
	/*	public void applyAnyStoredAgentChanges () {
		System.out.println( "applyAnyStoredAgentChanges called at time step: " 
						   + getTickCount() );

		for( int i = 0; i < agentChangesVector.size(); i++ ) {
			ACChangeObj tmpObj = (ACChangeObj) agentChangesVector.get( i );
			if( tmpObj.time == getTickCount() )
			{
				System.out.println( "applyAnyStoredAgentChanges():  Changing agent " 
					  + tmpObj.id + " " + tmpObj.varname + " to " +tmpObj.value );
				//				setAgentParameter( getAgentWithID( tmpObj.id ), tmpObj.varname, tmpObj.value );
			}
		}
		}*/
	///////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	// utility methods for accessing parts of model

	private void setObjectParameter( Object inObject, String varname, String value ) {
		String methodname = new String( "set" + capitalize( varname ) );
		Class c = inObject.getClass();
		Method[] methods = c.getMethods();
		Method setmethod = null;

		for ( int j = 0; j < methods.length; j++ ) {
			if ( methods[j].getName().equals( methodname ) ) {
				setmethod = methods[j];
				break;
			}
		}

		if(setmethod != null) {
			try {
				Class[] parameterTypes = setmethod.getParameterTypes();
				if( parameterTypes[0].getName().equals( "int" ) ) {
					DMSG( 3, "int parameter type" );
					setmethod.invoke( inObject, new Object[] { Integer.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "long" ) ) {
					DMSG( 3, "long parameter type" );
					setmethod.invoke( inObject, new Object[] { Long.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "double" ) ) {
					DMSG( 3, "double parameter type" );
					setmethod.invoke( inObject, new Object[] { Double.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "float" ) ) {
					DMSG( 3, "float parameter type" );
					setmethod.invoke( inObject, new Object[] { Float.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "java.lang.String" ) ) {
					DMSG( 3, "String parameter type" );
					setmethod.invoke( inObject, new Object[] { value } );
				}
				else {
					System.out.println( 
						"- setObjectParameter COULD NOT DETERMINE PARAMETER TYPE FOR " 
						+ varname );
				}
				DMSG( 1, "setObjectParameter():  " + varname + " changed to " + value );
			} catch( Exception e ) { e.printStackTrace(); }
		}
		else {
			System.out.println( "COULD NOT FIND SET METHOD FOR:  " + varname );
		}
	}

	private void processChange(Element c) {

		System.out.println("Processing A Change");

		NodeList tmpList = c.getElementsByTagName("*");

		ChangeObj newChange = new ChangeObj(0,"","");

		for(int i = 0; i < tmpList.getLength(); i++)  {
			Element tmpElement = (Element)tmpList.item(i);
			// System.out.println("tmpElement.getTagName(): " + tmpElement.getTagName());
			if(tmpElement.getTagName().equals("time")) {
				newChange.time = Integer.parseInt(tmpElement.getChildNodes().item(0).getNodeValue());
			}
			else {
				newChange.varname = tmpElement.getTagName();
				newChange.value = tmpElement.getChildNodes().item(0).getNodeValue();
			}
		}

		changesVector.add(newChange);

		System.out.println("Done processing a Change");
	}

	private void processChangeList( NodeList c ) {

		if ( rDebug > 1 )
			System.out.println( "Processing " + c.getLength() + " changes ..." );
		for( int i = 0; i < c.getLength(); i++ )
			processChange( (Element) c.item( i ) );

		for( int i = 0; i < changesVector.size(); i++ ) {
			ChangeObj tmpObj = (ChangeObj) changesVector.get( i );
			DMSG( 1, "Time:  " + tmpObj.time + "  VarName:  " 
				+ tmpObj.varname + "  Value:  " + tmpObj.value );
		}
	}

	/*	private void processAgentChange( Element c ) {

		System.out.println( "Processing an Agent Change" );

		NodeList tmpList = c.getElementsByTagName( "*" );

		ACChangeObj newChange = new ACChangeObj( 0, 0, "", "" );

		for ( int i = 0; i < tmpList.getLength(); i++ ) {
			Element tmpElement = (Element) tmpList.item( i );
			//System.out.println("tmpElement.getTagName(): " + tmpElement.getTagName());
			if (tmpElement.getTagName().equals( "time" )) {
				newChange.time = Integer.parseInt( tmpElement.getChildNodes().item(0).getNodeValue() );
			}
			else if (tmpElement.getTagName().equals("id")) {
				newChange.id = Integer.parseInt( tmpElement.getChildNodes().item(0).getNodeValue() );
			}
			else {
				newChange.varname = tmpElement.getTagName();
				newChange.value = tmpElement.getChildNodes().item( 0 ).getNodeValue();
			}
		}

		agentChangesVector.add( newChange );

		System.out.println( "Done processing Agent Change" );
		}*/

	/*	private void processAgentChangeList(NodeList c) {

		if ( rDebug > 0 )
			System.out.println( "Processing " + c.getLength() + " agent changes ..." );
		for ( int i = 0; i < c.getLength(); i++ )
			processAgentChange( (Element) c.item(i) );

		for ( int i = 0; i < agentChangesVector.size(); i++ ) {
			ACChangeObj tmpObj = (ACChangeObj) agentChangesVector.get( i );
			DMSG( 1, "Time:  " + tmpObj.time + "  ID:  " 
				+ tmpObj.id + "  VarName:  " 
				+ tmpObj.varname + "  Value:  " + tmpObj.value );
		}
		}*/

	private void set( String varname, String value ) {

		// first convert varname to the alias, if it is an alias
		varname = aliasToParameterName ( varname );

		Method setmethod = findSetMethodFor ( varname );

		if( setmethod != null ) {
			try {
				Class[] parameterTypes = setmethod.getParameterTypes();
				if ( parameterTypes[0].getName().equals( "int" ) ) {
					DMSG( 3, "int parameter type" );
					setmethod.invoke( this, new Object[] { Integer.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "long" ) ) {
					DMSG( 3, "long parameter type" );
					setmethod.invoke( this, new Object[] { Long.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "double" ) ) {
					DMSG( 3, "double parameter type" );
					setmethod.invoke( this, new Object[] { Double.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "float" ) ) {
					DMSG( 3, "float parameter type" );
					setmethod.invoke( this, new Object[] { Float.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "java.lang.String" ) ) {
					DMSG( 3, "String parameter type" );
					setmethod.invoke( this, new Object[] { value } );
				}
				else {
					System.out.println( "-set COULD NOT DETERMINE PARAMETER TYPE FOR " 
										+ varname );
				}
				DMSG( 1, "set():  " + varname + " changed to " + value );
			} catch( Exception e ) { e.printStackTrace(); }
		}
		else {
			System.out.println( "COULD NOT FIND SET METHOD FOR:  " + varname );
			System.out.println( "Is the parameter name correct?" );
		}
	}


	boolean isPar(String varname) {
		String parameternames[] = getInitParam();
		for ( int i = 0; i < parameternames.length; i++ ) {
			if ( parameternames[i].equals( varname ) ) {
				DMSG( 3, varname + " is a parameter" );
				return true;
			}
		}

		Format.printf( "'%s' is NOT a parameter", p.add( varname ) );
		return false;
	}

	// loadChangeParameters
	// we expect to see
	//   @changeParameters
	//   step=<timeStep>
	//   parName=parValue
	//   ...
	//   @endChangeParameters
	//   <timeStep> is time step changes are to occur.
	//   store in changeSteps[numberOfChanges]
	//   store number of parameters to change in changeIDs[numberOfChanges]
	//   increment 	 numberOfChanges
	// Return 0 if ok, 1 if not.  next line will be after @endChangeParameters
	public int loadChangeParameters( BufferedReader inFile ) {
		ArrayList lines = new ArrayList(16);
		String line, ends = "@endChangeParameters";
		int r,  step, numPars = 0, done = 0;
		NumberVariable iV = new IntegerVariable();
		Format.printf( "\n\n*** loadChangeParameters \n\n" );

		// first get the step= line, and the time and ID values
		line = skipCommentLines( inFile );
		Format.printf("0: %s\n", p.add(line) );
		r = Format.sscanf( line, "step=%i", p.add(iV) );
		step = iV.intValue();

		// get lines into a bunch of strings, add to list of these sets of lines.
		while ( done == 0 ) {
			line = skipCommentLines( inFile );
			if ( line.equals( ends ) )
				done = 1;
			else {
				// *** It would be nice to check these here...
				lines.add( line );
				++numPars;
			}
		}
		changeSpecs.add( lines );

		if ( numPars == 0 ) {  // oops!
			Format.printf( "\n*** loadChangeParameters found 0 changes! Last line='%s'\n",
						   p.add(line) );
			return -1;
		}

		// store time and id in next place in arrays.
		changeSteps[numberOfChanges] = step;
		changeIDs[numberOfChanges] = 0 - numPars;
		++numberOfChanges;
		
		for ( int c = 0; c < numberOfChanges; ++c ) {
			if ( changeIDs[c] >= 0 )
				continue;
			lines = (ArrayList) changeSpecs.get(c);
			Format.printf( "Change %d at t=%d, ID=%d:\n", 
						 p.add(c).add(changeSteps[c]).add(changeIDs[c]) );
			for ( int i = 0; i < numPars; ++i ) {
				Format.printf("%d: %s\n", p.add(i+1).add((String)lines.get(i)) );
			}
		}

   		return 0;
	}

	// printDebugMsgs
	// place for end of step debugging actions
	public void printDebugMsgs ( ) {
		if ( rDebug == 0 )
			return;

		Format.printf("==== End of step %d  ===============\n",
					  p.add(getTickCount()) );


		Format.printf("======================================\n\n");
	}

	public void DMSG(int debugLevel, String debugStr) {
		if( rDebug >= debugLevel ) {
			System.out.println("debug:\t" + debugStr);
		}
	}
    /// END SECTION RRR from 
    ////////////////////////////////////////////////////////////////////////

	// writeReportFileHeaders
	// customize to match what you are writing to the report files in stepReport.
	//
	public void writeReportFileHeaders () {
		writeLineToReportFile( "<comment>" );
		writeLineToReportFile( "          Town0    Town0   Town0    Town0   Town0    Town1   Town1    Town1   Town1    Town1" );
		writeLineToReportFile( "  time   Income   AvgFor  CS    HS    RS  Income  AvgFor  CS    HS    RS" );
		writeLineToReportFile( "</comment>" );

		writeLineToPlaintextReportFile( "#          Town0    Town0   Town0    Town0   Town0    Town1   Town1    Town1   Town1    Town1" );
		writeLineToPlaintextReportFile( "#  time   Income   AvgFor  CS    HS    RS  Income  AvgFor  CS    HS    RS" );

	}

	// printProjectHelp
	// this could be filled in with some help to get from running with -help parameter
	//
	public void printProjectHelp() {
		// print project help

		Format.printf( "\n%s -- \n", p.add(getName()) );
	}

	public static void main(String[] args) {
		uchicago.src.sim.engine.SimInit init = new uchicago.src.sim.engine.SimInit();
		Model model = new Model();

		model.setCommandLineArgs( args );

		// starts setup
		init.loadModel(model, null, false);
	}

}

///////////////////////////////////////////////////////////////////////////
// auxilliary classes for processing changes
//
//

class ChangeObj {
	public ChangeObj() {}
	public ChangeObj(int in_time, String in_varname, String in_value) {
		time = in_time;
		varname = in_varname;
		value = in_value;
	}
	public int time;
	public String varname;
	public String value;
}

class ACChangeObj {
	public ACChangeObj() {}
	public ACChangeObj(int in_time, int in_id, String in_varname, String in_value) {
		time = in_time;
		id = in_id;
		varname = in_varname;
		value = in_value;
	}
	public int time;
	public int id;
	public String varname;
	public String value;
}


