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

package com.hortonworks.beacon.rb;

/**
 * Message ID map enum.
 */
public enum MessageCode {

    //Common Messages
    COMM_010001("Unable to parse retention age limit: {0} {1}"),
    COMM_010002("Missing parameter: {0}"),
    COMM_010003("empty"),
    COMM_010004("success"),
    COMM_010005("Hive Action Type: {0} not supported:"),
    COMM_010006("mkdir failed for {0}"),
    COMM_010007("Invalid policy (Job) type: {0}"),
    COMM_010008("{0} cannot be null or empty"),
    COMM_010009("{0} type: {1} is not supported"),
    COMM_010010("Calculated start date: {0} crossed end date: {1} setting it to entity start date"),
    COMM_010011("Invalid policy (Job) type: {0}"),
    COMM_010012("recover policy instance: [{0}]"),
    COMM_010013("Invalid filter key:value pair provided: {0}"),
    COMM_010014("Invalid filters provided: {0}"),

    //Client module Messages
    CLIE_000001("Invalid entity type: {0}. Expected {1}."),
    CLIE_000002("Unable to initialize Beacon Client object. Cause {0}"),
    CLIE_000003("File not found:"),
    CLIE_000004("Exception occurred in Constructor of API Result: {0}"),
    CLIE_000005("Throwing client exception {0}"),

    //Common module Messages
    COMM_000001("invalid data found while loading the context."),
    COMM_000002("The provided alias {0} cannot be resolved"),
    COMM_000003("The provided configuration cannot be resolved"),
    COMM_000004("Error creating credential entry using the credential provider"),
    COMM_000005("Invalid date format. Valid format: {0}"),
    COMM_000006("Unable to evaluate {0}"),
    COMM_000007("Function not found {0}: {1}"),
    COMM_000008("Invalid boundary {0}"),
    COMM_000009("Exception while getting Distributed FileSystem: {0}"),
    COMM_000010("Exception while getting FileSystem: {0}"),
    COMM_000011("Exception while getting FileSystem. Unable to check TGT for user {0}"),
    COMM_000012("Exception creating FileSystem: {0}"),
    COMM_000013("Cannot get valid scheme for {0}"),
    COMM_000014("Policy of Replication type ({0} is not supported)"),
    COMM_000015("Particular filter key is not supported: {0}"),
    COMM_000016("Parameter {0} not defined"),
    COMM_000017("Filter Pattern constructed :{0}"),
    COMM_000018("Exception occurred in filter validation: {0}"),
    COMM_000019("Exception occurred while fetching logs: {0}"),
    COMM_000020("Log level: {0} not supported"),
    COMM_000021("Fetch Beacon logs for filter {0}"),
    COMM_000022("Creating {0} for the login user {1}, impersonation not required"),
    COMM_000023("Creating FS impersonating user {0}"),
    COMM_000024("{0} is not valid replication type"),
    COMM_000025("Failed to destroy service: {0}"),
    COMM_000026("Service initialized: {0}"),
    COMM_000027("Beacon {0} set to {1}"),
    COMM_000028("beacon properties file {0} does not exist in {1}"),
    COMM_000029("Fallback to classpath for: {0}"),
    COMM_000030("localClusterName not set for engine in beacon yml file"),
    COMM_000031("No properties file loaded"),
    COMM_000032("Unable to load yaml configuration  : "),
    COMM_000033("Initializing service: {0}"),
    COMM_000034("Failed to initialize service: {0} {1}"),
    COMM_000035("Destroying service: {0}"),
    COMM_000036("Unable to get instance for: {0}"),
    COMM_000037("Checking for HCFS Path: {0}"),
    COMM_000038("Instance Execution Details: {0}"),
    COMM_000039("Service destroyed: {0}"),

    //Entity module Messages
    ENTI_000001("No remote beacon endpoint for HCFS policy: {0}"),
    ENTI_000002("{0} time cannot be earlier than {1} time."),
    ENTI_000003("Unhandled entity type: {0}"),
    ENTI_000004("Clusters {0} and {1} are not paired.Pair the clusters before submitting or scheduling the policy"),
    ENTI_000005("HCFS to HCFS replication is not allowed"),
    ENTI_000006("Either sourceCluster or targetCluster should be same as local cluster name: {0}"),
    ENTI_000007("Specified Replication frequency {0} seconds should not be less than {1} seconds"),
    ENTI_000008("Lock acquired for {0} on {1} by {2}"),
    ENTI_000009("Successfully released lock on {0} by {1}"),

