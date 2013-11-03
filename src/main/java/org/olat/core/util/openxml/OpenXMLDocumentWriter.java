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
package org.olat.core.util.openxml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.openxml.OpenXMLDocument.HeaderReference;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * 
 * Initial date: 03.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OpenXMLDocumentWriter {
	
	private static final OLog log = Tracing.createLoggerFor(OpenXMLDocumentWriter.class);
	
	public static final String SCHEMA_CONTENT_TYPES = "http://schemas.openxmlformats.org/package/2006/content-types";
	public static final String SCHEMA_CORE_PROPERTIES = "http://schemas.openxmlformats.org/package/2006/metadata/core-properties";
	public static final String SCHEMA_EXT_PROPERTIES = "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties";
	public static final String SCHEMA_DOC_PROPS_VT = "http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes";
	public static final String SCHEMA_DC_TERMS = "http://purl.org/dc/terms/";
	public static final String SCHEMA_DC = "http://purl.org/dc/elements/1.1/";
	public static final String SCHEMA_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
	
	public static final String CT_RELATIONSHIP = "application/vnd.openxmlformats-package.relationships+xml";
	public static final String CT_EXT_PROPERTIES = "application/vnd.openxmlformats-officedocument.extended-properties+xml";
	public static final String CT_CORE_PROPERTIES = "application/vnd.openxmlformats-package.core-properties+xml";
	public static final String CT_WORD_DOCUMENT = "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml";
	public static final String CT_NUMBERING = "application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml";
	public static final String CT_STYLES = "application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml";
	public static final String CT_HEADER = "application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml";
	
	
	public void createDocument(ZipOutputStream out, OpenXMLDocument document)
	throws IOException {
		//flush header...
		document.appendPageSettings();
		
		//_rels
		ZipEntry rels = new ZipEntry("_rels/.rels");
		out.putNextEntry(rels);
		createShadowDocumentRelationships(out);
		out.closeEntry();
		
		//[Content_Types].xml
		ZipEntry contentType = new ZipEntry("[Content_Types].xml");
		out.putNextEntry(contentType);
		createContentTypes(document, out);
		out.closeEntry();
		
		//docProps/app.xml
		ZipEntry app = new ZipEntry("docProps/app.xml");
		out.putNextEntry(app);
		createDocPropsApp(out);
		out.closeEntry();
		
		//docProps/core.xml
		ZipEntry core = new ZipEntry("docProps/core.xml");
		out.putNextEntry(core);
		createDocPropsCore(out);
		out.closeEntry();
		
		//word/_rels/document.xml.rels
		ZipEntry docRels = new ZipEntry("word/_rels/document.xml.rels");
		out.putNextEntry(docRels);
		createDocumentRelationships(out, document);
		out.closeEntry();
		
		//word/media
		appendMedias(out, document);
		
		//word/document.xml
		ZipEntry wordDocument = new ZipEntry("word/document.xml");
		out.putNextEntry(wordDocument);
		OpenXMLUtils.writeTo(document.getDocument(), out, false);
		out.closeEntry();
		
		//word/headerxxx.xml
		for(HeaderReference headerRef:document.getHeaders()) {
			ZipEntry headerDocument = new ZipEntry("word/" + headerRef.getFilename());
			out.putNextEntry(headerDocument);
			IOUtils.write(headerRef.getHeader(), out);
			out.closeEntry();
		}

		//word/styles.xml
		ZipEntry styles = new ZipEntry("word/styles.xml");
		out.putNextEntry(styles);
		appendPredefinedStyles(out, document.getStyles());
		out.closeEntry();
	}
	
	protected void appendMedias(ZipOutputStream out, OpenXMLDocument document)
	throws IOException {
		for(DocReference img:document.getImages()) {
			FileInputStream in = new FileInputStream(img.getFile());
			ZipEntry wordDocument = new ZipEntry("word/media/" + img.getFile().getName());
			out.putNextEntry(wordDocument);

			IOUtils.copy(in, out);
			OpenXMLUtils.writeTo(document.getDocument(), out, false);
			out.closeEntry();
		}
	}
	
	private void appendPredefinedStyles(ZipOutputStream out, OpenXMLStyles styles) {
		InputStream in = null;
		try {
			in = OpenXMLDocumentWriter.class.getResourceAsStream("_resources/styles.xml");
			if(styles != null) {
				Document stylesDoc = OpenXMLUtils.createDocument(in);
				NodeList stylesElList = stylesDoc.getElementsByTagName("w:styles");
				if(stylesElList.getLength() == 1) {
					//Node stylesEl = stylesElList.item(0);
					//System.out.println("Append:" + stylesEl);
				}
				OpenXMLUtils.writeTo(stylesDoc, out, false);
			} else {
				IOUtils.copy(in, out);
			}
		} catch (IOException e) {
			log.error("", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
	
	/*
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/settings" Target="settings.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/webSettings" Target="webSettings.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/fontTable" Target="fontTable.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="theme/theme1.xml"/>
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
  <Relationship Id="rId2" Type="http://schemas.microsoft.com/office/2007/relationships/stylesWithEffects" Target="stylesWithEffects.xml"/>
</Relationships>
	 */
	protected void createDocumentRelationships(OutputStream out, OpenXMLDocument document) {
		try {
			Document doc = OpenXMLUtils.createDocument();
			Element relationshipsEl = (Element)doc.appendChild(doc.createElement("Relationships"));
			relationshipsEl.setAttribute("xmlns", SCHEMA_RELATIONSHIPS);

			addRelationship("rId1", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles",
					"styles.xml", relationshipsEl, doc);
			
			if(document != null) {
				for(DocReference docRef:document.getImages()) {
					addRelationship(docRef.getId(), "http://schemas.openxmlformats.org/officeDocument/2006/relationships/image",
							"media/" + docRef.getFile().getName(), relationshipsEl, doc);
				}
				
				for(HeaderReference headerRef:document.getHeaders()) {
					addRelationship(headerRef.getId(), "http://schemas.openxmlformats.org/officeDocument/2006/relationships/header",
							headerRef.getFilename(), relationshipsEl, doc);
				}
			}

			OpenXMLUtils.writeTo(doc, out, false);
		} catch (DOMException e) {
			log.error("", e);
		}
	}
	
	/*
<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
<Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>
	*/
	protected void createShadowDocumentRelationships(OutputStream out) {
		try {
			Document doc = OpenXMLUtils.createDocument();
			Element relationshipsEl = (Element)doc.appendChild(doc.createElement("Relationships"));
			relationshipsEl.setAttribute("xmlns", SCHEMA_RELATIONSHIPS);

			addRelationship("rId1", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties",
					"docProps/core.xml", relationshipsEl, doc);
			addRelationship("rId2", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties",
					"docProps/app.xml", relationshipsEl, doc);
			addRelationship("rId3", "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument",
					"word/document.xml", relationshipsEl, doc);
			
			OpenXMLUtils.writeTo(doc, out, false);
		} catch (DOMException e) {
			log.error("", e);
		}
	}
	
	private final void addRelationship(String id, String type, String target, Element propertiesEl, Document doc) {
		Element relEl = (Element)propertiesEl.appendChild(doc.createElement("Relationship"));
		relEl.setAttribute("Id", id);
		relEl.setAttribute("Type", type);
		relEl.setAttribute("Target", target);
	}
	
	/*
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
  xmlns:dcterms="http://purl.org/dc/terms/"
  xmlns:dc="http://purl.org/dc/elements/1.1/">
	<dc:creator>docx4j</dc:creator>
	<cp:lastModifiedBy>docx4j</cp:lastModifiedBy>
</cp:coreProperties>
	 */
	protected void createDocPropsCore(OutputStream out) {
		try {
			Document doc = OpenXMLUtils.createDocument();
			Element propertiesEl = (Element)doc.appendChild(doc.createElement("cp:coreProperties"));
			propertiesEl.setAttribute("xmlns:cp", SCHEMA_CORE_PROPERTIES);
			propertiesEl.setAttribute("xmlns:dcterms", SCHEMA_DC_TERMS);
			propertiesEl.setAttribute("xmlns:dc", SCHEMA_DC);
			addDCProperty("creator", "OpenOLAT", propertiesEl, doc);
			addCPProperty("lastModifiedBy", "OpenOLAT", propertiesEl, doc);
			OpenXMLUtils.writeTo(doc, out, false);
		} catch (DOMException e) {
			log.error("", e);
		}
	}
	
	private final void addCPProperty(String name, String value, Element propertiesEl, Document doc) {
		Element defaultEl = (Element)propertiesEl.appendChild(doc.createElement("cp:" + name));
		defaultEl.appendChild(doc.createTextNode(value));
	}
	
	private final void addDCProperty(String name, String value, Element propertiesEl, Document doc) {
		Element defaultEl = (Element)propertiesEl.appendChild(doc.createElement("dc:" + name));
		defaultEl.appendChild(doc.createTextNode(value));
	}
	
/*
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<properties:Properties xmlns:properties="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
  xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
	<properties:Application>OpenOLAT</properties:Application>
	<properties:AppVersion>9.1.0.</properties:AppVersion>
</properties:Properties>
 */
	protected void createDocPropsApp(OutputStream out) {
		try {
			Document doc = OpenXMLUtils.createDocument();
			Element propertiesEl = (Element)doc.appendChild(doc.createElement("properties:Properties"));
			propertiesEl.setAttribute("xmlns:properties", SCHEMA_EXT_PROPERTIES);
			addExtProperty("Application", "OpenOLAT", propertiesEl, doc);
			addExtProperty("AppVersion", "9.1.0", propertiesEl, doc);
			OpenXMLUtils.writeTo(doc, out, false);
		} catch (DOMException e) {
			log.error("", e);
		}
	}
	
	private final void addExtProperty(String name, String value, Element propertiesEl, Document doc) {
		Element defaultEl = (Element)propertiesEl.appendChild(doc.createElement("properties:" + name));
		defaultEl.appendChild(doc.createTextNode(value));
	}
	
/*
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
	<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml" />
	<Override ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml" PartName="/docProps/app.xml" />
	<Override ContentType="application/vnd.openxmlformats-package.core-properties+xml" PartName="/docProps/core.xml" />
	<Override ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml" PartName="/word/document.xml" />
	<Override ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.numbering+xml" PartName="/word/numbering.xml" />
	<Override ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml" PartName="/word/styles.xml" />
</Types>
 */
	protected void createContentTypes(OpenXMLDocument document, OutputStream out) {
		Document doc = OpenXMLUtils.createDocument();
		Element typesEl = (Element)doc.appendChild(doc.createElement("Types"));
		typesEl.setAttribute("xmlns", SCHEMA_CONTENT_TYPES);
		//Default
		createContentTypesDefault("rels", CT_RELATIONSHIP, typesEl, doc);
		createContentTypesDefault("xml", "application/xml", typesEl, doc);
		createContentTypesDefault("jpeg", "image/jpeg", typesEl, doc);
		createContentTypesDefault("jpg", "image/jpeg", typesEl, doc);
		createContentTypesDefault("png", "image/png", typesEl, doc);
		createContentTypesDefault("gif", "image/gif", typesEl, doc);
		//Override
		createContentTypesOverride("/docProps/app.xml", CT_EXT_PROPERTIES, typesEl, doc);
		createContentTypesOverride("/docProps/core.xml", CT_CORE_PROPERTIES, typesEl, doc);
		createContentTypesOverride("/word/document.xml", CT_WORD_DOCUMENT, typesEl, doc);
		createContentTypesOverride("/word/styles.xml", CT_STYLES, typesEl, doc);
		
		for(HeaderReference headerRef:document.getHeaders()) {
			createContentTypesOverride("/word/" + headerRef.getFilename(), CT_HEADER, typesEl, doc);
		}
		OpenXMLUtils.writeTo(doc, out, false);
	}
	
	private final void createContentTypesDefault(String extension, String type, Element typesEl, Document doc) {
		Element defaultEl = (Element)typesEl.appendChild(doc.createElement("Default"));
		defaultEl.setAttribute("Extension", extension);
		defaultEl.setAttribute("ContentType", type);
	}
	
	private final void createContentTypesOverride(String partName, String type, Element typesEl, Document doc) {
		Element overrideEl = (Element)typesEl.appendChild(doc.createElement("Override"));
		overrideEl.setAttribute("PartName", partName);
		overrideEl.setAttribute("ContentType", type);
	}
	

}