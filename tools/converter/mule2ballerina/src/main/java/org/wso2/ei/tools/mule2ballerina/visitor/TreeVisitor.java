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

package org.wso2.ei.tools.mule2ballerina.visitor;

import org.ballerinalang.model.BallerinaFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.ei.tools.converter.common.builder.BallerinaASTModelBuilder;
import org.wso2.ei.tools.mule2ballerina.model.Flow;
import org.wso2.ei.tools.mule2ballerina.model.GlobalConfiguration;
import org.wso2.ei.tools.mule2ballerina.model.HttpListener;
import org.wso2.ei.tools.mule2ballerina.model.HttpListenerConfig;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequest;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequestConfig;
import org.wso2.ei.tools.mule2ballerina.model.Payload;
import org.wso2.ei.tools.mule2ballerina.model.Processor;
import org.wso2.ei.tools.mule2ballerina.model.Root;
import org.wso2.ei.tools.mule2ballerina.util.Constant;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * {@code TreeVisitor} visits intermediate object tree and populate Ballerina AST
 */
public class TreeVisitor implements Visitor {

    private static Logger logger = LoggerFactory.getLogger(TreeVisitor.class);

    private BallerinaASTModelBuilder ballerinaASTAPI;
    private Root mRoot;
    private BallerinaFile ballerinaFile;
    private Map<String, Boolean> serviceTrack = new HashMap<String, Boolean>();
    private Map<String, Boolean> importTracker = new HashMap<String, Boolean>();
    private String inboundName;
    private int serviceCounter = 0;
    private int resourceCounter = 0;
    private int parameterCounter = 0;
    private int variableCounter = 0;
    private String connectorVarName;
    private String refVarName;
    private String funcParaName;
    private int resourceAnnotationCount = 0;

    public TreeVisitor(Root mRoot) {
        ballerinaASTAPI = new BallerinaASTModelBuilder();
        this.mRoot = mRoot;
    }

    @Override
    public void visit(Root root) {
        logger.debug("-SRoot");
        for (Flow flow : root.getFlowList()) {
            flow.accept(this);
        }
        logger.debug("-ERoot");
        ballerinaFile = ballerinaASTAPI.buildBallerinaFile();
    }