    //Job module Messages
    JOBS_000001("Starting the replication job for [{0}], type [{1}]"),

    //Main module Messages
    MAIN_000001("Submit successful {0}: {1}"),
    MAIN_000002("Exception while sync delete policy to remote cluster: {0}."),
    MAIN_000003("Exception while obtain replication type:"),
    MAIN_000004("{0} command is already issued for {1}"),
    MAIN_000005("This operation is not allowed on source cluster: {0}.Try it on target cluster: {1}"),
    MAIN_000006("No jobs to schedule for: {0}"),
    MAIN_000007("{0} ({1}) is cannot be suspended. Current status: {2}"),
    MAIN_000008("{0} ({1}) suspended successfully"),
    MAIN_000009("{0} ({1}) is cannot be resumed. Current status: {2}"),
    MAIN_000010("{0} ({1}) resumed successfully"),
    MAIN_000011("Failed to delete policy from Beacon Scheduler name: {0}, type: {1}"),
    MAIN_000012("{0} ({1}) removed successfully."),
    MAIN_000013("remoteClusterName {0} cannot be same as localClusterName {1}. Cluster cannot be paired with itself"),
    MAIN_000014("Cluster {0} has already been paired with {1}"),
    MAIN_000015("For pairing both local {0} and remote cluster {1} should be submitted."),
    MAIN_000016("Clusters successfully paired"),
    MAIN_000017("Cluster {0} is not yet paired with {1}"),
    MAIN_000018("For unpairing both local {0} and remote cluster {1} should have been submitted and paired."),
    MAIN_000019("Active policies are present, unpair operation can not be done."),
    MAIN_000020("Submit and Sync policy successful ({0})"),
    MAIN_000021("Update status succeeded"),
    MAIN_000022("Event Name: {0} not supported"),
    MAIN_000023("Policy [{0}] is not in [RUNNING] state. Current status [{1}]"),
    MAIN_000024("policy instance abort status [{0}]"),
    MAIN_000025("Remote cluster {0} returned error: {1}"),
    MAIN_000026("Policy id should be present during sync."),
    MAIN_000027("{0} ({1}) scheduled successfully"),
    MAIN_000028("Policy [{0}] submitAndSchedule successful"),
    MAIN_000029("type={0}"),
    MAIN_000030("Clusters successfully unpaired"),
    MAIN_000031("Target Dataset {0} must be same as source dataset: {1}"),
    MAIN_000032("Dataset {0} is already in replication"),
    MAIN_000033("Unable to persist cluster entity"),
    MAIN_000034("Unable to persist entity object"),
    MAIN_000035("No jobs to schedule for: [{0}]"),
    MAIN_000036("Entity schedule failed for name: [{0}], error: {1}"),
    MAIN_000037("Unable to suspend entity: {0}"),
    MAIN_000038("Unable to resume entity: {0}"),
    MAIN_000039("Failed to get {0} list: {1}"),
    MAIN_000040("Unable to get status for policy name: [{0}]"),
    MAIN_000041("Unable to get replication policy type for policy {0} ({1})"),
    MAIN_000042("Unable to policy entity definition for name: [{0}]"),
    MAIN_000043("Unable to get cluster definition for {0}"),
    MAIN_000044("Unable to delete the cluster {0}"),
    MAIN_000045("Unable to pair the clusters {0}"),
    MAIN_000046("Unable to getEntity entity definition from config store for ({0}): {1}"),
    MAIN_000047("Exception while Pairing local cluster to remote: {0}"),
    MAIN_000048("Unable to unpair the clusters {0}"),
    MAIN_000049("Exception while unpairing local cluster to remote: {0}"),
    MAIN_000050("Unable to sync the policy"),
    MAIN_000051("Exception while sync status for policy: [{0}] {1}."),
    MAIN_000052("Entity update status failed for {0}:  in remote cluster {1}"),
    MAIN_000053("All locks released on {0}"),
    MAIN_000054("No locks to release on {0}"),
    MAIN_000055("Exception while sync policy to source cluster: [{0}]"),
    MAIN_000056("Events id: {0} for event name: {1}"),
    MAIN_000057("Find events for the entity type: {0}"),
    MAIN_000058("Get events for type: {0}"),
    MAIN_000059("Exception occurred while obtaining Policy Replication Type: {0}"),
    MAIN_000060("Request for submit policy is received. policy-name: [{0}]"),
    MAIN_000061("Request for submit policy is processed successfully. policy-name: [{0}]"),
    MAIN_000062("Request for policy {0} is received. Policy-name: [{1}]"),
    MAIN_000063("Request for policy {0} is processed successfully. Policy-name: [{1}]"),
    MAIN_000064("Request for policy list is processed successfully. filterBy: [{0}]"),
    MAIN_000065("Request for policy getEntity is received. policy-name: [{0}], isArchived: [{1}]"),
    MAIN_000066("Request for policy getEntity is processed successfully. policy-name: [{0}], isArchived: [{1}]"),
    MAIN_000067("Request for policy sync is received. policy-name: [{0}], id: [{1}]"),
    MAIN_000068("This should never happen. Policy id should be present during policy sync."),
    MAIN_000069("Request for policy syncStatus is received. policy-name: [{0}], status: [{1}]"),
    MAIN_000070("Request for policy syncStatus is processed successfully. policy-name: [{0}], status: [{1}]"),
    MAIN_000071("Request for abort policy instance is received. policy-name: [{0}]"),
    MAIN_000072("Request for abort policy instance is processed successfully. policy-name: [{0}]"),
    MAIN_000073("Listing job instances for policy id: [{0}]"),
    MAIN_000074("Listing job instances completed for policy id: [{0}], size: [{1}]"),
    MAIN_000075("Throwing web exception: {0}"),
    MAIN_000076("calling shutdown hook"),
    MAIN_000077("Shutdown Complete."),
    MAIN_000078("Server shutdown failed with {0}"),
    MAIN_000079("App path: {0}"),
    MAIN_000080("Beacon cluster: {0}"),
    MAIN_000081("Server starting with TLS ? {0} on port {1}"),
    MAIN_000082("Request for policy list is received. filterBy: [{0}]"),
    MAIN_000083("authHandlerName: {0}"),
    MAIN_000084("authHandlerClassName: {0}"),
    MAIN_000085("klass: {0}"),
    MAIN_000086("authHandler: {0}"),
    MAIN_000087("Unable to initialize FileSignerSecretProvider, falling back to use random secrets."),
    MAIN_000088("Invalid AuthenticationToken type"),
    MAIN_000089("AuthenticationToken expired"),
    MAIN_000090("Kerberos doFilter"),
    MAIN_000091("AuthenticationToken ignored: {0}"),
    MAIN_000092("unauthorizedResponse: {0}"),
    MAIN_000093("Authentication exception: {0}"),
    MAIN_000094("sendError"),
    MAIN_000095("Kerberos doFilter = 4"),
    MAIN_000096("userName: {0}"),
    MAIN_000097("CORE_SITE_FILE: {0}"),
    MAIN_000098("HDFS_SITE_FILE: {0}"),
    MAIN_000099("No groups found for user: {0}"),
    MAIN_000100("groupNames: {0}"),
    MAIN_000101("Access Restricted. Could not process the request :: {0}"),
    MAIN_000102("Unauthorized"),
    MAIN_000103("Basic auth user: [{0}]"),
    MAIN_000104("Request URI: {0}"),
    MAIN_000105("Invalid Login credentials"),
    MAIN_000106("Wrong credentials provided for user: {0}"),
    MAIN_000107("Exception: {0}"),
    MAIN_000108("Knox SSO user: [{0}]"),
    MAIN_000109("Request URI: {0}"),
    MAIN_000110("Unable to parse the JWT token"),
    MAIN_000111("There is an active session and if you want local login to beacon, try this on a separate browser"),
    MAIN_000112("After sendRedirect: {0}"),
    MAIN_000113("Expiration time validation of JWT token failed."),
    MAIN_000114("Signature of JWT token could not be verified. Please check the public key"),
    MAIN_000115("SSO signature verification failed.Please check the public key"),
    MAIN_000116("Error while validating signature: {0}"),
    MAIN_000117("SSO expiration date validation failed."),
    MAIN_000118("Public key pem not specified for SSO auth provider. SSO auth will be disabled"),
    MAIN_000119("Unable to read public certificate file. JWT auth will be disabled."),
    MAIN_000120("ServletException while processing the properties"),
    MAIN_000121("CertificateException: {0}"),
    MAIN_000122("Unable to obtain BeaconAuthorizer."),
    MAIN_000123("Error occured when retrieving IP address"),
    MAIN_000124("Invalid action: '{0}'"),
    MAIN_000125("Skipping invalid policy line: {0}"),
    MAIN_000126("SimpleBeaconAuthorizer could not read file due to: {0}"),
    MAIN_000127("SimpleBeaconAuthorizer could not be initialized properly due to: {0}"),
    MAIN_000128("Checking for :: \nUser :: {0}\nGroups :: {1}\nAction :: {2}\nResource :: {3}"),
    MAIN_000129("Error while creating authorizer of type '{0}'"),
    MAIN_000130("Kerberos user: [{0}]"),
    MAIN_000131("Login into Beacon as = {0}"),
    MAIN_000132("Unable to read principal: {0}"),
    MAIN_000133("Kerberos username  from  request >>>>>>>> {0}"),
    MAIN_000134("Exception: {0}"),
    MAIN_000135("Beacon Session doFilter."),
    MAIN_000136("BeaconKerberosAuthenticationFilter initialization started"),
    MAIN_000137("Beacon KNOXSSO AuthenticationFilter doFilter-1."),
    MAIN_000138("Expires: {0}"),
    MAIN_000139("==> SimpleBeaconAuthorizer isAccessAllowed"),
    MAIN_000140("Name is empty. Setting Name Rule as 'DEFAULT'"),
    MAIN_000141("Provided Kerberos Credential : Principal = {0} and Keytab = {1}"),
    MAIN_000142("Starting Jetty Server using kerberos credential"),
    MAIN_000143("Jetty Server failed to start: {0}"),
    MAIN_000144("BeaconAuthorizer not initialized properly, please check the application logs"
        + " and add proper configurations."),
    MAIN_000145("Error while getting application properties."),
    MAIN_000146("Unable to find Beacon Resource corresponding to : {0}\nSetting {1}"),
    MAIN_000147("Failed to get beacon.kerberos.principal. Reason: {0}"),
    MAIN_000148("Authentication type must be specified: {0}|{1}|<class>"),

