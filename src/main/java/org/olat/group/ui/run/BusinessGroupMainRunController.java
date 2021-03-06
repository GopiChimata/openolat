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

package org.olat.group.ui.run;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.NewControllerFactory;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityModule;
import org.olat.basesecurity.Group;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.ui.GroupController;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.commons.calendar.CalendarModule;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.tree.GenericTreeModel;
import org.olat.core.gui.components.tree.GenericTreeNode;
import org.olat.core.gui.components.tree.MenuTree;
import org.olat.core.gui.components.tree.TreeModel;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.MainLayoutBasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.messages.MessageUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.activity.OlatResourceableType;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.nodes.iq.AssessmentEvent;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupMembership;
import org.olat.group.BusinessGroupService;
import org.olat.group.GroupLoggingAction;
import org.olat.group.ui.BGControllerFactory;
import org.olat.group.ui.edit.BusinessGroupEditController;
import org.olat.group.ui.edit.BusinessGroupModifiedEvent;
import org.olat.instantMessaging.CloseInstantMessagingEvent;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.InstantMessagingService;
import org.olat.modules.co.ContactFormController;
import org.olat.modules.openmeetings.OpenMeetingsModule;
import org.olat.modules.wiki.WikiManager;
import org.olat.portfolio.PortfolioModule;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.repository.ui.RepositoryTableModel;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.AccessResult;
import org.olat.resource.accesscontrol.ui.AccessEvent;
import org.olat.resource.accesscontrol.ui.AccessListController;
import org.olat.resource.accesscontrol.ui.OrdersAdminController;
import org.olat.util.logging.activity.LoggingResourceable;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Description: <BR>
 * Runtime environment for a business group. Use the BGControllerFactory and not
 * the constructor to create an instance of this controller.
 * <P>
 * 
 * @version Initial Date: Aug 11, 2004
 * @author patrick
 */

public class BusinessGroupMainRunController extends MainLayoutBasicController implements GenericEventListener, Activateable2 {

	private static final String INITVIEW_TOOLFOLDER = "toolfolder";
	public static final OLATResourceable ORES_TOOLFOLDER = OresHelper.createOLATResourceableType(INITVIEW_TOOLFOLDER);	
	
	private static final String INITVIEW_TOOLFORUM = "toolforum";
	public static final OLATResourceable ORES_TOOLFORUM = OresHelper.createOLATResourceableType(INITVIEW_TOOLFORUM);
	
	private static final String INITVIEW_TOOLWIKI = WikiManager.WIKI_RESOURCE_FOLDER_NAME;
	public static final OLATResourceable ORES_TOOLWIKI = OresHelper.createOLATResourceableType(INITVIEW_TOOLWIKI);

	private static final String INITVIEW_TOOLPORTFOLIO = "toolportfolio";
	public static final OLATResourceable ORES_TOOLPORTFOLIO = OresHelper.createOLATResourceableType(INITVIEW_TOOLPORTFOLIO);
	private static final String INITVIEW_TOOLOPENMEETINGS = "toolportfolio";
	public static final OLATResourceable ORES_TOOLOPENMEETINGS = OresHelper.createOLATResourceableType(INITVIEW_TOOLOPENMEETINGS);
	
	public static final String INITVIEW_TOOLCAL = "action.calendar.group";
	public static final OLATResourceable ORES_TOOLCAL = OresHelper.createOLATResourceableType(INITVIEW_TOOLCAL);
	//fxdiff BAKS-7 Resume function
	public static final OLATResourceable ORES_TOOLMSG = OresHelper.createOLATResourceableType("toolmsg");
	public static final OLATResourceable ORES_TOOLADMIN = OresHelper.createOLATResourceableType("tooladmin");
	public static final OLATResourceable ORES_TOOLCONTACT = OresHelper.createOLATResourceableType("toolcontact");
	public static final OLATResourceable ORES_TOOLMEMBERS = OresHelper.createOLATResourceableType("toolmembers");
	public static final OLATResourceable ORES_TOOLRESOURCES = OresHelper.createOLATResourceableType("toolresources");

	// activity identifyers are used as menu user objects and for the user
	// activity events
	// change value with care, used in logfiles etc!!
	/** activity identitfyer: user selected overview in menu * */
	public static final String ACTIVITY_MENUSELECT_OVERVIEW = "MENU_OVERVIEW";
	/** activity identitfyer: user selected information in menu * */
	public static final String ACTIVITY_MENUSELECT_INFORMATION = "MENU_INFORMATION";
	/** activity identitfyer: user selected memberlist in menu * */
	public static final String ACTIVITY_MENUSELECT_MEMBERSLIST = "MENU_MEMBERLIST";
	/** activity identitfyer: user selected contactform in menu * */
	public static final String ACTIVITY_MENUSELECT_CONTACTFORM = "MENU_CONTACTFORM";
	/** activity identitfyer: user selected forum in menu * */
	public static final String ACTIVITY_MENUSELECT_FORUM = "MENU_FORUM";
	/** activity identitfyer: user selected folder in menu * */
	public static final String ACTIVITY_MENUSELECT_FOLDER = "MENU_FOLDER";
	/** activity identitfyer: user selected chat in menu * */
	public static final String ACTIVITY_MENUSELECT_CHAT = "MENU_CHAT";
	/** activity identitfyer: user selected calendar in menu * */
	public static final String ACTIVITY_MENUSELECT_CALENDAR = "MENU_CALENDAR";
	/** activity identitfyer: user selected administration in menu * */
	public static final String ACTIVITY_MENUSELECT_ADMINISTRATION = "MENU_ADMINISTRATION";
	/** activity identitfyer: user selected show resources in menu * */
	public static final String ACTIVITY_MENUSELECT_SHOW_RESOURCES = "MENU_SHOW_RESOURCES";
	public static final String ACTIVITY_MENUSELECT_WIKI = "MENU_SHOW_WIKI";
	/* activity identitfyer: user selected show portoflio in menu */
	public static final String ACTIVITY_MENUSELECT_PORTFOLIO = "MENU_SHOW_PORTFOLIO";
	/* activity identifyer: user selected show OPENMEETINGS in menu */
	public static final String ACTIVITY_MENUSELECT_OPENMEETINGS = "MENU_SHOW_OPENMEETINGS";
	/* activity identitfyer: user selected show access control in menu */
	//fxdiff VCRP-1,2: access control of resources
	public static final String ACTIVITY_MENUSELECT_AC = "MENU_SHOW_AC";

