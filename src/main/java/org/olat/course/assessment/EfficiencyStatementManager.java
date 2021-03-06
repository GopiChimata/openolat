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

package org.olat.course.assessment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.IdentityRef;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.assessment.model.UserEfficiencyStatementImpl;
import org.olat.course.assessment.model.UserEfficiencyStatementLight;
import org.olat.course.assessment.model.UserEfficiencyStatementStandalone;
import org.olat.course.config.CourseConfig;
import org.olat.course.nodes.CourseNode;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryRef;
import org.olat.repository.RepositoryManager;
import org.olat.repository.model.RepositoryEntryRefImpl;
import org.olat.resource.OLATResource;
import org.olat.user.UserDataDeletable;
import org.olat.user.UserManager;

import com.thoughtworks.xstream.XStream;

/**
 * Description:<br>
 * Methods to update a users efficiency statement and to retrieve such statements
 * from the database.
 * 
 * <P>
 * Initial Date:  11.08.2005 <br>
 * @author gnaegi
 */
public class EfficiencyStatementManager extends BasicManager implements UserDataDeletable {

	public static final String KEY_ASSESSMENT_NODES = "assessmentNodes";
	public static final String KEY_COURSE_TITLE = "courseTitle";
	public static final String PROPERTY_CATEGORY = "efficiencyStatement";
	
	private static EfficiencyStatementManager INSTANCE;

	private DB dbInstance;
	private UserManager userManager;
	private RepositoryManager repositoryManager;
	private final XStream xstream = XStreamHelper.createXStreamInstance();

	
	/**
	 * Constructor
	 */
	private EfficiencyStatementManager() {
		INSTANCE = this;
	}
	
	/**
	 * Factory method
	 * @return
	 */
	public static EfficiencyStatementManager getInstance() {
		return INSTANCE;
	}
	
	/**
	 * [used by Spring]
	 * @param dbInstance
	 */
	public void setDbInstance(DB dbInstance) {
		this.dbInstance = dbInstance;
	}
	
	/**
	 * [used by Spring]
	 * @param repositoryManager
	 */
	public void setRepositoryManager(RepositoryManager repositoryManager) {
		this.repositoryManager = repositoryManager;
	}
	
	/**
	 * [used by Spring]
	 * @param userManager
	 */
	public void setUserManager(UserManager userManager) {
		this.userManager = userManager;
	}

	/**
	 * Updates the users efficiency statement for this course. <p>
	 * Called in AssessmentManager in a <code>doInSync</code> block, toghether with the saveScore.
	 * @param userCourseEnv
	 */
	protected void updateUserEfficiencyStatement(UserCourseEnvironment userCourseEnv) {
		Long courseResId = userCourseEnv.getCourseEnvironment().getCourseResourceableId(); 
		OLATResourceable courseOres = OresHelper.createOLATResourceableInstance(CourseModule.class, courseResId);
		RepositoryEntry re = repositoryManager.lookupRepositoryEntry(courseOres, false);
		updateUserEfficiencyStatement(userCourseEnv, re.getKey(), courseOres);
	}

	public UserEfficiencyStatement createUserEfficiencyStatement(Date creationDate, Float score, Boolean passed, Identity identity, OLATResource resource) {
		UserEfficiencyStatementImpl efficiencyProperty = new UserEfficiencyStatementImpl();
		efficiencyProperty.setCreationDate(creationDate);
		efficiencyProperty.setLastModified(new Date());
		efficiencyProperty.setScore(score);
		efficiencyProperty.setPassed(passed);

		efficiencyProperty.setTotalNodes(0);
		efficiencyProperty.setAttemptedNodes(0);
		efficiencyProperty.setPassedNodes(0);

		efficiencyProperty.setIdentity(identity);
		efficiencyProperty.setResource(resource);

		ICourse course = CourseFactory.loadCourse(resource.getResourceableId());
		efficiencyProperty.setTitle(course.getCourseEnvironment().getCourseTitle());
		efficiencyProperty.setShortTitle(course.getCourseEnvironment().getRunStructure().getRootNode().getShortTitle());
		efficiencyProperty.setCourseRepoKey(course.getCourseEnvironment().getCourseGroupManager().getCourseEntry().getKey());

		dbInstance.getCurrentEntityManager().persist(efficiencyProperty);

		return efficiencyProperty;
	}
	
