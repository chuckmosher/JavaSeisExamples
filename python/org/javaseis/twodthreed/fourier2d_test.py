import numpy as np
import pylops
import matplotlib.pyplot as plt
import os
from scipy.fftpack import fft2, ifft2
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
    def __init__(self, shape):
        self.m, self.n = shape
        self.shape = (self.m * self.n, self.m * self.n)
        self.dtype = np.complex64
        self.scale_factor = 1 / np.sqrt(self.m * self.n)  # Symmetric scaling
        self.matvec_count = 0  # Fix: Add tracking for matvec calls
        self.rmatvec_count = 0  # Fix: Add tracking for rmatvec calls

    def _matvec(self, x):
        self.matvec_count += 1  # Fix: Track matvec calls
        return (self.scale_factor * fft2(x.reshape(self.m, self.n))).flatten()
    
    def _rmatvec(self, y):
        self.rmatvec_count += 1  # Fix: Track rmatvec calls
        return (self.scale_factor * (self.m * self.n) * ifft2(y.reshape(self.m, self.n))).flatten()  # Correct scaling

# Soft-thresholding function for complex values
def soft_thresholding(z, threshold):
    magnitude = np.abs(z)
    ratio = np.maximum(magnitude - threshold, 0) / (magnitude + 1e-10)  # Protect against division by zero
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

# Visualization function
def visualize_fourier_operator():
    shape = (10, 10)
    x_true = generate_elevation(shape)
    
    # Define Fourier Transform operator
    F = FourierOperator(shape)
    
    # Apply Fourier Transform to true model
    x_fourier = F._matvec(x_true.flatten()).reshape(shape)
    x_magnitude = np.abs(x_fourier)
    
    # Set threshold to 10% of max magnitude
    threshold = 0.1 * np.max(x_magnitude)
    
    # Apply soft-thresholding to Fourier Transform
    x_fourier_thresholded = soft_thresholding(x_fourier, threshold)
    x_magnitude_thresholded = np.abs(x_fourier_thresholded)
    
    # Compute difference
    x_difference = x_magnitude - x_magnitude_thresholded
    
    # Compute inverse Fourier transforms
    x_reconstructed_original = np.abs(F._rmatvec(x_fourier).reshape(shape))
    x_reconstructed_thresholded = np.abs(F._rmatvec(x_fourier_thresholded).reshape(shape))
    x_reconstructed_difference = np.abs(x_reconstructed_original - x_reconstructed_thresholded)
    
    # Plot results
    fig, axs = plt.subplots(2, 3, figsize=(18, 12))
    
    im1 = axs[0, 0].imshow(np.abs(np.log1p(x_magnitude)), cmap='inferno', aspect='auto')
    axs[0, 0].set_title("Magnitude of Fourier Transform")
    plt.colorbar(im1, ax=axs[0, 0])
    
    im2 = axs[0, 1].imshow(np.abs(np.log1p(x_magnitude_thresholded)), cmap='inferno', aspect='auto')
    axs[0, 1].set_title(f"Thresholded Fourier Transform (Threshold: {threshold:.4f})")
    plt.colorbar(im2, ax=axs[0, 1])
    
    im3 = axs[0, 2].imshow(np.abs(np.log1p(x_difference)), cmap='inferno', aspect='auto')
    axs[0, 2].set_title("Difference (Original - Thresholded)")
    plt.colorbar(im3, ax=axs[0, 2])
    
    im4 = axs[1, 0].imshow(x_reconstructed_original, cmap='viridis', aspect='auto')
    axs[1, 0].set_title("Reconstructed Original Data (Space Domain)")
    plt.colorbar(im4, ax=axs[1, 0])
    
    im5 = axs[1, 1].imshow(x_reconstructed_thresholded, cmap='viridis', aspect='auto')
    axs[1, 1].set_title("Reconstructed Thresholded Data (Space Domain)")
    plt.colorbar(im5, ax=axs[1, 1])
    
    im6 = axs[1, 2].imshow(x_reconstructed_difference, cmap='viridis', aspect='auto')
    axs[1, 2].set_title("Difference (Original - Thresholded) in Space")
    plt.colorbar(im6, ax=axs[1, 2])
    
    plt.show()

# Run visualization with fixed threshold
if __name__ == "__main__":
    visualize_fourier_operator()
    shape = (10, 10)
    F = FourierOperator(shape)
    error = dot_product_test(F, shape)
    print(f"Dot Product Test Relative Error: {error:.6e}")
