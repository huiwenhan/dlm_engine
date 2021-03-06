/**
 * HORTONWORKS DATAPLANE SERVICE AND ITS CONSTITUENT SERVICES
 *
 * (c) 2016-2018 Hortonworks, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Hortonworks, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Hortonworks or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 * (A) HORTONWORKS PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 * (B) HORTONWORKS DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *    LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 * (C) HORTONWORKS IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *    FROM OR RELATED TO THE CODE; AND
 * (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, HORTONWORKS IS NOT LIABLE FOR ANY
 *    DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED TO,
 *    DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR UNAVAILABILITY,
 *    OR LOSS OR CORRUPTION OF DATA.
 */

package com.hortonworks.beacon.api;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Joiner;
import com.hortonworks.beacon.BeaconClientFactory;
import com.hortonworks.beacon.RequestContext;
import com.hortonworks.beacon.api.exception.BeaconWebException;
import com.hortonworks.beacon.api.util.ValidationUtil;
import com.hortonworks.beacon.client.BeaconClient;
import com.hortonworks.beacon.client.BeaconClientException;
import com.hortonworks.beacon.client.BeaconWebClient;
import com.hortonworks.beacon.client.entity.Cluster;
import com.hortonworks.beacon.client.entity.Entity;
import com.hortonworks.beacon.client.entity.ReplicationPolicy;
import com.hortonworks.beacon.client.resource.APIResult;
import com.hortonworks.beacon.client.resource.PolicyInstanceList;
import com.hortonworks.beacon.client.resource.PolicyList;
import com.hortonworks.beacon.client.resource.StatusResult;
import com.hortonworks.beacon.constants.BeaconConstants;
import com.hortonworks.beacon.entity.ReplicationPolicyProperties;
import com.hortonworks.beacon.entity.entityNeo.DataSet;
import com.hortonworks.beacon.entity.util.ClusterHelper;
import com.hortonworks.beacon.entity.util.PolicyHelper;
import com.hortonworks.beacon.entity.util.ReplicationPolicyBuilder;
import com.hortonworks.beacon.entity.util.hive.HiveClientFactory;
import com.hortonworks.beacon.entity.util.hive.HiveMetadataClient;
import com.hortonworks.beacon.events.BeaconEvents;
import com.hortonworks.beacon.events.EventEntityType;
import com.hortonworks.beacon.events.EventInfo;
import com.hortonworks.beacon.events.Events;
import com.hortonworks.beacon.exceptions.BeaconException;
import com.hortonworks.beacon.job.JobStatus;
import com.hortonworks.beacon.log.BeaconLogUtils;
import com.hortonworks.beacon.replication.ReplicationUtils;
import com.hortonworks.beacon.replication.fs.FSSnapshotUtils;
import com.hortonworks.beacon.scheduler.BeaconScheduler;
import com.hortonworks.beacon.scheduler.internal.AdminJobService;
import com.hortonworks.beacon.scheduler.internal.SyncPolicyDeleteJob;
import com.hortonworks.beacon.scheduler.internal.SyncStatusJob;
import com.hortonworks.beacon.scheduler.quartz.BeaconQuartzScheduler;
import com.hortonworks.beacon.service.Services;
import com.hortonworks.beacon.store.BeaconStoreException;
import com.hortonworks.beacon.store.bean.PolicyBean;
import com.hortonworks.beacon.store.bean.PolicyInstanceBean;
import com.hortonworks.beacon.store.executors.PolicyExecutor;
import com.hortonworks.beacon.util.DateUtil;
import com.hortonworks.beacon.util.ReplicationHelper;
import com.hortonworks.beacon.util.ReplicationType;
import com.hortonworks.beacon.util.StringFormat;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;


/**
 * Beacon policy resource management operations as REST API. Root resource (exposed at "myresource" path).
 */
