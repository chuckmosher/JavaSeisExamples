import numpy as np
import matplotlib.pyplot as plt
from scipy.interpolate import Rbf, RegularGridInterpolator

# Generate synthetic elevation model
def generate_elevation(shape=(101, 101), spacing=10):
    x = np.linspace(0, (shape[1] - 1) * spacing, shape[1])
    y = np.linspace(0, (shape[0] - 1) * spacing, shape[0])
    X, Y = np.meshgrid(x, y)
    elevation = np.sin(5 * X / 1000) * np.cos(3 * Y / 1000) + 0.5 * np.sin(8 * X * Y / 1e6)
    return x, y, elevation

# Generate random transects
def generate_transects(num_transects=10, percent_length=0.75, grid_shape=(101, 101), spacing=10, seed=42):
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

# Apply fault displacement
def apply_fault(elevation, x, y, fault_x, fault_y, fault_azimuth, throw):
    fault_angle_rad = np.radians(fault_azimuth)
    fault_dx = np.cos(fault_angle_rad)
    fault_dy = np.sin(fault_angle_rad)
    
    X, Y = np.meshgrid(x, y)
    dist_to_fault = (X - fault_x) * fault_dy - (Y - fault_y) * fault_dx
    
    elevation_shifted = elevation.copy()
    elevation_shifted[dist_to_fault > 0] += throw
    
    return elevation_shifted

# Extract elevation values along the transects
def extract_transect_data(x, y, elevation, transects):
    interpolator = RegularGridInterpolator((y, x), elevation, method='linear', bounds_error=False, fill_value=np.nan)
    transect_data = []
    
    for transect_x, transect_y in transects:
        points = np.vstack((transect_y, transect_x)).T  # Format for interpolation
        transect_z = interpolator(points)
        transect_data.append((transect_x, transect_y, transect_z))
    
    return transect_data

# RBF interpolation
def rbf_interpolation(x, y, transect_data, function='multiquadric', epsilon=10, smooth=0.1):
    known_x, known_y, known_z = [], [], []
    
    for transect_x, transect_y, transect_z in transect_data:
        known_x.extend(transect_x)
        known_y.extend(transect_y)
        known_z.extend(transect_z)
    
    known_x = np.array(known_x)
    known_y = np.array(known_y)
    known_z = np.array(known_z)
    
    rbf = Rbf(known_x, known_y, known_z, function=function, epsilon=epsilon, smooth=smooth)
    
    grid_x, grid_y = np.meshgrid(x, y)
    reconstructed_elevation = rbf(grid_x, grid_y)
    
    return reconstructed_elevation

# Main function to run the synthetic test
def main():
    shape = (101, 101)
    spacing = 10
    num_transects = 10
    percent_length = 0.75
    seed = 42
    
    x, y, elevation = generate_elevation(shape, spacing)
    
    # Define fault parameters
    fault_x, fault_y = 500, 500  # Fault location (center of the grid)
    fault_azimuth = 160  # Degrees East of North
    fault_throw = 0.5  # Meters vertical displacement
    
    elevation_faulted = apply_fault(elevation, x, y, fault_x, fault_y, fault_azimuth, fault_throw)
    
    transects = generate_transects(num_transects, percent_length, shape, spacing, seed)
    transect_data = extract_transect_data(x, y, elevation_faulted, transects)
    
    rbf_function = 'multiquadric'  # Options: 'multiquadric', 'inverse', 'gaussian', 'linear', etc.
    rbf_epsilon = 10  # Controls influence of points
    rbf_smooth = 0.1  # Regularization to allow deviation from exact fit
    rbf_elevation = rbf_interpolation(x, y, transect_data, function=rbf_function, epsilon=rbf_epsilon, smooth=rbf_smooth)
    
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    # Plot true elevation model with fault
    c1 = axes[0].contourf(x, y, elevation_faulted, levels=20, cmap='terrain')
    axes[0].set_title('True Elevation Model with Fault')
    fig.colorbar(c1, ax=axes[0])
    
    # Plot RBF reconstructed elevation model
    c2 = axes[1].contourf(x, y, rbf_elevation, levels=20, cmap='terrain')
    axes[1].set_title('RBF Reconstructed Elevation')
    fig.colorbar(c2, ax=axes[1])
    
    # Vector plot of real and reconstructed elevation along a transect
    transect_x, transect_y, transect_z = transect_data[0]
    interp_reconstructed = RegularGridInterpolator((y, x), rbf_elevation)(np.vstack((transect_y, transect_x)).T)
    axes[2].plot(transect_x, transect_z, 'r-', label='True Elevation')
    axes[2].plot(transect_x, interp_reconstructed, 'b--', label='RBF Reconstructed Elevation')
    axes[2].set_title('Transect Elevation Comparison')
    axes[2].legend()
    
    plt.show()

if __name__ == "__main__":
    main()
