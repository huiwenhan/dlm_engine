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

package com.hortonworks.beacon.main;

import com.hortonworks.beacon.config.BeaconConfig;
import com.hortonworks.beacon.config.Engine;
import com.hortonworks.beacon.entity.store.ConfigurationStore;
import com.hortonworks.beacon.scheduler.quartz.BeaconQuartzScheduler;
import com.hortonworks.beacon.service.ServiceInitializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Driver for running Beacon as a standalone server.
 */
public final class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    protected static Server server;

    private static final String APP_PATH = "app";
    private static final String APP_PORT = "port";
    private static final String LOCAL_CLUSTER = "localcluster";

    private Main() {
    }

    private static CommandLine parseArgs(String[] args) throws ParseException {
        Options options = new Options();
        Option opt;

        opt = new Option(APP_PATH, true, "Application Path");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option(APP_PORT, true, "Application Port");
        opt.setRequired(false);
        options.addOption(opt);

        opt = new Option(LOCAL_CLUSTER, true, "Cluster to run");
        opt.setRequired(false);
        options.addOption(opt);


        return new GnuParser().parse(options, args);
    }

    static class ShutDown extends Thread {
        public void run() {
            try {
                LOG.info("calling shutdown hook");
                if (server != null) {
                    server.stop();
                }
                BeaconQuartzScheduler.get().stopScheduler();
                LOG.info("Shutdown Complete.");
            } catch (Exception e) {
                LOG.error("Server shutdown failed with ", e);
            }
        }
    }


    public static void main(String[] args) throws Exception {

        Runtime.getRuntime().addShutdownHook(new ShutDown());
        CommandLine cmd = parseArgs(args);
        BeaconConfig conf = BeaconConfig.getInstance();
        Engine engine = conf.getEngine();
        if (cmd.hasOption(APP_PATH)) {
            engine.setAppPath(cmd.getOptionValue(APP_PATH));
        }
        if (cmd.hasOption(LOCAL_CLUSTER)) {
            engine.setLocalClusterName(cmd.getOptionValue(LOCAL_CLUSTER));
        }
        LOG.info("App path: {}", engine.getAppPath());
        LOG.info("Beacon cluster: {}", engine.getLocalClusterName());

        final boolean tlsEnabled = engine.getTlsEnabled();
        final int port = tlsEnabled ? engine.getTlsPort() : engine.getPort();
        Connector connector = new SocketConnector();
        connector.setPort(port);
        connector.setHost(engine.getHostName());
        connector.setHeaderBufferSize(engine.getSocketBufferSize());
        connector.setRequestBufferSize(engine.getSocketBufferSize());

        server = new Server();

        server.addConnector(connector);
        WebAppContext application = new WebAppContext(engine.getAppPath(), "/");
        application.setParentLoaderPriority(true);
        server.setHandler(application);
        LOG.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        LOG.info("Server starting with TLS ? {} on port {}", tlsEnabled, port);
        LOG.info("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        server.start();

        ConfigurationStore.getInstance().init();
        new ServiceInitializer().initialize();
        BeaconQuartzScheduler.get().startScheduler();
    }


}
