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
 * 12.10.2011 by frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.repository.model;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;

/**
 * 
 * Initial date: 28.04.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class SearchAuthorRepositoryEntryViewParams {
	private final Identity identity;
	private final Roles roles;
	
	private Boolean marked;
	private boolean ownedResourcesOnly;
	
	private String idAndRefs;
	private String idRefsAndTitle;
	private String author;
	private String displayname;
	private String description;
	
	private OrderBy orderBy;
	private boolean orderByAsc;
	private List<String> resourceTypes;
	private List<Long> repoEntryKeys;
	
	public SearchAuthorRepositoryEntryViewParams(Identity identity, Roles roles) {
		this.identity = identity;
		this.roles = roles;
	}

	public String getIdAndRefs() {
		return idAndRefs;
	}

	public void setIdAndRefs(String idAndRefs) {
		this.idAndRefs = idAndRefs;
	}

	public String getIdRefsAndTitle() {
		return idRefsAndTitle;
	}

	public void setIdRefsAndTitle(String idRefsAndTitle) {
		this.idRefsAndTitle = idRefsAndTitle;
	}

	public boolean isOwnedResourcesOnly() {
		return ownedResourcesOnly;
	}

	public void setOwnedResourcesOnly(boolean ownedResourcesOnly) {
		this.ownedResourcesOnly = ownedResourcesOnly;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getDisplayname() {
		return displayname;
	}

	public void setDisplayname(String displayname) {
		this.displayname = displayname;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Long> getRepoEntryKeys() {
		return repoEntryKeys;
	}

	public void setRepoEntryKeys(List<Long> repoEntryKeys) {
		this.repoEntryKeys = repoEntryKeys;
	}

	public OrderBy getOrderBy() {
		return orderBy;
	}

	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}
	
	public boolean isOrderByAsc() {
		return orderByAsc;
	}

	public void setOrderByAsc(boolean orderByAsc) {
		this.orderByAsc = orderByAsc;
	}

	public boolean isResourceTypesDefined() {
		return resourceTypes != null && resourceTypes.size() > 0;
	}

	public List<String> getResourceTypes() {
		return resourceTypes;
	}
	
	public void setResourceTypes(List<String> resourceTypes) {
		this.resourceTypes = resourceTypes;
	}
	
	public void addResourceTypes(String... types) {
		if(this.resourceTypes == null) {
			this.resourceTypes = new ArrayList<String>();
		}
		if(types != null) {
			for(String resourceType:types) {
				this.resourceTypes.add(resourceType);
			}
		}
	}
	
	public Identity getIdentity() {
		return identity;
	}
	
	public Roles getRoles() {
		return roles;
	}
	
	public Boolean getMarked() {
		return marked;
	}

	public void setMarked(Boolean marked) {
		this.marked = marked;
	}
	
	public enum OrderBy {
		key,
		favorit,
		type,
		displayname,
		authors,
		author,
		access,
		ac,
		creationDate,
		lastUsage,
		externalId,
		externalRef,
		lifecycleLabel,
		lifecycleSoftkey,
		lifecycleStart,
		lifecycleEnd
	}
}
