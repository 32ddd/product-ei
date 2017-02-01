/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.integrator.core;

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.config.xml.endpoints.EndpointFactory;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.endpoints.Endpoint;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.CarbonConfigurationContextFactory;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.wso2.carbon.integrator.core.handler.EndpointHolder;
import org.wso2.carbon.integrator.core.internal.IntegratorComponent;
import org.wso2.carbon.tomcat.api.CarbonTomcatService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.webapp.mgt.WebApplication;
import org.wso2.carbon.webapp.mgt.WebApplicationsHolder;
import org.wso2.carbon.webapp.mgt.utils.WebAppUtils;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class Utils {

    private static OMFactory fac = OMAbstractFactory.getOMFactory();
    private static final QName ENDPOINT_Q = new QName(XMLConfigConstants.SYNAPSE_NAMESPACE, "endpoint");
    private static final QName ADDRESS_Q = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "address");
    private static OMElement endpoint = fac.createOMElement(ENDPOINT_Q);
    private static OMElement address = fac.createOMElement(ADDRESS_Q);

    public static int getProtocolPort(String protocol) {
        CarbonTomcatService webAppAdminService;
        webAppAdminService = (CarbonTomcatService) PrivilegedCarbonContext.getThreadLocalCarbonContext().getOSGiService(CarbonTomcatService.class, null);
        if (webAppAdminService == null) {
            throw new RuntimeException("CarbonTomcatService service is not available.");
        }
        return webAppAdminService.getPort(protocol);
    }

    /**
     * Get the details of a deployed webApp
     *
     * @param path URI path
     * @return meta data for webapp
     */
    public static WebApplication getStartedWebapp(String path) {
        Map<String, WebApplicationsHolder> webApplicationsHolderMap = WebAppUtils.getAllWebappHolders(CarbonConfigurationContextFactory.getConfigurationContext());
        WebApplication matchedWebApplication;
        for (WebApplicationsHolder webApplicationsHolder : webApplicationsHolderMap.values()) {
            for (WebApplication webApplication : webApplicationsHolder.getStartedWebapps().values()) {
                if (path.contains(webApplication.getContextName())) {
                    matchedWebApplication = webApplication;
                    return matchedWebApplication;
                }
            }
        }
        return null;
    }

    /**
     * Get the details of a deployed webapp
     *
     * @param path URI path
     * @return meta data for webapp
     */
    public static WebApplication getStartedTenantWebapp(String tenantDomain, String path) {
        try {
            ConfigurationContextService contextService = IntegratorComponent.getContextService();
            ConfigurationContext configContext;
            ConfigurationContext tenantContext;
            if (null != contextService) {
                // Getting server's configContext instance
                configContext = contextService.getServerConfigContext();
                tenantContext = TenantAxisUtils.getTenantConfigurationContext(tenantDomain, configContext);
                Map<String, WebApplicationsHolder> webApplicationsHolderMap = WebAppUtils.getAllWebappHolders(tenantContext);
                WebApplication matchedWebApplication;
                for (WebApplicationsHolder webApplicationsHolder : webApplicationsHolderMap.values()) {
                    for (WebApplication webApplication : webApplicationsHolder.getStartedWebapps().values()) {
                        if (path.contains(webApplication.getContextName())) {
                            matchedWebApplication = webApplication;
                            return matchedWebApplication;
                        }
                    }
                }
            }
        } catch (Exception e) {
            //ignore
        }
        return null;
    }

    public static String getHostname(String host) {
        return host.split(":")[0];
    }

    public static String getContext(String uri) {
        String[] temp = uri.split("/");
        if (temp.length >= 2) {
            return "/".concat(temp[1]).toLowerCase();
        } else {
            return null;
        }
    }

    public static String getUniqueRequestID(String uri) {
        String input = uri + System.getProperty(CarbonConstants.START_TIME);
        return UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }

    public static String getDSSJsonBuilder() {
        Parameter dssJsonBuilder = CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getParameter(Constants.DATASERVICE_JSON_BUILDER);
        if (dssJsonBuilder == null) {
            return "org.apache.axis2.json.gson.JsonBuilder";
        } else {
            return dssJsonBuilder.getValue().toString();
        }
    }

    public static Endpoint createEndpoint(String addressURI, SynapseEnvironment environment) {
        if (EndpointHolder.getInstance().getEndpoint(addressURI) != null) {
            return EndpointHolder.getInstance().getEndpoint(addressURI);
        } else {
            address.addAttribute("uri", addressURI, null);
            endpoint.addChild(address);
            Endpoint ep = EndpointFactory.getEndpointFromElement(endpoint, true, null);
            ep.init(environment);
            EndpointHolder.getInstance().putEndpoint(addressURI, ep);
            return ep;
        }
    }

    public static String getPassThroughJsonBuilder() {
        Parameter psJsonBuilder = CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getParameter(Constants.PASSTHRU_JSON_BUILDER);
        if (psJsonBuilder == null) {
            return "org.apache.synapse.commons.json.JsonStreamBuilder";
        } else {
            return psJsonBuilder.getValue().toString();
        }
    }

    public static String getDSSJsonFormatter() {
        Parameter dssJsonFormatter = CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getParameter(Constants.DATASERVICE_JSON_FORMATTER);
        if (dssJsonFormatter == null) {
            return "org.apache.axis2.json.gson.JsonFormatter";
        } else {
            return dssJsonFormatter.getValue().toString();
        }
    }

    public static String getPassThroughJsonFormatter() {
        Parameter psJsonFormatter = CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getParameter(Constants.PASSTHRU_JSON_FORMATTER);
        if (psJsonFormatter == null) {
            return "org.apache.synapse.commons.json.JsonStreamFormatter";
        } else {
            return psJsonFormatter.getValue().toString();
        }
    }

    public static String getPassThruHttpPort() {
        return CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getTransportIn("http").
                getParameter("port").getValue().toString();
    }

    public static String getPassThruHttpsPort() {
        return CarbonConfigurationContextFactory.getConfigurationContext().getAxisConfiguration().getTransportIn("https").
                getParameter("port").getValue().toString();
    }

    public static boolean validateHeader(String key, String uri) {
        String input = uri + System.getProperty(CarbonConstants.START_TIME);
        return (UUID.nameUUIDFromBytes(input.getBytes()).toString().equals(key));
    }

    public static void setIntegratorHeader(MessageContext synCtx) {
        String uri = synCtx.getTo().getAddress();
        Axis2MessageContext axis2smc = (Axis2MessageContext) synCtx;
        org.apache.axis2.context.MessageContext axis2MessageCtx = axis2smc.getAxis2MessageContext();
        Object headers = axis2MessageCtx.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        if (headers != null && headers instanceof Map) {
            Map headersMap = (Map) headers;
            headersMap.put(Constants.INTEGRATOR_HEADER, Utils.getUniqueRequestID(uri));
        }
        if (headers == null) {
            Map<String, String> headersMap = new HashMap<String, String>();
            headersMap.put(Constants.INTEGRATOR_HEADER, Utils.getUniqueRequestID(uri));
            axis2MessageCtx.setProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS, headersMap);
        }

    }

    public static boolean isDataService(org.apache.axis2.context.MessageContext messageContext) {
        String filePath = messageContext.getAxisService().getFileName().getPath();
        return filePath.contains("dataservices");
    }

    /**
     * In this method we rewrite the location header which comes from the tomcat transport.
     * @param location location url
     * @param messageContext message context.
     */
    public static void rewriteLocationHeader(String location, MessageContext messageContext) {
        if (location.contains(":")) {
            String[] tmp = location.split(":");
            if (tmp.length == 2) {
                return;
            }
            String protocol = tmp[0];
            String host = null;
            for (String tmpname : tmp[1].split("/")) {
                if (!tmpname.isEmpty()) {
                    host = tmpname;
                    break;
                }
            }
            String newPort;
            String port = null;
            if ("http".equals(protocol)) {
                newPort = getPassThruHttpPort();
            } else {
                newPort = getPassThruHttpsPort();
            }
            if (tmp.length > 2) {
                port = tmp[2].substring(0, tmp[2].indexOf("/"));
            }
            String oldEndpoint = protocol + "://" + host + ":" + port;
            if (EndpointHolder.getInstance().containsEndpoint(oldEndpoint)) {
                location = location.replace(port, newPort);
                Object headers = ((Axis2MessageContext) messageContext).getAxis2MessageContext().getProperty("TRANSPORT_HEADERS");
                if (headers instanceof TreeMap) {
                    ((TreeMap) headers).put("Location", location);
                }
            }
        }
    }

}
