/*
* Port the Thrust library to Java with JavaCpp tool. 
*/

#ifndef try_h__
#define try_h__

#include "cuda.h"
#include "cuda_runtime.h"
#include "device_launch_parameters.h"
#define _USE_MATH_DEFINES // otherwise cmath doesn't have M_PI
#include <cmath>
#include <thrust/host_vector.h>
#include <thrust/device_vector.h>
#include <thrust/generate.h>
#include <thrust/copy.h>
#include <thrust/reduce.h>
#include <thrust/functional.h>
#include <thrust/extrema.h>
#include <thrust/sort.h>
#include <algorithm>
#include <cstdlib>
using namespace thrust;

// Macro defines functors for linear transformations inside an elementary unary function
// Ftype: float or double
// for example, functor: exp(x) 
// functor_1: exp(x + b) 
// functor_2: exp(a * x)
// functor_3: exp(a*x + b)
// default a = 1 and b = 0
#define GEN_linear_functor(name, Ftype) \
struct functor_##name##_##Ftype{ \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(x); } \
}; \
struct functor_##name##_##Ftype##_1{ \
	const Ftype b; \
	functor_##name##_##Ftype##_1(Ftype _b = 0) : b(_b) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(x + b); } \
}; \
struct functor_##name##_##Ftype##_2{ \
	const Ftype a; \
	functor_##name##_##Ftype##_2(Ftype _a = 1) : a(_a) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(a * x); } \
}; \
struct functor_##name##_##Ftype##_3{ \
	const Ftype a, b; \
	functor_##name##_##Ftype##_3(Ftype _a = 1, Ftype _b = 0) : a(_a), b(_b) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(a * x + b); } \
};

// Macro defines corresponding thrust::transform for various linear unary functors
// gpu_exp_float(begin, size) is in place transformation, while gpu_exp_float(begin, size, out) writes to an output pointer.
#define GEN_transf_ftype(name, Ftype) \
GEN_linear_functor(name, Ftype); \
inline void gpu_##name##_##Ftype(device_ptr<Ftype> begin, int size, Ftype a = 1, Ftype b = 0) \
{ \
	if (a == 1 && b == 0) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype()); \
	else if (a == 1) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_1(b)); \
	else if (b == 0) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_2(a)); \
	else \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_3(a, b)); \
} \
inline void gpu_##name##_##Ftype(device_ptr<Ftype> begin, int size, \
	device_ptr<Ftype> out, Ftype a = 1, Ftype b = 0) \
{ \
if (a == 1 && b == 0) \
	transform(begin, begin + size, out, functor_##name##_##Ftype()); \
	else if (a == 1) \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_1(b)); \
	else if (b == 0) \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_2(a)); \
	else \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_3(a, b)); \
}


// Macro defines functors for linear transformations inside a binary function
// Ftype: float or double
// for example, functor: pow(x, p) 
// functor_1: pow(x + b, p) 
// functor_2: pow(a * x, p)
// functor_3: pow(a*x + b, p)
// default a = 1 and b = 0
#define GEN_linear_functor_2(name, Ftype) \
struct functor_##name##_##Ftype{ \
	const Ftype p; \
	functor_##name##_##Ftype(Ftype _p) : p(_p) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(x, p); } \
}; \
struct functor_##name##_##Ftype##_1{ \
	const Ftype p, b; \
	functor_##name##_##Ftype##_1(Ftype _p, Ftype _b = 0) : p(_p), b(_b) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(x + b, p); } \
}; \
struct functor_##name##_##Ftype##_2{ \
	const Ftype p, a; \
	functor_##name##_##Ftype##_2(Ftype _p, Ftype _a = 1) : p(_p), a(_a) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(a * x, p); } \
}; \
struct functor_##name##_##Ftype##_3{ \
	const Ftype p, a, b; \
	functor_##name##_##Ftype##_3(Ftype _p, Ftype _a = 1, Ftype _b = 0) : p(_p), a(_a), b(_b) {} \
	__host__ __device__ Ftype operator()(const Ftype& x) const { return name(a * x + b, p); } \
};

