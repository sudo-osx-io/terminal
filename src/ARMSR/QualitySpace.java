///////////////////////////////////////////////////////////////////////////////
//  QualitySpace.java
//  Sluce Project
//  This defines the quality spaces which are where we keep track of all 
//    attributes of cells in the world
package ARMSR;

import uchicago.src.sim.space.RasterSpace;  // The base class
import java.io.IOException;

public class QualitySpace extends RasterSpace {

	// this simple constructor just calls the parent constructor with the
	//   right world sizes
	public QualitySpace(int x, int y) {
		super(0,0,1,y,x);
	}

	// call the rasterSpace constructor
	public QualitySpace(String fname) throws IOException {
		super(fname);
	}

	// CURRENTLY NOT USED
	// scale the space, this is necessary because ESRI outputs integers
	public void scale() {
		
		// go through the whole space and divide by 8191
		for (int x=0; x<getSizeX(); x++) {
			for (int y=0; y<getSizeY(); y++) {

				putValueAt(x, y, getValueAt(x,y) / 8191.0);

				double value = getValueAt(x,y);
				//				System.out.println("Putting " + value + " at " + x + ", " + y);

			}
		}
	}

	// this will initialize the grid with random values given an average
	//  and standard deviation, and with upper and lower limits
	void initializeNormalMean(double mu, double sigma, double lower, double upper) {
		int x,y;     // the current values

		// loop through the world
		//			System.out.println("size x is " + getSizeX());
		for ( x=0; x<getSizeX(); x++) {
			for (y=0; y<getSizeY(); y++) {
				double value;  // the value we are assigning

				// assign a random value to each cell with appropriate
				//   mean and stddev
				do {
					value = Model.getNormalDouble(mu, sigma);
				} while ((value < lower) || (value > upper));

				putValueAt( x, y, value);

				// now make sure that the value was assigned
				//	System.out.println("Putting " + value + " at " + x + ", " + y);
				//System.out.println("Getting " + getValueAt(x,y) + " at " + x + ", " + y); 
			}
		}
	}

	// initialize with 1 or 0's where p is the probability of generating a 1
	void initializeBinary(double p) {
		int x,y;     // the current values

		// loop through the world
		for ( x=0; x<getSizeX(); x++) {
			for (y=0; y<getSizeY(); y++) {
				double value;  // the value we are assigning

				// assign a random value to each cell using p to bias
				if (p < Model.getUniformDoubleFromTo(0,1)) {
					value = 1;
				}
				else {
					value = 0;
				}

				putValueAt( x, y, value);
				//				System.out.println("Putting " + value + " at " + x + ", " + y);
			}
		}
	}
	
}

