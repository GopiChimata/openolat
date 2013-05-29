package de.unileipzig.xman.exam.controllers;

import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.olat.admin.user.UserSearchController;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.Form;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.components.stack.PopEvent;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.table.TableMultiSelectEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.util.Formatter;
import org.olat.core.util.Util;
import org.olat.user.HomePageConfigManagerImpl;
import org.olat.user.UserInfoMainController;

import de.unileipzig.xman.admin.mail.MailManager;
import de.unileipzig.xman.admin.mail.form.MailForm;
import de.unileipzig.xman.appointment.Appointment;
import de.unileipzig.xman.appointment.AppointmentManager;
import de.unileipzig.xman.appointment.tables.AppointmentLecturerOralTableModel;
import de.unileipzig.xman.comment.CommentManager;
import de.unileipzig.xman.esf.ElectronicStudentFile;
import de.unileipzig.xman.esf.ElectronicStudentFileManager;
import de.unileipzig.xman.esf.form.ESFCommentCreateAndEditForm;
import de.unileipzig.xman.exam.Exam;
import de.unileipzig.xman.exam.ExamDBManager;
import de.unileipzig.xman.exam.forms.EditMarkForm;
import de.unileipzig.xman.protocol.Protocol;
import de.unileipzig.xman.protocol.ProtocolManager;

public class ExamLecturerOralController extends BasicController {
	
	private static final String VELOCITY_ROOT = Util.getPackageVelocityRoot(Exam.class);
	
	private Exam exam;
	private VelocityContainer baseVC;
	private VelocityContainer mainVC;
	
	private TableController appointmentTable;
	private AppointmentLecturerOralTableModel appointmentTableModel;

	private CloseableModalController cmc;
	
	private Link refreshTableButton;

	private UserSearchController userSearchController;
	private Appointment userSearchControllerAppointmentHolder;

	private EditMarkForm editMarkForm;
	private List<Appointment> editMarkFormAppointmentHolder;

	private ESFCommentCreateAndEditForm editCommentForm;
	private List<Appointment> editCommentFormAppointmentHolder;

	private MailForm editMailForm;
	private List<Appointment> editMailFormAppointmentHolder;

	/**
	 * The exam given MUST be oral, otherwise InvalidParameterException is thrown!
	 * 
	 * @param ureq
	 * @param wControl
	 * @param exam The oral exam to manage
	 * @throws InvalidParameterException
	 */
	protected ExamLecturerOralController(UserRequest ureq, WindowControl wControl, StackedController stack, Exam exam) {
		super(ureq, wControl);
		
		if(!exam.getIsOral())
			throw new InvalidParameterException("Expected oral exam, got written one");
		
		setTranslator(Util.createPackageTranslator(Exam.class, ureq.getLocale()));
		this.exam = exam;
		
		listenTo(stack); // listen for pop events
		
		baseVC = new VelocityContainer("examBase", VELOCITY_ROOT + "/examBase.html", getTranslator(), this);
		init(ureq, wControl);
		putInitialPanel(baseVC);
	}
	
	private void init(UserRequest ureq, WindowControl wControl) {
		baseVC.contextPut("examType", translate("oral"));
		baseVC.contextPut("regStartDate", exam.getRegStartDate() == null ? "n/a" : Formatter.getInstance(ureq.getLocale()).formatDateAndTime(exam.getRegStartDate()));
		baseVC.contextPut("regEndDate", exam.getRegEndDate() == null ? "n/a" : Formatter.getInstance(ureq.getLocale()).formatDateAndTime(exam.getRegEndDate()));
		baseVC.contextPut("signOffDate", exam.getSignOffDate() == null ? "n/a" : Formatter.getInstance(ureq.getLocale()).formatDateAndTime(exam.getSignOffDate()));
		baseVC.contextPut("earmarkedEnabled", translate(exam.getEarmarkedEnabled() ? "yes" : "no"));
		baseVC.contextPut("multiSubscriptionEnabled", translate(exam.getIsMultiSubscription() ? "yes" : "no"));
		String comments = exam.getComments();
		baseVC.contextPut("comments", comments.isEmpty() ? translate("examBase_html.comments.isEmpty") : comments);
		
		mainVC = new VelocityContainer("examStudentView", VELOCITY_ROOT + "/examLecturerOralView.html", getTranslator(), this);
		baseVC.put("anyForm", mainVC);

		if(AppointmentManager.getInstance().findAllAppointmentsByExamId(exam.getKey()).size() > 0) {
			mainVC.contextPut("showAppointmentTable", true);
			
			refreshTableButton = LinkFactory.createButton("ExamLecturerWrittenController.refreshTable", mainVC, this);
			
			buildAppointmentTable(ureq, wControl);
		} else {
			mainVC.contextPut("showAppointmentTable", false);
		}
	}
	
