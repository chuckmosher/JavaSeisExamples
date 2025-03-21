import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import RegularGridInterpolator, Rbf

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

# Extract elevation values along the transects with noise
def extract_transect_data(x, y, elevation, transects):
    interpolator = RegularGridInterpolator((y, x), elevation, method='linear', bounds_error=False, fill_value=np.nan)
    transect_data = []
    
    for transect_x, transect_y in transects:
        points = np.vstack((transect_y, transect_x)).T  # Format for interpolation
        transect_z = interpolator(points)
        noise = 0.10 * transect_z * np.random.randn(*transect_z.shape)  # 10% noise
        transect_z_noisy = transect_z + noise
        transect_data.append((transect_x, transect_y, transect_z_noisy))
    
    return transect_data

# Thin Plate Spline (TPS) reconstruction
def thin_plate_spline_reconstruction(x, y, transect_data, grid_shape, smoothness=0.1):
    known_x, known_y, known_z = [], [], []
    
    for transect_x, transect_y, transect_z in transect_data:
        known_x.extend(transect_x)
        known_y.extend(transect_y)
        known_z.extend(transect_z)
    
    known_x = np.array(known_x)
    known_y = np.array(known_y)
    known_z = np.array(known_z)
    
    # Thin Plate Spline interpolation using RBF
    tps = Rbf(known_x, known_y, known_z, function='thin_plate', smooth=smoothness)
    
    grid_x, grid_y = np.meshgrid(x, y)
    reconstructed_elevation = tps(grid_x, grid_y)
    
    return reconstructed_elevation

# Main function to generate and visualize test data
def main():
    shape = (101, 101)
    spacing = 10
    num_transects = 10
    percent_length = 0.75
    seed = 42
    
    x, y, elevation = generate_elevation(shape, spacing)
    transects = generate_transects(num_transects, percent_length, shape, spacing, seed)
    transect_data = extract_transect_data(x, y, elevation, transects)
    
    smoothness = 1  # Adjust this value as needed
    tps_elevation = thin_plate_spline_reconstruction(x, y, transect_data, shape, smoothness=smoothness)
    
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    # Plot true elevation model
    c1 = axes[0].contourf(x, y, elevation, levels=20, cmap='terrain')
    axes[0].set_title('True Elevation Model')
    fig.colorbar(c1, ax=axes[0])
    
    # Overlay transects
    for transect_x, transect_y, _ in transect_data:
        axes[0].plot(transect_x, transect_y, 'r-', linewidth=1)
    
    # Plot Thin Plate Spline reconstructed elevation model
    c2 = axes[1].contourf(x, y, tps_elevation, levels=20, cmap='terrain')
    axes[1].set_title('Thin Plate Spline Reconstructed Elevation with Noise')
    fig.colorbar(c2, ax=axes[1])
    
    # Overlay transects on reconstructed model
    for transect_x, transect_y, _ in transect_data:
        axes[1].plot(transect_x, transect_y, 'r-', linewidth=1)
    
    # Select one transect and plot true vs. reconstructed elevation
    transect_x, transect_y, transect_z = transect_data[0]
    interp_reconstructed = RegularGridInterpolator((y, x), tps_elevation)(np.vstack((transect_y, transect_x)).T)
    axes[2].plot(transect_x, transect_z, 'r-', label='Noisy True Elevation')
    axes[2].plot(transect_x, interp_reconstructed, 'b--', label='Reconstructed Elevation')
    axes[2].set_title('Transect Elevation Comparison (with Noise)')
    axes[2].legend()
    
    plt.show()

if __name__ == "__main__":
    main()
