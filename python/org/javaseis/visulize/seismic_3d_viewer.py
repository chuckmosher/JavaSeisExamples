import sys
import numpy as np
from PyQt5 import QtWidgets, QtCore
import pyqtgraph as pg

class SeismicCubeViewer(QtWidgets.QWidget):
    def __init__(self, cube):
        super().__init__()
        self.cube = cube  # 3D numpy array [Y, X, T]
        self.ny, self.nx, self.nt = cube.shape

        # Initial slice indices
        self.slice_y = self.ny // 2
        self.slice_x = self.nx // 2
        self.slice_t = self.nt // 2

        self.init_ui()
        self.update_views()

    def init_ui(self):
        layout = QtWidgets.QGridLayout(self)

        # Image views
        self.view_time = pg.ImageView()
        self.view_inline = pg.ImageView()
        self.view_crossline = pg.ImageView()

        layout.addWidget(self.view_time, 0, 0)
        layout.addWidget(self.view_inline, 1, 0)
        layout.addWidget(self.view_crossline, 1, 1)

        # Sliders
        self.slider_y = QtWidgets.QSlider(QtCore.Qt.Horizontal)
        self.slider_y.setRange(0, self.ny - 1)
        self.slider_y.setValue(self.slice_y)
        self.slider_y.valueChanged.connect(self.set_y)

        self.slider_x = QtWidgets.QSlider(QtCore.Qt.Horizontal)
        self.slider_x.setRange(0, self.nx - 1)
        self.slider_x.setValue(self.slice_x)
        self.slider_x.valueChanged.connect(self.set_x)

        self.slider_t = QtWidgets.QSlider(QtCore.Qt.Horizontal)
        self.slider_t.setRange(0, self.nt - 1)
        self.slider_t.setValue(self.slice_t)
        self.slider_t.valueChanged.connect(self.set_t)

        layout.addWidget(self.slider_t, 0, 1)
        layout.addWidget(self.slider_y, 2, 0)
        layout.addWidget(self.slider_x, 2, 1)

        self.setLayout(layout)
        self.setWindowTitle("Seismic Cube Viewer")
        self.resize(1000, 800)

    def set_y(self, val):
        self.slice_y = val
        self.update_views()

    def set_x(self, val):
        self.slice_x = val
        self.update_views()

    def set_t(self, val):
        self.slice_t = val
        self.update_views()

    def update_views(self):
        self.view_time.setImage(self.cube[:, :, self.slice_t].T, autoLevels=False)
        self.view_inline.setImage(self.cube[self.slice_y, :, :].T, autoLevels=False)
        self.view_crossline.setImage(self.cube[:, self.slice_x, :].T, autoLevels=False)

if __name__ == '__main__':
    app = QtWidgets.QApplication(sys.argv)

    # Generate a test 3D cube: [Y, X, T]
    cube = np.random.rand(100, 120, 80).astype(np.float32)

    viewer = SeismicCubeViewer(cube)
    viewer.show()
    sys.exit(app.exec_())
