import numpy as np
from scipy.sparse.linalg import LinearOperator

# Define Restriction Operator using Nearest Neighbor Selection
class RestrictionOperatorNearest(LinearOperator):
    def __init__(self, grid_x, grid_y, sample_x, sample_y):
        self.grid_x, self.grid_y = np.asarray(grid_x), np.asarray(grid_y)
        self.sample_x, self.sample_y = np.asarray(sample_x), np.asarray(sample_y)
        self.n_samples = self.sample_x.size
        self.n_grid = self.grid_x.size * self.grid_y.size
        dtype = np.float32
        shape = (self.n_samples, self.n_grid)
        super().__init__(dtype=dtype, shape=shape)
    
    def _matvec(self, Z):
        Z = Z.reshape(len(self.grid_y), len(self.grid_x))
        d = np.zeros(self.n_samples)
        for i in range(self.n_samples):
            iy = np.searchsorted(self.grid_y, self.sample_y[i]) - 1
            ix = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            iy = np.clip(iy, 0, len(self.grid_y) - 1)
            ix = np.clip(ix, 0, len(self.grid_x) - 1)
            d[i] = Z[iy, ix]
        return d
    
    def _rmatvec(self, d):
        Z_adjoint = np.zeros((len(self.grid_y), len(self.grid_x)))
        for i in range(self.n_samples):
            iy = np.searchsorted(self.grid_y, self.sample_y[i]) - 1
            ix = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            iy = np.clip(iy, 0, len(self.grid_y) - 1)
            ix = np.clip(ix, 0, len(self.grid_x) - 1)
            Z_adjoint[iy, ix] += d[i]
        return Z_adjoint.ravel()
    
    def dot_product_test(self):
        np.random.seed(42)
        x = np.random.randn(self.shape[1])
        y = np.random.randn(self.shape[0])
        lhs = np.dot(y, self._matvec(x))
        rhs = np.dot(self._rmatvec(y), x)
        relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
        print(f"Restriction Operator Dot Product Test - LHS: {lhs:.6e}, RHS: {rhs:.6e}")
        return relative_error

# Define Restriction Operator using linear interpolation along rows
class RestrictionOperatorLinear(LinearOperator):
    def __init__(self, grid_x, grid_y, sample_x, sample_y):
        self.grid_x, self.grid_y = np.asarray(grid_x), np.asarray(grid_y)
        self.sample_x, self.sample_y = np.asarray(sample_x), np.asarray(sample_y)
        self.n_samples = self.sample_x.size
        self.n_grid = self.grid_x.size * self.grid_y.size
        dtype = np.float32
        shape = (self.n_samples, self.n_grid)
        super().__init__(dtype=dtype, shape=shape)
    
    def dot_product_test(self):
        np.random.seed(42)
        x = np.random.randn(self.shape[1])
        y = np.random.randn(self.shape[0])
        lhs = np.dot(y, self._matvec(x))
        rhs = np.dot(self._rmatvec(y), x)
        relative_error = np.abs(lhs - rhs) / (np.abs(lhs) + np.abs(rhs))
        print(f"Restriction Operator Dot Product Test - LHS: {lhs:.6e}, RHS: {rhs:.6e}")
        return relative_error

    def _matvec(self, Z):
        Z = Z.reshape(len(self.grid_y), len(self.grid_x))
        d = np.zeros(self.n_samples)
        for i in range(self.n_samples):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = max(0, min(x1_idx + 1, len(self.grid_x) - 1))
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            z1, z2 = Z[iy, x1_idx], Z[iy, x2_idx]
            d[i] = z1 + (z2 - z1) * (self.sample_x[i] - x1) / (x2 - x1) if x1 != x2 else z1
        return d

    def _rmatvec(self, d):
        Z_adjoint = np.zeros((len(self.grid_y), len(self.grid_x)))
        for i in range(len(self.sample_x)):
            iy = np.argmin(np.abs(self.grid_y - self.sample_y[i]))
            x1_idx = np.searchsorted(self.grid_x, self.sample_x[i]) - 1
            x2_idx = max(0, min(x1_idx + 1, len(self.grid_x) - 1))
            x1, x2 = self.grid_x[x1_idx], self.grid_x[x2_idx]
            if x1 != x2:
                weight1 = (x2 - self.sample_x[i]) / (x2 - x1)
                weight2 = (self.sample_x[i] - x1) / (x2 - x1)
                Z_adjoint[iy, x1_idx] += d[i] * weight1
                Z_adjoint[iy, x2_idx] += d[i] * weight2
            else:
                Z_adjoint[iy, x1_idx] += d[i]
        return Z_adjoint.ravel()
