/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */


package org.olat.restapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.restapi.repository.course.CoursesWebService;
import org.olat.restapi.support.vo.CourseVO;
import org.olat.restapi.support.vo.CourseVOes;
import org.olat.test.OlatJerseyTestCase;

public class CoursesTest extends OlatJerseyTestCase {
	
	private static final OLog log = Tracing.createLoggerFor(CoursesTest.class);
	
	private Identity admin;
	private ICourse course1, course2;

	/**
	 * SetUp is called before each test.
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		try {
			// create course and persist as OLATResourceImpl
			admin = BaseSecurityManager.getInstance().findIdentityByName("administrator");
			course1 = CoursesWebService.createEmptyCourse(admin, "courses1", "courses1 long name", null);
			course2 = CoursesWebService.createEmptyCourse(admin, "courses2", "courses2 long name", null);
			
			DBFactory.getInstance().closeSession();
		} catch (Exception e) {
			log.error("Exception in setUp(): " + e);
		}
	}
	
	@Test
	public void testGetCourses() throws IOException {
		HttpClient c = loginWithCookie("administrator", "olat");
		
		HttpMethod method = createGet("/repo/courses", MediaType.APPLICATION_JSON, true);
		int code = c.executeMethod(method);
		assertEquals(code, 200);
		String body = method.getResponseBodyAsString();
		List<CourseVO> courses = parseCourseArray(body);
		assertNotNull(courses);
		assertTrue(courses.size() >= 2);
		
		CourseVO vo1 = null;
		CourseVO vo2 = null;
		for(CourseVO course:courses) {
			if(course.getTitle().equals("courses1")) {
				vo1 = course;
			} else if(course.getTitle().equals("courses2")) {
				vo2 = course;
			}
		}
		assertNotNull(vo1);
		assertEquals(vo1.getKey(), course1.getResourceableId());
		assertNotNull(vo2);
		assertEquals(vo2.getKey(), course2.getResourceableId());
	}
	
	@Test
	public void testGetCoursesWithPaging() throws IOException {
		HttpClient c = loginWithCookie("administrator", "olat");
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
				.queryParam("start", "0").queryParam("limit", "1").build();
		
		HttpMethod method = createGet(uri, MediaType.APPLICATION_JSON + ";pagingspec=1.0", true);
		int code = c.executeMethod(method);
		assertEquals(code, 200);
		InputStream body = method.getResponseBodyAsStream();
		CourseVOes courses = parse(body, CourseVOes.class);
		assertNotNull(courses);
		assertNotNull(courses.getCourses());
		assertEquals(1, courses.getCourses().length);
	}
	
	@Test
	public void testCreateEmptyCourse() throws IOException {
		HttpClient c = loginWithCookie("administrator", "olat");
		
		URI uri = UriBuilder.fromUri(getContextURI()).path("repo").path("courses")
			.queryParam("shortTitle", "course3").queryParam("title", "course3 long name").build();
		PutMethod method = createPut(uri, MediaType.APPLICATION_JSON, true);
		
		int code = c.executeMethod(method);
		assertEquals(code, 200);
		String body = method.getResponseBodyAsString();
		CourseVO course = parse(body, CourseVO.class);
		assertNotNull(course);
		assertEquals("course3", course.getTitle());
		//check repository entry
		RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(course.getRepoEntryKey());
		assertNotNull(re);
		assertNotNull(re.getOlatResource());
		assertNotNull(re.getOwnerGroup());
	}
	
	protected List<CourseVO> parseCourseArray(String body) {
		try {
			ObjectMapper mapper = new ObjectMapper(jsonFactory); 
			return mapper.readValue(body, new TypeReference<List<CourseVO>>(){/* */});
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}