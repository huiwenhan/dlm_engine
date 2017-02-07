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

package com.hortonworks.beacon.api.plugin.repltarget;

import com.hortonworks.beacon.api.plugin.ReplEventInfo;
import com.hortonworks.beacon.api.plugin.ReplMessage;
import com.hortonworks.beacon.api.plugin.StatusReporter;
import com.hortonworks.beacon.api.plugin.src.ReplSource;
import com.hortonworks.beacon.client.entity.ReplicationPolicy;
import com.hortonworks.beacon.exceptions.BeaconException;

/**
 * This API describes the plugin interface for plugins on the target side.  These plugins are
 * expected to receive data from Beacon that has been packaged by the
 * {@link ReplSource}.
 *
 * A given ReplTarget handles replicating events of a particular type.  For example, there should be
 * one for Hive, one for HDFS, etc.  In addition to being called for each
 * replication message, the handler will also be called for bootstrapping, failover, and failback.
 *
 * A ReplTarget instance will always be invoked in a separate thread from the main dispatcher to
 * avoid delaying the dispatcher.  ReplTargets themselves may wish to multi-task and thus run work in
 * multiple threads.  To avoid swamping the system it is necessary to bound the total number of
 * threads in the system.  This is achieved by having the ReplTargets always use the passed in
 * ExecutorService rather than spawn new threads themselves.
 */
public interface ReplTarget {
    /**
     * This method will be called when a plugin is found by the system and initiated.  It needs to
     * return the message type that this plugin will process.
     * @param info information about the Beacon service, such as the thread pool to use
     * @return Information about this plugin, including what types of messages it handles and the
     * class(es) to use to deserialize them.
     */
    PluginInfo register(BeaconInfo info);

    /**
     * Handle a replication message.
     * @param msg message containing data regarding the event.  The class of this will be the
     *            class indicated in the PluginInfo returned by {@link #register(BeaconInfo)}.
     * @throws BeaconException if something goes wrong
     */
    void handleMessage(ReplMessage msg) throws BeaconException;

    /**
     * Initiate a bootstrap operation.  The plugin is expected to perform all of the operations
     * to bootstrap a new policy for its particular type.
     * @param policy to setup with initial snapshot
     * @param eventInfo information relevant to all messages generated by this event
     * @param status StatusReporter to use to pass back status info to requester
     * @throws BeaconException if something goes wrong
     */
    void boostrap(ReplicationPolicy policy, ReplEventInfo eventInfo, StatusReporter status)
            throws BeaconException;

    /**
     * Repair replication that was broken.  This will only be called if the system has determined
     * that events were lost at the source.
     * @param policy that needs repaired
     * @param eventInfo information relevant to all messages generated by this event
     * @param status StatusReporter to use to pass back status info to requester
     * @throws BeaconException if something goes wrong
     */
    void repair(ReplicationPolicy policy, ReplEventInfo eventInfo, StatusReporter status)
            throws BeaconException;

    /**
     * Failover a policy, switching the target to primary and ceasing replication.
     * @param policy to failover
     * @param eventInfo information relevant to all messages generated by this event
     * @param status StatusReporter to use to pass back status info to requester
     * @throws BeaconException if something goes wrong
     */
    void failover(ReplicationPolicy policy, ReplEventInfo eventInfo, StatusReporter status)
            throws BeaconException;

    /**
     * Begin failback of a policy.  This will initiate contact with the source and set it up as a
     * temporary target (mark its objects read only).  It will then copy changes since failover
     * from the target to the source. It will not yet stop replication running from target to
     * source.
     * @param policy to failback
     * @param eventInfo information relevant to all messages generated by this event
     * @param status StatusReporter to use to pass back status info to requester
     * @throws BeaconException if something goes wrong
     */
    void initiateFailback(ReplicationPolicy policy, ReplEventInfo eventInfo, StatusReporter status)
            throws BeaconException;

    /**
     * Complete failback of a policy.  This expects that the target system has been quiesced and
     * the queue is empty (thus the target and source should now be in sync).  It then switches
     * the target back to read only, the source to read/write, and begins replication moving back
     * from source to target.
     * @param policy
     * @param eventInfo information relevant to all messages generated by this event
     * @param status
     * @throws BeaconException
     */
    void completeFailback(ReplicationPolicy policy, ReplEventInfo eventInfo, StatusReporter status)
            throws BeaconException;
}
