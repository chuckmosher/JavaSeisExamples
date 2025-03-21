import numpy as np
import matplotlib.pyplot as plt
from org.javaseis.twodthreed.synthdata import generate_elevation

# Generate random transects
def generate_transects(num_transects=21, percent_length=0.75, grid_shape=(101, 101), spacing=10, seed=42):
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

def main():
    shape = (101, 101)
    spacing = 10
    x, y, elevation = generate_elevation(shape, spacing)
    
    # Generate and plot transects
    transects = generate_transects(num_transects=21, percent_length=0.75, grid_shape=shape, spacing=spacing)
    plt.figure(figsize=(8, 6))
    plt.contourf(x, y, elevation, levels=50, cmap='terrain')
    plt.colorbar(label='Elevation')
    for tx, ty in transects:
        plt.plot(tx, ty, 'r-', linewidth=1.5)
    plt.xlabel('X (m)')
    plt.ylabel('Y (m)')
    plt.title('Elevation Map with Transects')
    plt.show()

if __name__ == "__main__":
    main()
