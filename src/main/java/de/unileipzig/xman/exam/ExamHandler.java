package de.unileipzig.xman.exam;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OLATResourceableJustBeforeDeletedEvent;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.fileresource.types.ResourceEvaluation;
import org.olat.repository.ErrorList;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.handlers.EditionSupport;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.model.RepositoryEntrySecurity;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;

import de.unileipzig.xman.admin.mail.MailManager;
import de.unileipzig.xman.appointment.AppointmentManager;
import de.unileipzig.xman.calendar.CalendarManager;
import de.unileipzig.xman.exam.controllers.ExamMainController;
import de.unileipzig.xman.protocol.Protocol;
import de.unileipzig.xman.protocol.ProtocolManager;

public class ExamHandler implements RepositoryHandler {
	@Override
	public String getSupportedType() {
		return Exam.ORES_TYPE_NAME;
	}

	@Override
	public String getCreateLabelI18nKey() {
		return "new.exam";
	}

	@Override
	public boolean isCreate() {
		return true;
	}

	@Override
	public boolean isPostCreateWizardAvailable() {
		return false;
	}

	@Override
	public boolean supportsLaunch() {
		return true;
	}

	@Override
	public EditionSupport supportsEdit(OLATResourceable resource) {
		return EditionSupport.yes;
	}

	@Override
	public boolean supportsDownload() {
		// TODO not yet implemented
		return false;
	}

	@Override
	public RepositoryEntry createResource(Identity initialAuthor, String displayname, String description, Object createObject, Locale locale) {
		Exam exam = ExamDBManager.getInstance().createExam();
		exam.setName(displayname);
		exam.setIdentity(initialAuthor);
		exam.setComments(description);
		ExamDBManager.getInstance().saveExam(exam);
		OLATResource resource = OLATResourceManager.getInstance().findOrPersistResourceable(exam);
		RepositoryEntry re = CoreSpringFactory.getImpl(RepositoryService.class).create(initialAuthor, null, "", displayname, description, resource, RepositoryEntry.ACC_OWNERS);
		DBFactory.getInstance().commitAndCloseSession();
		return re;
	}

	@Override
	public MainLayoutController createLaunchController(RepositoryEntry re, RepositoryEntrySecurity reSecurity, UserRequest ureq, WindowControl wControl) {
		boolean isStudent = !ureq.getUserSession().getRoles().isGuestOnly();
		boolean canEdit = ureq.getUserSession().getRoles().isOLATAdmin() || ureq.getUserSession().getRoles().isInstitutionalResourceManager();
		
		if(RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ureq.getIdentity(), re)) {
			canEdit = true;
		}
		
		MainLayoutController launchController;
		Exam exam = ExamDBManager.getInstance().findExamByID(re.getOlatResource().getResourceableId());
		if(canEdit) {
			launchController = new ExamMainController(ureq, wControl, exam, ExamMainController.View.LECTURER);
		} else if(isStudent) {
			launchController = new ExamMainController(ureq, wControl, exam, ExamMainController.View.STUDENT);
		} else {
			launchController = new ExamMainController(ureq, wControl, exam, ExamMainController.View.OTHER);
		}
		
