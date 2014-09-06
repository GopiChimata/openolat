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

package org.olat.modules.fo.archiver.formatters;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.id.Identity;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.vfs.VFSItem;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.MessageNode;

/**
 * Initial Date: Dec 19, 2013 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 * @author Patrick Brunner, Alexander Schneider
 */
public class ForumStreamedRTFFormatter extends ForumFormatter {
	
	private static final OLog log = Tracing.createLoggerFor(ForumStreamedRTFFormatter.class);

	private ZipOutputStream exportStream;
	private ForumManager fm = ForumManager.getInstance();
	
	final Pattern PATTERN_HTML_BOLD = Pattern.compile("<strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE);
	final Pattern PATTERN_HTML_ITALIC = Pattern.compile("<em>(.*?)</em>", Pattern.CASE_INSENSITIVE);
	final Pattern PATTERN_HTML_BREAK = Pattern.compile("<br />", Pattern.CASE_INSENSITIVE);
	final Pattern PATTERN_HTML_PARAGRAPH = Pattern.compile("<p>(.*?)</p>", Pattern.CASE_INSENSITIVE);
	final Pattern PATTERN_HTML_AHREF = Pattern.compile("<a href=\"([^\"]+)\"[^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE);
	final Pattern PATTERN_HTML_LIST = Pattern.compile("<li>(.*?)</li>", Pattern.CASE_INSENSITIVE);
	final Pattern HTML_SPACE_PATTERN = Pattern.compile("&nbsp;");
	
	final Pattern PATTERN_CSS_O_FOQUOTE = Pattern.compile("<div class=\"o_quote_wrapper\">\\s*<div class=\"b_quote_author mceNonEditable\">(.*?)</div>\\s*<blockquote class=\"b_quote\">\\s*(.*?)\\s*</blockquote>\\s*</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	final Pattern PATTERN_THREEPOINTS = Pattern.compile("&#8230;", Pattern.CASE_INSENSITIVE);
	final String THREEPOINTS = "...";
	
	//TODO: (LD) translate this!
	private String HIDDEN_STR = "VERBORGEN";
	private final String path;
		
	/**
	 * 
	 * @param container
	 * @param filePerThread
	 */

	public ForumStreamedRTFFormatter(ZipOutputStream exportStream, String path, boolean filePerThread) {
		// init String Buffer in ForumFormatter
		super();
		// where to write
		this.exportStream = exportStream;
		this.filePerThread = filePerThread;
		this.path = path;
		addStandardImages();
	}
	
	private String fileName;

	/**
	 * @see org.olat.core.util.tree.Visitor#visit(org.olat.core.util.nodes.INode)
	 */
	public void visit(INode node) {
		MessageNode mn = (MessageNode)node;
		if (isTopThread) {
			//important!
			fileName = "Thread_" + mn.getKey().toString() + ".rtf";
			isTopThread = false;
		}
		// Message Title
		sb.append("{\\pard \\brdrb\\brdrs\\brdrw10 \\f1\\fs30\\b ");
		sb.append(getImageRTF(mn));
		sb.append(getTitlePrefix(mn));
		sb.append(mn.getTitle());
		sb.append("\\par}");
		// Message Body
		sb.append("{\\pard \\f0");
		sb.append(convertHTMLMarkupToRTF(mn.getBody()));
		sb.append("\\par}");
		// Message key
		sb.append("{\\pard \\f0\\fs15 Message key: ");
		sb.append(mn.getKey());
		sb.append("} \\line ");
		sb.append("{\\pard \\f0\\fs15 created: ");
		// Creator and creation date
		sb.append(mn.getCreator().getUser().getProperty(UserConstants.FIRSTNAME, null));
		sb.append(", ");
		sb.append(mn.getCreator().getUser().getProperty(UserConstants.LASTNAME, null));
		sb.append(" ");
		sb.append(mn.getCreationDate().toString());
		// Modifier and modified date
		Identity modifier = mn.getModifier();
		if (modifier != null) {
			sb.append(" \\line modified: ");
			sb.append(modifier.getUser().getProperty(UserConstants.FIRSTNAME, null));
			sb.append(", ");
			sb.append(modifier.getUser().getProperty(UserConstants.LASTNAME,  null));
			sb.append(" ");
			sb.append(mn.getModifiedDate().toString());
		}
		sb.append(" \\par}");
		// attachment(s)
		OlatRootFolderImpl msgContainer = fm.getMessageContainer(getForumKey(), mn.getKey());
		List<VFSItem> attachments = msgContainer.getItems();
		if (attachments != null && attachments.size() > 0){
			sb.append("{\\pard \\f0\\fs15 Attachment(s): ");
			boolean commaFlag = false;
			for (VFSItem attachment: attachments) {
				if (commaFlag) sb.append(", ");
				sb.append(attachment.getName());
				commaFlag = true;
				
				ZipUtil.addToZip(attachment, path + "/attachments", exportStream);
			}
			sb.append("} \\line");
		}
		sb.append("{\\pard \\brdrb\\brdrs\\brdrw10 \\par}");
	}
	
	private void addStandardImages() {
		String[] images = new String[]{ "fo_sticky_closed", "fo_closed", "fo_sticky"};
		try {
			for(String image:images) {
				String iconPath = getImagePath(image);		
				if (iconPath != null) {
					File file = new File(iconPath);
					if (file.exists()) {
						exportStream.putNextEntry(new ZipEntry(path + "/" + file.getName()));
						FileUtils.copyFile(file, exportStream);
						exportStream.closeEntry();
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @see org.olat.modules.fo.archiver.formatters.ForumFormatter#openThread()
	 */
	public void openThread() {
		super.openThread();
		if(filePerThread){
			sb.append("{\\rtf1\\ansi\\deff0");
			sb.append("{\\fonttbl {\\f0\\fswiss Arial;}} ");
			sb.append("\\deflang1033\\plain");
		}
		sb.append("{\\pard \\brdrb \\brdrs \\brdrdb \\brsp20 \\par}{\\pard\\par}");
	}

	/**
	 * 
	 * @see org.olat.modules.fo.archiver.formatters.ForumFormatter#getThreadResult()
	 */
	public StringBuilder closeThread() {
		String footerThread = "{\\pard \\brdrb \\brdrs \\brdrw20 \\brsp20 \\par}{\\pard\\par}";
		sb.append(footerThread);
		if(filePerThread){
			sb.append("}");
			writeToFile(sb);
			sb = new StringBuilder();
		}
		return sb;
	}
	
	/**
	 * 
	 * @see org.olat.modules.fo.archiver.formatters.ForumFormatter#openForum()
	 */
	public void openForum(){
		if(!filePerThread){
			//make one ForumFile
			Long forumKey = getForumKey();
			fileName = "Threads_" + forumKey.toString() + ".rtf";
			sb.append("{\\rtf1\\ansi\\deff0");
			sb.append("{\\fonttbl {\\f0\\fswiss Arial;}} ");
			sb.append("\\deflang1033\\plain");
		}
	}

	/**
	 * 
	 * @see org.olat.modules.fo.archiver.formatters.ForumFormatter#closeForum()
	 */
	public StringBuilder closeForum(){
		if(!filePerThread){
			String footerForum = "}";
			sb.append(footerForum);
			writeToFile(sb);			
		}
		return sb;
	}
	
	/**
	 * 
	 * @param append
	 * @param buff
	 */
	private void writeToFile(StringBuilder buff){
		try {
			StringBuilder out = new StringBuilder();
			int len = buff.length();
			for (int i = 0; i < len; i++) {
				char c = buff.charAt(i);
				int val = c;
				if (val > 127) {
					out.append("\\u").append(String.valueOf(val)).append("?");
				} else {
					out.append(c);
				}
			}
			String encoded = out.toString();
			exportStream.putNextEntry(new ZipEntry(path + "/" + fileName));
			IOUtils.write(encoded, exportStream);
			exportStream.closeEntry();
		} catch (UnsupportedEncodingException ueEx) {
			throw new AssertException("could not encode stream from forum export file: " + ueEx);
		} catch (IOException e) {
			throw new AssertException("could not write to forum export file: " + e);
		}
	}
	
	/**
	 * 
	 * @param originalText
	 * @return
	 */
	private String convertHTMLMarkupToRTF(String originalText){
		String htmlText = originalText;

		Matcher mb = PATTERN_HTML_BOLD.matcher(htmlText);
		StringBuffer bolds = new StringBuffer();
		while (mb.find()) {
			mb.appendReplacement(bolds, "{\\\\b $1} ");
		}
		mb.appendTail(bolds);
		htmlText = bolds.toString();

		Matcher mi = PATTERN_HTML_ITALIC.matcher(htmlText);
		StringBuffer italics = new StringBuffer();
		while (mi.find()) {
			mi.appendReplacement(italics, "{\\\\i $1} ");
		}
		mi.appendTail(italics);
		htmlText = italics.toString();
		
		Matcher mbr = PATTERN_HTML_BREAK.matcher(htmlText);
		StringBuffer breaks = new StringBuffer();
		while(mbr.find()){
			mbr.appendReplacement(breaks, "\\\\line ");
		}
		mbr.appendTail(breaks);
		htmlText = breaks.toString();
		
		Matcher mofo = PATTERN_CSS_O_FOQUOTE.matcher(htmlText);
		StringBuffer foquotes = new StringBuffer();
		while(mofo.find()){
			mofo.appendReplacement(foquotes, "\\\\line {\\\\i $1} {\\\\pard $2\\\\par}");
		}
		mofo.appendTail(foquotes);
		htmlText = foquotes.toString();
		
		Matcher mp = PATTERN_HTML_PARAGRAPH.matcher(htmlText);
		StringBuffer paragraphs = new StringBuffer();
		while(mp.find()){
			mp.appendReplacement(paragraphs, "\\\\line $1 \\\\line");
		}
		mp.appendTail(paragraphs);
		htmlText = paragraphs.toString();
		
		Matcher mahref = PATTERN_HTML_AHREF.matcher(htmlText);
		StringBuffer ahrefs = new StringBuffer();
		while(mahref.find()){
			mahref.appendReplacement(ahrefs, "{\\\\field{\\\\*\\\\fldinst{HYPERLINK\"$1\"}}{\\\\fldrslt{\\\\ul $2}}}");
		}
		mahref.appendTail(ahrefs);
		htmlText = ahrefs.toString();
		
		Matcher mli = PATTERN_HTML_LIST.matcher(htmlText);
		StringBuffer lists = new StringBuffer();
		while(mli.find()){
			mli.appendReplacement(lists, "$1\\\\line ");
		}
		mli.appendTail(lists);
		htmlText = lists.toString();
		
		Matcher mtp = PATTERN_THREEPOINTS.matcher(htmlText);
		StringBuffer tps = new StringBuffer();
		while (mtp.find()) {
			mtp.appendReplacement(tps, THREEPOINTS);
		}
		mtp.appendTail(tps);
		htmlText = tps.toString();

		// strip all other html-fragments, because not convertable that easy
		htmlText = FilterFactory.getHtmlTagsFilter().filter(htmlText);
		// Remove all &nbsp;
		Matcher tmp = HTML_SPACE_PATTERN.matcher(htmlText);
		htmlText = tmp.replaceAll(" ");
		htmlText = StringEscapeUtils.unescapeHtml(htmlText);

		return htmlText;
	}
	
	/**
	 * 
	 * @param messageNode
	 * @return title prefix for hidden forum threads.
	 */
	private String getTitlePrefix(MessageNode messageNode) {
		StringBuilder sb = new StringBuilder();
		if(messageNode.isHidden()) {
			sb.append(HIDDEN_STR);
		} 		
		if(sb.length()>1) {
			sb.append(": ");
		}
		return sb.toString();
	}
	
	/**
	 * Gets the RTF image section for the input messageNode.
	 * @param messageNode
	 * @return the RTF image section for the input messageNode.
	 */
	private String getImageRTF(MessageNode messageNode) {			
		StringBuilder sb = new StringBuilder();
		for(String fileName : addImagesToVFSContainer(messageNode)) {
			sb.append("{\\field\\fldedit{\\*\\fldinst { INCLUDEPICTURE ")
			  .append("\"").append(fileName).append("\"")
			  .append(" \\\\d }}{\\fldrslt {}}}");			
		}				
		return sb.toString();
	}
	
	/**
	 * Retrieves the appropriate images for the input messageNode, if any, 
	 * and adds it to the input container.
	 * 
	 * @param messageNode
	 * @param container
	 * @return
	 */
	private List<String> addImagesToVFSContainer(MessageNode messageNode) {
		List<String> fileNameList = new ArrayList<String>();
		String iconPath = null;
		if(messageNode.isClosed() && messageNode.isSticky()) {
			iconPath = getImagePath("fo_sticky_closed");
		} else if(messageNode.isClosed()) {
			iconPath = getImagePath("fo_closed");			
		} else if(messageNode.isSticky()) {
			iconPath = getImagePath("fo_sticky");			
		}		
		if (iconPath != null) {
			File file = new File(iconPath);
			if (file.exists()) {
				fileNameList.add(file.getName());
			} else {
				log.error("Could not find image for forum RTF formatter::" + iconPath);
			}
		}
		return fileNameList;
	}
	
	/**
	 * TODO: LD: to clarify whether there it a better way to get the image path?
	 * Gets the image path.
	 * @param val
	 * @return the path of the static icon image.
	 */
	private String getImagePath(Object val) { 		
		return WebappHelper.getContextRealPath("/static/images/forum/" + val.toString() + ".png");				
	}
}
