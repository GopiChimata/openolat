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
package org.olat.modules.coach.model;

import org.olat.basesecurity.IdentityShort;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.course.assessment.UserEfficiencyStatement;
import org.olat.repository.RepositoryEntry;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  9 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class EfficiencyStatementEntry {
	
	private final Long studentKey;
	private final String studentFullName;
	private final RepositoryEntry course;
	private final UserEfficiencyStatement efficencyStatement;
	
	public EfficiencyStatementEntry(IdentityShort student, RepositoryEntry course, UserEfficiencyStatement efficencyStatement) {
		this.course = course;
		this.efficencyStatement = efficencyStatement;
		this.studentKey = student.getKey();
		this.studentFullName = getFullName(student);
	}
	
	public EfficiencyStatementEntry(Identity student, RepositoryEntry course, UserEfficiencyStatement efficencyStatement) {
		this.course = course;
		this.efficencyStatement = efficencyStatement;
		this.studentKey = student.getKey();
		this.studentFullName = getFullName(student);
	}
	
	private String getFullName(IdentityShort student) {
		if(student == null) {
			return "";
		}
		return student.getFirstName() + " " + student.getLastName();
	}
	
	private String getFullName(Identity student) {
		if(student == null) {
			return "";
		}
		return student.getUser().getProperty(UserConstants.FIRSTNAME, null) + " " + student.getUser().getProperty(UserConstants.LASTNAME, null);
	}
	
	public Long getStudentKey() {
		return studentKey;
	}
	
	public String getStudentFullName() {
		return studentFullName;
	}
	
	public String getCourseDisplayName() {
		return null;
	}
	
	public RepositoryEntry getCourse() {
		return course;
	}
	
	/**
	 * Can return null
	 * @return
	 */
	public UserEfficiencyStatement getUserEfficencyStatement() {
		return efficencyStatement;
	}
}
