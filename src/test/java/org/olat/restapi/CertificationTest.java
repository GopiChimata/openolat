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
package org.olat.restapi;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.certificate.Certificate;
import org.olat.course.certificate.CertificatesManager;
import org.olat.course.certificate.model.CertificateInfos;
import org.olat.repository.RepositoryEntry;
import org.olat.restapi.support.ObjectFactory;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatJerseyTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 17.11.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CertificationTest extends OlatJerseyTestCase {

	@Autowired
	private DB dbInstance;
	@Autowired
	private CertificatesManager certificatesManager;


	@Test
	public void getCertificate() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		Identity assessedIdentity = JunitTestHelper.createAndPersistIdentityAsRndUser("cert-1");
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("cert-2");
		RepositoryEntry entry = JunitTestHelper.deployBasicCourse(author);

		MailerResult result = new MailerResult();
		CertificateInfos certificateInfos = new CertificateInfos(assessedIdentity, 2.0f, true);
		Certificate certificate = certificatesManager.generateCertificate(certificateInfos, entry, null, result);
		dbInstance.commitAndCloseSession();
		Assert.assertNotNull(certificate);

		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.path(entry.getOlatResource().getKey().toString())
				.path("certificates").path(assessedIdentity.getKey().toString()).build();
		HttpGet method = conn.createGet(uri, "application/pdf", true);
		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());

		EntityUtils.consume(response.getEntity());

		conn.shutdown();
	}
	
	@Test
	public void generateCertificate() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		Identity assessedIdentity = JunitTestHelper.createAndPersistIdentityAsRndUser("cert-1");
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("cert-2");
		RepositoryEntry entry = JunitTestHelper.deployBasicCourse(author);
		dbInstance.commitAndCloseSession();

		Date creationDate = createDate(2014, 9, 9);
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.path(entry.getOlatResource().getKey().toString())
				.path("certificates").path(assessedIdentity.getKey().toString())
				.queryParam("score", "3.2")
				.queryParam("passed", "true")
				.queryParam("creationDate", ObjectFactory.formatDate(creationDate)).build();

		HttpPut method = conn.createPut(uri, MediaType.APPLICATION_JSON, true);

		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		EntityUtils.consume(response.getEntity());
		
		//async process mean sleep a little
		sleep(2000);
		
		//check certificate
		Certificate certificate = certificatesManager.getLastCertificate(assessedIdentity, entry.getOlatResource().getKey());
		Assert.assertNotNull(certificate);
		Assert.assertEquals(creationDate, certificate.getCreationDate());
		//check the certificate file
		VFSLeaf certificateLeaf = certificatesManager.getCertificateLeaf(certificate);
		Assert.assertNotNull(certificateLeaf);
		Assert.assertTrue(certificateLeaf.getSize() > 500);
	}

	@Test
	public void uploadCertificate() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		Identity assessedIdentity = JunitTestHelper.createAndPersistIdentityAsRndUser("cert-1");
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("cert-2");
		RepositoryEntry entry = JunitTestHelper.deployBasicCourse(author);
		dbInstance.commitAndCloseSession();

		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.path(entry.getOlatResource().getKey().toString())
				.path("certificates").path(assessedIdentity.getKey().toString()).build();

		URL certificateUrl = CertificationTest.class.getResource("certificate.pdf");
		Assert.assertNotNull(certificateUrl);
		File certificateFile = new File(certificateUrl.toURI());
		HttpPost method = conn.createPost(uri, MediaType.APPLICATION_JSON);
		
		Date creationDate = createDate(2014, 7, 1);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addTextBody("filename", certificateFile.getName())
				.addBinaryBody("file", certificateFile, ContentType.APPLICATION_OCTET_STREAM, certificateFile.getName())
				.addTextBody("creationDate", ObjectFactory.formatDate(creationDate));
		method.setEntity(builder.build());

		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		EntityUtils.consume(response.getEntity());

		//check certificate
		Certificate certificate = certificatesManager.getLastCertificate(assessedIdentity, entry.getOlatResource().getKey());
		Assert.assertNotNull(certificate);
		Assert.assertEquals(creationDate, certificate.getCreationDate());
		//check the certificate file
		VFSLeaf certificateLeaf = certificatesManager.getCertificateLeaf(certificate);
		Assert.assertNotNull(certificateLeaf);
		Assert.assertEquals(certificateFile.length(), certificateLeaf.getSize());
	}
	
	@Test
	public void uploadCertificate_standalone() throws IOException, URISyntaxException {
		RestConnection conn = new RestConnection();
		Assert.assertTrue(conn.login("administrator", "openolat"));

		Identity assessedIdentity = JunitTestHelper.createAndPersistIdentityAsRndUser("cert-1");
		dbInstance.commitAndCloseSession();

		Long resourceKey = 23687468l;
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.path(resourceKey.toString())
				.path("certificates").path(assessedIdentity.getKey().toString()).build();

		URL certificateUrl = CertificationTest.class.getResource("certificate.pdf");
		Assert.assertNotNull(certificateUrl);
		File certificateFile = new File(certificateUrl.toURI());
		HttpPost method = conn.createPost(uri, MediaType.APPLICATION_JSON);
		
		Date creationDate = createDate(2014, 7, 1);
		MultipartEntityBuilder builder = MultipartEntityBuilder.create()
				.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
				.addTextBody("filename", certificateFile.getName())
				.addBinaryBody("file", certificateFile, ContentType.APPLICATION_OCTET_STREAM, certificateFile.getName())
				.addTextBody("creationDate", ObjectFactory.formatDate(creationDate));
		method.setEntity(builder.build());

		HttpResponse response = conn.execute(method);
		Assert.assertEquals(200, response.getStatusLine().getStatusCode());
		EntityUtils.consume(response.getEntity());

		//check certificate
		Certificate certificate = certificatesManager.getLastCertificate(assessedIdentity, resourceKey);
		Assert.assertNotNull(certificate);
		Assert.assertEquals(creationDate, certificate.getCreationDate());
		//check the certificate file
		VFSLeaf certificateLeaf = certificatesManager.getCertificateLeaf(certificate);
		Assert.assertNotNull(certificateLeaf);
		Assert.assertEquals(certificateFile.length(), certificateLeaf.getSize());
	}
	
	private Date createDate(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DATE, day);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}
}