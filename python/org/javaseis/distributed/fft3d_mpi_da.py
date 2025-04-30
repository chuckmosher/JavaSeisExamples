from mpi4py import MPI
import numpy as np
import time

try:
    import cupy as cp
    GPU_AVAILABLE = True
except ImportError:
    GPU_AVAILABLE = False

from mpi4py_fft import PFFT, newDistArray

class DistributedArray3D:
    def __init__(self, shape, padding=(0, 0, 0), backend='cpu', dtype='float64', comm=MPI.COMM_WORLD):
        self.global_shape = shape
        self.padding = padding
        self.backend = backend
        self.dtype = dtype
        self.comm = comm

        self.padded_shape = tuple(s + p for s, p in zip(shape, padding))

        if backend == 'cpu':
            self._allocate_cpu()
        elif backend == 'gpu':
            if not GPU_AVAILABLE:
                raise RuntimeError("CuPy is not installed. GPU backend not available.")
            self._allocate_gpu()
        else:
            raise ValueError(f"Unknown backend: {backend}")

    def _allocate_cpu(self):
        self.fft = PFFT(self.comm, self.padded_shape, axes=(0, 1, 2), dtype=self.dtype)
        self.data = self.fft.forward.input_array

    def _allocate_gpu(self):
        self.data = cp.zeros(self.global_shape, dtype=self.dtype)

    def fft3d_forward(self):
        if self.backend == 'cpu':
            return self.fft.forward(self.data)
        elif self.backend == 'gpu':
            return cp.fft.fftn(self.data)

    def fft3d_inverse(self, data_hat):
        if self.backend == 'cpu':
            return self.fft.backward(data_hat)
        elif self.backend == 'gpu':
            return cp.fft.ifftn(data_hat)

    def fill_random(self):
        if self.backend == 'cpu':
            local_shape = self.data.shape
            self.data[:] = np.random.rand(*local_shape)
        elif self.backend == 'gpu':
            self.data[:] = cp.random.rand(*self.data.shape)

    def get_local_data(self):
        return self.data


# Example usage: Save as fft3d_test.py and run with: mpirun -np 4 python fft3d_test.py
if __name__ == '__main__':
    shape = (64, 64, 64)  # Global shape
    padding = (0, 0, 0)
    backend = 'cpu'  # Change to 'gpu' if using CuPy

    comm = MPI.COMM_WORLD
    rank = comm.Get_rank()

    da = DistributedArray3D(shape, padding=padding, backend=backend)
    da.fill_random()

    # Time forward FFT
    comm.Barrier()
    t0 = time.time()
    data_hat = da.fft3d_forward()
    comm.Barrier()
    t1 = time.time()

    # Time inverse FFT
    t2 = time.time()
    data_back = da.fft3d_inverse(data_hat)
    comm.Barrier()
    t3 = time.time()

    # Check fidelity
    if backend == 'cpu':
        original = da.get_local_data()
        reconstructed = data_back
        error = np.linalg.norm(original - reconstructed.real) / np.linalg.norm(original)
    else:
        original = da.get_local_data()
        reconstructed = data_back
        error = cp.linalg.norm(original - reconstructed.real) / cp.linalg.norm(original)
        error = cp.asnumpy(error)

    if rank == 0:
        print("=== 3D FFT Test with mpi4py ===")
        print(f"Global shape: {shape}")
        print(f"Backend: {backend.upper()}")
        print(f"Forward FFT time: {t1 - t0:.6f} seconds")
        print(f"Inverse FFT time: {t3 - t2:.6f} seconds")
        print(f"Relative reconstruction error: {error:.2e}")
