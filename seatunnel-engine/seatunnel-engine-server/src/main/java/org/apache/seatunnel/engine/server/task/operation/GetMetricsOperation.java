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

package org.apache.seatunnel.engine.server.task.operation;

import org.apache.seatunnel.api.common.metrics.RawJobMetrics;
import org.apache.seatunnel.engine.server.metrics.ZetaMetricsCollector;
import org.apache.seatunnel.engine.server.serializable.TaskDataSerializerHook;

import com.hazelcast.cluster.Address;
import com.hazelcast.internal.metrics.MetricDescriptor;
import com.hazelcast.logging.ILogger;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.impl.operationservice.Operation;

import java.io.IOException;
import java.util.function.Predicate;

public class GetMetricsOperation extends Operation implements IdentifiedDataSerializable {

    private Predicate<MetricDescriptor> metricDescriptorPredicate;
    private RawJobMetrics response;

    public GetMetricsOperation() {}

    public GetMetricsOperation(Predicate<MetricDescriptor> metricDescriptorPredicate) {
        this.metricDescriptorPredicate = metricDescriptorPredicate;
    }

    @Override
    public void run() {
        ILogger logger = getLogger();

        Address callerAddress = getCallerAddress();

        NodeEngineImpl nodeEngine = (NodeEngineImpl) getNodeEngine();
        Address masterAddress = getNodeEngine().getMasterAddress();
        if (!callerAddress.equals(masterAddress)) {
            throw new IllegalStateException(
                    "Caller "
                            + callerAddress
                            + " cannot get metrics"
                            + " because it is not master. Master is: "
                            + masterAddress);
        }

        ZetaMetricsCollector metricsRenderer =
                new ZetaMetricsCollector(
                        metricDescriptorPredicate, nodeEngine.getLocalMember(), logger);
        nodeEngine.getMetricsRegistry().collect(metricsRenderer);
        response = metricsRenderer.getMetrics();
    }

    @Override
    protected void writeInternal(ObjectDataOutput out) throws IOException {
        super.writeInternal(out);
        out.writeObject(metricDescriptorPredicate);
    }

    @Override
    protected void readInternal(ObjectDataInput in) throws IOException {
        super.readInternal(in);
        this.metricDescriptorPredicate = in.readObject();
    }

    @Override
    public Object getResponse() {
        return response;
    }

    @Override
    public int getFactoryId() {
        return TaskDataSerializerHook.FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return TaskDataSerializerHook.GET_METRICS_OPERATION;
    }
}
