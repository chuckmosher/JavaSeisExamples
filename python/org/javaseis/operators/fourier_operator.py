import numpy as np
import pylops
import matplotlib.pyplot as plt
import os
try:
    import cupy as cp
    CUPY_AVAILABLE = True
except ImportError:
    CUPY_AVAILABLE = False
    cp = np  # Fallback to NumPy if CuPy is unavailable

from pylops import LinearOperator

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
        self.scale_factor = 1 / np.sqrt(self.m * self.n)
        self.matvec_count = 0  # Track matvec calls
        self.rmatvec_count = 0  # Track rmatvec calls
        self.use_gpu = use_gpu and CUPY_AVAILABLE

    def _matvec(self, x):
        self.matvec_count += 1
        backend = cp if self.use_gpu else np
        return (self.scale_factor * backend.fft.fft2(x.reshape(self.m, self.n))).flatten()
        #x = backend.array(x).reshape(self.m, self.n)
        #return backend.fft.fft2(x, norm="ortho").flatten()  # Use "ortho" for symmetric scaling
    
    def _rmatvec(self, y):
        self.rmatvec_count += 1
        backend = cp if self.use_gpu else np  # Fix: Track rmatvec calls
        return (self.scale_factor * (self.m * self.n) * np.fft.ifft2(y.reshape(self.m, self.n))).flatten()
        #y = backend.array(y).reshape(self.m, self.n)
        #return backend.fft.ifft2(y, norm="ortho").flatten()  # Use "ortho" for symmetric scaling

# Soft-thresholding function for complex values
def soft_thresholding(z, threshold):
    magnitude = np.abs(z)
    ratio = np.maximum(magnitude - threshold, 0) / (magnitude + 1e-10)
    return z * ratio

# Dot product test for Fourier Transform Operator
def dot_product_test(operator, shape):
    np.random.seed(42)
    x = np.random.randn(shape[0] * shape[1]) + 1j * np.random.randn(shape[0] * shape[1])
    y = np.random.randn(shape[0] * shape[1]) + 1j * np.random.randn(shape[0] * shape[1])
    lhs = np.vdot(y, operator._matvec(x))
    rhs = np.vdot(operator._rmatvec(y), x)
    relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
    print(f"LHS: {lhs:.6e}, RHS: {rhs:.6e}")
    return relative_error

# Run visualization and test with GPU support
if __name__ == "__main__":
    shape = (64, 64)
    F = FourierOperator(shape, use_gpu=True)  # Enable GPU support if available
    error = dot_product_test(F, shape)
    print(f"Dot Product Test Relative Error: {error:.6e}")

