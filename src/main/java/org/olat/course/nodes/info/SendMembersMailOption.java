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

package org.olat.course.nodes.info;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.olat.basesecurity.GroupRoles;
import org.olat.commons.info.ui.SendMailOption;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.util.Util;
import org.olat.group.BusinessGroupService;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.resource.OLATResource;

/**
 * 
 * Description:<br>
 * Send mails to members, coaches and owner of the course
 * 
 * <P>
 * Initial Date:  29 jul. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class SendMembersMailOption implements SendMailOption {
	
	private final OLATResource courseResource;
	private final RepositoryManager rm;
	private final RepositoryService repositoryService;
	private final BusinessGroupService businessGroupService;
	
	public SendMembersMailOption(OLATResource courseResource, RepositoryManager rm, RepositoryService repositoryService, BusinessGroupService businessGroupService) {
		this.courseResource = courseResource;
		this.rm = rm;
		this.repositoryService = repositoryService;
		this.businessGroupService = businessGroupService;
	}

	@Override
	public String getOptionKey() {
		return "send-mail-course-members";
	}

	@Override
	public String getOptionTranslatedName(Locale locale) {
		Translator translator = Util.createPackageTranslator(SendMembersMailOption.class, locale);
		return translator.translate("wizard.step1.send_option.member");
	}

	@Override
	public List<Identity> getSelectedIdentities() {
		RepositoryEntry repositoryEntry = rm.lookupRepositoryEntry(courseResource, true);
		
		List<Identity> members = businessGroupService.getMembersOf(repositoryEntry, true, true);
		Set<Identity> identities = new HashSet<Identity>(members);
		List<Identity> reMembers = repositoryService.getMembers(repositoryEntry, GroupRoles.participant.name(), GroupRoles.coach.name(), GroupRoles.owner.name());
		identities.addAll(reMembers);

		return new ArrayList<Identity>(identities);
	}
}
