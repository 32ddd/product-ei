/*
 *     Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *     WSO2 Inc. licenses this file to you under the Apache License,
 *     Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */
package org.wso2.ei.bps.analytics.core.bpmn.internal;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.ei.bps.analytics.core.bpmn.services.BPMNAnalyticsCoreServer;

/**
 * Data holder for Generic Analytics
 */
public class BPMNAnalyticsCoreServerHolder {

    private static BPMNAnalyticsCoreServerHolder instance = new BPMNAnalyticsCoreServerHolder();
    private RegistryService registryService;
    private RealmService realmService;
    private BPMNAnalyticsCoreServer bpmnAnalyticsCoreServer;

    private BPMNAnalyticsCoreServerHolder() {
    }

    public static BPMNAnalyticsCoreServerHolder getInstance() {
        return instance;
    }

    public RegistryService getRegistryService() {
        return registryService;
    }

    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public BPMNAnalyticsCoreServer getBPMNAnalyticsCoreServer() {
        return bpmnAnalyticsCoreServer;
    }

    public void setBPMNAnalyticsCoreServer(BPMNAnalyticsCoreServer bpsAnalyticsServer) {
        this.bpmnAnalyticsCoreServer = bpsAnalyticsServer;
    }
}
