/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.pig.tuple;

import org.apache.pig.Algebraic;

/**
 * This is to merge ArrayOfDoublesSketches.
 * It supports all three ways: exec(), Accumulator and Algebraic
 */
public class MergeArrayOfDoublesSketch extends MergeArrayOfDoublesSketchBase implements Algebraic {
  /**
   * Constructor
   * @param sketchSize String representation of sketch size
   * @param numValues String representation of number of values per key
   */
  public MergeArrayOfDoublesSketch(String sketchSize, String numValues) {
    super(Integer.parseInt(sketchSize), Integer.parseInt(numValues));
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
     * Default constructor to make pig validation happy.
     */
    public Initial() {}

    /**
     * Constructor for the initial pass of an Algebraic function. This will be passed the same
     * constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param numValues String representation of number of values per key
     */
    public Initial(String sketchSize, String numValues) {}
  }

  public static class IntermediateFinal extends MergeArrayOfDoublesSketchAlgebraicIntermediateFinal {
    /**
     * Default constructor to make pig validation happy.
     */
    public IntermediateFinal() {}

    /**
     * Constructor for the intermediate and final passes of an Algebraic function. This will be
     * passed the same constructor arguments as the original UDF.
     * @param sketchSize String representation of sketch size
     * @param numValues String representation of number of values per key
     */
    public IntermediateFinal(String sketchSize, String numValues) {
      super(Integer.parseInt(sketchSize), Integer.parseInt(numValues));
    }
  }
}
