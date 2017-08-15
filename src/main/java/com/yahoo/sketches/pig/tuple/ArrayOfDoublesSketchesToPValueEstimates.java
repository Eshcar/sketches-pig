/*
 * Copyright 2017, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.pig.tuple;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.tuple.ArrayOfDoublesSketch;
import com.yahoo.sketches.tuple.ArrayOfDoublesSketches;
import com.yahoo.sketches.tuple.ArrayOfDoublesSketchIterator;

import java.io.IOException;

import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import org.apache.pig.EvalFunc;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.Tuple;

/**
 * Calculate p-values given two ArrayOfDoublesSketch. Each value in the sketch
 * is treated as a separate metric measurement, and a p-value will be generated
 * for each metric.
 */
public class ArrayOfDoublesSketchesToPValueEstimates extends EvalFunc<Tuple> {
    @Override
    public Tuple exec(final Tuple input) throws IOException {
        if ((input == null) || (input.size() != 2)) {
            return null;
        }

        // Get the two sketches
        final DataByteArray dbaA = (DataByteArray) input.get(0);
        final DataByteArray dbaB = (DataByteArray) input.get(1);
        final ArrayOfDoublesSketch sketchA = ArrayOfDoublesSketches.wrapSketch(Memory.wrap(dbaA.get()));
        final ArrayOfDoublesSketch sketchB = ArrayOfDoublesSketches.wrapSketch(Memory.wrap(dbaB.get()));

        // Check that the size of the arrays in the sketches are the same
        if (sketchA.getNumValues() != sketchB.getNumValues()) {
            throw new IllegalArgumentException("Both sketches must have the same number of values");
        }

        // Store the number of metrics
        int numMetrics = sketchA.getNumValues();

        // Check if either sketch is empty
        if (sketchA.isEmpty() || sketchB.isEmpty()) {
            return null;
        }

        // Check that each sketch has at least 2 values
        if (sketchA.getRetainedEntries() < 2 || sketchB.getRetainedEntries() < 2) {
            return null;
        }

        //// Get the stastical summary from each sketch
        SummaryStatistics[] summaryA = new SummaryStatistics[numMetrics];
        SummaryStatistics[] summaryB = new SummaryStatistics[numMetrics];

        // Init the arrays
        for (int i = 0; i < numMetrics; i++) {
            summaryA[i] = new SummaryStatistics();
            summaryB[i] = new SummaryStatistics();
        }

        // Summary of A
        ArrayOfDoublesSketchIterator it = sketchA.iterator();
        while (it.next()) {
            for (int i = 0; i < it.getValues().length; i++) {
                summaryA[i].addValue(it.getValues()[i]);
            }
        }

        // Summary of B
        it = sketchB.iterator();
        while (it.next()) {
            for (int i = 0; i < it.getValues().length; i++) {
                summaryB[i].addValue(it.getValues()[i]);
            }
        }

        // Calculate the p-values
        double[] pValues = new double[numMetrics];
        TTest tTest = new TTest();
        for (int i = 0; i < numMetrics; i++) {
            // Pass the sampled values for each metric
            pValues[i] = tTest.tTest(summaryA[i], summaryB[i]);
        }

        return Util.doubleArrayToTuple(pValues);
    }
}
