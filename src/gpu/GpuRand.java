package gpu;

import static jcuda.jcurand.JCurand.*;
import static jcuda.jcurand.curandRngType.*;

import java.util.Random;

import jcuda.jcurand.curandGenerator;

/**
 * Generates random floats on GPU
 */
public class GpuRand
{
	private curandGenerator generator;
	
	/**
	 * Ctor with seed
	 */
	public GpuRand(long seed)
	{
		createGenerator(seed);
	}
	
	private void createGenerator(long seed)
	{
		generator = new curandGenerator();
		curandCreateGenerator(generator, CURAND_RNG_PSEUDO_DEFAULT);
		curandSetPseudoRandomGeneratorSeed(generator, seed);
	}
	
	/**
	 * Ctor with random seed
	 */
	private static Random rand = new Random();
	public GpuRand()
	{
		this(rand.nextLong());
	}
	
	/**
	 * Re-initialize with a specified seed
	 * We actually destroy and reallocate the generator 
	 * because resetting seed doesn't ensure the same random sequence
	 */
	public void resetSeed(long seed)
	{
		destroy();
		createGenerator(seed);
	}

	/**
	 * Dtor
	 * Call after you're done with the random generator
	 */
	public void destroy()
	{
		if (generator != null)
    		curandDestroyGenerator(generator);
	}

	/**
	 * Fill a FloatMat with random uniform float
	 * @return input FloatMat A
	 */
	public FloatMat genUniformFloat(FloatMat A, double low, double high)
	{
		curandGenerateUniform(generator, A.toDevice(), A.size());
		A.linear((float)(high - low), (float)low);
		return A;
	}
	
	/**
	 * Fill a FloatMat with random uniform float
	 * @return input FloatMat A
	 */
	public FloatMat genUniformFloat(FloatMat A)
	{
		curandGenerateUniform(generator, A.toDevice(), A.size());
		return A;
	}

	/**
	 * Generate a new FloatMat with random uniform float
	 */
	public FloatMat genUniformFloat(int row, int col)
	{
		return genUniformFloat(new FloatMat(row, col, false));
	}
	
	/**
	 * Generate a new FloatMat with random uniform float
	 */
	public FloatMat genUniformFloat(int row, int col, double low, double high)
	{
		return genUniformFloat(new FloatMat(row, col, false), low, high);
	}
	
	/**
	 * Generate a new FloatMat (vector) with random uniform float
	 */
	public FloatMat genUniformFloat(int n)
	{
		return genUniformFloat(n, 1);
	}
	
	/**
	 * Fill a FloatMat with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genNormalFloat(FloatMat A, double mean, double stdev)
	{
		curandGenerateNormal(generator, A.toDevice(), A.size(), (float)mean, (float)stdev);
		return A;
	}
	
	/**
	 * Fill a FloatMat with standard normal distribution
	 */
	public FloatMat genNormalFloat(FloatMat A)
	{
		return genNormalFloat(A, 0, 1);
	}
	
	/**
	 * Generate a new FloatMat with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genNormalFloat(int row, int col, double mean, double stdev)
	{
		return genNormalFloat(new FloatMat(row, col, false), mean, stdev);
	}
	
	/**
	 * Generate a new FloatMat with standard normal distribution
	 */
	public FloatMat genNormalFloat(int row, int col)
	{
		return genNormalFloat(row, col, 0, 1);
	}
	
	/**
	 * Generate a new FloatMat (vector) with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genNormalFloat(int n, double mean, double stdev)
	{
		return genNormalFloat(n, 1, mean, stdev);
	}
	
	/**
	 * Generate a new FloatMat (vector) with standard normal distribution
	 */
	public FloatMat genNormalFloat(int n)
	{
		return genNormalFloat(n, 1, 0f, 1f);
	}
	
	/**
	 * Fill a FloatMat with lognormal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genLogNormalFloat(FloatMat A, double mean, double stdev)
	{
		curandGenerateLogNormal(generator, A.toDevice(), A.size(), (float)mean, (float)stdev);
		return A;
	}
	
	/**
	 * Fill a FloatMat with standard lognormal distribution
	 */
	public FloatMat genLogNormalFloat(FloatMat A)
	{
		return genLogNormalFloat(A, 0, 1);
	}

	/**
	 * Generate a new FloatMat with lognormal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genLogNormalFloat(int row, int col, double mean, double stdev)
	{
		return genLogNormalFloat(new FloatMat(row, col, false), mean, stdev);
	}
	
	/**
	 * Generate a new FloatMat with std lognormal distribution
	 */
	public FloatMat genLogNormalFloat(int row, int col)
	{
		return genLogNormalFloat(row, col, 0, 1);
	}
	
