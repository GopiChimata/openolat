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
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.gui.control;

import org.olat.admin.layout.FooterInformations;
import org.olat.admin.layout.LayoutModule;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.controllers.impressum.ImpressumInformations;
import org.olat.core.commons.controllers.impressum.ImpressumMainController;
import org.olat.core.commons.controllers.impressum.ImpressumModule;
import org.olat.core.commons.fullWebApp.BaseFullWebappController;
import org.olat.core.commons.fullWebApp.LockableController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.util.UserLoggedInCounter;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.creator.ControllerCreator;
import org.olat.core.gui.control.generic.popup.PopupBrowserWindow;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.social.SocialModule;
import org.olat.social.shareLink.ShareLinkController;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Overrides the default footer of the webapplication framework showing the 
 * <ul>
 * <li>connected users</li>
 * <li>username</li>
 * <li>powered by</li>
 * </ul>
 * <P>
 * Initial Date: 16.06.2006 <br>
 * 
 * @author patrickb
 */
public class OlatFooterController extends BasicController implements LockableController { 
	
	private final Link impressumLink;
	private final VelocityContainer olatFootervc;
	private ShareLinkController shareLinkCtr;
	
	@Autowired
	private SocialModule socialModule;
	@Autowired
	private LayoutModule layoutModule;
	@Autowired
	private ImpressumModule impressumModule;

	public OlatFooterController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(BaseFullWebappController.class, getLocale(), Util.createPackageTranslator(OlatFooterController.class,getLocale())));

		olatFootervc = createVelocityContainer("olatFooter");

		Identity identity = ureq.getIdentity();
		boolean isGuest = (identity == null ? true : ureq.getUserSession().getRoles().isGuestOnly());
		boolean isInvitee = (identity == null ? false : ureq.getUserSession().getRoles().isInvitee());
		
		// Show user count
		UserLoggedInCounter userCounter = new UserLoggedInCounter();
		olatFootervc.put("userCounter", userCounter);

		// share links
		if (socialModule.isShareEnabled() && socialModule.getEnabledShareLinkButtons().size() > 0) {
			shareLinkCtr = new ShareLinkController(ureq, wControl);
			listenTo(shareLinkCtr);
			olatFootervc.put("shareLink", shareLinkCtr.getInitialComponent());
		}
		
		olatFootervc.contextPut("impressumInfos", new ImpressumInformations(impressumModule));
		impressumLink = LinkFactory.createLink("topnav.impressum", olatFootervc, this);
		impressumLink.setTooltip("topnav.impressum.alt");
		impressumLink.setIconLeftCSS("o_icon o_icon_impress o_icon-lg");
		impressumLink.setAjaxEnabled(false);
		impressumLink.setTarget("_blank");

		// Push information about user
		if (!isGuest && ureq.getUserSession().isAuthenticated()) {
			olatFootervc.contextPut("loggedIn", Boolean.TRUE);
			if(isInvitee) {
				olatFootervc.contextPut("username", translate("logged.in.invitee"));
			} else {
				String fullName = CoreSpringFactory.getImpl(UserManager.class).getUserDisplayName(ureq.getIdentity());
				olatFootervc.contextPut("username", StringHelper.escapeHtml(fullName));
			}
		} else {
			olatFootervc.contextPut("loggedIn", Boolean.FALSE);
		}

		olatFootervc.contextPut("appName", Settings.getApplicationName());
		olatFootervc.contextPut("appVersion", Settings.getVersion());
		
		olatFootervc.contextPut("buildIdentifier", Settings.getBuildIdentifier());
		olatFootervc.contextPut("revisionNumber", WebappHelper.getRevisionNumber());
		olatFootervc.contextPut("changeSet", WebappHelper.getChangeSet());
		olatFootervc.contextPut("olatversion", Settings.getFullVersionInfo() +" "+ Settings.getNodeInfo());
		olatFootervc.contextPut("footerInfos", new FooterInformations(layoutModule));

		putInitialPanel(olatFootervc);
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void lockResource(OLATResourceable resource) {
		if(shareLinkCtr != null) {
			olatFootervc.remove(shareLinkCtr.getInitialComponent());
		}
	}

	@Override
	public void unlockResource() {
		if(shareLinkCtr != null) {
			olatFootervc.put("shareLink", shareLinkCtr.getInitialComponent());
		}
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(impressumLink == source) {
			doOpenImpressum(ureq);
		}
	}
	
	protected void doOpenImpressum(UserRequest ureq) {
		ControllerCreator impressumControllerCreator = new ControllerCreator() {
			@Override
			public Controller createController(UserRequest lureq, WindowControl lwControl) {
				return new ImpressumMainController(lureq, lwControl);
			}
		};
		PopupBrowserWindow popupBrowserWindow;
		if(ureq.getUserSession().isAuthenticated()) {
			popupBrowserWindow = Windows.getWindows(ureq).getWindowManager().createNewPopupBrowserWindowFor(ureq, impressumControllerCreator);
		} else {
			popupBrowserWindow = Windows.getWindows(ureq).getWindowManager().createNewUnauthenticatedPopupWindowFor(ureq, impressumControllerCreator);
		}
		popupBrowserWindow.open(ureq);
	}
}
