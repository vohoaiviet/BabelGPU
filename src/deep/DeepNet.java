package deep;

import gpu.*;

import java.io.Serializable;
import java.util.*;

import utils.*;
import utils.MiscUtil.ManagedIterator;
import deep.units.*;

public class DeepNet implements Iterable<ComputeUnit>, Serializable
{
	private static final long serialVersionUID = 1L;
	public String name;
	public ComputeUnit head;
	public transient InletUnit inlet;
	public TerminalUnit terminal;
	public LearningPlan learningPlan;
	// Called at the end of every epoch to save stuff
	// User defined. Default save nothing. 
	public EpochSaver epochSaver = DummySaver;
	
	private transient boolean setup = false; // should only setup once
	private boolean debug = false; // the whole net is in debug mode

	// Parameter list in forward order: for updating regularization term
	public ParamList paramList = null;
	// Best parameter data so far
	public ParamList bestParamList = null;
	// From last epoch
	public ParamList lastEpochParamList = null;

	public DeepNet(String name, InletUnit inlet, ComputeUnit... units)
	{
		this.name = name;
		this.head = units[0];
		this.terminal = (TerminalUnit) units[units.length - 1];
		this.inlet = inlet;
		head.input = this.inlet;
		this.inlet.setParent(head);
		chain(units);
	}
	
	public DeepNet(String name, InletUnit inlet, ArrayList<ComputeUnit> units)
	{
		this(name, inlet, MiscUtil.toArray(units, ComputeUnit.class));
	}
	
	/**
	 * Link an Inlet with head ComputeUnit. 
	 * Useful for deserialization
	 */
	public void linkInlet(InletUnit inlet)
	{
		this.inlet = inlet;
		head.input = this.inlet;
		this.inlet.setParent(head);
		for (ComputeUnit unit : this)
			unit.inlet = inlet;
	}

	/**
	 * Ctor helper
	 * Set 'prev' and 'next' links. 
	 */
	public static void chain(ComputeUnit... units)
	{
		int len = units.length;
		for (int i = 0; i < len; i++)
		{
			if (i != 0) units[i].prev = units[i - 1];
			if (i != len - 1) units[i].next = units[i + 1];
		}
	}

	//**************************************************/
	//******************* Main training entry *******************/
	//**************************************************/
	public void forwprop()
	{
		for (ComputeUnit unit : this)
			unit.forward();
	}

	public void backprop()
	{
		for (ComputeUnit unit : unitIter(false))
			unit.backward();
	}

