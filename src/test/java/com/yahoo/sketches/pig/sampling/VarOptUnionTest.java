/*
 * Copyright 2017, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.pig.sampling;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

import java.io.IOException;

import org.apache.pig.data.BagFactory;
import org.apache.pig.data.DataBag;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.sketches.sampling.VarOptItemsSketch;
import com.yahoo.sketches.sampling.VarOptItemsUnion;

public class VarOptUnionTest {
  @SuppressWarnings("unused")
  @Test
  public void checkConstructors() {
    // these three should work
    VarOptUnion udf = new VarOptUnion();
    assertNotNull(udf);

    udf = new VarOptUnion("255");
    assertNotNull(udf);

    try {
      new VarOptUnion("-1");
      fail("Accepted negative k");
    } catch (final IllegalArgumentException e) {
       // expected
    }
  }

  @Test
  public void checkExecution() {
    final int k = 5;
    final VarOptUnion udf = new VarOptUnion(Integer.toString(k));

    final DataBag inputBag = BagFactory.getInstance().newDefaultBag();
    final Tuple inputTuple = TupleFactory.getInstance().newTuple(1);

    try {
      final VarOptItemsSketch<Tuple> sketch = VarOptItemsSketch.newInstance(k);
      final VarOptItemsUnion<Tuple> union = VarOptItemsUnion.newInstance(k);
      for (int i = 1; i < k / 2; ++i) {
        sketch.reset();
        final Tuple t = TupleFactory.getInstance().newTuple(3);
        t.set(0, 1.0 * i);
        t.set(1, i);
        t.set(2, -i);
        sketch.update(t, 1.0 * i);

        // serialize sketch and wrap in Tuple, add to both bag and union
        final Tuple sketchWrapper = TupleFactory.getInstance().newTuple(1);
        final DataByteArray dba = new DataByteArray(sketch.toByteArray(new ArrayOfTuplesSerDe()));
        sketchWrapper.set(0, dba);
        inputBag.add(sketchWrapper);
        union.update(sketch);
        union.update(sketch); // calling accumulate() twice later
      }
      inputTuple.set(0, inputBag);

      assertNull(udf.getValue());
      udf.accumulate(inputTuple);
      udf.accumulate(inputTuple);
      final DataByteArray outBytes = udf.getValue();
      udf.cleanup();
      assertNull(udf.getValue());

      final VarOptItemsSketch<Tuple> result = VarOptItemsSketch.heapify(Memory.wrap(outBytes.get()),
              new ArrayOfTuplesSerDe());

      assertNotNull(result);
      VarOptCommonAlgebraicTest.compareResults(result, union.getResult());
    } catch (final IOException e) {
      fail("Unexpected exception");
    }
  }

  @Test
  public void degenerateExecInput() {
    final VarOptUnion udf = new VarOptUnion();

    try {
      assertNull(udf.exec(null));
      assertNull(udf.exec(TupleFactory.getInstance().newTuple(0)));

      final Tuple in = TupleFactory.getInstance().newTuple(1);
      in.set(0, null);
      assertNull(udf.exec(in));
    } catch (final IOException e) {
      fail("Unexpected exception");
    }
  }

  @Test
  public void outputSchemaTest() throws IOException {
    final VarOptUnion udf = new VarOptUnion("5");

    final Schema inputSchema = new Schema();
    inputSchema.add(new Schema.FieldSchema("bytes", DataType.BYTEARRAY));

    final Schema output = udf.outputSchema(inputSchema);
    assertEquals(output.size(), 1);
    assertEquals(output.getField(0).type, DataType.BYTEARRAY);
  }
}
