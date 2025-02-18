import numpy as np
import pylops
import matplotlib.pyplot as plt
import os
from scipy.fftpack import fft2, ifft2
from pylops.optimization.sparsity import FISTA
from pylops import LinearOperator

# Generate elevation model (replaces random numbers in x_true)
def generate_elevation(shape=(10, 10)):
    x = np.linspace(0, 1, shape[1])
    y = np.linspace(0, 1, shape[0])
    X, Y = np.meshgrid(x, y)
    elevation = np.sin(5 * X) * np.cos(3 * Y) + 0.5 * np.sin(8 * X * Y)
    return elevation

# Create a 2D test problem with elevation data and a restriction operator
def generate_test_problem(shape=(10, 10), noise_level=0.01, sample_fraction=0.5):
    np.random.seed(42)
    m, n = shape
    x_true = generate_elevation(shape)  # Replace random with elevation model
    
    # Generate indices for restriction (random subset of values)
    total_samples = m * n
    num_samples = int(sample_fraction * total_samples)
    sample_indices = np.random.choice(total_samples, num_samples, replace=False)
    
    # Define restriction operator
    R = pylops.Restriction(total_samples, sample_indices)
    
    # Apply restriction to true elevation model
    b = R @ x_true.flatten() + noise_level * np.random.randn(num_samples)
    
    return R, x_true, b, sample_indices

# Dot product test for Restriction Operator
def dot_product_test(operator, shape):
    np.random.seed(42)
    x = np.random.randn(shape[0] * shape[1])
    y = np.random.randn(len(operator.matvec(x)))
    lhs = y.T @ operator.matvec(x)
    rhs = operator.rmatvec(y).T @ x
    relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
    return relative_error

# Visualize Restriction Operator Performance
def visualize_restriction_operator(shape=(10, 10)):
    R, x_true, b, sample_indices = generate_test_problem(shape)
    
    # Perform dot product test
    error = dot_product_test(R, shape)
    print(f"Dot Product Test Relative Error: {error:.6e}")
    
    # Only show plots if running interactively
    if os.getenv("CI") is None:
        # Create an empty array and insert sampled values
        x_sampled = np.full(x_true.shape, np.nan)
        x_flattened = x_true.flatten()
        for idx in sample_indices:
            x_sampled.flat[idx] = x_flattened[idx]
        
        # Apply restriction operator to true model
        x_restricted = np.zeros_like(x_true.flatten())
        x_restricted[sample_indices] = b
        x_restricted = x_restricted.reshape(shape)
        
        # Compute difference
        x_difference = np.copy(x_sampled)
        mask = ~np.isnan(x_sampled)
        x_difference[mask] -= x_restricted[mask]
        
        # Determine color scale limits for consistency
        vmin, vmax = -1, 1  # Set fixed color scale range
        
        # Plot results
        fig, axs = plt.subplots(1, 4, figsize=(24, 6))
        
        im1 = axs[0].imshow(x_true, cmap='viridis', aspect='auto', vmin=vmin, vmax=vmax)
        axs[0].set_title("True Elevation Model")
        plt.colorbar(im1, ax=axs[0])
        
        im2 = axs[1].imshow(x_sampled, cmap='viridis', aspect='auto', vmin=vmin, vmax=vmax)
        axs[1].set_title("Random Subset of True Model")
        plt.colorbar(im2, ax=axs[1])
        
        im3 = axs[2].imshow(x_restricted, cmap='viridis', aspect='auto', vmin=vmin, vmax=vmax)
        axs[2].set_title("Values from Restriction Operator")
        plt.colorbar(im3, ax=axs[2])
        
        im4 = axs[3].imshow(x_difference, cmap='RdBu', aspect='auto', vmin=vmin, vmax=vmax)
        axs[3].set_title("Difference (Subset - Restricted Values)")
        plt.colorbar(im4, ax=axs[3])
        
        plt.show()

if __name__ == "__main__":
    visualize_restriction_operator()
