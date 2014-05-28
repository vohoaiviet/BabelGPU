package utils;

import static jcuda.runtime.JCuda.*;
import static jcuda.runtime.cudaMemcpyKind.*;
import static jcuda.Sizeof.*;
import jcuda.Pointer;

public class GpuUtil
{
	/**
	 * cudaMemcpy from device pointer to host float array
	 */
	public static float[] deviceToHostFloat(Pointer device, int n)
	{
		 // Copy device memory to host 
		float[] host = new float[n];
        cudaMemcpy(Pointer.to(host), device, 
            n * FLOAT, cudaMemcpyDeviceToHost);
        
        return host;
	}

	/**
	 * A single float to a pointer wrapper on the host
	 */
	public static Pointer toFloatPointer(float a)
	{
		return Pointer.to(new float[] {a});
	}
	
	/**
	 * Create a float array on device
	 * @param memsetToZero true to initialize the memory to 0. Default false.
	 */
	public static Pointer createDeviceFloat(int n, boolean memsetToZero)
	{
		Pointer p = new Pointer();
		cudaMalloc(p, n * FLOAT);
		if (memsetToZero)
    		cudaMemset(p, 0, n * FLOAT);
		return p;
	}

	/**
	 * Default: memset = false
	 */
	public static Pointer createDeviceFloat(int n)
	{
		return createDeviceFloat(n, false);
	}
	

	//**************************************************/
	//******************* DOUBLE *******************/
	//**************************************************/
	/**
	 * cudaMemcpy from device pointer to host double array
	 */
	public static double[] deviceToHostDouble(Pointer device, int n)
	{
		 // Copy device memory to host 
		double[] host = new double[n];
        cudaMemcpy(Pointer.to(host), device, 
            n * DOUBLE, cudaMemcpyDeviceToHost);
        
        return host;
	}

	/**
	 * A single double to a pointer wrapper on the host
	 */
	public static Pointer toDoublePointer(double a)
	{
		return Pointer.to(new double[] {a});
	}
	
	/**
	 * Create a double array on device
	 * @param memsetToZero true to initialize the memory to 0. Default false.
	 */
	public static Pointer createDeviceDouble(int n, boolean memsetToZero)
	{
		Pointer p = new Pointer();
		cudaMalloc(p, n * DOUBLE);
		if (memsetToZero)
    		cudaMemset(p, 0, n * DOUBLE);
		return p;
	}

	/**
	 * Default: memset = false
	 */
	public static Pointer createDeviceDouble(int n)
	{
		return createDeviceDouble(n, false);
	}
}
