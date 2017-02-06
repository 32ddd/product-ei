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
import org.apache.axis2.description.Parameter;
import org.apache.axis2.dispatchers.RequestURIBasedServiceDispatcher;
import org.apache.axis2.engine.AbstractDispatcher;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;

/**
 * This Axis2 handler is written to dispatch messages to synapse environment, when the message is received for a stateful service.
 *
 */
public class IntegratorStatefulHandler extends AbstractDispatcher {
    private static final String NAME = "IntegratorStatefulHandler";
    private static final Log log = LogFactory.getLog(IntegratorSynapseHandler.class);
    private static Object synapseHandler;
    private static Method synapseHandlerFindOperationMethod;
    private static Method synapseHandlerFindServiceMethod;
    private RequestURIBasedServiceDispatcher rubsd = new RequestURIBasedServiceDispatcher();

    public IntegratorStatefulHandler() {
    }

    static {
        try {
            Class<?> synapseHandlerClass = IntegratorStatefulHandler.class.getClassLoader().loadClass("org.apache.synapse.core.axis2.SynapseDispatcher");
            synapseHandler = synapseHandlerClass.newInstance();
            synapseHandlerFindServiceMethod = synapseHandlerClass.getMethod("findService", MessageContext.class);
            synapseHandlerFindOperationMethod = synapseHandlerClass.getMethod("findOperation", AxisService.class, MessageContext.class);
        } catch (Exception e) {
            log.error("Error occurred while initializing IntegratorStatefulHandler");
        }
    }

    @Override
    public AxisOperation findOperation(AxisService axisService, MessageContext messageContext) throws AxisFault {
        if (isStatefulService(axisService) && messageContext.getProperty("transport.http.servletRequest") == null && messageContext.getProperty("pass-through.pipe") != null) {
            try {
                messageContext.setAxisService((AxisService) synapseHandlerFindServiceMethod.invoke(synapseHandler, messageContext));
                if(log.isDebugEnabled()) {
                    log.debug("AxisService is changing from " + axisService.getName() + " to " + messageContext.getAxisService().getName());
                }
                messageContext.setProperty("raplacedAxisService","true");
                return (AxisOperation) synapseHandlerFindOperationMethod.invoke(synapseHandler, messageContext.getAxisService(), messageContext);
            } catch (Exception e) {
                log.error("Error occurred while invoking stateful service.");
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public AxisService findService(MessageContext messageContext) throws AxisFault {
        return this.rubsd.findService(messageContext);
    }

    @Override
    public void initDispatcher() {
        this.init(new HandlerDescription(NAME));
    }

    /**
     * In this method check we check whether that particular service is a admin service or session enabled service.
     *
     * @param axisService AxisService
     * @return isStatefulService boolean
     */
    private boolean isStatefulService(AxisService axisService) {
        Parameter parameter = axisService.getParameter("adminService");
        return parameter != null && ("transportsession".equals(axisService.getScope()) || "true".equals(axisService.getParameter("adminService").getValue()));
    }

}
