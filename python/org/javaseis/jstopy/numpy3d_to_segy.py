import numpy as np
import segyio
import os

def write_3d_numpy_to_segy_with_coordinates(
    filename: str,
    data: np.ndarray,
    client: str = "MOMACMO LIMITED",
    sample_interval_us: int = 2000,
    inline_start: int = 1000,
    crossline_start: int = 1000,
    inline_step: int = 1,
    crossline_step: int = 1,
    origin: tuple = (0.0, 0.0),    # (y0, x0)
    spacing: tuple = (25.0, 25.0),  # (dy, dx)
    x_axis_azimuth: float = 90.0,  # angle in degrees east of north
    coord_scalar: int = -100,      # -100 means: divide stored values by 100
    projection: str = "UNKNOWN PROJECTION",
    provider: str = "MOMACMO LIMITED"
):
    """
    Write a 3D numpy array to a SEGY file using segyio, with spatial coordinates and configurable scalar.

    Parameters:
        filename (str): Output SEGY file path
        data (np.ndarray): 3D array with shape (n_inline, n_crossline, n_samples)
        client (str): Client for the data
        sample_interval_us (int): Sample interval in microseconds
        inline_start (int): Starting inline number
        crossline_start (int): Starting crossline number
        inline_step (int): Step between inlines
        crossline_step (int): Step between crosslines
        origin (tuple): (y0, x0) origin coordinates in map units (e.g. meters)
        spacing (tuple): (dy, dx) spacing in map units
        x_axis_azimuth (float): Azimuth of the crossline (X) direction in degrees east of true north
        coord_scalar (int): SEGY coordinate scalar (negative means divide)
        projection (str): Description of the projection system used (e.g. "NAD27 Wyoming East Central EPSG:32056")
        provider (str): Provider of the data
    """
    n_inline, n_crossline, n_samples = data.shape
    n_traces = n_inline * n_crossline
    flat_data = data.reshape(n_traces, n_samples)

    # Define SEGY spec
    spec = segyio.spec()
    spec.sorting = segyio.TraceSortingFormat.INLINE_SORTING
    spec.format = segyio.segysampleformat.SegySampleFormat.IEEE_FLOAT_4_BYTE
    spec.samples = np.arange(n_samples) * (sample_interval_us / 1e6)  # in seconds
    spec.tracecount = n_traces

    y0, x0 = origin
    dy, dx = spacing

    with segyio.create(filename, spec) as segyfile:
        trace_index = 0
        for i in range(n_inline):
            for j in range(n_crossline):
                inline = inline_start + i * inline_step
                crossline = crossline_start + j * crossline_step

                # Compute coordinates
                y = y0 + i * dy  # INLINE → Y
                x = x0 + j * dx  # CROSSLINE → X

                # Apply scalar (store as int)
                y_scaled = int(round(y * abs(coord_scalar)))
                x_scaled = int(round(x * abs(coord_scalar)))

                segyfile.header[trace_index] = {
                    segyio.TraceField.INLINE_3D: inline,
                    segyio.TraceField.CROSSLINE_3D: crossline,
                    segyio.TraceField.CDP_Y: y_scaled,
                    segyio.TraceField.CDP_X: x_scaled,
                    segyio.TraceField.SourceY: y_scaled,
                    segyio.TraceField.SourceX: x_scaled,
                    segyio.TraceField.GroupY: y_scaled,
                    segyio.TraceField.GroupX: x_scaled,
                    segyio.TraceField.SourceGroupScalar: coord_scalar,
                    segyio.TraceField.TRACE_SAMPLE_COUNT: n_samples,
                    segyio.TraceField.TRACE_SEQUENCE_LINE: trace_index + 1,
                }

                segyfile.trace[trace_index] = flat_data[trace_index]
                trace_index += 1

        segyfile.bin[segyio.BinField.Traces] = n_traces
        segyfile.bin[segyio.BinField.Samples] = n_samples
        segyfile.bin[segyio.BinField.Interval] = sample_interval_us

        # Add SEGY revision and format metadata
        segyfile.bin[segyio.BinField.Format] = 5  # IEEE float
        segyfile.bin[segyio.BinField.SEGYRevision] = 1
        segyfile.bin[segyio.BinField.SEGYRevisionMinor] = 0
        segyfile.bin[segyio.BinField.ExtendedHeaders] = 0

        # Add EBCDIC textual header
        ebcdic_lines = [
            "C 1 CLIENT: {}".format(client[:68]),
            "C 2 DATA WRITTEN WITH SEGYIO USING IEEE 4-BYTE FLOAT FORMAT",
            "C 3 DATA STORED AS INLINE/CROSSLINE ORDERING (3D VOLUME)",
            "C 4 INLINE_3D START: {} STEP: {}".format(inline_start, inline_step),
            "C 5 CROSSLINE_3D START: {} STEP: {}".format(crossline_start, crossline_step),
            "C 6 SAMPLE INTERVAL (MICROSECONDS): {}".format(sample_interval_us),
            "C 7 COORDINATE ORIGIN: Y0 = {:.3f}, X0 = {:.3f}".format(y0, x0),
            "C 8 COORDINATE SPACING: DY = {:.3f}, DX = {:.3f}".format(dy, dx),
            "C 9 COORDINATES SCALED BY: {} (NEGATIVE = DIVIDE)".format(coord_scalar),
            "C10 CDP_X/CDP_Y = SourceX/SourceY = GroupX/GroupY (ZERO-OFFSET)",
            "C11 PROJECTION: {}".format(projection[:66]),
            "C12 INLINE(X-AXIS) AZIMUTH (DEGREES EAST OF NORTH): {:.2f}".format(x_axis_azimuth),
            "C13 GENERATED BY: {}".format(provider[:62])
        ]
        while len(ebcdic_lines) < 40:
            ebcdic_lines.append("C{:2d} ".format(len(ebcdic_lines) + 1))
        segyfile.text[0] = segyio.tools.create_text_header(ebcdic_lines)

    print(f"SEGY file written to: {os.path.abspath(filename)}")


# Example usage
if __name__ == "__main__":
    cube = np.random.rand(10, 20, 500).astype(np.float32)
    write_3d_numpy_to_segy_with_coordinates(
        "output_with_coords_scalar.sgy",
        cube,
        origin=(710000.123, 465000.456),
        spacing=(25.0, 25.0),
        coord_scalar=-1000,  # store with 0.001m precision
        projection="NAD27 Wyoming East Central EPSG:32056",
        x_axis_azimuth=87.3
    )
