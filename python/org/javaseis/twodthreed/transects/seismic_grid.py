import numpy as np

class SeismicGrid:
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