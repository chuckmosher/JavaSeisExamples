import numpy as np
import matplotlib.pyplot as plt
from org.javaseis.operators.restriction_operator import RestrictionOperatorNearest

def test_restriction_operator():
    grid_x = np.linspace(0, 100, 50)
    grid_y = np.linspace(0, 100, 50)
    X, Y = np.meshgrid(grid_x, grid_y)
    Z_true = np.sin(5 * X) * np.cos(3 * Y) + 0.5 * np.sin(8 * X * Y)

    sample_mask = np.random.rand(*Z_true.shape) < 0.5  # 50% subsampling
    sample_x, sample_y = X[sample_mask], Y[sample_mask]

    R = RestrictionOperatorNearest(grid_x, grid_y, sample_x, sample_y, use_gpu=False)
    d = R @ Z_true.ravel()
    Z_recon = R.T @ d

    plt.ion()
    plt.figure(figsize=(12, 8))
    plt.subplot(2, 2, 1)
    plt.imshow(Z_true, extent=[0, 100, 0, 100], origin='lower')
    plt.title("True Elevation Map")
    plt.colorbar()

    plt.subplot(2, 2, 2)
    plt.scatter(sample_x, sample_y, c=d, cmap='viridis')
    plt.title("Sampled Elevation Data")
    plt.colorbar()

    plt.subplot(2, 2, 3)
    plt.imshow(Z_recon.reshape(50, 50), extent=[0, 100, 0, 100], origin='lower')
    plt.title("Adjoint Reconstruction")
    plt.colorbar()

    plt.subplot(2, 2, 4)
    plt.imshow(Z_true - Z_recon.reshape(50, 50), extent=[0, 100, 0, 100], origin='lower')
    plt.title("Difference (Error)")
    plt.colorbar()
    plt.show(block=True)

if __name__ == "__main__":
    test_restriction_operator()
