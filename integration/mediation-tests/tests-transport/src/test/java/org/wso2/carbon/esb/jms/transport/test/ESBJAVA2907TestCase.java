package org.wso2.carbon.esb.jms.transport.test;

import org.apache.axiom.om.OMElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.esb.integration.common.utils.clients.axis2client.AxisServiceClient;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.JMSEndpointManager;
import org.wso2.esb.integration.common.utils.Utils;
import org.wso2.carbon.logging.view.data.xsd.LogEvent;

/**
 * ESBJAVA-2907 OMElements are not added as properties when saving messages to the MessageStore
 */
public class ESBJAVA2907TestCase extends ESBIntegrationTest {

	private static String GET_QUOTE_REQUEST_BODY = "OM_ELEMENT_PREFIX_ = <ns:getQuote xmlns:ns=\"http://services.samples\"><ns:request><ns:symbol>IBM</ns:symbol></ns:request></ns:getQuote>";

	@BeforeClass(alwaysRun = true)
	protected void init() throws Exception {
		super.init();
		OMElement synapse = esbUtils
				.loadResource("/artifacts/ESB/synapseconfig/messageStore/ESBJAVA-2907StoreOmElementsAsProperties.xml");
		updateESBConfiguration(JMSEndpointManager.setConfigurations(synapse));
	}

	@Test(groups = "wso2.esb", description = "Test adding OMElements as properties when saving messages to the MessageStore")
	public void testAddingOMElementPropertyToMessageStore() throws Exception {
		AxisServiceClient client = new AxisServiceClient();
		client.sendRobust(Utils.getStockQuoteRequest("IBM"), getProxyServiceURLHttp("testPS"), "getQuote");
		LogViewerClient cli = new LogViewerClient(contextUrls.getBackEndUrl(),getSessionCookie());
		boolean hasPrefix = Utils.checkForLog(cli, GET_QUOTE_REQUEST_BODY, 5);
		Assert.assertTrue(hasPrefix, "OMElement is not saved to the message store");
		log.info(cli.getAllRemoteSystemLogs());
	}

	@AfterClass(alwaysRun = true)
	public void UndeployService() throws Exception {
		super.cleanup();
	}
}