// Macro defines corresponding thrust::transform for various linear binary functors
// gpu_pow_float(begin, size) is in place transformation, while gpu_pow_float(begin, size, out) writes to an output pointer.
#define GEN_transf_ftype_2(name, Ftype) \
GEN_linear_functor_2(name, Ftype); \
inline void gpu_##name##_##Ftype(device_ptr<Ftype> begin, int size, Ftype p, Ftype a = 1, Ftype b = 0) \
{ \
	if (a == 1 && b == 0) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype(p)); \
	else if (a == 1) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_1(p, b)); \
	else if (b == 0) \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_2(p, a)); \
	else \
		transform(begin, begin + size, begin, functor_##name##_##Ftype##_3(p, a, b)); \
} \
inline void gpu_##name##_##Ftype(device_ptr<Ftype> begin, int size, \
	device_ptr<Ftype> out, Ftype p, Ftype a = 1, Ftype b = 0) \
{ \
if (a == 1 && b == 0) \
	transform(begin, begin + size, out, functor_##name##_##Ftype(p)); \
	else if (a == 1) \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_1(p, b)); \
	else if (b == 0) \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_2(p, a)); \
	else \
	transform(begin, begin + size, out, functor_##name##_##Ftype##_3(p, a, b)); \
}

// Combines float and double functions
#define GEN_transf(name) \
	GEN_transf_ftype(name, float); \
	GEN_transf_ftype(name, double);

#define GEN_transf_2(name) \
	GEN_transf_ftype_2(name, float); \
	GEN_transf_ftype_2(name, double);

namespace MyGpu
{
	// Generate unary transform functions
	// Exp and logs
	GEN_transf(exp);
	GEN_transf(log);
	GEN_transf(log10);
	GEN_transf(sqrt);

	// Trigs
	GEN_transf(cos);
	GEN_transf(sin);
	GEN_transf(tan);
	GEN_transf(acos);
	GEN_transf(asin);
	GEN_transf(atan);
	GEN_transf(cosh);
	GEN_transf(sinh);
	GEN_transf(tanh);

	// Other
	GEN_transf(fabs); // abs()
	GEN_transf(floor);
	GEN_transf(ceil);
	GEN_transf(); // gpu__float(), for plain linear transformation

	// Generate binary transform functions
	GEN_transf_2(pow);
	GEN_transf_2(fmod);

	/* Other non-standard functions */
	__host__ __device__
	inline float sigmoid(float x) { return 1.0 / (1 + exp(-x)); }
	GEN_transf(sigmoid);
	// Sigmoid derivative: x .* (1 - x)
	__host__ __device__
	inline float sigmoid_deriv(float x) { return x * (1 - x); }
	GEN_transf(sigmoid_deriv);

	// simple square routine
	__host__ __device__
	inline float square(float x) { return x * x; }
	GEN_transf(square);
	// simple cube routine
	__host__ __device__
	inline float cube(float x) { return x * x * x; }
	GEN_transf(cube);
	// simple reciprocal routine
	__host__ __device__
	inline float reciprocal(float x) { return 1.0/x; }
	GEN_transf(reciprocal);

	// Random distribution generators
	// Cauchy CDF: given a uniform random var, transform it to be cauchy
	__host__ __device__
	inline float cauchy(float x) { return tan(M_PI * (x - 0.5)); }
	GEN_transf(cauchy);

	// Laplacian CDF: given a uniform random var, transform it to be lap
	// if < 0, return -1; > 0, return 1
	__host__ __device__
	inline float signum(float val)
	{
		return (0 < val) - (val < 0);
	}
	__host__ __device__
	inline float laplacian(float x)
	{ 
		x -= 0.5;
		return -signum(x) * log(1 - 2 * fabs(x));
	}
	GEN_transf(laplacian);
	GEN_transf(signum);


//#ifndef _WIN32
//	GEN_transf(exp2);
//	GEN_transf(expm1); // exp - 1
//	GEN_transf(log1p); // ln + 1
//	GEN_transf(log2);
//	GEN_transf(cbrt); // cubic root
//	GEN_transf(hypot); // hypotenus
//	GEN_transf(erf); // error function
//	GEN_transf(erfc); // complementary error function
//	GEN_transf(tgamma); // gamma function
//	GEN_transf(lgamma); // log-gamma function
//	GEN_transf(acosh);
//	GEN_transf(asinh);
//	GEN_transf(atanh);
//#endif

	// gpu_min|max_float|double
#define GEN_minmax_ftype(name, Ftype) \
	inline Ftype gpu_##name##_##Ftype(device_ptr<Ftype> begin, int size) \
	{ \
		device_ptr<Ftype> m = name##_element(begin, begin + size); \
		return *m; \
	}
    #define GEN_minmax(name) \
	GEN_minmax_ftype(name, float); \
	GEN_minmax_ftype(name, double);

