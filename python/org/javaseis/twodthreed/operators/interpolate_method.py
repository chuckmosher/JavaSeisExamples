import numpy as np
from scipy.interpolate import Rbf, griddata
from scipy.sparse import lil_matrix
from scipy.sparse.linalg import lsqr
from abc import ABC, abstractmethod

class Grid:
    """Class representing a spatial grid for interpolation."""
    def __init__(self, origin: tuple, upper_right: tuple, spacing: float):
        self.origin = origin  # (X, Y) lower-left corner
        self.upper_right = upper_right  # (X, Y) upper-right corner
        self.spacing = spacing  # Grid spacing

        # Compute grid dimensions
        self.Nx = int((self.upper_right[0] - self.origin[0]) / self.spacing) + 1
        self.Ny = int((self.upper_right[1] - self.origin[1]) / self.spacing) + 1
        self.x_grid = np.linspace(self.origin[0], self.upper_right[0], self.Nx)
        self.y_grid = np.linspace(self.origin[1], self.upper_right[1], self.Ny)
        self.X_grid, self.Y_grid = np.meshgrid(self.x_grid, self.y_grid)

# Create grid instance
grid = Grid(origin=(680000, 380000), upper_right=(700000, 410000), spacing=25)

class InterpolationMethod(ABC):
    """Abstract base class for interpolation methods."""
    @abstractmethod
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        pass

class RBFInterpolation(InterpolationMethod):
    """Radial Basis Function interpolation."""
    def __init__(self, function='multiquadric', epsilon=10, smooth=0.1):
        self.function = function
        self.epsilon = epsilon
        self.smooth = smooth
    
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        rbf = Rbf(x, y, z, function=self.function, epsilon=self.epsilon, smooth=self.smooth)
        return rbf(grid.X_grid, grid.Y_grid)

class RBFTPSInterpolation(InterpolationMethod):
    """Thin Plate Spline interpolation using RBF with TPS function."""
    def __init__(self, smoothness=0.1):
        self.smoothness = smoothness
    
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        rbf_tps = Rbf(x, y, z, function='thin_plate', smooth=self.smoothness)
        return rbf_tps(grid.X_grid, grid.Y_grid)

class MCGInterpolation(InterpolationMethod):
    """Minimum Curvature Gridding """
    
    def __init__(self, weight=1):
        self.weight = weight
    
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        ny, nx = grid.Ny, grid.Nx
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
        for tx, ty, tz in zip(x, y, z):
            ix = np.digitize(tx, grid.x_grid) - 1
            iy = np.digitize(ty, grid.y_grid) - 1
            if 0 <= ix < nx and 0 <= iy < ny:
                idx = index(ix, iy)
                A[idx, idx] += self.weight  # Weighted constraint
                b[idx] += self.weight * tz

        # Convert to sparse matrix and solve using least squares
        A = A.tocsr()
        elevation_grid = lsqr(A, b)[0].reshape(ny, nx)
        return elevation_grid

class GriddataLinearInterpolation(InterpolationMethod):
    """Linear interpolation using scipy's griddata (similar to bilinear on irregular grids)."""
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        points = np.column_stack((x, y))
        values = z
        xi = np.column_stack((grid.X_grid.ravel(), grid.Y_grid.ravel()))
        zi = griddata(points, values, xi, method='linear')
        return zi.reshape(grid.Y_grid.shape)

class GriddataNearestInterpolation(InterpolationMethod):
    """Nearest neighbor interpolation using scipy's griddata."""
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        points = np.column_stack((x, y))
        values = z
        xi = np.column_stack((grid.X_grid.ravel(), grid.Y_grid.ravel()))
        zi = griddata(points, values, xi, method='nearest')
        return zi.reshape(grid.Y_grid.shape)

class GriddataCubicInterpolation(InterpolationMethod):
    """Cubic interpolation using scipy's griddata."""
    def interpolate(self, x, y, z, grid: Grid) -> np.ndarray:
        points = np.column_stack((x, y))
        values = z
        xi = np.column_stack((grid.X_grid.ravel(), grid.Y_grid.ravel()))
        zi = griddata(points, values, xi, method='cubic')
        return zi.reshape(grid.Y_grid.shape)

def perform_interpolation(method: InterpolationMethod, transect_data, grid: Grid):
    """Performs interpolation using the given method."""
    known_x, known_y, known_z = [], [], []

    for transect_x, transect_y, transect_z in transect_data:
        known_x.extend(transect_x)
        known_y.extend(transect_y)
        known_z.extend(transect_z)

    known_x = np.array(known_x)
    known_y = np.array(known_y)
    known_z = np.array(known_z)

    return method.interpolate(known_x, known_y, known_z, grid)