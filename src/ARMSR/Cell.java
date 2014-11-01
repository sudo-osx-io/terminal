/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  and this is the base class for cells.
 *$$*/
package ARMSR;

/**
 * Not sure how many of these imports I need but since this is
 * a first simulation in RePast I will leave them all in.
 */

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Color;
import java.util.Hashtable;

import uchicago.src.sim.gui.*;
import uchicago.src.sim.space.*;
import uchicago.src.sim.util.SimUtilities;

import uchicago.src.reflector.DescriptorContainer;
import uchicago.src.reflector.BooleanPropertyDescriptor;

import cern.jet.random.Uniform;

/**
 * The cell (highest level space) for ARMSR
 */

public class Cell implements Drawable, DescriptorContainer {

	// Quality spaces
	private static QualitySpace forestSpace = null;
	private static QualitySpace roadSpace = null;
	private static QualitySpace soilSpace = null;
	private static QualitySpace waterSpace = null;
	private static QualitySpace elevationSpace = null;

	private Owner ownedBy = null;    // Owner of cell

	private int x;                   // coordinates of cell
	private int y;
 
	private Object2DGrid world;
	private Dimension worldSize;
	
	private int worldXSize;
	private int worldYSize;

	private Lot lot;   // the lot that owns the cell

	private Township township;  // the township the cell is in

	private Hashtable descriptors = new Hashtable();

	public Cell(Object2DGrid world, int x, int y)
	{
		this.x = x;
		this.y = y;
		this.world = world;
		worldSize = world.getSize();
		worldXSize = worldSize.width;
		worldYSize = worldSize.height;
	}

	public void setXY(int x, int y) {
		this.x = x;
		this.y = y;
		world.putObjectAt(x, y, this);
	}

	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}

	// an accessor for the lot
	public Lot getLot() {
		return lot;
	}

	public void setLot(Lot l) {
		lot = l;
	}
	
	public void draw(SimGraphics g) {
		if (ownedBy != null) {
			g.drawFastRect(ownedBy.getColor());
		}
		else {
			g.drawFastRoundRect(Color.black);
		}
	}

	// DescriptorContainer interface
	public Hashtable getParameterDescriptors() {
		return descriptors;
	}	

	public Owner getOwnedBy() {
		return ownedBy;
	}

	public void setOwnedBy(Owner o) {
		ownedBy = o;
	}

	public void setTownship(Township t) {
		township = t;
	}

	public Township getTownship() {
		return township;
	}

	//set the qualityspaces
	public static void setQualitySpaces(QualitySpace r, QualitySpace s, QualitySpace f, QualitySpace w, QualitySpace e) {
		roadSpace = r;
		soilSpace = s;
		forestSpace = f;
		waterSpace = w;
		elevationSpace = e;
	}

	// returns the value stored in the forestSpace
	public double getForest() {
		return forestSpace.getValueAt(x,y);
	}

	// returns the value stored in the waterSpace
	public double getWater() {
		return waterSpace.getValueAt(x,y);
	}

	// returns the value stored in the elevationSpace
	public double getElevation() {
		return elevationSpace.getValueAt(x,y);
	}

	// returns true if most neighboring cells are at least 62ft lower (20m)
	public boolean testPanoramicView() {
		double currentElevation = this.getElevation();
		//System.out.println("currentElevation is " + currentElevation + " at " + x + "," + y);
		double neighbElevation = 0.0;
		double ratio = 0.0;
		int counter = 0;
		int lowerCells = 0;
		int x = this.getX();
		int y = this.getY();
		int minX = x-1;
		int minY = y-1;
		int maxX = x+1;
		int maxY = y+1;
		
		//make sure that it stays within world bounds
		if ( x == 0 ) {
			minX = 0;
		}
		if ( x == worldXSize-1 ) {
			maxX = worldXSize-1;
		}
		if ( y == 0 ) {
			minY = 0;
		}
		if ( y == worldYSize-1 ) {
			maxY = worldYSize-1;
		}

		for ( int i = minX; i <= maxX; ++i ) {
			for ( int j = minY; j <= maxY; ++j ) {
				neighbElevation = elevationSpace.getValueAt(i,j);
				//System.out.println("neighbElevation is " + neighbElevation + " at " + i + "," + j);
				counter = counter + 1;
				if ( (neighbElevation+20) <= currentElevation ) {
					lowerCells = lowerCells + 1;
					//System.out.println("lowerCells is " + lowerCells);
				}
			}
		}
		ratio = (double) lowerCells/counter;
		//System.out.println("ratio is " + ratio);
		if ( ratio >= 0.5 ) {
			//System.out.println("panoramic is true");
			return true;
		}
		else {
			//System.out.println("panoramic is false");
			return false;
		}
	}
}