	GEN_minmax(min);
	GEN_minmax(max);

// dir = ascending: 1, descending -1
#define GEN_sort(Ftype) \
	inline void gpu_sort_##Ftype(device_ptr<Ftype> begin, int size, int dir = 1) \
	{ \
		if (dir > 0) \
			thrust::sort(begin, begin+size); \
		else /* descending sort */ \
			thrust::sort(begin, begin+size, greater<Ftype>()); \
	}

	GEN_sort(float); GEN_sort(double);

#define GEN_dot_mult(Ftype) \
	/* begin2 = begin1 .* begin2 */ \
	inline void gpu_dot_mult_##Ftype(device_ptr<Ftype> begin1, int size, device_ptr<Ftype> begin2) \
	{ transform(begin1, begin1 + size, begin2, begin2, thrust::multiplies<Ftype>()); } \
	/* out = begin1 .* begin2 */ \
	inline void gpu_dot_mult_##Ftype(device_ptr<Ftype> begin1, int size, device_ptr<Ftype> begin2, device_ptr<Ftype> out) \
	{ transform(begin1, begin1 + size, begin2, out, thrust::multiplies<Ftype>()); }

	GEN_dot_mult(float); GEN_dot_mult(double);

	inline float gpu_sum_float(device_ptr<float> begin, int size)
	{ return reduce(begin, begin+size, 0.0, thrust::plus<float>()); }
	inline double gpu_sum_double(device_ptr<double> begin, int size)
	{ return reduce(begin, begin+size, 0.0, thrust::plus<double>()); }

	inline float gpu_product_float(device_ptr<float> begin, int size)
	{ return reduce(begin, begin+size, 1.0, thrust::multiplies<float>()); }
	inline double gpu_product_double(device_ptr<double> begin, int size)
	{ return reduce(begin, begin+size, 1.0, thrust::multiplies<double>()); }

	// Fill the array with the same value
	inline void gpu_fill_float(device_ptr<float> begin, int size, float val)
	{ thrust::fill_n(begin, size, val); }
	inline void gpu_fill_double(device_ptr<double> begin, int size, double val)
	{ thrust::fill_n(begin, size, val); }

	inline void gpu_copy_float(device_ptr<float> begin, int size, device_ptr<float> out)
	{ thrust::copy(begin, begin + size, out); }
	inline void gpu_copy_double(device_ptr<double> begin, int size, device_ptr<double> out)
	{ thrust::copy(begin, begin + size, out); }

	// Swap two arrays
	inline void gpu_swap_float(device_ptr<float> begin, int size, device_ptr<float> out)
	{ thrust::swap_ranges(begin, begin + size, out); }
	inline void gpu_swap_double(device_ptr<double> begin, int size, device_ptr<double> out)
	{ thrust::swap_ranges(begin, begin + size, out); }

	// Utility function for java
	inline device_ptr<float> offset(device_ptr<float> begin, int offset)
	{ return begin + offset; }
	inline device_ptr<double> offset(device_ptr<double> begin, int offset)
	{ return begin + offset; }

	// Set or update a single value on device
	// for gradCheck perturb()
#define GEN_change_single_val(Ftype) \
	inline void gpu_set_single_##Ftype(device_ptr<Ftype> begin, int offset, Ftype newVal) \
	{ begin[offset] = newVal; } \
	inline void gpu_incr_single_##Ftype(device_ptr<Ftype> begin, int offset, Ftype incrVal) \
	{ begin[offset] += incrVal; }

	GEN_change_single_val(float); GEN_change_single_val(double);

	// Deal with int, float, and double raw GPU pointers
#define GEN_raw_pointer_func(Ftype) \
	inline Ftype* offset(Ftype *begin, int offset) \
	{ return begin + offset; } \
	inline void free_device(Ftype *device) { cudaFree(device); } \
	inline void free_host(Ftype *host) { free(host); } \
	inline Ftype* malloc_device_##Ftype(int size, bool memsetTo0) \
	{  \
		Ftype *device; \
		cudaMalloc((void **)&device, size * sizeof(Ftype)); \
		if (memsetTo0) \
			cudaMemset(device, 0, size * sizeof(Ftype)); \
		return device; \
	} \
	inline Ftype *copy_host_to_device(Ftype *host, int size) \
	{ \
		Ftype *device; size *= sizeof(Ftype); \
		cudaMalloc((void **)&device, size); \
		cudaMemcpy(device, host, size, cudaMemcpyHostToDevice); \
		return device; \
	} \
	inline Ftype *copy_device_to_host(Ftype *device, int size) \
	{ \
		size *= sizeof(Ftype); \
		Ftype *host = (Ftype *)malloc(size); \
		cudaMemcpy(host, device, size, cudaMemcpyDeviceToHost); \
		return host; \
	} \
	/* Can be used to copy directly to a primitive array (JavaCpp @Ptr) */ \
	inline void copy_device_to_host(Ftype *device, Ftype host[], int offset, int size) \
	{ \
		cudaMemcpy(host + offset, device, size * sizeof(Ftype), cudaMemcpyDeviceToHost); \
	}

