# Fourier Transform Operator for Compressive Sensing in Python

import numpy as np
import matplotlib.pyplot as plt
import os
try:
    import cupy as cp
    CUPY_AVAILABLE = True
except ImportError:
    CUPY_AVAILABLE = False
    cp = np  # Fallback to NumPy if CuPy is unavailable

from scipy.sparse.linalg import LinearOperator

# Generate elevation model (replaces random numbers in x_true)
def generate_elevation(shape=(10, 10)):
    x = np.linspace(0, 1, shape[1])
    y = np.linspace(0, 1, shape[0])
    X, Y = np.meshgrid(x, y)
    elevation = np.sin(5 * X) * np.cos(3 * Y) + 0.5 * np.sin(8 * X * Y)
    return elevation

# Define 2D Fourier Transform operator with symmetric scaling
class FourierOperator(LinearOperator):
    def __init__(self, shape, use_gpu=False):
        self.m, self.n = shape
        self.shape = (self.m * self.n, self.m * self.n)
        self.dtype = np.complex64
        self.use_gpu = use_gpu and CUPY_AVAILABLE

        # Corrected Scaling Factors
        self.fwd_scale = 1 / np.sqrt(self.m * self.n)  # Forward FFT scaling
        self.inv_scale = self.m * self.n / np.sqrt(self.m * self.n)  # Inverse FFT scaling

    def _matvec(self, x):
        backend = cp if self.use_gpu else np
        return (self.fwd_scale * backend.fft.fft2(x.reshape(self.m, self.n))).flatten()

    def _rmatvec(self, y):
        backend = cp if self.use_gpu else np
        return (self.inv_scale * backend.fft.ifft2(y.reshape(self.m, self.n))).flatten()
    
    def dot_product_test(self):
        np.random.seed(42)
        x = np.random.randn(self.m * self.n) + 1j * np.random.randn(self.m * self.n)
        y = np.random.randn(self.m * self.n) + 1j * np.random.randn(self.m * self.n)
        lhs = np.vdot(y, self._matvec(x))
        rhs = np.vdot(self._rmatvec(y), x)
        relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
        print(f"Fourier Operator Dot Product Test - LHS: {lhs:.6e}, RHS: {rhs:.6e}")
        return relative_error

# Define Restriction Operator
class RestrictionOperator(LinearOperator):
    def __init__(self, grid_x, grid_y, sample_x, sample_y):
        self.grid_x, self.grid_y = np.asarray(grid_x), np.asarray(grid_y)
        self.sample_x, self.sample_y = np.asarray(sample_x), np.asarray(sample_y)
        self.n_samples = self.sample_x.size
        self.n_grid = self.grid_x.size * self.grid_y.size
        dtype = np.float32
        shape = (self.n_samples, self.n_grid)
        super().__init__(dtype=dtype, shape=shape)
    
    def dot_product_test(self):
        np.random.seed(42)
        x = np.random.randn(self.shape[1])
        y = np.random.randn(self.shape[0])
        lhs = np.dot(y, self._matvec(x))
        rhs = np.dot(self._rmatvec(y), x)
        relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
        print(f"Restriction Operator Dot Product Test - LHS: {lhs:.6e}, RHS: {rhs:.6e}")
        return relative_error

    def _matvec(self, Z):
        Z = Z.reshape(len(self.grid_y), len(self.grid_x))
        d = np.zeros(self.n_samples)
        for i in range(self.n_samples):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = max(0, min(x1_idx + 1, len(self.grid_x) - 1))
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            z1, z2 = Z[iy, x1_idx], Z[iy, x2_idx]
            d[i] = z1 + (z2 - z1) * (self.sample_x[i] - x1) / (x2 - x1) if x1 != x2 else z1
        return d

    def _rmatvec(self, d):
        Z_adjoint = np.zeros((len(self.grid_y), len(self.grid_x)))
        for i in range(len(self.sample_x)):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = max(0, min(x1_idx + 1, len(self.grid_x) - 1))
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            if x1 != x2:
                weight1 = (x2 - self.sample_x[i]) / (x2 - x1)
                weight2 = (self.sample_x[i] - x1) / (x2 - x1)
                Z_adjoint[iy, x1_idx] += d[i] * weight1
                Z_adjoint[iy, x2_idx] += d[i] * weight2
            else:
                Z_adjoint[iy, x1_idx] += d[i]
        return Z_adjoint.ravel()

# Run tests
if __name__ == "__main__":
    shape = (64, 64)
    grid_x = np.linspace(0, 1, shape[1])
    grid_y = np.linspace(0, 1, shape[0])
    sample_x = np.random.uniform(0, 1, 100)
    sample_y = np.random.uniform(0, 1, 100)
    R = RestrictionOperator(grid_x, grid_y, sample_x, sample_y)
    error_R = R.dot_product_test()
    print(f"Restriction Operator Dot Product Test Relative Error: {error_R:.6e}")
    
    F = FourierOperator(shape, use_gpu=False)
    error_F = F.dot_product_test()
    print(f"Fourier Operator Dot Product Test Relative Error: {error_F:.6e}")