@Path("/api/beacon/policy")
public class PolicyResource extends AbstractResourceManager {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyResource.class);
    private static final List<String> COMPLETION_STATUS = JobStatus.getCompletionStatus();

    @POST
    @Path("submitAndSchedule/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.submitAndSchedule")
    public APIResult submitAndSchedule(@PathParam("policy-name") String policyName,
                                       @DefaultValue("true") @QueryParam("validateCloud") String validateCloudStr,
                                       PropertiesIgnoreCase requestProperties) {
        try {
            LOG.info("Request for policy submitAndSchedule is received. Policy-name: [{}]", policyName);
            BeaconLogUtils.prefixPolicy(
                    policyName,
                    requestProperties.getPropertyIgnoreCase(ReplicationPolicy.ReplicationPolicyFields.ID.getName()));
            ReplicationPolicy replicationPolicy = ReplicationPolicyBuilder.buildPolicy(requestProperties,
                                                                                       policyName, false);
            boolean validateCloud = true;
            if ("false".equalsIgnoreCase(validateCloudStr)) {
                validateCloud = false;
            }
            modifyDBReplProperty(replicationPolicy, AlterDBProperty.SET);
            return submitAndSchedulePolicy(replicationPolicy, validateCloud);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @PUT
    @Path("{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.update")
    public APIResult update(@PathParam("policy-name") String policyName, PropertiesIgnoreCase properties) {
        try {
            LOG.info("Request for policy update is received. Policy-name: [{}]", policyName);
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, policy.getPolicyId());
            ValidationUtil.validatePolicyOnUpdate(policy,  properties);
            updatePolicy(policyName, properties);
            return new APIResult(APIResult.Status.SUCCEEDED, "Policy [{}] update request succeeded.", policyName);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable, Response.Status.BAD_REQUEST);
        }
    }

    @POST
    @Path("dryrun/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.dryrun")
    public APIResult validatePolicy(@PathParam("policy-name") String policyName,
                                    PropertiesIgnoreCase requestProperties) {
        try {
            LOG.info("Request for policy dry-run is received. Policy-name: [{}]", policyName);
            BeaconLogUtils.prefixPolicy(
                    policyName,
                    requestProperties.getPropertyIgnoreCase(ReplicationPolicy.ReplicationPolicyFields.ID.getName()));
            ReplicationPolicy policy = ReplicationPolicyBuilder.buildPolicy(requestProperties, policyName, true);
            return validatePolicyInternal(policy);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @GET
    @Path("list")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.list")
    public PolicyList list(@DefaultValue("") @QueryParam("fields") String fields,
                           @DefaultValue("name") @QueryParam("orderBy") String orderBy,
                           @DefaultValue("") @QueryParam("filterBy") String filterBy,
                           @DefaultValue("asc") @QueryParam("sortOrder") String sortOrder,
                           @DefaultValue("0") @QueryParam("offset") Integer offset,
                           @QueryParam("numResults") Integer resultsPerPage,
                           @DefaultValue("3") @QueryParam("instanceCount") Integer instanceCount) {
        resultsPerPage = resultsPerPage == null ? getDefaultResultsPerPage() : resultsPerPage;
        instanceCount = instanceCount > getMaxInstanceCount() ? getMaxInstanceCount() : instanceCount;
        resultsPerPage = resultsPerPage <= getMaxResultsPerPage() ? resultsPerPage : getMaxResultsPerPage();
        offset = checkAndSetOffset(offset);
        PolicyList policyList = getPolicyList(fields, orderBy, filterBy, sortOrder,
                offset, resultsPerPage, instanceCount);
        LOG.info("Request for policy list is processed successfully. filterBy: [{}]", filterBy);
        return policyList;
    }



    @GET
    @Path("status/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.status")
    public StatusResult status(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for policy status is received. Policy-name: [{}]", policyName);
            String status = fetchPolicyStatus(policyName);
            LOG.info("Request for policy status is processed successfully. Policy-name: [{}]", policyName);
            return new StatusResult(policyName, status);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @GET
    @Path("info/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.replicationPolicyType")
    public APIResult replicationPolicyType(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        String replicationPolicyType = getReplicationType(policyName);
        return new APIResult(APIResult.Status.SUCCEEDED, "Type={}", replicationPolicyType);
    }



    @GET
    @Path("getEntity/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.definition")
    public PolicyList definition(@PathParam("policy-name") String policyName,
                                 @DefaultValue("false") @QueryParam("archived") String archived) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            boolean isArchived = Boolean.parseBoolean(archived);
            LOG.info("Request for policy getEntity is received. policy-name: [{}], isArchived: [{}]", policyName,
                isArchived);
            PolicyList policyList = getPolicyDefinition(policyName, isArchived);
            LOG.info("Request for policy getEntity is processed successfully. policy-name: [{}], isArchived: [{}]",
                policyName, isArchived);
            return policyList;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @DELETE
    @Path("delete/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.delete")
    public APIResult delete(@PathParam("policy-name") String policyName,
                            @DefaultValue("false") @QueryParam("isInternalSyncDelete") boolean isInternalSyncDelete) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for policy delete is received. Policy-name: [{}]", policyName);
            ReplicationPolicy replicationPolicy = policyDao.getActivePolicy(policyName);
            modifyDBReplProperty(replicationPolicy, AlterDBProperty.UNSET);
            APIResult result = deletePolicy(policyName, isInternalSyncDelete);
            if (APIResult.Status.SUCCEEDED == result.getStatus()) {
                LOG.info("Request for policy delete is processed successfully. Policy-name: [{}]", policyName);
            }
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @POST
    @Path("suspend/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.suspend")
    public APIResult suspend(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for policy suspend is received. Policy-name: [{}]", policyName);
            APIResult result = suspendInternal(policyName);
            if (APIResult.Status.SUCCEEDED == result.getStatus()) {
                LOG.info("Request for policy suspend is processed successfully. Policy-name: [{}]", policyName);
            }
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @POST
    @Path("resume/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.resume")
    public APIResult resume(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for policy resume is received. Policy-name: [{}]", policyName);
            APIResult result = resumeInternal(policyName);
            if (APIResult.Status.SUCCEEDED == result.getStatus()) {
                LOG.info("Request for policy resume is processed successfully. Policy-name: [{}]", policyName);
            }
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    /**
     * sync the policy in remote beacon cluster.
     * @param policyName
     * @param update True when policy has been updated on the source cluster,
     *               false when policy has been created on the source cluster.
     * @param requestProperties
     * @return
     */
    @POST
    @Path("sync/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.sync")
    public APIResult syncPolicy(@PathParam("policy-name") String policyName,
                                @QueryParam("update") boolean update,
                                PropertiesIgnoreCase requestProperties) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            BeaconLogUtils.prefixPolicy(
                    policyName,
                    requestProperties.getPropertyIgnoreCase(ReplicationPolicy.ReplicationPolicyFields.ID.getName()));
            String id = requestProperties.getPropertyIgnoreCase(ReplicationPolicy.ReplicationPolicyFields.ID.getName());
            String executionType = requestProperties.getPropertyIgnoreCase(
                    ReplicationPolicy.ReplicationPolicyFields.EXECUTIONTYPE.getName());
            LOG.info("Request for policy sync is received. Policy-name: [{}], id: [{}]", policyName, id);
            if (StringUtils.isBlank(id)) {
                LOG.error("Internal error. Policy id should be present during policy sync.");
                throw BeaconWebException.newAPIException("Policy id should be present during sync.");
            }
            requestProperties.remove(ReplicationPolicy.ReplicationPolicyFields.ID.getName());
            requestProperties.remove(ReplicationPolicy.ReplicationPolicyFields.EXECUTIONTYPE.getName());
            ReplicationPolicy replicationPolicy = ReplicationPolicyBuilder.buildPolicy(requestProperties, policyName,
                    false);
            modifyDBReplProperty(replicationPolicy, AlterDBProperty.SET);
            APIResult result = syncPolicy(policyName, requestProperties, id, executionType, update);
            if (APIResult.Status.SUCCEEDED == result.getStatus()) {
                LOG.info("Request for policy sync is processed successfully. Policy-name: [{}]", policyName);
            }
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @POST
    @Path("syncStatus/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.syncStatus")
    public APIResult syncPolicyStatus(@PathParam("policy-name") String policyName,
                                      @QueryParam("status") String status,
                                      @DefaultValue("false") @QueryParam("isInternalStatusSync")
                                              boolean isInternalStatusSync) {
        BeaconLogUtils.prefixPolicy(policyName);
        if (StringUtils.isBlank(status)) {
            throw BeaconWebException.newAPIException("Query param status cannot be null or empty");
        }
        try {
            LOG.info("Request for policy syncStatus is received. Policy-name: [{}], status: [{}]", policyName, status);
            APIResult result = syncPolicyStatusInternal(policyName, status, isInternalStatusSync);
            if (APIResult.Status.SUCCEEDED == result.getStatus()) {
                LOG.info("Request for policy syncStatus is processed successfully. Policy-name: [{}], status: [{}]",
                    policyName, status);
            }
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @GET
    @Path("instance/list/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.listInstances")
    public PolicyInstanceList listPolicyInstances(@PathParam("policy-name") String policyName,
                                                  @QueryParam("filterBy") String filters,
                                                  @DefaultValue("startTime") @QueryParam("orderBy") String orderBy,
                                                  @DefaultValue("DESC") @QueryParam("sortOrder") String sortOrder,
                                                  @DefaultValue("0") @QueryParam("offset") Integer offset,
                                                  @QueryParam("numResults") Integer resultsPerPage,
                                                  @DefaultValue("false") @QueryParam("archived") String archived) {
        BeaconLogUtils.prefixPolicy(policyName);
        resultsPerPage = resultsPerPage == null ? getDefaultResultsPerPage() : resultsPerPage;
        try {
            boolean isArchived = Boolean.parseBoolean(archived);
            return listPolicyInstance(policyName, filters, orderBy, sortOrder, offset, resultsPerPage, isArchived);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }


    @POST
    @Path("instance/abort/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.abort")
    public APIResult abortPolicyInstance(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for abort policy instance is received. Policy-name: [{}]", policyName);
            APIResult result = abortPolicyInstanceInternal(policyName);
            LOG.info("Request for abort policy instance is processed successfully. Policy-name: [{}]", policyName);
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    @POST
    @Path("instance/rerun/{policy-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
    @Timed(absolute = true, name="api.beacon.policy.rerun")
    public APIResult rerunPolicyInstance(@PathParam("policy-name") String policyName) {
        BeaconLogUtils.prefixPolicy(policyName);
        try {
            LOG.info("Request for rerun policy instance is received. Policy-name: [{}]", policyName);
            APIResult result = rerunPolicyInstanceInternal(policyName);
            LOG.info("Request for rerun policy instance is processed successfully. Policy-name: [{}]", policyName);
            return result;
        } catch (BeaconWebException e) {
            throw e;
        } catch (Throwable throwable) {
            throw BeaconWebException.newAPIException(throwable);
        }
    }

    private synchronized APIResult submit(ReplicationPolicy policy, boolean syncWithPeer) throws BeaconWebException {
        try {
            RequestContext.get().startTransaction();
            submitInternal(policy, syncWithPeer);
            RequestContext.get().commitTransaction();
            return new APIResult(APIResult.Status.SUCCEEDED, "Submit successful {}: {}", policy.getEntityType(),
                    policy.getName());
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private void submitInternal(ReplicationPolicy policy, boolean syncWithPeer) throws BeaconException {
        policyDao.retireCompletedPolicy(policy.getName());
        policyDao.persistPolicy(policy);
        // Sync the policy with remote cluster
        if (syncWithPeer) {
            syncPolicyInRemote(policy, false);
        }
        //Sync Event is true, if current cluster is equal to source cluster.
        boolean syncEvent = StringUtils.isNotBlank(policy.getSourceCluster())
                && (policy.getSourceCluster()).equals(ClusterHelper.getLocalCluster().getName());
        BeaconEvents.createEvents(Events.SUBMITTED, EventEntityType.POLICY,
                policyDao.getPolicyBean(policy), getEventInfo(policy, syncEvent));
        LOG.info("Request for submit policy is processed successfully. Policy-name: [{}]", policy.getName());
    }

    private synchronized APIResult updatePolicy(String policyName, PropertiesIgnoreCase properties)
            throws BeaconException {
        try {
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            boolean requireReschedule = requireJobReschedule(policy, properties);
            updateSubmittedPolicy(policy, properties, true);
            if (requireReschedule) {
                rescheduleJob(policy, properties);
            }
            return new APIResult(APIResult.Status.SUCCEEDED, "Update policy successful ({})", policyName);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        }
    }

    private static boolean requireJobReschedule(ReplicationPolicy policy, PropertiesIgnoreCase props) {
        String startTimeStr = props.getPropertyIgnoreCase(ReplicationPolicyProperties.STARTTIME.getName());
        if (startTimeStr != null && !DateUtil.parseDate(startTimeStr).equals(policy.getStartTime())) {
            return true;
        }
        String endTimeStr = props.getPropertyIgnoreCase(ReplicationPolicyProperties.ENDTIME.getName());
        if (endTimeStr != null && !DateUtil.parseDate(endTimeStr).equals(policy.getEndTime())) {
            return true;
        }
        String freqStr = props.getPropertyIgnoreCase(ReplicationPolicyProperties.FREQUENCY.getName());
        if (freqStr != null && !(Integer.parseInt(freqStr) == policy.getFrequencyInSec())) {
            return true;
        }
        return false;
    }

    private APIResult updateSubmittedPolicy(ReplicationPolicy policy, PropertiesIgnoreCase properties,
                                            boolean syncWithPeer) throws BeaconException {
        try {
            RequestContext.get().startTransaction();
            if (!syncWithPeer) {
                ValidationUtil.validatePolicyFields(policy, properties);
            }
            updatePolicyInternal(policy, properties, syncWithPeer);
            RequestContext.get().commitTransaction();
            return new APIResult(APIResult.Status.SUCCEEDED, "Update successful {}: {}", policy.getEntityType(),
                    policy.getName());
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private void updatePolicyInternal(ReplicationPolicy policy, PropertiesIgnoreCase properties,
                                      boolean syncWithPeer) throws BeaconException {
        // Prepare policy objects for existing and updated request.
        ReplicationPolicy updatedPolicy = ReplicationPolicyBuilder.buildPolicyWithPartialData(
                                                                                          properties, policy.getName());
        if (updatedPolicy.getFrequencyInSec() == -1) {
            //set it to the active policy value;
            updatedPolicy.setFrequencyInSec(policyDao.getActivePolicy(policy.getName()).getFrequencyInSec());
        }
        // Prepare for policy update into store
        PropertiesIgnoreCase updatedProps = new PropertiesIgnoreCase();
        PropertiesIgnoreCase newProps = new PropertiesIgnoreCase();
        findUpdatedAndNewCustomProps(updatedPolicy, policy, updatedProps, newProps);
        boolean snapshotsWasEnabled = Boolean.parseBoolean(policy.getEnableSnapshotBasedReplication());
        ReplicationPolicy modifiedExistingPolicy = policy;
        addNewPropsAndUpdateOlderPropsValue(modifiedExistingPolicy, updatedPolicy, newProps, updatedProps);
        // persist policy update information
        policyDao.persistUpdatedPolicy(modifiedExistingPolicy, updatedProps, newProps);

        if (syncWithPeer) {
            modifiedExistingPolicy.setStartTime(updatedPolicy.getStartTime());
            syncPolicyInRemote(modifiedExistingPolicy, true);
        }
        boolean snapshotsIsEnabled = Boolean.parseBoolean(modifiedExistingPolicy.getEnableSnapshotBasedReplication());
        if (snapshotsWasEnabled && !snapshotsIsEnabled) {
            deleteExistingAndDisallowSnapshots(modifiedExistingPolicy);
        } else {
            LOG.debug("Skipping snapshots deletion as snapshotsWasEnabled:{}, snapshotsIsEnabled:{} ",
                    snapshotsWasEnabled, snapshotsIsEnabled);
        }

        LOG.info("Request for update policy is processed successfully. Policy-name: [{}]", policy.getName());

    }

    private void deleteExistingAndDisallowSnapshots(ReplicationPolicy policy)
            throws BeaconException {
        try {
            String snapshotNamePrefix = FSSnapshotUtils.getSnapshotNamePrefix(policy.getName());
            boolean isSourceClusterLocal = true;
            boolean isHCFS = PolicyHelper.isPolicyHCFS(policy);
            if (isHCFS) {
                if (policy.getSourceCluster() != null) {
                    isSourceClusterLocal = true;
                } else {
                    isSourceClusterLocal = false;
                }
            } else {
                Cluster srcCluster = clusterDao.getActiveCluster(policy.getSourceCluster());
                isSourceClusterLocal = srcCluster.isLocal();
            }
            if (isSourceClusterLocal) {
                String sourceDataset = policy.getSourceDataset();
                DataSet srcDataSet = null;
                try {
                    srcDataSet = DataSet.create(sourceDataset, policy.getSourceCluster(), policy);
                    deleteExistingSnapshots(policy.getName(), policy.getSourceCluster(), srcDataSet,
                            snapshotNamePrefix);
                    disallowSnapshots(policy.getName(), policy.getSourceCluster(), srcDataSet);
                } finally {
                    if (srcDataSet != null) {
                        srcDataSet.close();
                    }
                }
            } else {
                String targetDataset = policy.getTargetDataset();
                DataSet tgtDataSet = null;
                try {
                    tgtDataSet = DataSet.create(targetDataset, policy.getTargetCluster(), policy);
                    deleteExistingSnapshots(policy.getName(), policy.getTargetCluster(), tgtDataSet,
                            snapshotNamePrefix);
                    disallowSnapshots(policy.getName(), policy.getTargetCluster(), tgtDataSet);
                } finally {
                    if (targetDataset != null) {
                        tgtDataSet.close();
                    }
                }
            }
        } catch (IOException ex) {
            throw new BeaconException(ex);
        }
    }

    private void deleteExistingSnapshots(String policyName, String clusterName, DataSet dataSet,
                                         String snapshotNamePrefix) throws IOException {
        if (dataSet.isSnapshottable()) {
            LOG.info("Deleting existing snapshot(s) for dataset [{}]", dataSet);
            dataSet.deleteAllSnapshots(snapshotNamePrefix);
        } else {
            LOG.warn("Target snapshots dir for dataset [{}] does not exist", dataSet);
        }
    }

    private void disallowSnapshots(String policyName, String clusterName, DataSet dataSet)
            throws BeaconException, IOException {
        try {
            boolean datasetInRepl = isDataSetInReplication(dataSet.toString(), policyName);
            if (!datasetInRepl) {
                Cluster cluster = clusterDao.getActiveCluster(clusterName);
                FSSnapshotUtils.disallowSnapshot(cluster, dataSet);
                LOG.info("Disallowed snapshots for dataset[{}]", dataSet.toString());
            } else {
                LOG.info("Active policies are using the datasource [{}], so can not disallow snapshots", dataSet);
            }
        } catch (org.apache.hadoop.hdfs.protocol.SnapshotException ex) {
            //Just log the exception and ignore
            LOG.warn("Couldn't disallow the snapshots on dataset {}, exception message - {}", dataSet.toString(),
                    ex.getMessage());
        }
    }

    private boolean isDataSetInReplication(String dataset, String currentPolicy) throws BeaconStoreException {
        PolicyExecutor policyExecutor = new PolicyExecutor(new PolicyBean(ReplicationType.FS.getName()));
        List<PolicyBean> policyBeanList = policyExecutor.getPolicies(PolicyExecutor.PolicyQuery.GET_POLICIES_FOR_TYPE);
        boolean datasetInRepl = false;
        for (PolicyBean policyBean: policyBeanList) {
            if ((dataset.equals(policyBean.getSourceDataset())
                    || dataset.equals(policyBean.getTargetDataset())) && !policyBean.getName().equals(currentPolicy)) {
                datasetInRepl = true;
                break;
            }
        }
        return datasetInRepl;
    }

    private void addNewPropsAndUpdateOlderPropsValue(ReplicationPolicy modifiedExistingPolicy,
                                                     ReplicationPolicy updatedPolicy, PropertiesIgnoreCase newProps,
                                                     PropertiesIgnoreCase updatedProps) {
        Properties customProps = modifiedExistingPolicy.getCustomProperties();
        customProps.putAll(newProps);
        customProps.putAll(updatedProps);
        if (StringUtils.isNotBlank(updatedPolicy.getDescription())) {
            modifiedExistingPolicy.setDescription(updatedPolicy.getDescription());
        }
        if (updatedPolicy.getStartTime() != null) {
            modifiedExistingPolicy.setStartTime(updatedPolicy.getStartTime());
        }
        if (updatedPolicy.getEndTime() != null) {
            modifiedExistingPolicy.setEndTime(updatedPolicy.getEndTime());
        }
        if (updatedPolicy.getPlugins() != null) {
            modifiedExistingPolicy.setPlugins(updatedPolicy.getPlugins());
        }
        modifiedExistingPolicy.setFrequencyInSec(updatedPolicy.getFrequencyInSec());
    }

    private void rescheduleJob(ReplicationPolicy policy, PropertiesIgnoreCase properties)
            throws BeaconWebException, BeaconException {
        BeaconScheduler scheduler = getScheduler();
        scheduler.reschedulePolicy(policy.getPolicyId(), policy.getStartTime(), policy.getEndTime(),
                policy.getFrequencyInSec());
    }

    private APIResult syncPolicy(String policyName, PropertiesIgnoreCase requestProperties, String id,
                                 String executionType, boolean update) throws BeaconException {
        String message = null;
        ReplicationPolicy policy = null;
        if (update) {
            policy = policyDao.getActivePolicy(policyName);
            updateSubmittedPolicy(policy, requestProperties, false);
            message = "Update and sync policy successful ({})";
        } else {
            policy = ReplicationPolicyBuilder.buildPolicy(requestProperties, policyName, false);
            policy.setPolicyId(id);
            policy.setExecutionType(executionType);
            submit(policy, false);
            message = "Submit and sync policy successful ({})";
        }
        return new APIResult(APIResult.Status.SUCCEEDED, message, policyName);
    }

    private void findUpdatedAndNewCustomProps(ReplicationPolicy updatedPolicy, ReplicationPolicy existingPolicy,
                                              PropertiesIgnoreCase updatedProps, PropertiesIgnoreCase newProps) {
        Properties existingPolicyCustomProps = existingPolicy.getCustomProperties();
        Properties updatedClusterCustomProps = updatedPolicy.getCustomProperties();
        for (String property : updatedClusterCustomProps.stringPropertyNames()) {
            if (existingPolicyCustomProps.getProperty(property) != null) {
                updatedProps.setProperty(property, updatedClusterCustomProps.getProperty(property));
            } else {
                newProps.setProperty(property, updatedClusterCustomProps.getProperty(property));
            }
        }
    }

    public void scheduleInternal(ReplicationPolicy policy) throws BeaconException {
        try {
            RequestContext.get().startTransaction();
            ValidationUtil.validateIfAPIRequestAllowed(policy);

            BeaconScheduler scheduler = getScheduler();
            scheduler.schedulePolicy(false, policy.getName(), policy.getPolicyId(), policy.getStartTime(),
                    policy.getEndTime(), policy.getFrequencyInSec());
            policyDao.updatePolicyStatus(policy.getPolicyId(), JobStatus.RUNNING.name());
            BeaconEvents.createEvents(Events.SCHEDULED, EventEntityType.POLICY,
                    policyDao.getPolicyBean(policy), getEventInfo(policy, false));
            RequestContext.get().commitTransaction();
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private APIResult submitAndSchedulePolicy(ReplicationPolicy replicationPolicy, boolean validateCloud) {
        try {
            String policyName = replicationPolicy.getName();
            String executionType = ReplicationUtils.getReplicationPolicyType(replicationPolicy);
            replicationPolicy.setExecutionType(executionType);
            ValidationUtil.validationOnSubmission(replicationPolicy, validateCloud);
            validate(replicationPolicy);
            submit(replicationPolicy, true);
            schedule(replicationPolicy);
            LOG.info("Request for policy submitAndSchedule is processed successfully. Policy-name: [{}]", policyName);
            return new APIResult(APIResult.Status.SUCCEEDED, "Policy [{}] submitAndSchedule successful", policyName);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e, Response.Status.BAD_REQUEST);
        }
    }

    private APIResult validatePolicyInternal(ReplicationPolicy replicationPolicy) throws BeaconException {
        try {
            String policyName = replicationPolicy.getName();
            String executionType = ReplicationUtils.getReplicationPolicyType(replicationPolicy);
            replicationPolicy.setExecutionType(executionType);
            ValidationUtil.validationOnSubmission(replicationPolicy, true);
            LOG.info("Request for policy dry run is processed successfully. Policy-name: [{}]", policyName);
            return new APIResult(APIResult.Status.SUCCEEDED, "Policy [{}] dry-run successful", policyName);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e, Response.Status.BAD_REQUEST);
        }
    }

    private void schedule(ReplicationPolicy policy) throws BeaconException {
        try {
            scheduleInternal(policy);
            // Sync status in remote
            syncPolicyStatusInRemote(policy, Entity.EntityStatus.RUNNING.name());
        } catch (Exception e) {
            LOG.error("Exception while scheduling policy: [{}]", policy.getName(), e);
        }
    }

    private APIResult suspendInternal(String policyName) {
        try {
            RequestContext.get().startTransaction();
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, policy.getPolicyId());
            ValidationUtil.validateIfAPIRequestAllowed(policy);
            String policyStatus = policy.getStatus();
            if (policyStatus.equalsIgnoreCase(JobStatus.RUNNING.name())) {
                BeaconScheduler scheduler = getScheduler();
                scheduler.suspendPolicy(policy.getPolicyId());
                policyDao.updatePolicyStatus(policy.getPolicyId(),
                        JobStatus.SUSPENDED.name());
                syncPolicyStatusInRemote(policy, JobStatus.SUSPENDED.name());
            } else {
                throw BeaconWebException.newAPIException("{} ({}) cannot be suspended. Current status: {}",
                    policy.getName(), policy.getType(), policyStatus);
            }

            BeaconEvents.createEvents(Events.SUSPENDED, EventEntityType.POLICY,
                    policyDao.getPolicyBean(policy), getEventInfo(policy, false));
            RequestContext.get().commitTransaction();
            return new APIResult(APIResult.Status.SUCCEEDED, "{} ({}) suspended successfully", policy.getName(),
                    policy.getType());
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private APIResult resumeInternal(String policyName) {
        try {
            RequestContext.get().startTransaction();
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, policy.getPolicyId());
            ValidationUtil.validateIfAPIRequestAllowed(policy);
            if (!PolicyHelper.isPolicyHCFS(policy.getSourceDataset(), policy.getTargetDataset())) {
                ClusterHelper.validateIfClustersPaired(policy.getSourceCluster(), policy.getTargetCluster());
            }
            String policyStatus = policy.getStatus();
            if (policyStatus.equalsIgnoreCase(Entity.EntityStatus.SUSPENDED.name())
                    || policyStatus.equalsIgnoreCase(Entity.EntityStatus.SUSPENDEDFORINTERVENTION.name())) {
                BeaconScheduler scheduler = getScheduler();
                scheduler.resumePolicy(policy.getPolicyId());
                String status = Entity.EntityStatus.RUNNING.name();
                policyDao.updatePolicyStatus(policy.getPolicyId(),
                        JobStatus.RUNNING.name());
                syncPolicyStatusInRemote(policy, status);
            } else {
                throw new IllegalStateException(StringFormat.format("{} ({}) cannot be resumed. Current status: {}",
                    policy.getName(), policy.getType(), policyStatus));
            }
            BeaconEvents.createEvents(Events.RESUMED, EventEntityType.POLICY,
                    policyDao.getPolicyBean(policy), getEventInfo(policy, false));
            RequestContext.get().commitTransaction();
            return new APIResult(APIResult.Status.SUCCEEDED, "{} ({}) resumed successfully", policy.getName(),
                    policy.getType());
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private PolicyList getPolicyList(String fieldStr, String orderBy, String filterBy,
                                     String sortOrder, Integer offset, Integer resultsPerPage, int instanceCount) {
        try {
            return policyDao.getFilteredPolicy(fieldStr, filterBy, orderBy, sortOrder,
                    offset, resultsPerPage, instanceCount);
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e);
        }
    }

    private String fetchPolicyStatus(String name) {
        try {
            return policyDao.getPolicyStatus(name);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e);
        }
    }

    private String getReplicationType(String policyName) {
        String replicationPolicyType;
        try {
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            replicationPolicyType = getReplicationType(policy);
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        }
        return replicationPolicyType;
    }

    private PolicyList getPolicyDefinition(String name, boolean isArchived) {
        try {
            return policyDao.getPolicyDefinitions(name, isArchived);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        }
    }

    private APIResult deletePolicy(String policyName, boolean isInternalSyncDelete) throws BeaconException {
        try {
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, policy.getPolicyId());
            if (!isInternalSyncDelete) {
                ValidationUtil.validateIfAPIRequestAllowed(policy);
            }
            return deletePolicy(policy, isInternalSyncDelete);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        }
    }

    private APIResult deletePolicy(ReplicationPolicy policy, boolean isInternalSyncDelete) throws BeaconException {
        boolean syncEvent = false;
        boolean schedulerJobDelete;
        try {
            RequestContext.get().startTransaction();
            String status = policy.getStatus();
            // This is not a sync call
            Date retirementTime = new Date();
            if (!isInternalSyncDelete) {
                // The status of the policy is not submitted.
                if (!JobStatus.SUBMITTED.name().equalsIgnoreCase(status)) {
                    List<PolicyInstanceBean> instances = policyDao.getPolicyInstance(
                            policy.getPolicyId());
                    // For a failed running instance retry is scheduled, in mean time user issues the
                    // policy deletion operation, so move the instance to DELETED state from RUNNING.
                    policyDao.updateInstanceStatus(policy.getPolicyId());
                    policyDao.deletePolicy(policy.getName(), retirementTime);
                    schedulerJobDelete = getScheduler().deletePolicy(policy.getPolicyId());
                } else {
                    // Status of the policy is submitted.
                    policyDao.deletePolicy(policy.getName(), retirementTime);
                    schedulerJobDelete = true;
                }
            } else {
                // This is a sync call.
                syncEvent = (policy.getSourceCluster()).equals(ClusterHelper.getLocalCluster().getName());
                policyDao.deletePolicy(policy.getName(), retirementTime);
                schedulerJobDelete = true;
            }
            // Check policy is deleted from scheduler and commit.
            if (schedulerJobDelete) {
                BeaconEvents.createEvents(Events.DELETED, EventEntityType.POLICY,
                        policyDao.getPolicyBean(policy), getEventInfo(policy, syncEvent));
                RequestContext.get().commitTransaction();
            } else {
                throw new BeaconException("Failed to delete policy from beacon scheduler name: {}, type: {}",
                    policy.getName(), policy.getType());
            }

            //Call syncDelete when scheduler job is deleted (true) and not a sync delete call.
            if (!isInternalSyncDelete) {
                try {
                    syncDeletePolicyToRemote(policy);
                } catch (Exception e) {
                    return new APIResult(APIResult.Status.SUCCEEDED,
                        "Policy [{}] deleted from target cluster but failed to delete on source cluster.",
                        policy.getName());
                }
            }
            return new APIResult(APIResult.Status.SUCCEEDED, "{} ({}) removed successfully.", policy.getName(),
                policy.getType());
        } catch (BeaconException e) {
            throw BeaconWebException.newAPIException(e);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private PolicyInstanceList listPolicyInstance(String policyName, String filters, String orderBy, String sortOrder,
                                Integer offset, Integer resultsPerPage, boolean isArchived) throws BeaconException {
        if (!isArchived) {
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            ValidationUtil.validateIfAPIRequestAllowed(policy);
        }

        StringBuilder newFilters = new StringBuilder();
        if (StringUtils.isNotBlank(filters)) {
            String[] filtersArray = filters.split(BeaconConstants.COMMA_SEPARATOR);
            List<String> asList = Arrays.asList(filtersArray);
            for (String str : asList) {
                if (str.startsWith("name" + BeaconConstants.COLON_SEPARATOR)) {
                    continue;
                }
                newFilters.append(str).append(BeaconConstants.COMMA_SEPARATOR);
            }
        }
        newFilters.append("name" + BeaconConstants.COLON_SEPARATOR).append(policyName);
        filters = newFilters.toString();
        return listInstance(filters, orderBy, sortOrder, offset, resultsPerPage, isArchived);
    }


    private APIResult abortPolicyInstanceInternal(String policyName) {
        try {
            ReplicationPolicy activePolicy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, activePolicy.getPolicyId());
            String status = activePolicy.getStatus();
            if (JobStatus.SUBMITTED.name().equalsIgnoreCase(status)
                    || COMPLETION_STATUS.contains(status.toUpperCase())) {
                throw BeaconWebException.newAPIException("Policy [{}] is not in [RUNNING] state. Current status [{}]",
                    policyName, status);
            }
            BeaconScheduler scheduler = getScheduler();
            boolean abortStatus = scheduler.abortInstance(activePolicy.getPolicyId());
            return new APIResult(APIResult.Status.SUCCEEDED, "Policy instance abort status [{}]", abortStatus);
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        }
    }

    private APIResult rerunPolicyInstanceInternal(String policyName) {
        try {
            RequestContext.get().startTransaction();
            ReplicationPolicy activePolicy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, activePolicy.getPolicyId());
            String status = activePolicy.getStatus();
            // Policy should be in the RUNNING state.
            if (!JobStatus.RUNNING.name().equalsIgnoreCase(status)) {
                throw BeaconWebException.newAPIException("Policy [{}] is not in [RUNNING] state. Current status [{}]",
                    policyName, status);
            }
            PolicyInstanceBean latestInstance = policyDao.getInstanceForRerun(activePolicy.getPolicyId());
            status = latestInstance.getStatus();
            /**
             * For rerun to proceed last should be one of {@link JobStatus#getAllowedRerunStatus()}
             */
            if (status != null && JobStatus.getAllowedRerunStatus().contains(JobStatus.valueOf(status))) {
                BeaconScheduler scheduler = getScheduler();
                boolean isRerun = scheduler.rerunPolicyInstance(activePolicy.getPolicyId(),
                        String.valueOf(latestInstance.getCurrentOffset()), latestInstance.getInstanceId());
                if (isRerun) {
                    policyDao.updateInstanceRerun(latestInstance.getInstanceId());
                    RequestContext.get().commitTransaction();
                    return new APIResult(APIResult.Status.SUCCEEDED,
                        "Policy instance {} is scheduled for immediate rerun successfully.",
                        latestInstance.getInstanceId());
                } else {
                    RequestContext.get().commitTransaction();
                    return new APIResult(APIResult.Status.FAILED,
                        "Policy instance {} is not scheduled for rerun into scheduler.",
                        latestInstance.getInstanceId());
                }
            } else {
                throw BeaconWebException.newAPIException(
                    "Policy instance is not in {} state. Last instance: {} status: {}.",
                        JobStatus.getAllowedRerunStatus(),
                        latestInstance.getInstanceId(), status);
            }
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (BeaconWebException e) {
            throw e;
        } catch (Exception e) {
            throw BeaconWebException.newAPIException(e, Response.Status.BAD_REQUEST);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    // TODO : In future when house keeping async is added ignore any errors as this will be retried async
    private void syncDeletePolicyToRemote(ReplicationPolicy policy) throws BeaconException {
        if (PolicyHelper.isPolicyHCFS(policy.getSourceDataset(), policy.getTargetDataset())) {
            // No policy sync delete needed for HCFS
            return;
        }

        String remoteEndPoint = PolicyHelper.getRemoteBeaconEndpoint(policy);
        String remoteClusterName = PolicyHelper.getRemoteClusterName(policy);
        String remoteKnoxURL = PolicyHelper.getRemoteKnoxBaseURL(policy);
        try {
            BeaconClient remoteClient = new BeaconWebClient(remoteEndPoint, remoteKnoxURL);
            remoteClient.deletePolicy(policy.getName(), true);
            checkAndDeleteSyncStatus(policy.getName());
        } catch (BeaconClientException e) {
            LOG.error("Remote cluster {} returned error: {}", remoteClusterName, e.getMessage());
            scheduleSyncPolicyDelete(remoteEndPoint, remoteKnoxURL, policy.getName(), e);
        } catch (Exception e) {
            LOG.error("Exception while sync delete policy to remote cluster: {}.", remoteClusterName, e);
            scheduleSyncPolicyDelete(remoteEndPoint, remoteKnoxURL, policy.getName(), e);
        }
    }

    private void syncPolicyStatusInRemote(ReplicationPolicy policy, String status) throws BeaconException {
        if (PolicyHelper.isPolicyHCFS(policy.getSourceDataset(), policy.getTargetDataset())) {
            // No policy status sync needed for HCFS
            return;
        }
        String remoteBeaconEndpoint = PolicyHelper.getRemoteBeaconEndpoint(policy);
        String remoteKnoxURL = PolicyHelper.getRemoteKnoxBaseURL(policy);

        try {
            //TODO Check is there any sync status job scheduled. removed them and update it.
            BeaconClient remoteClient = BeaconClientFactory.getBeaconClient(remoteBeaconEndpoint,
                    PolicyHelper.getRemoteKnoxBaseURL(policy));
            remoteClient.syncPolicyStatus(policy.getName(), status, true);
            checkAndDeleteSyncStatus(policy.getName());
        } catch (Exception e) {
            LOG.error("Exception while sync status for policy: [{}].", policy.getName(), e);
            scheduleSyncStatus(policy, status, remoteBeaconEndpoint, remoteKnoxURL, e);
        }
    }

    private void syncPolicyInRemote(ReplicationPolicy policy, boolean update)
            throws BeaconException {
        if (PolicyHelper.isPolicyHCFS(policy.getSourceDataset(), policy.getTargetDataset())) {
            // No policy sync needed for HCFS
            return;
        }
        syncPolicyInRemote(policy,
                PolicyHelper.getRemoteBeaconEndpoint(policy), PolicyHelper.getRemoteKnoxBaseURL(policy), update);
    }

    private void modifyDBReplProperty(ReplicationPolicy policy, AlterDBProperty alterDBProperty)
            throws BeaconException {
        String sourceCluster = policy.getSourceCluster();
        Cluster cluster = ClusterHelper.getLocalCluster();
        if (cluster.getName().equalsIgnoreCase(sourceCluster)) {
            ReplicationType replType = ReplicationHelper.getReplicationType(policy.getType());
            if (replType == ReplicationType.HIVE) {
                HiveMetadataClient hiveMetadataClient = null;
                try {
                    hiveMetadataClient = HiveClientFactory.getMetadataClient(cluster);
                    String dbName = policy.getSourceDataset();
                    String policyName = policy.getName();
                    String propertyKey = "repl.source.for";
                    String propertyValue = hiveMetadataClient.getDatabaseProperty(dbName, propertyKey);
                    if (alterDBProperty == AlterDBProperty.SET) {
                        setDBReplProperty(hiveMetadataClient, dbName, propertyKey, propertyValue, policyName);
                    } else {
                        unsetDBReplProperty(hiveMetadataClient, dbName, propertyKey, propertyValue, policyName);
                    }
                } finally {
                    HiveClientFactory.close(hiveMetadataClient);
                }
            }
        }
    }

    private void setDBReplProperty(HiveMetadataClient hiveMetadataClient, String dbName, String propertyKey,
                                   String existingPropertyValue, String newPropertyValue) throws BeaconException {
        List<String> policyNameList;
        if (StringUtils.isNotEmpty(existingPropertyValue)) {
            policyNameList = new ArrayList<>(Arrays.asList(existingPropertyValue.split(BeaconConstants
                    .SEMICOLON_SEPARATOR)));
        } else {
            policyNameList = new ArrayList<>();
        }
        if (policyNameList.size() == 0 || !policyNameList.contains(newPropertyValue)) {
            synchronized(this) {
                if (policyNameList.size() == 0 || !policyNameList.contains(newPropertyValue)) {
                    LOG.info("Setting policy {} to DB {} property for repl", newPropertyValue, dbName);
                    policyNameList.add(newPropertyValue);
                    String newPolicyNameList = Joiner.on(BeaconConstants.SEMICOLON_SEPARATOR).skipNulls()
                            .join(policyNameList);
                    hiveMetadataClient.setDatabaseProperty(dbName, propertyKey, newPolicyNameList);
                }
            }
        }
    }

    private void unsetDBReplProperty(HiveMetadataClient hiveMetadataClient, String dbName, String propertyKey,
                                     String existingPropertyValue, String removePropertyValue) throws BeaconException {
        if (StringUtils.isNotEmpty(existingPropertyValue)) {
            List<String> policyNameList = new ArrayList<>(Arrays.asList(existingPropertyValue.split(BeaconConstants
                    .SEMICOLON_SEPARATOR)));
            if (policyNameList.size() != 0 && policyNameList.contains(removePropertyValue)) {
                synchronized (this) {
                    if (policyNameList.size() != 0 && policyNameList.contains(removePropertyValue)) {
                        LOG.info("Unsetting policy {} from DB {} property for repl", removePropertyValue, dbName);
                        policyNameList.remove(removePropertyValue);
                        String newPolicyNameList = Joiner.on(BeaconConstants.SEMICOLON_SEPARATOR).skipNulls()
                                .join(policyNameList);
                        hiveMetadataClient.setDatabaseProperty(dbName, propertyKey, newPolicyNameList);
                    }
                }
            }
        }
    }

    // TODO : In future when house keeping async is added ignore any errors as this will be retried async
    private void syncPolicyInRemote(ReplicationPolicy policy, String remoteBeaconEndpoint, String knoxBaseUrl,
                                    boolean update) {
        try {
            BeaconClient remoteClient = BeaconClientFactory.getBeaconClient(remoteBeaconEndpoint, knoxBaseUrl);
            remoteClient.syncPolicy(policy.getName(), policy.asProperties(), update);
            BeaconEvents.createEvents(Events.SYNCED, EventEntityType.POLICY,
                    policyDao.getPolicyBean(policy), getEventInfo(policy, false));
        } catch (BeaconClientException e) {
            throw BeaconWebException.newAPIException(
                    Response.Status.fromStatusCode(e.getStatus()), e, "Remote cluster returned error: ");
        }
    }

    private void scheduleSyncPolicyDelete(String remoteEndPoint, String remoteKnoxURL, String policyName, Exception e)
            throws BeaconException {
        AdminJobService service = getAdminJobService();
        if (service != null) {
            SyncPolicyDeleteJob deleteJob = new SyncPolicyDeleteJob(remoteEndPoint, remoteKnoxURL, policyName);
            int frequency = config.getScheduler().getHousekeepingSyncFrequency();
            int maxRetry = config.getScheduler().getHousekeepingSyncMaxRetry();
            service.checkAndSchedule(deleteJob, frequency, maxRetry);
            checkAndDeleteSyncStatus(policyName);
        } else {
            throw new BeaconException(e);
        }
    }

    private void checkAndDeleteSyncStatus(String policyName) throws BeaconException {
        AdminJobService adminJobService = getAdminJobService();
        if (adminJobService != null) {
            SyncStatusJob syncStatusJob = new SyncStatusJob(null, null, policyName, null);
            adminJobService.checkAndDelete(syncStatusJob);
        }
    }

    private void scheduleSyncStatus(ReplicationPolicy policy, String status, String remoteBeaconEndpoint,
                                    String remoteKnoxURL, Exception e)
            throws BeaconException {
        AdminJobService adminJobService = getAdminJobService();
        if (adminJobService != null) {
            SyncStatusJob syncStatusJob = new SyncStatusJob(remoteBeaconEndpoint,
                    remoteKnoxURL, policy.getName(), status);
            int frequency = config.getScheduler().getHousekeepingSyncFrequency();
            int maxRetry = config.getScheduler().getHousekeepingSyncMaxRetry();
            adminJobService.checkAndSchedule(syncStatusJob, frequency, maxRetry);
        } else {
            throw new BeaconException(e);
        }
    }

    private AdminJobService getAdminJobService() {
        AdminJobService adminJobService = null;
        try {
            adminJobService = Services.get().getService(AdminJobService.class);
        } catch (NoSuchElementException e) {
            //AdminJob Service might not be configured, so log the message and processed.
            LOG.error(e.getMessage());
        }
        return adminJobService;
    }

    private APIResult syncPolicyStatusInternal(String policyName, String status,
                                       boolean isInternalStatusSync) throws BeaconException {
        try {
            RequestContext.get().startTransaction();
            ReplicationPolicy policy = policyDao.getActivePolicy(policyName);
            BeaconLogUtils.prefixPolicy(policyName, policy.getPolicyId());
            boolean isCompletionStatus = COMPLETION_STATUS.contains(status.toUpperCase());
            if (isCompletionStatus) {
                policyDao.updateCompletionStatus(policy.getPolicyId(), status.toUpperCase());
            } else {
                policyDao.updatePolicyStatus(policy.getPolicyId(), status);
            }
            RequestContext.get().commitTransaction();
            return new APIResult(APIResult.Status.SUCCEEDED, "Update status succeeded");
        } catch (NoSuchElementException e) {
            throw BeaconWebException.newAPIException(e, Response.Status.NOT_FOUND);
        } catch (Throwable e) {
            throw BeaconWebException.newAPIException(e);
        } finally {
            RequestContext.get().rollbackTransaction();
        }
    }

    private String getReplicationType(final ReplicationPolicy policy) throws BeaconException {
        String replicationPolicyType;
        try {
            replicationPolicyType = ReplicationUtils.getReplicationPolicyType(policy);
        } catch (BeaconException e) {
            throw new BeaconException("Exception while obtaining replication type:", e);
        }
        return replicationPolicyType;
    }

    BeaconScheduler getScheduler() {
        return Services.get().getService(BeaconQuartzScheduler.class);
    }

    private EventInfo getEventInfo(ReplicationPolicy policy, boolean syncEvent) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.updateEventsInfo(policy.getSourceCluster(), policy.getTargetCluster(),
                policy.getSourceDataset(), syncEvent);
        return eventInfo;
    }

    private enum AlterDBProperty {
        SET,
        UNSET
    }
}
