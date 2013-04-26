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
package org.olat.group;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.olat.basesecurity.SecurityGroup;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.async.ProgressDelegate;
import org.olat.core.util.mail.MailPackage;
import org.olat.core.util.mail.MailerResult;
import org.olat.group.area.BGArea;
import org.olat.group.model.BGRepositoryEntryRelation;
import org.olat.group.model.BusinessGroupEnvironment;
import org.olat.group.model.BusinessGroupMembershipChange;
import org.olat.group.model.DisplayMembers;
import org.olat.group.model.EnrollState;
import org.olat.group.model.MembershipModification;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryShort;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.model.ResourceReservation;

/**
 * 
 * 
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public interface BusinessGroupService {

	/**
	 * Extension-point method to register objects which have deletable group-data.
	 * Listener will be called in method deleteBusinessGroup.
	 * @param listener
	 */
	public void registerDeletableGroupDataListener(DeletableGroupData listener);

	/**
	 * Create a persistent BusinessGroup with the provided
	 * parameters. The BusinessGroup can have a waiting-list.
	 * @param creator
	 * @param name
	 * @param description
	 * @param minParticipants
	 * @param maxParticipants
	 * @param waitingListEnabled
	 * @param autoCloseRanksEnabled
	 * @param resource
	 * @return
	 */
	public BusinessGroup createBusinessGroup(Identity creator, String name, String description,
			Integer minParticipants, Integer maxParticipants, boolean waitingListEnabled, boolean autoCloseRanksEnabled,
			RepositoryEntry resource);
	
	/**
	 * Update the business group with the supplied arguments and do it in sync
	 * @param group
	 * @param name
	 * @param desc
	 * @param minParticipants
	 * @param maxParticipants
	 * @return
	 */
	public BusinessGroup updateBusinessGroup(Identity ureqIdentity, BusinessGroup group, String name, String description,
			Integer minParticipants, Integer maxParticipants);
	
	/**
	 * Update the business group with the supplied arguments and do it in sync
	 * @param group
	 * @param name
	 * @param description
	 * @param minParticipants
	 * @param maxParticipants
	 * @param waitingList
	 * @param autoCloseRanks
	 * @return
	 */
	public BusinessGroup updateBusinessGroup(Identity ureqIdentity, BusinessGroup group, String name, String description,
			Integer minParticipants, Integer maxParticipants, Boolean waitingList, Boolean autoCloseRanks);
	
	public DisplayMembers getDisplayMembers(BusinessGroup group);
	
	public void updateDisplayMembers(BusinessGroup group, DisplayMembers displayMembers);
	
	/**
	 * Delete a business group from the persistence store
	 * @param group
	 */
	public void deleteBusinessGroup(BusinessGroup group);
	
	/**
	 * Delete a business group and send a mail to all users in the group
	 * @param group
	 * @param businessPath
	 * @param deletedBy
	 * @param locale
	 * @return
	 */
	public MailerResult deleteBusinessGroupWithMail(BusinessGroup group, String businessPath, Identity deletedBy, Locale locale);

	/**
	 * Set certain business-group as active (set last-usage and delete time stamp for
	 * 'SEND_DELETE_EMAIL_ACTION' in LifeCycleManager):
	 * @param group
	 * @return
	 */
	public BusinessGroup setLastUsageFor(Identity identity, BusinessGroup group);
	
	/**
	 * Reload the business group
	 * @param group
	 * @return The reloaded business group
	 */
	public BusinessGroup loadBusinessGroup(BusinessGroup group);
	
	/**
	 * Reload the business group
	 * @param key
	 * @return The reloaded business group
	 */
	public BusinessGroup loadBusinessGroup(Long key);
	
	/**
	 * Load business groups by keys
	 * @param keys
	 * @return The list of business groups
	 */
	public List<BusinessGroup> loadBusinessGroups(Collection<Long> keys);
	
	public List<BusinessGroupShort> loadShortBusinessGroups(Collection<Long> keys);
	
	/**
	 * Load all business groups (be cautious, it takes lot of times)
	 * @return The list of all business groups
	 */
	public List<BusinessGroup> loadAllBusinessGroups();
	
	/**
	 * Load the business group by it's OLAT resource
	 * @param resource
	 * @return The business group
	 */
	public BusinessGroup loadBusinessGroup(OLATResource resource);

	/**
	 * Create and persist a new business group based on a source group.
	 * @param sourceBusinessGroup The group that will be used as the source group and everything
	 * @param targetName
	 * @param targetDescription
	 * @param targetMin
	 * @param targetMax
	 * @param targetResource
	 * @param areaLookupMap The area lookup map (the target group will references
	 *          mapped areas) or null (target group will reference the same areas
	 *          as the original group)
	 * @param copyAreas
	 * @param copyCollabToolConfig
	 * @param copyRights
	 * @param copyOwners
	 * @param copyParticipants
	 * @param copyMemberVisibility
	 * @param copyWaitingList
	 * @return The copied business group
	 */
	public BusinessGroup copyBusinessGroup(Identity identity, BusinessGroup sourceBusinessGroup, String targetName, String targetDescription,
			Integer targetMin, Integer targetMax, boolean copyAreas, boolean copyCollabToolConfig, boolean copyRights,
			boolean copyOwners, boolean copyParticipants, boolean copyMemberVisibility, boolean copyWaitingList, boolean copyRelations);

	/**
	 * Merge the owners, participants and the waiting list.
	 * 
	 * @param targetGroup
	 * @param groupsToMerge
	 * @return
	 */
	public BusinessGroup mergeBusinessGroups(Identity ureqIdentity, BusinessGroup targetGroup,
			List<BusinessGroup> groupsToMerge, MailPackage mailing);
	
	/**
	 * Update the members of a list of business groups. The process is additive, a current
	 * participant which is marked as "add owner", will be participant and owner.
	 * @param membersMod
	 * @param groups
	 */
	public void updateMembership(Identity identity, MembershipModification modifications,
			List<BusinessGroup> groups, MailPackage mailing);
	
	/**
	 * Very fine tuned membership changes on several groups
	 * 
	 * @param ureqIdentity
	 * @param changes
	 */
	public void updateMemberships(Identity ureqIdentity, List<BusinessGroupMembershipChange> changes,
			MailPackage mailing);
	
	//search methods
	/**
	 * Find the business group associated with this security group
	 * @param secGroup
	 * @return Return the business group or null if not found
	 */
	public BusinessGroup findBusinessGroup(SecurityGroup secGroup);

	/**
	 * Find the BusinessGroups list associated with the supplied identity, where the identity is an owner.
	 * @param identity
	 * @param resource
	 * @return
	 */
	public List<BusinessGroup> findBusinessGroupsOwnedBy(Identity identity, OLATResource resource);
	
	/**
	 * Find the list of BusinessGroups associated with the supplied identity, where
	 * the identity is a participant.
	 * @param identity
	 * @param resource
	 * @return
	 */
	public List<BusinessGroup> findBusinessGroupsAttendedBy(Identity identity, OLATResource resource);
	
	/**
	 * Find all business-groups where the identity is on the waiting-list.
	 * @param identity
	 * @param resource
	 * @return
	 */
	public List<BusinessGroup> findBusinessGroupsWithWaitingListAttendedBy(Identity identity,OLATResource resource);
	
	public int countBusinessGroups(SearchBusinessGroupParams params, OLATResource resource);
	
	public List<BusinessGroup> findBusinessGroups(SearchBusinessGroupParams params, OLATResource resource, int firstResult, int maxResults, BusinessGroupOrder... ordering);
	
	
	public int countBusinessGroupViews(SearchBusinessGroupParams params, OLATResource resource);
	/**
	 * Find business groups (the view)
	 * @param params
	 * @param resource
	 * @param firstResult
	 * @param maxResults
	 * @param ordering
	 * @return
	 */
	public List<BusinessGroupView> findBusinessGroupViews(SearchBusinessGroupParams params, OLATResource resource, int firstResult, int maxResults, BusinessGroupOrder... ordering);
	
	/**
	 * Find all groups within resources where the identity is author.
	 * @param author
	 * @return
	 */
	public List<BusinessGroupView> findBusinessGroupViewsWithAuthorConnection(Identity author);
	
	public List<Long> toGroupKeys(String groupNames, OLATResource resource);

	//retrieve repository entries

	public boolean hasResources(BusinessGroup group);
	
	public boolean hasResources(List<BusinessGroup> groups);
	
	public void addResourceTo(BusinessGroup group, RepositoryEntry re);
	
	public void addResourcesTo(List<BusinessGroup> groups, List<RepositoryEntry> resources);
	
	public void removeResourceFrom(List<BusinessGroup> group, RepositoryEntry re);
	
	public void removeResource(OLATResource resource);
	
	public List<RepositoryEntry> findRepositoryEntries(Collection<BusinessGroup> groups, int firstResult, int maxResults);
	
	/**
	 * Same as above but do a better caching
	 * @param groups
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	public List<RepositoryEntryShort> findShortRepositoryEntries(Collection<BusinessGroupShort> groups, int firstResult, int maxResults);
	
	public List<BGRepositoryEntryRelation> findRelationToRepositoryEntries(Collection<Long> groups, int firstResult, int maxResults);
	
	public List<OLATResource> findResources(Collection<BusinessGroup> groups, int firstResult, int maxResults);
	
	
	//found identities
	public int countContacts(Identity identity);
	
	public List<Identity> findContacts(Identity identity, int firstResult, int maxResults);
	
	public int countMembersOf(OLATResource resource, boolean owner, boolean attendee);
	
	public List<Identity> getMembersOf(OLATResource resource, boolean owner, boolean attendee);
	
	/**
	 * Get position of an identity on a certain waiting-list 
	 * @param identity
	 * @param businessGroup
	 * @return
	 */
	public int getPositionInWaitingListFor(Identity identity, BusinessGroup businessGroup);
	
	//memberships
	/**
	 * Adds a user to a group as owner and does all the magic that needs to be
	 * done: - add to security group (optional) - add to jabber roster - fire multi user event
	 * @param ureqIdentity
	 * @param addIdentities  The users who should be added
	 * @param group
	 * @param flags
	 * @return
	 */
	public BusinessGroupAddResponse addOwners(Identity ureqIdentity, Roles ureqRoles, List<Identity> addIdentities,
			BusinessGroup group, MailPackage mailing);
	
	/**
	 * Remove a list of users from a group as owner and does all the magic that needs to be
	 * done: - remove from security group (optional) - remove from jabber roster - fire
	 * multi-user event
	 * @param ureqIdentity
	 * @param identitiesToRemove
	 * @param group
	 * @param flags
	 */
	public void removeOwners(Identity ureqIdentity, Collection<Identity> identitiesToRemove, BusinessGroup group);
	
	/**
	 * Enroll an identity to the group following the rules set by reservation, max participants,
	 * waiting list, auto close ranks...
	 * 
	 * 
	 * @param group
	 * @param identity
	 * @return
	 */
	public EnrollState enroll(Identity ureqIdentity, Roles ureqRoles, Identity identityToEnroll, BusinessGroup group,
			MailPackage mailing);
	
	/**
	 * Adds a list of users to a group as participant and does all the magic that needs to
	 * be done:
	 * <ul>
	 * 	<li>add to security group (optional)
	 *  <li>add to jabber roster
	 *  <li>fire multi-user
	 * </ul>
	 * Method execute in doInSync
	 * event
	 * @param ureqIdentity
	 * @param addIdentities The users who should be added
	 * @param currBusinessGroup
	 * @param flags
	 * @return
	 */
	public BusinessGroupAddResponse addParticipants(Identity ureqIdentity, Roles ureqRoles, List<Identity> addIdentities,
			BusinessGroup currBusinessGroup, MailPackage mailing);
	
	/**
	 * 
	 * @param ureqIdentity
	 * @param reservationOwner
	 * @param resource
	 */
	public void acceptPendingParticipation(Identity ureqIdentity, Identity reservationOwner, OLATResource resource);
	
	public void cancelPendingParticipation(Identity ureqIdentity, ResourceReservation reservation);
	
	/**
	 * Remove a list of users from a group as participant and does all the magic that needs
	 * to be done:
	 * <ul>
	 * 	<li>remove from secgroup (optional)
	 *  <li>remove from jabber roster -
	 *  <li>fire multi-user event
	 * </ul>
	 * The method is made under doInSync.
	 * 
	 * @param ureqIdentity
	 * @param identities The user who should be removed
	 * @param group
	 * @param flags
	 */
	public void removeParticipants(Identity ureqIdentity, List<Identity> identities, BusinessGroup group, MailPackage mailing);
	
	/**
	 * Remove the members (tutors and participants) from all business groups connected
	 * to the resource (the resource can be a BusinessGroup) by cancelling their membership
	 * or their reservations.
	 * 
	 * @param ureqIdentity
	 * @param identities
	 * @param group
	 */
	public void removeMembers(Identity ureqIdentity, List<Identity> identities, OLATResource resource, MailPackage mailing);

	
	/**
	 * Adds a  list of users to a waiting-list of a group and does all the magic that needs to
	 * be done:
	 * <ul>
	 * 	<li>add to security group (optional)
	 *  <li>add to jabber roster
	 *  <li>send notification email
	 *  <li>fire multi user event
	 * </ul>
	 * Method executed under doInSync
	 * @param ureqIdentity
	 * @param addIdentities
	 * @param currBusinessGroup
	 * @param flags
	 * @return
	 */
	public BusinessGroupAddResponse addToWaitingList(Identity ureqIdentity, List<Identity> addIdentities,
			BusinessGroup businessGroup, MailPackage mailing);

	/**
	 * Remove a list of users from a waiting-list as participant and does all the magic that needs
	 * to be done:
	 * - remove from security group (optional)<br/>
	 * - send notification email<br/>
	 * - fire multi-user event
	 * @param ureqIdentity
	 * @param identities
	 * @param currBusinessGroup
	 * @param flags
	 */
	public void removeFromWaitingList(Identity ureqIdentity, List<Identity> identities, BusinessGroup businessGroup, MailPackage mailing);

	/**
	 * Move users from a waiting-list to participant-list.
	 * @param identities
	 * @param ureqIdentity
	 * @param currBusinessGroup
	 * @param flags
	 * @return
	 */
	public BusinessGroupAddResponse moveIdentityFromWaitingListToParticipant(Identity ureqIdentity, List<Identity> identities,
			BusinessGroup currBusinessGroup, MailPackage mailing);


	public void removeAndFireEvent(Identity ureqIdentity, List<Identity> addIdentities, SecurityGroup secGroup);
	
	/**
	 * Count the duplicates
	 * @param entry
	 * @param coaches
	 * @param participants
	 */
	public int countDuplicateMembers(RepositoryEntry entry, boolean coaches, boolean participants);
	
	/**
	 * Remove the members of the repository entry which are already in a business group
	 * linked to it.
	 */
	public void dedupMembers(Identity ureqIdentity, RepositoryEntry entry, boolean coaches, boolean participants);
	
	/**
	 * Deduplicate all the courses in the repository
	 * @param ureqIdentity
	 * @param coaches
	 * @param participants
	 * @param progressDelegate
	 */
	public void dedupMembers(Identity ureqIdentity, boolean coaches, boolean participants, ProgressDelegate progressDelegate);

	
	//security
	/**
	 * Checks if an identity is in a specific business group, either as owner or
	 * as participant
	 * @param identity
	 * @param businessGroup
	 * @return
	 */
	public boolean isIdentityInBusinessGroup(Identity identity, BusinessGroup businessGroup);
	
	/**
	 * Checks if an identity is in the list of business groups either as owner or as participant
	 * @param identity
	 * @param businessGroups
	 * @return The list of group keys where the identity is either participant or owner
	 */
	public List<BusinessGroupMembership> getBusinessGroupMembership(Collection<Long> businessGroups, Identity... identity);

	/**
	 * Checks if an identity is in a business group with a specific key, either as owner or
	 * as participant
	 * @param identity
	 * @param groupKey The group key (optional)
	 * @param ownedById
	 * @param attendedById
	 * @param resource The resource context (mandatory)
	 * @return
	 */
	public boolean isIdentityInBusinessGroup(Identity identity, Long groupKey, boolean ownedById, boolean attendedById, OLATResource resource);

	
	//export - import
	/**
	 * Export group definitions to file.
	 * @param groups
	 * @param fExportFile
	 */
	public void exportGroups(List<BusinessGroup> groups, List<BGArea> areas, File fExportFile, BusinessGroupEnvironment env, boolean backwardsCompatible);

	/**
	 * Import previously exported group definitions.
	 * @param resource
	 * @param fGroupExportXML
	 */
	public BusinessGroupEnvironment importGroups(RepositoryEntry re, File fGroupExportXML);
	
	public void archiveGroups(List<BusinessGroup> groups, File exportFile);

	public File archiveGroupMembers(OLATResource resource, List<String> columnList, List<BusinessGroup> groupList, String archiveType,
			Locale locale, String charset);

}