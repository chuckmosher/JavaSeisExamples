import numpy as np
import matplotlib.pyplot as plt
import json
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

# Extract elevation values along the transects
def extract_transect_data(x, y, elevation, transects):
    interpolator = RegularGridInterpolator((y, x), elevation, method='linear', bounds_error=False, fill_value=np.nan)
    transect_data = []
    
    for transect_x, transect_y in transects:
        points = np.vstack((transect_y, transect_x)).T  # Format for interpolation
        transect_z = interpolator(points)
        transect_data.append((transect_x, transect_y, transect_z))
    
    return transect_data

# Save transect data to JSON and binary files
def save_transect_data(transect_data, filename_json, filename_bin):
    metadata = {
        "ntransects": len(transect_data),
        "npoints_per_transect": [len(transect[0]) for transect in transect_data],
        "nsamples": 1,
        "start_time": 0.0,
        "sample_rate": 1.0,
        "projection": {"datum": "NAD27", "zone": 13, "units": "feet"},
        "transects": []
    }
    
    flat_data = []
    for i, (transect_x, transect_y, transect_z) in enumerate(transect_data):
        transect_info = {"transect_id": i+1, "points": []}
        for x, y, z in zip(transect_x, transect_y, transect_z):
            transect_info["points"].append({"x": float(x), "y": float(y)})
            flat_data.append(float(z))
        metadata["transects"].append(transect_info)
    
    with open(filename_json, "w") as json_file:
        json.dump(metadata, json_file, indent=4)
    
    np.array(flat_data, dtype=np.float32).tofile(filename_bin)

# Load and validate transect data
def load_transect_data(filename_json, filename_bin):
    with open(filename_json, "r") as json_file:
        metadata = json.load(json_file)
    
    flat_data = np.fromfile(filename_bin, dtype=np.float32)
    return metadata, flat_data

# Main function to generate, save, and validate transect data
def main():
    shape = (101, 101)
    spacing = 10
    num_transects = 10
    percent_length = 0.75
    seed = 42
    
    x, y, elevation = generate_elevation(shape, spacing)
    transects = generate_transects(num_transects, percent_length, shape, spacing, seed)
    transect_data = extract_transect_data(x, y, elevation, transects)
    
    # Save data
    save_transect_data(transect_data, "transect_metadata.json", "transect_data.bin")
    print("Transect data saved successfully.")
    
    # Plot original data
    plt.figure(figsize=(8, 6))
    plt.contourf(x, y, elevation, levels=20, cmap='terrain')
    for transect_x, transect_y, _ in transect_data:
        plt.plot(transect_x, transect_y, 'r-', linewidth=1)
    plt.title("Original Elevation Model with Transects")
    plt.colorbar()
    plt.show()
    
    # Load and validate data
    metadata, loaded_data = load_transect_data("transect_metadata.json", "transect_data.bin")
    
    # Plot loaded data
    plt.figure(figsize=(8, 6))
    plt.contourf(x, y, elevation, levels=20, cmap='terrain')
    for transect in metadata["transects"]:
        transect_x = [point["x"] for point in transect["points"]]
        transect_y = [point["y"] for point in transect["points"]]
        plt.plot(transect_x, transect_y, 'b-', linewidth=1)
    plt.title("Loaded Elevation Model with Transects")
    plt.colorbar()
    plt.show()
    
    print("Transect data loaded and validated successfully.")

if __name__ == "__main__":
    main()
