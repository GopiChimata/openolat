/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.test.rest;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.olat.restapi.RestConnection;
import org.olat.user.restapi.RolesVO;
import org.olat.user.restapi.UserVO;

/**
 * REST client for the user webservice.
 * 
 * 
 * Initial date: 19.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class UserRestClient {
	
	private static final AtomicInteger counter = new AtomicInteger();
	
	private final URL deploymentUrl;
	private final String username;
	private final String password;
	
	public UserRestClient(URL deploymentUrl) {
		this(deploymentUrl, "administrator", "openolat");
	}
	
	public UserRestClient(URL deploymentUrl, String username, String password) {
		this.deploymentUrl = deploymentUrl;
		this.username = username;
		this.password = password;
	}
	

	
	public UserVO createRandomUser()
	throws IOException, URISyntaxException {
		return createRandomUser("Selena");
	}
	
	public UserVO createRandomUser(String name)
	throws IOException, URISyntaxException {
		RestConnection restConnection = new RestConnection(deploymentUrl);
		assertTrue(restConnection.login(username, password));
		
		UserVO user = createUser(restConnection, name, "Rnd");

		restConnection.shutdown();
		return user;
	}
	
	public UserVO createAuthor()
	throws IOException, URISyntaxException {
		RestConnection restConnection = new RestConnection(deploymentUrl);
		assertTrue(restConnection.login(username, password));
		
		UserVO user = createUser(restConnection, "Selena", "Auth");
		
		RolesVO roles = new RolesVO();
		roles.setAuthor(true);
		
		//update roles of author
		URI request = getUsersURIBuilder().path(user.getKey().toString()).path("roles").build();
		HttpPost method = restConnection.createPost(request, MediaType.APPLICATION_JSON);
		restConnection.addJsonEntity(method, roles);
		HttpResponse response = restConnection.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		EntityUtils.consume(response.getEntity());

		restConnection.shutdown();
		return user;
	}
	
	private UserVO createUser(RestConnection restConnection, String name, String role)
	throws URISyntaxException, IOException {
		String uuid = Integer.toString(counter.incrementAndGet()) + UUID.randomUUID().toString();
		
		UserVO vo = new UserVO();
		String username = (name + "-" + uuid).substring(0, 24);
		vo.setLogin(username);
		String password = ("passwd-" + uuid).substring(0, 24);
		vo.setPassword(password);
		vo.setFirstName(name + "-"+ role + "-" + uuid);
		vo.setLastName("Smith");
		vo.setEmail(username + "@frentix.com");
		vo.putProperty("telOffice", "39847592");
		vo.putProperty("telPrivate", "39847592");
		vo.putProperty("telMobile", "39847592");
		vo.putProperty("gender", "Female");//male or female
		vo.putProperty("birthDay", "12/12/2009");

		URI request = getUsersURIBuilder().build();
		HttpPut method = restConnection.createPut(request, MediaType.APPLICATION_JSON, true);
		restConnection.addJsonEntity(method, vo);
		method.addHeader("Accept-Language", "en");

		HttpResponse response = restConnection.execute(method);
		int responseCode = response.getStatusLine().getStatusCode();
		assertTrue(responseCode == 200 || responseCode == 201);
		InputStream body = response.getEntity().getContent();
		UserVO current = restConnection.parse(body, UserVO.class);
		Assert.assertNotNull(current);
		
		current.setPassword(vo.getPassword());
		return current;
	}
	
	private UriBuilder getUsersURIBuilder()
	throws URISyntaxException {
		return UriBuilder.fromUri(deploymentUrl.toURI()).path("restapi").path("users");
	}

	
}