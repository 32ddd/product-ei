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

package org.wso2.ei.tools.synapse2ballerina.wrapper;

import org.apache.synapse.Mediator;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.wso2.ei.tools.synapse2ballerina.visitor.Visitor;

/**
 * {@code SequenceMediatorWrapper} Wrapper class for synapse SequenceMediator
 */
public class SequenceMediatorWrapper extends MediatorWrapper {

    private SequenceMediator sequenceMediator;

    public SequenceMediatorWrapper(Mediator original) {
        super(original);
        if (original instanceof SequenceMediator) {
            this.sequenceMediator = (SequenceMediator) original;
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(sequenceMediator);
    }

}
