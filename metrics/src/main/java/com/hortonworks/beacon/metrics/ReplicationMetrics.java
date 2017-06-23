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

package com.hortonworks.beacon.metrics;

import com.google.gson.Gson;

import java.util.Map;

/**
 * Replication metrics.
 */
public class ReplicationMetrics {

    private String jobId;
    private JobType jobType;
    private long numMapTasks;
    private long bytesCopied;
    private long filesCopied;
    private long timeTaken;

    /**
     * Enum for repliction job type.
     */
    public enum JobType {
        MAIN,
        RECOVERY,
    }

    public ReplicationMetrics() {
    }

    public String getJobId() {
        return jobId;
    }

    private void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public long getNumMapTasks() {
        return numMapTasks;
    }

    public void setNumMapTasks(long numMapTasks) {
        this.numMapTasks = numMapTasks;
    }

    public long getBytesCopied() {
        return bytesCopied;
    }

    private void setBytesCopied(long bytesCopied) {
        this.bytesCopied = bytesCopied;
    }

    public long getFilesCopied() {
        return filesCopied;
    }

    public void setFilesCopied(long filesCopied) {
        this.filesCopied = filesCopied;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    private void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public String toJsonString() {
        return new Gson().toJson(this);
    }

    private void updateReplicationMetricsDetails(String jobid, JobType type, long nummaptasks, long bytescopied,
                                                 long copyfiles, long timetaken) {
        this.setJobId(jobid);
        this.setJobType(type);
        this.setNumMapTasks(nummaptasks);
        this.setBytesCopied(bytescopied);
        this.setFilesCopied(copyfiles);
        this.setTimeTaken(timetaken);
    }

    public void updateReplicationMetricsDetails(String jobid, JobType type, Map<String, Long> metrics) {
        long numMapTasksVal = metrics.get(ReplicationJobMetrics.NUMMAPTASKS.getName()) != null
                ? metrics.get(ReplicationJobMetrics.NUMMAPTASKS.getName()) : 0L;
        long bytesCopiedVal = metrics.get(ReplicationJobMetrics.BYTESCOPIED.getName()) != null
                ? metrics.get(ReplicationJobMetrics.BYTESCOPIED.getName()) : 0L;
        long filesCopiedVal = metrics.get(ReplicationJobMetrics.COPY.getName()) != null
                ? metrics.get(ReplicationJobMetrics.COPY.getName()) : 0L;
        long timeTakenVal = metrics.get(ReplicationJobMetrics.TIMETAKEN.getName()) != null
                ? metrics.get(ReplicationJobMetrics.TIMETAKEN.getName()) : 0L;
        updateReplicationMetricsDetails(jobid, type, numMapTasksVal, bytesCopiedVal, filesCopiedVal, timeTakenVal);
    }

    @Override
    public String toString() {
        return "ReplicationMetrics{"
                + "jobId='" + jobId + '\''
                + "jobType='" + jobType + '\''
                + ", numMapTasks='" + numMapTasks + '\''
                + ", bytesCopied='" + bytesCopied + '\''
                + ", filesCopied='" + filesCopied + '\''
                + ", timeTaken=" + timeTaken
                + '}';
    }
}
