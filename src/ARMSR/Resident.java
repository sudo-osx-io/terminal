/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the Resident class
 *$$*/
package ARMSR;

import java.util.ArrayList;  // This is the array list storage class
import java.awt.Color;       // For colors
import uchicago.src.sim.util.Random.*;

/**
 * The Resident for ARMSR
 */

public class Resident extends Owner {

	// Define some constants for use by the class
	
	public static final int KIDS_YES = 1;
	public static final int KIDS_NO = 0;

	public static final int INCOME_HIGH = 1;
	public static final int INCOME_LOW = 0;

	public static final int ENVGROUP_YES = 1;
	public static final int ENVGROUP_NO = 0;

	private static int nextID = 0; // A class variable to keep track of what 
	                               //   resident this is

	private Lot lot;               // the lot that the resident owns
	private java.awt.Color color;  // The color of all cells belonging to this 
                                   //  aggregate
	private int resID;             // The id for this instance

	// distribution of demographic characteristics
	private static double demogrKids = 0.0;
	private static double demogrIncome = 0.0;
	private static double demogrEnv = 0.0;

	// characteristics for the residents
	private int kids;              // Declared above as KIDS_YES, KIDS_NO
	private int income;            // Declared above as INCOME_HIGH, INCOME_LOW
	private int envGroup;          // Declared above as ENVGROUP_YES, ENVGROUP_NO

	// preferences (these are determined by demographics)
	private double alphaF; 
	private double alphaPV;
	private double alphaR;
	private double alphaW;
	private double alphaD;


	// township data
	private Township township;

	// assigning demographic distribution
	public static void setDemographics(double dk, double di, double de) {
		demogrKids = dk;
		demogrIncome = di;
		demogrEnv= de;
	}
	

	// The basic constructor for the resident
	public Resident()
	{
		// This sets the individual sub ID
		this.resID = nextID++;

		// color is set to black
		color = java.awt.Color.black;

		//		System.out.println("resID is " + resID + "\n");

		// Generate attributes randomly now, eventually will be set somewhere
		//   else
		initializeDemographics();

		// set the preferences based on demographics
		initializePreferences();
	}

	// this sets up the demographics for the resident
	private void initializeDemographics() {

		// Initialize kids attribute
		if (Model.getUniformDoubleFromTo(0.0, 1.0) < demogrKids) {
			kids = KIDS_YES;
		}
		else {
			kids = KIDS_NO;
		}

		// Initialize Income attribute
		if (Model.getUniformDoubleFromTo(0.0, 1.0) < demogrIncome) {
			income = INCOME_HIGH;
		}
		else {
			income = INCOME_LOW;
		}

		// Initialize envgroup attribute
		if (Model.getUniformDoubleFromTo(0.0, 1.0) < demogrEnv) {
			envGroup = ENVGROUP_YES;
		}
		else {
			envGroup = ENVGROUP_NO;
		}

	}

	// this establishes the preferences based on the demographics
	private void initializePreferences() {
		
		// Forest preference
		alphaF = 0.0;

		// only rich environmentalist really want to pay for forest
		if ( (envGroup == ENVGROUP_YES) && (income == INCOME_HIGH) ) {
			alphaF = 1.0;
		}
		else {
			alphaF = 0.5;
		}

		// Panoramic view preference
		alphaPV = 0.0;

		// only rich people can afford panoramic views
		if ( income == INCOME_HIGH ) {
			alphaPV = 1.0;
		}
		else {
			alphaPV = 0.5;
		}

		// relief preference
		alphaR = 0.0;

		// only rich environmentalist really want to pay for relief
		if ( (envGroup == ENVGROUP_YES) && (income == INCOME_HIGH) ) {
			alphaR = 1.0;
		}
		else {
			alphaR = 0.5;
		}

		// water preference
		alphaW = 0.0;

		// only rich people can afford water
		if ( income == INCOME_HIGH ) {
			alphaW = 1.0;
		}
		else {
			alphaW = 0.0;
		}

		// preference for low development
		alphaD = 0.0;

		// only rich environmentalist really want to pay to avoid development
		if ( (envGroup == ENVGROUP_YES) && (income == INCOME_HIGH) ) {
			alphaD = 1.0;
		}
		else {
			alphaD = 0.5;
		}

		/*  This is for debugging
		System.out.println("Envgroup is " + envGroup);
		System.out.println("Kids is " + kids);
		System.out.println("Income is " + income);

		System.out.println("alphaF is " + alphaF);
		System.out.println("alphaR is " + alphaR);
		System.out.println("alphaW is " + alphaW);
		System.out.println("alphaD is " + alphaD);
		System.out.println("alphaPV is " + alphaPV);
		*/

	}

	// accessor methods
	public Lot getLot() {
		return lot;
	}

	public void setLot(Lot l) {
		lot = l;
	}

	public java.awt.Color getColor() {
		return color;
	}

	// accessor methods for preference attributes
	public int getKids() {
		return kids;
	}

	public int getIncome() {
		return income;
	}

	public int getEnvGroup() {
		return envGroup;
	}

	public void setTownship(Township t) {
		township = t;
	}

	public Township getTownship() {
		return township;
	}

