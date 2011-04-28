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
* Copyright (c) 2008 frentix GmbH, Switzerland<br>
* <p>
*/

package org.olat.resource.accesscontrol.ui;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.manager.ACFrontendManager;
import org.olat.resource.accesscontrol.model.OfferAccess;


/**
 * 
 * Description:<br>
 * Ask for the token
 * 
 * <P>
 * Initial Date:  15 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class TokenAccessController extends FormBasicController implements FormController {
	
	private TextElement tokenEl;
	private final OfferAccess link;
	private final ACFrontendManager acFrontendManager;

	public TokenAccessController(UserRequest ureq, WindowControl wControl, OfferAccess link) {
		super(ureq, wControl);
		
		this.link = link;
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
			
		initForm(ureq);
	}
	
	public TokenAccessController(UserRequest ureq, WindowControl wControl, OfferAccess link, Form form) {
		super(ureq, wControl, LAYOUT_DEFAULT, null, form);
		
		this.link = link;
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
			
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("access.token.title");
		setFormDescription("access.token.desc");
			
		tokenEl = uifactory.addTextElement("token", "accesscontrol.token", 255, "", formLayout);
			
		final FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
		buttonGroupLayout.setRootForm(mainForm);
		formLayout.add(buttonGroupLayout);
			
		uifactory.addFormSubmitButton("access.button", formLayout);
	}
		
	@Override
	protected void doDispose() {
			//
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		String token = tokenEl.getValue();
		tokenEl.clearError();
		if(token == null || token.length() < 2) {
			tokenEl.setErrorKey("invalid.token.format", null);
			allOk = false;
		}
		
		return allOk && super.validateFormLogic(ureq);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		String token = tokenEl.getValue();
		AccessResult result = acFrontendManager.accessResource(getIdentity(), link, token);
		
		if(result.isAccessible()) {
			fireEvent(ureq, AccessEvent.ACCESS_OK_EVENT);
		} else {
			String msg = translate("invalid.token");
			fireEvent(ureq, new AccessEvent(AccessEvent.ACCESS_FAILED, msg));
		}
	}

	@Override
	public FormItem getInitialFormItem() {
		return flc;
	}
}