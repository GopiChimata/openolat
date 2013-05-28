package de.unileipzig.xman.exam;

import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.id.Identity;
import org.olat.core.id.ModifiedInfo;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryStatus;
import org.olat.repository.RepositoryManager;
import org.olat.resource.references.ReferenceImpl;

import de.unileipzig.xman.appointment.Appointment;
import de.unileipzig.xman.appointment.AppointmentManager;
import de.unileipzig.xman.protocol.Protocol;
import de.unileipzig.xman.protocol.ProtocolManager;

/**
 * Implementation of the Exam interface. Declares all class variables.
 * It defines the method isOral().
 * 
 * @author iggy
 *
 */
public class ExamImpl extends PersistentObject implements Exam {
	
	private String name;
	private Date regStartDate;
	private Date regEndDate;
	private Date signOffDate;
	private Date lastModified;
	private boolean isOral;
	private boolean earmarkedEnabled;
	private String comments;
	private Identity identity;
	
	public ExamImpl() {
		
		// nothing to do here
	}
	
	/**
	 * returns the repoEntry of the Course the exam belongs to
	 * returns null if there is no reference for this exam yet
	 */
	public RepositoryEntry getCourseRepoEntry() {
		
		ReferenceImpl ref = this.getCourseReference();
		if (ref == null) return null;
				
		String query = "select c from " +
				"org.olat.repository.RepositoryEntry c inner join fetch c.olatResource as courseO," +
				"org.olat.resource.references.ReferenceImpl ref " +
				"where ref.source = courseO " +
				"and ref = :ref";
		
		DBQuery dbQuery = DBFactory.getInstance().createQuery(query);
		dbQuery.setEntity("ref", ref);
		
		List result = dbQuery.list();
		if(result.size()==0) return null;
		else {
			RepositoryEntry rep = (RepositoryEntry)result.get(0);
			return rep;
		}
		
		
	}
	/**
	 * returns the reference of the exam belongs to, if it was put into a course
	 * returns null if there is no reference for this exam yet
	 */
	public ReferenceImpl getCourseReference() {
				
		String query = "select ref from " +
				"org.olat.resource.references.ReferenceImpl ref, " +
				"org.olat.resource.OLATResourceImpl ores " +
				"where ores.resId = :examId " +
				"and ref.target = ores";
		
		DBQuery dbQuery = DBFactory.getInstance().createQuery(query);
		dbQuery.setLong("examId", this.getKey().longValue());
		
		List result = dbQuery.list();
		if(result.size()==0) return null;
		else {
			ReferenceImpl ref = (ReferenceImpl)result.get(0);
			return ref;
		}
		
		
	}

	/*------------------------- getter -------------------------*/	
	
	
	/* (non-Javadoc)
	 * @see org.olat.core.id.OLATResourceablegetId()
	 */
	public Long getResourceableId() {
		
		Long id = this.getKey();
		//if (id == null) throw new AssertException("getId() must not be called before an id has been generated by the persister");				
		return id;
	}

	/* (non-Javadoc)
	 * @see org.olat.core.id.OLATResourceablegetTypeName()
	 */
	public String getResourceableTypeName() {
		
		return ORES_TYPE_NAME;
	}
	
	/*
	 * @see de.xman.exam.Exam#getName()
	 */
	public String getName() {
		
		return this.name;
	}
	
	/*
	 * @see de.xman.exam.Exam#getComments()
	 */
	public String getComments() {
		
		return this.comments;
	}

	/*
	 * @see de.xman.exam.Exam#getEarmarkedEnabled()
	 */
	public boolean getEarmarkedEnabled() {
	
		return this.earmarkedEnabled;
	}

	/*
	 * @see de.xman.exam.Exam#getRegEndDate()
	 */
	public Date getRegEndDate() {
		
		return this.regEndDate;
	}

	/*
	 * @see de.xman.exam.Exam#getRegStartDate()
	 */
	public Date getRegStartDate() {
		
		return this.regStartDate;
	}

	/*
	 * @see de.xman.exam.Exam#getSignOffDate()
	 */
	public Date getSignOffDate() {
		
		return this.signOffDate;
	}
	
	/*
	 * (non-Javadoc)
	 * @see de.xman.exam.Exam#getIsOral()
	 */
	public boolean getIsOral() {
		
		return isOral;
	}
	
	public Identity getIdentity() {
		
		return this.identity;
	}
	
	/*------------------------- setter -------------------------*/
	
	
	/**
	 * @see de.xman.exam.Exam#setName(String)
	 */
	public void setName(String name) {
		
		this.name = name;
	}

	/**
	 * @see de.xman.exam.Exam#setComments(String)
	 */
	public void setComments(String comments) {
		
		this.comments = comments;
	}

	/**
	 * @see de.xman.exam.Exam#setEarmarkedEnabled(boolean)
	 */
	public void setEarmarkedEnabled(boolean enable) {
		
		this.earmarkedEnabled = enable;
	}

	/**
	 * @see de.xman.exam.Exam#setRegEndDate(Date)
	 */
	public void setRegEndDate(Date endDate) {
		
		this.regEndDate = endDate;
	}

	/**
	 * @see de.xman.exam.Exam#setRegStartDate(Date)
	 */
	public void setRegStartDate(Date startDate) {

		this.regStartDate = startDate;
	}

	/**
	 * @see de.xman.exam.Exam#setSignOffDate(Date)
	 */
	public void setSignOffDate(Date signOffDate) {
		
		this.signOffDate = signOffDate;
	}

	/**
	 * @see de.xman.exam.Exam#setOral(boolean)
	 */
	public void setIsOral(boolean oral) {
		
		this.isOral = oral;
	}
	
	public void setIdentity(Identity identity) {
		
		this.identity = identity;
	}
	
	public Date getLastModified() {
		
		return this.lastModified;
	}

	public void setLastModified(Date lastModified) {
		
		this.lastModified = lastModified;
	}
}
