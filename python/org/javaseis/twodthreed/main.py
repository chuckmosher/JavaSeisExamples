import numpy as np
import matplotlib.pyplot as plt
from datetime import datetime

from scipy.ndimage import gaussian_filter
from org.javaseis.twodthreed.transects.seismic_transects import SeismicTransects
from org.javaseis.twodthreed.transects.seismic_grid import SeismicGrid
from org.javaseis.twodthreed.operators.interpolate_method import RBFInterpolation
from org.javaseis.twodthreed.operators.interpolate_method import GriddataLinearInterpolation
from org.javaseis.twodthreed.operators.interpolate_method import GriddataNearestInterpolation

def save_3d_grid_binary(filename: str, volume: np.ndarray):
    """
    Save a 3D NumPy array to a binary file in IEEE 754 little-endian float32 format.

    Parameters:
        filename (str): Path to output binary file.
        volume (np.ndarray): 3D NumPy array [Ny, Nx, Nt] to be saved.
    """
    if not isinstance(volume, np.ndarray) or volume.ndim != 3:
        raise ValueError("Input must be a 3D NumPy array.")

    # Ensure little-endian float32 format
    le_volume = volume.astype('<f4')  # < = little-endian, f4 = float32

    # Flatten in C order (Ny, Nx, Nt)
    le_volume.tofile(filename)

    print(f"3D volume saved to '{filename}' with shape {volume.shape}")

# Example usage
if __name__ == "__main__":
    seismic_transects: SeismicTransects = SeismicTransects.load_from_files("/home/chuck/Niobrara2dFprobV4.json", "/home/chuck/Niobrara2dFprobV4.bin")
    seismic_transects.summarize()
    seismic_transects.plot_transects_on_map()   
    seismic_transects.plot_seismic_data(3)
    
    num_samples = seismic_transects.metadata.num_samples
    
    GRID_ORIGIN = (681900, 383900)  # Lower-left corner (X, Y)
    GRID_UPPER_RIGHT = (695100, 401170)  # Upper-right corner (X, Y)
    GRID_SPACING = 55
    
    grid: SeismicGrid = SeismicGrid( GRID_ORIGIN, GRID_UPPER_RIGHT, GRID_SPACING)
    print("NT, NX, NY = ",num_samples,grid.Nx,grid.Ny)
    interpolated_grid = np.zeros((grid.Ny, grid.Nx, num_samples), dtype=np.float32)
    print("Begin interpolation at: ",datetime.now().strftime("%H:%M:%S"))
    
    for sample_index in range(num_samples):
        known_x, known_y, known_z = seismic_transects.extract_elevation_profile(sample_index)
        if known_x.size == 0:
            continue  # Skip if no data available for this sample
        
        print("Sample: ",sample_index," at ",datetime.now().strftime("%H:%M:%S"))
        # method = RBFInterpolation(epsilon=10,smooth=0.1)
        method = GriddataLinearInterpolation()
        interpolated_grid[:, :, sample_index] = gaussian_filter(
            method.interpolate(known_x, known_y, known_z, grid),10)
    
    print("Interpolation complete. 3D grid shape:", interpolated_grid.shape)
    save_3d_grid_binary("/home/chuck/grid.bin",interpolated_grid)
    
    # Optional: plot a slice of the grid at a given time sample
    sample_index = 125
    plt.figure(figsize=(10, 6))
    plt.imshow(interpolated_grid[:, :,sample_index], 
               extent=[GRID_ORIGIN[0], GRID_UPPER_RIGHT[0], GRID_ORIGIN[1], GRID_UPPER_RIGHT[1]],
               aspect='auto', cmap='viridis', origin='lower')
    plt.colorbar(label='Elevation (m)')
    plt.title(f'Interpolated Elevation Map at Sample {sample_index}')
    plt.xlabel('X Coordinate (ft)')
    plt.ylabel('Y Coordinate (ft)')
    plt.show()


