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
package org.olat.repository.ui.list;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.queryparser.classic.ParseException;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.DefaultResultInfos;
import org.olat.core.commons.persistence.ResultInfos;
import org.olat.core.commons.persistence.SortKey;
import org.olat.core.gui.components.form.flexible.impl.elements.table.FlexiTableDataSourceDelegate;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.repository.RepositoryEntryMyView;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoryService;
import org.olat.repository.SearchMyRepositoryEntryViewParams;
import org.olat.repository.SearchMyRepositoryEntryViewParams.Filter;
import org.olat.repository.SearchMyRepositoryEntryViewParams.OrderBy;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.repository.ui.PriceMethod;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.ACService;
import org.olat.resource.accesscontrol.model.OLATResourceAccess;
import org.olat.resource.accesscontrol.model.PriceMethodBundle;
import org.olat.resource.accesscontrol.ui.PriceFormat;
import org.olat.search.QueryException;
import org.olat.search.ServiceNotAvailableException;
import org.olat.search.service.searcher.SearchClient;
import org.olat.search.service.searcher.SearchClientLocal;

/**
 * 
 * Initial date: 28.01.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class DefaultRepositoryEntryDataSource implements FlexiTableDataSourceDelegate<RepositoryEntryRow> {
	
	private static final OLog log = Tracing.createLoggerFor(DefaultRepositoryEntryDataSource.class);

	private final RepositoryEntryDataSourceUIFactory uifactory;
	private final SearchMyRepositoryEntryViewParams searchParams;
	

	private final ACService acService;
	private final SearchClient searchClient;
	private final RepositoryService repositoryService;
	private final RepositoryManager repositoryManager;
	
	private Integer count;
	
	public DefaultRepositoryEntryDataSource(SearchMyRepositoryEntryViewParams searchParams,
			RepositoryEntryDataSourceUIFactory uifactory) {
		this.uifactory = uifactory;
		this.searchParams = searchParams;
		
		acService = CoreSpringFactory.getImpl(ACService.class);
		searchClient = CoreSpringFactory.getImpl(SearchClientLocal.class);
		repositoryService = CoreSpringFactory.getImpl(RepositoryService.class);
		repositoryManager = CoreSpringFactory.getImpl(RepositoryManager.class);
	}
	
	public void setFilters(List<Filter> filters) {
		searchParams.setFilters(filters);
		count = null;
	}
	
	public void setOrderBy(OrderBy orderBy) {
		searchParams.setOrderBy(orderBy);
	}

	@Override
	public int getRowCount() {
		if(count == null) {
			count = repositoryService.countMyView(searchParams);
		}
		return count.intValue();
	}

	@Override
	public List<RepositoryEntryRow> reload(List<RepositoryEntryRow> rows) {
		return Collections.emptyList();
	}

	@Override
	public final ResultInfos<RepositoryEntryRow> getRows(String query, List<String> condQueries,
			int firstResult, int maxResults, SortKey... orderBy) {

		if(StringHelper.containsNonWhitespace(query)) {
			try {
				List<Long> fullTextResults = searchClient.doSearch(query, null, searchParams.getIdentity(), searchParams.getRoles(), 0, 100);
				searchParams.setRepoEntryKeys(fullTextResults);
			} catch (ServiceNotAvailableException | ParseException | QueryException e) {
				log.error("", e);
			}
		}
		
		List<RepositoryEntryMyView> views = repositoryService.searchMyView(searchParams, firstResult, maxResults);
		List<RepositoryEntryRow> rows = processViewModel(views);
		ResultInfos<RepositoryEntryRow> results = new DefaultResultInfos<RepositoryEntryRow>(firstResult + rows.size(), -1, rows);
		if(firstResult == 0 && views.size() < maxResults) {
			count = new Integer(views.size() );
		}
		return results;
	}

	private List<RepositoryEntryRow> processViewModel(List<RepositoryEntryMyView> repoEntries) {
		List<Long> repoKeyWithAC = new ArrayList<>(repoEntries.size());
		List<OLATResource> resourcesWithAC = new ArrayList<>(repoEntries.size());
		for(RepositoryEntryMyView entry:repoEntries) {
			if(entry.isValidOfferAvailable()) {
				repoKeyWithAC.add(entry.getKey());
				resourcesWithAC.add(entry.getOlatResource());
			}
		}
		List<OLATResourceAccess> resourcesWithOffer = acService.filterResourceWithAC(resourcesWithAC);
		repositoryService.filterMembership(searchParams.getIdentity(), repoKeyWithAC);

		List<RepositoryEntryRow> items = new ArrayList<RepositoryEntryRow>();
		for(RepositoryEntryMyView entry:repoEntries) {
			RepositoryEntryRow row = new RepositoryEntryRow();
			row.setKey(entry.getKey());
			row.setDisplayName(entry.getDisplayname());
			row.setDescription(entry.getDescription());
			row.setOLATResourceable(OresHelper.clone(entry.getOlatResource()));
			row.setAuthors(entry.getAuthors());
			
			//bookmark
			row.setMarked(entry.isMarked());
			
			//efficiency statement
			row.setPassed(entry.getPassed());
			row.setScore(AssessmentHelper.getRoundedScore(entry.getScore()));
			
			//user course infos
			row.setInitialLaunch(entry.getInitialLaunch());
			row.setRecentLaunch(entry.getRecentLaunch());
			if(entry.getVisit() != null) {
				row.setVisit(entry.getVisit().intValue());
			} else {
				row.setVisit(0);
			}
			if(entry.getTimeSpend() != null) {
				row.setTimeSpend(entry.getTimeSpend().longValue());
			} else {
				row.setTimeSpend(0l);
			}
			
			//rating
			row.setMyRating(entry.getMyRating());
			row.setAverageRating(entry.getAverageRating());
			row.setNumOfRatings(entry.getNumOfRatings());
			row.setNumOfComments(entry.getNumOfComments());
			
			RepositoryEntryLifecycle lifecycle = entry.getLifecycle();
			if(lifecycle != null) {
				row.setLifecycleStart(lifecycle.getValidFrom());
				row.setLifecycleEnd(lifecycle.getValidTo());
				if(!lifecycle.isPrivateCycle()) {
					row.setLifecycle(lifecycle.getLabel());
					row.setLifecycleSoftKey(lifecycle.getSoftKey());
				}
			}

			VFSLeaf image = repositoryManager.getImage(entry);
			if(image != null) {
				row.setThumbnailRelPath(uifactory.getMapperThumbnailUrl() + "/" + image.getName());
			}

			List<PriceMethod> types = new ArrayList<PriceMethod>();
			if (entry.isMembersOnly()) {
				// members only always show lock icon
				types.add(new PriceMethod("", "b_access_membersonly_icon"));
			} else {
				// collect access control method icons
				OLATResource resource = entry.getOlatResource();
				for(OLATResourceAccess resourceAccess:resourcesWithOffer) {
					if(resource.getKey().equals(resourceAccess.getResource().getKey())) {
						for(PriceMethodBundle bundle:resourceAccess.getMethods()) {
							String type = (bundle.getMethod().getMethodCssClass() + "_icon").intern();
							String price = bundle.getPrice() == null || bundle.getPrice().isEmpty() ? "" : PriceFormat.fullFormat(bundle.getPrice());
							types.add(new PriceMethod(price, type));
						}
					}
				}
				
				row.setMember(repoKeyWithAC.contains(entry.getKey()));
			}
			
			if(!types.isEmpty()) {
				row.setAccessTypes(types);
			}
			
			uifactory.forgeMarkLink(row);
			uifactory.forgeSelectLink(row);
			uifactory.forgeStartLink(row);
			uifactory.forgeDetails(row);
			uifactory.forgeRatings(row);
			uifactory.forgeComments(row);
			
			items.add(row);
		}
		return items;
	}
}