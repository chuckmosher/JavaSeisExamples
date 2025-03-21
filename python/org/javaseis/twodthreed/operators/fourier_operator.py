
import numpy as np
try:
    import cupy as cp
    CUPY_AVAILABLE = True
except ImportError:
    CUPY_AVAILABLE = False
    cp = np  # Fallback to NumPy if CuPy is unavailable

from scipy.sparse.linalg import LinearOperator

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
