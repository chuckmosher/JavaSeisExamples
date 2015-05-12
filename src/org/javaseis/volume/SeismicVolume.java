package org.javaseis.volume;

import org.javaseis.grid.BinGrid;
import org.javaseis.grid.GridDefinition;
import org.javaseis.properties.AxisDefinition;

import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.regulargrid.IRegularGrid;
import beta.javaseis.regulargrid.OrientationType;
import beta.javaseis.regulargrid.RegularGrid;

public class SeismicVolume implements ISeismicVolume, IRegularGrid {

  GridDefinition globalGridDefinition, volumeGridDefinition;

  IRegularGrid volumeGrid;

  DistributedArray volume;

  public SeismicVolume(IParallelContext pc, GridDefinition grid) {
    globalGridDefinition = grid;
    AxisDefinition[] axis = new AxisDefinition[3];
    int[] volumeShape = new int[3];
    for (int i = 0; i < 3; i++) {
      axis[i] = globalGridDefinition.getAxis(i);
      volumeShape[i] = (int) axis[i].getLength();
    }
    volumeGridDefinition = new GridDefinition(3, axis);
    volume = new DistributedArray(pc, volumeShape);
    volumeGrid = new RegularGrid(volume, volumeGridDefinition,
        BinGrid.simpleBinGrid(volumeShape[1], volumeShape[2]));
  }

  public SeismicVolume(IParallelContext pc, GridDefinition grid, BinGrid binGrid) {
    globalGridDefinition = grid;
    AxisDefinition[] axis = new AxisDefinition[3];
    int[] volumeShape = new int[3];
    for (int i = 0; i < 3; i++) {
      axis[i] = globalGridDefinition.getAxis(i);
      volumeShape[i] = (int) axis[i].getLength();
    }
    volumeGridDefinition = new GridDefinition(3, axis);
    volume = new DistributedArray(pc, volumeShape);
    volumeGrid = new RegularGrid(volume, volumeGridDefinition, binGrid);
  }

  public DistributedArray getDistributedArray() {
    return volume;
  }

  public OrientationType getOrientation() {
    return volumeGrid.getOrientation();
  }

  public int getNumDimensions() {
    return volumeGrid.getNumDimensions();
  }

  public int[] getLengths() {
    return volumeGrid.getLengths();
  }

  public int[] getLocalLengths() {
    return volumeGrid.getLocalLengths();
  }

  public double[] getDeltas() {
    return volumeGrid.getDeltas();
  }

  public boolean isPositionLocal(int[] position) {
    return volumeGrid.isPositionLocal(position);
  }

  public float getSample(int[] position) {
    return volumeGrid.getSample(position);
  }

  public float getFloat(int[] position) {
    return volumeGrid.getFloat(position);
  }

  public int getInt(int[] position) {
    return volumeGrid.getInt(position);
  }

  public double getDouble(int[] position) {
    return volumeGrid.getDouble(position);
  }

  public void putSample(float val, int[] position) {
    volumeGrid.putSample(val, position);
  }

  public void putSample(double val, int[] position) {
    volumeGrid.putSample(val, position);
  }

  public int localToGlobal(int dimension, int index) {
    return volumeGrid.localToGlobal(dimension, index);
  }

  public int globalToLocal(int dimension, int index) {
    return volumeGrid.globalToLocal(dimension, index);
  }

  public int[] localPosition(int[] pos) {
    return volumeGrid.localPosition(pos);
  }

  public void worldCoords(int[] pos, double[] wxyz) {
    volumeGrid.worldCoords(pos, wxyz);
  }

  public IRegularGrid createCopy() {
    return volumeGrid.createCopy();
  }

  public void copyVolume(ISeismicVolume source) {
    if (!source.matches(this))
      throw new IllegalArgumentException(
          "Source volume and this volume do not match");
    this.getDistributedArray().copy(source.getDistributedArray());
  }

  @Override
  public GridDefinition getGlobalGrid() {
    return globalGridDefinition;
  }

  @Override
  public boolean matches(ISeismicVolume seismicVolume) {
    return globalGridDefinition.matches(seismicVolume.getGlobalGrid());
  }
}
