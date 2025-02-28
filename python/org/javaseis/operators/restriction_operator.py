import numpy as np
import pylops

class RestrictionOperator(pylops.LinearOperator):
    def __init__(self, grid_x, grid_y, sample_x, sample_y, use_gpu=False):
        """
        Restriction operator that restricts the dense elevation model Z to sample points using 1D linear interpolation along rows.
        
        Args:
            grid_x, grid_y: 1D arrays defining the uniform grid.
            sample_x, sample_y: 1D arrays of sparse sample locations.
            use_gpu (bool): Whether to use CuPy for GPU acceleration (if available).
        """
        self.grid_x, self.grid_y = np.asarray(grid_x), np.asarray(grid_y)
        self.sample_x, self.sample_y = np.asarray(sample_x), np.asarray(sample_y)
        
        self.n_samples = self.sample_x.size
        self.n_grid = self.grid_x.size * self.grid_y.size
        
        dtype = np.float32
        shape = (self.n_samples, self.n_grid)
        super().__init__(dtype=dtype, shape=shape)

    def _matvec(self, Z):
        """Forward operation: Restrict dense Z to sampled locations using 1D linear interpolation along rows."""
        Z = Z.reshape(len(self.grid_y), len(self.grid_x))
        d = np.zeros(self.n_samples)
        for i in range(self.n_samples):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = x1_idx + 1
            x1_idx = max(0, min(x1_idx, len(self.grid_x) - 1))
            x2_idx = max(0, min(x2_idx, len(self.grid_x) - 1))
            
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            z1, z2 = Z[iy, x1_idx], Z[iy, x2_idx]
            
            if x1 == x2:
                d[i] = z1
            else:
                d[i] = z1 + (z2 - z1) * (self.sample_x[i] - x1) / (x2 - x1)
        
        return d

    def _rmatvec(self, d):
        """Adjoint operation: Spread sampled values back onto the grid using linear interpolation along rows."""
        Z_adjoint = np.zeros((len(self.grid_y), len(self.grid_x)))
        
        for i in range(len(self.sample_x)):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = x1_idx + 1
            x1_idx = max(0, min(x1_idx, len(self.grid_x) - 1))
            x2_idx = max(0, min(x2_idx, len(self.grid_x) - 1))
            
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            
            if x1 != x2:
                weight1 = (x2 - self.sample_x[i]) / (x2 - x1)
                weight2 = (self.sample_x[i] - x1) / (x2 - x1)
                Z_adjoint[iy, x1_idx] += d[i] * weight1
                Z_adjoint[iy, x2_idx] += d[i] * weight2
            else:
                Z_adjoint[iy, x1_idx] += d[i]
        
        return Z_adjoint.ravel()
