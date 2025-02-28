import numpy as np
import matplotlib.pyplot as plt
from org.javaseis.operators.fourier_operator import FourierOperator, soft_thresholding

try:
    import cupy as cp
    CUPY_AVAILABLE = True
except ImportError:
    CUPY_AVAILABLE = False
    cp = np  # Fallback to NumPy if CuPy is unavailable

def test_fourier_operator():
    shape = (64, 64)
    F = FourierOperator(shape, use_gpu=CUPY_AVAILABLE)

    backend = cp if CUPY_AVAILABLE else np
    Z_true = backend.random.randn(*shape) + 1j * backend.random.randn(*shape)
    FZ = F @ Z_true.ravel()
    Z_recon = F.T @ FZ

    # Compute magnitude for visualization
    FZ_mag = backend.abs(FZ.reshape(shape))
    Z_recon_mag = backend.abs(Z_recon.reshape(shape))
    Z_true_mag = backend.abs(Z_true)

    # Apply soft-thresholding with a 10% threshold
    threshold = 0.1 * backend.max(FZ_mag)
    FZ_thresholded = soft_thresholding(FZ, threshold)
    Z_recon_thresholded = F.T @ FZ_thresholded
    Z_recon_thresholded_mag = backend.abs(Z_recon_thresholded.reshape(shape))

    # Dot product test
    dot1 = backend.vdot(FZ, Z_true.ravel())
    dot2 = backend.vdot(FZ, FZ)
    print(f"Dot product test: {dot1:.6f} vs {dot2:.6f}")

    # Visualization
    plt.ion()
    plt.figure(figsize=(12, 8))

    plt.subplot(2, 3, 1)
    plt.imshow(Z_true_mag.get() if CUPY_AVAILABLE else Z_true_mag, extent=[0, 64, 0, 64], origin='lower', cmap='viridis')
    plt.title("True Data Magnitude")
    plt.colorbar()
    
    plt.subplot(2, 3, 2)
    plt.imshow(FZ_mag.get() if CUPY_AVAILABLE else FZ_mag, extent=[0, 64, 0, 64], origin='lower', cmap='viridis')
    plt.title("Fourier Transform Magnitude")
    plt.colorbar()

    plt.subplot(2, 3, 3)
    plt.imshow(Z_recon_mag.get() if CUPY_AVAILABLE else Z_recon_mag, extent=[0, 64, 0, 64], origin='lower', cmap='viridis')
    plt.title("Reconstructed Data Magnitude")
    plt.colorbar()

    plt.subplot(2, 3, 4)
    plt.imshow(Z_recon_thresholded_mag.get() if CUPY_AVAILABLE else Z_recon_thresholded_mag,
               extent=[0, 64, 0, 64], origin='lower', cmap='viridis')
    plt.title("Reconstructed After Thresholding")
    plt.colorbar()

    plt.subplot(2, 3, 5)
    plt.imshow((Z_true_mag - Z_recon_thresholded_mag).get() if CUPY_AVAILABLE else (Z_true_mag - Z_recon_thresholded_mag),
               extent=[0, 64, 0, 64], origin='lower', cmap='viridis')
    plt.title("Difference After Thresholding")
    plt.colorbar()

    plt.show(block=True)

if __name__ == "__main__":
    test_fourier_operator()

