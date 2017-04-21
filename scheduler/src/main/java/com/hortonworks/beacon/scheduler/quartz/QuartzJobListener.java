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

package com.hortonworks.beacon.scheduler.quartz;

import com.hortonworks.beacon.events.BeaconEvents;
import com.hortonworks.beacon.events.EventStatus;
import com.hortonworks.beacon.events.Events;
import com.hortonworks.beacon.job.InstanceExecutionDetails;
import com.hortonworks.beacon.job.JobContext;
import com.hortonworks.beacon.job.JobStatus;
import com.hortonworks.beacon.replication.InstanceReplication;
import com.hortonworks.beacon.store.bean.InstanceJobBean;
import com.hortonworks.beacon.store.bean.PolicyInstanceBean;
import com.hortonworks.beacon.store.executors.InstanceJobExecutor;
import com.hortonworks.beacon.store.executors.InstanceJobExecutor.InstanceJobQuery;
import com.hortonworks.beacon.store.executors.PolicyInstanceExecutor;
import com.hortonworks.beacon.store.executors.PolicyInstanceExecutor.PolicyInstanceQuery;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Beacon extended implementation for JobListenerSupport.
 */
public class QuartzJobListener extends JobListenerSupport {

    private static final Logger LOG = LoggerFactory.getLogger(QuartzJobListener.class);
    private String name;
    private Map<JobKey, JobKey> chainLinks;


    public QuartzJobListener(String name) {
        this.name = name;
        chainLinks = new Hashtable<>();
    }

