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
package org.olat.course.assessment.manager;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.course.CorruptedCourseException;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.config.CourseConfig;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.model.SearchRepositoryEntryParameters;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 27.02.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class EfficiencyStatementManagerTest extends OlatTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private RepositoryManager repositoryManager;
	
	@Test
	public void testBigDatas() {
		SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters();
		params.setRoles(new Roles(true, false, false, false, false, false, false));
		params.setResourceTypes(Collections.singletonList("CourseModule"));
		List<RepositoryEntry> entries = repositoryManager.genericANDQueryWithRolesRestriction(params, 0, -1, true);
		
		List<Identity> loadIdentities = securityManager
				.getVisibleIdentitiesByPowerSearch(null, null, false, null, null, null, null, null, 0, 10000);
		EfficiencyStatementManager efficiencyStatementManager = EfficiencyStatementManager.getInstance();
		
		int count = 0;
		for(RepositoryEntry entry:entries) {
			Long resourceableId = entry.getOlatResource().getResourceableId();
		
			try {
				ICourse course = CourseFactory.loadCourse(resourceableId);			
				boolean enabled =course.getCourseEnvironment().getCourseConfig().isEfficencyStatementEnabled();
				if(!enabled) {
					course = CourseFactory.openCourseEditSession(entry.getOlatResource().getResourceableId());
					CourseConfig courseConfig = course.getCourseEnvironment().getCourseConfig();
					courseConfig.setEfficencyStatementIsEnabled(true);
					CourseFactory.setCourseConfig(course.getResourceableId(), courseConfig);
					CourseFactory.saveCourse(course.getResourceableId());
					CourseFactory.closeCourseEditSession(course.getResourceableId(),true);
				}
				DBFactory.getInstance().commitAndCloseSession();
				
				try {
					int fromIndex = (int)(Math.random() * loadIdentities.size() - 1);
					if(fromIndex < 100) {
						fromIndex = 100;
					}
					List<Identity> assessedIdentities = loadIdentities.subList(fromIndex - 100, fromIndex);
					//force the storing of the efficiencyStatement - this is usually done only at Learnresource/modify properties/Efficiency statement (ON)
					efficiencyStatementManager.updateEfficiencyStatements(course, assessedIdentities);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				DBFactory.getInstance().commitAndCloseSession();
				DBFactory.getInstance().closeSession();
			} catch (CorruptedCourseException e) {
				System.out.println("Error");
			}
			
			if(count++ % 100 == 0) {
				dbInstance.commitAndCloseSession();
			}
		}
		

	}

}
