/**
 *   Copyright  (c) 2016-2017, Hortonworks Inc.  All rights reserved.
 *
 *   Except as expressly permitted in a written agreement between you or your
 *   company and Hortonworks, Inc. or an authorized affiliate or partner
 *   thereof, any use, reproduction, modification, redistribution, sharing,
 *   lending or other exploitation of all or any part of the contents of this
 *   software is strictly prohibited.
 */

package com.hortonworks.beacon.entity.util;

import com.hortonworks.beacon.constants.BeaconConstants;
import com.hortonworks.beacon.entity.HiveDRProperties;
import com.hortonworks.beacon.exceptions.BeaconException;
import com.hortonworks.beacon.log.BeaconLog;
import com.hortonworks.beacon.rb.MessageCode;
import com.hortonworks.beacon.rb.ResourceBundleService;
import com.hortonworks.beacon.util.HiveActionType;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

/**
 * Utility Class for Hive Repl Status.
 */
public final class HiveDRUtils {
    private static final BeaconLog LOG = BeaconLog.getLog(HiveDRUtils.class);

    private static final String DRIVER_NAME = "org.apache.hive.jdbc.HiveDriver";
    private static final int TIMEOUT_IN_SECS = 300;
    public static final String JDBC_PREFIX = "jdbc:";
    public static final String BOOTSTRAP = "bootstrap";
    public static final String DEFAULT = "default";

    private HiveDRUtils() {}

    private static String getSourceHS2ConnectionUrl(Properties properties, HiveActionType actionType) {
        String connString;
        switch (actionType) {
            case EXPORT:
                connString = getHS2ConnectionUrl(properties.getProperty(HiveDRProperties.SOURCE_HS2_URI.getName()),
                        properties);
                break;
            case IMPORT:
                connString =  getHS2ConnectionUrl(properties.getProperty(HiveDRProperties.TARGET_HS2_URI.getName()),
                        properties);
                break;
            default:
                throw new IllegalArgumentException(
                    ResourceBundleService.getService()
                            .getString(MessageCode.COMM_010005.name(), actionType));
        }

        return connString;
    }

    public static String getHS2ConnectionUrl(final String hs2Uri, final Properties properties) {
        StringBuilder connString = new StringBuilder();
        String queueName = properties.getProperty(HiveDRProperties.QUEUE_NAME.getName());

        if (hs2Uri.contains("serviceDiscoveryMode=zooKeeper")) {
            connString.append(hs2Uri);
        } else {
            connString.append(JDBC_PREFIX).append(StringUtils.removeEnd(hs2Uri, "/"));
        }

        if (StringUtils.isNotBlank(queueName)) {
            connString.append("?").append(BeaconConstants.MAPRED_QUEUE_NAME).append(BeaconConstants.EQUAL_SEPARATOR).
                    append(queueName);
        }

        LOG.info(MessageCode.REPL_000057.name(), connString);
        return connString.toString();
    }

    public static Connection getDriverManagerConnection(Properties properties,
                                                        HiveActionType actionType) throws BeaconException {
        String connString = getSourceHS2ConnectionUrl(properties, actionType);
        return getConnection(connString);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public static void setConfigParameters(Statement statement, Properties properties) throws SQLException {
        if (properties.containsKey(BeaconConstants.HA_CONFIG_KEYS)) {
            String haConfigKeys = properties.getProperty(BeaconConstants.HA_CONFIG_KEYS);
            for(String haConfigKey: haConfigKeys.split(BeaconConstants.COMMA_SEPARATOR)) {
                statement.execute(BeaconConstants.SET
                        + haConfigKey + BeaconConstants.EQUAL_SEPARATOR
                        + properties.getProperty(haConfigKey));
            }
        }

        if (UserGroupInformation.isSecurityEnabled()) {
            statement.execute(BeaconConstants.SET + BeaconConstants.MAPREDUCE_JOB_HDFS_SERVERS
                    + BeaconConstants.EQUAL_SEPARATOR
                    + properties.getProperty(HiveDRProperties.SOURCE_NN.getName()) + ","
                    + properties.getProperty(HiveDRProperties.TARGET_NN.getName()));

            statement.execute(BeaconConstants.SET + BeaconConstants.MAPREDUCE_JOB_SEND_TOKEN_CONF
                    + BeaconConstants.EQUAL_SEPARATOR
                    + PolicyHelper.getRMTokenConf());
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("DMI_EMPTY_DB_PASSWORD")
    public static Connection getConnection(String connString) throws BeaconException {
        Connection connection;
        String user = "";
        try {
            UserGroupInformation currentUser = UserGroupInformation.getLoginUser();
            if (currentUser != null) {
                user = currentUser.getShortUserName();
            }
            connection = DriverManager.getConnection(connString, user, "");
        } catch (IOException | SQLException ex) {
            LOG.error(MessageCode.REPL_000018.name(), ex);
            throw new BeaconException(MessageCode.REPL_000018.name(), ex, ex.getMessage());
        }
        return connection;
    }

    public static void initializeDriveClass() throws BeaconException {
        try {
            Class.forName(DRIVER_NAME);
            DriverManager.setLoginTimeout(TIMEOUT_IN_SECS);
        } catch (ClassNotFoundException e) {
            LOG.error(MessageCode.REPL_000058.name(), DRIVER_NAME, e);
            throw new BeaconException(MessageCode.REPL_000058.name(), e, DRIVER_NAME, e.getMessage());
        }
    }

    public static void setDistcpOptions(Statement statement, Properties properties) throws SQLException {
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            if (prop.getKey().toString().startsWith(BeaconConstants.DISTCP_OPTIONS)) {
                statement.execute(BeaconConstants.SET + prop.getKey().toString()
                        + BeaconConstants.EQUAL_SEPARATOR
                        + prop.getValue().toString());
            }
        }
    }

    public static void cleanup(Statement statement, Connection connection) throws BeaconException {
        try {
            if (statement != null) {
                statement.close();
            }

            if (connection != null) {
                connection.close();
            }
        } catch (SQLException sqe) {
            throw new BeaconException(MessageCode.REPL_000017.name(), sqe, sqe.getMessage());
        }
    }
}
