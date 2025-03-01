import numpy as np
import matplotlib.pyplot as plt
from pylops import Restriction, Gradient
from pylops.optimization.sparsity import FISTA
from scipy.interpolate import griddata

# Step 1: Generate a synthetic elevation model
def generate_elevation(grid_size):
    x_full = np.linspace(0, 1, grid_size[1])
    y_full = np.linspace(0, 1, grid_size[0])
    X_full, Y_full = np.meshgrid(x_full, y_full)
    elevation = np.sin(5 * X_full) * np.cos(3 * Y_full) + 0.5 * np.sin(8 * X_full * Y_full)
    return X_full, Y_full, elevation

# Step 2: Sample data along transects
def sample_transects(X, Y, elevation, num_transects=15, num_points=200):
    np.random.seed(42)
    transect_x = np.linspace(0, 1, X.shape[1])
    transect_y = np.random.uniform(0, 1, num_transects)[:, np.newaxis]
    transect_y = np.tile(transect_y, (1, len(transect_x)))
    x_samples = transect_x.flatten()
    y_samples = transect_y.flatten()
    z_samples = griddata((X.ravel(), Y.ravel()), elevation.ravel(), (x_samples, y_samples), method='nearest')
    return x_samples, y_samples, z_samples

# Step 3: Apply bilinear interpolation
def bilinear_interpolation(X, Y, x_samples, y_samples, z_samples):
    return griddata((x_samples, y_samples), z_samples, (X, Y), method='linear')

# Step 4: Apply FISTA + TV reconstruction using PyLops
def fista_tv_reconstruction(grid_size, x_samples, y_samples, z_samples, lambda_tv=0.1, niter=200):
    # Create restriction operator
    coords = np.column_stack((y_samples * (grid_size[0] - 1), x_samples * (grid_size[1] - 1)))
    idx = np.ravel_multi_index(coords.T.astype(int), grid_size)
    R = Restriction(np.prod(grid_size), idx)

    # Create gradient operator
    G = Gradient(dims=grid_size)

    # Observed data
    d = z_samples

    # FISTA solver
    x0 = np.zeros(np.prod(grid_size))
    reconstructed, _ = FISTA(R, d, G, eps=1e-3, niter=niter, tol=1e-4, x0=x0, lmbda=lambda_tv, show=True)
    return reconstructed.reshape(grid_size)

# Step 5: Visualization
def visualize_results(elevation, x_samples, y_samples, bilinear_reconstructed, fista_reconstructed):
    fig, axs = plt.subplots(1, 3, figsize=(15, 5))
    axs[0].imshow(elevation, cmap='terrain', origin='lower')
    axs[0].set_title("True Elevation (Ground Truth)")
    axs[1].imshow(bilinear_reconstructed, cmap='terrain', origin='lower')
    axs[1].set_title("Bilinear Interpolation")
    axs[2].imshow(fista_reconstructed, cmap='terrain', origin='lower')
    axs[2].set_title("FISTA + TV Reconstruction")
    plt.tight_layout()
    plt.show()

# Main Execution
def main():
    grid_size = (200, 200)  # Define grid size
    X, Y, elevation = generate_elevation(grid_size)
    x_samples, y_samples, z_samples = sample_transects(X, Y, elevation)
    bilinear_reconstructed = bilinear_interpolation(X, Y, x_samples, y_samples, z_samples)
    fista_reconstructed = fista_tv_reconstruction(grid_size, x_samples, y_samples, z_samples)
    visualize_results(elevation, x_samples, y_samples, bilinear_reconstructed, fista_reconstructed)

if __name__ == "__main__":
    main()
