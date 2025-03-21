import numpy as np
import matplotlib.pyplot as plt
import pandas as pd
from scipy.interpolate import RegularGridInterpolator, Rbf

# Define fixed-width column specifications
COLUMN_SPECS = [(0, 17), (17, 35), (35, 47), (47, 59), (59, 69), (69, 79), (79, 89)]
COLUMN_NAMES = ["HorizonName", "LineName", "X", "Y", "TrcNo", "ShotPt", "Z"]
DATA_TYPES = {"X": float, "Y": float, "Z": float}

# Define grid parameters (feet)
GRID_ORIGIN = (683000, 385000)  # Lower-left corner (X, Y)
GRID_UPPER_RIGHT = (694000, 400000)  # Upper-right corner (X, Y)
GRID_SPACING = 25  # Grid cell size in feet

# Compute grid dimensions
Nx = int((GRID_UPPER_RIGHT[0] - GRID_ORIGIN[0]) / GRID_SPACING) + 1
Ny = int((GRID_UPPER_RIGHT[1] - GRID_ORIGIN[1]) / GRID_SPACING) + 1
x_grid = np.linspace(GRID_ORIGIN[0], GRID_UPPER_RIGHT[0], Nx)
y_grid = np.linspace(GRID_ORIGIN[1], GRID_UPPER_RIGHT[1], Ny)

# Read transect data from a fixed-width file
def read_transects_from_file(file_path):
    df = pd.read_fwf(file_path, colspecs=COLUMN_SPECS, names=COLUMN_NAMES, dtype=DATA_TYPES, skiprows=3)
    transect_data = []
    
    for line_name, group in df.groupby("LineName"):
        transect_x = (group["X"].values - GRID_ORIGIN[0]) / GRID_SPACING  # Convert to grid index
        transect_y = (group["Y"].values - GRID_ORIGIN[1]) / GRID_SPACING  # Convert to grid index
        transect_z = group["Z"].values
        transect_data.append((transect_x, transect_y, transect_z))
    
    return transect_data

# Radial Basis Function (RBF) interpolation
def rbf_interpolation(x, y, transect_data, function='multiquadric', epsilon=10, smooth=0.1):
    known_x, known_y, known_z = [], [], []
    
    for transect_x, transect_y, transect_z in transect_data:
        known_x.extend(transect_x)
        known_y.extend(transect_y)
        known_z.extend(transect_z)
    
    known_x = np.array(known_x)
    known_y = np.array(known_y)
    known_z = np.array(known_z)
    
    # Radial Basis Function interpolation
    rbf = Rbf(known_x, known_y, known_z, function=function, epsilon=epsilon, smooth=smooth)
    
    grid_x, grid_y = np.meshgrid(np.arange(Nx), np.arange(Ny))
    reconstructed_elevation = rbf(grid_x, grid_y)
    
    return reconstructed_elevation

# Export the reconstructed elevation to ZMAP format
def export_to_zmap(file_path, elevation_grid):
    with open(file_path, 'w') as f:
        f.write(f'! ZMAP FORMATTED GRID FILE\n')
        f.write(f'{Nx} {Ny}\n')
        f.write(f'{GRID_ORIGIN[0]} {GRID_UPPER_RIGHT[0]} {GRID_ORIGIN[1]} {GRID_UPPER_RIGHT[1]}\n')
        f.write(f'{np.min(elevation_grid)} {np.max(elevation_grid)}\n')
        
        for j in range(Ny):  # Column-major order
            for i in range(Nx):
                real_x = GRID_ORIGIN[0] + i * GRID_SPACING  # Convert back to NAD 27 X
                real_y = GRID_ORIGIN[1] + j * GRID_SPACING  # Convert back to NAD 27 Y
                f.write(f'{real_x:.1f} {real_y:.1f} {elevation_grid[j, i]:.3f}\n')

# Main function to read, display, and interpolate transects
def main():
    file_path = "/home/chuck/NiobraraAuto.txt"  # Path to fixed-width file
    output_zmap_file = "/data2/home/data/ODData/Vector/Export/Niobrara3dAuto.zmap"  # Output ZMAP file
    
    transect_data = read_transects_from_file(file_path)
    
    fig, ax = plt.subplots(figsize=(8, 6))
    
    # Plot transects in reprojected grid coordinates
    for transect_x, transect_y, _ in transect_data:
        ax.plot(transect_x, transect_y, 'r-', linewidth=1)
    
    ax.set_title('Transects in Grid-Aligned Coordinate System')
    ax.set_xlabel('Grid X Index')
    ax.set_ylabel('Grid Y Index')
    ax.set_xlim([0, Nx - 1])
    ax.set_ylim([0, Ny - 1])
    ax.grid(True)
    
    # Perform RBF interpolation
    rbf_function = 'multiquadric'  # Options: 'multiquadric', 'inverse', 'gaussian', 'linear', etc.
    rbf_epsilon = 1  # Controls influence of points
    rbf_smooth = 10  # Regularization to allow deviation from exact fit
    rbf_elevation = rbf_interpolation(x_grid, y_grid, transect_data, function=rbf_function, epsilon=rbf_epsilon, smooth=rbf_smooth)
    
    # Export reconstructed elevation to ZMAP format
    export_to_zmap(output_zmap_file, rbf_elevation)
    # print(f'Reconstructed elevation saved to {output_zmap_file}')
    
    fig, ax2 = plt.subplots(figsize=(8, 6))
    c = ax2.contourf(x_grid, y_grid, rbf_elevation, levels=20, cmap='terrain')
    ax2.set_title('RBF Reconstructed Elevation')
    fig.colorbar(c, ax=ax2)
    
    plt.show()

if __name__ == "__main__":
    main()