	private void buildAppointmentTable(UserRequest ureq, WindowControl wControl) {
		removeAsListenerAndDispose(appointmentTable);
		
		appointmentTableModel = new AppointmentLecturerOralTableModel(exam, ureq.getLocale());
		
		TableGuiConfiguration tableGuiConfiguration = new TableGuiConfiguration();
		tableGuiConfiguration.setColumnMovingOffered(true);
		tableGuiConfiguration.setDownloadOffered(true);
		tableGuiConfiguration.setTableEmptyMessage(translate("ExamEditorController.appointmentTable.empty"));
		tableGuiConfiguration.setMultiSelect(true);
		tableGuiConfiguration.setPreferencesOffered(true, "ExamLecturerOralController.appointmentTable");
		appointmentTable = new TableController(tableGuiConfiguration, ureq, wControl, getTranslator());
		
		appointmentTableModel.createColumns(appointmentTable);
		appointmentTable.setTableDataModel(appointmentTableModel);
		appointmentTable.setSortColumn(appointmentTableModel.getColumnCount(), false); // sort by last, zerobased,  +1 for multiselect
		
		listenTo(appointmentTable);
		
		mainVC.put("appointmentTable", appointmentTable.getInitialComponent());
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		// check that exam was not closed in the meantime
		if(ExamDBManager.getInstance().isClosed(exam)) {
			showInfo("ExamMainController.info.closed");
			return;
		}

		if(event instanceof PopEvent) {
			// reload exam
			exam = ExamDBManager.getInstance().findExamByID(exam.getKey());
			// complete rebuild
			init(ureq, getWindowControl());
		}
		
		if(source == appointmentTable) {
			if(event instanceof TableEvent) {
				TableEvent tableEvent = (TableEvent) event;
				
				/**
				 * open vcard of selected user
				 */
				if(tableEvent.getActionId().equals(AppointmentLecturerOralTableModel.ACTION_USER)) {
					Protocol p = appointmentTableModel.getProtocol(appointmentTableModel.getObject(tableEvent.getRowId()));
					
					OLATResourceable ores = HomePageConfigManagerImpl.getInstance().loadConfigFor(p.getIdentity().getName());

					DTabs dts = (DTabs) Windows.getWindows(ureq).getWindow(ureq).getAttribute("DTabs");
					DTab dt = dts.getDTab(ores);
					if (dt == null) {
						// does not yet exist
						dt = dts.createDTab(ores, p.getIdentity().getName());
						if (dt == null) return;
						UserInfoMainController uimc = new UserInfoMainController(ureq, dt.getWindowControl(), p.getIdentity());
						dt.setController(uimc);
						dts.addDTab(ureq, dt);
					}
					dts.activate(ureq, dt, null);
				}
			} else if(event instanceof TableMultiSelectEvent) {
				TableMultiSelectEvent tableEvent = (TableMultiSelectEvent) event;
				
				/**
				 * add student manually to exam
				 */
				if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_ADD)) {
					if(tableEvent.getSelection().cardinality() != 1) {
						showError("ExamLecturerOralController.error.selectOne");
						return;
					}
					
					// Guaranteed to work because we checked that exactly one is selected
					userSearchControllerAppointmentHolder = appointmentTableModel.getObjects(tableEvent.getSelection()).get(0);
					
					if(appointmentTableModel.existsProtocol(userSearchControllerAppointmentHolder)) {
						showInfo("ExamLecturerOralController.error.appNotAvailable");
						return;
					}
					
					removeAsListenerAndDispose(userSearchController);
					userSearchController = new UserSearchController(ureq, getWindowControl(), false, false);
					listenTo(userSearchController);
					
					cmc = new CloseableModalController(this.getWindowControl(), translate("close"), userSearchController.getInitialComponent());
					cmc.activate();
				
				/**
				 * create form to edit result (grade)
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_EDIT_RESULT)) {
					editMarkFormAppointmentHolder = filterAppointmentsByType(appointmentTableModel.getObjects(tableEvent.getSelection()), true);
					
					if(editMarkFormAppointmentHolder.isEmpty()) {
						showInfo("ExamLecturerOralController.info.selectOneWithProtocol");
						return;
					}
					
					removeAsListenerAndDispose(editMarkForm);
					editMarkForm = new EditMarkForm(ureq, getWindowControl(), "editMarkForm", getTranslator());
					listenTo(editMarkForm);
					
					cmc = new CloseableModalController(this.getWindowControl(), translate("close"), editMarkForm.getInitialComponent());
					cmc.activate();
				
				/**
				 * create form to add comments of edit if chosen only one
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_EDIT_COMMENT)) {
					editCommentFormAppointmentHolder = filterAppointmentsByType(appointmentTableModel.getObjects(tableEvent.getSelection()), true);
					String defaultText = "";
					
					if(editCommentFormAppointmentHolder.isEmpty()) {
						showInfo("ExamLecturerOralController.info.selectOneWithProtocol");
						return;
					}
					
					if(tableEvent.getSelection().cardinality() == 1) {
						if(appointmentTableModel.existsProtocol(editCommentFormAppointmentHolder.get(0)))
							defaultText = appointmentTableModel.getProtocol(editCommentFormAppointmentHolder.get(0)).getComments();
					}
					
					removeAsListenerAndDispose(editCommentForm);
					editCommentForm = new ESFCommentCreateAndEditForm(ureq, getWindowControl(), "editCommentForm", getTranslator(), defaultText);
					listenTo(editCommentForm);
					
					cmc = new CloseableModalController(this.getWindowControl(), translate("close"), editCommentForm.getInitialComponent());
					cmc.activate();
				
				/**
				 * create form to send emails to students
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_MAIL)) {
					editMailFormAppointmentHolder = filterAppointmentsByType(appointmentTableModel.getObjects(tableEvent.getSelection()), true);
					
					if(editMailFormAppointmentHolder.isEmpty()) {
						showInfo("ExamLecturerOralController.info.selectOneWithProtocol");
						return;
					}
					
					ArrayList<String> recipients = new ArrayList<String>();
					for(Appointment app : editMailFormAppointmentHolder) {
						recipients.add(getName(appointmentTableModel.getProtocol(app).getIdentity()));
					}
					
					removeAsListenerAndDispose(editMailForm);
					editMailForm = new MailForm(ureq, getWindowControl(), "editMailForm", getTranslator(), recipients.toArray(new String[0]));
					listenTo(editMailForm);
					
					cmc = new CloseableModalController(this.getWindowControl(), translate("close"), editMailForm.getInitialComponent());
					cmc.activate();
					
				/**
				 * change status of selected students to earmarked
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_EARMARK)) {
					List<Appointment> apps = appointmentTableModel.getObjects(tableEvent.getSelection());
					
					for(Appointment app : apps) {
						if(appointmentTableModel.existsProtocol(app)) {
							Protocol proto = ProtocolManager.getInstance().findProtocolByID(appointmentTableModel.getProtocol(app).getKey());
							proto.setEarmarked(true);
							ProtocolManager.getInstance().updateProtocol(proto);
							
							Translator userTranslator = Util.createPackageTranslator(Exam.class, new Locale(proto.getIdentity().getUser().getPreferences().getLanguage()));
							BusinessControlFactory bcf = BusinessControlFactory.getInstance();

							// Email MoveToEarmarked
							MailManager.getInstance().sendEmail(
								userTranslator.translate("Mail.MoveToEarmarked.Subject", new String[] { exam.getName() }),
								userTranslator.translate("Mail.MoveToEarmarked.Body",
									new String[] {
										exam.getName(),
										getName(proto.getIdentity()),
										DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, userTranslator.getLocale()).format(proto.getAppointment().getDate()),
										proto.getAppointment().getPlace(),
										new Integer(proto.getAppointment().getDuration()).toString(),
										userTranslator.translate("oral"),
										bcf.getAsURIString(bcf.createCEListFromString(ExamDBManager.getInstance().findRepositoryEntryOfExam(exam)), true)
									}),
								proto.getIdentity()
							);

							// add a comment to the esf
							String commentText = translate("ExamLecturerOralController.earmarkedStudentManually", new String[] { getName(ureq.getIdentity()), exam.getName() });
							
							CommentManager.getInstance().createCommentInEsa(ElectronicStudentFileManager.getInstance().retrieveESFByIdentity(proto.getIdentity()), commentText, ureq.getIdentity());
						}
					}
					
					// update view
					appointmentTableModel.update();
					appointmentTable.modelChanged();
				
				/**
				 * change status of selected users to registered
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_REGISTER)) {
					List<Appointment> apps = appointmentTableModel.getObjects(tableEvent.getSelection());
					
					for(Appointment app : apps) {
						if(appointmentTableModel.existsProtocol(app)) {
							Protocol proto = ProtocolManager.getInstance().findProtocolByID(appointmentTableModel.getProtocol(app).getKey());
							proto.setEarmarked(false);
							ProtocolManager.getInstance().updateProtocol(proto);
							
							Translator userTranslator = Util.createPackageTranslator(Exam.class, new Locale(proto.getIdentity().getUser().getPreferences().getLanguage()));
							BusinessControlFactory bcf = BusinessControlFactory.getInstance();

							// calculate semester
							String semester;
							Calendar cal = Calendar.getInstance();
							cal.setTime(app.getDate());
							if (cal.get(Calendar.MONTH) >= 3 && cal.get(Calendar.MONTH) <= 8)
								semester = "SS " + cal.get(Calendar.YEAR);
							else
								semester = "WS " + cal.get(Calendar.YEAR) + "/" + (cal.get(Calendar.YEAR) + 1);
							
							// Email Register
							MailManager.getInstance().sendEmail(
								userTranslator.translate("Mail.Register.Subject", new String[] { exam.getName() }),
								userTranslator.translate("Mail.Register.Body",
									new String[] {
										exam.getName(),
										getName(proto.getIdentity()),
										DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, userTranslator.getLocale()).format(proto.getAppointment().getDate()),
										proto.getAppointment().getPlace(),
										new Integer(proto.getAppointment().getDuration()).toString(),
										userTranslator.translate("oral"),
										bcf.getAsURIString(bcf.createCEListFromString(ExamDBManager.getInstance().findRepositoryEntryOfExam(exam)), true),
										userTranslator.translate(proto.getEarmarked() ? "ExamLecturerOralController.status.earmarked" : "ExamLecturerOralController.status.registered"),
										semester,
										proto.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALEMAIL, null),
										proto.getIdentity().getUser().getProperty(UserConstants.EMAIL, null),
										proto.getIdentity().getUser().getProperty(UserConstants.STUDYSUBJECT, null)
									}),
								proto.getIdentity()
							);

							// add a comment to the esf
							String commentText = translate("ExamLecturerOralController.registeredFromEarmarkedStudentManually", new String[] { getName(ureq.getIdentity()), exam.getName() });
							
							CommentManager.getInstance().createCommentInEsa(ElectronicStudentFileManager.getInstance().retrieveESFByIdentity(proto.getIdentity()), commentText, ureq.getIdentity());
						}
					}
					
					// update view
					appointmentTableModel.update();
					appointmentTable.modelChanged();
				
				/**
				 *  remove selected users from exam
				 */
				} else if(tableEvent.getAction().equals(AppointmentLecturerOralTableModel.ACTION_MULTI_UNREGISTER)) {
					List<Appointment> apps = appointmentTableModel.getObjects(tableEvent.getSelection());
					
					for(Appointment app : apps) {
						if(appointmentTableModel.existsProtocol(app)) {
							Protocol proto = ProtocolManager.getInstance().findProtocolByID(appointmentTableModel.getProtocol(app).getKey());
							
							Translator userTranslator = Util.createPackageTranslator(Exam.class, new Locale(proto.getIdentity().getUser().getPreferences().getLanguage()));
							BusinessControlFactory bcf = BusinessControlFactory.getInstance();

							// Email Remove
							MailManager.getInstance().sendEmail(
								userTranslator.translate("Mail.Remove.Subject", new String[] { exam.getName() }),
								userTranslator.translate("Mail.Remove.Body",
									new String[] {
										// exam name, user name, exam date, exam location, exam duration, oral/written, link
										exam.getName(),
										getName(proto.getIdentity()),
										DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, userTranslator.getLocale()).format(proto.getAppointment().getDate()),
										proto.getAppointment().getPlace(),
										new Integer(proto.getAppointment().getDuration()).toString(),
										userTranslator.translate("oral"),
										bcf.getAsURIString(bcf.createCEListFromString(ExamDBManager.getInstance().findRepositoryEntryOfExam(exam)), true)
									}),
								proto.getIdentity()
							);
							
							// update appointment
							Appointment tempApp = AppointmentManager.getInstance().findAppointmentByID(proto.getAppointment().getKey());
							tempApp.setOccupied(false);
							AppointmentManager.getInstance().updateAppointment(tempApp);
							tempApp = null;
							
							// delete protocol
							ProtocolManager.getInstance().deleteProtocol(proto);
							
							// add a comment to the esf
							String commentText = translate("ExamLecturerOralController.removedStudentManually", new String[] { getName(ureq.getIdentity()), exam.getName() });
							
							CommentManager.getInstance().createCommentInEsa(ElectronicStudentFileManager.getInstance().retrieveESFByIdentity(proto.getIdentity()), commentText, ureq.getIdentity());
						}
					}
					
					// update view
					appointmentTableModel.update();
					appointmentTable.modelChanged();
				}
			}
		
		/**
		 * subscribe student to exam manually
		 */
		} else if(source == userSearchController) {
			if(event instanceof SingleIdentityChosenEvent) {
				// close modal
				cmc.deactivate();
				
				SingleIdentityChosenEvent searchEvent = (SingleIdentityChosenEvent) event;				
				ElectronicStudentFile esf = ElectronicStudentFileManager.getInstance().retrieveESFByIdentity(searchEvent.getChosenIdentity());
				
				assert(userSearchControllerAppointmentHolder != null);
				
				if(appointmentTableModel.existsProtocol(userSearchControllerAppointmentHolder)) {
					showError("ExamLecturerOralController.error.appNotAvailable");
					return;
				}
				
				if (esf != null) {
					if(ProtocolManager.getInstance().registerStudent(userSearchControllerAppointmentHolder, esf, getTranslator(), false, "")) {
						// create comment in esf
						String commentText = translate("ExamLecturerOralController.registeredStudentManually", new String[] { getName(ureq.getIdentity()), exam.getName()});
						
						CommentManager.getInstance().createCommentInEsa(esf, commentText, ureq.getIdentity());
						
						// update view
						appointmentTableModel.update();
						appointmentTable.modelChanged();
					}
				} else {
					showError("ExamLecturerOralController.error.studentHasNoESF");
				}
				
				userSearchControllerAppointmentHolder = null;
			}
		
		/**
		 * process edit marks form
		 */
		} else if(source == editMarkForm) {
			if(event == Form.EVNT_VALIDATION_OK) {
				cmc.deactivate();
				
				for (Appointment app : editMarkFormAppointmentHolder) {
					if(appointmentTableModel.existsProtocol(app)) {
						Protocol proto = ProtocolManager.getInstance().findProtocolByID(appointmentTableModel.getProtocol(app).getKey());
						proto.setGrade(editMarkForm.getGrade());
						ProtocolManager.getInstance().updateProtocol(proto);
						
						Translator userTranslator = Util.createPackageTranslator(Exam.class, new Locale(proto.getIdentity().getUser().getPreferences().getLanguage()));
						BusinessControlFactory bcf = BusinessControlFactory.getInstance();
						
						// Email GetMark
						MailManager.getInstance().sendEmail(
							userTranslator.translate("Mail.GetMark.Subject", new String[] { exam.getName() }),
							userTranslator.translate("Mail.GetMark.Body",
								new String[] {
									exam.getName(),
									getName(proto.getIdentity()),
									DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, userTranslator.getLocale()).format(proto.getAppointment().getDate()),
									proto.getAppointment().getPlace(),
									new Integer(proto.getAppointment().getDuration()).toString(),
									userTranslator.translate("oral"),
									bcf.getAsURIString(bcf.createCEListFromString(ExamDBManager.getInstance().findRepositoryEntryOfExam(exam)), true)
								}),
							proto.getIdentity()
						);
					}
				}
				
				editMarkFormAppointmentHolder = null;

				// update view
				appointmentTableModel.update();
				appointmentTable.modelChanged();
			}
		
		/**
		 * process edit comments form
		 */
		} else if(source == editCommentForm) {
			if(event == Form.EVNT_VALIDATION_OK) {
				cmc.deactivate();
				
				for (Appointment app : editCommentFormAppointmentHolder) {
					if(appointmentTableModel.existsProtocol(app)) {
						Protocol proto = ProtocolManager.getInstance().findProtocolByID(appointmentTableModel.getProtocol(app).getKey());
						proto.setComments(editCommentForm.getComment());
						ProtocolManager.getInstance().updateProtocol(proto);
					}
				}
				
				editCommentFormAppointmentHolder = null;

				// update view
				appointmentTableModel.update();
				appointmentTable.modelChanged();
			}
		
		/**
		 * send mails to students and save them in their esf
		 */
		} else if(source == editMailForm) {
			if(event == Form.EVNT_VALIDATION_OK) {
				cmc.deactivate();
				
				String subject = editMailForm.getSubject();
				String body = editMailForm.getBody();
			
				for (Appointment app : editMailFormAppointmentHolder) {
					if(appointmentTableModel.existsProtocol(app)) {
						Protocol proto = appointmentTableModel.getProtocol(app);
						
						MailManager.getInstance().sendEmail(subject, body, proto.getIdentity());
						
						CommentManager.getInstance().createCommentInEsa(ElectronicStudentFileManager.getInstance().retrieveESFByIdentity(proto.getIdentity()), "E-Mail: " + subject + "\n" + body, ureq.getIdentity());
					}
				}
				
				editMailFormAppointmentHolder = null;
			}
		}
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		// check that exam was not closed in the meantime
		if(ExamDBManager.getInstance().isClosed(exam)) {
			showInfo("ExamMainController.info.closed");
			return;
		}

		if(source == refreshTableButton) {
			// update view
			appointmentTableModel.update();
			appointmentTable.modelChanged();
		}
	}

	@Override
	protected void doDispose() {
		removeAsListenerAndDispose(appointmentTable);
		removeAsListenerAndDispose(userSearchController);
		removeAsListenerAndDispose(editMarkForm);
		removeAsListenerAndDispose(editCommentForm);
		removeAsListenerAndDispose(editMailForm);
	}
	
	protected String getName(Identity id) {
		return id.getUser().getProperty(UserConstants.FIRSTNAME, null) + " " + id.getUser().getProperty(UserConstants.LASTNAME, null);
	}
	
	protected List<Appointment> filterAppointmentsByType(List<Appointment> oldList, boolean withProtocol) {
		List<Appointment> filteredList = new ArrayList<Appointment>();
		
		for(Appointment app : oldList) {
			if(withProtocol == appointmentTableModel.existsProtocol(app))
				filteredList.add(app);
		}
		
		return filteredList;
	}

}