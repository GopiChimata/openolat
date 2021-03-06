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

package org.olat.group.ui.edit;

import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.components.tabbedpane.TabbedPaneChangedEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.activity.ActionType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupService;
import org.olat.group.GroupLoggingAction;
import org.olat.group.ui.BGControllerFactory;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description: <BR>
 * This controller displays a tabbed pane that lets the user configure and
 * modify a business group.
 * <P>
 * Fires BusinessGroupModifiedEvent via the OLATResourceableEventCenter
 * <P>
 * Initial Date: Aug 17, 2004
 * 
 * @author patrick, srosse
 */
public class BusinessGroupEditController extends BasicController implements ControllerEventListener, GenericEventListener, Activateable2 {

	private boolean hasResources;
	private BusinessGroup currBusinessGroup;
	@Autowired
	private BusinessGroupService businessGroupService;
	@Autowired
	private AccessControlModule acModule;

	private TabbedPane tabbedPane;
	private VelocityContainer mainVC;

	private LockResult lockEntry;
	private DialogBoxController alreadyLockedDialogController;

	//controllers in tabs
	private BusinessGroupEditDetailsController editDetailsController;
	private BusinessGroupToolsController collaborationToolsController;
	private BusinessGroupMembersController membersController;
	private BusinessGroupEditResourceController resourceController;
	private BusinessGroupEditAccessController tabAccessCtrl;
	
	private int membersTab;

