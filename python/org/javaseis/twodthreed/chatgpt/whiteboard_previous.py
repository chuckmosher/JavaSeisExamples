import numpy as np
import json
import matplotlib.pyplot as plt
from typing import List, Dict

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
    def __init__(self, metadata: TransectMetadata, transect_data: List[List[np.ndarray]]):
        self.metadata: TransectMetadata = metadata
        self.transect_data: List[List[np.ndarray]] = transect_data  # List of lists of NumPy arrays
    
    @staticmethod
    def load_from_files(metadata_file: str, data_file: str) -> "SeismicTransects":
        # Load metadata
        with open(metadata_file, "r") as f:
            metadata_dict: Dict[str, object] = json.load(f)
        metadata: TransectMetadata = TransectMetadata.from_dict(metadata_dict)
        
        # Load binary data
        transect_data: List[List[np.ndarray]] = []  # Store each transect as a list of NumPy arrays
        with open(data_file, "rb") as f:
            for i in range(metadata.num_transects):
                transect: List[np.ndarray] = []
                for _ in range(metadata.traces_per_transect[i]):
                    trace: np.ndarray = np.fromfile(f, dtype=np.float32, count=metadata.num_samples)
                    transect.append(trace)
                transect_data.append(transect)  # Store as a list instead of np.array()
        
        return SeismicTransects(metadata, transect_data)
    
    def get_transect_samples(self, transect_index: int) -> np.ndarray:
        """Returns all sample values for a given transect as a 2D NumPy array."""
        if transect_index >= self.metadata.num_transects:
            raise IndexError("Transect index out of range.")
        
        num_traces = self.metadata.traces_per_transect[transect_index]
        num_samples = self.metadata.num_samples
        
        samples_array = np.zeros((num_traces, num_samples), dtype=np.float32)
        
        for trace_index in range(num_traces):
            for sample_index in range(num_samples):
                samples_array[trace_index, sample_index] = self.get_sample_at(transect_index, trace_index, sample_index)
        
        return samples_array
    
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

        # Ensure the trace is not empty
        trace_data = self.transect_data[transect_index][trace_index]
        if trace_data.size == 0:
            raise ValueError(f"Trace {trace_index} in transect {transect_index} is empty.")

        return float(trace_data[sample_index])
    
    def plot_seismic_data(self, transect_index: int) -> None:
        """Plot seismic data for a given transect in grayscale."""
        seismic_data = self.get_transect_samples(transect_index)  # Get structured 2D array
        fig, ax = plt.subplots(figsize=(10, 6))
        ax.imshow(seismic_data.T, aspect='auto', cmap='gray', origin='lower',
                  extent=[0, seismic_data.shape[0], 0, seismic_data.shape[1]])
        ax.set_title(f"Seismic Data for Transect {transect_index}")
        ax.set_xlabel("Time (samples)")
        ax.set_ylabel("Traces")
        plt.show()
    
    def plot_transects_on_map(self) -> None:
        """Plot transect locations in map view."""
        fig, ax = plt.subplots(figsize=(10, 6))
        for transect_index in range(self.metadata.num_transects):
            transect_xyz = self.get_xyz_for_transect(transect_index)
            ax.plot(transect_xyz[:, 0], transect_xyz[:, 1], 'ro-', markersize=5)  # Plot XY locations
        ax.set_title("Transect Locations on Map")
        ax.set_xlabel("X Coordinate (ft)")
        ax.set_ylabel("Y Coordinate (ft)")
        plt.show()
    
    def summarize(self) -> None:
        """Prints a summary of the transect dataset."""
        print(f"Number of Transects: {self.metadata.num_transects}")
        print(f"Number of Samples per trace: {self.metadata.num_samples}")
        print("Traces per transect:", self.metadata.traces_per_transect)

# Example usage
if __name__ == "__main__":
    seismic_transects: SeismicTransects = SeismicTransects.load_from_files("/home/chuck/test.json", "/home/chuck/test.bin")
    seismic_transects.summarize()
    seismic_transects.plot_transects_on_map()  # Plot transects on map view
    seismic_transects.plot_seismic_data(0)  # Plot seismic data for transect 0
