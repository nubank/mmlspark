// Copyright (C) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License. See LICENSE in project root for information.

package com.microsoft.ml.spark.core.schema

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.encoders.{ExpressionEncoder, RowEncoder}
import org.apache.spark.sql.types.StructType

import scala.reflect.runtime.universe.TypeTag

abstract class SparkBindings[T: TypeTag] extends Serializable {

  lazy val schema: StructType = enc.schema
  private lazy val enc: ExpressionEncoder[T] = ExpressionEncoder[T]().resolveAndBind()
  private lazy val rowEnc: ExpressionEncoder[Row] = RowEncoder(enc.schema).resolveAndBind()

  // WARNING: each time you use this function on a dataframe, you should make a new converter.
  // Spark does some magic that makes this leak memory if re-used on a
  // different symbolic node of the parallel computation. That being said,
  // you should make a single converter before using it in a udf so
  // that the slow resolving and binding is not in the hotpath
  def makeFromRowConverter: Row => T = {
    val fromRow = enc.resolveAndBind().createDeserializer()
    val toRow = rowEnc.resolveAndBind().createSerializer();
    { r: Row => fromRow(toRow(r)) }
  }

  def makeFromInternalRowConverter: InternalRow => T = {
    val fromRow = enc.resolveAndBind().createDeserializer();
    { r: InternalRow => fromRow(r) }
  }

  def makeToRowConverter: T => Row = {
    val toRow = enc.resolveAndBind().createSerializer()
    val fromRow = rowEnc.resolveAndBind().createDeserializer();
    { v: T => fromRow(toRow(v)) }
  }

  def makeToInternalRowConverter: T => InternalRow = {
    val toRow = enc.resolveAndBind().createSerializer();
    { v: T => toRow(v) }
  }

}
