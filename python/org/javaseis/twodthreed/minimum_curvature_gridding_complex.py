import numpy as np
import matplotlib.pyplot as plt
import argparse
from scipy.interpolate import RegularGridInterpolator
from scipy.sparse import lil_matrix
from scipy.sparse.linalg import lsqr

# Generate magnetic field data with amplitude and phase
def generate_magnetic_data(shape, spacing):
    x = np.linspace(0, (shape[1] - 1) * spacing, shape[1])
    y = np.linspace(0, (shape[0] - 1) * spacing, shape[0])
    X, Y = np.meshgrid(x, y)
    amplitude = np.sin(5 * X / 1000) * np.cos(3 * Y / 1000) + 0.5 * np.sin(8 * X * Y / 1e6)
    phase = 0.2 * np.sin(2 * X / 1000) + 0.1 * np.cos(3 * Y / 2000)  # Slowly varying phase
    return x, y, amplitude * np.exp(1j * phase)

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

# Extract transect data for complex field
def extract_transect_data_complex(x, y, data, transects):
    interpolator_real = RegularGridInterpolator((y, x), data.real, method='linear', bounds_error=False, fill_value=np.nan)
    interpolator_imag = RegularGridInterpolator((y, x), data.imag, method='linear', bounds_error=False, fill_value=np.nan)
    
    transect_data = []
    for transect_x, transect_y in transects:
        points = np.vstack((transect_y, transect_x)).T
        transect_real = interpolator_real(points)
        transect_imag = interpolator_imag(points)
        transect_data.append((transect_x, transect_y, transect_real + 1j * transect_imag))
    
    return transect_data

# Minimum Curvature Gridding for scalar data
def minimum_curvature_gridding(x, y, transect_data, grid_shape, weight, atol, btol, iter_lim):
    ny, nx = grid_shape
    A = lil_matrix((nx * ny, nx * ny))
    b = np.zeros(nx * ny)
    
    def index(ix, iy):
        return iy * nx + ix
    
    for iy in range(1, ny - 1):
        for ix in range(1, nx - 1):
            idx = index(ix, iy)
            A[idx, index(ix, iy)] = -4
            A[idx, index(ix - 1, iy)] = 1
            A[idx, index(ix + 1, iy)] = 1
            A[idx, index(ix, iy - 1)] = 1
            A[idx, index(ix, iy + 1)] = 1
    
    for transect_x, transect_y, transect_z in transect_data:
        for tx, ty, tz in zip(transect_x, transect_y, transect_z):
            ix = np.digitize(tx, x) - 1
            iy = np.digitize(ty, y) - 1
            if 0 <= ix < nx and 0 <= iy < ny:
                idx = index(ix, iy)
                A[idx, idx] += weight
                b[idx] += weight * tz
    
    A = A.tocsr()
    elevation_grid = lsqr(A, b, atol=atol, btol=btol, iter_lim=iter_lim)[0].reshape(ny, nx)
    return elevation_grid

# Minimum Curvature Gridding for complex data
def minimum_curvature_gridding_complex(x, y, transect_data, grid_shape, weight, atol, btol, iter_lim):
    real_part = minimum_curvature_gridding(x, y, [(tx, ty, np.real(tz)) for tx, ty, tz in transect_data], grid_shape, weight, atol, btol, iter_lim)
    imag_part = minimum_curvature_gridding(x, y, [(tx, ty, np.imag(tz)) for tx, ty, tz in transect_data], grid_shape, weight, atol, btol, iter_lim)
    return real_part + 1j * imag_part

# Main function
def main():
    grid_shape = (101, 101)
    spacing = 10
    x, y, magnetic_data = generate_magnetic_data(grid_shape, spacing)
    transects = generate_transects(21, 0.75, grid_shape, spacing, 42)
    transect_data = extract_transect_data_complex(x, y, magnetic_data, transects)
    mcg_magnetic = minimum_curvature_gridding_complex(x, y, transect_data, grid_shape, weight=10, atol=1e-6, btol=1e-6, iter_lim=1000)
    
    true_amplitude = np.abs(magnetic_data)
    recon_amplitude = np.abs(mcg_magnetic)
    
    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    
    c1 = axes[0].contourf(x, y, true_amplitude, levels=20, cmap='viridis')
    axes[0].set_title('True Magnetic Field Amplitude')
    fig.colorbar(c1, ax=axes[0])
    
    c2 = axes[1].contourf(x, y, recon_amplitude, levels=20, cmap='viridis')
    axes[1].set_title('Reconstructed Magnetic Field Amplitude')
    fig.colorbar(c2, ax=axes[1])
    
    transect_x, _, transect_z = transect_data[0]
    transect_real = np.real(transect_z)
    transect_imag = np.imag(transect_z)
    
    recon_interp = RegularGridInterpolator((y, x), mcg_magnetic)
    recon_z = recon_interp(np.vstack((transect_x, transect_x)).T)
    recon_real = np.real(recon_z)
    recon_imag = np.imag(recon_z)
    
    axes[2].plot(transect_x, transect_real, 'r-', label='True Real Part')
    axes[2].plot(transect_x, transect_imag, 'b--', label='True Imaginary Part')
    axes[2].plot(transect_x, recon_real, 'r-.', label='Reconstructed Real Part')
    axes[2].plot(transect_x, recon_imag, 'b:', label='Reconstructed Imaginary Part')
    axes[2].set_title('Real and Imaginary Parts Along Transect')
    axes[2].legend()
    
    plt.show()

if __name__ == "__main__":
    main()