    /**
     * Navigate flow processors
     *
     * @param flow
     */
    @Override
    public void visit(Flow flow) {
        logger.debug("--SFlow");
        int i = 0;
        int flowSize = flow.getFlowProcessors().size();
        for (Processor processor : flow.getFlowProcessors()) {
            processor.accept(this);
            i++;
            //If end of flow
            if (flowSize == i) {
                ballerinaASTAPI.createNameReference(null, refVarName);
                ballerinaASTAPI.createVariableRefExpr();
                ballerinaASTAPI.createReplyStatement();
                ballerinaASTAPI.endCallableBody();
                String resourceName = Constant.BLANG_RESOURCE_NAME + ++resourceCounter;
                ballerinaASTAPI.endOfResource(resourceName, resourceAnnotationCount);
                resourceAnnotationCount = 0;
                logger.debug("--EFlow");

                /* At the end of each flow get the flow queue associate with its config and
                 * remove it from the queue. So that when there are no flows (resources) associate with a config
                 * (service) we can close the service
                 */
                if (mRoot.getServiceMap() != null) {
                    Queue<Flow> flows = mRoot.getServiceMap().get(inboundName);
                    if (flows != null) {
                        flows.remove();
                        if (flows.size() == 0) {
                            String serviceName = Constant.BLANG_SERVICE_NAME + ++serviceCounter;
                            ballerinaASTAPI.endOfService(serviceName);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(Payload payload) {
        if (importTracker.isEmpty() || importTracker.get(Constant.BLANG_PKG_MESSAGES) == null) {
            ballerinaASTAPI
                    .addImportPackage(ballerinaASTAPI.getBallerinaPackageMap().get(Constant.BLANG_PKG_MESSAGES), null);
            importTracker.put(Constant.BLANG_PKG_MESSAGES, true);
        }
        logger.debug("----Payload");
        ballerinaASTAPI.createNameReference(Constant.BLANG_PKG_MESSAGES, Constant.BLANG_PKG_MESSAGES_FUNC);
        ballerinaASTAPI.startExprList();
        ballerinaASTAPI.createNameReference(null, refVarName);
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.createStringLiteral(payload.getValue());
        ballerinaASTAPI.endExprList(2);
        ballerinaASTAPI.createFunctionInvocation(true);
    }

    @Override
    public void visit(HttpListenerConfig listenerConfig) {

        /*If ballerina http package is not already added to import packages , add it*/
        if (importTracker.isEmpty() || importTracker.get(Constant.BLANG_HTTP) == null) {
            ballerinaASTAPI.addImportPackage(ballerinaASTAPI.getBallerinaPackageMap().get(Constant.BLANG_HTTP), null);
            importTracker.put(Constant.BLANG_HTTP, true);
        }
        logger.debug("--HttpListenerConfig");

        /*If the service is not yet created, start creating service definition*/
        if (serviceTrack.get(listenerConfig.getName()) == null) {
            ballerinaASTAPI.startService();

            /*Create annotations belong to the service definition*/
            ballerinaASTAPI
                    .createAnnotationAttachment(Constant.BLANG_HTTP, Constant.BLANG_BASEPATH, Constant.BLANG_VALUE,
                            listenerConfig.getBasePath());
            ballerinaASTAPI.addAnnotationAttachment(1); //attributesCount is never used
            serviceTrack.put(listenerConfig.getName(), true);
            inboundName = listenerConfig.getName();
        }
    }

    @Override
    public void visit(HttpListener listener) {
        logger.debug("----HttpListener");
        GlobalConfiguration globalConfiguration = mRoot.getConfigMap().get(listener.getConfigName());
        globalConfiguration.accept(this);

        /*Inbound connectors need to start the resource definition. Resource is not created at the start of a flow
        , because for the creation of a resource, a service definition has to be started, which only happens once the
        first processor's config is visited */
        ballerinaASTAPI.startResource();
        String allowedMethods = Constant.BLANG_METHOD_GET;
        if (listener.getAllowedMethods() != null) { //TODO: allowedmethods can come comma separated. Handle that here.
            allowedMethods = listener.getAllowedMethods();
            String[] bits = allowedMethods.split(",");
            for (String method : bits) {
                /*Create an annotation without attribute values*/
                ballerinaASTAPI.createAnnotationAttachment(Constant.BLANG_HTTP, method, null, null);
                ballerinaASTAPI.addAnnotationAttachment(0);
                resourceAnnotationCount++;
            }
        } else {
            ballerinaASTAPI.createAnnotationAttachment(Constant.BLANG_HTTP, allowedMethods, null, null);
            ballerinaASTAPI.addAnnotationAttachment(0);
            resourceAnnotationCount++;
        }

        if (listener.getPath() != null) {
            ballerinaASTAPI.createAnnotationAttachment(Constant.BLANG_HTTP, Constant.BLANG_PATH, Constant.BLANG_VALUE,
                    listener.getPath());
            ballerinaASTAPI.addAnnotationAttachment(1);
            resourceAnnotationCount++;
        }

        ballerinaASTAPI.addTypes(Constant.BLANG_TYPE_MESSAGE); //type of the parameter
        funcParaName = Constant.BLANG_DEFAULT_MSG_PARAM_NAME + ++parameterCounter;
        ballerinaASTAPI.addParameter(0, false, funcParaName);
        ballerinaASTAPI.startCallableBody();
        createVariableWithEmptyMap(Constant.BLANG_TYPE_MESSAGE, Constant.BLANG_RES_VARIABLE_NAME + ++variableCounter,
                true);
    }

    @Override
    public void visit(HttpRequest request) {
        logger.debug("----HttpRequest");
        GlobalConfiguration globalConfiguration = mRoot.getConfigMap().get(request.getConfigName());
        globalConfiguration.accept(this);

        ballerinaASTAPI.createVariableRefList();
        ballerinaASTAPI.createNameReference(null, refVarName);
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.endVariableRefList(1);
        ballerinaASTAPI.createNameReference(Constant.BLANG_HTTP, Constant.BLANG_CLIENT_CONNECTOR);
        ballerinaASTAPI.startExprList();
        ballerinaASTAPI.createNameReference(null, connectorVarName);
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.createStringLiteral(request.getPath());
        ballerinaASTAPI.createNameReference(null, funcParaName);
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.endVariableRefList(3);
        ballerinaASTAPI.createAction(Constant.BLANG_CLIENT_CONNECTOR_GET_ACTION, true);
        ballerinaASTAPI.createAssignmentStatement();
    }

    @Override
    public void visit(HttpRequestConfig requestConfig) {
        logger.debug("----HttpRequestConfig");
        /*Create reference type variable LHS*/
        ballerinaASTAPI.createNameReference(Constant.BLANG_HTTP, Constant.BLANG_CLIENT_CONNECTOR);
        ballerinaASTAPI.createRefereceTypeName();
        /*Create an object out of above created ref type and initialize it with values*/
        ballerinaASTAPI.createNameReference(Constant.BLANG_HTTP, Constant.BLANG_CLIENT_CONNECTOR);
        ballerinaASTAPI.startExprList();

        String protocol = (Constant.HTTPS.equalsIgnoreCase(requestConfig.getProtocol()) ?
                Constant.HTTPS_PROTOCOL :
                Constant.HTTP_PROTOCOL);
        String strUrl = "";
        if (!Constant.DEFAULT_PORT.equals(requestConfig.getPort())) {
            strUrl = protocol + requestConfig.getHost() + ":" + requestConfig.getPort() +
                    requestConfig.getBasePath();
        } else {
            strUrl = protocol + requestConfig.getHost() + requestConfig.getBasePath();
        }

        ballerinaASTAPI.createStringLiteral(strUrl);
        ballerinaASTAPI.endExprList(1); // no of arguments
        ballerinaASTAPI.initializeConnector(true); //arguments available
        connectorVarName = Constant.BLANG_CONNECT_VARIABLE_NAME + ++variableCounter;
        ballerinaASTAPI.createVariable(connectorVarName, true);
    }

    public BallerinaFile getBallerinaFile() {
        return ballerinaFile;
    }

    private void createVariableWithEmptyMap(String typeOfTheParamater, String variableName, boolean exprAvailable) {
        ballerinaASTAPI.addTypes(typeOfTheParamater);
        ballerinaASTAPI.addMapStructLiteral();
        ballerinaASTAPI.createVariable(variableName, exprAvailable);
        refVarName = variableName;
    }
}
