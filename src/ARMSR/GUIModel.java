package ARMSR;

/**
 GUI extension of basic template model
**/

import java.io.*;
import java.util.*;
import java.awt.Color;
import java.awt.FileDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
// KeyListeners, MouseListeners etc.
import java.awt.event.*;

//MSM
import javax.swing.event.MouseInputAdapter;


// import Graphics, necessary for code to override  the DisplaySurface class
import java.awt.Graphics;

import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.*;
import uchicago.src.sim.space.*;
import uchicago.src.sim.network.*;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.util.Random;
import uchicago.src.sim.analysis.*;
import uchicago.src.collection.RangeMap;
import uchicago.src.sim.event.SliderListener;
import uchicago.src.sim.util.*;

import com.braju.beta.format.*; // for printf
import com.braju.beta.lang.*;
import rlriolo.ioutils.*;

import graph3d.*;

public class GUIModel extends Model {

	// implementation variables
    public  OpenSequenceGraph		graph;
	private DisplaySurface			dsurf;


	// if you use this initialView for the "view" of the graph, then
	// you have a top down view of the graph that corresponds to
	// the top-down view in the network displays
	final double[] initialView = 
		new double[] {0, .5, 0, 0, -.5, 0, 0, 0, 
			0, 0, .5, 0, 0, 0, 0, 1.0 };

	///////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////
	// main entry point
	public static void main( String[] args ) {

		uchicago.src.sim.engine.SimInit init =
			new uchicago.src.sim.engine.SimInit();
		GUIModel model = new GUIModel();

		// set the type of model class, this is necessary
		// so the parameters object knows whether or not
		// to do GUI related updates of panels,etc when a
		// parameter is changed
		model.setModelType("GUIModel");

        // CHANGE 9/12/02
        // Do this to set the Update Probes option to true in the
        // Repast Actions panel
        Controller.UPDATE_PROBES = true;

		model.setCommandLineArgs( args );
		init.loadModel( model, null, false ); // does setup()

		// this new function calls ProbeUtilities.updateProbePanels() and 
		//ProbeUtilities.updateModelProbePanel()
		model.updateAllProbePanels();
		//ProbeUtilities.updateProbePanels() updates all probe panels except the model's
		// ProbeUtilities.updateModelProbePanel() updates the parameter panel
		//		ProbeUtilities.updateProbePanels();
		model.testVar( "main done" );
	}

	public void testVar ( String msg ) {
		Format.printf("  (GUIModel) %s -- \n",
			 p.add(msg) );
	}

	/////////////////////////////////////////////////////////////////////
	// the setup() function runs automatically when the model starts
	// and when you click the reload button
	public void setup() {
	   	Format.printf( "GUIModel setup() called...\n" );

		super.setup();

	   	Format.printf( "GUIModel setup() continued...\n" );
		if (graph != null)  graph.dispose();
		graph = null;

		if (dsurf != null) dsurf.dispose();

		dsurf = null;
		dsurf = new MyDisplaySurface(0, this, "Agent Display");
		registerDisplaySurface("Main Display", dsurf);

		//		Agent.setGUIModel(this);
		setupCustomAction();
		updateAllProbePanels();
	}

	// the begin() function runs when you click the "initialize" button
	// (the button with the single arrow that goes around in a circle)
	public void begin()	{
		testVar( "---> enter GUIModel-begin()" );
		//  **** KLUDGE ALERT -- watch this constant, and also
		//           it must be passed to classes 
		//           **before** they are loaded!!

		buildModel();     // the base model does this
		buildDisplay();
		buildSchedule();
		dsurf.display();
		//updateGraphs();
		testVar( "<--- GUIModel-leave begin()" );
	}

	// builds the display and display related things
	public void buildDisplay() {
		Object2DDisplay worldDisplay = new Object2DDisplay(world);
		worldDisplay.setObjectList(cellList);
		
		dsurf.addDisplayableProbeable(worldDisplay, "Cells");

		addSimEventListener(dsurf);
	}

	public void step() {
		// Call Model's step()
		//		Format.printf("---> enter GUIModel-step %d, call super.step()...\n", 
		//			  p.add(getTickCount()));
		super.step();
		//SimUtilities.updateProbePanel( this );
		dsurf.updateDisplay();
		//		Format.printf("\n---> end of GUIModel step %d\n",
		//		p.add(getTickCount()) );
		updateAllProbePanels();
	}

	////////////////////////////////////////////////////////////////
	// builds the schedule
	// 
	public void buildSchedule() {

		// call the buildSchedule() function shared by the batch and GUI models
		super.buildSchedule();

		// schedule the current GUIModel's step() function
		// to execute every time step starting with time step 0
		schedule.scheduleActionBeginning( 0, this, "step" );
		schedule.scheduleActionBeginning (0, this, "stepReport");

		// schedule the current GUIModel's processEndOfRun() 
		// function to execute at the end of the run
		schedule.scheduleActionAtEnd( this, "processEndOfRun" );
	}


	// setupCustonAction
	//
	private void setupCustomAction() {

		modelManipulator.init();
		/*
		modelManipulator.addButton( "Save Agents", new ActionListener() {
			public void actionPerformed ( ActionEvent evt ) {
				//				saveAgents();
			}
		} );

		modelManipulator.addButton( "Load Agents", new ActionListener() {
			public void actionPerformed ( ActionEvent evt ) {
				//				parseAgentsFile();
			}
			} ); */

		modelManipulator.addButton( "Update Parameters", new ActionListener() {
			public void actionPerformed ( ActionEvent evt ) {
				//				updateClassParameters();
			}
		} );

		modelManipulator.addButton( "Update Displays", new ActionListener() {
			public void actionPerformed ( ActionEvent evt ) {
				updateAllProbePanels();
			}
		} );
	}

}

/////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////

class MyDisplaySurface extends DisplaySurface {
  private GUIModel guimodel;
  // type is the type of graph that is on this display surface.

  int type;

  public void updateDisplay() {
    guimodel.DMSG( 4, "UpdateDisplay() called for type " + type );
    if(type == 0) {
      super.updateDisplay();
    }
  }

  public void updateDisplayDirect()  {
    guimodel.DMSG( 4, "UpdateDisplayDirect() called for type " + type );
    if(type == 0) {
      super.updateDisplayDirect();
    }
  }

  public void simEventPerformed(SimEvent evt) {
    guimodel.DMSG( 4, "simEventPerformed(evt) called for type " + type );
    if(type == 0) {
      super.simEventPerformed(evt);
    }
  }

  public void paint(Graphics g) {
    if(type == 0) {
      super.paint(g);
    }
  }

  public MyDisplaySurface(int in_type, GUIModel in_GUIModel, String name) {
    super(in_GUIModel, name);
    guimodel = in_GUIModel;
    type = in_type;
    
    //removeMouseListener(getMouseListeners()[0]);
    //removeMouseMotionListener(getMouseMotionListeners()[0]);
    //addMouseListener(myDSMouseAdapter);
    //addMouseMotionListener(myDSMouseAdapter); 
  }

}
 
///////////////////////////////////////////////////////////////////////////////
class MyNetwork2DDisplay extends Network2DDisplay {
  private GUIModel guimodel;

  public MyNetwork2DDisplay(GUIModel in_GUIModel, GraphLayout layout) {
    super(layout);
    guimodel = in_GUIModel;
  }

  public void setMoveableXY(Moveable moveable, int x, int y) {
    moveable.setX(x);
    moveable.setY(y);
    //guimodel.updateGraphs();
  }
}
