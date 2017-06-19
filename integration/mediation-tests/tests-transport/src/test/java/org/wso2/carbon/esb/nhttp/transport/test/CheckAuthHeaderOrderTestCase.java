package org.wso2.carbon.esb.nhttp.transport.test;

import java.io.File;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.esb.integration.common.utils.ESBIntegrationTest;
import org.wso2.esb.integration.common.utils.servers.WireMonitorServer;

/**
 * This test case is written to track the issue reported in
 * https://wso2.org/jira/browse/ESBJAVA-5121.This checks whether the order of
 * the auth headers is correct before sending the request to the endpoint
 */

public class CheckAuthHeaderOrderTestCase extends ESBIntegrationTest {

	public WireMonitorServer wireServer;

	@BeforeClass(alwaysRun = true)
	public void setEnvironment() throws Exception {
		init();
		wireServer = new WireMonitorServer(8991);
		wireServer.start();
		loadESBConfigurationFromClasspath(File.separator + "artifacts" + File.separator + "ESB" + File.separator
				+ "nhttp" + File.separator + "transport" + File.separator + "auth-headers.xml");
	}

	@Test(groups = { "wso2.esb" }, description = "Sending a Message Via REST to check the order of the auth headers")
	public void testAuthHeaderOrder() throws Exception {

		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpPost httpPost = new HttpPost(getApiInvocationURL("stockquote") + "/order/");
		httpPost.addHeader("WWW-Authenticate", "NTLM");
		httpPost.addHeader("WWW-Authenticate", "Basic realm=\"BasicSecurityFilterProvider\"");
		httpPost.addHeader("WWW-Authenticate", "ANTLM3");

		httpClient.execute(httpPost);

		String response = wireServer.getCapturedMessage();

		Assert.assertNotNull(response);
		Assert.assertTrue(response.contains(
				"WWW-Authenticate: NTLM\r\nWWW-Authenticate: Basic realm=\"BasicSecurityFilterProvider\"\r\nWWW-Authenticate: ANTLM3"));

	}

	@AfterClass(alwaysRun = true)
	public void destroy() throws Exception {
		super.cleanup();
	}

}
