/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.streaming.sqs

import java.util.Locale

import org.apache.spark.sql.streaming.{StreamingQuery, StreamingQueryException, StreamTest}
import org.apache.spark.sql.types._

class SqsSourceSuite extends StreamTest  {

  import org.apache.spark.sql.streaming.sqs.SqsSource._

  test("partitioned data source - base path not specified") {

    var query: StreamingQuery = null

    val expectedMsg = s"$BASE_PATH is mandatory if schema contains partitionColumns"

    try {
      val errorMessage = intercept[StreamingQueryException] {

        val metaData = (new MetadataBuilder).putBoolean(IS_PARTITIONED, true).build()

        val partitionedSchema = new StructType().add(StructField(
          "col1", IntegerType, true, metaData))

        val reader = spark
          .readStream
          .format("s3-sqs")
          .option("sqsUrl", "https://DUMMY_URL")
          .option("fileFormat", "json")
          .option("region", "us-east-1")
          .schema(partitionedSchema)
          .load()

        query = reader.writeStream
          .queryName("testQuery")
          .format("memory")
          .start()

        query.processAllAvailable()
      }.getMessage
      assert(errorMessage.toLowerCase(Locale.ROOT).contains(expectedMsg.toLowerCase(Locale.ROOT)))
    } finally {
      if (query != null) {
        // terminating streaming query if necessary
        query.stop()
      }
    }

  }

  test("isPartitioned doesn't contain true or false") {

    var query: StreamingQuery = null

    val columName = "col1"

    val expectedMsg = s"$IS_PARTITIONED for column $columName must be true or false"

    try {
      val errorMessage = intercept[StreamingQueryException] {

        val metaData = (new MetadataBuilder).putString(IS_PARTITIONED, "x").build()

        val partitionedSchema = new StructType().add(StructField(
          "col1", IntegerType, true, metaData))

        val reader = spark
          .readStream
          .format("s3-sqs")
          .option("sqsUrl", "https://DUMMY_URL")
          .option("fileFormat", "json")
          .option("region", "us-east-1")
          .schema(partitionedSchema)
          .load()

        query = reader.writeStream
          .format("memory")
          .queryName("testQuery")
          .start()

        query.processAllAvailable()
      }.getMessage
      assert(errorMessage.toLowerCase(Locale.ROOT).contains(expectedMsg.toLowerCase(Locale.ROOT)))
    } finally {
      if (query != null) {
        // terminating streaming query if necessary
        query.stop()
      }
    }

  }

}