	GEN_raw_pointer_func(int);
	GEN_raw_pointer_func(float);
	GEN_raw_pointer_func(double);


	// Utility for setting blockDim and gridDim (1D). A block cannot have more than 1024 threads
	// number of threads needed, 2 output params
	inline void setKernelDim1D(int threads, dim3& gridDim, dim3& blockDim)
	{
		if (threads > 1024) // we don't have enough threads on a single block
		{
			gridDim.x = threads / 1024 + 1;
			blockDim.x = 1024;
		}
		else // try to make block dim a multiple of 32 to conform with 'warp'
		{
			if (threads % 32 == 0) blockDim.x = threads;
			else blockDim.x = (threads / 32 + 1) * 32;
		}
	}

// Should be used inside kernel functions only
#define ThreadIndex1D(idx, limit) \
	int idx = blockIdx.x * blockDim.x + threadIdx.x; \
	if (idx >= limit) return; // out of bound

// because kernel<<<>>> doesn't return anything, we need another way to get the error code
#define DebugKernel \
	cudaDeviceSynchronize(); \
	printf("Kernel launch: %s\n", cudaGetErrorString(cudaGetLastError()));


	// The specified col will be set to a specific value
	// negative 'colIdx' means counting from the last col (-n => col - n)
	inline void gpu_fill_col_float(device_ptr<float> begin, int row, int col, int colIdx, float val)
	{
		if (colIdx < 0)  colIdx += col;
		thrust::fill_n(begin + row * colIdx, row, val);
	}

	// Change a specific row of a column major matrix to a specific value
	// negative 'rowIdx' means counting from the last row (-n => row - n)
	__global__
	void gpu_fill_row_float_kernel(float *begin, int row, int col, int rowIdx, float val)
	{
		ThreadIndex1D(idx, col);

		begin += row * idx + rowIdx; // end of a column
		*begin = val; // set the value
	}

	// The specified row will be set to a specific value
	// negative 'rowIdx' means counting from the last row (-n => row - n)
	inline void gpu_fill_row_float(device_ptr<float> begin, int row, int col, int rowIdx, float val)
	{
		dim3 gridDim, blockDim;
		setKernelDim1D(col, gridDim, blockDim);

		if (rowIdx < 0) rowIdx += row;

		gpu_fill_row_float_kernel<<<gridDim, blockDim>>>(
			thrust::raw_pointer_cast(begin), row, col, rowIdx, val);
	}


// Matrix transposition
// Code from http://www.evl.uic.edu/aej/525/code/transpose_kernel.cu
// http://www.evl.uic.edu/aej/525/code/transpose.cu
#define BLOCK_DIM 16
	__global__ void gpu_transpose_float_kernel(float *in, int width, int height, float *out)
	{
		__shared__ float block[BLOCK_DIM][BLOCK_DIM + 1];

		// read the matrix tile into shared memory
		unsigned int xIndex = blockIdx.x * BLOCK_DIM + threadIdx.x;
		unsigned int yIndex = blockIdx.y * BLOCK_DIM + threadIdx.y;
		if ((xIndex < width) && (yIndex < height))
		{
			unsigned int index_in = yIndex * width + xIndex;
			block[threadIdx.y][threadIdx.x] = in[index_in];
		}

		__syncthreads();

		// write the transposed matrix tile to global memory
		xIndex = blockIdx.y * BLOCK_DIM + threadIdx.x;
		yIndex = blockIdx.x * BLOCK_DIM + threadIdx.y;
		if ((xIndex < height) && (yIndex < width))
		{
			unsigned int index_out = yIndex * height + xIndex;
			out[index_out] = block[threadIdx.x][threadIdx.y];
		}
	}

	// Transposes 'in' and fills 'out'
	inline void gpu_transpose_float(device_ptr<float> in, int row, int col, device_ptr<float> out)
	{
		dim3 gridDim(std::max(row / BLOCK_DIM, 1), std::max(col / BLOCK_DIM, 1)), 
			blockDim(BLOCK_DIM, BLOCK_DIM);

		gpu_transpose_float_kernel<<<gridDim, blockDim >>>(
			thrust::raw_pointer_cast(in), row, col, thrust::raw_pointer_cast(out));
	}
}
#endif // try_h__