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
package org.olat.course.member;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.MultipleSelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.group.ui.main.SearchMembersParams;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.EmailProperty;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MemberSearchForm extends FormBasicController {
	
	private String[] roleKeys = {"owner", "tutor", "attendee", "waiting"};
	private String[] originKeys = new String[]{"all", "repo", "group"};
	
	private TextElement login;
	private SingleSelection originEl;
	private MultipleSelectionElement rolesEl;
	
	private Map<String,FormItem> propFormItems;
	private List<UserPropertyHandler> userPropertyHandlers;
	
	private final UserManager userManager;

	public MemberSearchForm(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, "search_form", Util.createPackageTranslator(UserPropertyHandler.class, ureq.getLocale()));
		
		userManager = CoreSpringFactory.getImpl(UserManager.class);

		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		FormLayoutContainer leftContainer = FormLayoutContainer.createDefaultFormLayout("left_1", getTranslator());
		leftContainer.setRootForm(mainForm);
		formLayout.add(leftContainer);
		//user property
		login = uifactory.addTextElement("login", "search.login", 128, "", leftContainer);
		login.setDisplaySize(28);

		userPropertyHandlers = userManager.getUserPropertyHandlersFor(getClass().getCanonicalName(), false);
		propFormItems = new HashMap<String,FormItem>();
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) continue;

			FormItem fi = userPropertyHandler.addFormItem(getLocale(), null, getClass().getCanonicalName(), false, leftContainer);
			fi.setTranslator(this.getTranslator());
			// DO NOT validate email field => see OLAT-3324, OO-155, OO-222
			if (userPropertyHandler instanceof EmailProperty && fi instanceof TextElement) {
				TextElement textElement = (TextElement)fi;
				textElement.setItemValidatorProvider(null);
				
			}
			if(fi instanceof TextElement) {
				((TextElement)fi).setDisplaySize(28);
			}
			
			propFormItems.put(userPropertyHandler.getName(), fi);
		}

		//others
		FormLayoutContainer rightContainer = FormLayoutContainer.createDefaultFormLayout("right_1", getTranslator());
		rightContainer.setRootForm(mainForm);
		formLayout.add(rightContainer);
		
		//roles
		String[] roleValues = new String[roleKeys.length];
		for(int i=roleKeys.length; i-->0; ) {
			roleValues[i] = translate("search." + roleKeys[i]);
		}
		rolesEl = uifactory.addCheckboxesHorizontal("roles", "search.roles", rightContainer, roleKeys, roleValues);
		for(String roleKey: roleKeys) {
			rolesEl.select(roleKey, true);
		}

		String[] openValues = new String[originKeys.length];
		for(int i=originKeys.length; i-->0; ) {
			openValues[i] = translate("search." + originKeys[i]);
		}
		originEl = uifactory.addRadiosHorizontal("openBg", "search.origin", rightContainer, originKeys, openValues);
		originEl.select("all", true);

		FormLayoutContainer buttonLayout = FormLayoutContainer.createDefaultFormLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("search", "search", buttonLayout);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		SearchMembersParams params = new SearchMembersParams();
		//roles
		Collection<String> selectedKeys = rolesEl.getSelectedKeys();
		params.setRepoOwners(selectedKeys.contains("owner"));
		params.setRepoTutors(selectedKeys.contains("tutor"));
		params.setGroupTutors(selectedKeys.contains("tutor"));
		params.setRepoParticipants(selectedKeys.contains("attendee"));
		params.setGroupParticipants(selectedKeys.contains("attendee"));
		params.setGroupWaitingList(selectedKeys.contains("waiting"));

		//origin
		if(!originEl.isOneSelected() || originEl.isSelected(0)) {
			params.setRepoOrigin(true);
			params.setGroupOrigin(true);
		} else if(originEl.isSelected(1)) {
			params.setRepoOrigin(true);
			params.setGroupOrigin(false);
		} else if(originEl.isSelected(2)) {
			params.setRepoOrigin(false);
			params.setGroupOrigin(true);
		}
		
		String loginVal = login.getValue();
		if(StringHelper.containsNonWhitespace(loginVal)) {
			params.setLogin(loginVal);
		}
		
		//user properties
		Map<String, String> userPropertiesSearch = new HashMap<String, String>();				
		for (UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) continue;
			FormItem ui = propFormItems.get(userPropertyHandler.getName());
			String uiValue = userPropertyHandler.getStringValue(ui);
			if (StringHelper.containsNonWhitespace(uiValue)) {
				userPropertiesSearch.put(userPropertyHandler.getName(), uiValue);
			}
		}
		if(!userPropertiesSearch.isEmpty()) {
			params.setUserPropertiesSearch(userPropertiesSearch);
		}

		fireEvent(ureq, params);
	}
}