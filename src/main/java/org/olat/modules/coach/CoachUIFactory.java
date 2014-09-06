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
package org.olat.modules.coach;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.modules.coach.ui.CourseListController;
import org.olat.modules.coach.ui.GroupListController;
import org.olat.modules.coach.ui.StudentListController;

/**
 * 
 * Description:<br>
 * UI factory for the coach site (used by the controller creator configured
 * in coachContext.xml)
 * 
 * <P>
 * Initial Date:  8 févr. 2012 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class CoachUIFactory {
	
	/**
	 * Return a controller which shows an overview over all students the user tutored.
	 * tutoring.
	 * @param ureq
	 * @param wControl
	 * @return The students overview controller
	 */
	public static Controller createStudentsController(UserRequest ureq, WindowControl wControl) {
		return new StudentListController(ureq, wControl);
	}
	
	/**
	 * Return a controller which shows an overview of all groups the user tutored.
	 * @param ureq
	 * @param wControl
	 * @return The groups overview controller
	 */
	public static Controller createGroupsController(UserRequest ureq, WindowControl wControl) {
		return new GroupListController(ureq, wControl);
	}
	
	/**
	 * Return a controller which shows an overview of all courses the user tutored.
	 * @param ureq
	 * @param wControl
	 * @return The courses overview controller
	 */
	public static Controller createCoursesController(UserRequest ureq, WindowControl wControl) {
		return new CourseListController(ureq, wControl);
	}
}
