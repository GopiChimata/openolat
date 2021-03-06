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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.cyberneko.html.parsers.SAXParser;
import org.olat.core.commons.services.image.ImageUtils;
import org.olat.core.commons.services.image.Size;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.io.ShieldInputStream;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import fmath.conversion.ConvertFromLatexToMathML;
import fmath.conversion.ConvertFromMathMLToWord;

/**
 * 
 * Initial date: 04.09.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class OpenXMLDocument {
	
	private static final OLog log = Tracing.createLoggerFor(OpenXMLDocument.class);
	
	private final Document document;
	private final Element rootElement;
	private final Element bodyElement;
	
	private final OpenXMLStyles styles;
	
	private int currentId = 4;
	private int currentNumberingId = 0;
	private String documentHeader;
	private Set<String> imageFilenames = new HashSet<>();
	private Map<File, DocReference> fileToImagesMap = new HashMap<File, DocReference>();
	
	private List<Node> cursorStack = new ArrayList<>();
	private List<ListParagraph> numbering = new ArrayList<>();
	private List<HeaderReference> headers = new ArrayList<>();
	
	private VFSContainer mediaContainer;
	
	public OpenXMLDocument() {
		document = OpenXMLUtils.createDocument();
		rootElement =	createRootElement(document);
		bodyElement = createBodyElement(rootElement, document);
		styles = new OpenXMLStyles();
		cursorStack.add(bodyElement);
	}
	
	public String getDocumentHeader() {
		return documentHeader;
	}
	
	public void setDocumentHeader(String header) {
		documentHeader = header;
		if(StringHelper.containsNonWhitespace(documentHeader)) {
			documentHeader = documentHeader.replace("&", "&amp;");
		}
	}
	
	public VFSContainer getMediaContainer() {
		return mediaContainer;
	}

	public void setMediaContainer(VFSContainer mediaContainer) {
		this.mediaContainer = mediaContainer;
	}

	public Document getDocument() {
		return document;
	}
	
	public OpenXMLStyles getStyles() {
		return styles;
	}
	
	public Collection<DocReference> getImages() {
		return fileToImagesMap.values();
	}
	
	public Collection<HeaderReference> getHeaders() {
		return headers;
	}
	
	public Collection<ListParagraph> getNumbering() {
		return numbering;
	}
	
	public Node getCursor() {
		return cursorStack.get(cursorStack.size() - 1);
	}
	
	public void pushCursor(Node el) {
		cursorStack.add(el);
	}
	
	public void popCursor(Node el) {
		int index = cursorStack.indexOf(el);
		if(index > 1) {
			for(int i=cursorStack.size(); i-->index; ) {
				cursorStack.remove(i);
			}
		}
	}
	
	public void resetCursor() {
		for(int i=cursorStack.size(); i-->1; ) {
			cursorStack.remove(i);
		}
	}
	
	public void appendTitle(String text) {
		appendHeading(text, Heading.title, null);
	}
	
	public void appendHeading1(String text, String additionalText) {
		appendHeading(text, Heading.heading1, additionalText);
	}
	
	public void appendHeading2(String text, String additionalText) {
		appendHeading(text, Heading.heading2, additionalText);
	}
	
	private void appendHeading(String text, Heading style, String additionalText) {
		if(!StringHelper.containsNonWhitespace(text)) return;

		Element textEl = createTextEl(text);
		List<Element> runsEl = new ArrayList<Element>(2);
		Element runEl = createRunEl(Collections.singletonList(textEl));
		runsEl.add(runEl);
		Element styleEl = createParagraphStyle(style.styleId());
		if(StringHelper.containsNonWhitespace(additionalText)) {
			//add an "insecable" blank between the title and the additional text
			Element blankRunEl = document.createElement("w:r");
			blankRunEl.appendChild(createPreserveSpaceEl());
			runsEl.add(blankRunEl);
			
			//add additional text
			Element addRunEl = document.createElement("w:r");
			Node addRunPrefsEl = addRunEl.appendChild(document.createElement("w:rPr"));
			Element bEl = (Element)addRunPrefsEl.appendChild(document.createElement("w:b"));
			bEl.setAttribute("w:val", "0");
			Element colorEl = (Element)addRunPrefsEl.appendChild(document.createElement("w:color"));
			colorEl.setAttribute("w:val", "auto");
			Element szEl = (Element)addRunPrefsEl.appendChild(document.createElement("w:sz"));
			szEl.setAttribute("w:val", "24");
			Element szCsEl = (Element)addRunPrefsEl.appendChild(document.createElement("w:szCs"));
			szCsEl.setAttribute("w:val", "24");

			addRunEl.appendChild(createTextEl(additionalText));
			runsEl.add(addRunEl);
		}

		Element paragraphEl = createParagraphEl(styleEl, runsEl);
		getCursor().appendChild(paragraphEl);
	}
/*
<w:sectPr w:rsidR="00F528BA" w:rsidRPr="00DF16C8" w:rsidSect="007347AA">
	<w:pgSz w:w="11900" w:h="16840" />
	<w:pgMar w:top="1417" w:right="1417" w:bottom="1134" w:left="1417" w:header="708" w:footer="708" w:gutter="0" />
	<w:cols w:space="708" />
	<w:docGrid w:linePitch="360" />
</w:sectPr>
 */
	/**
	 * Must be done at the end of the document
	 */
	public void appendPageSettings() {
		Node lastChild = bodyElement.getLastChild();
		if(lastChild != null && "w:sectPr".equals(lastChild.getLocalName())) {
			return;//nothing to do, already set
		}

		Node sectionPrefs = bodyElement.appendChild(document.createElement("w:sectPr"));
		//A4
		Element pageSize = (Element)sectionPrefs.appendChild(document.createElement("w:pgSz"));
		pageSize.setAttribute("w:w", "11900");
		pageSize.setAttribute("w:h", "16840");
		Element margins = (Element)sectionPrefs.appendChild(document.createElement("w:pgMar"));
		margins.setAttribute("w:top", "1440");
		margins.setAttribute("w:right", "1440");
		margins.setAttribute("w:bottom", "1440");
		margins.setAttribute("w:left", "1440");
		margins.setAttribute("w:header", "708");
		margins.setAttribute("w:footer", "708");
		margins.setAttribute("w:gutter", "0");
		
		if(StringHelper.containsNonWhitespace(documentHeader)) {
			try(InputStream headerIn = OpenXMLDocument.class.getResourceAsStream("_resources/header.xml")) {
				String headerTemplate = IOUtils.toString(headerIn);
				String header = headerTemplate.replace("[oodocumentitlte]", documentHeader);

				String headerId = generateId();
				Element headerRefEl = (Element)sectionPrefs.appendChild(document.createElement("w:headerReference"));
				headerRefEl.setAttribute("w:type", "default");
				headerRefEl.setAttribute("r:id", headerId);
				
				HeaderReference headerRef = new HeaderReference(headerId, header);
				headers.add(headerRef);
			} catch (DOMException e) {
				log.error("", e);
			} catch (IOException e) {
				log.error("", e);
			}
		}
	}
	
	public void appendFillInBlanck(int length, boolean newParagraph) {
		Element paragraphEl = getParagraphToAppendTo(newParagraph);
		
		Node runEl = paragraphEl.appendChild(createRunEl(null));
		runEl.appendChild(createRunPrefsEl(Style.underline));

		int tabLength = length / 5;
		for(int i=tabLength; i-->0; ) {
			runEl.appendChild(document.createElement("w:tab"));
		}
		getCursor().appendChild(paragraphEl);
	}
	
