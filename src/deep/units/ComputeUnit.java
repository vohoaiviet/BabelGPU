package deep.units;

import gpu.*;
import deep.*;

public abstract class ComputeUnit extends Unit
{
	public ComputeUnit next = null;
	public ComputeUnit prev = null;
	public int outDim;

	/**
	 * ALWAYS equal to prev.output
	 */
	public DataUnit input;
	/**
	 * ALWAYS equal to next.input
	 */
	public DataUnit output;
	
	public LearningPlan learningPlan;
	
	public ComputeUnit(String name)
	{
		super(name);
	}
	
	public ComputeUnit(String name, int newDim)
	{
		super(name);
		this.outDim = newDim;
	}
	
	public void setup()
	{
		if (prev != null)
    		this.input = prev.output;
		setupOutput();
	}
	
	protected void setupOutput()
	{
		this.output = new DataUnit("out[" + this.name + "]", new FloatMat(outDim, input.batchSize()));
		this.output.initGradient();
	}
	
	public abstract void forward(); 
	public abstract void backward();
}