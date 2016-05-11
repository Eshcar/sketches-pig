/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.pig.tuple;

import org.apache.pig.Algebraic;

/**
 * This UDF creates an ArrayOfDoublesSketch from raw data.
 * It supports all three ways: exec(), Accumulator and Algebraic.
 */
public class DataToArrayOfDoublesSketch extends DataToArrayOfDoublesSketchBase implements Algebraic {
  /**
   * Constructor.
   * @param sketchSize String representation of sketch size
   * @param numValues Number of double values to keep for each key
   */
  public DataToArrayOfDoublesSketch(String sketchSize, String numValues) {
    super(Integer.parseInt(sketchSize), Integer.parseInt(numValues));
  }

  /**
   * Constructor.
   * @param sketchSize String representation of sketch size
   * @param samplingProbability probability from 0 to 1
   * @param numValues Number of double values to keep for each key
   */
  public DataToArrayOfDoublesSketch(String sketchSize, String samplingProbability, String numValues) {
    super(Integer.parseInt(sketchSize), Float.parseFloat(samplingProbability), Integer.parseInt(numValues));
  }

  @Override
  public String getInitial() {
    return Initial.class.getName();
  }

  @Override
  public String getIntermed() {
    return IntermediateFinal.class.getName();
  }

  @Override
  public String getFinal() {
    return IntermediateFinal.class.getName();
  }

  public static class Initial extends AlgebraicInitial {
    /**
     * Constructor for the initial pass of an Algebraic function. This will be passed the same
     * constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param numValues Number of double values to keep for each key
     */
    public Initial(String sketchSize, String numValues) {}

    /**
     * Constructor for the initial pass of an Algebraic function. This will be passed the same
     * constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param samplingProbability probability from 0 to 1
     * @param numValues Number of double values to keep for each key
     */
    public Initial(String sketchSize, String samplingProbability, String numValues) {}

    /**
     * Default constructor to make pig validation happy
     */
    public Initial() {}
  }

  public static class IntermediateFinal extends DataToArrayOfDoublesSketchAlgebraicIntermediateFinal {
    /**
     * Constructor for the intermediate and final passes of an Algebraic function. This will be
     * passed the same constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param numValues Number of double values to keep for each key
     */
    public IntermediateFinal(String sketchSize, String numValues) {
      super(Integer.parseInt(sketchSize), Integer.parseInt(numValues));
    }

    /**
     * Constructor for the intermediate and final passes of an Algebraic function. This will be
     * passed the same constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param samplingProbability probability from 0 to 1
     * @param numValues Number of double values to keep for each key
     */
    public IntermediateFinal(String sketchSize, String samplingProbability, String numValues) {
      super(Integer.parseInt(sketchSize), Float.parseFloat(samplingProbability), Integer.parseInt(numValues));
    }

    /**
     * Default constructor to make pig validation happy.
     */
    public IntermediateFinal() {}
  }
}