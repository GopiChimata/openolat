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

package org.olat.course.certificate.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.olat.NewControllerFactory;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.segmentedview.SegmentViewComponent;
import org.olat.core.gui.components.segmentedview.SegmentViewEvent;
import org.olat.core.gui.components.segmentedview.SegmentViewFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.assessment.EfficiencyStatement;
import org.olat.course.assessment.EfficiencyStatementManager;
import org.olat.course.assessment.IdentityAssessmentEditController;
import org.olat.course.assessment.IdentityAssessmentOverviewController;
import org.olat.course.assessment.portfolio.EfficiencyStatementArtefact;
import org.olat.course.certificate.Certificate;
import org.olat.course.certificate.CertificatesManager;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.modules.co.ContactFormController;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.ui.artefacts.collect.ArtefactWizzardStepsController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryService;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Description:<br>
 * Displays the users efficiency statement
 * 
 * <P>
 * Initial Date:  11.08.2005 <br>
 * @author gnaegi
 */
public class CertificateAndEfficiencyStatementController extends BasicController {

	private static final String usageIdentifyer = "org.olat.course.assessment.EfficiencyStatementController";
	
	private VelocityContainer mainVC;
	private SegmentViewComponent segmentView;
	private Link certificateLink, courseDetailsLink;
	private Link collectArtefactLink, homeLink, courseLink, groupLink, contactLink;
	
	private final Certificate certificate;
	private final EfficiencyStatement efficiencyStatement;
	private final Identity statementOwner;
	private final BusinessGroup businessGroup;
	private final RepositoryEntry courseRepoEntry;
	
	
	private Controller ePFCollCtrl;
	private CloseableModalController cmc;
	private ContactFormController contactCtrl;
	private CertificateController certificateCtrl;
	private IdentityAssessmentOverviewController courseDetailsCtrl;
	
	@Autowired
	private PortfolioModule portfolioModule;
	@Autowired
	private CertificatesManager certificatesManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	
	/**
	 * The constructor shows the efficiency statement given as parameter for the current user
	 * @param wControl
	 * @param ureq
	 * @param courseId
	 */
	public CertificateAndEfficiencyStatementController(WindowControl wControl, UserRequest ureq, EfficiencyStatement efficiencyStatement) {
		this(wControl, ureq, ureq.getIdentity(), null, null, null, efficiencyStatement, false);
	}
	
	/**
	 * This constructor show the efficiency statement for the course repository key and the current user
	 * @param wControl
	 * @param ureq
	 * @param courseRepoEntryKey
	 */
	public CertificateAndEfficiencyStatementController(WindowControl wControl, UserRequest ureq, Long resourceKey) {
		this(wControl, ureq, 
				ureq.getIdentity(), null, resourceKey, CoreSpringFactory.getImpl(RepositoryService.class).loadByResourceKey(resourceKey),
				EfficiencyStatementManager.getInstance().getUserEfficiencyStatementByResourceKey(resourceKey, ureq.getIdentity()),
				false);
	}
	
	public CertificateAndEfficiencyStatementController(WindowControl wControl, UserRequest ureq, RepositoryEntry entry) {
		this(wControl, ureq, 
				ureq.getIdentity(), null, entry.getOlatResource().getKey(), entry,
				EfficiencyStatementManager.getInstance().getUserEfficiencyStatementByResourceKey(entry.getOlatResource().getKey(), ureq.getIdentity()),
				false);
	}
	
