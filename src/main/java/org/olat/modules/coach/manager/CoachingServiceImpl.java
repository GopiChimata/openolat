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
package org.olat.modules.coach.manager;

import java.util.Collections;
import java.util.List;

import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.IdentityShort;
import org.olat.core.id.Identity;
import org.olat.course.assessment.UserEfficiencyStatement;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.modules.coach.CoachingService;
import org.olat.modules.coach.model.CourseStatEntry;
import org.olat.modules.coach.model.EfficiencyStatementEntry;
import org.olat.modules.coach.model.GroupStatEntry;
import org.olat.modules.coach.model.SearchCoachedIdentityParams;
import org.olat.modules.coach.model.StudentStatEntry;
import org.olat.repository.RepositoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  8 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
@Service
public class CoachingServiceImpl implements CoachingService {
	
	@Autowired
	private CoachingDAO coachingDao;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	

	@Override
	public boolean isCoach(Identity coach) {
		return coachingDao.isCoach(coach);
	}

	@Override
	public List<RepositoryEntry> getStudentsCourses(Identity coach, Identity student) {
		return coachingDao.getStudentsCourses(coach, student);
	}
	
	@Override
	public List<StudentStatEntry> getUsersStatistics(SearchCoachedIdentityParams params) {
		return coachingDao.getUsersStatisticsNative(params);
	}

	@Override
	public List<StudentStatEntry> getStudentsStatistics(Identity coach) {
		return coachingDao.getStudentsStatisticsNative(coach);
	}

	@Override
	public List<RepositoryEntry> getUserCourses(Identity student) {
		return coachingDao.getUserCourses(student);
	}

	@Override
	public List<CourseStatEntry> getCoursesStatistics(Identity coach) {
		return coachingDao.getCoursesStatisticsNative(coach);
	}

	@Override
	public List<GroupStatEntry> getGroupsStatistics(Identity coach) {
		return coachingDao.getGroupsStatisticsNative(coach);
	}

	@Override
	public List<EfficiencyStatementEntry> getGroup(BusinessGroup group) {
		List<Identity> students = businessGroupService.getMembers(group, GroupRoles.participant.name());
		List<RepositoryEntry> courses = businessGroupService.findRepositoryEntries(Collections.singletonList(group), 0, -1);
		return coachingDao.getEfficencyStatementEntriesAlt(students, courses);
	}

	@Override
	public List<EfficiencyStatementEntry> getCourse(Identity coach, RepositoryEntry entry) {
		List<Long> studentKeys = coachingDao.getStudents(coach, entry);
		List<IdentityShort> students = securityManager.findShortIdentitiesByKey(studentKeys);
		return coachingDao.getEfficencyStatementEntries(students, Collections.singletonList(entry));
	}

	@Override
	public EfficiencyStatementEntry getEfficencyStatement(UserEfficiencyStatement statement) {
		return coachingDao.getEfficencyStatementEntry(statement);
	}

	@Override
	public List<EfficiencyStatementEntry> getEfficencyStatements(Identity student, List<RepositoryEntry> courses) {
		IdentityShort identity = securityManager.loadIdentityShortByKey(student.getKey());
		List<IdentityShort> students = Collections.singletonList(identity);
		return coachingDao.getEfficencyStatementEntries(students, courses);
	}
	
	@Override
	public List<UserEfficiencyStatement> getEfficencyStatements(Identity student) {
		return coachingDao.getEfficencyStatementEntries(student);
	}

}
