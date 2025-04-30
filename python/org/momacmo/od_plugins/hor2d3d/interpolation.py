import numpy as np
from scipy.interpolate import Rbf, griddata
from abc import ABC, abstractmethod


class InterpolationMethod(ABC):
    @abstractmethod
    def interpolate(self, gx, gy, z, nx: int, ny: int, x0: float, dx: float, y0: float, dy: float) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
        pass


class RBFInterpolation(InterpolationMethod):
    def __init__(self, function='linear', epsilon=10, smooth=50):
        self.function = function
        self.epsilon = epsilon
        self.smooth = smooth

    def interpolate(self, gx, gy, z, nx, ny, x0, dx, y0, dy):
        gx_axis = x0 + np.arange(nx, dtype=np.float32) * dx
        gy_axis = y0 + np.arange(ny, dtype=np.float32) * dy
        gx_mesh, gy_mesh = np.meshgrid(gx_axis, gy_axis)

        rbf = Rbf(gx, gy, z, function=self.function, epsilon=self.epsilon, smooth=self.smooth)
        zgrid = rbf(gx_mesh, gy_mesh)
        return gy_axis, gx_axis, zgrid


class GriddataNearestInterpolation(InterpolationMethod):
    def interpolate(self, gx, gy, z, nx, ny, x0, dx, y0, dy):
        gx_axis = x0 + np.arange(nx, dtype=np.float32) * dx
        gy_axis = y0 + np.arange(ny, dtype=np.float32) * dy
        gx_mesh, gy_mesh = np.meshgrid(gx_axis, gy_axis)

        points = np.column_stack((gx, gy))
        grid_points = np.column_stack((gx_mesh.ravel(), gy_mesh.ravel()))
        zgrid = griddata(points, z, grid_points, method='nearest')
        return gy_axis, gx_axis, zgrid.reshape(ny, nx)


class GriddataLinearInterpolation(InterpolationMethod):
    def interpolate(self, gx, gy, z, nx, ny, x0, dx, y0, dy):
        gx_axis = x0 + np.arange(nx, dtype=np.float32) * dx
        gy_axis = y0 + np.arange(ny, dtype=np.float32) * dy
        gx_mesh, gy_mesh = np.meshgrid(gx_axis, gy_axis)

        points = np.column_stack((gx, gy))
        grid_points = np.column_stack((gx_mesh.ravel(), gy_mesh.ravel()))

        zgrid_linear = griddata(points, z, grid_points, method='linear')
        zgrid_nearest = griddata(points, z, grid_points, method='nearest')

        mask = np.isnan(zgrid_linear)
        zgrid_linear[mask] = zgrid_nearest[mask]

        return gy_axis, gx_axis, zgrid_linear.reshape(ny, nx)


def get_interpolator(name: str, **kwargs) -> InterpolationMethod:
    name = name.lower()
    if name == 'rbf':
        return RBFInterpolation(**kwargs)
    elif name == 'linear':
        return GriddataLinearInterpolation()
    elif name == 'nearest':
        return GriddataNearestInterpolation()
    else:
        raise ValueError(f"Unknown interpolation method: {name}")


def perform_interpolation(method: InterpolationMethod, gx, gy, z, nx: int, ny: int, x0: float, dx: float, y0: float, dy: float):
    return method.interpolate(gx, gy, z, nx, ny, x0, dx, y0, dy)


# Test driver
if __name__ == "__main__":
    import matplotlib.pyplot as plt

    def generate_sample_data():
        rng = np.random.default_rng(seed=42)
        gx = rng.uniform(100, 300, size=500)
        gy = rng.uniform(200, 500, size=500)
        z = np.sin(gx / 50) + np.cos(gy / 70) + rng.normal(0, 0.1, size=500)
        return gx, gy, z

    nx, ny = 400, 400
    x0, dx = 0.0, 1.0
    y0, dy = 0.0, 1.0

    gx, gy, z = generate_sample_data()

    methods = {
        "nearest": get_interpolator("nearest"),
        "linear": get_interpolator("linear"),
        "rbf": get_interpolator("rbf", function='multiquadric', epsilon=1, smooth=0.1)
    }

    fig, axes = plt.subplots(1, 3, figsize=(18, 6))
    for ax, (name, method) in zip(axes, methods.items()):
        gy_axis, gx_axis, zgrid = perform_interpolation(method, gx, gy, z, nx, ny, x0, dx, y0, dy)
        gx_grid, gy_grid = np.meshgrid(gx_axis, gy_axis)
        contour = ax.contourf(gx_grid, gy_grid, zgrid, levels=40, cmap='terrain')
        ax.set_title(f"{name.upper()} Interpolation")
        ax.set_xlabel("Grid X")
        ax.set_ylabel("Grid Y")
        ax.set_aspect('equal')

    plt.colorbar(contour, ax=axes, location='right', shrink=0.8, label='Z')
    plt.suptitle("Interpolation Method Comparison", fontsize=14)
    plt.tight_layout()
    plt.show()
