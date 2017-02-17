/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.integrator.core.handler;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.dispatchers.RequestURIBasedServiceDispatcher;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.core.axis2.SynapseDispatcher;
import org.wso2.carbon.integrator.core.Utils;

import java.net.URL;

/**
 * This Axis2 handler is written to dispatch messages to synapse environment, when the message is received for a
 * stateful service.
 */
public class IntegratorStatefulHandler extends AbstractDispatcher {
    private static final String NAME = "IntegratorStatefulHandler";
    private static final Log log = LogFactory.getLog(IntegratorSynapseHandler.class);
    private SynapseDispatcher synapseDispatcher = new SynapseDispatcher();
    private RequestURIBasedServiceDispatcher rubsd = new RequestURIBasedServiceDispatcher();

    public IntegratorStatefulHandler() {
    }

    @Override
    public AxisOperation findOperation(AxisService axisService, MessageContext messageContext) throws AxisFault {
        String uri = (String) messageContext.getProperty("TransportInURL");
        boolean isDataService = Utils.isDataService(messageContext);
        if ((Utils.isStatefulService(axisService) || isDataService || (uri != null && uri.contains("generateClient"))
        ) && messageContext.getProperty("transport.http.servletRequest") == null) {
            try {
                messageContext.setAxisService(synapseDispatcher.findService(messageContext));
                if (log.isDebugEnabled()) {
                    log.debug("AxisService is changing from " + axisService.getName() + " to " + messageContext
                            .getAxisService().getName());
                }
                if (isDataService) {
                    messageContext.setProperty("isDataService", "true");
                } else {
                    messageContext.setProperty("raplacedAxisService", "true");
                }
                return synapseDispatcher.findOperation(messageContext.getAxisService(), messageContext);
            } catch (AxisFault e) {
                log.error("Error occurred while invoking stateful service.");
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public AxisService findService(MessageContext messageContext) throws AxisFault {
        AxisService service = this.rubsd.findService(messageContext);
        boolean isDataService = false;
        if (service != null) {
            return service;
        } else {
            AxisService tenantAxisService = null;
            try {
                tenantAxisService = Utils.getMultitenantAxisService(messageContext);
            } catch (RuntimeException | AxisFault e) {
                if (log.isDebugEnabled()) {
                    log.debug("Exception occured while loading the tenant." + e.getMessage());
                }
            }
            if (tenantAxisService != null) {
                URL file = tenantAxisService.getFileName();
                if (file != null) {
                    String filePath = file.getPath();
                    isDataService = filePath.contains("dataservices");
                }
                String uri = (String) messageContext.getProperty("TransportInURL");
                if ((Utils.isStatefulService(tenantAxisService) || isDataService || (uri != null && uri.contains
                        ("generateClient"))
                ) && messageContext.getProperty("transport.http.servletRequest") == null) {
                    AxisService axisService = synapseDispatcher.findService(messageContext);
                    if (log.isDebugEnabled()) {
                        log.debug("AxisService is changing from " + tenantAxisService.getName() + " to " +
                                axisService.getName());
                    }
                    if (isDataService) {
                        messageContext.setProperty("isDataService", "true");
                    } else {
                        messageContext.setProperty("raplacedAxisService", "true");
                    }
                    messageContext.setAxisService(axisService);
                    messageContext.setAxisOperation(synapseDispatcher.findOperation(messageContext.getAxisService(),
                            messageContext));
                    return axisService;
                }
            }
        }
        return null;
    }

    @Override
    public void initDispatcher() {
        this.init(new HandlerDescription(NAME));
    }

}
