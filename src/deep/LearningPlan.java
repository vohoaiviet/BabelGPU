package deep;

import java.util.ArrayList;

public class LearningPlan
{
	/*
	 * Permanent section
	 */
	public String name;
	public DeepNet net;
	public String dir; // Path to directory to store everything
	public float lrStart; // Start learning rate
	public float reg; // Regularization
	public int totalSampleSize;
	public int totalEpochs;
	public LrScheme lrScheme;
	public RegScheme regScheme;
	
	/*
	 * Varying section
	 */
	public float lr; // current lr
	public int doneEpoch = 0;
	// number of samples already processed
	public int doneSampleSize = 0;
	// Records the performance (loss function value) from each epoch
	public ArrayList<Float> record = new ArrayList<>();
	
	public LearningPlan() {};
	
	public LearningPlan(String name, String dir, float lrStart, float reg, int totalSampleSize, int totalEpochs)
	{
		this.name = name;
		this.dir = dir;
		this.lrStart = lrStart;
		this.lr = lrStart;
		this.reg = reg;
		this.totalSampleSize = totalSampleSize;
		this.totalEpochs = totalEpochs;

		// Default schemes
		this.setLrScheme(LrScheme.dummyScheme());
		this.setRegScheme(RegScheme.squareSumScheme());
	}
	
	/**
	 * Copy ctor
	 * 'name' won't be copied
	 * Varying states won't copied
	 */
	public LearningPlan(LearningPlan other)
	{
		this.dir = other.dir;
		this.lrStart = other.lrStart;
		this.reg = other.reg;
		this.totalSampleSize = other.totalSampleSize;
		this.totalEpochs = other.totalEpochs;
		
		this.setLrScheme(other.lrScheme);
		this.setRegScheme(other.regScheme);
	}
	
	/**
	 * Prepare for the next epoch. 
	 * Update 'record' with latest performance measure. 
	 */
	public void prepareNextEpoch()
	{ 
		if (net.doesCalcLoss())
    		record.add(net.lossPure());
		this.doneSampleSize = 0; 
	}
	
	/**
	 * Reset for a complete re-run
	 */
	public void reset()
	{
		this.doneEpoch = 0;
		this.doneSampleSize = 0;
		this.lr = lrStart;
		this.record.clear();
	}
	
	/**
	 * How many samples left?
	 */
	public int remainTrainSize()
	{
		return this.totalSampleSize - this.doneSampleSize;
	}
	
	/**
	 * Do we use regularization?
	 */
	public boolean hasReg() { return this.reg > 0; }
	
	// ******************** Schemes ********************/
	/**
	 * NOTE: do not set the public field directly!!!
	 */
	public void setRegScheme(RegScheme scheme)
	{
		this.regScheme = scheme;
		this.linkScheme(scheme);
	}

	/**
	 * NOTE: do not set the public field directly!!!
	 */
	public void setLrScheme(LrScheme scheme)
	{
		this.lrScheme = scheme;
		this.linkScheme(scheme);
	}
	
	private void linkScheme(Scheme scheme)
	{
		scheme.linkPlan(this);
	}
	
	/**
	 * A scheme (learning rate scheme, regularization scheme) 
	 * should be linked to a LearningPlan
	 */
	static abstract class Scheme
	{
		protected LearningPlan plan;
		public void linkPlan(LearningPlan plan)
		{
			this.plan = plan;
		}
	}
	
	@Override
	public String toString()
	{
		return "LearningPlan [name=" + name + ", \ndir=" + dir + ", \nlr=" + lrStart
				+ ", \nreg=" + reg + ", \ntotalSampleSize=" + totalSampleSize
				+ ", \ntotalEpochs=" + totalEpochs + ", \ncurEpoch=" + doneEpoch
				+ ", \ndoneSampleSize=" + doneSampleSize + ", \nrecord="
				+ record + "]";
	}
}