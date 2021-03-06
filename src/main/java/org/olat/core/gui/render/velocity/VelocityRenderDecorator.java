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

package org.olat.core.gui.render.velocity;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.olat.core.commons.contextHelp.ContextHelpModule;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.winmgr.AJAXFlags;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.StringOutputPool;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.gui.util.CSSHelper;
import org.olat.core.helpers.Settings;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;
import org.olat.core.util.filter.impl.OWASPAntiSamyXSSFilter;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.login.LoginAuthprovidersController;

/**
 * @author Felix Jost
 */
public class VelocityRenderDecorator implements Closeable {
	private static final OLog log = Tracing.createLoggerFor(VelocityRenderDecorator.class);
	
	public static final String PARAM_CHELP_BUNDLE = "chelpbundle";
	private VelocityContainer vc;
	private Renderer renderer;
	private final boolean isIframePostEnabled;
	private StringOutput target;

	/**
	 * @param renderer
	 * @param vc
	 */
	public VelocityRenderDecorator(Renderer renderer, VelocityContainer vc, StringOutput target) {
		this.renderer = renderer;
		this.vc = vc;
		this.target = target;
		this.isIframePostEnabled = renderer.getGlobalSettings().getAjaxFlags().isIframePostEnabled();
	}

	@Override
	public void close() throws IOException {
		vc = null;
		target = null;
		renderer = null;
	}

	/**
	 * 
	 * @param prefix e.g. abc for "abc647547326" and so on
	 * @return an prefixed id (e.g. f23748932) which is unique in the current render tree.
	 * 
	 */
	public StringOutput getId(String prefix) {
		StringOutput sb = new StringOutput(16);
		sb.append("o_s").append(prefix).append(vc.getDispatchID());
		return sb;
	}
	
	public static String getId(String prefix, VelocityContainer vc) {
		StringOutput sb = StringOutputPool.allocStringBuilder(24);
		sb.append("o_s").append(prefix).append(vc.getDispatchID());
		return StringOutputPool.freePop(sb);
	}
	
	public String getUniqueId() {
		return Long.toString(CodeHelper.getRAMUniqueID());
	}

	/**
	 * 
	 * @return the componentid (e.g.) o_c32645732
	 */
	public StringOutput getCId() {
		StringOutput sb = new StringOutput(16);
		sb.append("o_c").append(vc.getDispatchID());
		return sb;
	}
	