	// this lets the resident move out and reverts control of the lot to
	//   the subdivision
	public void moveOut() {
		// find out from the lot what subdivision it is in
		//   check to make sure its valid as well
		if (lot == null) {
			System.out.println("Can't move out we don't own a lot yet!");
			System.exit(-1);
		}

		Subdivision sub = lot.getSub();

		// tell the subdivision that the lot is for sale
		sub.forSale(lot);

		// transfer ownership of the lot back to the subdivision
		lot.setOwnedBy(sub);

		// tell the township the resident moved out
		township.removeResident(this);
		

		// make sure lot is owned by subdivision
		// if not, exit program
		Owner o = lot.getOwnedBy();
		if (o != sub) {
			System.out.println("lot is not transferred back to subdivision!");
			System.exit(-1);
		}
		// now check that cells in the lot are transferred
		ArrayList cells = new ArrayList();
		cells = lot.getCells();
		for (int i = 0; i < cells.size(); i ++) {
			Cell c = (Cell)cells.get(i);
			Owner z = c.getOwnedBy();
			if (z != sub) {
				System.out.println("cell is not transferred back to subdivision!");
				System.exit(-1);
			}
		}
		
	}

	// evaluateSub tells what the residents value for a subdivision is
	public double evaluateSub(Subdivision s) {

		// keep track of the current value
		double value = 0.0;

		// flag to check for errors
		int valueSet = 0;

		// get the subdivision type
		int subType = s.getType();

		// now assign an apprporiate value for the subdivision first check
		//   based on subdivision type
		if (subType == Subdivision.COUNTRY) {

			// Only people who aren't in environmental groups and are either
			//   poor or have kids really want to live in a country sub
			if ( (envGroup == ENVGROUP_NO) && 
				 ( (income == INCOME_LOW) || ( kids == KIDS_YES ))) {
				value = 1.0;
				valueSet++;
			}

			// Poor environmental members and rich peopble without kids
			//  will be okay with a country sub
			if ( ( ( envGroup == ENVGROUP_YES ) && ( income == INCOME_LOW) ) ||
				 ( ( income == INCOME_HIGH ) && ( kids == KIDS_NO ) && (envGroup == ENVGROUP_NO))) {
				value = 0.5;
				valueSet++;
			}
		}
		else if (subType == Subdivision.REMNANT) {

			// Rich people can affort to live in remnant subs
			if ( income == INCOME_HIGH ) {
				value = 1.0;
				valueSet++;
			}
		}
		else if (subType == Subdivision.HORT) {

			// Rich people who aren't environmentalists don't mind hort subs
			if ( ( income == INCOME_HIGH ) && ( envGroup == ENVGROUP_NO) ) {
				value = 1.0;
				valueSet++;
			}

			// Rich people who are environmentalists will live in hort subs
			//   if they have to
			if ( ( income == INCOME_HIGH ) && ( envGroup == ENVGROUP_YES ) ) {
				value = 0.5;
				valueSet++;
			}
		}

		// This is an error check, we should only set the subdivision value
		//   given the subdivision type and the resident attributes
		if (valueSet > 1) {
			System.out.println("Set the value for subdivision more than once!");
			System.exit(-1);
		}
		
		return value;
	}

	// evaluateEnvironment returns the value that the resident places on the 
	//  environmental features of a lot
	private double evaluateEnvironment(Lot l) {

		// set up a variable to store the value
		double value = 0.0;

		// first get all the relevant features
		double forest = l.getCalculatedAvgForest();
		double panoramicView = l.getCalculatedPanoramicView();
		double relief = l.getCalculatedRelief();
		double water = l.getCalculatedAvgWater();
		double development = (l.getSub()).getCalculatedDevelopment();

		// the value is the feature times the preference for that feature
		value += alphaF * forest;
		//System.out.println("value (alpha) + forest is " + value + " (" + alphaF + ")");
		value += alphaPV * panoramicView;
		//System.out.println("value (alpha) + panoramic is " + value + " (" + alphaPV + ")");
		value += alphaR * relief;
		//System.out.println("value (alpha) + relief is " + value + " (" + alphaR + ")");
		value += alphaW * water ;
		//System.out.println("value (alpha) + water is " + value + " (" + alphaW + ")");

		// only one that's different, less development is better
		value += alphaD * (1.0 - development) ;
		//System.out.println("value (alpha) + devel is " + value + " (" + alphaD + ")");
		// normalize by the number of features
		value /= 5.0;
		//System.out.println(" normalized value is " + value );
		// return the value
		return value;
		
	}

	// evaluateLot returns the value that this resident places on the
	//   lot
	public double evaluateLot(Lot l) {
		double value = 1.0;  // this keeps track of the value of the lot
		                     // defaults to 1, so that all steps are multiplications

		// this routine evaluates whether the lot is preferred based on
		//  its subdivision

		// first grab the subdivision and find out what its type is
		Subdivision sub = l.getSub();

		// the first value is whatever the subdivision value is
		value*= evaluateSub(sub);

		// then we need to include the environmental feature value
		value*= evaluateEnvironment(l);

		return value;  // return the final value
	}
}
