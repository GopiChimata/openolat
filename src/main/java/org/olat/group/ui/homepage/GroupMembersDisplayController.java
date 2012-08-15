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
package org.olat.group.ui.homepage;

import org.olat.admin.securitygroup.gui.GroupController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.group.BusinessGroup;
import org.olat.group.model.DisplayMembers;

/**
 * 
 * Initial Date:  Aug 19, 2009 <br>
 * @author twuersch, www.frentix.com
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class GroupMembersDisplayController extends BasicController {


	private final VelocityContainer content;

	public GroupMembersDisplayController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup, DisplayMembers members) {
		super(ureq, wControl);
		// display owners and participants
		content = createVelocityContainer("groupmembersdisplay");

		if(members.isOwnersPublic()) {
			GroupController groupOwnersController = new GroupController(ureq, wControl, false, true, false, businessGroup.getOwnerGroup());
			content.put("owners", groupOwnersController.getInitialComponent());
		}
		if(members.isParticipantsPublic()) {
			GroupController groupParticipantsController = new GroupController(ureq, wControl, false, true, false, businessGroup.getPartipiciantGroup());
			content.put("participants", groupParticipantsController.getInitialComponent());
		}
		putInitialPanel(content);
	}

	@Override
	protected void doDispose() {
	// Nothing to do here.
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
	// Do nothing.
	}
}