    //Metrics
    METR_000001("Exception occurred while obtaining job counters: {0}"),

    //Persistence module Messages
    PERS_000001("Cluster entity already exists with name: {0} version: {1}"),
    PERS_000002("Invalid named query parameter passed: {0}"),
    PERS_000003("Cluster entity does not exists name: {0}"),
    PERS_000004("Beacon data store is in inconsistent state. More than 1 result found."),
    PERS_000005("Invalid filter type provided. Input filter type: {0}"),
    PERS_000006("ClusterPair table is in inconsistent state. Number of records found: {0}"),
    PERS_000007("Policy already exists with name: {0}"),
    PERS_000008("Policy does not exists name: {0}"),
    PERS_000009("Beacon data store is in inconsistent state. More than 1 result found.Cluster name: {0}"),
    PERS_000012("Parsing implementation is not present for filter: {0}"),
    PERS_000013("ClusterBean name: [{0}], version [{1}] is stored."),
    PERS_000014("Error while getting the active cluster: [{0}] from store."),
    PERS_000015("Cluster name [{0}] deleted, record updated [{1}]."),
    PERS_000016("Executing cluster list query: [{0}]"),
    PERS_000017("Error while persisting cluster pair data. Cluster name: [{0}], version; [{1}]"),
    PERS_000018("Storing cluster pair data. Source Cluster [{0}, {1}], Remote Cluster [{2}, {3}]"),
    PERS_000019("Cluster pair data stored. Source Cluster [{0}, {1}], Remote Cluster [{2}, {3}]"),
    PERS_000020("No pairing data found. Cluster name: [{0}], version: [{1}]"),
    PERS_000021("Cluster [local: {0}, remote: {1}] pair status: [{2}] updated for [{3}] records."),
    PERS_000022("Error while updating the status: [{0}]"),
    PERS_000023("Exception occurred while adding events: {0}"),
    PERS_000024("named query: {0}"),
    PERS_000025("Executing query: [{0}]"),
    PERS_000026("Executing All events info query: [{0}]"),
    PERS_000027("No job record found for instance id: [{0}], offset: [{1}]"),
    PERS_000028("Error message: {0}"),
    PERS_000029("PolicyBean for name: [{0}], type: [{1}] stored."),
    PERS_000030("Executing get policy for query: {0}"),

