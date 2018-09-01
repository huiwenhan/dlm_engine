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
package com.hortonworks.beacon.plugin.atlas;

import com.hortonworks.beacon.client.entity.Cluster;
import com.hortonworks.beacon.exceptions.BeaconException;
import com.hortonworks.beacon.plugin.DataSet;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.hortonworks.beacon.plugin.DataSet.DataSetType.HDFS;
import static com.hortonworks.beacon.plugin.DataSet.DataSetType.HIVE;

final class ExportRequestProvider {
    protected static final Logger LOG = LoggerFactory.getLogger(ExportRequestProvider.class);

    public static final String ATTRIBUTE_QUALIFIED_NAME = "qualifiedName";
    static final String ATTRIBUTE_PATH_NAME = "path";
    static final String ATLAS_TYPE_HIVE_DB = "hive_db";
    static final String ATLAS_TYPE_HDFS_PATH = "hdfs_path";
    private static final String PATH_FILE_SEPARATOR = "/";

    static final String QUALIFIED_NAME_FORMAT = "%s@%s";

    private ExportRequestProvider() {
    }

    public static AtlasExportRequest create(AtlasProcess process, DataSet dataSet) throws BeaconException {
        DataSet.DataSetType dataSetType = dataSet.getType();

        Cluster sourceCluster = dataSet.getSourceCluster();
        String sourceClusterName = process.getAtlasServerName(sourceCluster);
        String sourceDataSet = dataSet.getSourceDataSet();

        Cluster targetCluster = dataSet.getTargetCluster();
        String targetClusterName = process.getAtlasServerName(targetCluster);

        List<AtlasObjectId> itemsToExport = getItemsToExport(dataSetType, sourceClusterName, sourceDataSet);

        String entityGuid = getEntityGuid(process, sourceCluster, itemsToExport.get(0));

        long fromTimestamp = getFromTimestamp(process, targetCluster, sourceClusterName, entityGuid);
        Map<String, Object> options = getOptions(dataSetType, targetClusterName, fromTimestamp);

        return createRequest(itemsToExport, options);
    }

    private static String getEntityGuid(AtlasProcess process, Cluster cluster, AtlasObjectId objectId) {
        Map.Entry<String, Object> item = getFirstFromUniqueAttributeMap(objectId);
        if (item == null) {
            return null;
        }

        return process.getEntityGuid(cluster, objectId.getTypeName(), item.getKey(), (String) item.getValue());
    }

    private static Map.Entry<String, Object> getFirstFromUniqueAttributeMap(AtlasObjectId objectId) {
        for (Map.Entry<String, Object> entry : objectId.getUniqueAttributes().entrySet()) {
            return entry;
        }

        return null;
    }

    private static AtlasExportRequest createRequest(final List<AtlasObjectId> itemsToExport,
                                                    final Map<String, Object> options) {
        AtlasExportRequest request = new AtlasExportRequest() {{
                setItemsToExport(itemsToExport);
                setOptions(options);
            }};

        AtlasProcess.debugLog("createRequest: {}", request);
        return request;
    }

    private static long getFromTimestamp(AtlasProcess process,
                                         Cluster targetCluster,
                                         String sourceClusterName,
                                         String entityGuid) throws BeaconException {

        RESTClient client = process.getClient(targetCluster);

        AtlasServer cluster = client.getServer(sourceClusterName);
        long ret = (cluster != null && cluster.getAdditionalInfoRepl(entityGuid) != null)
                ? (long) cluster.getAdditionalInfoRepl(entityGuid)
                : 0L;

        AtlasProcess.debugLog("fromTimestamp: {}", ret);
        return ret;
    }

    private static Map<String, Object> getOptions(DataSet.DataSetType datasetType,
                                                  String targetClusterName,
                                                  long fromTimestamp) {
        Map<String, Object> options = new HashMap<>();

        options.put(AtlasExportRequest.OPTION_KEY_REPLICATED_TO, targetClusterName);
        options.put(AtlasExportRequest.OPTION_FETCH_TYPE, AtlasExportRequest.FETCH_TYPE_INCREMENTAL);
        options.put(AtlasExportRequest.FETCH_TYPE_INCREMENTAL_FROM_TIME, fromTimestamp);
        options.put(AtlasExportRequest.OPTION_SKIP_LINEAGE, true);

        if (datasetType == HDFS) {
            options.put(AtlasExportRequest.OPTION_ATTR_MATCH_TYPE, AtlasExportRequest.MATCH_TYPE_STARTS_WITH);
        }

        return options;
    }

    private static List<AtlasObjectId> getItemsToExport(DataSet.DataSetType dataSetType,
                                                        final String sourceClusterName,
                                                        final String sourceDataSet) throws BeaconException {
        final String typeName = getAtlasType(dataSetType);
        final AtlasObjectId objectId;

        switch (dataSetType) {
            case HDFS:
                objectId = new AtlasObjectId(typeName, ATTRIBUTE_PATH_NAME,
                                            getPathWithTrailingPathSeparator(sourceDataSet));
                break;

            case HIVE:
                final String qualifiedName = getQualifiedName(sourceDataSet, sourceClusterName);
                objectId = new AtlasObjectId(typeName, ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
                break;

            default:
                AtlasProcess.LOG.error("DataSet.DataSetType: {}: Not supported!", dataSetType);
                throw new BeaconException(String.format("DataSet.DataSetType: %s not supported.", dataSetType));
        }

        return new ArrayList<AtlasObjectId>() {{
                add(objectId);
            }};
    }

    static String getPathWithTrailingPathSeparator(String path) {
        if (!StringUtils.endsWith(path, PATH_FILE_SEPARATOR)) {
            return path.concat(PATH_FILE_SEPARATOR);
        }

        return path;
    }

    private static String getQualifiedName(String dataSetName, String clusterName) {

        String qualifiedName = String.format(QUALIFIED_NAME_FORMAT, dataSetName, clusterName);
        AtlasProcess.debugLog("getQualifiedName: {}", qualifiedName);
        return qualifiedName;
    }

    private static String getAtlasType(DataSet.DataSetType dataSetType) {
        return dataSetType == HIVE ? ATLAS_TYPE_HIVE_DB : ATLAS_TYPE_HDFS_PATH;
    }
}