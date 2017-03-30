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


import com.hortonworks.beacon.plugin.service.PluginManagerService;
import com.hortonworks.beacon.service.ServiceInitializer;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Test class for ServiceInitializer.
 */
public class ServiceInitializerTest {
    private static final List<String> SERVICES = Arrays.asList(PluginManagerService.class.getName());

    @Test
    public void testServiceInit() throws Exception {
        ServiceInitializer serviceInitializer = new ServiceInitializer();
        serviceInitializer.initialize();
        for (String service : SERVICES) {
            boolean isRegistered = serviceInitializer.isRegistered(service);
            Assert.assertTrue(isRegistered);
        }
    }

}