	private Panel mainPanel;
	private VelocityContainer main, vc_sendToChooserForm, resourcesVC;
	private Translator resourceTrans;

	private BusinessGroup businessGroup;

	private MenuTree bgTree;
	private LayoutMain3ColsController columnLayoutCtr;

	private Controller collabToolCtr;
	
	private BusinessGroupEditController bgEditCntrllr;
	//fxdiff VCRP-1,2: access control of resources
	private Controller bgACHistoryCtrl;
	private TableController resourcesCtr;

	private BusinessGroupSendToChooserForm sendToChooserForm;
	
	private GroupController gownersC;
	private GroupController gparticipantsC;
	private GroupController waitingListController;

	private boolean isAdmin;

	@Autowired
	private ACService acService;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private CalendarModule calendarModule;
	@Autowired
	private BusinessGroupService businessGroupService;
	private EventBus singleUserEventBus;
	private String adminNodeId; // reference to admin menu item

	// not null indicates tool is enabled
	private GenericTreeNode nodeFolder, nodeForum, nodeWiki, nodeCal, nodePortfolio, nodeOpenMeetings;
	private GenericTreeNode nodeContact, nodeGroupOwners, nodeResources, nodeInformation, nodeAdmin;
	private boolean groupRunDisabled;
	private OLATResourceable assessmentEventOres;
	private Controller accessController;
	
	private boolean needActivation;
	private boolean chatAvailable;

	/**
	 * Do not use this constructor! Use the BGControllerFactory instead!
	 *
	 * @param ureq
	 * @param control
	 * @param currBusinessGroup
	 * @param flags
	 * @param initialViewIdentifier supported are null, "toolforum", "toolfolder"
	 */
	public BusinessGroupMainRunController(UserRequest ureq, WindowControl control, BusinessGroup bGroup) {
		super(ureq, control);
		
		assessmentEventOres = OresHelper.createOLATResourceableType(AssessmentEvent.class);

		/*
		 * lastUsage, update lastUsage if group is run if you can acquire the lock
		 * on the group for a very short time. If this is not possible, then the
		 * lastUsage is already up to date within one-day-precision.
		 */
		businessGroup = businessGroupService.setLastUsageFor(getIdentity(), bGroup);
		if(businessGroup == null) {
			VelocityContainer vc = createVelocityContainer("deleted");
			vc.contextPut("name", bGroup.getName());
			columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, vc, "grouprun");
			listenTo(columnLayoutCtr); // cleanup on dispose
			putInitialPanel(columnLayoutCtr.getInitialComponent());
			chatAvailable = false;
			return;
		}

		List<BusinessGroupMembership> memberships = businessGroupService.getBusinessGroupMembership(Collections.singletonList(bGroup.getKey()), getIdentity());
		if(isOnWaitinglist(memberships)) {
			putInitialPanel(getOnWaitingListMessage(ureq, bGroup));
			chatAvailable = false;
			return;
		} else if(ureq.getUserSession().getRoles().isGuestOnly()) {
			//not a member
			putInitialPanel(getNoAccessMessage(ureq, bGroup));
			chatAvailable = false;
			return;
		}

