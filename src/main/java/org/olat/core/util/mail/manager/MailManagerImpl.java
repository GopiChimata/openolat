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

package org.olat.core.util.mail.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.Adler32;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.commons.services.notifications.Publisher;
import org.olat.core.commons.services.notifications.PublisherData;
import org.olat.core.commons.services.notifications.Subscriber;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.helpers.Settings;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.Encoder;
import org.olat.core.util.StringHelper;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.filter.impl.NekoHTMLFilter;
import org.olat.core.util.mail.ContactList;
import org.olat.core.util.mail.MailAttachment;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailContent;
import org.olat.core.util.mail.MailContext;
import org.olat.core.util.mail.MailHelper;
import org.olat.core.util.mail.MailManager;
import org.olat.core.util.mail.MailModule;
import org.olat.core.util.mail.MailTemplate;
import org.olat.core.util.mail.MailerResult;
import org.olat.core.util.mail.MailerSMTPAuthenticator;
import org.olat.core.util.mail.model.DBMail;
import org.olat.core.util.mail.model.DBMailAttachment;
import org.olat.core.util.mail.model.DBMailImpl;
import org.olat.core.util.mail.model.DBMailLight;
import org.olat.core.util.mail.model.DBMailLightImpl;
import org.olat.core.util.mail.model.DBMailRecipient;
import org.olat.core.util.vfs.FileStorage;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;

