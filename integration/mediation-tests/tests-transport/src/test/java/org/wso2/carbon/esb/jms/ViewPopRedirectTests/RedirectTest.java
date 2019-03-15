/*
 *Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *WSO2 Inc. licenses this file to you under the Apache License,
 *Version 2.0 (the "License"); you may not use this file except
 *in compliance with the License.
 *You may obtain a copy of the License at
 *
 *http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.wso2.carbon.esb.jms.ViewPopRedirectTests;

import org.apache.axis2.context.ConfigurationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.extensions.servers.jmsserver.client.JMSQueueMessageProducer;
import org.wso2.carbon.automation.test.utils.http.client.HttpRequestUtil;
import org.wso2.esb.integration.common.clients.mediation.MessageProcessorClient;
import org.wso2.esb.integration.common.clients.mediation.MessageStoreAdminClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class RedirectTest extends ESBIntegrationTest {
    private MessageStoreAdminClient messageStoreAdminClient;
    private MessageProcessorClient messageProcessorClient;
    private JMSQueueMessageProducer jmsQueueMessageProducer;

    private ConfigurationContext configurationContext;

    private final String STORE_NAME = "BASE_Store";
    private final String PROCESSOR_NAME = "BASE_Processor";
    private final String PROXY_NAME = "BASE_Proxy";

    private final String REDIRECT_STORE_NAME = "REDIRECT_Store";
    private final String REDIRECT_PROCESSOR_NAME = "REDIRECT_Processor";

    /**
     * Initializing environment variables
     */

    @BeforeClass(alwaysRun = true, description = "Test Message processor Redirect service")
    protected void setup() throws Exception {
        super.init();
        loadESBConfigurationFromClasspath("artifacts/ESB/messageProcessorConfig/RedirectTest.xml");

        // Initialization
        messageStoreAdminClient = new MessageStoreAdminClient(context.getContextUrls().getBackEndUrl(), sessionCookie);
        messageProcessorClient = new MessageProcessorClient(context.getContextUrls().getBackEndUrl(), sessionCookie);

        verifyMessageStoreExistence(STORE_NAME);
        verifyMessageStoreExistence(REDIRECT_STORE_NAME);
        verifyMessageProcessorExistence(PROCESSOR_NAME);
        verifyMessageProcessorExistence(REDIRECT_PROCESSOR_NAME);
        isProxySuccesfullyDeployed(PROXY_NAME);
    }

    /**
     * 1. Send one payload to the proxy while the backend is unavailable
     * 2. Check if the Message Processor has successfully deactivated
     * 3. Call the popandRedirect fucntion and verify that getMessage function is returning is expected message
     */

    @Test(groups = {"wso2.esb"}, description = "Test Redirect service for Message processor")
    public void testRedirect() throws Exception {

        //Initializing Payload
        String inputPayload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "<soapenv:Header/>\n"
                + "<soapenv:Body>\n"
                + "<m0:getQuote xmlns:m0=\"http://services.samples\">\n"
                + " <m0:request>IBM\n"
                + " </m0:request>\n"
                + "   <m0:request>WSO2\n"
                + " </m0:request>\n"
                + "</m0:getQuote>\n"
                + "</soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String expectedMessage = "<?xml version='1.0' encoding='utf-8'?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Body>"
                + "<m0:getQuote xmlns:m0=\"http://services.samples\">\n"
                + " <m0:request>IBM\n"
                + " </m0:request>\n"
                + "   <m0:request>WSO2\n"
                + " </m0:request>\n"
                + "</m0:getQuote>"
                + "</soapenv:Body>"
                + "</soapenv:Envelope>";

        Map<String, String> requestHeader = new HashMap<>();
        requestHeader.put("Content-type", "text/xml");
        requestHeader.put("SOAPAction", "urn:mediate");
        requestHeader.put("Accept", "application/json");

        //Send payload to the proxy
        System.out.println("=== Sending payload to " + PROXY_NAME + ":" + getProxyServiceURLHttp(PROXY_NAME));
        HttpRequestUtil.doPost(new URL(getProxyServiceURLHttp(PROXY_NAME)), inputPayload, requestHeader);

        Thread.sleep(5000); //Make sure that the Message Processor has time to deactivate

        //Check if the message processor has deactivated
        Assert.assertFalse(messageProcessorClient.isActive(PROCESSOR_NAME), "Message processor" + PROCESSOR_NAME + "should not be active, " +
                "but it is active.");

        //Deactivate the REDIRECT_PROCESSOR
        messageProcessorClient.deactivateProcessor(REDIRECT_PROCESSOR_NAME);
        Assert.assertFalse(messageProcessorClient.isActive(REDIRECT_PROCESSOR_NAME), "Message processor" + REDIRECT_PROCESSOR_NAME + " should not be active, " +
                "but it is active.");

        //Call popAndRedirect function to pass the message to REDIRECT_STORE
        messageProcessorClient.popAndRedirectMessage(PROCESSOR_NAME, REDIRECT_STORE_NAME);


        //Call the getMessage function from the REDIRECT_PROCESSOR
        String returnedMessage = messageProcessorClient.getMessage(REDIRECT_PROCESSOR_NAME);
        System.out.println("=== RETURNED MESSAGE === \n" + returnedMessage);
        Assert.assertEquals(returnedMessage, expectedMessage, "Returned message is not the same as expected message.");

    }
}