		addLoggingResourceable(LoggingResourceable.wrap(businessGroup));
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_OPEN, getClass());
		
		chatAvailable = isChatAvailable();
		isAdmin = ureq.getUserSession().getRoles().isOLATAdmin()
				|| ureq.getUserSession().getRoles().isGroupManager()
				|| businessGroupService.isIdentityInBusinessGroup(getIdentity(), businessGroup.getKey(), true, false, null);

		// Initialize translator:
		// package translator with default group fallback translators and type
		// translator
		setTranslator(Util.createPackageTranslator(BGControllerFactory.class, getLocale(), getTranslator()));
		resourceTrans = Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator());

		// main component layed out in panel
		main = createVelocityContainer("bgrun");
		exposeGroupDetailsToVC(businessGroup);

		mainPanel = new Panel("p_buddygroupRun");
		mainPanel.setContent(main);
		//
		bgTree = new MenuTree("bgTree");
		TreeModel trMdl = buildTreeModel();
		bgTree.setTreeModel(trMdl);
		bgTree.addListener(this);
		//
		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), bgTree, mainPanel, "grouprun");
		listenTo(columnLayoutCtr); // cleanup on dispose
		
		//
		putInitialPanel(columnLayoutCtr.getInitialComponent());
		// register for AssessmentEvents triggered by this user
		singleUserEventBus = ureq.getUserSession().getSingleUserEventCenter();
		singleUserEventBus.registerFor(this, ureq.getIdentity(), assessmentEventOres);
		
		//disposed message controller
		//must be created beforehand
		Panel empty = new Panel("empty");//empty panel set as "menu" and "tool"
		Controller disposedBusinessGroup = new DisposedBusinessGroup(ureq, getWindowControl());
		LayoutMain3ColsController disposedController = new LayoutMain3ColsController(ureq, getWindowControl(), empty, disposedBusinessGroup.getInitialComponent(), "disposed grouprun");
		disposedController.addDisposableChildController(disposedBusinessGroup);
		setDisposedMsgController(disposedController);

		// add as listener to BusinessGroup so we are being notified about changes.
		CoordinatorManager.getInstance().getCoordinator().getEventBus().registerFor(this, getIdentity(), businessGroup);

		// show disabled message when collaboration is disabled (e.g. in a test)		
		if(AssessmentEvent.isAssessmentStarted(ureq.getUserSession())){
			groupRunDisabled = true;
			showError("grouprun.disabled");				
		}
		
		Object wildcard = ureq.getUserSession().getEntry("wild_card_" + businessGroup.getKey());
		if(wildcard == null) {
			//check managed
			AccessResult acResult = acService.isAccessible(businessGroup, getIdentity(), false);
			if(acResult.isAccessible()) {
				needActivation = false;
			}  else if (businessGroup != null && acResult.getAvailableMethods().size() > 0) {
				accessController = new AccessListController(ureq, getWindowControl(), acResult.getAvailableMethods());
				listenTo(accessController);
				mainPanel.setContent(accessController.getInitialComponent());
				bgTree.setTreeModel(new GenericTreeModel());
				needActivation = true;
			} else {
				mainPanel.setContent(new Panel("empty"));
				bgTree.setTreeModel(new GenericTreeModel());
				needActivation = true;
			}
		} else {
			ureq.getUserSession().removeEntryFromNonClearedStore("wild_card_" + businessGroup.getKey());
			needActivation = false;
		}
	}
	
	private boolean isChatAvailable() {
		return CoreSpringFactory.getImpl(InstantMessagingModule.class).isEnabled() &&
				CoreSpringFactory.getImpl(InstantMessagingModule.class).isGroupEnabled() && 
				CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup).isToolEnabled(CollaborationTools.TOOL_CHAT);

	}
	
	private Component getOnWaitingListMessage(UserRequest ureq, BusinessGroup group) {
		VelocityContainer vc = createVelocityContainer("waiting");
		vc.contextPut("name", group.getName());
		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, vc, "grouprun");
		listenTo(columnLayoutCtr); // cleanup on dispose
		return columnLayoutCtr.getInitialComponent();
	}
	
	private Component getNoAccessMessage(UserRequest ureq, BusinessGroup group) {
		VelocityContainer vc = createVelocityContainer("access_denied");
		vc.contextPut("name", group.getName());
		columnLayoutCtr = new LayoutMain3ColsController(ureq, getWindowControl(), null, vc, "grouprun");
		listenTo(columnLayoutCtr); // cleanup on dispose
		return columnLayoutCtr.getInitialComponent();
	}
	
	private boolean isOnWaitinglist(List<BusinessGroupMembership> memberships) {
		boolean waiting = false;
		for(BusinessGroupMembership membership:memberships) {
			if(membership.isOwner() || membership.isParticipant()) {
				return false;
			} else if (membership.isWaiting()) {
				waiting = true;
			}
		}
		return waiting;
	}

	private void exposeGroupDetailsToVC(BusinessGroup currBusinessGroup) {
		main.contextPut("BuddyGroup", currBusinessGroup);
		main.contextPut("hasOwners", Boolean.TRUE);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		// events from menutree
		if (source == bgTree) { // user chose news, contactform, forum, folder or
			// administration
			if (!groupRunDisabled && event.getCommand().equals(MenuTree.COMMAND_TREENODE_CLICKED)) {
				TreeNode selTreeNode = bgTree.getSelectedNode();
				String cmd = (String) selTreeNode.getUserObject();
				handleTreeActions(ureq, cmd);
				//fxdiff BAKS-7 Resume function
				if(collabToolCtr != null) {
					addToHistory(ureq, collabToolCtr);
				}
			} else if (groupRunDisabled) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_OVERVIEW);
				this.showError("grouprun.disabled");
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == bgEditCntrllr) {
			// changes from the admin controller
			if (event == Event.CHANGED_EVENT) {
				TreeModel trMdl = buildTreeModel();
				bgTree.setTreeModel(trMdl);
			} else if (event == Event.CANCELLED_EVENT) {
				// could not get lock on business group, back to inital screen
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}

		} else if (source == resourcesCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				TableEvent te = (TableEvent) event;
				String actionid = te.getActionId();
				int rowid = te.getRowId();
				RepositoryTableModel repoTableModel = (RepositoryTableModel) resourcesCtr.getTableDataModel();
				if (RepositoryTableModel.TABLE_ACTION_SELECT_ENTRY.equals(actionid)
						|| RepositoryTableModel.TABLE_ACTION_SELECT_LINK.equals(actionid)) {

					RepositoryEntry currentRepoEntry = repoTableModel.getObject(rowid);
					OLATResource ores = currentRepoEntry.getOlatResource();
					if (ores == null) throw new AssertException("repoEntry had no olatresource, repoKey = " + currentRepoEntry.getKey());
					addLoggingResourceable(LoggingResourceable.wrap(ores, OlatResourceableType.genRepoEntry));

					String businessPath = "[RepositoryEntry:" + currentRepoEntry.getKey() + "]";
					NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
				}
			}
		} else if (source == sendToChooserForm) {
			if (event == Event.DONE_EVENT) {
				removeAsListenerAndDispose(collabToolCtr);
				collabToolCtr = createContactFormController(ureq);
				listenTo(collabToolCtr);
				mainPanel.setContent(collabToolCtr.getInitialComponent());
			} else if (event == Event.CANCELLED_EVENT) {
				// back to group overview
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}
		//fxdiff BAKS-7 Resume function -> need to be at the end, sendToChooserForm are collabToolCtr too
		} else if (source == collabToolCtr) {
			if (event == Event.CANCELLED_EVENT || event == Event.DONE_EVENT || event == Event.BACK_EVENT || event == Event.FAILED_EVENT) {
				// In all cases (success or failure) we
				// go back to the group overview page.
				bgTree.setSelectedNodeId(bgTree.getTreeModel().getRootNode().getIdent());
				mainPanel.setContent(main);
			}
		//fxdiff VCRP-1,2: access control of resources
		} else if (source == accessController) {
			if(event.equals(AccessEvent.ACCESS_OK_EVENT)) {
				removeAsListenerAndDispose(accessController);
				accessController = null;
				
				//check if on waiting list
				List<BusinessGroupMembership> memberships = businessGroupService.getBusinessGroupMembership(Collections.singletonList(businessGroup.getKey()), getIdentity());
				if(isOnWaitinglist(memberships)) {
					Component cmp = getOnWaitingListMessage(ureq, businessGroup);
					mainPanel.setContent(cmp);
				} else {
					mainPanel.setContent(main);
					bgTree.setTreeModel(buildTreeModel());
					needActivation = false;
				}
			} else if(event.equals(AccessEvent.ACCESS_FAILED_EVENT)) {
				String msg = ((AccessEvent)event).getMessage();
				if(StringHelper.containsNonWhitespace(msg)) {
					getWindowControl().setError(msg);
				} else {
					showError("error.accesscontrol");
				}
			}
		}
	}

	/**
	 * generates the email adress list.
	 * 
	 * @param ureq
	 * @return a contact form controller for this group
	 */
	private ContactFormController createContactFormController(UserRequest ureq) {
		ContactMessage cmsg = new ContactMessage(ureq.getIdentity());
		// two named ContactLists, the new way using the contact form
		// the same name as in the checkboxes are taken as contactlist names
		ContactList ownerCntctLst;// = new ContactList(translate("sendtochooser.form.chckbx.owners"));
		ContactList partipCntctLst;// = new ContactList(translate("sendtochooser.form.chckbx.partip"));
		ContactList waitingListContactList;// = new ContactList(translate("sendtochooser.form.chckbx.waitingList"));

		if (sendToChooserForm.ownerChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
			ownerCntctLst = new ContactList(translate("sendtochooser.form.radio.owners.all"));
			List<Identity> ownerList = businessGroupService.getMembers(businessGroup, GroupRoles.coach.name());
			ownerCntctLst.addAllIdentites(ownerList);
			cmsg.addEmailTo(ownerCntctLst);
		} else {
			if (sendToChooserForm.ownerChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
				ownerCntctLst = new ContactList(translate("sendtochooser.form.radio.owners.choose"));
				List<Identity> ownerList = businessGroupService.getMembers(businessGroup, GroupRoles.coach.name());
				List<Identity> changeableOwnerList = new ArrayList<>(ownerList);
				for (Identity identity : ownerList) {
					boolean keyIsSelected = false;
					for (Long key : sendToChooserForm.getSelectedOwnerKeys()) {
						if (key.equals(identity.getKey())) {
							keyIsSelected = true;
							break;
						}
					}
					if (!keyIsSelected) {
						changeableOwnerList.remove(changeableOwnerList.indexOf(identity));
					}
				}
				ownerCntctLst.addAllIdentites(changeableOwnerList);
				cmsg.addEmailTo(ownerCntctLst);
			}
		}

		if (sendToChooserForm != null) {
			if  (sendToChooserForm.participantChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
				partipCntctLst  = new ContactList(translate("sendtochooser.form.radio.partip.all"));
				List<Identity> participantsList = businessGroupService.getMembers(businessGroup, GroupRoles.participant.name());
				partipCntctLst.addAllIdentites(participantsList);
				cmsg.addEmailTo(partipCntctLst);
			} else {
				if (sendToChooserForm.participantChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
					partipCntctLst  = new ContactList(translate("sendtochooser.form.radio.partip.choose"));
					List<Identity> participantsList = businessGroupService.getMembers(businessGroup, GroupRoles.participant.name());
					List<Identity> changeableParticipantsList = new ArrayList<>(participantsList);
					for (Identity identity : participantsList) {
						boolean keyIsSelected = false;
						for (Long key : sendToChooserForm.getSelectedPartipKeys()) {
							if (key.equals(identity.getKey())) {
								keyIsSelected = true;
								break;
							}
						}
						if (!keyIsSelected) {
							changeableParticipantsList.remove(changeableParticipantsList.indexOf(identity));
						}
					}
					partipCntctLst.addAllIdentites(changeableParticipantsList);
					cmsg.addEmailTo(partipCntctLst);
				}
			}
			
		}
		if (sendToChooserForm != null && isAdmin && businessGroup.getWaitingListEnabled().booleanValue()) {
			if (sendToChooserForm.waitingListChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_ALL)) {
				waitingListContactList = new ContactList(translate("sendtochooser.form.radio.waitings.all"));
				List<Identity> waitingListIdentities = businessGroupService.getMembers(businessGroup, GroupRoles.waiting.name());
				waitingListContactList.addAllIdentites(waitingListIdentities);
				cmsg.addEmailTo(waitingListContactList);
			} else {
				if (sendToChooserForm.waitingListChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_CHOOSE)) {
					waitingListContactList = new ContactList(translate("sendtochooser.form.radio.waitings.choose"));
					List<Identity> waitingListIdentities = businessGroupService.getMembers(businessGroup, GroupRoles.waiting.name());
					List<Identity> changeableWaitingListIdentities = new ArrayList<>(waitingListIdentities);
					for (Identity indentity : waitingListIdentities) {
						boolean keyIsSelected = false;
						for (Long key : sendToChooserForm.getSelectedWaitingKeys()) {
							if (key.equals(indentity.getKey())) {
								keyIsSelected = true;
								break;
							}
						}
						if (!keyIsSelected) {
							changeableWaitingListIdentities.remove(changeableWaitingListIdentities.indexOf(indentity));
						}
					}
					waitingListContactList.addAllIdentites(changeableWaitingListIdentities);
					cmsg.addEmailTo(waitingListContactList);
				}
			}
		}
		
		cmsg.setSubject( translate("businessgroup.contact.subject", businessGroup.getName() ) );
		
		if (sendToChooserForm.waitingListChecked().equals(BusinessGroupSendToChooserForm.NLS_RADIO_NOTHING)) {
			String restUrl = BusinessControlFactory.getInstance().getAsURIString(getWindowControl().getBusinessControl(), true);
			cmsg.setBodyText( getTranslator().translate("businessgroup.contact.bodytext", new String[]{ businessGroup.getName(), restUrl} ) );
		} else {
			cmsg.setBodyText ("");
		}
		
		CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);
		ContactFormController cofocntrllr = collabTools.createContactFormController(ureq, getWindowControl(), cmsg);
		return cofocntrllr;
	}

	/**
	 * handles the different tree actions
	 * 
	 * @param ureq
	 * @param cmd
	 */
	private void handleTreeActions(UserRequest ureq, String cmd) {
		// release edit lock if available
		removeAsListenerAndDispose(bgEditCntrllr);
		removeAsListenerAndDispose(collabToolCtr);
		
		CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);

		// init new controller according to user click
		if (ACTIVITY_MENUSELECT_OVERVIEW.equals(cmd)) {
			// root node clicked display overview
			mainPanel.setContent(main);
		} else if (ACTIVITY_MENUSELECT_FORUM.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLFORUM, OlatResourceableType.forum));
			SubscriptionContext sc = new SubscriptionContext(businessGroup, INITVIEW_TOOLFORUM);
			
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLFORUM);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);

			collabToolCtr = collabTools.createForumController(ureq, bwControl, isAdmin, ureq.getUserSession().getRoles().isGuestOnly(),	sc);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_CHAT.equals(cmd)) {
			collabToolCtr = collabTools.createChatController(ureq, getWindowControl(), businessGroup, isAdmin);
			if(collabToolCtr == null) {
				showWarning("groupchat.not.available");
				mainPanel.setContent(new Panel("empty"));
			} else {
				mainPanel.setContent(collabToolCtr.getInitialComponent());
			}
		} else if (ACTIVITY_MENUSELECT_CALENDAR.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLCAL, OlatResourceableType.calendar));
			
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLCAL);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ce.getOLATResourceable()));
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);

			collabToolCtr = collabTools.createCalendarController(ureq, bwControl, this.businessGroup, isAdmin);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_INFORMATION.equals(cmd)) {
			//fxdiff BAKS-7 Resume function
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLMSG);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ce.getOLATResourceable()));
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, getWindowControl());
			collabToolCtr = collabTools.createNewsController(ureq, bwControl);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_FOLDER.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLFOLDER, OlatResourceableType.sharedFolder));

			SubscriptionContext sc = new SubscriptionContext(businessGroup, INITVIEW_TOOLFOLDER);
			
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the forum clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLFOLDER);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ce.getOLATResourceable()));
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			//fxdiff VCRP-8: collaboration tools folder access control
			collabToolCtr = collabTools.createFolderController(ureq, bwControl, businessGroup, isAdmin, sc);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_MEMBERSLIST.equals(cmd)) {
			doShowMembers(ureq);
		} else if (ACTIVITY_MENUSELECT_CONTACTFORM.equals(cmd)) {
			doContactForm(ureq);
		} else if (ACTIVITY_MENUSELECT_ADMINISTRATION.equals(cmd)) {
			doAdministration(ureq);
		} else if (ACTIVITY_MENUSELECT_SHOW_RESOURCES.equals(cmd)) {
			doShowResources(ureq);
		} else if (ACTIVITY_MENUSELECT_WIKI.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLWIKI, OlatResourceableType.wiki));
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the wiki clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLWIKI);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapWikiOres(ce.getOLATResourceable()));

			collabToolCtr = collabTools.createWikiController(ureq, bwControl);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_PORTFOLIO.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLPORTFOLIO, OlatResourceableType.portfolio));
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the wiki clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLPORTFOLIO);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapPortfolioOres(ce.getOLATResourceable()));

			collabToolCtr = collabTools.createPortfolioController(ureq, bwControl, businessGroup);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
		} else if (ACTIVITY_MENUSELECT_OPENMEETINGS.equals(cmd)) {
			addLoggingResourceable(LoggingResourceable.wrap(ORES_TOOLOPENMEETINGS, OlatResourceableType.portfolio));
			WindowControl bwControl = getWindowControl();
			// calculate the new businesscontext for the wiki clicked
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(ORES_TOOLOPENMEETINGS);
			bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, bwControl);
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapPortfolioOres(ce.getOLATResourceable()));

			collabToolCtr = collabTools.createOpenMeetingsController(ureq, bwControl, businessGroup, isAdmin);
			listenTo(collabToolCtr);
			mainPanel.setContent(collabToolCtr.getInitialComponent());
			//fxdiff VCRP-1,2: access control of resources
		}  else if (ACTIVITY_MENUSELECT_AC.equals(cmd)) {
			doAccessControlHistory(ureq);
		} 
	}

	private void doAdministration(UserRequest ureq) {
		removeAsListenerAndDispose(bgEditCntrllr);
		//fxdiff BAKS-7 Resume function
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ORES_TOOLADMIN));
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ORES_TOOLADMIN, null, getWindowControl());
		collabToolCtr = bgEditCntrllr = new BusinessGroupEditController(ureq, bwControl, businessGroup);
		listenTo(bgEditCntrllr);
		mainPanel.setContent(bgEditCntrllr.getInitialComponent());
	}
	
	//fxdiff VCRP-1,2: access control of resources
	private void doAccessControlHistory(UserRequest ureq) {
		removeAsListenerAndDispose(bgACHistoryCtrl);
		OLATResource resource = businessGroup.getResource();
		bgACHistoryCtrl = new OrdersAdminController(ureq, getWindowControl(), resource);
		listenTo(bgACHistoryCtrl);
		mainPanel.setContent(bgACHistoryCtrl.getInitialComponent());
	}

	private void doContactForm(UserRequest ureq) {
		if (vc_sendToChooserForm == null) vc_sendToChooserForm = createVelocityContainer("cosendtochooser");
		removeAsListenerAndDispose(sendToChooserForm);
		//fxdiff BAKS-7 Resume function
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ORES_TOOLCONTACT, null, getWindowControl());
		sendToChooserForm = new BusinessGroupSendToChooserForm(ureq, bwControl, businessGroup, isAdmin);
		listenTo(sendToChooserForm);
		vc_sendToChooserForm.put("vc_sendToChooserForm", sendToChooserForm.getInitialComponent());
		mainPanel.setContent(vc_sendToChooserForm);
	}

	private void doShowMembers(UserRequest ureq) {
		VelocityContainer membersVc = createVelocityContainer("ownersandmembers");
		// 1. show owners if configured with Owners
		boolean downloadAllowed = businessGroup.isDownloadMembersLists();
		Group group = businessGroupService.getGroup(businessGroup);
		if (businessGroup.isOwnersVisibleIntern()) {
			removeAsListenerAndDispose(gownersC);
			gownersC = new GroupController(ureq, getWindowControl(), false, true, true, false, downloadAllowed, false, group, GroupRoles.coach.name());
			listenTo(gownersC);
			membersVc.put("owners", gownersC.getInitialComponent());
			membersVc.contextPut("showOwnerGroups", Boolean.TRUE);
		} else {
			membersVc.contextPut("showOwnerGroups", Boolean.FALSE);
		}
		// 2. show participants if configured with Participants
		if (businessGroup.isParticipantsVisibleIntern()) {
			removeAsListenerAndDispose(gparticipantsC);
			gparticipantsC = new GroupController(ureq, getWindowControl(), false, true, true, false, downloadAllowed, false, group, GroupRoles.participant.name());
			listenTo(gparticipantsC);
			
			membersVc.put("participants", gparticipantsC.getInitialComponent());
			membersVc.contextPut("showPartipsGroups", Boolean.TRUE);
		} else {
			membersVc.contextPut("showPartipsGroups", Boolean.FALSE);
		}
		// 3. show waiting-list if configured 
		membersVc.contextPut("hasWaitingList", new Boolean(businessGroup.getWaitingListEnabled()) );
		if (businessGroup.isWaitingListVisibleIntern()) {
			removeAsListenerAndDispose(waitingListController);
			waitingListController = new GroupController(ureq, getWindowControl(), false, true, true, false, downloadAllowed, false, group, GroupRoles.waiting.name());
			listenTo(waitingListController);
			membersVc.put("waitingList", waitingListController.getInitialComponent());
			membersVc.contextPut("showWaitingList", Boolean.TRUE);
		} else {
			membersVc.contextPut("showWaitingList", Boolean.FALSE);
		}
		mainPanel.setContent(membersVc);
		//fxdiff BAKS-7 Resume function
		collabToolCtr = null;
		addToHistory(ureq, ORES_TOOLMEMBERS, null);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		ThreadLocalUserActivityLogger.log(GroupLoggingAction.GROUP_CLOSED, getClass());
		
		if (chatAvailable) {
			CloseInstantMessagingEvent e = new CloseInstantMessagingEvent(businessGroup);
			singleUserEventBus.fireEventToListenersOf(e, InstantMessagingService.TOWER_EVENT_ORES);
		}
		CoordinatorManager.getInstance().getCoordinator().getEventBus().deregisterFor(this, businessGroup);
		if(singleUserEventBus != null) {
			singleUserEventBus.deregisterFor(this, assessmentEventOres);
		}
	}

	@Override
	//fxdiff BAKS-7 Resume function
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		if(needActivation) {
			return;
		}
		ContextEntry ce = entries.remove(0);
		activate(ureq, ce);
		if(collabToolCtr instanceof Activateable2) {
			((Activateable2)collabToolCtr).activate(ureq, entries, ce.getTransientState());
		}
	}

	//fxdiff BAKS-7 Resume function
	private void activate(UserRequest ureq, ContextEntry ce) {
		OLATResourceable ores = ce.getOLATResourceable();
		if (OresHelper.equals(ores, ORES_TOOLFORUM)) {
			// start the forum
			if (nodeForum != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_FORUM);
				bgTree.setSelectedNode(nodeForum);
			} else { // not enabled
				String text = translate("warn.forumnotavailable");
				Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
				listenTo(mc); // cleanup on dispose
				mainPanel.setContent(mc.getInitialComponent());
			}
		} else if (OresHelper.equals(ores, ORES_TOOLFOLDER)) {
			if (nodeFolder != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_FOLDER);
				bgTree.setSelectedNode(nodeFolder);
			} else { // not enabled
				String text = translate("warn.foldernotavailable");				
				Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
				listenTo(mc); // cleanup on dispose
				mainPanel.setContent(mc.getInitialComponent());
			}
		} else if (OresHelper.equals(ores, ORES_TOOLWIKI)) {
			if (nodeWiki != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_WIKI);
				bgTree.setSelectedNode(nodeWiki);
			} else { // not enabled
				String text = translate("warn.wikinotavailable");
				Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
				listenTo(mc); // cleanup on dispose
				mainPanel.setContent(mc.getInitialComponent());
			}
		} else if (OresHelper.equals(ores, ORES_TOOLCAL)) {
			if (nodeCal != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_CALENDAR);
				bgTree.setSelectedNode(nodeCal);
			} else { // not enabled
				String text = translate("warn.calnotavailable");
				Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
				listenTo(mc); // cleanup on dispose
				mainPanel.setContent(mc.getInitialComponent());
			}
		} else if (OresHelper.equals(ores, ORES_TOOLPORTFOLIO)) {
				if (nodePortfolio != null) {
					handleTreeActions(ureq, ACTIVITY_MENUSELECT_PORTFOLIO);
					bgTree.setSelectedNode(nodePortfolio);
				} else { // not enabled
					String text = translate("warn.portfolionotavailable");
					Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
					listenTo(mc); // cleanup on dispose
					mainPanel.setContent(mc.getInitialComponent());
				}
		} else if (OresHelper.equals(ores, ORES_TOOLOPENMEETINGS)) {
			if (nodeOpenMeetings != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_OPENMEETINGS);
				bgTree.setSelectedNode(nodeOpenMeetings);
			} else { // not enabled
				String text = translate("warn.portfolionotavailable");
				Controller mc = MessageUIFactory.createInfoMessage(ureq, getWindowControl(), null, text);
				listenTo(mc); // cleanup on dispose
				mainPanel.setContent(mc.getInitialComponent());
			}
		//fxdiff BAKS-7 Resume function
		} else if (OresHelper.equals(ores, ORES_TOOLADMIN)) {
			if (this.nodeAdmin != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_ADMINISTRATION);
				bgTree.setSelectedNode(nodeAdmin);
			}
		} else if (OresHelper.equals(ores, ORES_TOOLMSG)) {
			if (this.nodeInformation != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_INFORMATION);
				bgTree.setSelectedNode(nodeInformation);
			}
		} else if (OresHelper.equals(ores, ORES_TOOLCONTACT)) {
			if (this.nodeContact != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_CONTACTFORM);
				bgTree.setSelectedNode(nodeContact);
			}
		} else if (OresHelper.equals(ores, ORES_TOOLMEMBERS)) {
			if (this.nodeGroupOwners != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_MEMBERSLIST);
				bgTree.setSelectedNode(nodeGroupOwners);
			}
		} else if (OresHelper.equals(ores, ORES_TOOLRESOURCES)) {
			if (this.nodeResources != null) {
				handleTreeActions(ureq, ACTIVITY_MENUSELECT_SHOW_RESOURCES);
				bgTree.setSelectedNode(nodeResources);
			}
		}
	}

	/**
	 * @see org.olat.core.util.event.GenericEventListener#event(org.olat.core.gui.control.Event)
	 */
	public void event(Event event) {
		if (event instanceof OLATResourceableJustBeforeDeletedEvent) {
			OLATResourceableJustBeforeDeletedEvent delEvent = (OLATResourceableJustBeforeDeletedEvent) event;
			if (!delEvent.targetEquals(businessGroup)) throw new AssertException(
					"receiving a delete event for a olatres we never registered for!!!:" + delEvent.getDerivedOres());
			dispose();

		} else if (event instanceof BusinessGroupModifiedEvent) {
			BusinessGroupModifiedEvent bgmfe = (BusinessGroupModifiedEvent) event;
			if (event.getCommand().equals(BusinessGroupModifiedEvent.CONFIGURATION_MODIFIED_EVENT)) {
				// reset business group property manager
				// update reference to update business group object
				businessGroup = businessGroupService.loadBusinessGroup(businessGroup);
				chatAvailable = isChatAvailable();
				
				main.contextPut("BuddyGroup", businessGroup);
				TreeModel trMdl = buildTreeModel();
				bgTree.setTreeModel(trMdl);
				if (bgEditCntrllr == null) {
					// change didn't origin by our own edit controller
					showInfo("grouprun.configurationchanged");
					bgTree.setSelectedNodeId(trMdl.getRootNode().getIdent());
					mainPanel.setContent(main);
				} else {
					// Activate edit menu item
					bgTree.setSelectedNodeId(ACTIVITY_MENUSELECT_ADMINISTRATION);
				}
			} else if (bgmfe.wasMyselfRemoved(getIdentity())) {
				//nothing more here!! The message will be created and displayed upon disposing
				dispose();//disposed message controller will be set
			}
		} else if(event instanceof AssessmentEvent) {
			if(((AssessmentEvent)event).getEventType().equals(AssessmentEvent.TYPE.STARTED)) {
				groupRunDisabled = true;			 
			} else if (((AssessmentEvent)event).getEventType().equals(AssessmentEvent.TYPE.STOPPED)) {
				groupRunDisabled = false;
			}
		}
	}

	private void doShowResources(UserRequest ureq) {
		// always refresh data model, maybe it has changed
		RepositoryTableModel repoTableModel = new RepositoryTableModel(resourceTrans);
		List<RepositoryEntry> repoTableModelEntries = businessGroupService.findRepositoryEntries(Collections.singletonList(businessGroup), 0, -1);
		repoTableModel.setObjects(repoTableModelEntries);
		// init table controller only once
		if (resourcesCtr == null) {
			TableGuiConfiguration tableConfig = new TableGuiConfiguration();
			tableConfig.setTableEmptyMessage(translate("resources.noresources"));
			//removeAsListenerAndDispose(resourcesCtr);
			resourcesCtr = new TableController(tableConfig, ureq, getWindowControl(), resourceTrans);
			listenTo(resourcesCtr);
			
			resourcesVC = createVelocityContainer("resources");
			repoTableModel.addColumnDescriptors(resourcesCtr, null, true);
			resourcesVC.put("resources", resourcesCtr.getInitialComponent());
		}
		// add table model to table
		resourcesCtr.setTableDataModel(repoTableModel);
		mainPanel.setContent(resourcesVC);
		//fxdiff BAKS-7 Resume function
		addToHistory(ureq, ORES_TOOLRESOURCES, null);
	}

	/**
	 * Activates the administration menu item. Make sure you have the rights to do
	 * this, otherwhise this will throw a nullpointer exception
	 * 
	 * @param ureq
	 */
	public void activateAdministrationMode(UserRequest ureq) {
		doAdministration(ureq);
		bgTree.setSelectedNodeId(adminNodeId);
	}

	/**
	 * @return The menu tree model
	 */
	private TreeModel buildTreeModel() {
		GenericTreeNode gtnChild, root;

		GenericTreeModel gtm = new GenericTreeModel();
		root = new GenericTreeNode();
		root.setTitle(businessGroup.getName());
		root.setUserObject(ACTIVITY_MENUSELECT_OVERVIEW);
		root.setAltText(translate("menutree.top.alt") + " " + businessGroup.getName());
		root.setIconCssClass("o_icon o_icon_group");
		gtm.setRootNode(root);
		
		CollaborationTools collabTools = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(this.businessGroup);

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_NEWS)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.news"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_INFORMATION);
			gtnChild.setAltText(translate("menutree.news.alt"));
			gtnChild.setIconCssClass("o_icon_news");
			gtnChild.setCssClass("o_sel_group_news");
			root.addChild(gtnChild);
			nodeInformation = gtnChild;
		}

		if (calendarModule.isEnabled() && calendarModule.isEnableGroupCalendar() && collabTools.isToolEnabled(CollaborationTools.TOOL_CALENDAR)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.calendar"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CALENDAR);
			gtnChild.setAltText(translate("menutree.calendar.alt"));
			gtnChild.setIconCssClass("o_calendar_icon");
			gtnChild.setCssClass("o_sel_group_calendar");
			root.addChild(gtnChild);
			nodeCal = gtnChild;
		}
		
		boolean hasResources = businessGroupService.hasResources(businessGroup);
		if(hasResources) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.resources"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_SHOW_RESOURCES);
			gtnChild.setAltText(translate("menutree.resources.alt"));
			gtnChild.setIconCssClass("o_CourseModule_icon");
			gtnChild.setCssClass("o_sel_group_resources");
			root.addChild(gtnChild);
			nodeResources = gtnChild;
		}

		if (businessGroup.isOwnersVisibleIntern() || businessGroup.isParticipantsVisibleIntern()) {
			// either owners or participants, or both are visible
			// otherwise the node is not visible
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.members"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_MEMBERSLIST);
			gtnChild.setAltText(translate("menutree.members.alt"));
			gtnChild.setIconCssClass("o_icon_group");
			gtnChild.setCssClass("o_sel_group_members");
			root.addChild(gtnChild);
			nodeGroupOwners = gtnChild;
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_CONTACT)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.contactform"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CONTACTFORM);
			gtnChild.setAltText(translate("menutree.contactform.alt"));
			gtnChild.setIconCssClass("o_co_icon");
			gtnChild.setCssClass("o_sel_group_contact");
			root.addChild(gtnChild);
			nodeContact = gtnChild;
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.folder"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_FOLDER);
			gtnChild.setAltText(translate("menutree.folder.alt"));
			gtnChild.setIconCssClass("o_bc_icon");
			gtnChild.setCssClass("o_sel_group_folder");
			root.addChild(gtnChild);
			nodeFolder = gtnChild;
		}

		if (collabTools.isToolEnabled(CollaborationTools.TOOL_FORUM)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.forum"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_FORUM);
			gtnChild.setAltText(translate("menutree.forum.alt"));
			gtnChild.setIconCssClass("o_fo_icon");
			gtnChild.setCssClass("o_sel_group_forum");
			root.addChild(gtnChild);
			nodeForum = gtnChild;
		}

		
		if (chatAvailable) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.chat"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_CHAT);
			gtnChild.setAltText(translate("menutree.chat.alt"));
			gtnChild.setIconCssClass("o_icon_chat");
			gtnChild.setCssClass("o_sel_group_chat");
			root.addChild(gtnChild);
		}

		BaseSecurityModule securityModule = CoreSpringFactory.getImpl(BaseSecurityModule.class); 
		if (collabTools.isToolEnabled(CollaborationTools.TOOL_WIKI) && securityModule.isWikiEnabled()) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.wiki"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_WIKI);
			gtnChild.setAltText(translate("menutree.wiki.alt"));
			gtnChild.setIconCssClass("o_wiki_icon");
			gtnChild.setCssClass("o_sel_group_wiki");
			root.addChild(gtnChild);
			nodeWiki = gtnChild;
		}
		
		PortfolioModule portfolioModule = (PortfolioModule) CoreSpringFactory.getBean("portfolioModule");		
		if (collabTools.isToolEnabled(CollaborationTools.TOOL_PORTFOLIO) && portfolioModule.isEnabled()) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.portfolio"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_PORTFOLIO);
			gtnChild.setAltText(translate("menutree.portfolio.alt"));
			gtnChild.setIconCssClass("o_ep_icon");
			gtnChild.setCssClass("o_sel_group_portfolio");
			root.addChild(gtnChild);
			nodePortfolio = gtnChild;
		}
		
		OpenMeetingsModule openMeetingsModule = CoreSpringFactory.getImpl(OpenMeetingsModule.class);		
		if (openMeetingsModule.isEnabled() && collabTools.isToolEnabled(CollaborationTools.TOOL_OPENMEETINGS)) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.openmeetings"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_OPENMEETINGS);
			gtnChild.setAltText(translate("menutree.openmeetings.alt"));
			gtnChild.setIconCssClass("o_openmeetings_icon");
			root.addChild(gtnChild);
			nodePortfolio = gtnChild;
		}

		if (isAdmin) {
			gtnChild = new GenericTreeNode();
			gtnChild.setTitle(translate("menutree.administration"));
			gtnChild.setUserObject(ACTIVITY_MENUSELECT_ADMINISTRATION);
			gtnChild.setIdent(ACTIVITY_MENUSELECT_ADMINISTRATION);
			gtnChild.setAltText(translate("menutree.administration.alt"));
			gtnChild.setIconCssClass("o_icon_settings");
			root.addChild(gtnChild);
			adminNodeId = gtnChild.getIdent();
			nodeAdmin = gtnChild;

			AccessControlModule acModule = (AccessControlModule)CoreSpringFactory.getBean("acModule");
			if(acModule.isEnabled() && acService.isResourceAccessControled(businessGroup.getResource(), null)) {
				gtnChild = new GenericTreeNode();
				gtnChild.setTitle(translate("menutree.ac"));
				gtnChild.setUserObject(ACTIVITY_MENUSELECT_AC);
				gtnChild.setIdent(ACTIVITY_MENUSELECT_AC);
				gtnChild.setAltText(translate("menutree.ac.alt"));
				gtnChild.setIconCssClass("o_icon_booking");
				root.addChild(gtnChild);
			}
		}

		return gtm;
	}

}