	/**
	 * Generate a new FloatMat (vector) with lognormal distribution 
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genLogNormalFloat(int n, double mean, double stdev)
	{
		return genLogNormalFloat(n, 1, mean, stdev);
	}
	
	/**
	 * Generate a new FloatMat (vector) with std lognormal distribution 
	 */
	public FloatMat genLogNormalFloat(int n)
	{
		return genLogNormalFloat(n, 1, 0f, 1f);
	}
	
	/**
	 * Fill a FloatMat (int valued) with Poisson distribution
	 * @param lambda 
	 */
	public FloatMat genPoissonFloat(FloatMat A, double lambda)
	{
		curandGeneratePoisson(generator, A.toDevice(), A.size(), lambda);
		return A;
	}
	
	/**
	 * Generate a new FloatMat (int valued) with Poisson distribution
	 * @param lambda 
	 */
	public FloatMat genPoissonFloat(int row, int col, double lambda)
	{
		return genPoissonFloat(new FloatMat(row, col, false), lambda);
	}
	
	/**
	 * Generate a FloatMat (vector, int valued) with Poisson distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public FloatMat genPoissonFloat(int n, double lambda)
	{
		return genPoissonFloat(n, 1, lambda);
	}

	/**
	 * Generate a FloatMat with standard Laplacian distribution
	 */
	public FloatMat genLaplacianFloat(FloatMat A)
	{
		genUniformFloat(A);
		Thrust.laplacian(A);
		return A;
	}

	/**
	 * Generate a FloatMat with standard Laplacian distribution
	 * @return new
	 */
	public FloatMat genLaplacianFloat(int row, int col)
	{
		return genLaplacianFloat(new FloatMat(row, col, false));
	}
	
	/**
	 * Generate a FloatMat with standard Cauchy distribution
	 */
	public FloatMat genCauchyFloat(FloatMat A)
	{
		genUniformFloat(A);
		Thrust.cauchy(A);
		return A;
	}

	/**
	 * Generate a FloatMat with standard Cauchy distribution
	 * @return new
	 */
	public FloatMat genCauchyFloat(int row, int col)
	{
		return genCauchyFloat(new FloatMat(row, col, false));
	}
	
	//**************************************************/
	//******************* DOUBLE *******************/
	//**************************************************/
	/**
	 * Fill a DoubleMat with random uniform double
	 * @return input DoubleMat A
	 */
	public DoubleMat genUniformDouble(DoubleMat A)
	{
		curandGenerateUniformDouble(generator, A.toDevice(), A.size());
		return A;
	}

	/**
	 * Generate a new DoubleMat with random uniform double
	 */
	public DoubleMat genUniformDouble(int row, int col)
	{
		return genUniformDouble(new DoubleMat(row, col, false));
	}
	
	/**
	 * Generate a new DoubleMat (vector) with random uniform double
	 */
	public DoubleMat genUniformDouble(int n)
	{
		return genUniformDouble(n, 1);
	}
	
	/**
	 * Fill a DoubleMat with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genNormalDouble(DoubleMat A, double mean, double stdev)
	{
		curandGenerateNormalDouble(generator, A.toDevice(), A.size(), mean, stdev);
		return A;
	}
	
	/**
	 * Generate a new DoubleMat with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genNormalDouble(int row, int col, double mean, double stdev)
	{
		return genNormalDouble(new DoubleMat(row, col, false), mean, stdev);
	}
	
	/**
	 * Generate a new DoubleMat (vector) with normal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genNormalDouble(int n, double mean, double stdev)
	{
		return genNormalDouble(n, 1, mean, stdev);
	}
	
	/**
	 * Fill a DoubleMat with lognormal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genLogNormalDouble(DoubleMat A, double mean, double stdev)
	{
		curandGenerateLogNormalDouble(generator, A.toDevice(), A.size(), mean, stdev);
		return A;
	}

	/**
	 * Generate a new DoubleMat with lognormal distribution
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genLogNormalDouble(int row, int col, double mean, double stdev)
	{
		return genLogNormalDouble(new DoubleMat(row, col, false), mean, stdev);
	}
	
	/**
	 * Generate a new DoubleMat (vector) with lognormal distribution 
	 * @param mean 
	 * @param stdev standard deviation
	 */
	public DoubleMat genLogNormalDouble(int n, double mean, double stdev)
	{
		return genLogNormalDouble(n, 1, mean, stdev);
	}
}
