from opencps.tool import *
import numpy as np
from scipy.ndimage import sobel, gaussian_filter1d
from scipy.special import expit
from scipy.signal import butter, filtfilt

class Tool(basetool):
    def update(self):
        self.label = "Fault Probability (Tensor)"

        self.sigma = getFloatParam("sigma", self.locals, 5.0)
        self.hwin  = getFloatParam("hwin",  self.locals, 5.0)
        self.vwin  = getFloatParam("vwin",  self.locals, 5.0)

        self.n1Out = self.n1In
        self.o1Out = self.o1In
        self.d1Out = self.d1In

        self.n2Out = self.n2In
        self.o2Out = self.o2In
        self.d2Out = self.d2In

    def startExecution(self):
        pass  # no persistent buffers needed

    def execute(self, ntr, hds, trs):
        section = np.copy(trs[:ntr, :self.n1In])

        # Optional pre-filtering (horizontal smoothing)
        section = gaussian_filter1d(section, sigma=5.0, axis=1)

        # Compute tensor-based coherence
        gx = sobel(section, axis=1, mode='reflect')
        gy = sobel(section, axis=0, mode='reflect')

        Jxx = gaussian_filter1d(gx * gx, sigma=self.hwin, axis=1)
        Jxy = gaussian_filter1d(gx * gy, sigma=self.hwin, axis=1)
        Jyy = gaussian_filter1d(gy * gy, sigma=self.vwin, axis=0)

        trace = Jxx + Jyy
        det = Jxx * Jyy - Jxy**2

        coherence = np.zeros_like(section)
        mask = trace > 0
        coherence[mask] = 1.0 - 4.0 * det[mask] / (trace[mask]**2 + 1e-12)

        fault_prob = expit(self.sigma * (coherence - np.mean(coherence)))
        trs[:ntr, :self.n1In] = fault_prob
        return ntr