/*
<w:p w:rsidR="00F528BA" w:rsidRPr="00245F75" w:rsidRDefault="00F528BA" w:rsidP="00245F75">
	<w:pPr>
		<w:pBdr>
			<w:bottom w:val="single" w:sz="4" w:space="1" w:color="auto" />
		</w:pBdr>
	</w:pPr>
</w:p>
 */
	public void appendFillInBlanckWholeLine(int rows) {
		for(int i=rows+1; i-->0; ) {
			Element paragraphEl = createParagraphEl();
			Node pargraphPrefs = paragraphEl.appendChild(document.createElement("w:pPr"));
			Node pargraphBottomPrefs = pargraphPrefs.appendChild(document.createElement("w:pBdr"));
			Element bottomEl = (Element)pargraphBottomPrefs.appendChild(document.createElement("w:between"));
			bottomEl.setAttribute("w:val", "single");
			bottomEl.setAttribute("w:sz", "4");
			bottomEl.setAttribute("w:space", "1");
			bottomEl.setAttribute("w:color", "auto");
			getCursor().appendChild(paragraphEl);
		}
	}
	
	public void appendText(String text, boolean newParagraph, Style... styles) {
		if(!StringHelper.containsNonWhitespace(text)) return;
		
		List<Element> textEls = new ArrayList<Element>();
		for(StringTokenizer tokenizer = new StringTokenizer(text, "\n\r"); tokenizer.hasMoreTokens(); ) {
			String token = tokenizer.nextToken();
			Element textEl = createTextEl(token);
			textEls.add(textEl);
			if(tokenizer.hasMoreTokens()) {
				textEls.add(createBreakEl());
			}
		}
		
		if(textEls.size() > 0) {
			Element paragraphEl = getParagraphToAppendTo(newParagraph);
			Element runEl = document.createElement("w:r");
			if(styles != null && styles.length > 0) {
				runEl.appendChild(createRunPrefsEl(styles));
			}
			for(Element textEl:textEls) {
				runEl.appendChild(textEl);
			}
			paragraphEl.appendChild(runEl);
			getCursor().appendChild(paragraphEl);
		}
	}
	
	/**
	 * Get a paragraph, if @param newParagraph is false, try to get the
	 * last paragraph of the cursor. If @param newParagraph is true, create
	 * always a new paragraph.
	 * @param newParagraph
	 * @return
	 */
	private Element getParagraphToAppendTo(boolean newParagraph) {
		Element paragraphEl = null;
		if(!newParagraph) {
			paragraphEl = getCurrentParagraph();
			//add a blank between
			if(paragraphEl != null) {
				Element runEl = document.createElement("w:r");
				runEl.appendChild(createPreserveSpaceEl());
				paragraphEl.appendChild(runEl);
			}
		}
		if(paragraphEl == null) {
			paragraphEl = createParagraphEl();
		}
		return paragraphEl;
	}
	
	/**
	 * Return the paragraph if and only if it's the last element. Return
	 * null if not found.
	 * @return
	 */
	private Element getCurrentParagraph() {
		Element paragraphEl = null;
		Node currentNode = getCursor();
		if(currentNode != null && currentNode.getLastChild() != null
				&& "w:p".equals(currentNode.getLastChild().getNodeName())) {
			paragraphEl = (Element)currentNode.getLastChild();
		}
		return paragraphEl;
	}
	
	public void appendPageBreak() {
		getCursor().appendChild(createPageBreakEl());
	}
	
	public void appendBreak(boolean newParagraph) {
		Element breakEl = createBreakEl();
		Element paragraphEl = getParagraphToAppendTo(newParagraph);
		paragraphEl.appendChild(createRunEl(Collections.singletonList(breakEl)));
		getCursor().appendChild(paragraphEl);
	}
	
	public void appendHtmlText(String html, boolean newParagraph) {
		if(!StringHelper.containsNonWhitespace(html)) return;
		try {
			SAXParser parser = new SAXParser();
			Element paragraphEl = getParagraphToAppendTo(newParagraph);
			parser.setContentHandler(new HTMLToOpenXMLHandler(this, paragraphEl));
			parser.parse(new InputSource(new StringReader(html)));
		} catch (SAXException e) {
			log.error("", e);
		} catch (IOException e) {
			log.error("", e);
		}
	}
	
	public Node appendTable(Integer... width) {
		Element tableEl = createTable(width);
		return getCursor().appendChild(tableEl);
	}
	