    public String getName() {
        return name;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            String instanceId = handleStartNode(context);
            JobContext jobContext;
            if (instanceId != null) {
                jobContext = initializeJobContext(context, instanceId);
            } else {
                // context for non-start nodes gets loaded from DB.
                jobContext = transferJobContext(context);
                instanceId = jobContext.getJobInstanceId();
            }
            LOG.info("policy instance [{}] to be executed.", instanceId);
            updateInstanceCurrentOffset(jobContext);
            boolean parallelExecution = checkParallelExecution(context);
            if (!parallelExecution) {
                updateInstanceJobStatusStartTime(jobContext, JobStatus.RUNNING);
            } else {
                updateInstanceJobStatusStartTime(jobContext, JobStatus.IGNORED);
                LOG.info("policy instance [{}] will be ignored with status [{}]", instanceId, JobStatus.IGNORED.name());
            }
        } catch (Throwable e) {
            LOG.error("error while processing jobToBeExecuted. Message: {}", e.getMessage(), e);
        }
    }

    private void updateInstanceCurrentOffset(JobContext jobContext) {
        PolicyInstanceBean bean = new PolicyInstanceBean(jobContext.getJobInstanceId());
        bean.setCurrentOffset(jobContext.getOffset());
        PolicyInstanceExecutor executor = new PolicyInstanceExecutor(bean);
        executor.executeUpdate(PolicyInstanceQuery.UPDATE_CURRENT_OFFSET);
    }

    private void updateInstanceJobStatusStartTime(JobContext jobContext, JobStatus status) {
        String instanceId = jobContext.getJobInstanceId();
        int offset = jobContext.getOffset();
        InstanceJobBean bean = new InstanceJobBean(instanceId, offset);
        bean.setStatus(status.name());
        bean.setStartTime(new Date());
        InstanceJobExecutor executor = new InstanceJobExecutor(bean);
        executor.executeUpdate(InstanceJobQuery.UPDATE_STATUS_START);
    }

    private boolean checkParallelExecution(JobExecutionContext context) {
        // TODO check and prevent parallel execution execution of the job instance.
        // there is two cases:
        // 1. (covered) previous instance is still running and next instance triggered. (scheduler based, not store)
        // 2. (pending) After restart, previous instance is still in running state (store) but no actual jobs are
        // running.

        JobKey currentJob = context.getJobDetail().getKey();
        // Check the parallel for the START node only.
        if (currentJob.getGroup().equals(BeaconQuartzScheduler.START_NODE_GROUP)) {
            LOG.info("Check parallel execution [JobKey: {}, TriggerKey: {}].",
                    currentJob, context.getTrigger().getKey());
            List<JobExecutionContext> currentlyExecutingJobs;
            try {
                currentlyExecutingJobs = context.getScheduler().getCurrentlyExecutingJobs();
            } catch (SchedulerException e) {
                LOG.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            for (JobExecutionContext jobExecutionContext : currentlyExecutingJobs) {
                JobKey key = jobExecutionContext.getJobDetail().getKey();
                LOG.info("Execution List [JobKey: {}, TriggerKey: {}].",
                        key, jobExecutionContext.getTrigger().getKey());
                // The name for the two jobs will be same (policyId) and trigger key should be different.
                // The Trigger keys are auto generated by the scheduler while scheduling in the chaining.
                if (key.getName().equals(currentJob.getName())
                        && !jobExecutionContext.getTrigger().equals(context.getTrigger())) {
                    LOG.warn("another policy instance [{}] is in execution, current instance will be ignored.",
                            getJobContext(jobExecutionContext).getJobInstanceId());
                    context.getJobDetail().getJobDataMap().put(QuartzDataMapEnum.IS_PARALLEL.getValue(), true);
                    context.getJobDetail().getJobDataMap().put(QuartzDataMapEnum.PARALLEL_INSTANCE.getValue(),
                            getJobContext(jobExecutionContext).getJobInstanceId());
                    return true;
                }
            }
        }
        return false;
    }

    private String handleStartNode(JobExecutionContext context) {
        JobDetail jobDetail = context.getJobDetail();
        JobKey jobKey = jobDetail.getKey();
        if (jobKey.getGroup().equals(BeaconQuartzScheduler.START_NODE_GROUP)) {
            String policyId = jobKey.getName();
            String instanceId = insertPolicyInstance(policyId, getAndUpdateCounter(jobDetail),
                    JobStatus.RUNNING.name());
            int jobCount = jobDetail.getJobDataMap().getInt(QuartzDataMapEnum.NO_OF_JOBS.getValue());
            insertJobInstance(instanceId, jobCount);
            return instanceId;
        }
        return null;
    }

    // This is beacon managed job context which is used across all the jobs of a instance.
    private JobContext initializeJobContext(JobExecutionContext quartzContext, String instanceId) {
        JobContext context = new JobContext();
        context.setOffset(0);
        context.setJobInstanceId(instanceId);
        context.setShouldInterrupt(new AtomicBoolean(false));
        context.setJobContextMap(new HashMap<String, String>());
        quartzContext.getJobDetail().getJobDataMap().put(QuartzDataMapEnum.JOB_CONTEXT.getValue(), context);
        return context;
    }

    private JobContext transferJobContext(JobExecutionContext qContext) {
        JobKey jobKey = qContext.getJobDetail().getKey();
        String currentOffset = jobKey.getGroup();
        Integer prevOffset = Integer.parseInt(currentOffset) - 1;
        String instanceId = getInstanceId(qContext.getJobDetail());

        InstanceJobBean bean = new InstanceJobBean(instanceId, prevOffset);
        InstanceJobExecutor executor = new InstanceJobExecutor(bean);
        InstanceJobBean instanceJob = executor.getInstanceJob(InstanceJobQuery.GET_INSTANCE_JOB);

        String contextData = instanceJob.getContextData();
        JobContext jobContext = JobContext.parseJobContext(contextData);
        // Update the offset to current for job.
        jobContext.setOffset(Integer.parseInt(currentOffset));
        qContext.getJobDetail().getJobDataMap().put(QuartzDataMapEnum.JOB_CONTEXT.getValue(), jobContext);
        return jobContext;
    }

    private String getInstanceId(JobDetail jobDetail) {
        JobKey jobKey = jobDetail.getKey();
        String policyId = jobKey.getName();
        int counter = jobDetail.getJobDataMap().getInt(QuartzDataMapEnum.COUNTER.getValue());
        return policyId + "@" + counter;
    }

    private JobContext getJobContext(JobExecutionContext context) {
        return (JobContext) context.getJobDetail().getJobDataMap().get(QuartzDataMapEnum.JOB_CONTEXT.getValue());
    }

    private InstanceExecutionDetails getExecutionDetail(JobContext jobContext) {
        String instanceDetail = jobContext.getJobContextMap().get(
                InstanceReplication.INSTANCE_EXECUTION_STATUS);
        LOG.info("Instance Detail : {}", instanceDetail);
        return (new InstanceExecutionDetails()).getInstanceExecutionDetails(instanceDetail);
    }

    private void insertJobInstance(String instanceId, int jobCount) {
        int offsetCounter = 0;
        while (offsetCounter < jobCount) {
            InstanceJobBean bean = new InstanceJobBean(instanceId, offsetCounter);
            bean.setStatus(JobStatus.SUBMITTED.name());
            bean.setRunCount(0);
            offsetCounter++;
            InstanceJobExecutor executor = new InstanceJobExecutor(bean);
            executor.execute();
        }
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            JobContext jobContext = getJobContext(context);
            boolean isParallel = context.getJobDetail().getJobDataMap()
                    .getBoolean(QuartzDataMapEnum.IS_PARALLEL.getValue());
            if (isParallel) {
                context.getJobDetail().getJobDataMap().remove(QuartzDataMapEnum.IS_PARALLEL.getValue());
                String parallelId = (String)context.getJobDetail().getJobDataMap()
                        .remove(QuartzDataMapEnum.PARALLEL_INSTANCE.getValue());
                String message = "Parallel instance in execution was: " + parallelId;
                updatePolicyInstanceCompleted(jobContext, JobStatus.IGNORED.name(), message);
                updateInstanceJobCompleted(jobContext, JobStatus.IGNORED.name(), message);
                updateRemainingInstanceJobs(jobContext, JobStatus.IGNORED.name());
                return;
            }
            InstanceExecutionDetails detail = getExecutionDetail(jobContext);
            boolean jobSuccessful = isJobSuccessful(detail, jobException);
            LOG.info("execution status of the job [instance: {}, offset: {}], isSuccessful: [{}]",
                    jobContext.getJobInstanceId(), jobContext.getOffset(), jobSuccessful);
            if (jobSuccessful) {
                updateInstanceJobCompleted(jobContext, detail.getJobStatus(), detail.getJobMessage());
                boolean chainNextJob = chainNextJob(context, jobContext);
                if (!chainNextJob) {
                    updatePolicyInstanceCompleted(jobContext, detail.getJobStatus(), detail.getJobMessage());
                }
            } else {
                updatePolicyInstanceCompleted(jobContext, detail.getJobStatus(), detail.getJobMessage());
                updateInstanceJobCompleted(jobContext, detail.getJobStatus(), detail.getJobMessage());
                updateRemainingInstanceJobs(jobContext, detail.getJobStatus());
                // update all the instance job to failed/aborted.
            }
            //Clean up the job context so it does not get stored into the Quartz tables.
            context.getJobDetail().getJobDataMap().remove(QuartzDataMapEnum.JOB_CONTEXT.getValue());
        } catch (Throwable e) {
            LOG.error("error while processing jobWasExecuted. Message: {}", e.getMessage(), e);
        }
    }

    private void updateRemainingInstanceJobs(JobContext jobContext, String status)
            throws SchedulerException {
        InstanceJobBean bean = new InstanceJobBean();
        bean.setInstanceId(jobContext.getJobInstanceId());
        bean.setStatus(status);
        InstanceJobExecutor executor = new InstanceJobExecutor(bean);
        executor.executeUpdate(InstanceJobQuery.INSTANCE_JOB_UPDATE_STATUS);
    }

    private boolean chainNextJob(JobExecutionContext context, JobContext jobContext) throws SchedulerException {
        JobKey currentJobKey = context.getJobDetail().getKey();
        JobKey nextJobKey = chainLinks.get(currentJobKey);
        boolean isChained = context.getJobDetail().getJobDataMap().getBoolean(QuartzDataMapEnum.CHAINED.getValue());
        // next job is not available in the cache and it is not chained job.
        if (nextJobKey == null && !isChained) {
            return false;
        }
        // Get the next job from store when it is chained.
        if (nextJobKey == null) {
            nextJobKey = getNextJobFromStore(jobContext.getJobInstanceId(), jobContext.getOffset(),
                    currentJobKey.getName());
        }
        if (nextJobKey == null) {
            LOG.error("this should never happen. next chained job not found for instance id: [{}], offset: [{}]",
                    jobContext.getJobInstanceId(), jobContext.getOffset());
            return false;
        } else {
            chainLinks.put(currentJobKey, nextJobKey);
        }
        // This passing of the counter is required to load the context for next job. (check: transferJobContext)
        JobDetail nextJobDetail = context.getScheduler().getJobDetail(nextJobKey);
        nextJobDetail.getJobDataMap().put(QuartzDataMapEnum.COUNTER.getValue(),
                context.getJobDetail().getJobDataMap().getInt(QuartzDataMapEnum.COUNTER.getValue()));
        context.getScheduler().addJob(nextJobDetail, true);
        context.getScheduler().triggerJob(nextJobKey);
        LOG.info("Job [{}] is now chained to job [{}]", currentJobKey, nextJobKey);
        return true;
    }

    private void updateInstanceJobCompleted(JobContext jobContext, String status, String message) {
        InstanceJobBean bean = new InstanceJobBean(jobContext.getJobInstanceId(), jobContext.getOffset());
        bean.setStatus(status);
        bean.setMessage(truncateMessage(message));
        bean.setEndTime(new Date());
        bean.setContextData(jobContext.toString());
        InstanceJobExecutor executor = new InstanceJobExecutor(bean);
        executor.executeUpdate(InstanceJobQuery.UPDATE_JOB_COMPLETE);
    }

    private boolean isJobSuccessful(InstanceExecutionDetails detail, JobExecutionException jobException) {
        return !(jobException != null
                || detail.getJobStatus().equals(JobStatus.FAILED.name())
                || detail.getJobStatus().equals(JobStatus.KILLED.name()));
    }

    /**
     * When jobs are not found into the cached map, needs to be retrieved from store.
     * @param instanceId instance id
     * @param offset offset
     * @param policyId
     * @return key for the next job
     */
    private JobKey getNextJobFromStore(String instanceId, int offset, String policyId) {
        InstanceJobBean bean = new InstanceJobBean(instanceId, offset + 1);
        InstanceJobExecutor executor = new InstanceJobExecutor(bean);
        InstanceJobBean instanceJob = executor.getInstanceJob(InstanceJobQuery.GET_INSTANCE_JOB);
        return instanceJob != null
                ? new JobKey(policyId, String.valueOf(instanceJob.getOffset()))
                : null;
    }

    private void updatePolicyInstanceCompleted(JobContext jobContext, String status, String message) {
        PolicyInstanceBean bean = new PolicyInstanceBean();
        bean.setStatus(status);
        bean.setMessage(truncateMessage(message));
        bean.setPolicyId(jobContext.getJobInstanceId().split("@")[0]);
        bean.setEndTime(new Date());
        bean.setInstanceId(jobContext.getJobInstanceId());
        PolicyInstanceExecutor executor = new PolicyInstanceExecutor(bean);
        executor.executeUpdate(PolicyInstanceQuery.UPDATE_INSTANCE_COMPLETE);

        generateInstanceEvents(status, bean);
    }

    void addJobChainLink(JobKey firstJob, JobKey secondJob) {
        if (firstJob == null || secondJob == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }

        if (firstJob.getName() == null || secondJob.getName() == null) {
            throw new IllegalArgumentException("Key cannot have a null name!");
        }
        LOG.info("Job [key: {}] is chained with Job [key: {}]", firstJob, secondJob);
        chainLinks.put(firstJob, secondJob);
    }

    private String insertPolicyInstance(String policyId, int count, String status) {
        PolicyInstanceBean bean = new PolicyInstanceBean();
        String instanceId = policyId + "@" + count;
        bean.setInstanceId(instanceId);
        bean.setPolicyId(policyId);
        bean.setStartTime(new Date());
        bean.setRunCount(0);
        bean.setStatus(status);
        bean.setCurrentOffset(0);
        PolicyInstanceExecutor executor = new PolicyInstanceExecutor(bean);
        executor.execute();
        return instanceId;
    }

    private int getAndUpdateCounter(JobDetail jobDetail) {
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        int count = jobDataMap.getInt(QuartzDataMapEnum.COUNTER.getValue());
        count++;
        jobDataMap.put(QuartzDataMapEnum.COUNTER.getValue(), count);
        return count;
    }

    private String truncateMessage(String message) {
        return (message.length() > 4000)
                ? message.substring(0, 3899) + " ..."
                : message;
    }

    private void generateInstanceEvents(String status, PolicyInstanceBean bean) {
        JobStatus jobStatus;

        try {
            jobStatus = JobStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported status type : {}", e);
        }

        switch (jobStatus) {
            case SUCCESS:
                BeaconEvents.createPolicyInstanceEvents(Events.POLICY_INSTANCE_SUCCEEDED.getId(),
                        System.currentTimeMillis(), EventStatus.SUCCEEDED, "Policy Instance Successful", bean);
                break;
            case FAILED:
                BeaconEvents.createPolicyInstanceEvents(Events.POLICY_INSTANCE_FAILED.getId(),
                        System.currentTimeMillis(), EventStatus.FAILED, "Policy Instance Failed", bean);
                break;
            case IGNORED:
                BeaconEvents.createPolicyInstanceEvents(Events.POLICY_INSTANCE_IGNORED.getId(),
                        System.currentTimeMillis(), EventStatus.IGNORED, "Policy Instance Ignored", bean);
                break;
            case DELETED:
                BeaconEvents.createPolicyInstanceEvents(Events.POLICY_INSTANCE_DELETED.getId(),
                        System.currentTimeMillis(), EventStatus.DELETED, "Policy Instance Deleted", bean);
                break;

            case KILLED:
                BeaconEvents.createPolicyInstanceEvents(Events.POLICY_INSTANCE_KILLED.getId(),
                        System.currentTimeMillis(), EventStatus.KILLED, "Policy Instance Killed", bean);

                break;
            default:
                System.out.println("Job Status is not supported");
        }

    }
}
