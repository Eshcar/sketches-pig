/*
 * Copyright 2016, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.pig.tuple;

import static com.yahoo.sketches.Util.DEFAULT_NOMINAL_ENTRIES;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.tuple.ArrayOfDoublesSetOperationBuilder;
import com.yahoo.sketches.tuple.ArrayOfDoublesSketches;
import com.yahoo.sketches.tuple.ArrayOfDoublesUnion;
import com.yahoo.sketches.tuple.ArrayOfDoublesUpdatableSketch;
import com.yahoo.sketches.tuple.ArrayOfDoublesUpdatableSketchBuilder;

/**
 * Class used to calculate the intermediate pass (combiner) or the final pass
 * (reducer) of an Algebraic sketch operation. This may be called multiple times
 * (from the mapper and from the reducer). It will receive a bag of values
 * returned by either the Intermediate stage or the Initial stages, so
 * it needs to be able to differentiate between and interpret both types.
 */
abstract class DataToArrayOfDoublesSketchAlgebraicIntermediateFinal extends EvalFunc<Tuple> {
  private final int sketchSize_;
  private final float samplingProbability_;
  private final int numValues_;
  private boolean isFirstCall_ = true;

  DataToArrayOfDoublesSketchAlgebraicIntermediateFinal() {
    this(DEFAULT_NOMINAL_ENTRIES, 1);
  }

  DataToArrayOfDoublesSketchAlgebraicIntermediateFinal(final int numValues) {
    this(DEFAULT_NOMINAL_ENTRIES, numValues);
  }

  DataToArrayOfDoublesSketchAlgebraicIntermediateFinal(final int sketchSize, final int numValues) {
    this(sketchSize, 1f, numValues);
  }

  DataToArrayOfDoublesSketchAlgebraicIntermediateFinal(
      final int sketchSize, final float samplingProbability, final int numValues) {
    sketchSize_ = sketchSize;
    samplingProbability_ = samplingProbability;
    numValues_ = numValues;
  }

  @Override
  public Tuple exec(final Tuple inputTuple) throws IOException {
    if (isFirstCall_) {
      // this is to see in the log which way was used by Pig
      Logger.getLogger(getClass()).info("algebraic is used");
      isFirstCall_ = false;
    }
    final ArrayOfDoublesUnion union =
        new ArrayOfDoublesSetOperationBuilder().setNominalEntries(sketchSize_)
          .setNumberOfValues(numValues_).buildUnion();

    final DataBag bag = (DataBag) inputTuple.get(0);
    if (bag == null) {
      throw new IllegalArgumentException("InputTuple.Field0: Bag may not be null");
    }

    for (final Tuple dataTuple: bag) {
      final Object item = dataTuple.get(0);
      if (item instanceof DataBag) {
        // this is a bag from the Initial function.
        // just insert each item of the tuple into the sketch
        final ArrayOfDoublesUpdatableSketch sketch =
            new ArrayOfDoublesUpdatableSketchBuilder().setNominalEntries(sketchSize_)
              .setSamplingProbability(samplingProbability_).setNumberOfValues(numValues_).build();
        DataToArrayOfDoublesSketchBase.updateSketch((DataBag) item, sketch, numValues_);
        union.update(sketch);
      } else if (item instanceof DataByteArray) {
        // This is a sketch from a prior call to the
        // Intermediate function. merge it with the
        // current sketch.
        final DataByteArray dba = (DataByteArray) item;
        union.update(ArrayOfDoublesSketches.wrapSketch(Memory.wrap(dba.get())));
      } else {
        // we should never get here.
        throw new IllegalArgumentException("InputTuple.Field0: Bag contains unrecognized types: "
            + item.getClass().getName());
      }
    }
    return Util.tupleFactory.newTuple(new DataByteArray(union.getResult().toByteArray()));
  }
}