/*
<w:pPr>
	<w:pStyle w:val="berschrift1" />
</w:pPr>
 */
	public Element createParagraphStyle(String styleId) {
		Element paragraphEl = document.createElement("w:pPr");
		Element styleEl = (Element)paragraphEl.appendChild(document.createElement("w:pStyle"));
		styleEl.setAttribute("w:val", styleId);
		return paragraphEl;
	}
	
/*
<w:p w:rsidR="003231EA" w:rsidRDefault="00A53C12">
		<w:r>
				<w:t>Hello word</w:t>
		</w:r>
</w:p>
 */
	public Element createParagraphEl(Element styleEl, Collection<Element> runEls) {
		Element paragraphEl = document.createElement("w:p");
		if(styleEl != null) {
			paragraphEl.appendChild(styleEl);
		}
		for(Element runEl:runEls) {
			paragraphEl.appendChild(runEl);
		}
		return paragraphEl;
	}
	
	public Element createParagraphEl() {
		Element paragraphEl = document.createElement("w:p");
		return paragraphEl;
	}
	
	public Element createRunEl(Collection<? extends Node> textEls) {
		Element runEl = document.createElement("w:r");
		if(textEls != null && textEls.size() > 0) {
			for(Node textEl:textEls) {
				runEl.appendChild(textEl);
			}
		}
		return runEl;
	}
	
	public Node createRunPrefsEl(Style... styles) {
		Element runPrefsEl = document.createElement("w:rPr");
		return createRunPrefsEl(runPrefsEl, styles);
	}
	
	public Node createRunPrefsEl(Node runPrefsEl, Style... styles) {
		if(styles != null && styles.length > 0) {
			for(Style style:styles) {
				if(style != null) {
					switch(style) {
						case underline: {
							Element underlinePrefs = (Element)runPrefsEl.appendChild(document.createElement("w:u"));
							underlinePrefs.setAttribute("w:val", "single");
							break;
						}
						case italic: runPrefsEl.appendChild(document.createElement("w:i")); break;
						case bold: runPrefsEl.appendChild(document.createElement("w:b")); break;
						case strike: runPrefsEl.appendChild(document.createElement("w:strike")); break;
					}
				}
			}
		}
		return runPrefsEl;
	}
	
	public Node createRunReversePrefsEl(Node runPrefsEl, Style... styles) {
		if(styles != null && styles.length > 0) {
			for(Style style:styles) {
				if(style != null) {
					switch(style) {
						case underline:
							Element underlinePrefs = (Element)runPrefsEl.appendChild(document.createElement("w:u"));
							underlinePrefs.setAttribute("w:val", "none");
							break;
						case italic:
							Element italicPrefs = (Element)runPrefsEl.appendChild(document.createElement("w:i"));
							italicPrefs.setAttribute("w:val", "0");
							break;
						case bold:
							Element boldPrefs = (Element)runPrefsEl.appendChild(document.createElement("w:b"));
							boldPrefs.setAttribute("w:val", "0");
							break;
						case strike:
							Element strikePrefs = (Element)runPrefsEl.appendChild(document.createElement("w:strike"));
							strikePrefs.setAttribute("w:val", "0");
							break;
					}
				}
			}
		}
		return runPrefsEl;
	}
	
	/**
	 * Return a text element w:t
	 * @param text
	 * @return
	 */
	public Element createTextEl(String text) {
		Element textEl = document.createElement("w:t");
		textEl.appendChild(document.createTextNode(text));
		return textEl;
	}
	
	public Element createPreserveSpaceEl() {
		Element textEl = document.createElement("w:t");
		textEl.setAttribute("xml:space", "preserve");
		textEl.appendChild(document.createTextNode(" "));
		return textEl;
	}
	
	public Element createParagraphEl(String text) {
		Element paragraphEl = createParagraphEl();
		Node runEl = paragraphEl.appendChild(document.createElement("w:r"));

		for(StringTokenizer tokenizer = new StringTokenizer(text, "\n\r"); tokenizer.hasMoreTokens(); ) {
			String token = tokenizer.nextToken();
			Element textEl = createTextEl(token);
			runEl.appendChild(textEl);
			if(tokenizer.hasMoreTokens()) {
				runEl.appendChild(createBreakEl());
			}
		}
		
		return paragraphEl;
	}
	
	public Node createCheckbox(boolean checked) {
		try {
			String name = checked ? "image1.png" : "image2.png";
			URL imgUrl = OpenXMLDocument.class.getResource("_resources/" + name);
			File imgFile = new File(imgUrl.toURI());
			return createImageEl(imgFile);
		} catch (URISyntaxException e) {
			log.error("", e);
			return null;
		}
	}
	
	public Element createBreakEl() {
		return document.createElement("w:br");
	}
	
	public Element createPageBreakEl() {
		Element paragraphEl = document.createElement("w:p");
		Element runEl = (Element)paragraphEl.appendChild(document.createElement("w:r"));
		Element breakEl = (Element)runEl.appendChild(document.createElement("w:br"));
		breakEl.setAttribute("w:type", "page");
		return paragraphEl;
	}
	
	public Element createTable() {
		Element tableEl = document.createElement("w:tbl");
		
		//preferences table
		Element tablePrEl = (Element)tableEl.appendChild(document.createElement("w:tblPr"));
		createWidthEl("w:tblW", 5000, Unit.pct, tablePrEl);
		createWidthEl("w:tblCellSpacing", 22, Unit.dxa, tablePrEl);
		Node tableCellMarEl = tablePrEl.appendChild(document.createElement("w:tblCellMar"));
		createWidthEl("w:top", 45, Unit.dxa, tableCellMarEl);
		createWidthEl("w:left", 45, Unit.dxa, tableCellMarEl);
		createWidthEl("w:bottom", 45, Unit.dxa, tableCellMarEl);
		createWidthEl("w:right", 45, Unit.dxa, tableCellMarEl);
		Element tableLookEl = (Element)tablePrEl.appendChild(document.createElement("w:tblLook"));
		tableLookEl.setAttribute("w:val", "0000");
		tableLookEl.setAttribute("w:firstRow", "0");
		tableLookEl.setAttribute("w:lastRow", "0");
		tableLookEl.setAttribute("w:firstColumn", "0");
		tableLookEl.setAttribute("w:lastColumn", "0");
		tableLookEl.setAttribute("w:noHBand", "0");
		tableLookEl.setAttribute("w:noVBand", "0");
		
		//grid preferences
		tableEl.appendChild(document.createElement("w:tblGrid"));
		
		return tableEl;
	}
	
	/*
<w:tbl>
	<w:tblPr>
		<w:tblW w:w="5000" w:type="pct" />
		<w:tblCellSpacing w:w="22" w:type="dxa" />
		<w:tblBorders>
			<w:top w:val="nil" /><w:bottom w:val="nil" /><w:insideH w:val="nil" /><w:insideV w:val="nil" />
		</w:tblBorders>
		<w:tblCellMar>
			<w:top w:w="45" w:type="dxa" /><w:left w:w="45" w:type="dxa" /><w:bottom w:w="45" w:type="dxa" /><w:right w:w="45" w:type="dxa" />
		</w:tblCellMar>
		<w:tblLook w:val="0000" w:firstRow="0" w:lastRow="0" w:firstColumn="0" w:lastColumn="0" w:noHBand="0" w:noVBand="0" />
	</w:tblPr>
	<w:tblGrid>
		<w:gridCol w:w="10178" /><w:gridCol w:w="1116" />
	</w:tblGrid>
	 */
	public Element createTable(Integer... width) {
		Element tableEl = createTable();
		
		NodeList gridPrefs = tableEl.getElementsByTagName("w:tblGrid");
		Element tableGridEl = (Element)gridPrefs.item(0);
		//table grid
		for(Integer w:width) {
			createGridCol(w, tableGridEl);
		}
		
		return tableEl;
	}
