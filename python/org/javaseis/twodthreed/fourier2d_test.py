import numpy as np
import matplotlib
matplotlib.use("Qt5Agg")  # or "Qt5Agg" if you have PyQt installed
import matplotlib.pyplot as plt;
from org.javaseis.twodthreed.operators import fourier_operator
from org.javaseis.twodthreed.synthdata import generate_elevation


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
