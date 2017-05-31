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
import org.wso2.ei.tools.mule2ballerina.builder.BallerinaASTModelBuilder;
import org.wso2.ei.tools.mule2ballerina.model.Flow;
import org.wso2.ei.tools.mule2ballerina.model.GlobalConfiguration;
import org.wso2.ei.tools.mule2ballerina.model.HttpListener;
import org.wso2.ei.tools.mule2ballerina.model.HttpListenerConfig;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequest;
import org.wso2.ei.tools.mule2ballerina.model.HttpRequestConfig;
import org.wso2.ei.tools.mule2ballerina.model.Payload;
import org.wso2.ei.tools.mule2ballerina.model.Processor;
import org.wso2.ei.tools.mule2ballerina.model.Root;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * {@code TreeVisitor} visits Mule tree and populate Ballerina AST
 */
public class TreeVisitor implements Visitor {

    private static Logger logger = LoggerFactory.getLogger(TreeVisitor.class);

    private BallerinaASTModelBuilder ballerinaASTAPI;
    private Root mRoot;
    private BallerinaFile ballerinaFile;
    private int serviceCounter = 0;
    private int resourceCounter = 0;
    private Map<String, Boolean> serviceTrack = new HashMap<String, Boolean>();
    private Map<String, Boolean> importTracker = new HashMap<String, Boolean>();
    String inboundName;

    public TreeVisitor(Root mRoot) {
        ballerinaASTAPI = new BallerinaASTModelBuilder();
        this.mRoot = mRoot;
    }

    @Override
    public void visit(Root root) {
        logger.info("-SRoot");
        for (Flow flow : root.getFlowList()) {
            flow.accept(this);
        }
        logger.info("-ERoot");
        ballerinaFile = ballerinaASTAPI.buildBallerinaFile();
    }

    @Override
    public void visit(Flow flow) {
        logger.info("--SFlow");
        int i = 0;
        int flowSize = flow.getFlowProcessors().size();
        for (Processor processor : flow.getFlowProcessors()) {
            processor.accept(this);
            i++;
            if (flowSize == i) {
                ballerinaASTAPI.createNameReference(null, "response");
                ballerinaASTAPI.createVariableRefExpr();
                ballerinaASTAPI.createReplyStatement();

                ballerinaASTAPI.endCallableBody();

                String resourceName = "myResource" + ++resourceCounter;
                ballerinaASTAPI.endOfResource(resourceName, 1);
                logger.info("--EFlow");

                if (mRoot.getServiceMap() != null) {
                    Queue<Flow> flows = mRoot.getServiceMap().get(inboundName);
                    if (flows != null) {
                        flows.remove();
                        if (flows.size() == 0) {
                            String serviceName = "myService" + ++serviceCounter;
                            ballerinaASTAPI.endOfService(serviceName);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void visit(Payload payload) {
        if (importTracker.isEmpty() || importTracker.get("messages") == null) {
            ballerinaASTAPI.addImportPackage(ballerinaASTAPI.getBallerinaPackageMap().get("messages"), null);
            importTracker.put("messages", true);
        }
        logger.info("----Payload");
        ballerinaASTAPI.createNameReference("messages", "setStringPayload");
        ballerinaASTAPI.startExprList();
        ballerinaASTAPI.createNameReference(null, "response");
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.createStringLiteral(payload.getValue());
        ballerinaASTAPI.endExprList(2);
        ballerinaASTAPI.createFunctionInvocation(true);
    }

    @Override
    public void visit(HttpListenerConfig listenerConfig) {
        if (importTracker.isEmpty() || importTracker.get("http") == null) {
            ballerinaASTAPI.addImportPackage(ballerinaASTAPI.getBallerinaPackageMap().get("http"), null);
            importTracker.put("http", true);
        }
        logger.info("--HttpListenerConfig");
        if (serviceTrack.get(listenerConfig.getName()) == null) {
            ballerinaASTAPI.startService();
            ballerinaASTAPI.createAnnotationAttachment("http", "basePath", "value", listenerConfig.getBasePath());
            ballerinaASTAPI.addAnnotationAttachment(1);
            serviceTrack.put(listenerConfig.getName(), true);
            inboundName = listenerConfig.getName();
        }
    }

    @Override
    public void visit(HttpListener listener) {
        logger.info("----HttpListener");
        GlobalConfiguration globalConfiguration = mRoot.getConfigMap().get(listener.getConfigName());
        globalConfiguration.accept(this);
        ballerinaASTAPI.startResource();
        String allowedMethods = "GET";
        if (listener.getAllowedMethods() != null) {
            allowedMethods = listener.getAllowedMethods();
        }
        ballerinaASTAPI.createAnnotationAttachment("http", allowedMethods, null, null);
        ballerinaASTAPI.addAnnotationAttachment(0);
        ballerinaASTAPI.addTypes("message");
        ballerinaASTAPI.addParameter(0, false, "m");
        ballerinaASTAPI.startCallableBody();
        createVariableOfTypeMessage();
    }

    @Override
    public void visit(HttpRequest request) {
        logger.info("----HttpRequest");
        GlobalConfiguration globalConfiguration = mRoot.getConfigMap().get(request.getConfigName());
        globalConfiguration.accept(this);

        ballerinaASTAPI.createVariableRefList();
        ballerinaASTAPI.createNameReference(null, "response");
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.endVariableRefList(1);
        ballerinaASTAPI.createNameReference("http", "ClientConnector");
        ballerinaASTAPI.startExprList();
        ballerinaASTAPI.createNameReference(null, "mockiEP");
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.createStringLiteral(request.getPath());
        ballerinaASTAPI.createNameReference(null, "m");
        ballerinaASTAPI.createVariableRefExpr();
        ballerinaASTAPI.endVariableRefList(3);
        ballerinaASTAPI.createAction("get", true);
        ballerinaASTAPI.createAssignmentStatement();
    }

    @Override
    public void visit(HttpRequestConfig requestConfig) {
        logger.info("----HttpRequestConfig");
        ballerinaASTAPI.createNameReference("http", "ClientConnector");
        ballerinaASTAPI.createRefereceTypeName();
        ballerinaASTAPI.createNameReference("http", "ClientConnector");
        ballerinaASTAPI.startExprList();
        String strUrl = "http://www." + requestConfig.getHost() + ":" + requestConfig.getPort() + "/" + requestConfig
                .getBasePath();
        ballerinaASTAPI.createStringLiteral(strUrl);
        ballerinaASTAPI.endExprList(1);
        ballerinaASTAPI.initializeConnector(true);
        ballerinaASTAPI.createVariable("mockiEP", true);
    }

    public BallerinaFile getBallerinaFile() {
        return ballerinaFile;
    }

    private void createVariableOfTypeMessage() {
        ballerinaASTAPI.addTypes("message");
        ballerinaASTAPI.addMapStructLiteral();
        ballerinaASTAPI.createVariable("response", true);
    }
}
