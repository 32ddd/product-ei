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
import java.util.TreeMap;

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
    public InvocationResponse invoke(MessageContext msgctx) throws AxisFault {
        AxisService axisService = msgctx.getAxisService();
        if (axisService != null) {
            boolean isDataService = Utils.isDataService(msgctx);
            boolean isStatefulService = Utils.isStatefulService(axisService);
            if ((isDataService || isStatefulService) && msgctx.getProperty("transport.http.servletRequest") == null) {
                AxisOperation operation = msgctx.getAxisOperation();
                if (operation != null) {
                    String opernationName = operation.getName().toString();
                    if (opernationName.startsWith("_post") || opernationName.startsWith("_get") || opernationName.startsWith("_put") || opernationName.startsWith("_delete")) {
                        msgctx.setProperty("isDSSRest", true);
                    }
                }
                setSynapseContext(isDataService, msgctx, axisService);
            }
        }
        return super.invoke(msgctx);
    }

    @Override
    public AxisOperation findOperation(AxisService axisService, MessageContext messageContext) throws AxisFault {
        String uri = (String) messageContext.getProperty("TransportInURL");
        boolean isDataService = Utils.isDataService(messageContext);
        boolean isStatefulService = Utils.isStatefulService(axisService);
        if ((isStatefulService || isDataService || (uri != null && uri.contains("generateClient"))
        ) && messageContext.getProperty("transport.http.servletRequest") == null) {
            try {
                setSynapseContext(isDataService, messageContext, axisService);
                return messageContext.getAxisOperation();
            } catch (AxisFault e) {
                log.error("Error occurred while invoking stateful service.");
                return null;
            }
        }
        return null;
    }

    @Override
    public AxisService findService(MessageContext messageContext) throws AxisFault {
        AxisService service = this.rubsd.findService(messageContext);
        boolean isDataService;
        if (service != null && messageContext.getProperty("transport.http.servletRequest") == null) {
            URL file = service.getFileName();
            if (file != null) {
                String filePath = file.getPath();
                isDataService = filePath.contains("dataservices");
                String uri = (String) messageContext.getProperty("TransportInURL");
                boolean isStatefulService = Utils.isStatefulService(service);
                if (isDataService && !isApplicationJsonDSSRequest(messageContext) && !isStatefulService) {
                    return service;
                }
                if (isStatefulService || isDataService || (uri != null && uri.contains("generateClient"))) {
                    setSynapseContext(isDataService, messageContext, service);
                    return messageContext.getAxisService();
                }
            }
            return service;
        }
        return null;
    }

    @Override
    public void initDispatcher() {
        this.init(new HandlerDescription(NAME));
    }

    private void setSynapseContext(boolean isDataService, MessageContext messageContext, AxisService
            originalAxisService) throws AxisFault {
        AxisService axisService = synapseDispatcher.findService(messageContext);
        if (log.isDebugEnabled()) {
            log.debug("AxisService is changing from " + originalAxisService.getName() + " to " +
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
    }

    private boolean isApplicationJsonDSSRequest(MessageContext messageContext) {
        Object header = messageContext.getProperty("TRANSPORT_HEADERS");
        if (header instanceof TreeMap) {
            String acceptType = (String) ((TreeMap) header).get("Accept");
            if (acceptType == null) {
                acceptType = (String) ((TreeMap) header).get("accept");
            }
            if ((acceptType != null && acceptType.toLowerCase().equals("application/json"))) {
                return true;
            }
        }
        return false;
    }
}