	public CertificateAndEfficiencyStatementController(WindowControl wControl, UserRequest ureq, Identity statementOwner,
			BusinessGroup businessGroup, Long resourceKey, RepositoryEntry courseRepo, EfficiencyStatement efficiencyStatement, boolean links) {
		super(ureq, wControl);
		setTranslator(Util.createPackageTranslator(IdentityAssessmentEditController.class, getLocale(), getTranslator()));
		setTranslator(UserManager.getInstance().getPropertyHandlerTranslator(getTranslator()));
		
		this.courseRepoEntry = courseRepo;
		this.businessGroup = businessGroup;

		if(businessGroup == null && courseRepo != null) {
			SearchBusinessGroupParams params = new SearchBusinessGroupParams(statementOwner, false, true);
			List<BusinessGroup> groups = businessGroupService.findBusinessGroups(params, courseRepo, 0, -1);
			if(groups.size() > 0) {
				businessGroup = groups.get(0);
			}
		}

		this.statementOwner = statementOwner;
		this.efficiencyStatement = efficiencyStatement;
		certificate = certificatesManager.getLastCertificate(statementOwner, resourceKey);
		
		mainVC = createVelocityContainer("certificate_efficiencystatement");
		populateAssessedIdentityInfos(ureq, courseRepo, businessGroup, links);
		
		if(efficiencyStatement != null && certificate != null) {
			segmentView = SegmentViewFactory.createSegmentView("segments", mainVC, this);
			certificateLink = LinkFactory.createLink("details.certificate", mainVC, this);
			certificateLink.setElementCssClass("o_select_certificate_segement");
			segmentView.addSegment(certificateLink, true);
			selectCertificate(ureq);
			
			courseDetailsLink = LinkFactory.createLink("details.course.infos", mainVC, this);
			courseDetailsLink.setElementCssClass("o_select_statement_segment");
			segmentView.addSegment(courseDetailsLink, false);
		} else if(efficiencyStatement != null) {
			selectCourseInfos(ureq);
		} else if(certificate != null) {
			selectCertificate(ureq);
		}
		
		if(statementOwner.equals(ureq.getIdentity())) {
			EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(EfficiencyStatementArtefact.ARTEFACT_TYPE);
			if(portfolioModule.isEnabled() && artHandler != null && artHandler.isEnabled()) {
				collectArtefactLink = LinkFactory.createCustomLink("collectArtefactLink", "collectartefact", "", Link.NONTRANSLATED, mainVC, this);
				collectArtefactLink.setIconLeftCSS("o_icon o_icon-lg o_icon_eportfolio_add");
			}
		}

		putInitialPanel(mainVC);
		//message, that no data is available. This may happen in the case the "open efficiency" link is available, while in the meantime an author
		//disabled the efficiency statement.
		//String text = translate("efficiencystatement.nodata");
		//Controller messageCtr = MessageUIFactory.createSimpleMessage(ureq, getWindowControl(), text);
		//listenTo(messageCtr);//gets disposed as this controller gets disposed.
		//mainVC.put("assessmentOverviewTable",  messageCtr.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		//
	}

