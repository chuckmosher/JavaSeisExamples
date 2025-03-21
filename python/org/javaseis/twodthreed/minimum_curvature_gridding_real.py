import numpy as np
import matplotlib.pyplot as plt
import argparse
from scipy.interpolate import RegularGridInterpolator
from scipy.sparse import lil_matrix
from scipy.sparse.linalg import lsqr

# Generate elevation model
def generate_elevation(shape, spacing):
    x = np.linspace(0, (shape[1] - 1) * spacing, shape[1])
    y = np.linspace(0, (shape[0] - 1) * spacing, shape[0])
    X, Y = np.meshgrid(x, y)
    elevation = np.sin(5 * X / 1000) * np.cos(3 * Y / 1000) + 0.5 * np.sin(8 * X * Y / 1e6)
    return x, y, elevation

# Generate random transects
def generate_transects(num_transects, percent_length, grid_shape, spacing, seed):
    np.random.seed(seed)
    grid_width = (grid_shape[1] - 1) * spacing
    grid_height = (grid_shape[0] - 1) * spacing
    min_length = percent_length * grid_width
    transects = []
    
    for _ in range(num_transects):
        while True:
            x1, y1 = np.random.uniform(0, grid_width), np.random.uniform(0, grid_height)
            angle = np.random.uniform(0, np.pi)
            length = np.random.uniform(min_length, grid_width)
            x2 = x1 + length * np.cos(angle)
            y2 = y1 + length * np.sin(angle)
            if 0 <= x2 <= grid_width and 0 <= y2 <= grid_height:
                break
        
        num_samples = int(length / spacing)
        transect_x = np.linspace(x1, x2, num_samples)
        transect_y = np.linspace(y1, y2, num_samples)
        transects.append((transect_x, transect_y))
    
    return transects

# Extract elevation values along the transects
def extract_transect_data(x, y, elevation, transects):
    interpolator = RegularGridInterpolator((y, x), elevation, method='linear', bounds_error=False, fill_value=np.nan)
    transect_data = []
    
    for transect_x, transect_y in transects:
        points = np.vstack((transect_y, transect_x)).T  # Format for interpolation
        transect_z = interpolator(points)
        transect_data.append((transect_x, transect_y, transect_z))
    
    return transect_data

# Solve Minimum Curvature Gridding using finite differences
def minimum_curvature_gridding(x, y, transect_data, grid_shape, weight):
    ny, nx = grid_shape
    A = lil_matrix((nx * ny, nx * ny))
    b = np.zeros(nx * ny)
    
    def index(ix, iy):
        return iy * nx + ix
    
    # Build system of equations
    for iy in range(1, ny - 1):
        for ix in range(1, nx - 1):
            idx = index(ix, iy)
            A[idx, index(ix, iy)] = -4
            A[idx, index(ix - 1, iy)] = 1
            A[idx, index(ix + 1, iy)] = 1
            A[idx, index(ix, iy - 1)] = 1
            A[idx, index(ix, iy + 1)] = 1
    
    # Apply known transect constraints with soft enforcement
    for transect_x, transect_y, transect_z in transect_data:
        for tx, ty, tz in zip(transect_x, transect_y, transect_z):
            ix = np.digitize(tx, x) - 1
            iy = np.digitize(ty, y) - 1
            if 0 <= ix < nx and 0 <= iy < ny:
                idx = index(ix, iy)
                A[idx, idx] += weight  # Instead of forcing exactly 1, we weight the constraint
                b[idx] += weight * tz  # Weighted contribution
    
    # Convert to sparse matrix and solve using least squares
    A = A.tocsr()
    elevation_grid = lsqr(A, b)[0].reshape(ny, nx)
    return elevation_grid

# Main function to generate, extract, and reconstruct elevation
def main():
    parser = argparse.ArgumentParser(description="Minimum Curvature Gridding Experiment")
    parser.add_argument("--num-transects", type=int, default=21, help="Number of transects")
    parser.add_argument("--percent-length", type=float, default=0.75, help="Percentage of grid width each transect spans")
    parser.add_argument("--grid-size", type=int, default=101, help="Size of the elevation grid")
    parser.add_argument("--spacing", type=int, default=10, help="Grid spacing")
    parser.add_argument("--weight", type=float, default=10, help="Soft constraint weight")
    parser.add_argument("--seed", type=int, default=42, help="Random seed for reproducibility")
    args = parser.parse_args()
    
    grid_shape = (args.grid_size, args.grid_size)
    x, y, elevation = generate_elevation(grid_shape, args.spacing)
    transects = generate_transects(args.num_transects, args.percent_length, grid_shape, args.spacing, args.seed)
    transect_data = extract_transect_data(x, y, elevation, transects)
    
    # Solve Minimum Curvature Gridding
    mcg_elevation = minimum_curvature_gridding(x, y, transect_data, elevation.shape, args.weight)
    
    # Determine shared color scale
    vmin = min(np.min(elevation), np.min(mcg_elevation))
    vmax = max(np.max(elevation), np.max(mcg_elevation))
    
    # Plot results
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    c1 = axes[0].contourf(x, y, elevation, levels=20, cmap='terrain', vmin=vmin, vmax=vmax)
    axes[0].set_title('True Elevation')
    fig.colorbar(c1, ax=axes[0])
    
    c2 = axes[1].contourf(x, y, mcg_elevation, levels=20, cmap='terrain', vmin=vmin, vmax=vmax)
    axes[1].set_title('MCG Reconstructed Elevation')
    fig.colorbar(c2, ax=axes[1])
    
    # Select one transect and plot true vs. reconstructed elevation
    transect_x, transect_y, transect_z = transect_data[0]
    interp_reconstructed = RegularGridInterpolator((y, x), mcg_elevation)(np.vstack((transect_y, transect_x)).T)
    axes[2].plot(transect_x, transect_z, 'r-', label='True Elevation')
    axes[2].plot(transect_x, interp_reconstructed, 'b--', label='Reconstructed Elevation')
    axes[2].set_title('Transect Elevation Comparison')
    axes[2].legend()
    
    plt.show()

if __name__ == "__main__":
    main()
