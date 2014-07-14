package test.deep;

import static org.junit.Assert.*;
import java.util.Arrays;
import gpu.*;
import utils.*;
import deep.*;
import deep.units.*;

/**
 * Helps configure other unit tests
 * All settings will be global here through the test/deep package
 */
public class DeepTestKit
{
	// ******************** CONFIG ********************/
	// Input feature vector dimension
	public static int inDim = 9;
	// Number of training samples
	public static int batchSize = 5;
	// Output vector dimension
	public static int outDim = 9;
	// Test option: add bias?
	public static boolean hasBias = true;
	// Scale ElementComputeUnit output
	public static float scalor = 3f;
	// Regularization
	public static float reg = 1.5f;
	public static LearningPlan plan = new LearningPlan(2, reg, 0, batchSize);
	
	public static void systemInit()
	{
		GpuBlas.init();
		GpuUtil.enableExceptions();
	}
	
	public static int changeDim(int dim)
	{
		return hasBias ? dim + 1 : dim;
	}

	static float[][] dummyInput, dummyGold;
	/**
	 * CrossEntropyUnit requires that the sum of each column of goldMat must be 1
	 */
	public static InletUnit uniRandInlet(float inputLow, float inputHigh, float goldLow, float goldHigh, final boolean sumTo1)
	{
		// inDim and outDim + 1 because of bias units
		dummyInput = 
				CpuUtil.randFloatMat(inDim + 1, batchSize, inputLow, inputHigh);

		dummyGold = 
				CpuUtil.randFloatMat(outDim + 1, batchSize, goldLow, goldHigh);
		
		return new InletUnit("Dummy Inlet", changeDim(inDim) , batchSize)
		{
			boolean hasNext = true;
			{
				this.goldMat = new FloatMat(changeDim(outDim), batchSize);
			}
			@Override
			public void nextGold()
			{
				this.goldMat.setHostArray(dummyGold);
				this.goldMat.toDevice(true);
				if (hasBias) this.goldMat.fillLastRow0();

				if (sumTo1) // normalize col sum to 1 for CrossEntropyUnit
					for (int c = 0; c < goldMat.col; c++)
					{
						FloatMat colMat = goldMat.createColOffset(c, c+1);
						GpuBlas.scale(colMat, 1f / colMat.sum());
					}
			}
			@Override
			public void nextBatch()
			{
				this.data.setHostArray(dummyInput);
				this.data.toDevice(true);
				if (hasBias) this.data.fillLastRow1();
				hasNext = false;
			}
			@Override
			public boolean hasNext() { return hasNext; }
			@Override
			public void reset() { hasNext = true; }
		};
	}
	
	/**
	 * inputLow, High = +/- inputSymm
	 * goldLow, High = +/- goldSymm
	 * CrossEntropyUnit requires that the sum of each column of goldMat must be 1
	 */
	public static InletUnit uniRandInlet(float inputSymm, float goldSymm, boolean sumTo1)
	{
		return uniRandInlet(-inputSymm, inputSymm, -goldSymm, goldSymm, sumTo1);
	}
	// default sumTo1 = false
	public static InletUnit uniRandInlet(float inputLow, float inputHigh, float goldLow, float goldHigh)
	{
		return uniRandInlet(inputLow, inputHigh, goldLow, goldHigh, false);
	}
	// default sumTo1 = false
	public static InletUnit uniRandInlet(float inputSymm, float goldSymm)
	{
		return uniRandInlet(-inputSymm, inputSymm, -goldSymm, goldSymm, false);
	}
	
	// Produce an artificial Inlet with uniform input and gold
	public static InletUnit uniformInlet(final float inputVal, final float goldVal)
	{
		return new InletUnit("Uniform Inlet", changeDim(inDim), batchSize)
		{
			boolean hasNext = true;
			{
				this.goldMat = new FloatMat(changeDim(outDim), batchSize);
			}
			@Override
			public void nextGold()
			{
				this.goldMat.fill(goldVal);
				if (hasBias) this.goldMat.fillLastRow0();
			}
			@Override
			public void nextBatch()
			{
				this.data.fill(inputVal);
				if (hasBias) this.data.fillLastRow1();
				hasNext = false;
			}
			@Override
			public boolean hasNext() { return hasNext; }
			@Override
			public void reset() { hasNext = true; }
		};
	}
	
	/**
	 * @param TOL within tolerance percentage (already multiplied by 100)
	 * @param perturbRatio @see DeepNet#gradCheck()
	 * When debugging PureComputeLayers, make sure inDim == outDim
	 */
	public static void check(DeepNet net, double TOL, float perturbRatio, boolean verbose)
	{
		if (net.getParams().size() == 0// this is a PureCompute debug network 
			// the terminal class requires inDim == outDim
			&& Arrays.asList( new Class[] {SquareErrorUnit.class, CrossEntropyUnit.class}).contains(net.terminal.getClass())
			&& inDim != outDim)
		{
			fail("PureComputeLayer debug test must have inDim == outDim");
		}
		
		float avgPercentErr = net.gradCheck(plan, hasBias, perturbRatio, verbose);
		assertTrue(net.name + " grad check", CpuUtil.equal(avgPercentErr, 0, TOL));
	}
	public static void check(DeepNet net, double TOL, float perturbRatio) {	check(net, TOL, perturbRatio, false);	}
	public static void check(DeepNet net, double TOL, boolean verbose) {	check(net, TOL, 1e3f, verbose);	}
	public static void check(DeepNet net, double TOL) {	check(net, TOL, 1e3f, false);	}
}