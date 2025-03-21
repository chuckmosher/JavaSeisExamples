import numpy as np
import json
import matplotlib.pyplot as plt
from scipy.interpolate import Rbf
from typing import List, Dict
from datetime import datetime

# Define grid parameters (feet)
GRID_ORIGIN = (680000, 380000)  # Lower-left corner (X, Y)
GRID_UPPER_RIGHT = (700000, 410000)  # Upper-right corner (X, Y)
GRID_SPACING = 25  # Grid cell size in feet

# Compute grid dimensions
Nx = int((GRID_UPPER_RIGHT[0] - GRID_ORIGIN[0]) / GRID_SPACING) + 1
Ny = int((GRID_UPPER_RIGHT[1] - GRID_ORIGIN[1]) / GRID_SPACING) + 1
x_grid = np.linspace(GRID_ORIGIN[0], GRID_UPPER_RIGHT[0], Nx)
y_grid = np.linspace(GRID_ORIGIN[1], GRID_UPPER_RIGHT[1], Ny)
X_grid, Y_grid = np.meshgrid(x_grid, y_grid)

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
    
    reconstructed_elevation = rbf(X_grid, Y_grid)
    
    return reconstructed_elevation

class TransectMetadata:
    """Class representing the metadata of seismic transects."""
    def __init__(self, num_transects: int, traces_per_transect: List[int], num_samples: int, xyz: List[np.ndarray]):
        self.num_transects: int = num_transects
        self.traces_per_transect: List[int] = traces_per_transect  # List with number of traces per transect
        self.num_samples: int = num_samples
        self.xyz: List[np.ndarray] = xyz  # List of NumPy arrays
    
    def to_dict(self) -> Dict[str, object]:
        return {
            "num_transects": self.num_transects,
            "traces_per_transect": self.traces_per_transect,
            "num_samples": self.num_samples,
            "xyz": [xyz.tolist() for xyz in self.xyz]
        }
    
    @staticmethod
    def from_dict(metadata_dict: Dict[str, object]) -> "TransectMetadata":
        return TransectMetadata(
            metadata_dict["num_transects"],
            metadata_dict["traces_per_transect"],
            metadata_dict["num_samples"],
            [np.array(arr) for arr in metadata_dict["xyz"]]
        )