	public UserEfficiencyStatement createStandAloneUserEfficiencyStatement(Date creationDate, Float score, Boolean passed,
			Identity identity, Long resourceKey, String courseTitle) {
		UserEfficiencyStatementStandalone efficiencyProperty = new UserEfficiencyStatementStandalone();
		efficiencyProperty.setCreationDate(creationDate);
		efficiencyProperty.setLastModified(new Date());
		efficiencyProperty.setScore(score);
		efficiencyProperty.setPassed(passed);

		efficiencyProperty.setTotalNodes(0);
		efficiencyProperty.setAttemptedNodes(0);
		efficiencyProperty.setPassedNodes(0);

		efficiencyProperty.setIdentity(identity);
		efficiencyProperty.setResourceKey(resourceKey);

		efficiencyProperty.setTitle(courseTitle);
		efficiencyProperty.setShortTitle(courseTitle);
		efficiencyProperty.setCourseRepoKey(null);

		dbInstance.getCurrentEntityManager().persist(efficiencyProperty);

		return efficiencyProperty;
	}
	
	
	/**
	 * Updates the users efficiency statement for this course
	 * @param userCourseEnv
	 * @param repoEntryKey
	 * @param courseOres
	 */
	private void updateUserEfficiencyStatement(final UserCourseEnvironment userCourseEnv, final Long repoEntryKey, OLATResourceable courseOres) {
    //	o_clusterOK: by ld
		CourseConfig cc = userCourseEnv.getCourseEnvironment().getCourseConfig();
		// write only when enabled for this course
		if (cc.isEfficencyStatementEnabled()) {
			Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();				

			CourseNode rootNode = userCourseEnv.getCourseEnvironment().getRunStructure().getRootNode(); 
			List<Map<String,Object>> assessmentNodes = AssessmentHelper.addAssessableNodeAndDataToList(0, rootNode, userCourseEnv, true, true);
					
			EfficiencyStatement efficiencyStatement = new EfficiencyStatement();
			efficiencyStatement.setAssessmentNodes(assessmentNodes);
			efficiencyStatement.setCourseTitle(userCourseEnv.getCourseEnvironment().getCourseTitle());
			efficiencyStatement.setCourseRepoEntryKey(repoEntryKey);
			String userInfos = userManager.getUserDisplayName(identity);
			efficiencyStatement.setDisplayableUserInfo(userInfos);
			efficiencyStatement.setLastUpdated(System.currentTimeMillis());
							
			UserEfficiencyStatementImpl efficiencyProperty = getUserEfficiencyStatementFull(repoEntryKey, identity);
			if (assessmentNodes != null) {				
				if (efficiencyProperty == null) {
					// create new
					efficiencyProperty = new UserEfficiencyStatementImpl();
					efficiencyProperty.setIdentity(identity);
					efficiencyProperty.setCourseRepoKey(repoEntryKey);
					RepositoryEntry re = repositoryManager.lookupRepositoryEntry(repoEntryKey, false);
					if(re != null) {
						efficiencyProperty.setResource(re.getOlatResource());
						efficiencyProperty.setCourseRepoKey(re.getKey());
					}
					
					fillEfficiencyStatement(efficiencyStatement, efficiencyProperty);
					dbInstance.saveObject(efficiencyProperty);
					if (isLogDebugEnabled()) {
						logDebug("creating new efficiency statement property::" + efficiencyProperty.getKey() + " for id::" + identity.getName() + " repoEntry::" + repoEntryKey);
					}				
				} else {
					// update existing
					if (isLogDebugEnabled()) {
						logDebug("updating efficiency statement property::" + efficiencyProperty.getKey() + " for id::" + identity.getName() + " repoEntry::" + repoEntryKey);
					}	
					fillEfficiencyStatement(efficiencyStatement, efficiencyProperty);
					dbInstance.updateObject(efficiencyProperty);
				}
			} else {
				if (efficiencyProperty != null) {
					// remove existing since now empty empty efficiency statements
					if (isLogDebugEnabled()) {
						logDebug("removing efficiency statement property::" + efficiencyProperty.getKey() + " for id::"	+ identity.getName() + " repoEntry::" + repoEntryKey + " since empty");
					}
					dbInstance.deleteObject(efficiencyProperty);
				}
				// else nothing to create and nothing to delete
			}					
			
			// send modified event to everybody
			AssessmentChangedEvent ace = new AssessmentChangedEvent(AssessmentChangedEvent.TYPE_EFFICIENCY_STATEMENT_CHANGED, identity);
			CoordinatorManager.getInstance().getCoordinator().getEventBus().fireEventToListenersOf(ace, courseOres);
		}
	}
	
