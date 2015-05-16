package org.javaseis.volume;

import org.javaseis.array.ElementType;
import org.javaseis.grid.BinGrid;
import org.javaseis.grid.GridDefinition;
import org.javaseis.properties.AxisDefinition;

import beta.javaseis.distributed.Decomposition;
import beta.javaseis.distributed.DistributedArray;
import beta.javaseis.parallel.IParallelContext;
import beta.javaseis.regulargrid.IRegularGrid;
import beta.javaseis.regulargrid.OrientationType;

public class SeismicVolume implements ISeismicVolume, IRegularGrid {

  GridDefinition globalGrid, localGrid;

  IRegularGrid volumeGrid;

  BinGrid binGrid;

  DistributedArray volume;

  ElementType elementType;

  int elementCount;

  int decompType;

  int[] volumeShape;

  IParallelContext pc;

  public SeismicVolume(IParallelContext parallelContext, GridDefinition globalGridDefinition) {
    pc = parallelContext;
    globalGrid = globalGridDefinition;
    AxisDefinition[] axis = new AxisDefinition[3];
    int[] volumeShape = new int[3];
    for (int i = 0; i < 3; i++) {
      axis[i] = globalGrid.getAxis(i);
      volumeShape[i] = (int) axis[i].getLength();
    }
    localGrid = new GridDefinition(3, axis);
    volume = new DistributedArray(pc, volumeShape);
    binGrid = BinGrid.simpleBinGrid(volumeShape[1], volumeShape[2]);
    elementType = ElementType.FLOAT;
    elementCount = 1;
    decompType = Decomposition.BLOCK;
  }

  public SeismicVolume(IParallelContext parallelContext, GridDefinition globalGridDefinition,
      BinGrid binGridIn) {
    pc = parallelContext;
    AxisDefinition[] axis = new AxisDefinition[3];
    int[] volumeShape = new int[3];
    for (int i = 0; i < 3; i++) {
      axis[i] = globalGridDefinition.getAxis(i);
      volumeShape[i] = (int) axis[i].getLength();
    }
    localGrid = new GridDefinition(3, axis);
    binGrid = binGridIn;
  }

  public void allocate(long maxLength) {
    DistributedArray volume = new DistributedArray(pc, float.class, 3, elementCount, volumeShape, decompType,
        maxLength);
  }

  @Override
  public DistributedArray getDistributedArray() {
    return volume;
  }

  @Override
  public OrientationType getOrientation() {
    return volumeGrid.getOrientation();
  }

  @Override
  public int getNumDimensions() {
    return volumeGrid.getNumDimensions();
  }

  @Override
  public int[] getLengths() {
    return volumeGrid.getLengths();
  }

  @Override
  public int[] getLocalLengths() {
    return volumeGrid.getLocalLengths();
  }

  @Override
  public double[] getDeltas() {
    return volumeGrid.getDeltas();
  }

  @Override
  public boolean isPositionLocal(int[] position) {
    return volumeGrid.isPositionLocal(position);
  }

  @Override
  public float getSample(int[] position) {
    return volumeGrid.getSample(position);
  }

  @Override
  public float getFloat(int[] position) {
    return volumeGrid.getFloat(position);
  }

  @Override
  public int getInt(int[] position) {
    return volumeGrid.getInt(position);
  }

  @Override
  public double getDouble(int[] position) {
    return volumeGrid.getDouble(position);
  }

  @Override
  public void putSample(float val, int[] position) {
    volumeGrid.putSample(val, position);
  }

  @Override
  public void putSample(double val, int[] position) {
    volumeGrid.putSample(val, position);
  }

  @Override
  public int localToGlobal(int dimension, int index) {
    return volumeGrid.localToGlobal(dimension, index);
  }

  @Override
  public int globalToLocal(int dimension, int index) {
    return volumeGrid.globalToLocal(dimension, index);
  }

  @Override
  public int[] localPosition(int[] pos) {
    return volumeGrid.localPosition(pos);
  }

  @Override
  public void worldCoords(int[] pos, double[] wxyz) {
    volumeGrid.worldCoords(pos, wxyz);
  }

  @Override
  public IRegularGrid createCopy() {
    return volumeGrid.createCopy();
  }

  @Override
  public void copyVolume(ISeismicVolume source) {
    if (!source.matches(this))
      throw new IllegalArgumentException("Source volume and this volume do not match");
    this.getDistributedArray().copy(source.getDistributedArray());
  }

  @Override
  public GridDefinition getGlobalGrid() {
    return globalGrid;
  }

  @Override
  public boolean matches(ISeismicVolume seismicVolume) {
    return globalGrid.matches(seismicVolume.getGlobalGrid());
  }

  @Override
  public int getElementCount() {
    return elementCount;
  }

  @Override
  public ElementType getElementType() {
    return elementType;
  }
}
