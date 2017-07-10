/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.beacon.nodes;

import com.hortonworks.beacon.exceptions.BeaconException;
import com.hortonworks.beacon.job.BeaconJob;
import com.hortonworks.beacon.job.JobContext;
import com.hortonworks.beacon.job.JobStatus;
import com.hortonworks.beacon.log.BeaconLog;
import com.hortonworks.beacon.rb.MessageCode;
import com.hortonworks.beacon.replication.InstanceReplication;
import com.hortonworks.beacon.replication.ReplicationJobDetails;

/**
 * End node implementation.
 */
public class EndNode extends InstanceReplication implements BeaconJob {

    private static final BeaconLog LOG = BeaconLog.getLog(EndNode.class);

    public EndNode(ReplicationJobDetails jobDetails) {
        super(jobDetails);
    }

    @Override
    public void init(JobContext jobContext) throws BeaconException {

    }

    @Override
    public void perform(JobContext jobContext) throws BeaconException {
        LOG.info(MessageCode.JOBS_000001.name(),
                jobContext.getJobInstanceId(), getDetails().getType());
        setInstanceExecutionDetails(jobContext, JobStatus.SUCCESS);
    }

    @Override
    public void cleanUp(JobContext jobContext) throws BeaconException {

    }

    @Override
    public void recover(JobContext jobContext) throws BeaconException {
        LOG.info(MessageCode.COMM_010012.name(), jobContext.getJobInstanceId());
    }
}
