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
package org.olat.course.db;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

import org.olat.core.gui.media.MediaResource;
import org.olat.core.util.StringHelper;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  13 déc. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class CourseDBMediaResource implements MediaResource {
	
	private final String charset;
	private final String fileName;
	private final byte[] content;
	
	public CourseDBMediaResource(String charset, String fileName, byte[] content) {
		this.charset = charset;
		this.fileName = fileName;
		this.content = content;
	}

	@Override
	public String getContentType() {
		return "application/vnd.ms-excel; charset=" + charset;
	}

	@Override
	public Long getSize() {
		return content == null ? new Long(0) : content.length;
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(content);
	}

	@Override
	public Long getLastModified() {
		return -1l;
	}

	@Override
	public void prepare(HttpServletResponse hres) {
		hres.setHeader("Content-Disposition","filename=\"" + StringHelper.urlEncodeISO88591(fileName) + "\"");
		hres.setHeader("Content-Description",StringHelper.urlEncodeISO88591(fileName));

	}

	@Override
	public void release() {
		//
	}
}