/*
<w:tr>
	<w:trPr>
		<w:tblCellSpacing w:w="22" w:type="dxa" />
	</w:trPr>
 */
	public Element createTableRow() {
		Element rowEl = document.createElement("w:tr");
		//trPr
		return rowEl;	
	}
/*
<w:tc>
	<w:tcPr>
		<w:tcW w:w="0" w:type="auto" />
		<w:tcBorders>
			<w:top w:val="single" w:sz="6" w:space="0" w:color="E9EAF2" /><w:left w:val="single" w:sz="6" w:space="0" w:color="E9EAF2" /><w:bottom w:val="single" w:sz="6" w:space="0" w:color="E9EAF2" /><w:right w:val="single" w:sz="6" w:space="0" w:color="E9EAF2" />
		</w:tcBorders>
		<w:shd w:val="solid" w:color="E9EAF2" w:fill="auto" />
	</w:tcPr>
 */
	public Element createTableCell(String background, Integer width, Unit unit) {
		Element cellEl = document.createElement("w:tc");

		Node prefEl = null;
		if(unit != null) {
			prefEl = cellEl.appendChild(document.createElement("w:tcPr"));
			createWidthEl("w:tcW", width, unit, cellEl);
		}
		if(StringHelper.containsNonWhitespace(background)) {
			if(prefEl == null) {
				prefEl = cellEl.appendChild(document.createElement("w:tcPr"));
			}
			
			Node borderEl = prefEl.appendChild(document.createElement("w:tcBorders"));
			createBorder("w:top", background, borderEl);
			createBorder("w:left", background, borderEl);
			createBorder("w:bottom", background, borderEl);
			createBorder("w:right", background, borderEl);
			createShadow(background, prefEl);
		}

		return cellEl;	
	}
	
	public ListParagraph createListParagraph() {
		int abstractNumberingId = currentNumberingId++;
		int numberingId = currentNumberingId++;
		ListParagraph lp = new ListParagraph(abstractNumberingId, numberingId);
		numbering.add(lp);
		return lp;
	}
	
	/*
<w:p>
  <w:pPr>
    <w:pStyle w:val="ListParagraph"/>
    <w:numPr>
      <w:ilvl w:val="0"/>
      <w:numId w:val="1"/>
    </w:numPr>
  </w:pPr>
  <w:r>
    <w:t>One</w:t>
  </w:r>
</w:p>
	 */
	public Element createListParagraph(ListParagraph def) {
		Element paragraphEl = document.createElement("w:p");
		Element listEl = (Element)paragraphEl.appendChild(document.createElement("w:pPr"));
		Element pStyleEl = (Element)listEl.appendChild(document.createElement("w:pStyle"));
		pStyleEl.setAttribute("w:val", "ListParagraph");
		Element numberingEl = (Element)listEl.appendChild(document.createElement("w:numPr"));
		Element ilvlEl = (Element)numberingEl.appendChild(document.createElement("w:ilvl"));
		ilvlEl.setAttribute("w:val", "0");
		Element numIdEl = (Element)numberingEl.appendChild(document.createElement("w:numId"));
		numIdEl.setAttribute("w:val", Integer.toString(def.getNumId()));
		return paragraphEl;
	}
	
	/*
<w:abstractNum w:abstractNumId="0">
  <w:lvl w:ilvl="0">
    <w:start w:val="1"/>
    <w:numFmt w:val="bullet"/>
    <w:lvlText w:val="o"/>
    <w:lvlJc w:val="left"/>
    <w:pPr>
      <w:ind w:left="720"
             w:hanging="360"/>
    </w:pPr>
    <w:rPr>
      <w:rFonts w:ascii="Symbol"
                w:hAnsi="Symbol"
                w:hint="default"/>
    </w:rPr>
  </w:lvl>
	 */
	public Element createAbstractNumbering(ListParagraph def, Document doc) {
		Element numEl = doc.createElement("w:abstractNum");
		numEl.setAttribute("w:abstractNumId", Integer.toString(def.getAbstractNumId()));
		numEl.appendChild(createNumberingLevel(doc));
		return numEl;
	}
	
	private Element createNumberingLevel(Document numberingDoc) {
		Element levelEl = numberingDoc.createElement("w:lvl");
		levelEl.setAttribute("w:ilvl", "0");
		Element startEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:start"));
		startEl.setAttribute("w:val", "1");
		Element numFmtEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:numFmt"));
		numFmtEl.setAttribute("w:val", "bullet");
		Element lvlTextEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:lvlText"));
		lvlTextEl.setAttribute("w:val", Character.toString((char)0xB7));//bullet
		Element lvlJcEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:lvlJc"));
		lvlJcEl.setAttribute("w:val", "left");
		//pPr
		Element pPrEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:pPr"));
		Element indEl = (Element)pPrEl.appendChild(numberingDoc.createElement("w:ind"));
		indEl.setAttribute("w:left", "720");
		indEl.setAttribute("w:hanging", "360");
		//rPr
		Element rPrEl = (Element)levelEl.appendChild(numberingDoc.createElement("w:rPr"));
		Element rFontsEl = (Element)rPrEl.appendChild(numberingDoc.createElement("w:rFonts"));
		rFontsEl.setAttribute("w:ascii", "Symbol");
		rFontsEl.setAttribute("w:hAnsi", "Symbol");
		rFontsEl.setAttribute("w:hint", "default");
		return levelEl;
	}
	
	/*
  <w:num w:numId="1">
    <w:abstractNumId w:val="0"/>
  </w:num>
	 */
	public Element createNumbering(ListParagraph def, Document numberingDoc) {
		Element numEl = numberingDoc.createElement("w:num");
		numEl.setAttribute("w:numId", Integer.toString(def.getNumId()));
		Element abstractNumEl = (Element)numEl.appendChild(numberingDoc.createElement("w:abstractNumId"));
		abstractNumEl.setAttribute("w:val", Integer.toString(def.getAbstractNumId()));
		return numEl;
	}
	