	public String getUuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	/**
	 * 
	 * @param command
	 * @return e.g. /olat/auth/1:2:3:cid:com/
	 */
	public StringOutput commandURIbg(String command) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { command }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		return sb;
	}

	/**
	 * renderer a target="oaa" if ajax-mode is on, otherwise returns an empty string
	 * @return
	 */
	public StringOutput bgTarget() {
		StringOutput sb = new StringOutput(16);
		if (isIframePostEnabled) {
			renderer.getUrlBuilder().appendTarget(sb);
		}
		return sb;
	}
	
	/**
	 * FIXME:fj:b search occurences for $r.commandURI and try to replace them with $r.link(...) or such
	 * @param command
	 * @return
	 */
	public StringOutput commandURI(String command) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { command });
		return sb;
	}

	/**
	 * Creates a java script fragment to execute a background request. In ajax
	 * mode the request uses the ajax asynchronous methods, in legacy mode it
	 * uses a standard document.location.request
	 * 
	 * @param command
	 * @param paramKey
	 * @param paramValue
	 * @return
	 */
	public StringOutput javaScriptBgCommand(String command, String paramKey, String paramValue) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildJavaScriptBgCommand(sb, new String[] { VelocityContainer.COMMAND_ID, paramKey }, new String[] { command, paramValue }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		return sb;
	}

	/**
	 * Creates a java script fragment to execute a background request. In ajax
	 * mode the request uses the ajax asynchronous methods, in legacy mode it
	 * uses a standard document.location.request
	 * 
	 * @param command
	 * @return
	 */
	public StringOutput javaScriptBgCommand(String command) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildJavaScriptBgCommand(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { command}, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		return sb;
	}	
	
	/**
	 * Use it to create the action for a handmade form in a velocity template,
	 * e.g. '<form method="post" action="$r.formURIgb("viewswitch")">'
	 * @param command
	 * @return
	 */
	public StringOutput formURIbg(String command) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { command }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		return sb;
	}
	
	/**
	 * Use it to create the forced non-ajax action for a handmade form in a velocity template,
	 * e.g. '<form method="post" action="$r.formURIgb("viewswitch")">'
	 * @param command
	 * @return
	 */
	public StringOutput formURI(String command) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { command });
		return sb;
	}
	
	/**
	 * 
	 * @param command
	 * @return
	 */
	public StringOutput commandURI(String command, String paramKey, String paramValue) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID, paramKey }, new String[] { command, paramValue });
		return sb;
	}

	/**
	 * 
	 * @param command
	 * @return
	 */
	public StringOutput commandURIbg(String command, String paramKey, String paramValue) {
		StringOutput sb = new StringOutput(100);
		renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID, paramKey }, new String[] { command, paramValue }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);
		return sb;
	}
	
	/**
	 * should be called within the main .html template after the <head>tag. gets
	 * some js/css/onLoad code from the component to render/work correctly.
	 * 
	 * @return
	 */
	public StringOutput renderBodyOnLoadJSFunctionCall() {
		StringOutput sb = new StringOutput(100);
		renderer.renderBodyOnLoadJSFunctionCall(sb, vc);
		return sb;
	}

	/**
	 * should be called within the main .html template after the <head>tag. gets
	 * some js/css/onLoad code from the component to render/work correctly.
	 * 
	 * @return
	 */
	public StringOutput renderHeaderIncludes() {
		StringOutput sb = new StringOutput(100);
		renderer.renderHeaderIncludes(sb, vc);
		return sb;
	}

	/**
	 * Note: use only rarely - e.g. for static redirects to login screen or to a
	 * special dispatcher. Renders a uri which is mounted to the webapp/ directory
	 * of your webapplication.
	 * <p>
	 * For static references (e.g. images which cannot be delivered using css):
	 * use renderStaticURI instead!
	 */
	public StringOutput relLink(String URI) {
		StringOutput sb = new StringOutput(100);
		Renderer.renderNormalURI(sb, URI);
		return sb;
	}

	/**
	 * 
	 * e.g. "images/somethingicannotdowithcss.jpg" -> /olat/raw/61x/images/somethingicannotdowithcss.jpg"
	 * with /olat/raw/61x/ mounted to webapp/static directory of your webapp
	 * 
	 * @param URI
	 * @return
	 */
	public StringOutput staticLink(String URI) {
		StringOutput sb = new StringOutput(100);
		Renderer.renderStaticURI(sb, URI);
		return sb;
	}

	
	/**
	 * e.g. "/olat/"
	 * @return
	 */
	public String relWinLink() {
		return renderer.getUriPrefix();
	}

	/**
	 * @param componentName
	 * @return
	 */
	public StringOutput render(String componentName) {
		return doRender(componentName, null);
	}

	/**
	 * used to position help icon inside div-class o_chelp_wrapper
	 * @param packageName
	 * @param pageName
	 * @param hoverTextKey
	 * @return
	 */
	public StringOutput contextHelpWithWrapper(String packageName, String pageName, String hoverTextKey) {
		StringOutput sb = new StringOutput(100);
		if (ContextHelpModule.isContextHelpEnabled()) {
			sb.append("<span class=\"o_chelp_wrapper\">");
			sb.append(contextHelp(packageName, pageName, hoverTextKey));
			sb.append("</span>");
		}
		return sb;
	}

	/**
	 * @param packageName
	 * @param pageName
	 * @param hoverTextKey
	 * @return
	 */
	public StringOutput contextHelp(String packageName, String pageName, String hoverTextKey) {
		StringOutput sb = new StringOutput(100);
		if (ContextHelpModule.isContextHelpEnabled()) {
			String hooverText = renderer.getTranslator().translate(hoverTextKey);
			if (hooverText != null) {
				hooverText = StringEscapeUtils.escapeHtml(hooverText);
			}
			sb.append("<a href=\"javascript:");
			sb.append(contextHelpJSCommand(packageName, pageName));
			sb.append("\" title=\"").append(hooverText).append("\" class=\"o_chelp\"><i class='o_icon o_icon_help'></i> ");
			sb.append(renderer.getTranslator().translate("help"));
			sb.append("</a>");
		}
		return sb;
	}

	/**
	 * Create a js command to open a specific context help page
	 * @param packageName
	 * @param pageName
	 * @return
	 */
	public StringOutput contextHelpJSCommand(String packageName, String pageName) {
		StringOutput sb = new StringOutput(100);
		if (ContextHelpModule.isContextHelpEnabled()) {
			String langCode = renderer.getTranslator().getLocale().toString();
			sb.append("contextHelpWindow('");
			Renderer.renderNormalURI(sb, "help/");
			sb.append(langCode).append("/").append(packageName).append("/").append(pageName);
			sb.append("')");
		}
		return sb;
	}
	
	
	
	/**
	 * Create a link that can be used within a context help page to link to
	 * another context help page from the same package.
	 * 
	 * @param pageName e.g. "my-page.html"
	 * @return
	 */
	public StringOutput contextHelpRelativeLink(String pageName) {
		return contextHelpRelativeLink(null, pageName);
	}

	/**
	 * Create a link that can be used within a context help page to link to
	 * another context help page from another package. As link text the page title
	 * is used.
	 * 
	 * @param bundleName e.g. "org.olat.core"
	 * @param pageName e.g. "my-page.html"
	 * @return
	 */
	public StringOutput contextHelpRelativeLink(String bundleName, String pageName) {
		String linkText;
		int lastDotPos = pageName.lastIndexOf(".");
		if (lastDotPos != -1) {
			Translator pageTrans = renderer.getTranslator();
			if (bundleName != null) {
				Locale locale = pageTrans.getLocale();
				pageTrans = new PackageTranslator(bundleName, locale);
			}
			linkText = pageTrans.translate("chelp." + pageName.subSequence(0, lastDotPos) + ".title");					
		} else {
			linkText = pageName; // fallback
		}
		return contextHelpRelativeLink(bundleName, pageName, linkText);
	}

	/**
	 * Create a link that can be used within a context help page to link to
	 * another context help page from another package. The link text can be
	 * specified as a thirt attribute.
	 * 
	 * @param bundleName e.g. "org.olat.core"
	 * @param pageName e.g. "my-page.html"
	 * @return
	 */
	public StringOutput contextHelpRelativeLink(String bundleName, String pageName, String linkText) {
		StringOutput sb = new StringOutput(100);
		if (ContextHelpModule.isContextHelpEnabled()) {
			sb.append("<a href=\"");
			if (bundleName == null) {
				renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID }, new String[] { pageName }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);				
			} else {
				renderer.getUrlBuilder().buildURI(sb, new String[] { VelocityContainer.COMMAND_ID, PARAM_CHELP_BUNDLE }, new String[] { pageName, bundleName }, isIframePostEnabled? AJAXFlags.MODE_TOBGIFRAME : AJAXFlags.MODE_NORMAL);				
			}
			sb.append("\" ");
			if(isIframePostEnabled) {
				renderer.getUrlBuilder().appendTarget(sb);
			}
			sb.append(">");
			sb.append(linkText);
			sb.append("</a>");
		}
		return sb;
	}	

	
	/**
	 * @param componentName
	 * @param arg1
	 * @return
	 */
	public StringOutput render(String componentName, String arg1) {
		return doRender(componentName, new String[] { arg1 });
	}

	/**
	 * @param componentName
	 * @param arg1
	 * @param arg2
	 * @return
	 */
	public StringOutput render(String componentName, String arg1, String arg2) {
		return doRender(componentName, new String[] { arg1, arg2 });
	}

	/**
	 * Parametrised translator
	 * 
	 * @param key The translation key
	 * @param arg1 The argument list as a string array
	 * @return
	 */
	public String translate(String key, String[] arg1) {
		Translator trans = renderer.getTranslator();
		if (trans == null) return "{Translator is null: key_to_translate=" + key + "}";
		String res = trans.translate(key, arg1);
		if (res == null) return "?? key not found to translate: key_to_translate=" + key + " / trans info:" + trans + "}";
		return res;
	}

	/**
	 * Wrapper to make it possible to use the parametrised translator from within
	 * the velocity container
	 * 
	 * @param key The translation key
	 * @param arg1 The argument list as a list of strings
	 * @return the translated string
	 */
	public String translate(String key, List<String> arg1) {
		return translate(key, arg1.toArray(new String[arg1.size()]));
	}
	
	/**
	 * Wrapper to make it possible to use the parametrised translator from within
	 * the velocity container
	 * 
	 * @param key The translation key
	 * @param arg1 The argument sd string
	 * @return the translated string
	 */
	public String translate(String key, String arg1) {
		return translate(key, new String[] {arg1});
	}

	/**
	 * Method to translate a key that comes from another package. This should be
	 * used rarely. When a key is used withing multiple packages is is usually
	 * better to use a fallback translator or to move the key to the default
	 * packages.
	 * <p>
	 * Used in context help system
	 * @param bundleName the package name, e.g. 'org.olat.core'
	 * @param key the key, e.g. 'my.key'
	 * @param args optional arguments, null if not used
	 * @return
	 */
	public String translateWithPackage(String bundleName, String key, String[] args) {
		Translator pageTrans = renderer.getTranslator();
		if (pageTrans == null) return "{Translator is null: key_to_translate=" + key + "}";
		Locale locale = pageTrans.getLocale();
		Translator tempTrans = new PackageTranslator(bundleName, locale);
		String result = tempTrans.translate(key, args);
		if (result == null) {
			return "{Invalid bundle name: " + bundleName + " and key: " + key + "}";
		}
		return result;
	}

	/**
	 * Method to translate a key that comes from another package. This should be
	 * used rarely. When a key is used withing multiple packages is is usually
	 * better to use a fallback translator or to move the key to the default
	 * packages.
	 * @param bundleName the package name, e.g. 'org.olat.core'
	 * @param key the key, e.g. 'my.key'
	 * @return
	 */
	public String translateWithPackage(String bundleName, String key) {
		return translateWithPackage(bundleName, key, null);
	}


	/**escapes " entities in \"
	 * @param in the string to convert
	 * @deprecated please use escapeHtml.
	 * @return the escaped string
	 */
	public String escapeDoubleQuotes(String in) {
	    return Formatter.escapeDoubleQuotes(in).toString();
	}
	
	/**
	 * Escapes the characters in a String for JavaScript use.
	 */
	public String escapeJavaScript(String str) {
		return StringHelper.escapeJavaScript(str);
	}
	
	/**
	 * Escapes the characters in a String using HTML entities.
	 * @param str
	 * @return
	 */
	public String escapeHtml(String str) throws IOException {
		if(str == null) {
			return "";
		}
		return StringHelper.escapeHtml(str);
	}
	
	public String xssScan(String str) {
		if(str == null) {
			return "";
		}
		OWASPAntiSamyXSSFilter filter = new OWASPAntiSamyXSSFilter();
		return filter.filter(str);
	}
	
	public String filterHtml(String str) {
		if(str == null) {
			return "";
		}
		return FilterFactory.getHtmlTagsFilter().filter(str);
	}
	
	
	/**
	 * @param key
	 * @return
	 */
	public String translate(String key) {
		Translator trans = renderer.getTranslator();
		if (trans == null) return "{Translator is null: key_to_translate=" + key + "}";
		String res = trans.translate(key);
		if (res == null) return "?? key not found to translate: key_to_translate=" + key + " / trans info:" + trans + "}";
		return res;
	}
	
	/**
	 * Translates and escapesHTML. 
	 * It assumes that the HTML attribute value should be enclosed in double quotes.
	 * @param key
	 * @return
	 */
	public String translateInAttribute(String key) {
		return StringEscapeUtils.escapeHtml(translate(key));
	}

	/** 
	 * @return current language code as found in (current)Locale.toString() method
	 */
	public String getLanguageCode() {
		Locale currentLocale = I18nManager.getInstance().getCurrentThreadLocale();
		return currentLocale.toString();
	}

	/**
	 * 
	 * renders the component.
	 * if the component cannot be found, there is no error, but an empty String is returned. Not recommended to use normally, but rather use @see render(String componentName)
	 * 
	 * @param componentName
	 * @return
	 */
	public StringOutput renderForce(String componentName) {
		Component source = renderer.findComponent(componentName);
		StringOutput sb;
		if (source == null) {
			sb = new StringOutput(1);
		} else if (target == null) {
			sb = new StringOutput(10000);
			renderer.render(source, sb, null);
		} else {
			renderer.render(source, target, null);
		}
		return new StringOutput(1);
	}
	
	private StringOutput doRender(String componentName, String[] args) {
		Component source = renderer.findComponent(componentName);
		StringOutput sb;
		if (source == null) {
			sb = new StringOutput(128);
			sb.append(">>>>>>>>>>>>>>>>>>>>>>>>>> component " + componentName + " could not be found to be rendered!");
		} else if (target == null) {
			sb = new StringOutput(10000);
			renderer.render(source, sb, args);
		} else {
			sb = new StringOutput(1);
			renderer.render(source, target, args);
		}
		return sb;
	}
	
	/**
	 * @param componentName
	 * @return true if the component with name componentName is a child of the current container. Used to "if" the render 
	 * instruction "$r.render(componentName)" if it is not known beforehand whether the component is there or not.
	 */
	public boolean available(String componentName) {
		Component source = renderer.findComponent(componentName);
		return (source != null);
	}
	
	/**
	 * @param componentName
	 * @return true if the component with name componentName is a child of the current container and if this
	 * component is visible
	 */
	public boolean visible(String componentName) {
		Component source = renderer.findComponent(componentName);
		return (source != null && source.isVisible());
	}
	
	/**
	 * Return the component
	 * @param componentName
	 * @return
	 */
	public Component getComponent(String componentName) {
		Component source = renderer.findComponent(componentName);
		return source;
	}

	/**
	 * returns an object from the context of velocity
	 * 
	 * @param key
	 * @return
	 */
	public Object get(String key) {
		return vc.getContext().get(key);
	}
	
	public boolean absent(String key) {
		return !vc.getContext().containsKey(key);
	}
	
	public String formatDate(Date date){
		Formatter f = Formatter.getInstance(renderer.getTranslator().getLocale());
		return f.formatDate(date);
	}
	
	public String formatDateAndTime(Date date){
		Formatter f = Formatter.getInstance(renderer.getTranslator().getLocale());
		return f.formatDateAndTime(date);
	}

	public String formatBytes(long bytes) {
		return Formatter.formatBytes(bytes);
	}
	

	/**
	 * Wrapp given html code with a wrapper an add code to transform latex
	 * formulas to nice visual characters on the client side. The latex formulas
	 * must be within an HTML element that has the class 'math' attached.
	 * 
	 * @param htmlFragment A html element that might contain an element that has a
	 *          class 'math' with latex formulas
	 * @return
	 */
	public static String formatLatexFormulas(String htmlFragment) {
		return Formatter.formatLatexFormulas(htmlFragment);
	}
	
	/**
	 * Search in given text fragment for URL's and surround them with clickable
	 * HTML link objects.
	 * 
	 * @param textFragment
	 * @return text with clickable links
	 */
	public static String formatURLsAsLinks(String textFragment) {
		return Formatter.formatURLsAsLinks(textFragment);
	}	
	
 	/**
	 * Strips all HTML tags from the source string.
	 * 
	 * @param source
	 *            Source
	 * @return Source without HTML tags.
	 */
	public static String filterHTMLTags(String source) {
		Filter htmlTagsFilter = FilterFactory.getHtmlTagsFilter();
		return htmlTagsFilter.filter(source);
	}
	
	/**
	 * Get the icon css class that represents the filetype based on the file name
	 * @param filename 
	 * @return The css class for the file or a default css class
	 */
	public static String getFiletypeIconCss(String filename) {
		return CSSHelper.createFiletypeIconCssClassFor(filename);
	}
	
	/**
	 * Returns true when debug mode is configured, false otherwhise
	 * @return
	 */
	public boolean isDebuging() {
		return Settings.isDebuging();
	}
	
	/**
	 * To inject licenses (the NOTICE.TXT) in the help
	 * @return
	 */
	public String getLicences() {
		String licenses = "Not found";
		InputStream licensesStream = LoginAuthprovidersController.class.getResourceAsStream("../../../NOTICE.TXT");
		if(licensesStream != null) {
			try {
				licenses = IOUtils.toString(licensesStream);
			} catch (IOException e) {
				log.error("", e);
			} finally {
				IOUtils.closeQuietly(licensesStream);
			}
		}
		return licenses;
	}
	
	public String getVersion() {
		return Settings.getVersion();
	}
	
	public Languages getLanguages() {
		I18nManager i18nMgr = I18nManager.getInstance();
		Collection<String> enabledKeysSet = I18nModule.getEnabledLanguageKeys();
		Map<String, String> langNames = new HashMap<String, String>();
		Map<String, String> langTranslators = new HashMap<String, String>();
		String[] enabledKeys = ArrayHelper.toArray(enabledKeysSet);
		String[] names = new String[enabledKeys.length];
		for (int i = 0; i < enabledKeys.length; i++) {
			String key = enabledKeys[i];
			String langName = i18nMgr.getLanguageInEnglish(key, I18nModule.isOverlayEnabled());
			langNames.put(key, langName);
			names[i] = langName;
			String author = i18nMgr.getLanguageAuthor(key);
			langTranslators.put(key, author);
		}
		ArrayHelper.sort(enabledKeys, names, true, true, true);
		return new Languages(enabledKeys, langNames, langTranslators);
	}
	
	public static class Languages {
		private final String[] enabledKeys;
		private final Map<String, String> langNames;
		private final Map<String, String> langTranslators;
		
		public Languages(String[] enabledKeys, Map<String, String> langNames, Map<String, String> langTranslators) {
			this.enabledKeys = enabledKeys;
			this.langNames = langNames;
			this.langTranslators = langTranslators;
		}

		public String[] getEnabledKeys() {
			return enabledKeys;
		}

		public Map<String, String> getLangNames() {
			return langNames;
		}

		public Map<String, String> getLangTranslators() {
			return langTranslators;
		}
	}
}