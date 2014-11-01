/*$$
 *  This is the first attempt at writing the Repast version of the Sluce/Some 
 *  code with developers and emigration.
 *$$*/
package ARMSR;

/**
 * Not sure how many of these imports I need but since this is
 * a first simulation in RePast I will leave them all in.
 */

import java.util.Hashtable;

import uchicago.src.reflector.DescriptorContainer;
import uchicago.src.reflector.BooleanPropertyDescriptor;

/**
 * The owner (highest level agent) for ARMSR
 */

public class Owner implements DescriptorContainer {

	private Hashtable descriptors = new Hashtable();
	private java.awt.Color color;   // The color that this owner colors

	public Owner()
	{
	}

	public java.awt.Color getColor() {
		return color;
	}

	// DescriptorContainer interface
	public Hashtable getParameterDescriptors() {
		return descriptors;
	}
}