	/**
	 * Iterate over epochs. 
	 * At the end of every epoch, {@link #prepareNextEpoch()}
	 * and {@link #epochSaver}
	 * @return current epoch index
	 */
	public Iterable<Integer> epochIter()
	{
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator()
			{
				return new ManagedIterator<Integer>()
				{
					LearningPlan plan = DeepNet.this.learningPlan;
					@Override
					public boolean hasNext_()
					{
						return plan.curEpoch < plan.totalEpochs;
					}
					@Override
					public Integer next()
					{
						return plan.curEpoch;
					}
					@Override
					public void trailer()
					{
						DeepNet.this.prepareNextEpoch();
						++ plan.curEpoch;
						// Saves whatever necessary
						DeepNet.this.epochSaver.save(DeepNet.this);
					}
				};
			}
		};
	}
	
	/**
	 * Iterate over mini-batches within one epoch until totalSampleSize is exhausted
	 * @return learningPlan.doneSampleSize right after nextBatch()
	 */
	public Iterable<Integer> batchIter()
	{
		return new Iterable<Integer>() {
			@Override
			public Iterator<Integer> iterator()
			{		
				return new Iterator<Integer>()
				{
					LearningPlan plan = DeepNet.this.learningPlan;
					@Override
					public boolean hasNext()
					{
						return plan.doneSampleSize < plan.totalSampleSize;
					}
					@Override
					public Integer next()
					{
						inlet.nextBatch();
						learningPlan.lrScheme.updateBatch();
						return plan.doneSampleSize;
					}
					@Override
					public void remove() { }
				};
			}
		};
	}

	/**
	 * Setup a learning plan and run
	 * @see #run()
	 */
	public void run(LearningPlan learningPlan)
	{
		this.setup(learningPlan);
		this.run();
	}
	/**
	 * Assume setup() is already called
	 */
	@SuppressWarnings("unused")
	public void run()
	{
		for (int epoch : this.epochIter())
			for (int doneSample : this.batchIter())
			{
				forwprop();
				backprop();
			}
	}

	/**
	 * Reset LearningPlan, inlet and loss
	 * Should be used in real training loop
	 */
	public void prepareNextEpoch()
	{ 
		learningPlan.prepareNextEpoch();
		inlet.prepareNextEpoch();
		this.clearLoss();
	}

	/**
	 * Prepare a network for a complete re-run
	 * Reset all parameters, loss, inlet and LearningPlan
	 * Mostly for debugging purpose
	 * @see LearningPlan#reset()
	 * @see InletUnit#prepareNextEpoch()
	 * @see #clearLoss()
	 */
	public void reset()
	{
		Initializer.resetRand();
		for (ParamUnit w : this.getParamList())
			w.reInit();
		learningPlan.reset();
		inlet.reset();
		this.clearLoss();
	}
	
	// ******************** Training setups ********************/
	/**
	 * This function call will only setup once and do nothing later
	 * Any non-ParamComputeUnit before the first ParamComputeUnit doesn't need to calculate gradient
	 * We explicitly disable it.
	 */
	public void setup(LearningPlan learningPlan)
	{
		if (!setup)
		{
			setPlan(learningPlan);

			for (ComputeUnit unit : this)
    			unit.setup();
			
			// Explicitly disable gradient calculation in the first few non-paramComputeUnit
			if (!debug)
    			for (ComputeUnit unit : this)
    			{
    				// SetNoGradient for all non-paramComputeUnits and the first paramComputeUnit
    				unit.input.setNoGradient();
    				if (unit instanceof ParamComputeUnit)
    					break;
    			}

			getParamList(); // refresh param-list
			setup = true;
		}
	}

	/**
	 * Must be called after setup()
	 * @param learningPlan applies to all ComputeUnits and DataUnits
	 */
	public void setPlan(LearningPlan learningPlan)
	{
		this.learningPlan = learningPlan;
		this.learningPlan.net = this;
		for (ComputeUnit unit : this)
			unit.setPlan(learningPlan);
	}

	/**
	 * Set one initializer for all ParamComputeUnit 
	 */
	public void setInitializer(Initializer initer)
	{
		for (ComputeUnit unit : this)
			if (unit instanceof ParamComputeUnit)
				((ParamComputeUnit) unit).initer = initer;
	}

	/**
	 * Must be called BEFORE setup() !!
	 */
	public void setBias(boolean hasBias)
	{
		for (ComputeUnit unit : this)
			unit.setBias(hasBias);
	}
	
	/**
	 * Fill all compute units with default generated name
	 * @return this
	 */
	public DeepNet genDefaultUnitName()
	{
		HashMap<String, Integer> map = new HashMap<>();
		String className;
		for (ComputeUnit unit : this)
		{
			className = unit.getClass().getSimpleName();
			// className always ends with 'Unit'
			className = className.substring(0, className.length() - 4);
			Integer idx = map.get(className);
			if (idx == null)
			{
				map.put(className, 1);
				idx = 1;
			}
			else // use the last index + 1
				map.put(className, ++ idx);
			unit.name = String.format("%s{%d}", className, idx);
		}
		return this;
	}

	/**
	 * @return a new HashMap that maps name to ComputeUnit
	 */
	public HashMap<String, ComputeUnit> getUnitMap()
	{
		HashMap<String, ComputeUnit> unitMap = new HashMap<>();
		for (ComputeUnit unit : this)
			unitMap.put(unit.name, unit);
		return unitMap;
	}
	
	/**
	 * @return compute unit list
	 */
	public ArrayList<ComputeUnit> getUnitList()
	{
		ArrayList<ComputeUnit> unitList = new ArrayList<>();
		for (ComputeUnit unit : this)
			unitList.add(unit);
		return unitList;
	}
	
	// ******************** Serialization ********************/
	/**
	 * Default: save nothing
	 */
	public void setUnitOutputSaveMode(int saveMode)
	{
		for (ComputeUnit unit : this)
			unit.setOutputSaveMode(saveMode);
	}

	/**
	 * Default: save only 'data'
	 */
	public void setParamSaveMode(int saveMode)
	{
		for (ComputeUnit unit : this)
			if (unit instanceof ParamComputeUnit)
				((ParamComputeUnit) unit).setParamSaveMode(saveMode);
	}
	
	/**
	 * Called at the end of each epoch to save whatever necessary
	 * @see DeepNet#epochIter()
	 */
	public static abstract class EpochSaver implements Serializable
	{
		private static final long serialVersionUID = 1L;
		public abstract void save(DeepNet net);
	}
	
	/**
	 * Saves nothing
	 */
	public static final EpochSaver DummySaver = 
			new EpochSaver()
			{
				private static final long serialVersionUID = 1L;
				@Override
				public void save(DeepNet net) { }
			};
	
	/**
	 * Default: save nothing
	 * NOTE: for Java serialization to work, MUST declare an explicit static subclass 
	 * that extends EpochSaver. DO NOT use anonoymous class, because it will 
	 * implicitly store a reference to your own enclosing class.
	 */
	public void setEpochSaver(EpochSaver saver)
	{
		this.epochSaver = saver;
	}

	// ******************** ParamList management ********************/
	/**
	 * set this.paramList
	 * @return all ParamUnit from all ParamComputeUnits, in forward order
	 */
	public ParamList getParamList()
	{
		// Make sure we get the latest list of params
		// a non-empty paramList with null entries means the parameters aren't set yet
		if (paramList != null 
				&& paramList.size() != 0 
				&& paramList.get(0) != null)
			return paramList;
		
		return this.paramList = new ParamList(this);
	}
	
	public void clearParamLists()
	{
		this.paramList = null;
		this.bestParamList = null;
		this.lastEpochParamList = null;
	}
	
	/**
	 * Copy the current params to bestParamList if this is indeed the best epoch. 
	 * Best param units are deep copies of paramList, and their names suffixed by '_best'
	 * @see LearningPlan#prepareNextEpoch()
	 */
	public void recordBestParams()
	{
		if (bestParamList == null)
			bestParamList = new ParamList(getParamList(), "_best");
		else
			bestParamList.copyDataFrom(paramList);
	}
	
	/**
	 * Restore this.paramList from the best
	 */
	public void restoreBestParams()
	{
		this.paramList.copyDataFrom(bestParamList);
	}
	
	/**
	 * Copy the current params to bestParamList if this is indeed the best epoch. 
	 * Best param units are deep copies of paramList, and their names suffixed by '_best'
	 * @see LearningPlan#prepareNextEpoch()
	 */
	public void recordLastEpochParams()
	{
		if (lastEpochParamList == null)
			lastEpochParamList = new ParamList(getParamList(), "_last");
		else
		// Copies data over
			lastEpochParamList.copyDataFrom(paramList);
	}
	
	/**
	 * Restore this.paramList from the previous epoch
	 */
	public void restoreLastEpochParams()
	{
		this.paramList.copyDataFrom(lastEpochParamList);
	}

	// ******************** Deals with loss function and Terminal ********************/
	/**
	 * @see TerminalUnit#clearLoss()
	 */
	public void clearLoss()
	{
		this.terminal.clearLoss();
	}
	
	/**
	 * @return total loss function value = pure + reg
	 */
	public float lossTotal()
	{
		return this.terminal.lossTotal();
	}

	/**
	 * @return pure loss function value is without reg
	 */
	public float lossPure()
	{
		return this.terminal.lossPure();
	}
	
	/**
	 * Sometimes in training, we only care about the gradient updates, 
	 * not the actual loss value. 
	 * Turn off the loss calculation might save a lot of time.
	 */
	public void setCalcLoss(boolean doesCalcLoss)
	{
		this.terminal.setCalcLoss(doesCalcLoss);
	}
	
	/**
	 * Are we calculating loss in Terminal?
	 */
	public boolean doesCalcLoss() {	return this.terminal.doesCalcLoss;	}
	
	// ******************** Enable forward/backward iteration ********************/
	/**
	 * Iterate over all ComputeUnits in forward or backward order
	 */
	public Iterable<ComputeUnit> unitIter(final boolean forward)
	{
		return new Iterable<ComputeUnit>()
		{
			public Iterator<ComputeUnit> iterator()
			{
				return new Iterator<ComputeUnit>()
				{
					ComputeUnit unitptr;
					{
						unitptr = forward ? 
								DeepNet.this.head : DeepNet.this.terminal;
					}

					@Override
					public boolean hasNext() { return unitptr != null; }

					ComputeUnit tmpptr;
					@Override
					public ComputeUnit next()
					{
						tmpptr = unitptr;
						unitptr = forward ? unitptr.next : unitptr.prev;
						return tmpptr;
					}

					public void remove() { }
				};
			}
		};
	}

	/**
	 * Iterate over ComputeUnits in forward order
	 */
	@Override
	public Iterator<ComputeUnit> iterator()
	{
		return this.unitIter(true).iterator();
	}
	
	/**
	 * If debug mode enabled, we explicitly store the parameter gradient
	 */
	public void enableDebug(boolean debug)
	{
		this.debug = debug;
		for (ComputeUnit unit : this)
			unit.enableDebug(debug);
	}

	public void enableDebug() {	this.enableDebug(true); }

	//**************************************************/
	//******************* DEBUG only *******************/
	//**************************************************/
	public void runDebug(LearningPlan learningPlan, boolean hasBias)
	{
	 	this.enableDebug();
	 	this.setBias(hasBias); // all have bias units
	 	
		PP.pTitledSectionLine("RUN DEBUG", "=", 25);
	 	PP.pTitledSectionLine("SETUP");
	 	this.setup(learningPlan);
	 	this.printDebug(true);
		
		// Handle debug networks that have no params
		if (this.getParamList().size() == 0)
			inlet.initGradient();

		int i = 1;
		for (int doneSample : this.batchIter())
		{
			PP.pSectionLine("-", 70);
			PP.p("Iteration", i++, "reading inlet");
			PP.pTitledSectionLine("FORWARD");
			forwprop();
			printDebug(true);
			PP.pTitledSectionLine("BACKWARD");
			backprop();
			printDebug(false);
		}
		
		PP.p("\nRESULT =", terminal.lossTotal(), "\n");
	}
	
	public void printDebug(boolean forward)
	{
		for (ComputeUnit unit : this.unitIter(forward))
		{
			PP.p(unit.name);
			PP.p("input", unit.input.data().row, "*", unit.input.data().col, ":", unit.input, "\n");
			if (unit instanceof ParamComputeUnit)
			{
				ParamUnit W = ((ParamComputeUnit) unit).W;
				PP.p("W", W.data().row, "*", W.data().col, ":", W);
			}
			PP.pSectionLine();
		}
	}
	
	/**
	 * Gradient checking debug routine
	 * Supports both networks with and without parameters (e.g. pure compute layers)
	 * Will only process one batch from inlet
	 * Perturbation eps constant is auto-selected based on avg abs value of parameters (minimum: 1e-4f)
	 * @param hasBias enable bias units on all ComputeUnit
	 * @param perturbRatio default = 1e3f. perturb EPS = average_abs_val / perturbRatio
	 * @param verbose don't show the actual gradient comparison
	 * @return average percentage error (already multiplied by 100)
	 */
	public float gradCheck(LearningPlan learningPlan, boolean hasBias, float perturbRatio, boolean verbose)
	{
		PP.pTitledSectionLine("GRAD CHECK: " + this.name, "=", 10);
		
	 	this.enableDebug();
	 	this.setBias(hasBias); // all have bias units
	 	
		this.setup(learningPlan);
		this.reset(); inlet.nextBatch();
		
		ArrayList<ParamUnit> params = (ArrayList<ParamUnit>) this.getParamList().clone();
		// We also do gradient checking for pure computing networks that have no parameters
		boolean hasParams = params.size() != 0;
		
		int gradN = hasParams ? params.size() : 1;
		FloatMat propGrad[] = new FloatMat[gradN];
		FloatMat goldGrad[] = new FloatMat[gradN];
		
		if (!hasParams)
		{
			// add sentinel value: hack the loops below
			params.add(null); 
            // we treat inlet as 'parameter' and compute its finite-difference grad
			inlet.initGradient(); 
		}

		// Get the exact gradient by backprop first
		forwprop();
		backprop();
		
		FloatMat mat;
		
		// hack: can be a ParamUnit or an InletUnit, 
		// depend on whether this network is a pure compute net or not
		DataUnit w; 
		
		int totalSize = 0;
		float totalGradAbsSum = 0, totalDataAbsSum = 0;
		int i = 0;
		for (ParamUnit param : params)
		{
            // if doesn't have any param, 'params' will only have 1 null, iterate once and exit this loop
			w = hasParams ? param : inlet;
			
			mat = new FloatMat(w.data());
			mat.copyFrom(w.data());
			totalDataAbsSum += mat.abs_sum(); // mat is mutated

			mat.copyFrom(w.gradient());
			totalSize += mat.size() - (hasBias ? mat.row : 0);
			totalGradAbsSum += mat.abs_sum();
			propGrad[i ++] = mat;
		}
		
		// Heuristic to pick a good EPS
		final float EPS = Math.max( totalDataAbsSum / totalSize / perturbRatio, 1e-3f);
		
		// Do finite-diff forward prop for every entry in every parameter
		i = 0;
		for (ParamUnit param : params)
		{
			w = hasParams ? param : inlet;
			mat = new FloatMat(w.data());
					
			for (int idx = 0 ; idx < mat.size(); idx ++)
			{
				// Skip checking the last row if include bias units
				if (hasBias && mat.toCoord(idx).i == mat.row - 1)
					continue;

				// +EPS and -EPS perturb
				float negResult = 0, posResult = 0;
				for (int perturb : new int[] {-1, 1})
				{
    				// Re-init everything as the exact gradient initialization
					this.reset(); inlet.nextBatch();
    				
            		// Perturb -EPS
            		w.data().incrSingle(idx, perturb * EPS);
            		forwprop();
            		float result = terminal.lossTotal();

            		if (perturb < 0) negResult = result; else posResult = result;
				}
				// Compute symmetric numerical gradient and store to 'mat'
				mat.setSingle(idx, (posResult - negResult) / (2 * EPS));
			}
			// Store
			goldGrad[i ++] = mat;
		}
		
		if (verbose)
		{
    		PP.setSep("\n\n");
    		PP.pTitledSectionLine("Back-Prop");
    		PP.p(propGrad);
    		PP.p();
    		PP.pTitledSectionLine("Numerical Gold");
    		PP.p(goldGrad);
    		PP.p();
		}
		PP.pTitledSectionLine("Error Report", "-", 10);
        PP.setSep();
		PP.setPrecision(2); PP.setScientific(true);
		PP.p("\nPerturb EPS = ", EPS);
		// Compare difference
		float absErr = 0;
		for (i = 0 ; i < propGrad.length; i ++)
			absErr += GpuBlas.add(propGrad[i], goldGrad[i], 1, -1).abs_sum();
		float avgAbsErr = absErr / totalSize;
		
		float avgAbsVal = (hasParams ? totalGradAbsSum : totalDataAbsSum) / totalSize;
		float avgPercentErr = avgAbsErr / avgAbsVal * 100;
		
		PP.p("Average absolute error =", avgAbsErr);
		PP.p("Average percent error =", avgPercentErr, "%\n");
		PP.setPrecision(); PP.setScientific(false); // reset printer options
		
		return avgPercentErr;
	}
	
	/**
	 * Default verbose = false
	 */
	public float gradCheck(LearningPlan plan, boolean hasBias, float perturbRatio)
	{
		return this.gradCheck(plan, hasBias, perturbRatio, false);
	}
}