		return launchController;
	}

	@Override
	public Controller createEditorController(RepositoryEntry re, UserRequest ureq, WindowControl wControl, TooledStackedPanel toolbar) {
		Exam exam = ExamDBManager.getInstance().findExamByID(re.getOlatResource().getResourceableId());
		Controller editor;
		try {
			editor = new ExamMainController(ureq, wControl, exam, ExamMainController.View.LECTURER, true);
		} catch(AlreadyLockedException e) {
			Translator translator = Util.createPackageTranslator(Exam.class, ureq.getLocale());
			wControl.setInfo(translator.translate("ExamEditorController.alreadyLocked", new String[] { e.getName() }));
			return null;
		}
		return editor;
	}

	@Override
	public RepositoryEntry copy(RepositoryEntry source, RepositoryEntry target) {
		// TODO: currently not implemented

		//old code:
//		Exam oldExam = ExamDBManager.getInstance().findExamByID(res.getResourceableId());
//		Exam newExam = new ExamImpl();
//
//		newExam.setComments(oldExam.getComments());
//		newExam.setEarmarkedEnabled(oldExam.getEarmarkedEnabled());
//		newExam.setIsOral(oldExam.getIsOral());
//		newExam.setIsMultiSubscription(oldExam.getIsMultiSubscription());
//		newExam.setName(oldExam.getName());
//		newExam.setIdentity(ureq.getIdentity()); // set authorship to the copying user
//
//		ExamDBManager.getInstance().saveExam(newExam);
//
//		return ExamDBManager.getInstance().findExamByID(newExam.getKey());

		return null;
	}

	@Override
	public String archive(Identity archiveOnBehalfOf, String archivFilePath, RepositoryEntry repoEntry) {
		// not needed (archive user data upon deletion)
		return null;
	}

	@Override
	public MediaResource getAsMediaResource(OLATResourceable res, boolean backwardsCompatible) {
		throw new AssertException("download not supported");
	}

	@Override
	public StepsMainRunController createWizardController(OLATResourceable res, UserRequest ureq, WindowControl wControl) {
		throw new AssertException("createWizardController not implemented for Exam");
	}

	@Override
	public ResourceEvaluation acceptImport(File file, String filename) {
		// TODO import not yet supported
		return new ResourceEvaluation(false);
	}

	@Override
	public RepositoryEntry importResource(Identity initialAuthor, String initialAuthorAlt, String displayname, String description, boolean withReferences, Locale locale, File file, String filename) {
		throw new AssertException("import not yet supported");
	}

	@Override
	public boolean readyToDelete(OLATResourceable res, Identity identity, Roles roles, Locale locale, ErrorList errors) {
		// TODO delete not supported currently
		return false;
	}

	@Override
	public boolean cleanupOnDelete(OLATResourceable res) {
		// TODO is this method really needed?
		CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(new OLATResourceableJustBeforeDeletedEvent(res), res);

		// delete OLATResource
		OLATResourceManager rm = OLATResourceManager.getInstance();
		OLATResource ores = rm.findResourceable(res);
		rm.deleteOLATResource(ores);

		Exam deleteableExam = ExamDBManager.getInstance().findExamByID(res.getResourceableId());
		List<Protocol> protoList = ProtocolManager.getInstance().findAllProtocolsByExam(deleteableExam);

		// delete Protocols and calendar events
		for ( Protocol p : protoList ) {
			p = ProtocolManager.getInstance().findProtocolByID(p.getKey());

			CalendarManager.getInstance().deleteKalendarEventForExam(deleteableExam, p.getIdentity());

			Locale userLocale = new Locale(p.getIdentity().getUser().getPreferences().getLanguage());
			Translator tmpTranslator = new PackageTranslator(Util.getPackageName(Exam.class), userLocale);
			// Email DeleteExam
			MailManager.getInstance().sendEmail(
				tmpTranslator.translate("ExamHandler.DeleteExam.Subject", new String[] { deleteableExam.getName() }),

				tmpTranslator.translate("ExamHandler.DeleteExam.Body", new String[] { deleteableExam.getName() }),

				p.getIdentity()
			);
			ProtocolManager.getInstance().deleteProtocol(p);
		}

		protoList = null;

		// delete all appointments
		AppointmentManager.getInstance().deleteAllAppointmentsByExam(deleteableExam);

		// delete the exam
		ExamDBManager.getInstance().deleteExam(deleteableExam);

		deleteableExam = null;

		return true;
	}

	@Override
	public VFSContainer getMediaContainer(RepositoryEntry repoEntry) {
		// TODO media not yet supported
		return null;
	}

	@Override
	public LockResult acquireLock(OLATResourceable ores, Identity identity) { return null; }
	@Override
	public void releaseLock(LockResult lockResult) {}
	@Override
	public boolean isLocked(OLATResourceable ores) { return false; }
}