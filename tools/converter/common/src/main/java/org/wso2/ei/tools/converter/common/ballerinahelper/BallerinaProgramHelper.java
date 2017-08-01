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

package org.wso2.ei.tools.converter.common.ballerinahelper;

import org.wso2.ei.tools.converter.common.builder.BallerinaASTModelBuilder;
import org.wso2.ei.tools.converter.common.util.Constant;

import java.util.Map;

/**
 * Define ballerina helper methods
 */
public class BallerinaProgramHelper {

    /**
     * Add Import
     *
     * @param ballerinaASTModelBuilder
     * @param packageName
     * @param importTracker
     */
    public static void addImport(BallerinaASTModelBuilder ballerinaASTModelBuilder, String packageName,
            Map<String, Boolean> importTracker) {
        if (importTracker.isEmpty() || importTracker.get(packageName) == null) {
            ballerinaASTModelBuilder
                    .addImportPackage(ballerinaASTModelBuilder.getBallerinaPackageMap().get(packageName), null);
            importTracker.put(packageName, true);
        }
    }

    /**
     * Create Ballerina reply statement
     *
     * @param ballerinaASTModelBuilder
     * @param parameters
     */
    public static void createReply(BallerinaASTModelBuilder ballerinaASTModelBuilder, Map<String, Object> parameters) {
        ballerinaASTModelBuilder.createNameReference(null, (String) parameters.get(Constant.OUTBOUND_MSG));
        ballerinaASTModelBuilder.createSimpleVarRefExpr();
        ballerinaASTModelBuilder.createReplyStatement();
        ballerinaASTModelBuilder.endCallableBody();
    }

    /**
     * Create empty map
     *
     * @param ballerinaASTModelBuilder
     * @param typeOfTheParamater
     * @param variableName
     * @param exprAvailable
     * @return
     */
    public static String createVariableWithEmptyMap(BallerinaASTModelBuilder ballerinaASTModelBuilder,
            String typeOfTheParamater, String variableName, boolean exprAvailable) {
        ballerinaASTModelBuilder.addTypes(typeOfTheParamater);
        ballerinaASTModelBuilder.startMapStructLiteral();
        ballerinaASTModelBuilder.createMapStructLiteral();
        ballerinaASTModelBuilder.createVariable(variableName, exprAvailable);
        String outboundMsg = variableName;
        return outboundMsg;
    }

}
