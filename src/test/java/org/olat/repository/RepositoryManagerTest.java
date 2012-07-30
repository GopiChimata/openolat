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
* <p>
*/ 

package org.olat.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.SecurityGroup;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.test.JMSCodePointServerJunitHelper;
import org.olat.test.JunitTestHelper;
import org.olat.test.OlatTestCase;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class RepositoryManagerTest extends OlatTestCase {
	
	private static final OLog log = Tracing.createLoggerFor(RepositoryManagerTest.class);
	private static String CODEPOINT_SERVER_ID = "RepositoryManagerTest";

	@Autowired
	private DB dbInstance;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private OLATResourceManager resourceManager;
	@Autowired
	private RepositoryManager repositoryManager;
	@Autowired
	private BusinessGroupService businessGroupService;
	
	@Before
	public void setup() {
		try {
			// Setup for code-points
			JMSCodePointServerJunitHelper.startServer(CODEPOINT_SERVER_ID);
		} catch (Exception e) {
			log.error("Error while setting up activeMq or Codepointserver", e);
		}
	}

	@After public void tearDown() {
		try {
			JMSCodePointServerJunitHelper.stopServer();
			DBFactory.getInstance().closeSession();
		} catch (Exception e) {
			log.error("tearDown failed", e);
		}
	}

	/**
	 * Test creation of a repository entry.
	 */
	@Test
	public void testRawRepositoryEntryCreate() {
		try {
			DB db = DBFactory.getInstance();
			OLATResourceManager rm = OLATResourceManager.getInstance();
			// create course and persist as OLATResourceImpl
			OLATResourceable resourceable = new OLATResourceable() {
					public String getResourceableTypeName() {	return "RepoMgrTestCourse";}
					public Long getResourceableId() {return CodeHelper.getForeverUniqueID();}
			};
			OLATResource r =  rm.createOLATResourceInstance(resourceable);
			db.saveObject(r);
	
			// now make a repository entry for this course
			RepositoryEntry d = new RepositoryEntry();
			d.setOlatResource(r);
			d.setResourcename("Lernen mit OLAT");
			d.setInitialAuthor("Florian Gnägi");
			d.setDisplayname("JunitTest_RepositoryEntry");
			db.saveObject(d);
		} catch(Exception ex) {
			fail("No Exception allowed. ex=" + ex.getMessage());
		}
	}
	
	@Test
	public void lookupRepositoryEntryByOLATResourceable() {
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		
		RepositoryEntry loadedRe = repositoryManager.lookupRepositoryEntry(re.getOlatResource(), false);
		
		Assert.assertNotNull(loadedRe);
		Assert.assertEquals(re, loadedRe);
	}
	
	@Test
	public void lookupRepositoryEntryBySoftkey() {
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		
		RepositoryEntry loadedRe = repositoryManager.lookupRepositoryEntryBySoftkey(re.getSoftkey(), false);
		Assert.assertNotNull(loadedRe);
		Assert.assertEquals(re, loadedRe);
	}
	
	@Test
	public void lookupRepositoryEntryKey() {
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		
		//check with a return value
		Long repoKey1 = repositoryManager.lookupRepositoryEntryKey(re.getOlatResource(), false);
		Assert.assertNotNull(repoKey1);
		Assert.assertEquals(re.getKey(), repoKey1);
		
		//check with a return value
		Long repoKey2 = repositoryManager.lookupRepositoryEntryKey(re.getOlatResource(), true);
		Assert.assertNotNull(repoKey2);
		Assert.assertEquals(re.getKey(), repoKey2);
		
		//check with a return value
		OLATResourceable dummy = OresHelper.createOLATResourceableInstance(UUID.randomUUID().toString(), 0l);
		Long repoKey3 = repositoryManager.lookupRepositoryEntryKey(dummy, false);
		Assert.assertNull(repoKey3);
	}
	
	@Test(expected=AssertException.class)
	public void lookupRepositoryEntryKeyStrictFailed() {
		//check with a return value
		OLATResourceable dummy = OresHelper.createOLATResourceableInstance(UUID.randomUUID().toString(), 0l);
		Long repoKey3 = repositoryManager.lookupRepositoryEntryKey(dummy, true);
		Assert.assertNull(repoKey3);
	}
	
	@Test
	public void lookupDisplayNameByOLATResourceableId() {
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		
		String displayName = repositoryManager.lookupDisplayNameByOLATResourceableId(re.getOlatResource().getResourceableId());
		Assert.assertNotNull(displayName);
		Assert.assertEquals(re.getDisplayname(), displayName);
	}
	
	@Test
	public void queryByOwnerLimitAccess() {
		//create a repository entry with an owner
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-owner-la-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		securityManager.addIdentityToSecurityGroup(id, re.getOwnerGroup());
		dbInstance.commitAndCloseSession();
		
		List<RepositoryEntry> entries = repositoryManager.queryByOwnerLimitAccess(id, RepositoryEntry.ACC_OWNERS, Boolean.TRUE);
		Assert.assertNotNull(entries);
		Assert.assertEquals(1, entries.size());
		Assert.assertTrue(entries.contains(re));
	}
	
	@Test
	public void getLearningResourcesAsStudent() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-stud-la-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry(true);
		securityManager.addIdentityToSecurityGroup(id, re.getParticipantGroup());
		dbInstance.commitAndCloseSession();

		List<RepositoryEntry> entries = repositoryManager.getLearningResourcesAsStudent(id, 0, -1);
		Assert.assertNotNull(entries);
		Assert.assertFalse(entries.isEmpty());
		Assert.assertTrue(entries.contains(re));
		
		Set<Long> duplicates = new HashSet<Long>();
		for(RepositoryEntry entry:entries) {
			Assert.assertTrue(duplicates.add(entry.getKey()));
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
	}
	
	@Test
	public void getLearningResourcesAsStudentWithGroups() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-stud-lb-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "studg", "tg", null, null, false, false, re.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id, group.getPartipiciantGroup());
		dbInstance.commitAndCloseSession();

		List<RepositoryEntry> entries = repositoryManager.getLearningResourcesAsStudent(id, 0, -1);
		Assert.assertNotNull(entries);
		Assert.assertFalse(entries.isEmpty());
		Assert.assertTrue(entries.contains(re));
		
		Set<Long> duplicates = new HashSet<Long>();
		for(RepositoryEntry entry:entries) {
			Assert.assertTrue(duplicates.add(entry.getKey()));
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
	}
	
	@Test
	public void getLearningResourcesAsTeacher() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-teac-la-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		securityManager.addIdentityToSecurityGroup(id, re.getTutorGroup());
		dbInstance.commitAndCloseSession();

		List<RepositoryEntry> entries = repositoryManager.getLearningResourcesAsTeacher(id, 0, -1);
		Assert.assertNotNull(entries);
		Assert.assertFalse(entries.isEmpty());
		Assert.assertTrue(entries.contains(re));
		
		Set<Long> duplicates = new HashSet<Long>();
		for(RepositoryEntry entry:entries) {
			Assert.assertTrue(duplicates.add(entry.getKey()));
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
	}
	
	@Test
	public void getLearningResourcesAsTeacherWithGroups() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("re-teac-lb-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "teacherg", "tg", null, null, false, false, re.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id, group.getOwnerGroup());
		dbInstance.commitAndCloseSession();

		List<RepositoryEntry> entries = repositoryManager.getLearningResourcesAsTeacher(id, 0, -1);
		Assert.assertNotNull(entries);
		Assert.assertFalse(entries.isEmpty());
		Assert.assertTrue(entries.contains(re));
		
		Set<Long> duplicates = new HashSet<Long>();
		for(RepositoryEntry entry:entries) {
			Assert.assertTrue(duplicates.add(entry.getKey()));
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
	}
	
	@Test
	public void queryByTypeLimitAccess() {
		Identity id = JunitTestHelper.createAndPersistIdentityAsUser("qbtla-1-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry(true);
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "qbtla-1", "tg", null, null, false, false, re.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id, group.getOwnerGroup());
		dbInstance.commitAndCloseSession();
		
		//check
		List<RepositoryEntry> entries = repositoryManager.queryByTypeLimitAccess(id,
				re.getOlatResource().getResourceableTypeName(), new Roles(false, false, false, false, false, false, false));
		
		Assert.assertNotNull(entries);
		Assert.assertFalse(entries.isEmpty());
		Assert.assertTrue(entries.contains(re));
		for(RepositoryEntry entry:entries) {
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
	}
	
	@Test
	public void isMember() {
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-member-lc-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "memberg", "tg", null, null, false, false, re.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id1, group.getOwnerGroup());
		dbInstance.commitAndCloseSession();

		//id1 is member
		boolean member1 = repositoryManager.isMember(id1, re);
		Assert.assertTrue(member1);
		//id2 is not member
		boolean member2 = repositoryManager.isMember(id2, re);
		Assert.assertFalse(member2);
	}
	
	@Test
	public void isOwnerOfRepositoryEntry() {
		//create a repository entry with an owner and a participant
		Identity owner = JunitTestHelper.createAndPersistIdentityAsUser("re-owner-is-" + UUID.randomUUID().toString());
		Identity part = JunitTestHelper.createAndPersistIdentityAsUser("re-owner-is-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		securityManager.addIdentityToSecurityGroup(owner, re.getOwnerGroup());
		securityManager.addIdentityToSecurityGroup(part, re.getParticipantGroup());
		dbInstance.commitAndCloseSession();
		
		//check
		boolean isOwnerOwner = repositoryManager.isOwnerOfRepositoryEntry(owner, re);
		Assert.assertTrue(isOwnerOwner);
		boolean isPartOwner = repositoryManager.isOwnerOfRepositoryEntry(part, re);
		Assert.assertFalse(isPartOwner);
	}
	
	@Test
	public void isIdentityInTutorSecurityGroup() {
		//create a repository entry with an owner and a participant
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("re-tutor-is-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re3 = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		securityManager.addIdentityToSecurityGroup(identity, re1.getTutorGroup());
		securityManager.addIdentityToSecurityGroup(identity, re2.getParticipantGroup());
		securityManager.addIdentityToSecurityGroup(identity, re3.getOwnerGroup());
		dbInstance.commitAndCloseSession();
		
		//check
		boolean isTutor1 = repositoryManager.isIdentityInTutorSecurityGroup(identity, re1.getOlatResource());
		Assert.assertTrue(isTutor1);
		boolean isTutor2 = repositoryManager.isIdentityInTutorSecurityGroup(identity, re2.getOlatResource());
		Assert.assertFalse(isTutor2);
		boolean isTutor3 = repositoryManager.isIdentityInTutorSecurityGroup(identity, re3.getOlatResource());
		Assert.assertFalse(isTutor3);
	}
	
	@Test
	public void isIdentityInParticipantSecurityGroup() {
		//create a repository entry with an owner and a participant
		Identity identity = JunitTestHelper.createAndPersistIdentityAsUser("re-tutor-is-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re3 = JunitTestHelper.createAndPersistRepositoryEntry();
		dbInstance.commitAndCloseSession();
		securityManager.addIdentityToSecurityGroup(identity, re1.getTutorGroup());
		securityManager.addIdentityToSecurityGroup(identity, re2.getParticipantGroup());
		securityManager.addIdentityToSecurityGroup(identity, re3.getOwnerGroup());
		dbInstance.commitAndCloseSession();
		
		//check
		boolean isParticipant1 = repositoryManager.isIdentityInParticipantSecurityGroup(identity, re1.getOlatResource());
		Assert.assertFalse(isParticipant1);
		boolean isParticipant2 = repositoryManager.isIdentityInParticipantSecurityGroup(identity, re2.getOlatResource());
		Assert.assertTrue(isParticipant2);
		boolean isParticipant3 = repositoryManager.isIdentityInParticipantSecurityGroup(identity, re3.getOlatResource());
		Assert.assertFalse(isParticipant3);
	}
	

	@Test
	public void testCountByTypeLimitAccess() {
		String TYPE = UUID.randomUUID().toString().replace("-", "");
		
		int count = repositoryManager.countByTypeLimitAccess("unkown", RepositoryEntry.ACC_OWNERS_AUTHORS);
    assertEquals("Unkown type must return 0 elements", 0,count);
    int countValueBefore = repositoryManager.countByTypeLimitAccess(TYPE, RepositoryEntry.ACC_OWNERS_AUTHORS);
    // add 1 entry
    RepositoryEntry re = createRepositoryEntry(TYPE, 999999l);
		// create security group
		SecurityGroup ownerGroup = securityManager.createAndPersistSecurityGroup();
		re.setOwnerGroup(ownerGroup);
    repositoryManager.saveRepositoryEntry(re);
    count = repositoryManager.countByTypeLimitAccess(TYPE, RepositoryEntry.ACC_OWNERS_AUTHORS);
    // check count must be one more element
    assertEquals("Add one course repository-entry, but countByTypeLimitAccess does NOT return one more element", countValueBefore + 1,count);
	}
	
	@Test
	public void genericANDQueryWithRolesRestrictionMembersOnly() {
		//create 2 identities (repo owner and tutor)
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-gen-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-gen-2-" + UUID.randomUUID().toString());
		Identity id3 = JunitTestHelper.createAndPersistIdentityAsUser("re-gen-3-" + UUID.randomUUID().toString());
		RepositoryEntry re = JunitTestHelper.createAndPersistRepositoryEntry(true);
		securityManager.addIdentityToSecurityGroup(id1, re.getOwnerGroup());
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "teacherg", "tg", null, null, false, false, re.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id2, group.getOwnerGroup());
		dbInstance.commitAndCloseSession();
		
		
		//check for id 1 (owner of the repository entry)
		SearchRepositoryEntryParameters params1 = new SearchRepositoryEntryParameters();
		params1.setIdentity(id1);
		params1.setRoles(new Roles(false, false, false, false, false, false, false));
		params1.setOnlyExplicitMember(true);
		List<RepositoryEntry> entries1 = repositoryManager.genericANDQueryWithRolesRestriction(params1, 0, -1, true);
		Assert.assertNotNull(entries1);
		Assert.assertFalse(entries1.isEmpty());
		Assert.assertTrue(entries1.contains(re));
		for(RepositoryEntry entry:entries1) {
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
		
		//check for id2 (tutor)
		SearchRepositoryEntryParameters params2 = new SearchRepositoryEntryParameters();
		params2.setIdentity(id2);
		params2.setRoles(new Roles(false, false, false, false, false, false, false));
		params2.setOnlyExplicitMember(true);
		List<RepositoryEntry> entries2 = repositoryManager.genericANDQueryWithRolesRestriction(params2, 0, -1, true);
		Assert.assertNotNull(entries2);
		Assert.assertFalse(entries2.isEmpty());
		Assert.assertTrue(entries2.contains(re));
		for(RepositoryEntry entry:entries2) {
			if(!entry.equals(re)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
		
		//check for id3 (negative test)
		SearchRepositoryEntryParameters params3 = new SearchRepositoryEntryParameters();
		params3.setIdentity(id3);
		params3.setRoles(new Roles(false, false, false, false, false, false, false));
		params3.setOnlyExplicitMember(true);
		List<RepositoryEntry> entries3 = repositoryManager.genericANDQueryWithRolesRestriction(params3, 0, -1, true);
		Assert.assertNotNull(entries3);
		Assert.assertFalse(entries3.contains(re));
		for(RepositoryEntry entry:entries3) {
			Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
		}
	}
	
	@Test
	public void genericANDQueryWithRolesWithStandardUser() {
		//create 2 identities (repo owner and tutor)
		Identity id1 = JunitTestHelper.createAndPersistIdentityAsUser("re-gen-1-" + UUID.randomUUID().toString());
		Identity id2 = JunitTestHelper.createAndPersistIdentityAsUser("re-gen-2-" + UUID.randomUUID().toString());
		RepositoryEntry re1 = JunitTestHelper.createAndPersistRepositoryEntry();
		RepositoryEntry re2 = JunitTestHelper.createAndPersistRepositoryEntry(true);
		
		securityManager.addIdentityToSecurityGroup(id1, re2.getParticipantGroup());
		BusinessGroup group = businessGroupService.createBusinessGroup(null, "teacherg", "tg", null, null, false, false, re1.getOlatResource());
		securityManager.addIdentityToSecurityGroup(id2, group.getPartipiciantGroup());
		dbInstance.commitAndCloseSession();
		
		
		//check for guest (negative test)
		SearchRepositoryEntryParameters params1 = new SearchRepositoryEntryParameters();
		params1.setRoles(new Roles(false, false, false, false, true, false, false));
		List<RepositoryEntry> entries1 = repositoryManager.genericANDQueryWithRolesRestriction(params1, 0, -1, true);
		Assert.assertNotNull(entries1);
		Assert.assertFalse(entries1.contains(re1));
		Assert.assertFalse(entries1.contains(re2));
		for(RepositoryEntry entry:entries1) {
			Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS_GUESTS);
		}
		
		//check for identity 1 (participant re2 + re1 accessible to all users)
		SearchRepositoryEntryParameters params2 = new SearchRepositoryEntryParameters();
		params2.setIdentity(id1);
		params2.setRoles(new Roles(false, false, false, false, false, false, false));
		List<RepositoryEntry> entries2 = repositoryManager.genericANDQueryWithRolesRestriction(params2, 0, -1, true);
		Assert.assertNotNull(entries2);
		Assert.assertFalse(entries2.isEmpty());
		Assert.assertTrue(entries2.contains(re1));
		Assert.assertTrue(entries2.contains(re2));
		for(RepositoryEntry entry:entries2) {
			if(!entry.equals(re2)) {
				Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
			}
		}
		
		//check for identity 1 (re1 accessible to all users)
		SearchRepositoryEntryParameters params3 = new SearchRepositoryEntryParameters();
		params3.setIdentity(id2);
		params3.setRoles(new Roles(false, false, false, false, false, false, false));
		List<RepositoryEntry> entries3 = repositoryManager.genericANDQueryWithRolesRestriction(params3, 0, -1, true);
		Assert.assertNotNull(entries3);
		Assert.assertFalse(entries3.isEmpty());
		Assert.assertTrue(entries3.contains(re1));
		Assert.assertFalse(entries3.contains(re2));
		for(RepositoryEntry entry:entries3) {
			Assert.assertTrue(entry.getAccess() >= RepositoryEntry.ACC_USERS);
		}
	}

	private RepositoryEntry createRepositoryEntry(final String type, long i) {
		OLATResourceable resourceable = OresHelper.createOLATResourceableInstance(type, new Long(i));
		OLATResource r =  resourceManager.createOLATResourceInstance(resourceable);
		dbInstance.saveObject(r);
		
		// now make a repository entry for this course
		final RepositoryEntry re = repositoryManager.createRepositoryEntryInstance("Florian Gnägi", "Lernen mit OLAT " + i, "yo man description bla bla + i");
		re.setDisplayname("JunitTest_RepositoryEntry_" + i);		
		re.setOlatResource(r);
		re.setAccess(RepositoryEntry.ACC_OWNERS_AUTHORS);
		return re;
	}
}