	private void populateAssessedIdentityInfos(UserRequest ureq, RepositoryEntry courseRepo, BusinessGroup group, boolean links) { 
		if(efficiencyStatement != null) {
			mainVC.contextPut("courseTitle", StringHelper.escapeHtml(efficiencyStatement.getCourseTitle()));
			mainVC.contextPut("date", StringHelper.formatLocaleDateTime(efficiencyStatement.getLastUpdated(), ureq.getLocale()));
		} else if(courseRepo != null) {
			mainVC.contextPut("courseTitle", StringHelper.escapeHtml(courseRepo.getDisplayname()));
		}
		
		if(courseRepoEntry != null && links) {
			courseLink = LinkFactory.createButton("course.link", mainVC, this);
			courseLink.setIconLeftCSS("o_icon o_CourseModule_icon");
			mainVC.put("course.link", courseLink);
		}
		
		mainVC.contextPut("user", statementOwner.getUser());			
		mainVC.contextPut("username", statementOwner.getName());
		
		Roles roles = ureq.getUserSession().getRoles();
		boolean isAdministrativeUser = (roles.isAuthor() || roles.isGroupManager() || roles.isUserManager() || roles.isOLATAdmin());	
		List<UserPropertyHandler> userPropertyHandlers = UserManager.getInstance().getUserPropertyHandlersFor(usageIdentifyer, isAdministrativeUser);
		mainVC.contextPut("userPropertyHandlers", userPropertyHandlers);

		if(!getIdentity().equals(statementOwner) && links) {
			homeLink = LinkFactory.createButton("home.link", mainVC, this);
			homeLink.setIconLeftCSS("o_icon o_icon_home");
			mainVC.put("home.link", homeLink);
			
			contactLink = LinkFactory.createButton("contact.link", mainVC, this);
			contactLink.setIconLeftCSS("o_icon o_icon_mail");
			mainVC.put("contact.link", contactLink);
		}

		if(group != null) {
			mainVC.contextPut("groupName", StringHelper.escapeHtml(group.getName()));
			if(links) {
				groupLink = LinkFactory.createButton("group.link", mainVC, this);
				groupLink.setIconLeftCSS("o_icon o_icon_group");
				mainVC.put("group.link", groupLink);
			}
		}
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(source.equals(collectArtefactLink)){
			popupArtefactCollector(ureq);
		} else if (source == homeLink) {
			doOpenHome(ureq);
		} else if (source == courseLink) {
			doOpenCourse(ureq);
		} else if (source == groupLink) {
			doOpenGroup(ureq);
		} else if (source == contactLink) {
			contact(ureq);
		} else if(source == segmentView && event instanceof SegmentViewEvent) {
			SegmentViewEvent sve = (SegmentViewEvent)event;
			if(certificateLink.getComponentName().equals(sve.getComponentName())) {
				selectCertificate(ureq);
			} else if(courseDetailsLink.getComponentName().equals(sve.getComponentName())) {
				selectCourseInfos(ureq);
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == cmc) {
			cleanUp();
		} else if (source == contactCtrl) {
			cmc.deactivate();
			cleanUp();
		}
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(contactCtrl);
		cmc = null;
		contactCtrl = null;
	}
	
	private void selectCertificate(UserRequest ureq) {
		if(certificateCtrl == null) {
			certificateCtrl = new CertificateController(ureq, getWindowControl(), certificate);
			listenTo(certificateCtrl);
		}
		mainVC.put("segmentCmp", certificateCtrl.getInitialComponent());
	}
	
	private void selectCourseInfos(UserRequest ureq) {
		if(courseDetailsCtrl == null) {
			courseDetailsCtrl = new IdentityAssessmentOverviewController(ureq, getWindowControl(), efficiencyStatement.getAssessmentNodes());
			listenTo(courseDetailsCtrl);
		}
		mainVC.put("segmentCmp", courseDetailsCtrl.getInitialComponent());
	}

	private void contact(UserRequest ureq) {
		removeAsListenerAndDispose(cmc);

		ContactMessage cmsg = new ContactMessage(getIdentity());
		ContactList contactList = new ContactList("to");
		contactList.add(statementOwner);
		cmsg.addEmailTo(contactList);
		contactCtrl = new ContactFormController(ureq, getWindowControl(), true, false, false, cmsg);
		listenTo(contactCtrl);
		cmc = new CloseableModalController(getWindowControl(), translate("close"), contactCtrl.getInitialComponent());
		cmc.activate();
		listenTo(cmc);
	}
	
	private void doOpenGroup(UserRequest ureq) {
		if(businessGroup != null) {
			List<ContextEntry> ces = new ArrayList<ContextEntry>(1);
			OLATResourceable ores = OresHelper.createOLATResourceableInstance("BusinessGroup", businessGroup.getKey());
			ces.add(BusinessControlFactory.getInstance().createContextEntry(ores));
	
			BusinessControl bc = BusinessControlFactory.getInstance().createFromContextEntries(ces);
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
			NewControllerFactory.getInstance().launch(ureq, bwControl);
		}
	}
	
	private void doOpenCourse(UserRequest ureq) {
		if(courseRepoEntry != null) {
			List<ContextEntry> ces = new ArrayList<ContextEntry>(1);
			OLATResourceable ores = OresHelper.createOLATResourceableInstance("RepositoryEntry", courseRepoEntry.getKey());
			ces.add(BusinessControlFactory.getInstance().createContextEntry(ores));
	
			BusinessControl bc = BusinessControlFactory.getInstance().createFromContextEntries(ces);
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
			NewControllerFactory.getInstance().launch(ureq, bwControl);
		}
	}
	
	private void doOpenHome(UserRequest ureq) {
		List<ContextEntry> ces = new ArrayList<ContextEntry>(1);
		ces.add(BusinessControlFactory.getInstance().createContextEntry(statementOwner));

		BusinessControl bc = BusinessControlFactory.getInstance().createFromContextEntries(ces);
		WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
		NewControllerFactory.getInstance().launch(ureq, bwControl);
	}

	/**
	 * opens the collect-artefact wizard 
	 * 
	 * @param ureq
	 */
	private void popupArtefactCollector(UserRequest ureq) {
		EPArtefactHandler<?> artHandler = portfolioModule.getArtefactHandler(EfficiencyStatementArtefact.ARTEFACT_TYPE);
		if(artHandler != null && artHandler.isEnabled()) {
			AbstractArtefact artefact = artHandler.createArtefact();
			artefact.setAuthor(getIdentity());//only author can create artefact
			//no business path becouse we cannot launch an efficiency statement
			artefact.setCollectionDate(new Date());
			artefact.setTitle(translate("artefact.title", new String[]{efficiencyStatement.getCourseTitle()}));
			artHandler.prefillArtefactAccordingToSource(artefact, efficiencyStatement);
			ePFCollCtrl = new ArtefactWizzardStepsController(ureq, getWindowControl(), artefact, (VFSContainer)null);
			listenTo(ePFCollCtrl);
			
			//set flag for js-window-resizing (see velocity)
			mainVC.contextPut("collectwizard", true);
		}
	}
}