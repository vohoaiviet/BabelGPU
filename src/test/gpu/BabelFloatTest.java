package test.gpu;

import gpu.*;
import utils.*;
import static utils.CpuUtil.*;

public class BabelFloatTest
{
	private static final float TOL = 1e-5f;
	private static final int ITER = 10000;
	
	public static void main(String[] args)
	{
		GpuUtil.enableExceptions();
        
		PP.p("Float CPU-GPU-Matlab test");
		PP.setPrecision(3);
		
		/*
		 * Dimensions
		 */
		CsvReader csv = new CsvReader("input_dim.txt");
		int[] dims = csv.readIntVec(true);
		final int SAMPLES = dims[0];
		final int X_DIM = dims[1];
		final int X_NEW_DIM = dims[2];
		final int LABELS = dims[3];
		
		// Read in dummy data
		csv = new CsvReader("input_X.txt");
		float[][] jX = csv.readFloatMat();
		FloatMat X = new FloatMat(jX);
		csv = new CsvReader("input_W.txt");
		
		float[][] jW = csv.readFloatMat();
		FloatMat W = new FloatMat(jW);
		csv = new CsvReader("input_Y.txt");
		int[] Y = csv.readIntVec(true);
		
		/*
		 * Define a few learning constants
		 */
		csv = new CsvReader("input_learn.txt");
		float[] learns = csv.readFloatVec(true);
		final float LearningRate = learns[0];
		final float Lambda = learns[1];
		
		GpuBlas.init();

		/*
		 * Step 1: cos(W * x + b)
		 * W and b are combined. 
		 */
		// augment X with a column of 1
		FloatMat X1 = new FloatMat(SAMPLES, X_DIM + 1, false);
		X1.copyFrom(X);
		Natives.gpu_fill(X1.getThrustPointer().offset(X.size()), SAMPLES, 1);
		float[][] jX1 = addCol1(jX);
		
		// Xnew: X_NEW_DIM * SAMPLES
		FloatMat Xnew = GpuBlas.mult(W, X1.transpose()).cos();
		float[][] jXnew = cos(mult(jW, transpose(jX1)));
//		PP.p("check Xnew"); 
//		checkGold(Xnew, jXnew);
//		checkGold(jXnew, "Xnew");
//		checkGold(Xnew, "Xnew");

		/*
		 * Step2: Create Theta matrix and compute Theta * X_new
		 */
		FloatMat Theta = new FloatMat(LABELS, X_NEW_DIM);
		float[][] jTheta = new float[LABELS][X_NEW_DIM];

		FloatMat A = new FloatMat(LABELS, 1, false);
		float[][] jA = new float[LABELS][1];
		// Loop over samples column by column
		for (int s = 0; s < ITER; ++ s)
		{
			// Step2: extract a column
			FloatMat Xnew_s = Xnew.createOffset(s * X_NEW_DIM, X_NEW_DIM);
			float[][] jXnew_s = getCol(jXnew, s);
			

			// alpha_vector = Theta * Xnew_s, LABELS * 1
			GpuBlas.mult(Theta, Xnew_s, A);
			jA = mult(jTheta, jXnew_s);
			
			/*
			 * Step3: get Id[y==j] - P(yj | x, Theta)
			 */
			Thrust.batch_softmax_minus_id(A, Thrust.copy_host_to_device(new int[] {Y[s]}), false);
			A.linear(-1, 0);
			jbabel_id_minus_softmax(jA, Y[s]);
			
			// Step3: update Theta
			// Theta += Lr * ( (Id-P) * Xnew_s' - Lambda/SAMPLES * Theta)
			GpuBlas.mult(A, Xnew_s.transpose(), Theta, 
					LearningRate, 1 - LearningRate * Lambda / SAMPLES);
			
			updateTheta(jTheta, - LearningRate * Lambda / SAMPLES, mult(jA, transpose(jXnew_s)), LearningRate);

//		PP.p("Iteration", s);
//		checkGold(A, jA);
//		checkGold(Theta, jTheta);
		}
		PP.p("Check vector A");
//		PP.p(A.sort().getHost());
//		float[] jA_ = transpose(jA)[0];  Arrays.sort(jA_);
//		PP.po(jA_);
		PP.p(A);
		PP.po(jA);
		checkGold(A, jA);
		checkGold(jA, "A");
		checkGold(A, "A");

		/*
		 * DONE!
		 * Check results against plain Java
		 */
		PP.p("Done. Check Theta:");
		checkGold(Theta, jTheta);
		checkGold(jTheta, "Theta");
		checkGold(Theta, "Theta");
		
		/*
		 * Clean up and exit
		 */
		FloatMat[] mats = new FloatMat[] 
				{X, W, X1, Xnew, Theta};
		for (FloatMat mat : mats)
			mat.destroy();
		GpuBlas.destroy();
	}
	
	/**
	 * Check the gold standard from plain Java
	 */
	private static void checkGold(FloatMat gpu, float[][] cpu)
	{
		float[][] Host = gpu.deflatten();
		
		float diff = matAvgDiff(cpu, Host);
		PP.setPrecision(3);
		PP.setScientific(true);
		
		if (matAvgDiff(cpu, Host) < TOL)
    		PP.p("PASS float GPU-CPU: ", diff);
		else
			PP.p("FAIL float GPU-CPU: ", diff);
	}
	
	/**
	 * Check the gold standard generated from Matlab
	 */
	private static void checkGold(float[][] cpu, String goldFile)
	{
		CsvReader csv = new CsvReader("gold_" + goldFile + ".txt");
		float[][] Gold = csv.readFloatMat();
		
		float diff = matAvgDiff(Gold, cpu);
		PP.setPrecision(3);
		PP.setScientific(true);
		
		if (matAvgDiff(Gold, cpu) < TOL)
    		PP.p("PASS float CPU-Matlab: ", diff);
		else
			PP.p("FAIL float CPU-Matlab: ", diff);
	}
	
	/**
	 * Check the gold standard generated from Matlab
	 */
	private static void checkGold(FloatMat gpu, String goldFile)
	{
//		GpuUtil.checkGold(gpu, "gold_" + goldFile, "GPU-Matlab", TOL);
	}

	private static void updateTheta(float[][] theta, float alpha, float[][] b, float beta)
	{
		int r = theta.length;
		int c = theta[0].length;
		
		for (int i = 0; i < r; i++)
			for (int j = 0; j < c; j++)
				theta[i][j] += theta[i][j] * alpha + b[i][j] * beta;
	}
	
	private static void jbabel_id_minus_softmax(float[][] a, int id)
	{
		int r = a.length;
		float max = -Float.MAX_VALUE;
		for (int i = 0; i < r; i++)
			if (a[i][0] > max)
				max = a[i][0];
		
		for (int i = 0; i < r; i++)
			a[i][0] = (float) Math.exp( a[i][0] - max );
		
		float sum = 0;
		for (int i = 0; i < r; i++)
			sum += a[i][0];
		
		for (int i = 0; i < r; i++)
			a[i][0] *= -1.0f/sum;
		
		++ a[id][0];
	}
}