	/**
	 * Never call this constructor directly, use the BGControllerFactory instead!!
	 * 
	 * @param ureq
	 * @param wControl
	 * @param currBusinessGroup
	 * @param configurationFlags Flags to configure the controllers features. The
	 *          controller does no type specific stuff implicit just by looking at
	 *          the group type. Type specifig features must be flagged.
	 */
	public BusinessGroupEditController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup) {
		super(ureq, wControl);
		
		// OLAT-4955: setting the stickyActionType here passes it on to any controller defined in the scope of the editor,
		//            basically forcing any logging action called within the bg editor to be of type 'admin'
		getUserActivityLogger().setStickyActionType(ActionType.admin);
		addLoggingResourceable(LoggingResourceable.wrap(businessGroup));

		// Initialize translator:
		setTranslator(Util.createPackageTranslator(BGControllerFactory.class, getLocale(), getTranslator()));

		// try to acquire edit lock on business group
		String locksubkey = "groupEdit";
		lockEntry = CoordinatorManager.getInstance().getCoordinator().getLocker().acquireLock(businessGroup, ureq.getIdentity(), locksubkey);
		if (lockEntry.isSuccess()) {
			// reload group to minimize stale object exception and update last usage timestamp
			currBusinessGroup = businessGroupService.setLastUsageFor(getIdentity(), businessGroup);
			if(currBusinessGroup == null) {
				VelocityContainer vc = createVelocityContainer("deleted");
				vc.contextPut("name", businessGroup.getName());
				putInitialPanel(vc);
			} else {
				// add as listener to BusinessGroup so we are being notified about changes.
				CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, ureq.getIdentity(), currBusinessGroup);
	
				//create some controllers
				editDetailsController = new BusinessGroupEditDetailsController(ureq, getWindowControl(), businessGroup);
				listenTo(editDetailsController);
				
				collaborationToolsController = new BusinessGroupToolsController(ureq, getWindowControl(), businessGroup);
				listenTo(collaborationToolsController);
				
				membersController = new BusinessGroupMembersController(ureq, getWindowControl(), businessGroup);
				listenTo(membersController);
				
				//fxdiff VCRP-1,2: access control of resources
				tabbedPane = new TabbedPane("bgTabbs", ureq.getLocale());
				tabbedPane.addListener(this);
				setAllTabs(ureq);
				mainVC = createVelocityContainer("edit");
				mainVC.put("tabbedpane", tabbedPane);
				String[] title = new String[] { StringEscapeUtils.escapeHtml(currBusinessGroup.getName()) };
				mainVC.contextPut("title", getTranslator().translate("group.edit.title", title));
				putInitialPanel(mainVC);
			}
		} else {
			//lock was not successful !
			alreadyLockedDialogController = DialogBoxUIFactory.createResourceLockedMessage(ureq, wControl, lockEntry, "error.message.locked", getTranslator());
			listenTo(alreadyLockedDialogController);
			alreadyLockedDialogController.activate();
			putInitialPanel(new Panel("empty"));
		}
	}
	
	/**
	 * Learning areas and and course rights should only appear when at least one course is associated.</br>
	 * <ul><li>
	 * a) No courses associated and user is not author</br>
	 * Description, Tools, Members, Publishing and booking
	 * </li><li>
	 * b) No course associated and user is author:</br>
	 * Description, Tools, Members, Courses, Publishing and booking
	 * </li><li>
	 * c) With courses associated:</br>
	 * Description, Tools, Members, Courses, Learning areas, Course rights, Publishing and booking 
	 * 
	 * @param ureq
	 */
	private void setAllTabs(UserRequest ureq) {
		hasResources = businessGroupService.hasResources(currBusinessGroup);
		
		tabAccessCtrl = getAccessController(ureq);
		
		int currentSelectedPane = tabbedPane.getSelectedPane();

		tabbedPane.removeAll();
		editDetailsController.setAllowWaitingList(tabAccessCtrl == null || !tabAccessCtrl.isPaymentMethodInUse());
		tabbedPane.addTab(translate("group.edit.tab.details"), editDetailsController.getInitialComponent());
		tabbedPane.addTab(translate("group.edit.tab.collabtools"), collaborationToolsController.getInitialComponent());
		
		membersController.updateBusinessGroup(currBusinessGroup);
		membersTab = tabbedPane.addTab(translate("group.edit.tab.members"), membersController.getInitialComponent());
		//resources (optional)
		resourceController = getResourceController(ureq);
		if(resourceController != null) {
			tabbedPane.addTab(translate("group.edit.tab.resources"), resourceController.getInitialComponent());
		}
		
		if(tabAccessCtrl != null) {
			tabbedPane.addTab(translate("group.edit.tab.accesscontrol"), tabAccessCtrl.getInitialComponent());
		}
		
		if(currentSelectedPane > 0) {
			tabbedPane.setSelectedPane(currentSelectedPane);
		}
	}
	
	/**
	 * The resources / courses tab is enabled if the user is
	 * an administrator, a group manager or an author. Or if the group has
	 * already some resources.
	 * 
	 * @param ureq
	 * @return
	 */
	private BusinessGroupEditResourceController getResourceController(UserRequest ureq) {
		Roles roles = ureq.getUserSession().getRoles();
		boolean enabled = roles.isOLATAdmin() || roles.isGroupManager() || roles.isAuthor() || hasResources;
		if(enabled) {
			if(resourceController == null) {
				resourceController = new BusinessGroupEditResourceController(ureq, getWindowControl(), currBusinessGroup);
				listenTo(resourceController);
			}
			return resourceController;
		}
		removeAsListenerAndDispose(resourceController);
		resourceController = null;
		return null;
	}
	
	private BusinessGroupEditAccessController getAccessController(UserRequest ureq) {
		if(tabAccessCtrl == null && acModule.isEnabled()) { 
			tabAccessCtrl = new BusinessGroupEditAccessController(ureq, getWindowControl(), currBusinessGroup);
			if(BusinessGroupManagedFlag.isManaged(currBusinessGroup, BusinessGroupManagedFlag.bookings)
					&& tabAccessCtrl.getNumOfBookingConfigurations() == 0) {
				//booking is managed, no booking, don't show it
				tabAccessCtrl = null;
			} else {
				listenTo(tabAccessCtrl);
			}
		}
		if(tabAccessCtrl != null) {
			tabAccessCtrl.updateBusinessGroup(currBusinessGroup);
		}
		return tabAccessCtrl;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		 if (source == tabbedPane && event instanceof TabbedPaneChangedEvent) {
			tabbedPane.addToHistory(ureq, getWindowControl());
			if(tabbedPane.getSelectedPane() == membersTab) {
				membersController.updateBusinessGroup(currBusinessGroup);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == collaborationToolsController) {
			if (event == Event.CHANGED_EVENT) {
				// notify current active users of this business group
				BusinessGroupModifiedEvent
						.fireModifiedGroupEvents(BusinessGroupModifiedEvent.CONFIGURATION_MODIFIED_EVENT, currBusinessGroup, null);
				fireEvent(ureq, event);
			}
		} else if (source == alreadyLockedDialogController) {
			//closed dialog box either by clicking ok, or closing the box
			if (event == Event.CANCELLED_EVENT || DialogBoxUIFactory.isOkEvent(event)) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		} else if (source == editDetailsController) {
			if (event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				//reload the business group
				currBusinessGroup = editDetailsController.getGroup();
				// inform index about change
				setAllTabs(ureq);
				// notify current active users of this business group
				BusinessGroupModifiedEvent
						.fireModifiedGroupEvents(BusinessGroupModifiedEvent.CONFIGURATION_MODIFIED_EVENT, currBusinessGroup, null);
				// do logging
				ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_CONFIGURATION_CHANGED, getClass());
			}
		} else if (source == membersController) {
			if (event == Event.DONE_EVENT || event == Event.CHANGED_EVENT) {
				//reload the business group
				currBusinessGroup = membersController.getGroup();
				fireEvent(ureq, event);
			}
		} else if (source == tabAccessCtrl) {
			setAllTabs(ureq);
			fireEvent(ureq, event);
		} else if (source == resourceController) {
			setAllTabs(ureq);
			fireEvent(ureq, event);
		}
	}

	/**
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	public void event(Event event) {
		if (event instanceof OLATResourceableJustBeforeDeletedEvent) {
			OLATResourceableJustBeforeDeletedEvent delEvent = (OLATResourceableJustBeforeDeletedEvent) event;
			if (!delEvent.targetEquals(currBusinessGroup)) throw new AssertException(
					"receiving a delete event for a olatres we never registered for!!!:" + delEvent.getDerivedOres());
			dispose();
		} 
	}
	
	@Override
	//fxdiff BAKS-7 Resume function
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty() || tabbedPane == null) return;
		tabbedPane.activate(ureq, entries, state);
	}

	/**
	 * @return true if lock on group has been acquired, flase otherwhise
	 */
	public boolean isLockAcquired() {
		return lockEntry.isSuccess();
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean asynchronous)
	 */
	@Override
	protected void doDispose() {
		if(currBusinessGroup != null) {
			CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, currBusinessGroup);
			//release lock on dispose
			releaseBusinessGroupEditLock();
		}
	}

	private void releaseBusinessGroupEditLock() {
		if(lockEntry.isSuccess()){
			// release lock
			CoordinatorManager.getInstance().getCoordinator().getLocker().releaseLock(lockEntry);
		}else if(alreadyLockedDialogController != null){
			//dispose if dialog still visible
			alreadyLockedDialogController.dispose();
		}
	}
}