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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.DateChooser;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.util.StringHelper;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACUIFactory;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.manager.ACFrontendManager;
import org.olat.resource.accesscontrol.method.AccessMethodHandler;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.Offer;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.model.OfferImpl;

/**
 * 
 * Description:<br>
 * 
 * 
 * <P>
 * Initial Date:  14 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class AccessConfigurationController extends FormBasicController {

	private List<Link> addMethods = new ArrayList<Link>();
	private final String displayName;
	private final OLATResource resource;
	private final AccessControlModule acModule;
	private final ACFrontendManager acFrontendManager;
	
	private FormLink createLink;
	private FormLayoutContainer confControllerContainer;
	private CloseableCalloutWindowController createCalloutCtrl;
	private CloseableModalController cmc;
	private AbstractConfigurationMethodController newMethodCtrl;
	
	private final List<AccessInfo> confControllers = new ArrayList<AccessInfo>();
	
	private final boolean embbed;
	
	public AccessConfigurationController(UserRequest ureq, WindowControl wControl, OLATResource resource, String displayName) {
		super(ureq, wControl);
		
		this.resource = resource;
		this.displayName = displayName;
		acModule = (AccessControlModule)CoreSpringFactory.getBean("acModule");
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
		embbed = false;
		
		initForm(ureq);
	}
		
	public AccessConfigurationController(UserRequest ureq, WindowControl wControl, OLATResource resource, String displayName, Form form) {
		super(ureq, wControl, FormBasicController.LAYOUT_CUSTOM, "access_configuration", form);
		
		this.resource = resource;
		this.displayName = displayName;
		acModule = (AccessControlModule)CoreSpringFactory.getBean("acModule");
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
		embbed = true;
		
		initForm(ureq);
	}
	
	public FormItem getInitialFormItem() {
		return flc;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		createLink = uifactory.addFormLink("add.accesscontrol", formLayout, Link.BUTTON);
		
		String confPage = velocity_root + "/configuration_list.html";
		confControllerContainer = FormLayoutContainer.createCustomFormLayout("conf-controllers", getTranslator(), confPage);
		confControllerContainer.setRootForm(mainForm);
		formLayout.add(confControllerContainer);
		
		loadConfigurations();
		
		confControllerContainer.contextPut("confControllers", confControllers);
		
		if(!embbed) {
			final FormLayoutContainer buttonGroupLayout = FormLayoutContainer.createButtonLayout("buttonLayout", getTranslator());
			buttonGroupLayout.setRootForm(mainForm);
			formLayout.add(buttonGroupLayout);
			
			uifactory.addFormSubmitButton("save", formLayout);
		}
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(addMethods.contains(source)) {
			AccessMethod method = (AccessMethod)((Link)source).getUserObject();
			addMethod(ureq, method);
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(newMethodCtrl == source) {
			if(event.equals(Event.DONE_EVENT)) {
				OfferAccess newLink = newMethodCtrl.commitChanges();
				newLink = acFrontendManager.saveOfferAccess(newLink);
				addConfiguration(newLink);
			}
			cmc.deactivate();
			removeAsListenerAndDispose(newMethodCtrl);
			removeAsListenerAndDispose(cmc);
			newMethodCtrl = null;
			cmc = null;
		} else if (cmc == source) {
			removeAsListenerAndDispose(newMethodCtrl);
			removeAsListenerAndDispose(cmc);
			newMethodCtrl = null;
			cmc = null;
		} else {
			super.event(ureq, source, event);
		}
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(addMethods.contains(source)) {
			AccessMethod method = (AccessMethod)source.getUserObject();
			addMethod(ureq, method);
		} else if (source == createLink) {
			popupCallout(ureq);
		} else if (source.getName().startsWith("del_")) {
			AccessInfo infos = (AccessInfo)source.getUserObject();
			acFrontendManager.deleteOffer(infos.getLink().getOffer());
			confControllers.remove(infos);
		}
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	public void formOK(UserRequest ureq) {
		Map<String,FormItem> formItemMap = confControllerContainer.getFormComponents();
		
		List<OfferAccess> links = new ArrayList<OfferAccess>();
		for(AccessInfo info:confControllers) {
			FormItem dateFrom = formItemMap.get("from_" + info.getLink().getKey());
			if(dateFrom instanceof DateChooser) {
				Date from = ((DateChooser)dateFrom).getDate();
				info.getLink().setValidFrom(from);
				info.getLink().getOffer().setValidFrom(from);
			}
			
			FormItem dateTo = formItemMap.get("to_" + info.getLink().getKey());
			if(dateTo instanceof DateChooser) {
				Date to = ((DateChooser)dateTo).getDate();
				info.getLink().setValidTo(to);
				info.getLink().getOffer().setValidTo(to);
			}
			
			links.add(info.getLink());
		}
		acFrontendManager.saveOfferAccess(links);
	}
	
	protected void popupCallout(UserRequest ureq) {
		addMethods.clear();
		
		VelocityContainer mapCreateVC = createVelocityContainer("createAccessCallout");
		List<AccessMethod> methods = acFrontendManager.getAvailableMethods(getIdentity());
		for(AccessMethod method:methods) {
			AccessMethodHandler handler = acModule.getAccessMethodHandler(method.getType());
			Link add = LinkFactory.createLink("create." + handler.getType(), mapCreateVC, this);
			add.setCustomDisplayText(handler.getMethodName(getLocale()));
			add.setUserObject(method);
			addMethods.add(add);
			mapCreateVC.put(add.getComponentName(), add);
		}
		mapCreateVC.contextPut("methods", addMethods);
		
		String title = translate("add.accesscontrol");
		removeAsListenerAndDispose(createCalloutCtrl);
		createCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(), mapCreateVC, createLink, title, true, null);
		listenTo(createCalloutCtrl);
		createCalloutCtrl.activate();
		mainForm.setDirtyMarking(false);
	}
	
	protected void loadConfigurations() {
		List<Offer> offers = acFrontendManager.findOfferByResource(resource, true, null);
		for(Offer offer:offers) {
			List<OfferAccess> offerAccess = acFrontendManager.getOfferAccess(offer, true);
			for(OfferAccess access:offerAccess) {
				addConfiguration(access);
			}
		}
	}
	
	protected void addConfiguration(OfferAccess link) {
		AccessMethodHandler handler = acModule.getAccessMethodHandler(link.getMethod().getType());
		AccessInfo infos = new AccessInfo(handler.getMethodName(getLocale()), null, link);
		confControllers.add(infos);

		DateChooser dateFrom = uifactory.addDateChooser("from_" + link.getKey(), "from", "", confControllerContainer);
		dateFrom.setUserObject(infos);
		dateFrom.setDate(link.getValidFrom());
		confControllerContainer.add(dateFrom.getName(), dateFrom);
		
		DateChooser dateTo = uifactory.addDateChooser("to_" + link.getKey(), "to", "", confControllerContainer);
		dateTo.setUserObject(infos);
		dateTo.setDate(link.getValidTo());
		confControllerContainer.add(dateTo.getName(), dateTo);
		
		FormLink delLink = uifactory.addFormLink("del_" + link.getKey(), "delete", null, confControllerContainer, Link.LINK);
		delLink.setUserObject(infos);
		delLink.setCustomEnabledLinkCSS("b_with_small_icon_left b_delete_icon");
		confControllerContainer.add(delLink.getName(), delLink);
	}
	
	protected void addMethod(UserRequest ureq, AccessMethod method) {
		createCalloutCtrl.deactivate();
		
		Offer offer = acFrontendManager.createOffer(resource, displayName);
		OfferAccess link = acFrontendManager.createOfferAccess(offer, method);
		
		removeAsListenerAndDispose(newMethodCtrl);
		newMethodCtrl = ACUIFactory.createAccessConfigurationController(ureq, getWindowControl(), link);
		if(newMethodCtrl != null) {
			listenTo(newMethodCtrl);

			AccessMethodHandler handler = acModule.getAccessMethodHandler(method.getType());
			String title = handler.getMethodName(getLocale());
		
			cmc = new CloseableModalController(getWindowControl(), translate("close"), newMethodCtrl.getInitialComponent(), true, title);
			cmc.activate();
			listenTo(cmc);
		} else {
			OfferAccess newLink = acFrontendManager.saveOfferAccess(link);
			addConfiguration(newLink);
		}
	}
	
	public class AccessInfo {
		private String name;
		private String infos;
		private OfferAccess link;
		
		public AccessInfo(String name, String infos, OfferAccess link) {
			this.name = name;
			this.infos = infos;
			this.link = link;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getInfos() {
			if(infos == null && link.getOffer() != null) {
				OfferImpl casted = (OfferImpl)link.getOffer();
				if(StringHelper.containsNonWhitespace(casted.getToken())) {
					return casted.getToken();
				}
			}
			if(StringHelper.containsNonWhitespace(infos)) {
				return infos;
			}
			return "";
		}
		
		public void setInfos(String infos) {
			this.infos = infos;
		}

		public OfferAccess getLink() {
			return link;
		}

		public void setLink(OfferAccess link) {
			this.link = link;
		}
	}
}