/*
<w:shd w:val="solid" w:color="E9EAF2" w:fill="auto" />
 */
	private Element createShadow(String color, Node parent) {
		Element borderEl = (Element)parent.appendChild(document.createElement("w:shd"));
		borderEl.setAttribute("w:val", "solid");
		borderEl.setAttribute("w:fill", "auto");
		borderEl.setAttribute("w:color", color);
		return borderEl;
	}
	
/*
<w:top w:val="single" w:sz="6" w:space="0" w:color="E9EAF2" />
 */
	private Element createBorder(String name, String color, Node parent) {
		Element borderEl = (Element)parent.appendChild(document.createElement(name));
		borderEl.setAttribute("w:val", "single");
		borderEl.setAttribute("w:sz", "6");
		borderEl.setAttribute("w:space", "0");
		borderEl.setAttribute("w:color", color);
		return borderEl;
	}
	
	private Element createGridCol(Integer width, Node parent) {
		Element colEl = (Element)parent.appendChild(document.createElement("w:gridCol"));
		colEl.setAttribute("w:w", width.toString());
		return colEl;
	}
	
/*
<w:tblCellSpacing w:w="22" w:type="dxa" />
*/
	private Element createWidthEl(String name, Integer width, Unit unit, Node parent) {
		Element widthEl = (Element)parent.appendChild(document.createElement(name));
		if(unit == Unit.auto) {
			widthEl.setAttribute("w:w", "0");
			widthEl.setAttribute("w:type", "auto");
		} else {
			widthEl.setAttribute("w:w", width.toString());
			widthEl.setAttribute("w:type", unit.unit());
		}
		
		return widthEl;
	}
	
	public Element wrapInParagraph(Node el) {
		Element runEl = createRunEl(Collections.singletonList(el));
		return createParagraphEl(null, Collections.singletonList(runEl));
	}
	
	public List<Node> convertLaTeX(String latex) {
		List<Node> mathEls = new ArrayList<Node>();
		try {
			//convert latex -> mathml
			String mathml = ConvertFromLatexToMathML.convertToMathML(latex);

			//convert mathml to word docx
			ByteArrayOutputStream out = new ByteArrayOutputStream(20000);
			ConvertFromMathMLToWord.writeWordDocStreamFromMathML(out, mathml);
			ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
			out = null;
			
			//extract docx
			ZipInputStream zip = new ZipInputStream(in);
			ZipEntry entry = zip.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				if(name.endsWith("word/document.xml")) {
					
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					factory.setValidating(false);
					factory.setNamespaceAware(false);
					DocumentBuilder builder = factory.newDocumentBuilder();
					Document doc = builder.parse(new ShieldInputStream(zip));
					
					NodeList bodyList = doc.getElementsByTagName("w:body");
					if(bodyList.getLength() == 1) {
						Node body = bodyList.item(0);
						for(Node node=body.getFirstChild(); node!=null; node=node.getNextSibling()) {
							Node importedNode = document.importNode(node, true);
							mathEls.add(importedNode);
						}
					}
				}
				entry = zip.getNextEntry();
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return mathEls;
	}

	public Element createImageEl(String path) {
		if(mediaContainer == null) return null;
		
		VFSItem media = mediaContainer.resolve(path);
		if(media instanceof LocalFileImpl) {
			LocalFileImpl file = (LocalFileImpl)media;
			return createImageEl(file.getBasefile());
		}
		return null;
	}
	
/*
	<w:drawing>
		<wp:inline distT="0" distB="0" distL="0" distR="0"
			wp14:anchorId="152D4A51" wp14:editId="0588CC29">
			<wp:extent cx="2730500" cy="2730500" />
			<wp:effectExtent l="0" t="0" r="12700" b="12700" />
			<wp:docPr id="2" name="Bild 2" />
			<wp:cNvGraphicFramePr>
				<a:graphicFrameLocks
					xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
					noChangeAspect="1" />
			</wp:cNvGraphicFramePr>
			<a:graphic xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main">
				<a:graphicData
					uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
					<pic:pic
						xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
						<pic:nvPicPr>
							<pic:cNvPr id="0" name="aoi.jpg" />
							<pic:cNvPicPr />
						</pic:nvPicPr>
						<pic:blipFill>
							<a:blip r:embed="rId6">
								<a:extLst>
									<a:ext uri="{28A0092B-C50C-407E-A947-70E740481C1C}">
										<a14:useLocalDpi xmlns:a14="http://schemas.microsoft.com/office/drawing/2010/main" val="0" />
									</a:ext>
								</a:extLst>
							</a:blip>
							<a:stretch>
								<a:fillRect />
							</a:stretch>
						</pic:blipFill>
						<pic:spPr>
							<a:xfrm>
								<a:off x="0" y="0" />
								<a:ext cx="2730500" cy="2730500" />
							</a:xfrm>
							<a:prstGeom prst="rect">
								<a:avLst />
							</a:prstGeom>
							<a:noFill />
							<a:ln>
								<a:noFill />
							</a:ln>
						</pic:spPr>
					</pic:pic>
				</a:graphicData>
			</a:graphic>
		</wp:inline>
	</w:drawing>
 */
/**
 * <a:blip r:embed="rId6">
 * @param image
 * @return
 */
	public Element createImageEl(File image) {
		String id;
		Size emuSize;
		String filename;
		if(fileToImagesMap.containsKey(image)) {
			DocReference ref = fileToImagesMap.get(image);
			id = ref.getId();
			emuSize = ref.getEmuSize();
			filename = ref.getFilename();
		} else {
			id = generateId();
			Size size = ImageUtils.getImageSize(image);
			emuSize = OpenXMLUtils.convertPixelToEMUs(size, 72);
			filename = getUniqueFilename(image);
			fileToImagesMap.put(image, new DocReference(id, filename, emuSize, image));
		}

		Element drawingEl = document.createElement("w:drawing");
		Element inlineEl = (Element)drawingEl.appendChild(document.createElement("wp:inline"));
		inlineEl.setAttribute("distT", "0");
		inlineEl.setAttribute("distB", "0");
		inlineEl.setAttribute("distL", "0");
		inlineEl.setAttribute("distR", "0");
		//wp14:anchorId="152D4A51" wp14:editId="0588CC29"

		Element extentEl = (Element)inlineEl.appendChild(document.createElement("wp:extent"));
		extentEl.setAttribute("cx", Integer.toString(emuSize.getWidth()));
		extentEl.setAttribute("cy", Integer.toString(emuSize.getHeight()));
		Element effectExtentEl = (Element)inlineEl.appendChild(document.createElement("wp:effectExtent"));
		effectExtentEl.setAttribute("l", "0");
		effectExtentEl.setAttribute("t", "0");
		effectExtentEl.setAttribute("r", "12700");
		effectExtentEl.setAttribute("b", "0");
		Element docPrEl = (Element)inlineEl.appendChild(document.createElement("wp:docPr"));
		docPrEl.setAttribute("id", Integer.toString(currentId - 1));
		docPrEl.setAttribute("name", filename);
		
		Element cNvGraphicFramePrEl = (Element)inlineEl.appendChild(document.createElement("wp:cNvGraphicFramePr"));
		Element graphicFrameLocksEl = (Element)cNvGraphicFramePrEl.appendChild(document.createElement("a:graphicFrameLocks"));
		graphicFrameLocksEl.setAttribute("xmlns:a", "http://schemas.openxmlformats.org/drawingml/2006/main");
		graphicFrameLocksEl.setAttribute("noChangeAspect", "1");
		
		//big bloc graphic
		Element graphicEl = (Element)inlineEl.appendChild(document.createElement("a:graphic"));
		graphicEl.setAttribute("xmlns:a", "http://schemas.openxmlformats.org/drawingml/2006/main");
		Element graphicDataEl = (Element)graphicEl.appendChild(document.createElement("a:graphicData"));
		graphicDataEl.setAttribute("uri", "http://schemas.openxmlformats.org/drawingml/2006/picture");
		Element picEl = (Element)graphicDataEl.appendChild(document.createElement("pic:pic"));
		picEl.setAttribute("xmlns:pic", "http://schemas.openxmlformats.org/drawingml/2006/picture");
		
		//picture information
		Node nvPicPrEl = picEl.appendChild(document.createElement("pic:nvPicPr"));
		Element cNvPrEl = (Element)nvPicPrEl.appendChild(document.createElement("pic:cNvPr"));
		cNvPrEl.setAttribute("id", "0");
		cNvPrEl.setAttribute("name", filename);
		Node cNvPicPrEl = nvPicPrEl.appendChild(document.createElement("pic:cNvPicPr"));
		Element picLocksEl = (Element)cNvPicPrEl.appendChild(document.createElement("a:picLocks"));
		picLocksEl.setAttribute("noChangeAspect", "1");
		picLocksEl.setAttribute("noChangeArrowheads", "1");

		//picture blip
		Node blipFillEl = picEl.appendChild(document.createElement("pic:blipFill"));
		Element blipEl = (Element)blipFillEl.appendChild(document.createElement("a:blip"));
		blipEl.setAttribute("r:embed", id);
		
		//extLst
		Node extLstEl = blipEl.appendChild(document.createElement("a:extLst"));
		Element extEl = (Element)extLstEl.appendChild(document.createElement("a:ext"));
		extEl.setAttribute("uri", "{" + UUID.randomUUID().toString() + "}");
		Element useLocalDpiEl = (Element)extEl.appendChild(document.createElement("a14:useLocalDpi"));
		useLocalDpiEl.setAttribute("xmlns:a14", "http://schemas.microsoft.com/office/drawing/2010/main");
		useLocalDpiEl.setAttribute("val", "0");

		//srcRect
		blipFillEl.appendChild(document.createElement("a:srcRect"));
		//fill
		Node strechEl = blipFillEl.appendChild(document.createElement("a:stretch"));
		strechEl.appendChild(document.createElement("a:fillRect"));

		//pic -> spPr
		Element spPrEl = (Element)picEl.appendChild(document.createElement("pic:spPr"));
		spPrEl.setAttribute("bwMode", "auto");
		//pic -> spPr -> xfrm
		Node xfrmEl = spPrEl.appendChild(document.createElement("a:xfrm"));
		Element xfrmOffEl = (Element)xfrmEl.appendChild(document.createElement("a:off"));
		xfrmOffEl.setAttribute("x", "0");
		xfrmOffEl.setAttribute("y", "0");
		Element xfrmExtEl = (Element)xfrmEl.appendChild(document.createElement("a:ext"));
		xfrmExtEl.setAttribute("cx", Integer.toString(emuSize.getWidth()));
		xfrmExtEl.setAttribute("cy", Integer.toString(emuSize.getHeight()));
		//pic -> spPr -> prstGeom
		Element prstGeomEl = (Element)spPrEl.appendChild(document.createElement("a:prstGeom"));
		prstGeomEl.setAttribute("prst","rect");
		prstGeomEl.appendChild(document.createElement("a:avLst"));

		spPrEl.appendChild(document.createElement("a:noFill"));
		Node lnEl = spPrEl.appendChild(document.createElement("a:ln"));
		lnEl.appendChild(document.createElement("a:noFill"));

		
		return drawingEl;
	}
	
	private String getUniqueFilename(File image) {
		String filename = image.getName();
		if(imageFilenames.contains(filename)) {
			for(int i=1; i<1000; i++) {
				String nextFilename = i +"_" + filename;
				if(!imageFilenames.contains(nextFilename)) {
					filename = nextFilename;
					imageFilenames.add(filename);
					break;
				}
			}
		} else {
			imageFilenames.add(filename);
		}
		return filename;	
	}
	
	private String generateId() {
		return "rId" + (++currentId);
	}

	private final Element createRootElement(Document doc) {
		Element docEl = (Element)doc.appendChild(doc.createElement("w:document"));
		docEl.setAttribute("xmlns:wpc","http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas");
		docEl.setAttribute("xmlns:mo","http://schemas.microsoft.com/office/mac/office/2008/main");
		docEl.setAttribute("xmlns:mc","http://schemas.openxmlformats.org/markup-compatibility/2006");
		docEl.setAttribute("xmlns:mv","urn:schemas-microsoft-com:mac:vml");
		docEl.setAttribute("xmlns:o","urn:schemas-microsoft-com:office:office");
		docEl.setAttribute("xmlns:r","http://schemas.openxmlformats.org/officeDocument/2006/relationships");
		docEl.setAttribute("xmlns:m","http://schemas.openxmlformats.org/officeDocument/2006/math");
		docEl.setAttribute("xmlns:v","urn:schemas-microsoft-com:vml");
		docEl.setAttribute("xmlns:wp14","http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing");
		docEl.setAttribute("xmlns:wp","http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing");
		docEl.setAttribute("xmlns:w10","urn:schemas-microsoft-com:office:word");
		docEl.setAttribute("xmlns:w","http://schemas.openxmlformats.org/wordprocessingml/2006/main");
		docEl.setAttribute("xmlns:w14","http://schemas.microsoft.com/office/word/2010/wordml");
		docEl.setAttribute("xmlns:wpg","http://schemas.microsoft.com/office/word/2010/wordprocessingGroup");
		docEl.setAttribute("xmlns:wpi","http://schemas.microsoft.com/office/word/2010/wordprocessingInk");
		docEl.setAttribute("xmlns:wne","http://schemas.microsoft.com/office/word/2006/wordml");
		docEl.setAttribute("xmlns:wps","http://schemas.microsoft.com/office/word/2010/wordprocessingShape");
		docEl.setAttribute("mc:Ignorable","w14 wp14");
		return docEl;
	}
	
	private final Element createBodyElement(Element rootElement, Document doc) {
		Element bodyEl = (Element)rootElement.appendChild(doc.createElement("w:body"));
		return bodyEl;
	}
	
	public enum Style {
		underline,
		italic,
		bold,
		strike
	}
	
	public enum Unit {
		dxa("dxa"),
		pct("pct"),
		auto("auto");
		
		private final String unit;
		
		private Unit(String unit) {
			this.unit = unit;
		}
		
		public String unit() {
			return unit;
		}
	}
	
	public enum Heading {
		title("ooTitle"),
		heading1("ooHeading1"),
		heading2("ooHeading2");
		
		private final String styleId;
		
		private Heading(String styleId) {
			this.styleId = styleId;
		}

		public String styleId() {
			return styleId;
		}
	}
	
	public static class HeaderReference {
		
		private final String id;
		private final String header;
		
		public HeaderReference(String id, String header) {
			this.id = id;
			this.header = header;
		}
		
		public String getId() {
			return id;
		}
		
		public String getFilename() {
			return "header" + id + ".xml";
		}
		
		public String getHeader() {
			return header;
		}
	}
	
	public static class ListParagraph {
		
		private final int abstractNumId;
		private final int numId;
		
		public ListParagraph(int abstractNumId, int numId) {
			this.abstractNumId = abstractNumId;
			this.numId = numId;
		}
		
		public int getAbstractNumId() {
			return abstractNumId;
		}
		
		public int getNumId() {
			return numId;
		}
	}
}
