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
package org.olat.repository;

import java.util.Date;

import org.olat.core.id.OLATResourceable;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.resource.OLATResource;

/**
 * 
 * Initial date: 12.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public interface RepositoryEntryMyView extends OLATResourceable {
	
	public Long getKey();
	
	public String getDisplayname();
	
	public String getDescription();
	
	public String getAuthors();
	
	public boolean isMembersOnly();
	
	public OLATResource getOlatResource();
	
	public RepositoryEntryLifecycle getLifecycle();
	
	/**
	 * @return The passed/failed status saved in the efficiency statement
	 */
	public Boolean getPassed();
	
	/**
	 * @return The score saved in the efficiency statement
	 */
	public Float getScore();
	
	/**
	 * @return True if the user as bookmarked this entry
	 */
	public boolean isMarked();
	
	/**
	 * @return The date of the first launch saved in the user course infos.
	 */
	public Date getInitialLaunch();
	
	public Date getRecentLaunch();
	
	public Integer getVisit();
	
	public Long getTimeSpend();
	
	/**
	 * @return The rating made by the user or null if the user has never rated the entry
	 */
	public Integer getMyRating();
	
	/**
	 * @return The average rating of this entry, or null if the entry was never rated
	 */
	public Float getAverageRating();
	
	public long getNumOfRatings();
	
	public long getNumOfComments();
	
	/**
	 * @return True if some offers are currently available
	 */
	public boolean isValidOfferAvailable();
	
	public boolean isOfferAvailable();
	

}