    //Plug-in module Messages
    PLUG_000001("Job type {0} not supported"),
    PLUG_000002("Plugin {0} not registered. Cannot perform the job"),
    PLUG_000003("Plugin {0} is in {1} and not in active state"),
    PLUG_000004("Job action type {0} not supported for plugin {1}"),
    PLUG_000005("plugin info cannot be null or empty. Registration failed"),
    PLUG_000006("No such plugin {0} has been registered with Beacon"),
    PLUG_000007("Ranger plugin is not registered. Not adding any Plugin jobs to add."),
    PLUG_000008("No import needed for dataset: {0}"),
    PLUG_000009("Cannot find implementation for: {0}"),
    PLUG_000010("Ranger plugin is in Invalid state. Not registering any other Plugins."),
    PLUG_000011("Plugin {0} is in Invalid state. Not registering."),
    PLUG_000012("Registering plugin: {0}"),
    PLUG_000013("Plugin dependencies: {0}"),
    PLUG_000014("Plugin description: {0}"),
    PLUG_000015("Plugin staging dir: {0}"),
    PLUG_000016("Plugin version: {0}"),
    PLUG_000017("Plugin ignore failures for plugin jobs: {0}"),
    PLUG_000018("No registered plugins"),

    //Replication module Messages
    REPL_000001("No instance tracking info found for instance: {0}"),
    REPL_000002("Policy Type {0} not supported"),
    REPL_000003("Missing DR property for FS Replication: {0}"),
    REPL_000004("Exception occurred in init: {0}"),
    REPL_000005("Error occurred when checking target dir: {0} exists"),
    REPL_000006("Exception occurred while handling snapshot: {0}"),
    REPL_000007("Job exception occurred: {0}"),
    REPL_000008("Exception occurred while closing FileSystem: {0}"),
    REPL_000009("Error occurred when getting diff report for target dir: {0}, {1} fromSnapshot: {2}"
            + " & toSnapshot: {3}"),
    REPL_000010("Exception creating job client: {0}"),
    REPL_000011("isSnapShotsAvailable: {0} is not fully qualified path"),
    REPL_000012("Unable to verify if dir {0} is snapshot-able"),
    REPL_000013("{0} does not exist."),
    REPL_000014("Unable to find latest snapshot on targetDir {0}"),
    REPL_000015("Unable to create snapshot {0}"),
    REPL_000016("Unable to evict snapshots from dir {0}"),
    REPL_000017("Exception occurred while closing connection: {0}"),
    REPL_000018("Exception occurred initializing Hive Server: {0}"),
    REPL_000019("Interrupt occurred..."),
    REPL_000020("Missing DR property for Hive Replication: {0}"),
    REPL_000021("Current job type is not MAIN or RECOVERY"),
    REPL_000022("Exception occurred while storing replication metrics info: {0}"),
    REPL_000023("Exception occurred while populating metrics periodically: {0}"),
    REPL_000024("PolicyType {0} obtained for entity: {1}"),
    REPL_000025("Error while storing external id. Message: {0}"),
    REPL_000026("Getting tracking info for instance id: [{0}]"),
    REPL_000027("Getting tracking info completed for instance id: [{0}], size: [{1}]"),
    REPL_000028("Error while obtaining Policy Bean: {0}"),
    REPL_000029("Identified Parent dataset: {0} and child dataset: {1}"),
    REPL_000030("Setting distcp options for source paths and target path"),
    REPL_000031("Preserve ACL: {0}"),
    REPL_000032("Exception occurred in FS Replication: {0}"),
    REPL_000033("Started DistCp with source Path: {0}  target path: {1}"),
    REPL_000034("DistCp Hadoop job: {0} for policy instance: [{1}]"),
    REPL_000035("Exception occurred while performing copying of data: {0}"),
    REPL_000036("No job started, return"),
    REPL_000037("Checking Snapshot directory on Source and Target"),
    REPL_000038("Exception occurred while initializing DistributedFileSystem: {0}"),
    REPL_000039("TDE Encryption enabled: {0}"),
    REPL_000040("replication job: {0} interrupted, killing it."),
    REPL_000041("policy instance: [{0}] not snapshottable, return"),
    REPL_000043("Nothing to recover as no DistCp job was launched, return"),
    REPL_000044("recover job [{0}] and job type [{1}]"),
    REPL_000045("replicatedSnapshotName is null. No recovery needed for policy instance: [{0}], return"),
    REPL_000046("No recovery needed for policy instance: [{0}], return"),
    REPL_000047("Recovery needed for policy instance: [{0}]. Start recovery!"),
    REPL_000048("Trying to get job [{0}], attempt [{1}]"),
    REPL_000049("Validating if dir: {0} is snapshotable."),
    REPL_000050("Creating snapshot {0} in directory {1}"),
    REPL_000051("Unable to create snapshot {0} in filesystem {1}. Exception is {2}"),
    REPL_000052("Started evicting snapshots on dir {0} , agelimit {1}, numSnapshot {2}"),
    REPL_000053("No Eviction Required as number of snapshots: {0} is less than numSnapshots: {1}"),
    REPL_000054("Deleting snapshots with path: {0} and snapshot path: {1}"),
    REPL_000055("Creating snapshot on fs: {0} for URI: {1}"),
    REPL_000056("Snapshots Eviction on FS:  {0}"),
    REPL_000057("getHS2ConnectionUrl connection uri: {0}"),
    REPL_000058("{0} not found: {1}"),
    REPL_000059("Beacon Hive export completed successfully"),
    REPL_000060("Exception occurred while performing Export: {0}"),
    REPL_000061("Performing Export for database: {0}"),
    REPL_000062("Last replicated event id for database: {0} is {1}"),
    REPL_000063("Source Current Repl Event id: {0} , Target Last Repl Event id: {1}"),
    REPL_000064("Exception occurred for export statement: {0}"),
    REPL_000065("Location of Repl Dump Directory: {0}"),
    REPL_000066("Beacon Hive Replication Successful"),
    REPL_000067("Exception occurred while performing Import: {0}"),
    REPL_000068("Performing Import for database: {0}"),
    REPL_000069("Exception occurred for import statement: {0}"),
    REPL_000070("Recovering replication in Bootstrap process (true|false): {0}"),
    REPL_000071("Drop {0} command: {1}"),
    REPL_000072("Drop database: {0}"),
    REPL_000073("Exception occurred while dropping database in recover bootstrap process: {0}"),
    REPL_000074("Repl {0}: {1}"),
    REPL_000075("Exception occurred for drop {0} list: {1}"),
    REPL_000076("Exception occurred while obtaining Repl event Id: {0} for database: {1}"),
    REPL_000077("Distcp Copy is successful"),