class SeismicTransects:
    """Class for managing seismic transects with metadata and binary data."""
    def __init__(self, metadata: TransectMetadata, transect_data: List[np.ndarray]):
        self.metadata: TransectMetadata = metadata
        self.transect_data: List[np.ndarray] = transect_data  # List of NumPy arrays
    
    @staticmethod
    def load_from_files(metadata_file: str, data_file: str) -> "SeismicTransects":
        # Load metadata
        with open(metadata_file, "r") as f:
            metadata_dict: Dict[str, object] = json.load(f)
        metadata: TransectMetadata = TransectMetadata.from_dict(metadata_dict)
        
        # Load binary data
        transect_data: List[np.ndarray] = []
        with open(data_file, "rb") as f:
            for i in range(metadata.num_transects):
                transect: List[np.ndarray] = []
                for _ in range(metadata.traces_per_transect[i]):
                    trace: np.ndarray = np.fromfile(f, dtype=np.float32, count=metadata.num_samples)
                    transect.append(trace)
                transect_data.append(np.array(transect))
        
        return SeismicTransects(metadata, transect_data)
    
    def get_transect_samples(self, transect_index: int) -> np.ndarray:
        """Returns all sample values for a given transect."""
        if transect_index >= self.metadata.num_transects:
            raise IndexError("Transect index out of range.")
        return self.transect_data[transect_index]
    
    def get_xyz_for_transect(self, transect_index: int) -> np.ndarray:
        """Returns the XYZ coordinates for all traces in a given transect."""
        if transect_index >= self.metadata.num_transects:
            raise IndexError("Transect index out of range.")
        return self.metadata.xyz[transect_index]
    
    def get_sample_at(self, transect_index: int, trace_index: int, sample_index: int) -> float:
        """Returns a specific sample value from a given transect and trace."""
        if transect_index >= self.metadata.num_transects:
            raise IndexError("Transect index out of range.")
        if trace_index >= self.metadata.traces_per_transect[transect_index]:
            raise IndexError("Trace index out of range.")
        if sample_index >= self.metadata.num_samples:
            raise IndexError("Sample index out of range.")
        return float(self.transect_data[transect_index][trace_index][sample_index])
    
    def extract_elevation_profile(self, sample_index: int):
        """Extracts elevation data for a given sample index across all transects."""
        known_x, known_y, known_z = [], [], []
        
        for transect_index in range(self.metadata.num_transects):
            transect_xyz = self.metadata.xyz[transect_index]  # Get XYZ coordinates
            transect_samples = self.transect_data[transect_index]  # Get sample data
            
            if sample_index >= transect_samples.shape[1]:
                continue  # Skip if sample index is out of bounds
            
            known_x.extend(transect_xyz[:, 0])
            known_y.extend(transect_xyz[:, 1])
            known_z.extend(transect_samples[:, sample_index])
        
        return np.array(known_x), np.array(known_y), np.array(known_z)
      
    def save_metadata(self, metadata_file: str) -> None:
        """Saves metadata to a JSON file."""
        with open(metadata_file, "w") as f:
            json.dump(self.metadata.to_dict(), f, indent=4)
    
    def save_data(self, data_file: str) -> None:
        """Saves transect data to a binary file."""
        with open(data_file, "wb") as f:
            for i in range(self.metadata.num_transects):
                for trace in self.transect_data[i]:
                    trace.tofile(f)
    
    def summarize(self) -> None:
        """Prints a summary of the transect dataset."""
        print(f"Number of Transects: {self.metadata.num_transects}")
        print(f"Number of Samples per trace: {self.metadata.num_samples}")
        print("Traces per transect:", self.metadata.traces_per_transect)
    
    
    def plot_transects_on_map(self) -> None:
        """Plot transect locations in map view."""
        fig, ax = plt.subplots(figsize=(10, 6))
        for transect_index in range(self.metadata.num_transects):
            transect_xyz = self.get_xyz_for_transect(transect_index)
            ax.plot(transect_xyz[:, 0], transect_xyz[:, 1], 'ro-', markersize=5)  # Plotting the XY locations
        ax.set_title("Transect Locations on Map")
        ax.set_xlabel("X Coordinate (ft)")
        ax.set_ylabel("Y Coordinate (ft)")
        plt.show()
    
    def plot_seismic_data(self, transect_index: int) -> None:
        """Plot seismic data for a given transect in grayscale."""
        seismic_data = self.get_transect_samples(transect_index)
        fig, ax = plt.subplots(figsize=(10, 6))
        ax.imshow(seismic_data.T, aspect='auto', cmap='gray', origin='lower', extent=[0, seismic_data.shape[1], 0, seismic_data.shape[0]])
        ax.set_title(f"Seismic Data for Transect {transect_index}")
        ax.set_xlabel("Time (samples)")
        ax.set_ylabel("Traces")
        plt.show()

# Example usage
if __name__ == "__main__":
    seismic_transects: SeismicTransects = SeismicTransects.load_from_files("/home/chuck/test.json", "/home/chuck/test.bin")
    seismic_transects.summarize()
    seismic_transects.plot_transects_on_map()
    
    num_samples = seismic_transects.metadata.num_samples
    num_samples = 2
    interpolated_grid = np.zeros((Ny, Nx, num_samples), dtype=np.float32)
    print("Begin interpolation at: ",datetime.now().strftime("%H:%M:%S"))
    for sample_index in range(num_samples):
        known_x, known_y, known_z = seismic_transects.extract_elevation_profile(sample_index + 450)
        if known_x.size == 0:
            continue  # Skip if no data available for this sample
        
        print("Sample: ",sample_index," at ",datetime.now().strftime("%H:%M:%S"))
        rbf = Rbf(known_x, known_y, known_z, function='multiquadric', epsilon=10, smooth=0.1)
        interpolated_grid[:, :, sample_index] = rbf(X_grid, Y_grid)
    
    print("Interpolation complete. 3D grid shape:", interpolated_grid.shape)
    
    # Optional: plot a slice of the grid at a given time sample
    sample_index = 0
    plt.figure(figsize=(10, 6))
    plt.imshow(interpolated_grid[:, :, sample_index], extent=[GRID_ORIGIN[0], GRID_UPPER_RIGHT[0], GRID_ORIGIN[1], GRID_UPPER_RIGHT[1]],
               aspect='auto', cmap='viridis', origin='lower')
    plt.colorbar(label='Elevation (m)')
    plt.title(f'Interpolated Elevation Map at Sample {sample_index}')
    plt.xlabel('X Coordinate (ft)')
    plt.ylabel('Y Coordinate (ft)')
    plt.show()
