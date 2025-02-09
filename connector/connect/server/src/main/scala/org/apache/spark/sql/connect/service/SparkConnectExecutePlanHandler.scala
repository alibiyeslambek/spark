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

package org.apache.spark.sql.connect.service

import io.grpc.stub.StreamObserver

import org.apache.spark.connect.proto
import org.apache.spark.internal.Logging
import org.apache.spark.sql.connect.execution.ExecuteGrpcResponseSender

class SparkConnectExecutePlanHandler(responseObserver: StreamObserver[proto.ExecutePlanResponse])
    extends Logging {

  def handle(v: proto.ExecutePlanRequest): Unit = {
    val sessionHolder = SparkConnectService
      .getOrCreateIsolatedSession(v.getUserContext.getUserId, v.getSessionId)
    val executeHolder = sessionHolder.createExecuteHolder(v)

    try {
      executeHolder.eventsManager.postStarted()
      executeHolder.start()
      val responseSender =
        new ExecuteGrpcResponseSender[proto.ExecutePlanResponse](executeHolder, responseObserver)
      executeHolder.attachAndRunGrpcResponseSender(responseSender)
    } finally {
      if (!executeHolder.reattachable) {
        // Non reattachable executions release here immediately.
        executeHolder.close()
      } else {
        // Reattachable executions close release with ReleaseExecute RPC.
        // TODO We mark in the ExecuteHolder that RPC detached.
      }
    }
  }
}