    //Scheduler module Messages
    SCHD_000001("No scheduled policy found."),
    SCHD_000002("Beacon scheduler configuration is not provided."),
    SCHD_000003("Key cannot have a null name!"),
    SCHD_000004("No suspended policy found"),
    SCHD_000005("{0} time can not be null or earlier than {1} time."),
    SCHD_000006("Scheduler must be initialized before starting it."),
    SCHD_000007("Exception while execution {0}"),
    SCHD_000008("Number of instances for recovery: [{0}]"),
    SCHD_000009("Recovering instanceId: [{0}], current offset: [{1}]"),
    SCHD_000010("Recovered instanceId: [{0}], request status: [{1}]"),
    SCHD_000011("key [{0}] exists [{1}] in the cache."),
    SCHD_000012("inserting new entry into cache for key: [{0}], value: [{1}]."),
    SCHD_000013("removing entry from cache for key: [{0}]."),
    SCHD_000014("registering interruption for key: [{0}]."),
    SCHD_000015("querying interrupt flag for key: [{0}]."),
    SCHD_000016("Beacon quartz scheduler database {0}: [{1}={2}]"),
    SCHD_000017("{0} is not initialized. Error: {1}"),
    SCHD_000018("Scheduling admin job: [{0}], group: [{1}], policy name: [{2}] with frequency: [{3} sec]."),
    SCHD_000019("Admin job: [{0}], group: [{1}], policy name: [{2}] is deleted successfully."),
    SCHD_000020("Admin job: [{0}], group: [{1}], policy name: [{2}] does not exits."),
    SCHD_000021("Interrupt received for job [{0}]."),
    SCHD_000022("AdminJob [{0}] is completed successfully. Removing the scheduled job."),
    SCHD_000023("AdminJob [{0}] error message: {1}"),
    SCHD_000024("StoreCleanupService execution started with cleanupDate: [{0}]."),
    SCHD_000025("StoreCleanupService execution completed successfully."),
    SCHD_000026("Sync status admin job is executing policy: [{0}], status: [{1}]."),
    SCHD_000027("Beacon scheduler initialized successfully."),
    SCHD_000028("Instance of the Beacon scheduler is already running."),
    SCHD_000029("Beacon scheduler shutdown successfully."),
    SCHD_000030("Beacon scheduler is not running."),
    SCHD_000031("Deleting the scheduled replication entity with id: {0}"),
    SCHD_000032("another policy instance [{0}] is in execution, current instance will be ignored."),
    SCHD_000033("Job [instance: {0}, offset: {1}, type: {2}] execution started."),
    SCHD_000034("Exception occurred while doing replication instance execution: {0}"),
    SCHD_000035("Job [key: {0}] [type: {1}] execution finished."),
    SCHD_000036("Setting the interruptFlag: [{0}] and JobContext interrupt flag: [{1}]"),
    SCHD_000037("Interrupted the replication executing thread: [{0}]"),
    SCHD_000038("Processing interrupt for job: [{0}]"),
    SCHD_000039("Exception occurred while processing interrupt. Message: {0}"),
    SCHD_000040("JobDetail [key: {0}] is created. isChained: {1}"),
    SCHD_000041("JobDetail [key: {0}] is created."),
    SCHD_000042("policy instance [{0}] to be executed. isRetry: [{1}]"),
    SCHD_000043("policy instance [{0}] will be ignored with status [{1}]"),
    SCHD_000044("Instance Detail: {0}"),
    SCHD_000045("execution status of the job offset: [{0}], jobFailed: [{1}], isRetry: [{2}]"),
    SCHD_000046("error while processing jobWasExecuted. Message: {0}"),
    SCHD_000047("This should never happen. Next chained job not found for offset: [{0}]"),
    SCHD_000048("Job [{0}] is now chained to job [{1}]"),
    SCHD_000049("Job [key: {0}] is chained with Job [key: {1}]"),
    SCHD_000050("Job [key: {0}] and trigger [key: {1}] are scheduled."),
    SCHD_000051("Deleting job [key: {0}, result: {1}] from the scheduler."),
    SCHD_000052("No {0} policy found for job key: [{1}]"),
    SCHD_000053("Interrupt Job id: {0}, group: {1} from the currently running jobs."),
    SCHD_000054("Trigger [key: {0}, StartTime: {1}, EndTime: {2}] is created."),
    SCHD_000055("Trigger [key: {0}, StartTime: {1}, EndTime: {2}, frequency: {3}] is created."),
    SCHD_000056("Trigger key: [{0}] for job: [{1}] with fire time: {2} is created."),
    SCHD_000057("Trigger [key: {0}] fired for Job [key: {1}]"),
    SCHD_000058("Setting the parallel flag for job: [{0}]"),
    SCHD_000059("veto trigger [{0}] for job: [{1}]"),
    SCHD_000060("Trigger misfired for [key: {0}]."),
    SCHD_000061("Trigger [key: {0}] completed for Job [key: {1}]"),
    SCHD_000062("All retry [{0}] are exhausted."),
    SCHD_000063("Job is rescheduled for retry attempt: [{0}] with delay: [{1}s]."),
    SCHD_000064("Failed to reschedule retry of the job."),
    SCHD_000065("JobStatus: [{0}] is not supported. Message: {1}"),
    SCHD_000066("JobStatus: [{0}] is not supported."),
    SCHD_000067("last instance: {0} offset: {1} status: {2} for policy: {3}"),
    SCHD_000068("Error while processing jobToBeExecuted. Message: {0}"),

    //Tools module Messages
    TOOL_000001("Schema {0} does not exists: {1}"),
    TOOL_000002("Database setup is starting..."),
    TOOL_000003("Setting up database with schema file: {0}"),
    TOOL_000004("Database setup is completed."),
    TOOL_000005("Database setup failed with error: {0}"),
    TOOL_000006("Creating tables for the database..."),
    TOOL_000007("Database setup is already done. Returning..."),
    TOOL_000008("Checking database is already setup..."),
    TOOL_000009("Derby schema: {0}"),
    TOOL_000010("derby schema creation failed: {0}"),
    TOOL_000011("Failed table creation query: {0}"),
    TOOL_000012("Error message: {0}");

    private final String msg;

    MessageCode(String msg) {
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
