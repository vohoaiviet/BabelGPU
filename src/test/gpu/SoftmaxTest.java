package test.gpu;

import static test.gpu.GpuTestKit.*;
import org.junit.*;
import utils.*;
import gpu.*;
import com.googlecode.javacpp.*;

public class SoftmaxTest
{
	private int ROW;
	private int COL;
	private static GpuTestKit kit;
	
	@BeforeClass
	public static void setUp()
	{ 
		systemInit();
		kit = new GpuTestKit("Softmax");
	}
	
	@Test
	public void test()
	{
		kit.printSessionTitle();
		
		int[] dims = kit.loadInts("input_dim");
		ROW = dims[0]; COL = dims[1];
		
		// X shouldn't be changed
		FloatMat X_backup = kit.loadFloatMat("input_X", ROW, COL);
		FloatMat X = X_backup.clone();
		
		IntPointer labelsDevice = kit.loadIntGpu("input_Labels");

		/*
		 * softmax(X) intrusive
		 */
		Thrust.batch_softmax(X, false);
		kit.checkGold(X, "gold_batch_softmax");

		/*
		 * softmax(X) non-intrusive
		 */
		X.copyFrom(X_backup);
		FloatMat X_out = new FloatMat(X);
		Thrust.batch_softmax(X, X_out, false);
		kit.checkGold(X_out, "gold_batch_softmax");
		kit.checkGold(X, X_backup, "batch_softmax: X shouldn't be changed");
		// hasBias
		Thrust.batch_softmax(X, X_out, true); X_out.fillLastRow0();
		kit.checkGold(X_out, "gold_batch_softmax_bias");
		
		/*
		 * Softmax(X) - id intrusive
		 */
		X.copyFrom(X_backup);
		Thrust.batch_softmax_minus_id(X, labelsDevice, false);
		kit.checkGold(X, "gold_batch_softmax_minus_id");

		/*
		 * Softmax(X) - id non-intrusive
		 */
		X.copyFrom(X_backup);
		Thrust.batch_softmax_minus_id(X, X_out, labelsDevice, false);
		kit.checkGold(X_out, "gold_batch_softmax_minus_id");
		kit.checkGold(X, X_backup, "softmax - id: X shouldn't be changed");
		// hasBias
		Thrust.batch_softmax_minus_id(X, X_out, labelsDevice, true); X_out.fillLastRow0();
		kit.checkGold(X_out, "gold_batch_softmax_minus_id_bias");

		/*
		 * softmax(X) return only the probability at the correct label of each column
		 * non-intrusive
		 */
		X.copyFrom(X_backup);
		FloatMat outLogProbs = new FloatMat(1, COL, false);
		Thrust.batch_softmax_at_label(X, outLogProbs, labelsDevice, false);
		kit.checkGold(outLogProbs, "gold_batch_softmax_at_label");
		kit.checkGold(X, X_backup, "softmax_at_label: X shouldn't be changed");
		// compute sum of log likelihood
		kit.checkGold(Thrust.sum(outLogProbs), "gold_log_prob", 5e-4f, "Sum of log probs");

		// hasBias version
		Thrust.batch_softmax_at_label(X, outLogProbs, labelsDevice, true);
		kit.checkGold(outLogProbs, "gold_batch_softmax_at_label_bias");
		// compute sum of log likelihood
		kit.checkGold(Thrust.sum(outLogProbs), "gold_log_prob_bias", 5e-4f, "Sum of log probs with bias");
		
		/*
		 * Label where the maximum probability occurs
		 */
		X.copyFrom(X_backup);
		IntPointer reusedPtr = Thrust.malloc_device_int(COL);
		final int dummyOffset = 766;
		int outLabels[] = new int[COL + dummyOffset]; // 66 dummy offset
		Thrust.best_label(X, reusedPtr, outLabels, dummyOffset, false);
		int[] goldLabels = kit.loadInts("gold_best_labels");
		
		// checkGold
		int fault = -1; // if stays -1, then test passes
		for (int i = 0; i < COL; i ++)
			if (outLabels[i + dummyOffset] != goldLabels[i])
			{
				fault = i;
				break;
			}
		if (fault != -1)
			PP.p("[best label]: FAIL", fault);
		else
			PP.p("[best label]: PASS");
		kit.checkGold(X, X_backup, "best_labels: X shouldn't be changed");
	}
}
