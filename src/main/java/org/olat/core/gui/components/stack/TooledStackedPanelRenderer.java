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
package org.olat.core.gui.components.stack;

import java.util.ArrayList;
import java.util.List;

import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.DefaultComponentRenderer;
import org.olat.core.gui.components.dropdown.Dropdown;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.stack.TooledStackedPanel.Align;
import org.olat.core.gui.components.stack.TooledStackedPanel.Tool;
import org.olat.core.gui.render.RenderResult;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;

/**
 * 
 * Initial date: 25.03.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class TooledStackedPanelRenderer extends DefaultComponentRenderer {

	@Override
	public void render(Renderer renderer, StringOutput sb, Component source, URLBuilder ubu, Translator translator,
			RenderResult renderResult, String[] args) {
		TooledStackedPanel panel = (TooledStackedPanel) source;
		List<Link> breadCrumbs = panel.getBreadCrumbs();
		List<Tool> tools = panel.getTools();
		if(breadCrumbs.size() > 1 || tools.size() > 0) {
			String mainCssClass = panel.getCssClass();
			sb.append("<div id='o_main_toolbar' class='navbar navbar-default clearfix ").append(mainCssClass, mainCssClass != null).append("'>")
			  .append("<div class='o_breadcrumb container-fluid'>");

			Link backLink = panel.getBackLink();
			int numOfCrumbs = breadCrumbs.size();
			if(backLink.isVisible() && numOfCrumbs > 1) {
				sb.append("<ul class='nav navbar-nav navbar-left'>")
				  .append("<li><a href='#' class='dropdown-toggle' data-toggle='dropdown'>&#x25C4; <b class='caret'></b></a>")
				  .append("<ul class='dropdown-menu' role='menu'>")
				  /*.append("<li class='o_breadcrumb_back'>");
				backLink.getHTMLRendererSingleton().render(renderer, sb, backLink, ubu, translator, renderResult, args);
				sb.append("</li>")*/;
				
				for(int i=0; i<numOfCrumbs; i++) {
					sb.append("<li>");
					renderer.render(breadCrumbs.get(i), sb, args);
					sb.append("</li>");
				}
				sb.append("</ul></li></ul>");
			}
			
			List<Tool> notAlignedTools = getTools(tools, null);
			if(notAlignedTools.size() > 0) {
				sb.append("<ul class='nav navbar-nav'>");
				renderTools(notAlignedTools, renderer, sb, args);
				sb.append("</ul>");
			}
			
			List<Tool> leftTools = getTools(tools, Align.left);
			if(leftTools.size() > 0) {
				sb.append("<ul class='nav navbar-nav navbar-left'>");
				renderTools(leftTools, renderer, sb, args);
				sb.append("</ul>");
			}
			
			List<Tool> rightTools = getTools(tools, Align.right);
			if(rightTools.size() > 0) {
				sb.append("<ul class='nav navbar-nav navbar-right'>");
				renderTools(rightTools, renderer, sb, args);
				sb.append("</ul>");
			}

			sb.append("</div></div>");
		}
		
		Component toRender = panel.getContent();
		if(toRender != null) {
			renderer.render(sb, toRender, args);
		}
	}
	
	private List<Tool> getTools(List<Tool> tools, Align alignement) {
		List<Tool> alignedTools = new ArrayList<>(tools.size());
		if(alignement == null) {
			for(Tool tool:tools) {
				if(tool.getAlign() == null) {
					alignedTools.add(tool);
				}
			}
		} else {
			for(Tool tool:tools) {
				if(alignement.equals(tool.getAlign())) {
					alignedTools.add(tool);
				}
			}
		}
		return alignedTools;
	}
	
	private void renderTools(List<Tool> tools, Renderer renderer, StringOutput sb, String[] args) {
		int numOfTools = tools.size();
		for(int i=0; i<numOfTools; i++) {
			Tool tool = tools.get(i);
			Component cmp = tool.getComponent();
			String cssClass;
			if(cmp instanceof Dropdown) {
				cssClass = "dropdown";
			} else if(cmp instanceof Link && !cmp.isEnabled()) {
				cssClass = "navbar-text";
			} else {
				cssClass = "";
			}
			sb.append("<li class='o_tool ").append(cssClass).append("'>");
			renderer.render(cmp, sb, args);
			sb.append("</li>");
		}
	}
}