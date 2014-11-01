package ARMSR;

/**
  BatchModel is a non-gui extension of base Model
**/

import java.io.*;
import java.util.*;
import java.awt.Color;
import javax.swing.JFrame;
// KeyListeners, MouseListeners etc.
import java.awt.event.*;

import uchicago.src.sim.engine.*;
import uchicago.src.sim.gui.*;    // for the ColorMap's
import uchicago.src.sim.space.*;
import uchicago.src.sim.network.*;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.reflector.ListPropertyDescriptor;
import uchicago.src.sim.util.Random;
import uchicago.src.sim.analysis.*;
import uchicago.src.collection.RangeMap;
import uchicago.src.sim.util.*;

import com.braju.beta.format.*; // for printf
import com.braju.beta.lang.*;
import rlriolo.ioutils.*;

public class BatchModel extends Model {


	///////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////
	// main entry point
	public static void main( String[] args ) {

		BatchModel model = new BatchModel();

		// set the type of model class, this is necessary
		// so the parameters object knows whether or not
		// to do GUI related updates of panels, etc when a
		// parameter is changed
		model.setModelType("BatchModel");

		model.setCommandLineArgs(args);

		PlainController control = new PlainController();
		model.setController(control);
		control.setExitOnExit(true);
		control.setModel(model);
		model.addSimEventListener(control);

//		Format.printf("\n==> BatchModel main...about to processCommandLinePars...\n");
//		model.processCommandLinePars ( args );

		Format.printf("\n==> BatchModel main...about to startSimulation...\n");
		control.startSimulation();
	}

	/////////////////////////////////////////////////////////////////////
	public void setup() {
		super.setup();
		// kludge
		// need to have schedule != null in order for 
		// control.startSimulation() call in main() to work
		// then in begin() the schedule needs to be set null
		// before buildModel ( because the way buildModel 
		// knows if it should record changes or not is if 
		// schedule != null, and we don't want the changes 
		// recorded during buildModel.
		schedule = new Schedule();
	}

	public void begin() {
		// set schedule to null so buildModel knows not to 
		// record changes ( changes are recorded if 
		// schedule != null ).  in buildSchedule() the 
		// schedule is allocated before the actual 
		// schedule is created.
		schedule = null;
		buildModel();     // the base model does this
		buildSchedule();
	}

	public void step() {
		// Call Model's step()
		super.step();
		if ( super.getRDebug() > 0 ) {
		Format.printf("\n---> end of BatchModel step %d\n",
					  p.add(getTickCount()) );
		}
	}

	////////////////////////////////////////////////////////////////
	// builds the schedule
	// 
	public void buildSchedule() {

		// call buildSchedule() function shared by the batch and GUI models
		super.buildSchedule();

		// schedule the current BatchModel's step() function
		// to execute every time step starting with time  step 0
		schedule.scheduleActionBeginning(0, this, "step");
		schedule.scheduleActionBeginning(0, this, "stepReport");

		// schedule the current BatchModel's processEndOfRun() 
		// function to execute at the end of the Batch Run.
		// You need to specify the time to schedule it (instead 
		// of doing scheduleActionAtEnd() or it will just run forever
		schedule.scheduleActionAt(getStopT(), this, "processEndOfRun");
	}

	public void processEndOfRun ( ) {
		//		super.processEndOfRun();
		this.fireEndSim();
	}

}

class PlainController extends BaseController {
	private boolean exitonexit;

	public PlainController() {
		super();
		exitonexit = false;
	}

	public void startSimulation() {
		startSim();
	}

	public void stopSimulation() {
		stopSim();
	}
	
	public void exitSim(){ exitSim(); }

	public void pauseSimulation() {
		pauseSim();
	}

	public boolean isBatch() {
		return true;
	}

	protected void onTickCountUpdate() {}

	// this might not be necessary
	public void setExitOnExit(boolean in_Exitonexit) {
		exitonexit = in_Exitonexit;
	}

	public void simEventPerformed(SimEvent evt) {
		if(evt.getId() == SimEvent.STOP_EVENT) {
			stopSimulation();
		}
		else if(evt.getId() == SimEvent.END_EVENT) {
			if(exitonexit) {
				System.exit(0);
			}
		}
		else if(evt.getId() == SimEvent.PAUSE_EVENT) {
			pauseSimulation();
		}
	}

}