/**
 * 
 * Description:<br>
 * Manager which send e-mails, make the triage between mails which are
 * really send by POP, or only saved in the intern mail system (a.k.a on
 * the database).
 * 
 * <P>
 * Initial Date:  24 mars 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MailManagerImpl extends BasicManager implements MailManager {

	public static final String MAIL_TEMPLATE_FOLDER = "/customizing/mail/";
	
	private VelocityEngine velocityEngine;
	
	private final MailModule mailModule;
	private DB dbInstance;
	private FileStorage attachmentStorage;
	private NotificationsManager notificationsManager;
	
	public MailManagerImpl(MailModule mailModule) {
		this.mailModule = mailModule;
	}
	
	/**
	 * [used by Spring]
	 * 
	 * @param dbInstance
	 */
	public void setDbInstance(DB dbInstance) {
		this.dbInstance = dbInstance;
	}
	
	/**
	 * [used by Spring]
	 * @param notificationsManager
	 */
	public void setNotificationsManager(NotificationsManager notificationsManager) {
		this.notificationsManager = notificationsManager;
	}
	
	/**
	 * [used by Spring]
	 */
	public void init() {
		VFSContainer root = mailModule.getRootForAttachments();
		attachmentStorage = new FileStorage(root);
		
		PublisherData pdata = getPublisherData();
		SubscriptionContext scontext = getSubscriptionContext();
		notificationsManager.getOrCreatePublisher(scontext, pdata);
		
		Properties p = null;
		try {
			velocityEngine = new VelocityEngine();
			p = new Properties();
			p.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
			p.setProperty("runtime.log.logsystem.log4j.category", "syslog");
			velocityEngine.init(p);
		} catch (Exception e) {
			throw new RuntimeException("config error " + p.toString());
		}
	}
	
	@Override
	public SubscriptionContext getSubscriptionContext() {
		return new SubscriptionContext("Inbox", 0l, "");
	}

	@Override
	public PublisherData getPublisherData() {
		String data = "";
		String businessPath = "[Inbox:0]";
		PublisherData publisherData = new PublisherData("Inbox", data, businessPath);
		return publisherData;
	}

	@Override
	public Subscriber getSubscriber(Identity identity) {
		SubscriptionContext context = getSubscriptionContext();
		if(context == null) return null;
		Publisher publisher = notificationsManager.getPublisher(context);
		if(publisher == null) {
			return null;
		}
		return notificationsManager.getSubscriber(identity, publisher);
	}

	@Override
	public void subscribe(Identity identity) {
		PublisherData data = getPublisherData();
		SubscriptionContext context = getSubscriptionContext();
		if(context != null) {
			notificationsManager.subscribe(identity, context, data);
		}
	}

	@Override
	public DBMail getMessageByKey(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select mail from ").append(DBMailImpl.class.getName()).append(" mail")
			.append(" left join fetch mail.recipients recipients")
			.append(" where mail.key=:mailKey");

		List<DBMail> mails = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBMail.class)
				.setParameter("mailKey", key)
				.getResultList();
		if(mails.isEmpty()) return null;
		return mails.get(0);
	}

	@Override
	public List<DBMailAttachment> getAttachments(DBMailLight mail) {
		StringBuilder sb = new StringBuilder();
		sb.append("select attachment from ").append(DBMailAttachment.class.getName()).append(" attachment")
			.append(" inner join attachment.mail mail")
			.append(" where mail.key=:mailKey");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBMailAttachment.class)
				.setParameter("mailKey", mail.getKey())
				.getResultList();
	}
	
	private DBMailAttachment getAttachment(Long key) {
		StringBuilder sb = new StringBuilder();
		sb.append("select attachment from ").append(DBMailAttachment.class.getName()).append(" attachment")
			.append(" where attachment.key=:attachmentKey");

		List<DBMailAttachment> attachments = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBMailAttachment.class)
				.setParameter("attachmentKey", key)
				.getResultList();

		if(attachments.isEmpty()) {
			return null;
		}
		return attachments.get(0);
	}

	@Override
	public String saveAttachmentToStorage(String name, String mimetype, long checksum, long size, InputStream stream) {
		String hasSibling = getAttachmentSibling(name, mimetype, checksum, size);
		if(StringHelper.containsNonWhitespace(hasSibling)) {
			return hasSibling;
		}
		
		String uuid = Encoder.md5hash(name + checksum);
		String dir = attachmentStorage.generateDir(uuid, false);
		VFSContainer container = attachmentStorage.getContainer(dir);
		String uniqueName = VFSManager.similarButNonExistingName(container, name);
		VFSLeaf file = container.createChildLeaf(uniqueName);
		VFSManager.copyContent(stream, file);
		return dir + uniqueName;
	}
	
	private String getAttachmentSibling(String name, String mimetype, long checksum, long size) {
		StringBuilder sb = new StringBuilder();
		sb.append("select attachment from ").append(DBMailAttachment.class.getName()).append(" attachment")
			.append(" where attachment.checksum=:checksum and attachment.size=:size and attachment.name=:name");
		if(mimetype == null) {
			sb.append(" and attachment.mimetype is null");
		} else {
			sb.append(" and attachment.mimetype=:mimetype");
		}
		
		TypedQuery<DBMailAttachment> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBMailAttachment.class)
				.setParameter("checksum", new Long(checksum))
				.setParameter("size", new Long(size))
				.setParameter("name", name);
		if(mimetype != null) {
			query.setParameter("mimetype", mimetype);
		}

		List<DBMailAttachment> attachments = query.getResultList();
		if(attachments.isEmpty()) {
			return null;
		}
		return attachments.get(0).getPath();
	}
	
	private int countAttachment(String path) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(attachment) from ").append(DBMailAttachment.class.getName()).append(" attachment")
			.append(" where attachment.path=:path");

		return dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), Number.class)
				.setParameter("path", path)
				.getSingleResult().intValue();
	}

	@Override
	public VFSLeaf getAttachmentDatas(Long key) {
		DBMailAttachment attachment = getAttachment(key);
		return getAttachmentDatas(attachment);
	}

	@Override
	public VFSLeaf getAttachmentDatas(MailAttachment attachment) {
		String path = attachment.getPath();
		if(StringHelper.containsNonWhitespace(path)) {
			VFSContainer root = mailModule.getRootForAttachments();
			VFSItem item = root.resolve(path);
			if(item instanceof VFSLeaf) {
				return (VFSLeaf)item;
			}
		}
		return null;
	}

	@Override
	public boolean hasNewMail(Identity identity) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(mail) from ").append(DBMailImpl.class.getName()).append(" mail")
			.append(" inner join mail.recipients recipient")
			.append(" inner join recipient.recipient recipientIdentity")
			.append(" where recipientIdentity.key=:recipientKey and recipient.read=false and recipient.deleted=false");

		Number count = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), Number.class)
				.setParameter("recipientKey", identity.getKey())
				.getSingleResult();
		return count.intValue() > 0;
	}
	
	/**
	 * 
	 * @param mail
	 * @param read cannot be null
	 * @param identity
	 * @return true if the read flag has been changed
	 */
	@Override
	public boolean setRead(DBMailLight mail, Boolean read, Identity identity) {
		if(mail == null || read == null || identity == null) throw new NullPointerException();
		
		boolean changed = false;
		for(DBMailRecipient recipient:mail.getRecipients()) {
			if(recipient == null) continue;
			if(recipient.getRecipient() != null && recipient.getRecipient().equalsByPersistableKey(identity)) {
				if(!read.equals(recipient.getRead())) {
					recipient.setRead(read);
					dbInstance.updateObject(recipient);
					changed |= true;
				}
			}
		}
		return changed;
	}

	@Override
	public DBMailLight toggleRead(DBMailLight mail, Identity identity) {
		Boolean read = null;
		for(DBMailRecipient recipient:mail.getRecipients()) {
			if(recipient == null) continue;
			if(recipient.getRecipient() != null && recipient.getRecipient().equalsByPersistableKey(identity)) {
				if(read == null) {
					read = recipient.getRead() == null ? Boolean.FALSE : recipient.getRead();
				}
				recipient.setRead(read.booleanValue() ? Boolean.FALSE : Boolean.TRUE);
				dbInstance.updateObject(recipient);
			}
		}
		return mail;
	}

	/**
	 * @param mail
	 * @param marked cannot be null
	 * @param identity
	 * @return true if the marked flag has been changed
	 */
	@Override
	public boolean setMarked(DBMailLight mail, Boolean marked, Identity identity) {
		if(mail == null || marked == null || identity == null) throw new NullPointerException();

		boolean changed = false;
		for(DBMailRecipient recipient:mail.getRecipients()) {
			if(recipient == null) continue;
			if(recipient != null && recipient.getRecipient() != null && recipient.getRecipient().equalsByPersistableKey(identity)) {
				if(marked == null) {
					marked = Boolean.FALSE;
				}
				if(!marked.equals(recipient.getMarked())) {
					recipient.setMarked(marked.booleanValue());
					dbInstance.updateObject(recipient);
					changed |= true;
				}
			}
		}
		return changed;
	}

	@Override
	public DBMailLight toggleMarked(DBMailLight mail, Identity identity) {
		Boolean marked = null;
		for(DBMailRecipient recipient:mail.getRecipients()) {
			if(recipient == null) continue;
			if(recipient != null && recipient.getRecipient() != null && recipient.getRecipient().equalsByPersistableKey(identity)) {
				if(marked == null) {
					marked = recipient.getMarked() == null ? Boolean.FALSE : recipient.getMarked();
				}
				recipient.setMarked(marked.booleanValue() ? Boolean.FALSE : Boolean.TRUE);
				dbInstance.updateObject(recipient);
			}
		}
		return mail;
	}
	
	/**
	 * Set the mail as deleted for a user
	 * @param mail
	 * @param identity
	 */
	@Override
	public void delete(DBMailLight mail, Identity identity, boolean deleteMetaMail) {
		if(mail == null) return;//already deleted
		if(StringHelper.containsNonWhitespace(mail.getMetaId()) && deleteMetaMail) {
			List<DBMailLight> mails = getEmailsByMetaId(mail.getMetaId());
			for(DBMailLight childMail:mails) {
				deleteMail(childMail, identity, false);
			}
		} else {
			deleteMail(mail, identity, false);
		}
	}

	private void deleteMail(DBMailLight mail, Identity identity, boolean forceRemoveRecipient) {
		boolean delete = true;
		List<DBMailRecipient> updates = new ArrayList<DBMailRecipient>();
		if(mail.getFrom() != null && mail.getFrom().getRecipient() != null) {
			if(identity.equalsByPersistableKey(mail.getFrom().getRecipient())) {
				DBMailRecipient from = mail.getFrom();
				from.setDeleted(Boolean.TRUE);
				if(forceRemoveRecipient) {
					from.setRecipient(null);
				}
				updates.add(from);
			}
			if(mail.getFrom().getDeleted() != null) {
				delete &= mail.getFrom().getDeleted().booleanValue();
			}
		}
		
		for(DBMailRecipient recipient:mail.getRecipients()) {
			if(recipient == null) continue;
			if(recipient.getRecipient() != null && recipient.getRecipient().equalsByPersistableKey(identity)) {
				recipient.setDeleted(Boolean.TRUE);
				if(forceRemoveRecipient) {
					recipient.setRecipient(null);
				}
				updates.add(recipient);
			}
			if(recipient.getDeleted() != null) {
				delete &= recipient.getDeleted().booleanValue();
			}
		}
		
		if(delete) {
			Set<String> paths = new HashSet<String>();
			
			//all marked as deleted -> delete the mail
			List<DBMailAttachment> attachments = getAttachments(mail);
			for(DBMailAttachment attachment: attachments) {
				mail = attachment.getMail();//reload from the hibernate session
				dbInstance.deleteObject(attachment);
				if(StringHelper.containsNonWhitespace(attachment.getPath())) {
					paths.add(attachment.getPath());
				}
			}
			dbInstance.deleteObject(mail);
			
			//try to remove orphans file
			for(String path:paths) {
				int count = countAttachment(path);
				if(count == 0) {
					VFSItem item = mailModule.getRootForAttachments().resolve(path);
					if(item instanceof VFSLeaf) {
						((VFSLeaf)item).delete();
					}
				}
			}
		} else {
			for(DBMailRecipient update:updates) {
				dbInstance.updateObject(update);
			}
		}
	}
	
	/**
	 * Load all mails with the identity as from, mail which are not deleted
	 * for this user. Recipients are loaded.
	 * @param from
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	@Override
	public List<DBMailLight> getOutbox(Identity from, int firstResult, int maxResults, boolean fetchRecipients) {
		StringBuilder sb = new StringBuilder();
		sb.append("select distinct(mail) from ").append(DBMailLightImpl.class.getName()).append(" mail")
			.append(" inner join fetch mail.from fromRecipient")
			.append(" inner join fromRecipient.recipient fromRecipientIdentity")
			.append(" inner join ").append(fetchRecipients ? "fetch" : "").append(" mail.recipients recipient")
			.append(" inner join ").append(fetchRecipients ? "fetch" : "").append(" recipient.recipient recipientIdentity")
			.append(" where fromRecipientIdentity.key=:fromKey and fromRecipient.deleted=false and recipientIdentity.key!=:fromKey")
			.append(" order by mail.creationDate desc");

		TypedQuery<DBMailLight> query = dbInstance.getCurrentEntityManager()
				.createQuery(sb.toString(), DBMailLight.class)
				.setParameter("fromKey", from.getKey());
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		if(firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		List<DBMailLight> mails = query.getResultList();
		return mails;
	}

	@Override
	public List<DBMailLight> getEmailsByMetaId(String metaId) {
		if(!StringHelper.containsNonWhitespace(metaId)) return Collections.emptyList();
		
		StringBuilder sb = new StringBuilder();
		sb.append("select mail from ").append(DBMailLightImpl.class.getName()).append(" mail")
			.append(" inner join fetch mail.from fromRecipient")
			.append(" inner join fromRecipient.recipient fromRecipientIdentity")
			.append(" where mail.metaId=:metaId");

		return dbInstance.getCurrentEntityManager().createQuery(sb.toString(), DBMailLight.class)
				.setParameter("metaId", metaId)
				.getResultList();
	}
	
	/**
	 * Load all mails with the identity as recipient, only mails which are not deleted
	 * for this user. Recipients are NOT loaded if not explicitly wanted!
	 * @param identity
	 * @param unreadOnly
	 * @param fetchRecipients
	 * @param from
	 * @param firstResult
	 * @param maxResults
	 * @return
	 */
	@Override
	public List<DBMailLight> getInbox(Identity identity, Boolean unreadOnly, Boolean fetchRecipients, Date from, int firstResult, int maxResults) {
		StringBuilder sb = new StringBuilder();
		String fetchOption = (fetchRecipients != null && fetchRecipients.booleanValue()) ? "fetch" : "";
		sb.append("select mail from ").append(DBMailLightImpl.class.getName()).append(" mail")
			.append(" inner join fetch ").append(" mail.from fromRecipient")
		  .append(" inner join ").append(fetchOption).append(" mail.recipients recipient")
			.append(" inner join ").append(fetchOption).append(" recipient.recipient recipientIdentity")
			.append(" where recipientIdentity.key=:recipientKey and recipient.deleted=false");
		if(unreadOnly != null && unreadOnly.booleanValue()) {
			sb.append(" and recipient.read=false");
		}
		if(from != null) {
			sb.append(" and mail.creationDate>=:from");
		}
		sb.append(" order by mail.creationDate desc");

		TypedQuery<DBMailLight> query = dbInstance.getCurrentEntityManager().createQuery(sb.toString(), DBMailLight.class)
				.setParameter("recipientKey", identity.getKey());
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		if(firstResult > 0) {
			query.setFirstResult(firstResult);
		}
		if(from != null) {
			query.setParameter("from", from, TemporalType.TIMESTAMP);
		}

		List<DBMailLight> mails = query.getResultList();
		return mails;
	}

	@Override
	public String getMailTemplate() {
		File baseFolder = new File(WebappHelper.getUserDataRoot(), MAIL_TEMPLATE_FOLDER);	
		File template = new File(baseFolder, "mail_template.html");
		if(template.exists()) {
			try(InputStream in = new FileInputStream(template)) {
				return IOUtils.toString(in);
			} catch (IOException e) {
				logError("", e);
			}
		}
		return getDefaultMailTemplate();
	}

	@Override
	public void setMailTemplate(String template) {
		File baseFolder = new File(WebappHelper.getUserDataRoot(), MAIL_TEMPLATE_FOLDER);
		if(!baseFolder.exists()) {
			baseFolder.mkdirs();
		}
		OutputStream out = null;
		try {
			File templateFile = new File(baseFolder, "mail_template.html");
			StringReader reader = new StringReader(template);
			out = new FileOutputStream(templateFile);
			IOUtils.copy(reader, out);
		} catch (IOException e) {
			logError("", e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	@Override
	public String getDefaultMailTemplate() {
		try(InputStream in = MailModule.class.getResourceAsStream("_content/mail_template.html")) {
			return IOUtils.toString(in);
		} catch (IOException e) {
			logError("Cannot read the default mail template", e);
			return null;
		}
	}

	@Override
	public MailBundle[] makeMailBundles(MailContext ctxt, List<Identity> recipientsTO,
			MailTemplate template, Identity sender, String metaId, MailerResult result) {
		List<MailBundle> bundles = new ArrayList<MailBundle>();
		if(recipientsTO != null) {
			for(Identity recipient: recipientsTO) {
				MailBundle bundle =  makeMailBundle(ctxt, recipient, template, sender, metaId, result);
				if(bundle != null) {
					bundles.add(bundle);
				}
			}
		}

		return bundles.toArray(new MailBundle[bundles.size()]);
	}

	@Override
	public MailBundle makeMailBundle(MailContext ctxt, Identity recipientTO,
			MailTemplate template, Identity sender, String metaId, MailerResult result) {	

		MailBundle bundle;
		if(MailHelper.isDisabledMailAddress(recipientTO, result)) {
			bundle = null;//email disabled, nothing to do
		} else {
			MailContent msg = createWithContext(recipientTO, template, result);
			if(msg != null && result.getReturnCode() == MailerResult.OK){
				// send mail
				bundle = new MailBundle();
				bundle.setContext(ctxt);
				bundle.setFromId(sender);
				bundle.setToId(recipientTO);
				bundle.setMetaId(metaId);
				bundle.setContent(msg);
			} else {
				bundle = null;
			}
		}
		return bundle;
	}

	@Override
	public MailerResult sendMessage(MailBundle... bundles) {
		MailerResult result = new MailerResult();
		for(MailBundle bundle:bundles) {
			MailContent content = decorateMail(bundle);
			if(mailModule.isInternSystem()) {
				saveDBMessage(bundle.getContext(), bundle.getFromId(), bundle.getFrom(),
						bundle.getToId(), bundle.getTo(), bundle.getCc(),
						bundle.getContactLists(), bundle.getMetaId(), content, result);
			} else {
				sendExternMessage(bundle.getFromId(), bundle.getFrom(),
						bundle.getToId(), bundle.getTo(), bundle.getCc(),
						bundle.getContactLists(), content, result);
			}
		}
		return result;
	}
	
	protected MailContent decorateMail(MailBundle bundle) {
		MailContent content = bundle.getContent();

		String template = getMailTemplate();
		boolean htmlTemplate = isHtmlEmail(template);
		String body = content.getBody();
		boolean htmlContent =  isHtmlEmail(body);
		if(htmlTemplate && !htmlContent) {
			body = body.replace("&", "&amp;");
			body = body.replace("<", "&lt;");
			body = body.replace("\n", "<br />");
		}
		VelocityContext context = new VelocityContext();
		context.put("content", body);
		context.put("footer", MailHelper.getMailFooter(bundle));
		context.put("server", Settings.getServerContextPathURI());

		StringWriter writer = new StringWriter(2000);
		MailerResult result = new MailerResult();
		evaluate(context, template, writer, result);
		
		String decoratedBody;
		if(result.isSuccessful()) {
			decoratedBody = writer.toString();
		} else {
			decoratedBody = content.getBody();
		}
		return new MessageContent(content.getSubject(), decoratedBody, content.getAttachments());
	}
	
	protected MailContent createWithContext(Identity recipient, MailTemplate template, MailerResult result) {
		VelocityContext context;
		if(template != null && template.getContext() != null) {
			context = new VelocityContext(template.getContext());
		} else {
			context = new VelocityContext();
		}
		template.putVariablesInMailContext(context, recipient);

		// merge subject template with context variables
		StringWriter subjectWriter = new StringWriter();
		evaluate(context, template.getSubjectTemplate(), subjectWriter, result);
		// merge body template with context variables
		StringWriter bodyWriter = new StringWriter();
		evaluate(context, template.getBodyTemplate(), bodyWriter, result);
		// check for errors - exit
		if (result.getReturnCode() != MailerResult.OK) {
			return null;
		}
		
		String subject = subjectWriter.toString();
		String body = bodyWriter.toString();
		List<File> checkedFiles = MailHelper.checkAttachments(template.getAttachments(), result);
		File[] attachments = checkedFiles.toArray(new File[checkedFiles.size()]);
		return new MessageContent(subject, body, attachments);
	}
	
	/**
	 * Internal Helper: merges a velocity context with a template.
	 * 
	 * @param context
	 * @param template
	 * @param writer writer that contains merged result
	 * @param mailerResult
	 */
	protected void evaluate(Context context, String template, StringWriter writer, MailerResult mailerResult) {
		try {
			boolean result = velocityEngine.evaluate(context, writer, "mailTemplate", template);
			if (result) {
				mailerResult.setReturnCode(MailerResult.OK);
			} else {
				logWarn("can't send email from user template with no reason", null);
				mailerResult.setReturnCode(MailerResult.TEMPLATE_GENERAL_ERROR);
			}
		} catch (ParseErrorException e) {
			logWarn("can't send email from user template", e);
			mailerResult.setReturnCode(MailerResult.TEMPLATE_PARSE_ERROR);
		} catch (MethodInvocationException e) {
			logWarn("can't send email from user template", e);
			mailerResult.setReturnCode(MailerResult.TEMPLATE_GENERAL_ERROR);
		} catch (ResourceNotFoundException e) {
			logWarn("can't send email from user template", e);
			mailerResult.setReturnCode(MailerResult.TEMPLATE_GENERAL_ERROR);
		} catch (Exception e) {
			logWarn("can't send email from user template", e);
			mailerResult.setReturnCode(MailerResult.TEMPLATE_GENERAL_ERROR);
		}
	}
	
	private static class MessageContent implements MailContent {
		private final String subject;
		private final String body;
		private final List<File> attachments;
		
		public MessageContent(String subject, String body, File[] attachmentArr) {
			this.subject = subject;
			this.body = body;
			
			attachments = new ArrayList<File>();
			if(attachmentArr != null && attachmentArr.length > 0) {
				for(File attachment:attachmentArr) {
					if(attachment != null && attachment.exists()) {
						attachments.add(attachment);
					}
				}
			}
		}
		
		public MessageContent(String subject, String body, List<File> attachmentList) {
			this.subject = subject;
			this.body = body;
			if(attachmentList == null) {
				this.attachments = new ArrayList<File>(1);
			} else {
				this.attachments = new ArrayList<File>(attachmentList);
			}
		}

		@Override
		public String getSubject() {
			return subject;
		}

		@Override
		public String getBody() {
			return body;
		}

		@Override
		public List<File> getAttachments() {
			return attachments;
		}
	}

	@Override
	public MailerResult forwardToRealInbox(Identity identity, DBMail mail, MailerResult result) {
		
		if(result == null) {
			result = new MailerResult();
		}
		
		List<DBMailAttachment> attachments = getAttachments(mail);

		try {
			Address from = createAddress(WebappHelper.getMailConfig("mailFrom"));
			Address to = createAddress(identity, result, true);
			MimeMessage message = createForwardMimeMessage(from, to, mail.getSubject(), mail.getBody(), attachments, result);
			if(message != null) {
				sendMessage(message, result);
			}
		} catch (AddressException e) {
			logError("mailFrom is not configured", e);
		}
		return result;
	}
	
	@Override
	public MailerResult sendExternMessage(MailBundle bundle, MailerResult result) {
		return sendExternMessage(bundle.getFromId(), bundle.getFrom(), bundle.getToId(), bundle.getTo(), bundle.getCc(), bundle.getContactLists(),
				bundle.getContent(), result);
	}
	
	
	/**
	 * Send the message via e-mail, always.
	 * @param from
	 * @param to
	 * @param cc
	 * @param contactLists
	 * @param listAsBcc
	 * @param subject
	 * @param body
	 * @param attachments
	 * @return
	 */
	private MailerResult sendExternMessage(Identity fromId, String from, Identity toId, String to, Identity cc, List<ContactList> bccLists,
			MailContent content, MailerResult result) {

		if(result == null) {
			result = new MailerResult();
		}
		MimeMessage mail = createMimeMessage(fromId, from, toId, to, cc, bccLists, content, result);
		if(mail != null) {
			sendMessage(mail, result);
		}
		return result;
	}
	
	private boolean wantRealMailToo(Identity id) {
		if(id == null) return false;
		String want = id.getUser().getPreferences().getReceiveRealMail();
		if(want != null) {
			return "true".equals(want);
		}
		return mailModule.isReceiveRealMailUserDefaultSetting();
	}
	
	protected DBMail saveDBMessage(MailContext context, Identity fromId, String from, Identity toId, String to, 
			Identity cc,  List<ContactList> bccLists, String metaId, MailContent content, MailerResult result) {
		
		try {
			DBMailImpl mail = new DBMailImpl();
			if(result == null) {
				result = new MailerResult();
			}
			
			boolean makeRealMail = makeRealMail(toId, cc, bccLists);
			Address fromAddress = null;
			List<Address> toAddress = new ArrayList<Address>();
			List<Address> ccAddress = new ArrayList<Address>();
			List<Address> bccAddress = new ArrayList<Address>();
			
			if(fromId != null) {
				DBMailRecipient fromRecipient = new DBMailRecipient();
				fromRecipient.setRecipient(fromId);
				if(StringHelper.containsNonWhitespace(from)) {
					fromRecipient.setEmailAddress(from);
					fromAddress = createFromAddress(from, result);
				} else {
					fromAddress = createFromAddress(fromId, result);
				}
				fromRecipient.setVisible(Boolean.TRUE);
				fromRecipient.setMarked(Boolean.FALSE);
				fromRecipient.setDeleted(Boolean.FALSE);
				mail.setFrom(fromRecipient);
			} else {
				if(!StringHelper.containsNonWhitespace(from)) {
					from = WebappHelper.getMailConfig("mailFrom");
				}
				DBMailRecipient fromRecipient = new DBMailRecipient();
				fromRecipient.setEmailAddress(from);
				fromRecipient.setVisible(Boolean.TRUE);
				fromRecipient.setMarked(Boolean.FALSE);
				fromRecipient.setDeleted(Boolean.TRUE);//marked as delted as nobody can read it
				mail.setFrom(fromRecipient);
				fromAddress = createFromAddress(from, result);
			}
			
			if(result.getReturnCode() != MailerResult.OK) {
				return null;
			}
			
			mail.setMetaId(metaId);
			String subject = content.getSubject();
			if(subject != null && subject.length() > 500) {
				logWarn("Cut a too long subkect in name. Size: " + subject.length(), null);
				subject = subject.substring(0, 500);
			}
			mail.setSubject(subject);
			String body = content.getBody();
			if(body != null && body.length() > 16777210) {
				logWarn("Cut a too long body in mail. Size: " + body.length(), null);
				body = body.substring(0, 16000000);
			}
			mail.setBody(body);
			mail.setLastModified(new Date());
			
			if(context != null) {
				OLATResourceable ores = context.getOLATResourceable();
				if(ores != null) {
					String resName = ores.getResourceableTypeName();
					if(resName != null && resName.length() > 50) {
						logWarn("Cut a too long resourceable type name in mail context: " + resName, null);
						resName = resName.substring(0, 49);
					}
					mail.getContext().setResName(ores.getResourceableTypeName());
					mail.getContext().setResId(ores.getResourceableId());
				}
				
				String resSubPath = context.getResSubPath();
				if(resSubPath != null && resSubPath.length() > 2000) {
					logWarn("Cut a too long resSubPath in mail context: " + resSubPath, null);
					resSubPath = resSubPath.substring(0, 2000);
				}
				mail.getContext().setResSubPath(resSubPath);
				
				String businessPath = context.getBusinessPath();
				if(businessPath != null && businessPath.length() > 2000) {
					logWarn("Cut a too long resSubPath in mail context: " + businessPath, null);
					businessPath = businessPath.substring(0, 2000);
				}
				mail.getContext().setBusinessPath(businessPath);
			}
			
			//add to
			DBMailRecipient recipientTo = null;
			if(toId != null) {
				recipientTo = new DBMailRecipient();
				if(toId instanceof PersistentObject) {
					recipientTo.setRecipient(toId);
				} else {
					to = toId.getUser().getProperty(UserConstants.EMAIL, null);
				}
				if(StringHelper.containsNonWhitespace(to)) {
					recipientTo.setEmailAddress(to);
				}
				recipientTo.setVisible(Boolean.TRUE);
				recipientTo.setDeleted(Boolean.FALSE);
				recipientTo.setMarked(Boolean.FALSE);
				recipientTo.setRead(Boolean.FALSE);
			} else if (StringHelper.containsNonWhitespace(to)) {
				recipientTo = new DBMailRecipient();
				recipientTo.setEmailAddress(to);
				recipientTo.setVisible(Boolean.TRUE);
				recipientTo.setDeleted(Boolean.TRUE);
				recipientTo.setMarked(Boolean.FALSE);
				recipientTo.setRead(Boolean.FALSE);
			}
			
			if(recipientTo != null) {
				mail.getRecipients().add(recipientTo);
				createAddress(toAddress, recipientTo, true, result, true);
			} 
			if(makeRealMail && StringHelper.containsNonWhitespace(to)) {
				createAddress(toAddress, to);
			}
			
			if(cc != null) {
				DBMailRecipient recipient = new DBMailRecipient();
				if(cc instanceof PersistentObject) {
					recipient.setRecipient(cc);
				} else {
					recipient.setEmailAddress(cc.getUser().getProperty(UserConstants.EMAIL, null));
				}
				recipient.setVisible(Boolean.TRUE);
				recipient.setDeleted(Boolean.FALSE);
				recipient.setMarked(Boolean.FALSE);
				recipient.setRead(Boolean.FALSE);
				mail.getRecipients().add(recipient);
				createAddress(ccAddress, recipient, false, result, true);
			}

			//add bcc recipients
			appendRecipients(mail, bccLists, toAddress, bccAddress, false, makeRealMail, result);
			
			dbInstance.getCurrentEntityManager().persist(mail);
			
			//save attachments
			List<File> attachments = content.getAttachments();
			if(attachments != null && !attachments.isEmpty()) {
				for(File attachment:attachments) {

					FileInputStream in = null;
					try {
						DBMailAttachment data = new DBMailAttachment();
						data.setSize(attachment.length());
						data.setName(attachment.getName());
						
						long checksum = FileUtils.checksum(attachment, new Adler32()).getValue();
						data.setChecksum(new Long(checksum));
						data.setMimetype(WebappHelper.getMimeType(attachment.getName()));
						
						in = new FileInputStream(attachment);
						String path = saveAttachmentToStorage(data.getName(), data.getMimetype(), checksum, attachment.length(), in);
						data.setPath(path);
						data.setMail(mail);

						dbInstance.getCurrentEntityManager().persist(data);
					} catch (FileNotFoundException e) {
						logError("File attachment not found: " + attachment, e);
					} catch (IOException e) {
						logError("Error with file attachment: " + attachment, e);
					} finally {
						IOUtils.closeQuietly(in);
					}
				}
			}
			
			if(makeRealMail) {
				//check that we send an email to someone
				if(!toAddress.isEmpty() || !ccAddress.isEmpty() || !bccAddress.isEmpty()) {
					sendRealMessage(fromAddress, toAddress, ccAddress, bccAddress, subject, body, attachments, result);
				}
			}

			//update subscription
			for(DBMailRecipient recipient:mail.getRecipients()) {
				if(recipient.getRecipient() != null) {
					subscribe(recipient.getRecipient());
				}
			}

			SubscriptionContext subContext = getSubscriptionContext();
			notificationsManager.markPublisherNews(subContext, null, false);
			return mail;
		} catch (AddressException e) {
			logError("Cannot send e-mail: ", e);
			result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
			return null;
		}
	}
	
	private void appendRecipients(DBMailImpl mail, List<ContactList> ccLists, List<Address> toAddress, List<Address> ccAddress,
			boolean visible, boolean makeRealMail, MailerResult result) throws AddressException {
		
		//append cc/bcc recipients
		if(ccLists != null && !ccLists.isEmpty()) {
			for(ContactList contactList:ccLists) {
				if(makeRealMail && StringHelper.containsNonWhitespace(contactList.getName())) {
					Address[] groupAddress = InternetAddress.parse(contactList.getRFC2822Name() + ";");
					if(groupAddress != null && groupAddress.length > 0) {
						for(Address groupAdd:groupAddress) {
							toAddress.add(groupAdd);
						}
					}
				}

				for(String email:contactList.getStringEmails().values()) {
					DBMailRecipient recipient = new DBMailRecipient();
					recipient.setEmailAddress(email);
					recipient.setGroup(contactList.getName());
					recipient.setVisible(visible);
					recipient.setDeleted(Boolean.FALSE);
					recipient.setMarked(Boolean.FALSE);
					recipient.setRead(Boolean.FALSE);
					mail.getRecipients().add(recipient);
					
					if(makeRealMail) {
						createAddress(ccAddress, recipient, false, result, false);
					}
				}
				
				for(Identity identityEmail:contactList.getIdentiEmails().values()) {
					DBMailRecipient recipient = new DBMailRecipient();
					if(identityEmail instanceof PersistentObject) {
						recipient.setRecipient(identityEmail);
					} else {
						recipient.setEmailAddress(identityEmail.getUser().getProperty(UserConstants.EMAIL, null));
					}
					recipient.setGroup(contactList.getName());
					recipient.setVisible(visible);
					recipient.setDeleted(Boolean.FALSE);
					recipient.setMarked(Boolean.FALSE);
					recipient.setRead(Boolean.FALSE);
					mail.getRecipients().add(recipient);
					
					if(makeRealMail) {
						createAddress(ccAddress, recipient, false, result, false);
					}
				}
			}
		}
	}
	
	private boolean makeRealMail(Identity toId, Identity cc, List<ContactList> bccLists) {
		//need real mail to???
		boolean makeRealMail = false;
		// can occur on self-registration
		if (toId == null && cc == null && bccLists == null) return true;
		
		if(toId != null) {
			makeRealMail |= wantRealMailToo(toId);
		}
		
		if(cc != null) {
			makeRealMail |= wantRealMailToo(cc);
		}
		
		//add bcc recipients
		if(bccLists != null && !bccLists.isEmpty()) {
			for(ContactList contactList:bccLists) {
				for(Identity identityEmail:contactList.getIdentiEmails().values()) {
					makeRealMail |= wantRealMailToo(identityEmail);
				}
				
				if(!contactList.getStringEmails().isEmpty()) {
					makeRealMail |= true;
				}
			}
		}
		
		return makeRealMail;
	}
	
	private MimeMessage createMimeMessage(Identity fromId, String mailFrom, Identity toId, String to, Identity ccId,
			List<ContactList> bccLists, MailContent content, MailerResult result) {
		try {
			Address from;
			if(StringHelper.containsNonWhitespace(mailFrom)) {
				from = createFromAddress(mailFrom, result);
			} else if (fromId != null) {
				from = createFromAddress(fromId, result);
			} else {
				// fxdiff: change from/replyto, see FXOLAT-74 . if no from is set, use default sysadmin-address (adminemail).
				from = createAddress(WebappHelper.getMailConfig("mailReplyTo"));
				if(from == null) {
					logError("MailConfigError: mailReplyTo is not set", null);
				}
			}

			List<Address> toList = new ArrayList<Address>();
			if(StringHelper.containsNonWhitespace(to)) {
				Address[] toAddresses = InternetAddress.parse(to);
				for(Address toAddress:toAddresses) {
					toList.add(toAddress);
				}
			} else if (toId != null) {
				Address toAddress = createAddress(toId, result, true);
				if(toAddress != null) {
					toList.add(toAddress);
				} 
			}
			
			List<Address> ccList = new ArrayList<Address>();
			if(ccId != null) {
				Address ccAddress = createAddress(ccId, result, true);
				if(ccAddress != null) {
					ccList.add(ccAddress);
				}
			}
			
			//add bcc contact lists
			List<Address> bccList = new ArrayList<Address>();
			if(bccLists != null) {
				for (ContactList contactList : bccLists) {
					if(StringHelper.containsNonWhitespace(contactList.getName())) {
						Address[] groupNames = InternetAddress.parse(contactList.getRFC2822Name() + ";");
						for(Address groupName:groupNames) {
							toList.add(groupName);
						}
					}
					
					Address[] members = contactList.getEmailsAsAddresses();
					for(Address member:members) {
						bccList.add(member);
					}
				}
			}
			
			Address[] tos = toList.toArray(new Address[toList.size()]);
			Address[] ccs = ccList.toArray(new Address[ccList.size()]);
			Address[] bccs = bccList.toArray(new Address[bccList.size()]);
			return createMimeMessage(from, tos, ccs, bccs, content.getSubject(), content.getBody(), content.getAttachments(), result);
		} catch (MessagingException e) {
			logError("", e);
			return null;
		}
	}
	
	private Address createAddress(String address) throws AddressException {
		if(address == null) return null;
		return new InternetAddress(address);
	}
	
	private Address createAddressWithName(String address, String name) throws UnsupportedEncodingException, AddressException {
		InternetAddress add = new InternetAddress(address, name);
		try {
			add.validate();
		} catch (AddressException e) {
			throw e;
		}
		return add;
	}
	
	private Address createFromAddress(String address, MailerResult result) throws AddressException {
		try {
			Address add = new InternetAddress(address);
			return add;
		} catch (AddressException e) {
			result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);
			throw e;
		}
	}
	
	private boolean createAddress(List<Address> addressList, String address) throws AddressException {
		Address add = createAddress(address);
		if(add != null) {
			addressList.add(add);
		}
		return true;
	}
	
	private boolean createAddress(List<Address> addressList, DBMailRecipient recipient, boolean force, MailerResult result, boolean error) {
		String emailAddress = recipient.getEmailAddress();
		if(recipient.getRecipient() == null) {
			try {
				Address address = createAddress(emailAddress);
				if(address != null) {
					addressList.add(address);
					return true;
				} else {
					if(error) {
						result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
					}
				}
			} catch (AddressException e) {
				if(error) {
					result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
				}
			}
		} else if(recipient.getRecipient().getStatus() == Identity.STATUS_LOGIN_DENIED) {
			result.addFailedIdentites(recipient.getRecipient());
		} else {
			if(force || wantRealMailToo(recipient.getRecipient())) {
				if(!StringHelper.containsNonWhitespace(emailAddress)) {
					emailAddress = recipient.getRecipient().getUser().getProperty(UserConstants.EMAIL, null);
				}
				try {
					Address address = createAddress(emailAddress);
					if(address != null) {
						addressList.add(address);
						return true;
					} else {
						result.addFailedIdentites(recipient.getRecipient());
						if(error) {
							result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
						}
					}
				} catch (AddressException e) {
					result.addFailedIdentites(recipient.getRecipient());
					if(error) {
						result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
					}
				}
			}
		}
		return false;
	}
	
	private Address createAddress(Identity recipient, MailerResult result, boolean error) {
		if(recipient != null) {
			if(recipient.getStatus() == Identity.STATUS_LOGIN_DENIED) {
				result.addFailedIdentites(recipient);
			} else {
				String emailAddress = recipient.getUser().getProperty(UserConstants.EMAIL, null);
				Address address;
				try {
					address = createAddress(emailAddress);
					if(address == null) {
						result.addFailedIdentites(recipient);
						if(error) {
							result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
						}
					}
					return address;
				} catch (AddressException e) {
					result.addFailedIdentites(recipient);
					if(error) {
						result.setReturnCode(MailerResult.RECIPIENT_ADDRESS_ERROR);
					}
				}
			}
		}
		return null;
	}
	
	private Address createFromAddress(Identity recipient, MailerResult result) {
		if(recipient != null) {
			String emailAddress = recipient.getUser().getProperty(UserConstants.EMAIL, null);
			String name = recipient.getUser().getProperty(UserConstants.FIRSTNAME, null) + " " + recipient.getUser().getProperty(UserConstants.LASTNAME, null); 
			Address address;
			try {
				address = createAddressWithName(emailAddress, name);
				if(address == null) {
					result.addFailedIdentites(recipient);
					result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);
				}
				return address;
			} catch (AddressException e) {
				result.addFailedIdentites(recipient);
				result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);			
			} catch (UnsupportedEncodingException e) {
				result.addFailedIdentites(recipient);
				result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);	
			}
		}
		return null;
	}
	
	private void sendRealMessage(Address from, List<Address> toList, List<Address> ccList, List<Address> bccList, String subject, String body,
			List<File> attachments, MailerResult result) {
		
		Address[] tos = null;
		if(toList != null && !toList.isEmpty()) {
			tos = new Address[toList.size()];
			tos = toList.toArray(tos);
		}
		
		Address[] ccs = null;
		if(ccList != null && !ccList.isEmpty()) {
			ccs = new Address[ccList.size()];
			ccs = ccList.toArray(ccs);
		}
		
		Address[] bccs = null;
		if(bccList != null && !bccList.isEmpty()) {
			bccs = new Address[bccList.size()];
			bccs = bccList.toArray(bccs);
		}

		MimeMessage msg = createMimeMessage(from, tos, ccs, bccs, subject, body, attachments, result);
		sendMessage(msg, result);
	}
	
	private MimeMessage createForwardMimeMessage(Address from, Address to, String subject, String body,
			List<DBMailAttachment> attachments, MailerResult result) {
		
		try {
			Address convertedFrom = getRawEmailFromAddress(from);
			MimeMessage msg = createMessage(convertedFrom);
			msg.setFrom(from);
			msg.setSubject(subject, "utf-8");

			if(to != null) {
				msg.addRecipient(RecipientType.TO, to);
			}

			if (attachments != null && !attachments.isEmpty()) {
				// with attachment use multipart message
				Multipart multipart = new MimeMultipart();
				// 1) add body part
				BodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setText(body);
				multipart.addBodyPart(messageBodyPart);
				// 2) add attachments
				for (DBMailAttachment attachment : attachments) {
					// abort if attachment does not exist
					if (attachment == null || attachment.getSize()  <= 0) {
						result.setReturnCode(MailerResult.ATTACHMENT_INVALID);
						logError("Tried to send mail wit attachment that does not exist::"
								+ (attachment == null ? null : attachment.getName()), null);
						return msg;
					}
					messageBodyPart = new MimeBodyPart();

					VFSLeaf data = getAttachmentDatas(attachment);
					DataSource source = new VFSDataSource(attachment.getName(), attachment.getMimetype(), data);
					messageBodyPart.setDataHandler(new DataHandler(source));
					messageBodyPart.setFileName(attachment.getName());
					multipart.addBodyPart(messageBodyPart);
				}
				// Put parts in message
				msg.setContent(multipart);
			} else {
				// without attachment everything is easy, just set as text
				msg.setText(body, "utf-8");
			}
			msg.setSentDate(new Date());
			msg.saveChanges();
			return msg;
		} catch (MessagingException e) {
			logError("", e);
			return null;
		}
	}
	
	/**
	 * 
	 * @param bounceAdress must be a raw email, without anything else (no "bla bli <bla@bli.ch>" !)
	 * @return
	 */
	private MimeMessage createMessage(Address bounceAdress) {
		String mailhost = WebappHelper.getMailConfig("mailhost");
		String mailhostTimeout = WebappHelper.getMailConfig("mailTimeout");
		boolean sslEnabled = Boolean.parseBoolean(WebappHelper.getMailConfig("sslEnabled"));
		boolean sslCheckCertificate = Boolean.parseBoolean(WebappHelper.getMailConfig("sslCheckCertificate"));
		
		Authenticator smtpAuth;
		if (WebappHelper.isMailHostAuthenticationEnabled()) {
			String smtpUser = WebappHelper.getMailConfig("smtpUser");
			String smtpPwd = WebappHelper.getMailConfig("smtpPwd");
			smtpAuth = new MailerSMTPAuthenticator(smtpUser, smtpPwd);
		} else {
			smtpAuth = null;
		}
		
		Properties p = new Properties();
		p.put("mail.smtp.from", bounceAdress.toString());
		p.put("mail.smtp.host", mailhost);
		p.put("mail.smtp.timeout", mailhostTimeout);
		p.put("mail.smtp.connectiontimeout", mailhostTimeout);
		p.put("mail.smtp.ssl.enable", sslEnabled);
		p.put("mail.smtp.ssl.checkserveridentity", sslCheckCertificate);
		Session mailSession;
		if (smtpAuth == null) {
			mailSession = javax.mail.Session.getInstance(p);
		} else {
			// use smtp authentication from configuration
			p.put("mail.smtp.auth", "true");
			mailSession = Session.getDefaultInstance(p, smtpAuth); 
		}
		if (isLogDebugEnabled()) {
			// enable mail session debugging on console
			mailSession.setDebug(true);
		}
		return new MimeMessage(mailSession);
	}
	
	// converts an address "bla bli <bla@bli.ch>" => "bla@bli.ch"
	private InternetAddress getRawEmailFromAddress(Address address) throws AddressException {
		if(address == null) {
			throw new AddressException("Address cannot be null");
		}
		InternetAddress fromAddress = new InternetAddress(address.toString());
		String fromPlainAddress = fromAddress.getAddress();
		return new InternetAddress(fromPlainAddress);
	}

	@Override
	public MimeMessage createMimeMessage(Address from, Address[] tos, Address[] ccs, Address[] bccs, String subject, String body,
			List<File> attachments, MailerResult result) {
		
		try {
			//	see FXOLAT-74: send all mails as <fromemail> (in config) to have a valid reverse lookup and therefore pass spam protection.
			// following doesn't work correctly, therefore add bounce-address in message already
			Address convertedFrom = getRawEmailFromAddress(from); 
			MimeMessage msg = createMessage(convertedFrom);
			Address viewableFrom = createAddressWithName(WebappHelper.getMailConfig("mailFrom"), WebappHelper.getMailConfig("mailFromName"));
			msg.setFrom(viewableFrom);
			msg.setSubject(subject, "utf-8");
			// reply to can only be an address without name (at least for postfix!), see FXOLAT-312
			msg.setReplyTo(new Address[] { convertedFrom });

			if(tos != null && tos.length > 0) {
				msg.addRecipients(RecipientType.TO, tos);
			}
			
			if(ccs != null && ccs.length > 0) {
				msg.addRecipients(RecipientType.CC, ccs);
			}
			
			if(bccs != null && bccs.length > 0) {
				msg.addRecipients(RecipientType.BCC, bccs);
			}

			if (attachments != null && !attachments.isEmpty()) {
				// with attachment use multipart message
				Multipart multipart = new MimeMultipart("mixed");
				// 1) add body part
				if(isHtmlEmail(body)) {
					Multipart alternativePart = createMultipartAlternative(body);
					MimeBodyPart wrap = new MimeBodyPart();
					wrap.setContent(alternativePart);
					multipart.addBodyPart(wrap);
				} else {
					BodyPart messageBodyPart = new MimeBodyPart();
					messageBodyPart.setText(body);
					multipart.addBodyPart(messageBodyPart);
				}
				
				// 2) add attachments
				for (File attachmentFile : attachments) {
					// abort if attachment does not exist
					if (attachmentFile == null || !attachmentFile.exists()) {
						result.setReturnCode(MailerResult.ATTACHMENT_INVALID);
						logError("Tried to send mail wit attachment that does not exist::"
								+ (attachmentFile == null ? null : attachmentFile.getAbsolutePath()), null);
						return msg;
					}
					BodyPart filePart = new MimeBodyPart();
					DataSource source = new FileDataSource(attachmentFile);
					filePart.setDataHandler(new DataHandler(source));
					filePart.setFileName(attachmentFile.getName());
					multipart.addBodyPart(filePart);
				}
				// Put parts in message
				msg.setContent(multipart);
			} else {
				// without attachment everything is easy, just set as text
				if(isHtmlEmail(body)) {
					msg.setContent(createMultipartAlternative(body));
				} else {
					msg.setText(body, "utf-8");
				}
			}
			msg.setSentDate(new Date());
			msg.saveChanges();
			return msg;
		} catch (AddressException e) {
			result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);
			logError("", e);
			return null;
		} catch (MessagingException e) {
			result.setReturnCode(MailerResult.SEND_GENERAL_ERROR);
			logError("", e);
			return null;
		} catch (UnsupportedEncodingException e) {
			result.setReturnCode(MailerResult.SENDER_ADDRESS_ERROR);
			logError("", e);
			return null;
		}
	}
	
	private Multipart createMultipartAlternative(String text)
	throws MessagingException {
		String pureText = new NekoHTMLFilter().filter(text, true);
		MimeBodyPart textPart = new MimeBodyPart();
		textPart.setText(pureText, "utf-8");
		
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setText(text, "utf-8", "html");

		Multipart multipart = new MimeMultipart("alternative");
		multipart.addBodyPart(textPart);
		multipart.addBodyPart(htmlPart);
		return multipart;
	}
	
	@Override
	public boolean isHtmlEmail(String text) {
		if(text.contains("<html") || text.contains("<body") || text.contains("<p") || text.contains("<span")) {
			return true;
		}
		if(text.contains("<HTML") || text.contains("<BODY") || text.contains("<P") || text.contains("<SPAN")) {
			return true;
		}
		return false;
	}

	@Override
	public void sendMessage(MimeMessage msg, MailerResult result){
		try{
			if(Settings.isJUnitTest()) {
				//we want not send really e-mails
			} else if (mailModule.isMailHostEnabled() && result.getReturnCode() == MailerResult.OK) {
				// now send the mail
				if(Settings.isDebuging()) {
					try {
						logInfo("E-mail send: " + msg.getSubject());
						logInfo("Content    : " + msg.getContent());
					} catch (IOException e) {
						logError("", e);
					}
				}
				Transport.send(msg);
			} else if(Settings.isDebuging() && result.getReturnCode() == MailerResult.OK) {
				try {
					logInfo("E-mail send: " + msg.getSubject());
					logInfo("Content    : " + msg.getContent());
				} catch (IOException e) {
					logError("", e);
				}
			} else {
				result.setReturnCode(MailerResult.MAILHOST_UNDEFINED);
			}
		} catch (MessagingException e) {
			result.setReturnCode(MailerResult.SEND_GENERAL_ERROR);
			logWarn("Could not send mail", e);
		}
	}
	
	private static class VFSDataSource implements DataSource {
		
		private final String name;
		private final String contentType;
		private final VFSLeaf file;
		
		public VFSDataSource(String name, String contentType, VFSLeaf file) {
			this.name = name;
			this.contentType = contentType;
			this.file = file;
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return file.getInputStream();
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return null;
		}
	}
}
