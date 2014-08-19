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
package org.olat.repository.ui.author;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.olat.NewControllerFactory;
import org.olat.admin.user.UserSearchController;
import org.olat.basesecurity.GroupRoles;
import org.olat.basesecurity.events.MultiIdentityChosenEvent;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.commons.services.mark.Mark;
import org.olat.core.commons.services.mark.MarkManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableElement;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableFilter;
import org.olat.core.gui.components.form.flexible.elements.FlexiTableSortOptions;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.BooleanCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DateFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.DefaultFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataModelFactory;
import org.olat.core.gui.components.form.flexible.impl.elements.table.SelectionEvent;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiCellRenderer;
import org.olat.core.gui.components.form.flexible.impl.elements.table.StaticFlexiColumnModel;
import org.olat.core.gui.components.form.flexible.impl.elements.table.TextFlexiCellRenderer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.stack.TooledStackedPanel;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableCalloutWindowController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.wizard.StepsMainRunController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.LockResult;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CorruptedCourseException;
import org.olat.course.run.RunMainController;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryModule;
import org.olat.repository.RepositoryService;
import org.olat.repository.controllers.WizardCloseResourceController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;
import org.olat.repository.handlers.RepositoryHandlerFactory.OrderedRepositoryHandler;
import org.olat.repository.model.SearchAuthorRepositoryEntryViewParams;
import org.olat.repository.model.SearchAuthorRepositoryEntryViewParams.OrderBy;
import org.olat.repository.ui.RepositoyUIFactory;
import org.olat.repository.ui.author.AuthoringEntryDataModel.Cols;
import org.olat.user.UserManager;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Initial date: 28.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class AuthorListController extends FormBasicController implements Activateable2, AuthoringEntryDataSourceUIFactory {

	private final String i18nName;
	private FlexiTableElement tableEl;
	private final TooledStackedPanel stackPanel;
	
	private boolean withSearch;
	
	private AuthoringEntryDataModel model;
	private AuthoringEntryDataSource dataSource;
	private final SearchAuthorRepositoryEntryViewParams searchParams;

	private ToolsController toolsCtrl;
	private CloseableModalController cmc;
	private StepsMainRunController wizardCtrl;
	private AuthorSearchController searchCtrl;
	private UserSearchController userSearchCtr;
	private ImportRepositoryEntryController importCtrl;
	private CreateRepositoryEntryController createCtrl;
	private CloseableCalloutWindowController toolsCalloutCtrl;

	private DialogBoxController deleteDialogCtrl;
	private WizardCloseResourceController wc;
	private CopyRepositoryEntryController copyCtrl;

	private Link importLink;
	private Dropdown createDropdown;
	private FormLink addOwnersButton;

	private LockResult lockResult;
	private final AtomicInteger counter = new AtomicInteger();
	//only used as marker for dirty, model cannot load specific rows
	private final List<Integer> dirtyRows = new ArrayList<>();
	
	@Autowired
	private UserManager userManager;
	@Autowired
	private MarkManager markManager;
	@Autowired
	private RepositoryModule repositoryModule;
	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RepositoryHandlerFactory repositoryHandlerFactory;
	
	public AuthorListController(UserRequest ureq, WindowControl wControl, String i18nName,
			SearchAuthorRepositoryEntryViewParams searchParams, boolean withSearch) {
		super(ureq, wControl, "entries");
		setTranslator(Util.createPackageTranslator(RepositoryService.class, getLocale(), getTranslator()));

		this.i18nName = i18nName;
		this.withSearch = withSearch;
		this.searchParams = searchParams;

		dataSource = new AuthoringEntryDataSource(searchParams, this);
		initForm(ureq);

		stackPanel = new TooledStackedPanel(i18nName, getTranslator(), this);
		stackPanel.pushController(translate(i18nName), this);
		if(!withSearch) {
			importLink = LinkFactory.createLink("cmd.import.ressource", getTranslator(), this);
			importLink.setDomReplacementWrapperRequired(false);
			importLink.setIconLeftCSS("o_icon o_icon_import");
			stackPanel.addTool(importLink, Align.left);
			
			Collection<String> types = repositoryHandlerFactory.getSupportedTypes();
			createDropdown = new Dropdown("cmd.create.ressource", "cmd.create.ressource", false, getTranslator());
			createDropdown.setElementCssClass("o_sel_author_create");
			createDropdown.setIconCSS("o_icon o_icon_add");
			for(String type:types) {
				RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(type);
				if(handler != null && handler.isCreate()) {
					addCreateLink(handler, createDropdown);
				}
			}
			stackPanel.addTool(createDropdown, Align.left);
		}
	}
	
	@Override
	public Translator getTranslator() {
		return super.getTranslator();
	}

	private void addCreateLink(RepositoryHandler handler, Dropdown dropdown) {
		Link createLink = LinkFactory.createLink(handler.getSupportedType(), getTranslator(), this);
		createLink.setIconLeftCSS("o_icon o_icon-fw " + RepositoyUIFactory.getIconCssClass(handler.getSupportedType()));
		createLink.setElementCssClass("o_sel_author_create-" + handler.getSupportedType());
		createLink.setUserObject(handler);
		dropdown.addComponent(createLink);
	}
	
	public boolean isEmpty() {
		return dataSource.getRowCount() == 0;
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		//search form
		if(withSearch) {
			setFormDescription("table.search.author.desc");
			searchCtrl = new AuthorSearchController(ureq, getWindowControl(), true, mainForm);
			searchCtrl.setEnabled(false);
			listenTo(searchCtrl);
		}
		
		//add the table
		FlexiTableColumnModel columnsModel = FlexiTableDataModelFactory.createFlexiTableColumnModel();
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.key.i18nKey(), Cols.key.ordinal(), true, OrderBy.key.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.mark.i18nKey(), Cols.mark.ordinal(), true, OrderBy.favorit.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.type.i18nKey(), Cols.type.ordinal(), true, OrderBy.type.name(),
				FlexiColumnModel.ALIGNMENT_LEFT, new TypeRenderer()));
		FlexiCellRenderer renderer = new StaticFlexiCellRenderer("select", new TextFlexiCellRenderer());
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel(Cols.displayName.i18nKey(), Cols.displayName.ordinal(), "select",
				true, OrderBy.displayname.name(), renderer));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.authors.i18nKey(), Cols.authors.ordinal(),
				true, OrderBy.authors.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lifecycleLabel.i18nKey(), Cols.lifecycleLabel.ordinal(),
				true, OrderBy.lifecycleLabel.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lifecycleSoftkey.i18nKey(), Cols.lifecycleSoftkey.ordinal(),
				true, OrderBy.lifecycleSoftkey.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.lifecycleStart.i18nKey(), Cols.lifecycleStart.ordinal(),
				true, OrderBy.lifecycleStart.name(), FlexiColumnModel.ALIGNMENT_LEFT, new DateFlexiCellRenderer(getLocale())));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.lifecycleEnd.i18nKey(), Cols.lifecycleEnd.ordinal(),
				true, OrderBy.lifecycleEnd.name(), FlexiColumnModel.ALIGNMENT_LEFT, new DateFlexiCellRenderer(getLocale())));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.author.i18nKey(), Cols.author.ordinal(),
				true, OrderBy.author.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.access.i18nKey(), Cols.access.ordinal(),
				true, OrderBy.access.name(), FlexiColumnModel.ALIGNMENT_LEFT, new AccessRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(true, Cols.ac.i18nKey(), Cols.ac.ordinal(),
				true, OrderBy.ac.name(), FlexiColumnModel.ALIGNMENT_LEFT, new ACRenderer()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.creationDate.i18nKey(), Cols.creationDate.ordinal(),
				true, OrderBy.creationDate.name()));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(false, Cols.lastUsage.i18nKey(), Cols.lastUsage.ordinal(),
				true, OrderBy.lastUsage.name()));
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel("details", -1, "details",
				new StaticFlexiCellRenderer("" /* translate("details")*/, "details", "o_icon-lg o_icon_details")));
		columnsModel.addFlexiColumnModel(new DefaultFlexiColumnModel(Cols.tools.i18nKey(), Cols.tools.ordinal()));
		columnsModel.addFlexiColumnModel(new StaticFlexiColumnModel(Cols.editionSupported.i18nKey(), Cols.editionSupported.ordinal(), "edit",
				new BooleanCellRenderer(new StaticFlexiCellRenderer("" /* translate("edit") */, "edit", "o_icon-lg o_icon_edit"), null)));
		
		model = new AuthoringEntryDataModel(dataSource, columnsModel);
		tableEl = uifactory.addTableElement(getWindowControl(), "table", model, 20, false, getTranslator(), formLayout);
		tableEl.setSearchEnabled(withSearch);
		tableEl.setExportEnabled(true);
		tableEl.setExtendedSearch(searchCtrl);
		tableEl.setCustomizeColumns(true);
		tableEl.setElementCssClass("o_coursetable");
		tableEl.setMultiSelect(true);
		tableEl.setSelectAllEnable(true);
		tableEl.setEmtpyTableMessageKey("table.sEmptyTable");
		tableEl.setSortSettings(new FlexiTableSortOptions(true, new SortKey(OrderBy.displayname.name(), true)));
		tableEl.setAndLoadPersistedPreferences(ureq, "authors-list-" + i18nName);
		if(!withSearch) {
			tableEl.reloadData();
			tableEl.setFilters(null, getFilters());
		}
		
		addOwnersButton = uifactory.addFormLink("tools.add.owners", formLayout, Link.BUTTON);
	}
	
	private List<FlexiTableFilter> getFilters() {
		List<OrderedRepositoryHandler> supportedHandlers = repositoryHandlerFactory.getOrderRepositoryHandlers();
		List<FlexiTableFilter> resources = new ArrayList<>(supportedHandlers.size() + 1);
		for(OrderedRepositoryHandler handler:supportedHandlers) {
			String type = handler.getHandler().getSupportedType();
			String inconLeftCss = RepositoyUIFactory.getIconCssClass(type);
			resources.add(new FlexiTableFilter(translate(type), type, inconLeftCss));
		}
		return resources;
	}
	
	public TooledStackedPanel getStackPanel() {
		return stackPanel;
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
	}
	
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if(importLink == source) {
			doImport(ureq);
		} else if(source instanceof Link && ((Link)source).getUserObject() instanceof RepositoryHandler) {
			RepositoryHandler handler = (RepositoryHandler)((Link)source).getUserObject();
			if(handler != null) {
				doCreate(ureq, handler);
			}
		}
		super.event(ureq, source, event);
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(cmc == source) {
			cleanUp();
		} else if(createCtrl == source) {
			cmc.deactivate();
			if(Event.DONE_EVENT.equals(event)) {
				launchEditDescription(ureq, createCtrl.getAddedEntry());
				cleanUp();
			} else if(CreateRepositoryEntryController.CREATION_WIZARD.equals(event)) {
				doPostCreateWizard(ureq, createCtrl.getAddedEntry(), createCtrl.getHandler());
			} else {
				cleanUp();
			}
		}  else if(importCtrl == source) {
			cmc.deactivate();
			if(Event.DONE_EVENT.equals(event)) {
				launchEditDescription(ureq, importCtrl.getImportedEntry());
				cleanUp();
			} else {
				cleanUp();
			}
		} else if(wizardCtrl == source) {
			if (event.equals(Event.CHANGED_EVENT) || event.equals(Event.CANCELLED_EVENT)) {
				getWindowControl().pop();
				RepositoryEntry newEntry = (RepositoryEntry)wizardCtrl.getRunContext().get("authoringNewEntry");
				cleanUp();
				launchEditDescription(ureq, newEntry);
			}
		} else if(searchCtrl == source) {
			if(event instanceof SearchEvent) {
				SearchEvent se = (SearchEvent)event;
				doSearch(se);
			} else if(event == Event.CANCELLED_EVENT) {
				searchParams.setResourceTypes(null);
				searchParams.setIdAndRefs(null);
				searchParams.setAuthor(null);
				searchParams.setDisplayname(null);
				searchParams.setDescription(null);
			}
		} else if(userSearchCtr == source) {
			@SuppressWarnings("unchecked")
			List<AuthoringEntryRow> rows = (List<AuthoringEntryRow>)userSearchCtr.getUserObject();
			if (event instanceof MultiIdentityChosenEvent) {
				MultiIdentityChosenEvent mice = (MultiIdentityChosenEvent) event; 
				doAddOwners(mice.getChosenIdentities(), rows);
			} else if (event instanceof SingleIdentityChosenEvent) {
				SingleIdentityChosenEvent  sice = (SingleIdentityChosenEvent) event;
				List<Identity> futureOwners = Collections.singletonList(sice.getChosenIdentity());
				doAddOwners(futureOwners, rows);
			}
			cmc.deactivate();
			cleanUp();
		} else if(toolsCtrl == source) {
			if(event == Event.DONE_EVENT) {
				toolsCalloutCtrl.deactivate();
				cleanUp();
			}
		}
		super.event(ureq, source, event);
	}
	
	private void cleanUp() {
		removeAsListenerAndDispose(toolsCalloutCtrl);
		removeAsListenerAndDispose(userSearchCtr);
		removeAsListenerAndDispose(createCtrl);
		removeAsListenerAndDispose(importCtrl);
		removeAsListenerAndDispose(wizardCtrl);
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(cmc);
		toolsCalloutCtrl = null;
		userSearchCtr = null;
		createCtrl = null;
		importCtrl = null;
		wizardCtrl = null;
		toolsCtrl = null;
		cmc = null;
	}
	
	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(addOwnersButton == source) {
			Set<Integer> selections = tableEl.getMultiSelectedIndex();
			if(selections.isEmpty()) {
				//get rows
			} else {
				List<AuthoringEntryRow> rows = new ArrayList<>();
				for(Integer i:selections) {
					AuthoringEntryRow row = model.getObject(i.intValue());
					if(row != null) {
						rows.add(row);
					}
				}
				doAddOwners(ureq, rows);
			}
		} else if(source instanceof FormLink) {
			FormLink link = (FormLink)source;
			String cmd = link.getCmd();
			if("mark".equals(cmd)) {
				AuthoringEntryRow row = (AuthoringEntryRow)link.getUserObject();
				boolean marked = doMark(row);
				link.setIconLeftCSS(marked ? "o_icon o_icon_bookmark o_icon-lg" : "o_icon o_icon_bookmark_add o_icon-lg");
				link.setTitle(translate(marked ? "details.bookmark.remove" : "details.bookmark"));
				link.getComponent().setDirty(true);
				row.setMarked(marked);
			} else if("tools".equals(cmd)) {
				AuthoringEntryRow row = (AuthoringEntryRow)link.getUserObject();
				doOpenTools(ureq, row, link);
			}
		} else if(source == tableEl) {
			if(event instanceof SelectionEvent) {
				SelectionEvent se = (SelectionEvent)event;
				String cmd = se.getCommand();
				AuthoringEntryRow row = model.getObject(se.getIndex());
				if("details".equals(cmd)) {
					launchDetails(ureq, row);
				} else if("edit".equals(cmd)) {
					launchEditor(ureq, row);
				} else if("select".equals(cmd)) {
					launch(ureq, row);
				}
			}
		} 
		super.formInnerEvent(ureq, source, event);
	}

	@Override
	protected void propagateDirtinessToContainer(FormItem fiSrc) {
		//do not update the 
	}
	
	protected void reloadDirtyRows() {
		if(dirtyRows.size() > 0) {
			tableEl.reloadData();
			dirtyRows.clear();
		}
	}

	private void doOpenTools(UserRequest ureq, AuthoringEntryRow row, FormLink link) {
		removeAsListenerAndDispose(toolsCtrl);
		removeAsListenerAndDispose(toolsCalloutCtrl);

		toolsCtrl = new ToolsController(ureq, getWindowControl(), row);
		listenTo(toolsCtrl);

		toolsCalloutCtrl = new CloseableCalloutWindowController(ureq, getWindowControl(),
				toolsCtrl.getInitialComponent(), link.getFormDispatchId(), "", true, "");
		listenTo(toolsCalloutCtrl);
		toolsCalloutCtrl.activate();
	}
	
	private void doImport(UserRequest ureq) {
		if(importCtrl != null) return;

		removeAsListenerAndDispose(importCtrl);
		importCtrl = new ImportRepositoryEntryController(ureq, getWindowControl());
		listenTo(importCtrl);
		removeAsListenerAndDispose(cmc);
		
		String title = translate("cmd.import.ressource");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), importCtrl.getInitialComponent(),
				true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCreate(UserRequest ureq, RepositoryHandler handler) {
		if(createCtrl != null) return;

		removeAsListenerAndDispose(createCtrl);
		createCtrl = new CreateRepositoryEntryController(ureq, getWindowControl(), handler);
		listenTo(createCtrl);
		removeAsListenerAndDispose(cmc);
		
		String title = translate(handler.getCreateLabelI18nKey());
		cmc = new CloseableModalController(getWindowControl(), translate("close"), createCtrl.getInitialComponent(),
				true, title);
		cmc.setCustomWindowCSS("o_sel_author_create_popup");
		listenTo(cmc);
		cmc.activate();
	}
	
	protected boolean doMark(AuthoringEntryRow row) {
		OLATResourceable item = OresHelper.createOLATResourceableInstance("RepositoryEntry", row.getKey());
		if(markManager.isMarked(item, getIdentity(), null)) {
			markManager.removeMark(item, getIdentity(), null);
			return false;
		} else {
			String businessPath = "[RepositoryEntry:" + item.getResourceableId() + "]";
			markManager.setMark(item, getIdentity(), null, businessPath);
			return true;
		}
	}
	
	private void doPostCreateWizard(UserRequest ureq, RepositoryEntry newEntry, RepositoryHandler handler) {
		if(wizardCtrl != null) return;
		
		cleanUp();
		wizardCtrl = handler.createWizardController(newEntry, ureq, getWindowControl());
		wizardCtrl.getRunContext().put("authoringNewEntry", newEntry);
		listenTo(wizardCtrl);
		getWindowControl().pushAsModalDialog(wizardCtrl.getInitialComponent());
	}
	
	private void doSearch(SearchEvent se) {
		if(StringHelper.containsNonWhitespace(se.getType())) {
			searchParams.setResourceTypes(Collections.singletonList(se.getType()));
		} else {
			searchParams.setResourceTypes(null);
		}

		searchParams.setIdAndRefs(se.getId());
		searchParams.setAuthor(se.getAuthor());
		searchParams.setDisplayname(se.getDisplayname());
		searchParams.setDescription(se.getDescription());
		tableEl.reset();
	}
	
	private void doAddOwners(UserRequest ureq, List<AuthoringEntryRow> rows) {
		if(userSearchCtr != null) return;
		
		removeAsListenerAndDispose(userSearchCtr);
		userSearchCtr = new UserSearchController(ureq, getWindowControl(), false, true, UserSearchController.ACTION_KEY_CHOOSE_FINISH);
		userSearchCtr.setUserObject(rows);
		listenTo(userSearchCtr);
		
		String title = translate("tools.add.owners");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), userSearchCtr.getInitialComponent(),
				true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doAddOwners(List<Identity> futureOwners, List<AuthoringEntryRow> rows) {
		for(AuthoringEntryRow row:rows) {
			RepositoryEntry re = repositoryService.loadByKey(row.getKey());
			for(Identity futureOwner:futureOwners) {
				repositoryService.addRole(futureOwner, re, GroupRoles.owner.name());
			}
		}
	}
	
	private void doCloseResource(UserRequest ureq, AuthoringEntryRow row) {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(wc);
		
		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		RepositoryHandler repoHandler = repositoryHandlerFactory.getRepositoryHandler(entry);
		wc = repoHandler.createCloseResourceController(ureq, getWindowControl(), entry);
		listenTo(wc);
		wc.startWorkflow();
		
		String title = wc.getAndRemoveWizardTitle();
		cmc = new CloseableModalController(getWindowControl(), translate("close"), wc.getInitialComponent(), true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doCopy(UserRequest ureq, AuthoringEntryRow row) {
		removeAsListenerAndDispose(cmc);
		removeAsListenerAndDispose(copyCtrl);

		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		copyCtrl = new CopyRepositoryEntryController(ureq, getWindowControl(), entry);
		listenTo(copyCtrl);
		
		String title = translate("details.copy");
		cmc = new CloseableModalController(getWindowControl(), translate("close"), copyCtrl.getInitialComponent(), true, title);
		listenTo(cmc);
		cmc.activate();
	}
	
	private void doDelete(UserRequest ureq, AuthoringEntryRow row) {
		boolean isOwner = true;
		if (!isOwner) throw new OLATSecurityException("Trying to delete, but not allowed: user = " + ureq.getIdentity());

		//show how many users are currently using this resource
		String dialogTitle = translate("del.header", row.getDisplayname());
		Long resId = row.getRepositoryEntryResourceable().getResourceableId();
		OLATResourceable courseRunOres = OresHelper.createOLATResourceableInstance(RunMainController.ORES_TYPE_COURSE_RUN, resId);
		int cnt = CoordinatorManager.getInstance().getCoordinator().getEventBus().getListeningIdentityCntFor(courseRunOres);
		
		boolean corrupted = false;
		
		String dialogText = translate(corrupted ? "del.confirm.corrupted" : "del.confirm", String.valueOf(cnt));
		deleteDialogCtrl = activateYesNoDialog(ureq, dialogTitle, dialogText, deleteDialogCtrl);
	}
	
	private void doDownload(UserRequest ureq, AuthoringEntryRow row) {
		RepositoryHandler typeToDownload = repositoryHandlerFactory.getRepositoryHandler(row.getResourceType());
		if (typeToDownload == null) {
			StringBuilder sb = new StringBuilder(translate("error.download"));
			sb.append(": No download handler for repository entry: ")
			  .append(row.getKey());
			showError(sb.toString());
			return;
		}
		
		RepositoryEntry entry = repositoryService.loadByKey(row.getKey());
		OLATResourceable ores = entry.getOlatResource();
		if (ores == null) {
			showError("error.download");
			return;
		}
		
		boolean isAlreadyLocked = typeToDownload.isLocked(ores);
		try {			
		  lockResult = typeToDownload.acquireLock(ores, ureq.getIdentity());
		  if(lockResult == null || (lockResult !=null && lockResult.isSuccess() && !isAlreadyLocked)) {
		    MediaResource mr = typeToDownload.getAsMediaResource(ores, false);
		    if(mr!=null) {
		      repositoryService.incrementDownloadCounter(entry);
		      ureq.getDispatchResult().setResultingMediaResource(mr);
		    } else {
			    showError("error.export");
			    fireEvent(ureq, Event.FAILED_EVENT);			
		    }
		  } else if(lockResult !=null && lockResult.isSuccess() && isAlreadyLocked) {
		  	String fullName = userManager.getUserDisplayName(lockResult.getOwner());
		  	showInfo("warning.course.alreadylocked.bySameUser", fullName);
		  	lockResult = null; //invalid lock, it was already locked
		  } else {
		  	String fullName = userManager.getUserDisplayName(lockResult.getOwner());
		  	showInfo("warning.course.alreadylocked", fullName);
		  }
		} finally {	
			if((lockResult!=null && lockResult.isSuccess() && !isAlreadyLocked)) {
			  typeToDownload.releaseLock(lockResult);		
			  lockResult = null;
			}
		}
	}
	
	private void launch(UserRequest ureq, AuthoringEntryRow row) {
		try {
			RepositoryHandler handler = repositoryHandlerFactory.getRepositoryHandler(row.getResourceType());
			if(handler != null && handler.supportsLaunch()) {
				String businessPath = "[RepositoryEntry:" + row.getKey() + "]";
				NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
			}
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}
	
	private void launchCatalog(UserRequest ureq, RepositoryEntryRef ref) {
		String businessPath = "[RepositoryEntry:" + ref.getKey() + "][Catalog:0]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	private void launchDetails(UserRequest ureq, RepositoryEntryRef ref) {
		String businessPath = "[RepositoryEntry:" + ref.getKey() + "][Infos:0]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	private void launchEditDescription(UserRequest ureq, RepositoryEntryRef ref) {
		String businessPath = "[RepositoryEntry:" + ref.getKey() + "][EditDescription:0]";
		NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
	}
	
	private void launchEditor(UserRequest ureq, AuthoringEntryRow row) {
		try {
			String businessPath = "[RepositoryEntry:" + row.getKey() + "][Editor:0]";
			NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}
	
	private void launchMembers(UserRequest ureq, AuthoringEntryRow row) {
		try {
			String businessPath = "[RepositoryEntry:" + row.getKey() + "][MembersMgmt:0]";
			NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
		} catch (CorruptedCourseException e) {
			logError("Course corrupted: " + row.getKey() + " (" + row.getOLATResourceable().getResourceableId() + ")", e);
			showError("cif.error.corrupted");
		}
	}

	@Override
	public void forgeLinks(AuthoringEntryRow row) {
		//mark
		FormLink markLink = uifactory.addFormLink("mark_" + counter.incrementAndGet(), "mark", "", null, null, Link.NONTRANSLATED);
		markLink.setIconLeftCSS(row.isMarked() ? Mark.MARK_CSS_LARGE : Mark.MARK_ADD_CSS_LARGE);
		markLink.setTitle(translate(row.isMarked() ? "details.bookmark.remove" : "details.bookmark"));
		markLink.setUserObject(row);
		row.setMarkLink(markLink);
		//tools
		FormLink toolsLink = uifactory.addFormLink("tools_" + counter.incrementAndGet(), "tools", "", null, null, Link.NONTRANSLATED);
		toolsLink.setIconLeftCSS("o_icon o_icon_actions o_icon-lg");
		toolsLink.setTitle(" ");
		toolsLink.setUserObject(row);
		row.setToolsLink(toolsLink);
	}
	
	private class ToolsController extends BasicController {
		
		private final AuthoringEntryRow row;
		private final RepositoryEntry entry;
		private final VelocityContainer mainVC;
		
		private boolean isOwner;
		private boolean isOlatAdmin;
		private boolean isAuthor;
		
		public ToolsController(UserRequest ureq, WindowControl wControl, AuthoringEntryRow row) {
			super(ureq, wControl);
			setTranslator(AuthorListController.this.getTranslator());
			this.row = row;
			this.entry = repositoryService.loadByKey(row.getKey());

			Identity identity = getIdentity();
			Roles roles = ureq.getUserSession().getRoles();
			isOlatAdmin = roles.isOLATAdmin();
			boolean isInstitutionalResourceManager = !roles.isGuestOnly()
						&& RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(identity, roles, entry);
			isOwner = isOlatAdmin || repositoryService.hasRole(ureq.getIdentity(), entry, GroupRoles.owner.name())
						|| isInstitutionalResourceManager;
			isAuthor = isOlatAdmin || roles.isAuthor() | isInstitutionalResourceManager;

			mainVC = createVelocityContainer("tools");
			List<String> links = new ArrayList<>();

			if(isOwner) {
				addLink("tools.edit.description", "description", "o_icon o_icon-fw o_icon_details", links);
				if(repositoryModule.isCatalogEnabled()) {
					addLink("tools.edit.catalog", "catalog", "o_icon o_icon-fw o_icon_catalog", links);
				}
				addLink("details.members", "members", "o_icon o_icon-fw o_icon_membersmanagement", links);
			}
			links.add("-");
			boolean copyManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.copy);
			if ((isAuthor || isOwner) && !copyManaged) {
				addLink("details.copy", "copy", "o_icon o_icon-fw o_icon_copy", links);
			}
			addLink("details.download", "download", "o_icon o_icon-fw o_icon_download", links);
			if(isOwner) {
				boolean closeManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.close);
				boolean deleteManaged = RepositoryEntryManagedFlag.isManaged(entry, RepositoryEntryManagedFlag.delete);
				if(!closeManaged || !deleteManaged) {
					links.add("-");
				}

				if(!closeManaged) {
					addLink("details.close.ressoure", "close", "o_icon o_icon-fw o_icon_close_resource", links);
				}
				if(!deleteManaged) {
					addLink("details.delete", "delete", "o_icon o_icon-fw o_icon_delete_item", links);
				}
			}

			mainVC.contextPut("links", links);
			putInitialPanel(mainVC);
		}
		
		private void addLink(String name, String cmd, String iconCSS, List<String> links) {
			Link link = LinkFactory.createLink(name, cmd, getTranslator(), mainVC, this, Link.LINK);
			if(iconCSS != null) {
				link.setIconLeftCSS(iconCSS);
			}
			mainVC.put(name, link);
			links.add(name);
		}

		@Override
		protected void event(UserRequest ureq, Component source, Event event) {
			fireEvent(ureq, Event.DONE_EVENT);
			if(source instanceof Link) {
				Link link = (Link)source;
				String cmd = link.getCommand();
				if("description".equals(cmd)) {
					launchEditDescription(ureq, row);
				} else if("catalog".equals(cmd)) {
					launchCatalog(ureq, row);
				}  else if("members".equals(cmd)) {
					launchMembers(ureq, row);
				} else if("copy".equals(cmd)) {
					doCopy(ureq, row);
				} else if("download".equals(cmd)) {
					doDownload(ureq, row);
				} else if("close".equals(cmd)) {
					doCloseResource(ureq, row);
				} else if("delete".equals(cmd)) {
					doDelete(ureq, row);
				}
			}
		}

		@Override
		protected void doDispose() {
			//
		}
	}
}