	public void fillEfficiencyStatement(EfficiencyStatement efficiencyStatement, UserEfficiencyStatementImpl efficiencyProperty) {
		List<Map<String,Object>> nodeData = efficiencyStatement.getAssessmentNodes();
		if(!nodeData.isEmpty()) {
			Map<String,Object> rootNode = nodeData.get(0);
			Object passed = rootNode.get(AssessmentHelper.KEY_PASSED);
			if(passed instanceof Boolean) {
				efficiencyProperty.setPassed((Boolean)passed);
			}
			
			Object fscore = rootNode.get(AssessmentHelper.KEY_SCORE_F);
			if(fscore instanceof Float) {
				efficiencyProperty.setScore((Float)fscore);
			}
	
			Object shortTitle = rootNode.get(AssessmentHelper.KEY_TITLE_SHORT);
			if(shortTitle instanceof String) {
				efficiencyProperty.setShortTitle((String)shortTitle);
			}
			
			Object longTitle = rootNode.get(AssessmentHelper.KEY_TITLE_LONG);
			if(longTitle instanceof String) {
				efficiencyProperty.setTitle((String)longTitle);
			}
			
			int totalNodes = getTotalNodes(nodeData);
			efficiencyProperty.setTotalNodes(totalNodes);
			
			int attemptedNodes = getAttemptedNodes(nodeData);
			efficiencyProperty.setAttemptedNodes(attemptedNodes);
			
			int passedNodes = getPassedNodes(nodeData);
			efficiencyProperty.setPassedNodes(passedNodes);
		}

		efficiencyProperty.setLastModified(new Date());
		efficiencyProperty.setStatementXml(xstream.toXML(efficiencyStatement));
	}
	
	/**
	 * LD: Debug method. 
	 * @param efficiencyStatement
	 */
	protected void printEfficiencyStatement(EfficiencyStatement efficiencyStatement) {
		List<Map<String,Object>> assessmentNodes = efficiencyStatement.getAssessmentNodes();
		if (assessmentNodes != null) {
			Iterator<Map<String,Object>> iter = assessmentNodes.iterator();
			while (iter.hasNext()) {
				Map<String,Object> nodeData = iter.next();
				String title = (String)nodeData.get(AssessmentHelper.KEY_TITLE_SHORT);
				String score = (String)nodeData.get(AssessmentHelper.KEY_SCORE);
				Boolean passed = (Boolean)nodeData.get(AssessmentHelper.KEY_PASSED);
				Integer attempts = (Integer)nodeData.get(AssessmentHelper.KEY_ATTEMPTS);
				String attemptsStr = attempts==null ? null : String.valueOf(attempts.intValue());				
				logInfo("title: " + title + " score: " + score + " passed: " + passed + " attempts: " + attemptsStr);				
			}
		}		
	}
	

	/**
	 * Get the user efficiency statement list for this course
	 * @param courseRepoEntryKey
	 * @param identity
	 * @return Map containing a list of maps that contain the nodeData for this user and course using the
	 * keys defined in the AssessmentHelper and the title of the course
	 */
	public EfficiencyStatement getUserEfficiencyStatementByCourseRepoKey(Long courseRepoEntryKey, Identity identity){
		UserEfficiencyStatementImpl s = getUserEfficiencyStatementFull(courseRepoEntryKey, identity);
		if(s == null || s.getStatementXml() == null) {
			return null;
		}
		return (EfficiencyStatement)xstream.fromXML(s.getStatementXml());
	}
	
	public EfficiencyStatement getUserEfficiencyStatementByResourceKey(Long resourceKey, Identity identity){
		StringBuilder sb = new StringBuilder();
		sb.append("select statement from ").append(UserEfficiencyStatementStandalone.class.getName()).append(" as statement ")
		  .append(" where statement.identity.key=:identityKey and statement.resourceKey=:resourceKey");

		List<UserEfficiencyStatementStandalone> statement = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), UserEfficiencyStatementStandalone.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("resourceKey", resourceKey)
				.getResultList();
		if(statement.isEmpty() || statement.get(0).getStatementXml() == null) {
			return null;
		}
		return (EfficiencyStatement)xstream.fromXML(statement.get(0).getStatementXml());
	}
	

	public UserEfficiencyStatementImpl getUserEfficiencyStatementFull(Long courseRepoEntryKey, Identity identity) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.identity.key=:identityKey and statement.courseRepoKey=:repoKey");

			List<UserEfficiencyStatementImpl> statement = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementImpl.class)
					.setParameter("identityKey", identity.getKey())
					.setParameter("repoKey", courseRepoEntryKey)
					.getResultList();
			if(statement.isEmpty()) {
				return null;
			}
			return statement.get(0);
		} catch (Exception e) {
			logError("Cannot retrieve efficiency statement: " + courseRepoEntryKey + " from " + identity, e);
			return null;
		}
	}
	
	public UserEfficiencyStatementImpl getUserEfficiencyStatementFullByResourceKey(Long resourceKey, Identity identity) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.identity.key=:identityKey")
			  .append(" and statement.resource.key=:resourceKey");

			List<UserEfficiencyStatementImpl> statement = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementImpl.class)
					.setParameter("identityKey", identity.getKey())
					.setParameter("resourceKey", resourceKey)
					.getResultList();
			if(statement.isEmpty()) {
				return null;
			}
			return statement.get(0);
		} catch (Exception e) {
			logError("Cannot retrieve efficiency statement: " + resourceKey + " from " + identity, e);
			return null;
		}
	}
	
	public boolean hasUserEfficiencyStatement(Long courseRepoEntryKey, IdentityRef identity) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(statement) from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
		  .append(" where statement.identity.key=:identityKey and statement.courseRepoKey=:repoKey");

		List<Number> count = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("identityKey", identity.getKey())
				.setParameter("repoKey", courseRepoEntryKey)
				.getResultList();
		if(count.isEmpty()) {
			return false;
		}
		return count.get(0).intValue() > 0;
	}
	
	public UserEfficiencyStatement getUserEfficiencyStatementLightByRepositoryEntry(RepositoryEntryRef courseRepo, IdentityRef identity) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
			  .append(" where statement.identity.key=:identityKey and statement.courseRepoKey=:repoKey");

			List<UserEfficiencyStatement> statement = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatement.class)
					.setParameter("identityKey", identity.getKey())
					.setParameter("repoKey", courseRepo.getKey())
					.getResultList();
			if(statement.isEmpty()) {
				return null;
			}
			return statement.get(0);
		} catch (Exception e) {
			logError("Cannot retrieve efficiency statement: " + courseRepo.getKey() + " from " + identity, e);
			return null;
		}
	}
	
	public UserEfficiencyStatement getUserEfficiencyStatementLightByResource(Long resourceKey, IdentityRef identity) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
			  .append(" where statement.identity.key=:identityKey and statement.resource.key=:resourceKey");

			List<UserEfficiencyStatement> statement = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatement.class)
					.setParameter("identityKey", identity.getKey())
					.setParameter("resourceKey", resourceKey)
					.getResultList();
			if(statement.isEmpty()) {
				return null;
			}
			return statement.get(0);
		} catch (Exception e) {
			logError("Cannot retrieve efficiency statement: " + resourceKey + " from " + identity, e);
			return null;
		}
	}
	
	public EfficiencyStatement getUserEfficiencyStatementByKey(Long key) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.key=:key");

			List<UserEfficiencyStatementImpl> statement = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementImpl.class)
					.setParameter("key", key)
					.getResultList();
			if(statement.isEmpty()) {
				return null;
			}
			return (EfficiencyStatement)xstream.fromXML(statement.get(0).getStatementXml());
		} catch (Exception e) {
			logError("Cannot retrieve efficiency statement: " + key, e);
			return null;
		}
	}
	
	public UserEfficiencyStatementLight getUserEfficiencyStatementLightByKey(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select statement from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
		  .append(" where statement.key=:key");

		List<UserEfficiencyStatementLight> statement = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), UserEfficiencyStatementLight.class)
				.setParameter("key", key)
				.getResultList();
		if(statement.isEmpty()) {
			return null;
		}
		return statement.get(0);
	}

	/**
	 * Get the passed value of a course node of a specific efficiency statment
	 * @param nodeIdent
	 * @param efficiencyStatement
	 * @return true if passed, false if not, null if node not found
	 */
	public Boolean getPassed(String nodeIdent, EfficiencyStatement efficiencyStatement) {
		List<Map<String,Object>> assessmentNodes = efficiencyStatement.getAssessmentNodes();
		if (assessmentNodes != null) {
			Iterator<Map<String,Object>> iter = assessmentNodes.iterator();
			while (iter.hasNext()) {
				Map<String,Object> nodeData = iter.next();
				if (nodeData.get(AssessmentHelper.KEY_IDENTIFYER).equals(nodeIdent)) {
					return (Boolean) nodeData.get(AssessmentHelper.KEY_PASSED);
				}
			}
		}
		return null;
	}
	
	public int getTotalNodes(List<Map<String,Object>> assessmentNodes) {
		int count = 0;
		for (Iterator<Map<String,Object>> iter = assessmentNodes.iterator(); iter.hasNext(); ) {
			Map<String,Object> nodeData = iter.next();
			Boolean selectable = (Boolean)nodeData.get(AssessmentHelper.KEY_SELECTABLE);
			if(selectable != null && selectable.booleanValue()) {
				count++;
			}
		}
		return count;
	}
	
	public int getAttemptedNodes(List<Map<String,Object>> assessmentNodes) {
		int count = 0;
		for (Iterator<Map<String,Object>> iter = assessmentNodes.iterator(); iter.hasNext(); ) {
			Map<String,Object> nodeData = iter.next();
			Boolean selectable = (Boolean)nodeData.get(AssessmentHelper.KEY_SELECTABLE);
			if(selectable != null && selectable.booleanValue()) {
				if(nodeData.containsKey(AssessmentHelper.KEY_SCORE)) {
					count++;
				} else if (nodeData.containsKey(AssessmentHelper.KEY_PASSED)) {
					count++;
				}
			}
		}
		return count;
	}
	
	public int getPassedNodes(List<Map<String,Object>> assessmentNodes) {
		int count = 0;
		for (Iterator<Map<String,Object>> iter = assessmentNodes.iterator(); iter.hasNext(); ) {
			Map<String,Object> nodeData = iter.next();
			Boolean passed = (Boolean)nodeData.get(AssessmentHelper.KEY_PASSED);
			Boolean selectable = (Boolean)nodeData.get(AssessmentHelper.KEY_SELECTABLE);
			if(passed != null && passed.booleanValue() && selectable != null && selectable.booleanValue()) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Get the score value of a course node of a specific efficiency statment
	 * @param nodeIdent
	 * @param efficiencyStatement
	 * @return the score, null if node not found
	 */
	public Double getScore(String nodeIdent, EfficiencyStatement efficiencyStatement) {
		List<Map<String,Object>> assessmentNodes = efficiencyStatement.getAssessmentNodes();
		if (assessmentNodes != null) {
			Iterator<Map<String,Object>> iter = assessmentNodes.iterator();
			while (iter.hasNext()) {
				Map<String,Object> nodeData = iter.next();
				if (nodeData.get(AssessmentHelper.KEY_IDENTIFYER).equals(nodeIdent)) {
					String scoreString = (String) nodeData.get(AssessmentHelper.KEY_SCORE);
					return Double.valueOf(scoreString);
				}
			}
		}
		return null;
	}

	
	/**
	 * Find all efficiency statements for a specific user
	 * @param identity
	 * @return List of efficiency statements
	 */
	protected List<EfficiencyStatement> findEfficiencyStatements(Identity identity) {
		List<EfficiencyStatement> efficiencyStatements = new ArrayList<EfficiencyStatement>();
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.identity.key=:identityKey");

			List<UserEfficiencyStatementImpl> statements = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementImpl.class)
					.setParameter("identityKey", identity.getKey())
					.getResultList();
			for(UserEfficiencyStatementImpl statement:statements) {
				EfficiencyStatement s = (EfficiencyStatement)xstream.fromXML(statement.getStatementXml());
				efficiencyStatements.add(s);
			}

		} catch (Exception e) {
			logError("findEfficiencyStatements: " + identity, e);
		}
		return efficiencyStatements;
	}
	
	public List<UserEfficiencyStatementLight> findEfficiencyStatementsLight(Identity identity) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
				.append(" left join fetch statement.resource resource")
			  .append(" where statement.identity.key=:identityKey");

			return dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementLight.class)
					.setParameter("identityKey", identity.getKey())
					.getResultList();
		} catch (Exception e) {
			logError("findEfficiencyStatements: " + identity, e);
			return Collections.emptyList();
		}
	}
	
	public List<UserEfficiencyStatementLight> findEfficiencyStatementsLight(List<Long> keys) {
		if(keys == null || keys.isEmpty()) return Collections.emptyList();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select statement from ").append(UserEfficiencyStatementLight.class.getName()).append(" as statement ")
			.append(" left join fetch statement.resource resource")
		  .append(" where statement.key in (:keys)");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), UserEfficiencyStatementLight.class)
				.setParameter("keys", keys)
				.getResultList();
	}
	
	/**
	 * Find all identities who have an efficiency statement for this course repository entry
	 * @param courseRepoEntryKey
	 * @return List of identities
	 */
	protected List<Identity> findIdentitiesWithEfficiencyStatements(Long courseRepoEntryKey) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select distinct(statement.identity) from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.courseRepoKey=:repoKey");

			return dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), Identity.class)
					.setParameter("repoKey", courseRepoEntryKey)
					.getResultList();
		} catch (Exception e) {
			logError("findIdentitiesWithEfficiencyStatements: " + courseRepoEntryKey, e);
			return Collections.emptyList();
		}
	}
	
	/**
	 * Delete all efficiency statements from the given course for all users
	 * @param courseRepoEntryKey
	 * @return int number of deleted efficiency statements
	 */
	public void deleteEfficiencyStatementsFromCourse(Long courseRepoEntryKey) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("select statement from ").append(UserEfficiencyStatementImpl.class.getName()).append(" as statement ")
			  .append(" where statement.courseRepoKey=:repoKey");

			List<UserEfficiencyStatementImpl> statements = dbInstance.getCurrentEntityManager()
					.createQuery(sb.toString(), UserEfficiencyStatementImpl.class)
					.setParameter("repoKey", courseRepoEntryKey)
					.getResultList();
			for(UserEfficiencyStatementImpl statement:statements) {
				dbInstance.deleteObject(statement);
			}
		} catch (Exception e) {
			logError("deleteEfficiencyStatementsFromCourse: " + courseRepoEntryKey, e);
		}
	}

	/**
	 * Delete the given efficiency statement for this person
	 * @param identity
	 * @param efficiencyStatement
	 */
	protected void deleteEfficiencyStatement(Identity identity, EfficiencyStatement efficiencyStatement) {
		RepositoryEntryRef ref = new RepositoryEntryRefImpl(efficiencyStatement.getCourseRepoEntryKey());
		UserEfficiencyStatement s = getUserEfficiencyStatementLightByRepositoryEntry(ref, identity);
		if(s != null) {
			dbInstance.getCurrentEntityManager().remove(s);
		}
	}
	
	/**
	 * Delete the given efficiency statement for this person
	 * @param efficiencyStatement
	 */
	public void deleteEfficiencyStatement(UserEfficiencyStatementLight efficiencyStatement) {
		dbInstance.getCurrentEntityManager().remove(efficiencyStatement);
	}

	/**
	 * Create or update all efficiency statment lists for the given list of identities and this course
	 * This is called from only one thread, since the course is locked at editing (either CourseEdit or CourseDetails edit).
	 * 
	 * @param ores The resource to load the course
	 * @param identities List of identities
	 * false: always create new one (be careful with this one!)
	 */	
	public void updateEfficiencyStatements(OLATResourceable ores, List<Identity> identities) {
		if (identities.size() > 0) {
			final ICourse course = CourseFactory.loadCourse(ores);
			logAudit("Updating efficiency statements for course::" + course.getResourceableId() + ", this might produce temporary heavy load on the CPU");
			Long courseResId = course.getCourseEnvironment().getCourseResourceableId(); 
			final RepositoryEntry re = repositoryManager.lookupRepositoryEntry(
					OresHelper.createOLATResourceableInstance(CourseModule.class, courseResId), false);

			// preload cache to speed up things
			long start = System.currentTimeMillis();
			AssessmentManager am = course.getCourseEnvironment().getAssessmentManager();

			Iterator<Identity> iter = identities.iterator();			
			while (iter.hasNext()) {
				final Identity identity = iter.next();					
				//o_clusterOK: by ld
				OLATResourceable efficiencyStatementResourceable = am.createOLATResourceableForLocking(identity);
				CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(efficiencyStatementResourceable, new SyncerExecutor() {
					public void execute() {					
						// create temporary user course env
						UserCourseEnvironment uce = AssessmentHelper.createAndInitUserCourseEnvironment(identity, course);
						updateUserEfficiencyStatement(uce, re.getKey(), course);
					}
				});
				if (Thread.interrupted()) break;
			}
			//}
			if (isLogDebugEnabled()) {
				long end = System.currentTimeMillis();
				logDebug("Updated efficiency statements for course::" + course.getResourceableId() 
					 + "ms; Updating statements: " + (end-start) + "ms; Users: " + identities.size());
			}
		}
	}

	public void archiveUserData(Identity identity, File archiveDir) {
		List<EfficiencyStatement> efficiencyStatements = findEfficiencyStatements(identity);
		EfficiencyStatementArchiver.getInstance().archive(efficiencyStatements, identity, archiveDir);
	}
	
	/**
	 * Delete all efficiency-statements for certain identity.
	 * @param identity  Delete data for this identity.
	 */
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		List<EfficiencyStatement> efficiencyStatements = findEfficiencyStatements(identity);
		for (Iterator<EfficiencyStatement> iter = efficiencyStatements.iterator(); iter.hasNext();) {
			deleteEfficiencyStatement(identity, iter.next());
		}
		logDebug("All efficiency statements deleted for identity=" + identity);
	}
	
}
