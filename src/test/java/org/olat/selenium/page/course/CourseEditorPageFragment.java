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
package org.olat.selenium.page.course;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.portfolio.PortfolioPage;
import org.olat.selenium.page.repository.AuthoringEnvPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CourseEditorPageFragment {
	
	public static final By editorBy = By.className("o_course_editor");
	public static final By createNodeButton = By.className("o_sel_course_editor_create_node");
	public static final By createNodeModalBy = By.id("o_course_editor_choose_nodetype");
	
	public static final By publishButtonBy = By.className("o_sel_course_editor_publish");

	public static final By toolbarBackBy = By.cssSelector("li.o_breadcrumb_back>a");
	
	public static final By navBarNodeConfiguration = By.cssSelector("ul.o_node_config>li>a");
	
	public static final By chooseCpButton = By.className("o_sel_cp_choose_repofile");
	public static final By chooseWikiButton = By.className("o_sel_wiki_choose_repofile");
	public static final By chooseTestButton = By.className("o_sel_test_choose_repofile");
	public static final By chooseFeedButton = By.className("o_sel_feed_choose_repofile");
	public static final By chooseScormButton = By.className("o_sel_scorm_choose_repofile");
	public static final By choosePortfolioButton = By.className("o_sel_map_choose_repofile");
	
	
	public static final List<By> chooseRepoEntriesButtonList = new ArrayList<>();
	static {
		chooseRepoEntriesButtonList.add(chooseCpButton);
		chooseRepoEntriesButtonList.add(chooseWikiButton);
		chooseRepoEntriesButtonList.add(chooseTestButton);
		chooseRepoEntriesButtonList.add(chooseFeedButton);
		chooseRepoEntriesButtonList.add(chooseScormButton);
		chooseRepoEntriesButtonList.add(choosePortfolioButton);
	}
	
	@Drone
	private WebDriver browser;
	
	@FindBy(className="o_course_editor")
	private WebElement editor;
	
	public static CourseEditorPageFragment getEditor(WebDriver browser) {
		OOGraphene.waitElement(editorBy, browser);
		WebElement main = browser.findElement(By.id("o_main"));
		return Graphene.createPageFragment(CourseEditorPageFragment.class, main);
	}
	
	public CourseEditorPageFragment assertOnEditor() {
		Assert.assertTrue(editor.isDisplayed());
		return this;
	}
	
	/**
	 * Select the root course element.
	 */
	public CourseEditorPageFragment selectRoot() {
		By rootNodeBy = By.cssSelector("span.o_tree_link.o_tree_l0>a");
		browser.findElement(rootNodeBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	/**
	 * Select the tab score in a structure node.
	 * 
	 */
	public CourseEditorPageFragment selectTabScore() {
		By scoreTabBy = By.cssSelector("fieldset.o_sel_structure_score");
		return selectTab(scoreTabBy);
	}
	
	private CourseEditorPageFragment selectTab(By tabBy) {
		List<WebElement> tabLinks = browser.findElements(navBarNodeConfiguration);

		boolean found = false;
		a_a:
		for(WebElement tabLink:tabLinks) {
			tabLink.click();
			OOGraphene.waitBusy(browser);
			List<WebElement> chooseRepoEntry = browser.findElements(tabBy);
			if(chooseRepoEntry.size() > 0) {
				found = true;
				break a_a;
			}
		}

		Assert.assertTrue("Found the tab", found);
		return this;
	}
	
	/**
	 * Enable passed and points by nodes
	 * @return
	 */
	public CourseEditorPageFragment enableRootScoreByNodes() {
		By enablePointBy = By.cssSelector("fieldset.o_sel_structure_score .o_sel_has_score input[type='checkbox']");
		browser.findElement(enablePointBy).click();
		OOGraphene.waitBusy(browser); //scform.scoreNodeIndents
		
		By enablePointNodesBy = By.cssSelector("fieldset.o_sel_structure_score input[type='checkbox'][name='scform.scoreNodeIndents']");
		List<WebElement> pointNodeEls = browser.findElements(enablePointNodesBy);
		for(WebElement pointNodeEl:pointNodeEls) {
			pointNodeEl.click();
			OOGraphene.waitBusy(browser);
		}
		
		By enablePassedBy = By.cssSelector("fieldset.o_sel_structure_score .o_sel_has_passed input[type='checkbox']");
		browser.findElement(enablePassedBy).click();
		OOGraphene.waitBusy(browser);
		
		By passedInheritBy = By.cssSelector("fieldset.o_sel_structure_score input[type='radio'][name='passedType'][value='inherit']");
		browser.findElement(passedInheritBy).click();
		OOGraphene.waitBusy(browser);
		
		By enablePassedNodesBy = By.cssSelector("fieldset.o_sel_structure_score input[type='checkbox'][name='scform.passedNodeIndents']");
		List<WebElement> enablePassedNodeEls = browser.findElements(enablePassedNodesBy);
		for(WebElement enablePassedNodeEl:enablePassedNodeEls) {
			enablePassedNodeEl.click();
			OOGraphene.waitBusy(browser);
		}
		
		//save
		By submitBy = By.cssSelector("fieldset.o_sel_structure_score button.btn.btn-primary");
		browser.findElement(submitBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	/**
	 * Create a new course element
	 * @param nodeAlias The type of the course element
	 * @return
	 */
	public CourseEditorPageFragment createNode(String nodeAlias) {
		WebElement createButton = browser.findElement(createNodeButton);
		Assert.assertTrue(createButton.isDisplayed());
		createButton.click();
		OOGraphene.waitElement(createNodeModalBy, browser);
		
		//modal
		WebElement createNodeModal = browser.findElement(createNodeModalBy);
		
		//create the node
		By node = By.className("o_sel_course_editor_node-" + nodeAlias);
		WebElement createNodeLink = createNodeModal.findElement(node);
		Assert.assertTrue(createNodeLink.isDisplayed());
		createNodeLink.click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	/**
	 * Set the course element title and short title
	 * 
	 * @param title
	 * @return
	 */
	public CourseEditorPageFragment nodeTitle(String title) {
		By shortTitle = By.cssSelector("div.o_sel_node_editor_shorttitle input");
		WebElement shortTitleEl = browser.findElement(shortTitle);
		shortTitleEl.clear();
		shortTitleEl.sendKeys(title);
		
		By longtitle = By.cssSelector("div.o_sel_node_editor_title input");
		WebElement titleEl = browser.findElement(longtitle);
		titleEl.clear();
		titleEl.sendKeys(title);
		
		By saveButton = By.cssSelector("button.o_sel_node_editor_submit");
		browser.findElement(saveButton).click();
		OOGraphene.waitBusy(browser);
		
		return this;
	}
	
	/**
	 * Loop the tabs of the course element configuration to find
	 * the one with a button to select a repository entry.
	 * 
	 * @return
	 */
	public CourseEditorPageFragment selectTabLearnContent() {
		List<WebElement> tabLinks = browser.findElements(navBarNodeConfiguration);

		boolean found = false;
		a_a:
		for(WebElement tabLink:tabLinks) {
			tabLink.click();
			OOGraphene.waitBusy(browser);
			for(By chooseRepoEntriesButton: chooseRepoEntriesButtonList) {
				List<WebElement> chooseRepoEntry = browser.findElements(chooseRepoEntriesButton);
				if(chooseRepoEntry.size() > 0) {
					found = true;
					break a_a;
				}
			}
		}

		Assert.assertTrue("Found the tab learn content", found);
		return this;
	}
	
	/**
	 * @see chooseResource
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment chooseCP(String resourceTitle) {
		return chooseResource(chooseCpButton, resourceTitle);
	}
	
	/**
	 * @see chooseResource
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment chooseWiki(String resourceTitle) {
		return chooseResource(chooseWikiButton, resourceTitle);
	}
	
	/**
	 * @see chooseResource
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment chooseTest(String resourceTitle) {
		return chooseResource(chooseTestButton, resourceTitle);
	}
	
	/**
	 * @see chooseResource
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment chooseScorm(String resourceTitle) {
		return chooseResource(chooseScormButton, resourceTitle);
	}
	
	/**
	 * Click the choose button, which open the resource chooser. Select
	 * the "My entries" segment, search the rows for the resource title,
	 * and select it.
	 * 
	 * 
	 * @param chooseButton The By of the choose button in the course node editor
	 * @param resourceTitle The resource title to find
	 * @return
	 */
	public CourseEditorPageFragment chooseResource(By chooseButton, String resourceTitle) {
		browser.findElement(chooseButton).click();
		OOGraphene.waitBusy(browser);
		//popup
		WebElement popup = browser.findElement(By.className("o_sel_search_referenceable_entries"));
		popup.findElement(By.cssSelector("a.o_sel_repo_popup_my_resources")).click();
		OOGraphene.waitBusy(browser);
		
		//find the row
		WebElement selectRow = null;
		List<WebElement> rows = popup.findElements(By.cssSelector("div.o_segments_content table.o_table tr"));
		for(WebElement row:rows) {
			String text = row.getText();
			if(text.contains(resourceTitle)) {
				selectRow = row;
				break;
			}
		}
		Assert.assertNotNull(selectRow);
		
		//find the select in the row
		WebElement selectLink = selectRow.findElement(By.xpath("//a[contains(@href,'rtbSelectLink')]"));
		selectLink.click();
		OOGraphene.waitBusy(browser);
		
		//double check that the resource is selected (search the preview link)
		By previewLink = By.xpath("//a/span[text()[contains(.,'" + resourceTitle + "')]]");
		browser.findElement(previewLink);

		return this;
	}
	
	/**
	 * Create a wiki from the chooser popup
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment createWiki(String resourceTitle) {
		return createResource(chooseWikiButton, resourceTitle);
	}
	
	/**
	 * Create a QTI 1.2 test from the chooser popup
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment createQTI12Test(String  resourceTitle) {
		return createResource(chooseTestButton, resourceTitle);
	}
	
	/**
	 * Create a podcast or a blog
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment createFeed(String resourceTitle) {
		return createResource(chooseFeedButton, resourceTitle);
	}
	
	/**
	 * Create a portfolio template
	 * @param resourceTitle
	 * @return
	 */
	public CourseEditorPageFragment createPortfolio(String resourceTitle) {
		return createResource(choosePortfolioButton, resourceTitle);
	}
	
	/**
	 * Edit the map in the course element learn content tab.
	 * @return
	 */
	public PortfolioPage editPortfolio() {
		By editBy = By.className("o_sel_edit_map");
		WebElement editLink = browser.findElement(editBy);
		editLink.click();
		OOGraphene.waitBusy(browser);
		
		WebElement main = browser.findElement(By.id("o_main_wrapper"));
		return Graphene.createPageFragment(PortfolioPage.class, main);
	}
	
	private CourseEditorPageFragment createResource(By chooseButton, String resourceTitle) {
		OOGraphene.closeBlueMessageWindow(browser);
		
		browser.findElement(chooseButton).click();
		OOGraphene.waitBusy(browser);
		//popup
		WebElement popup = browser.findElement(By.className("o_sel_search_referenceable_entries"));
		popup.findElement(By.cssSelector("a.o_sel_repo_popup_my_resources")).click();
		OOGraphene.waitBusy(browser);
		
		//click create
		popup.findElement(By.className("o_sel_repo_popup_create_resource")).click();
		OOGraphene.waitBusy(browser);

		//fill the create form
		return fillCreateForm(resourceTitle);
	}
	
	private CourseEditorPageFragment fillCreateForm(String displayName) {
		WebElement modal = browser.findElement(By.cssSelector("div.modal.o_sel_author_create_popup"));
		modal.findElement(AuthoringEnvPage.displayNameInput).sendKeys(displayName);
		modal.findElement(AuthoringEnvPage.createSubmit).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	/**
	 * Don't forget to set access
	 * 
	 * @return
	 */
	public CoursePageFragment autoPublish() {
		//back
		By breadcrumpBackBy = By.cssSelector("#o_main_toolbar li.o_breadcrumb_back a");
		browser.findElement(breadcrumpBackBy).click();
		OOGraphene.waitBusy(browser);
		
		//auto publish
		By autoPublishBy = By.cssSelector("div.modal  a.o_sel_course_quickpublish_auto");
		browser.findElement(autoPublishBy).click();
		OOGraphene.waitBusy(browser);
		
		return new CoursePageFragment(browser);
	}

	/**
	 * Open the publish process
	 * @return
	 */
	public PublisherPageFragment publish() {
		WebElement publishButton = browser.findElement(publishButtonBy);
		Assert.assertTrue(publishButton.isDisplayed());
		publishButton.click();
		
		By modalBy = By.className("modal");
		OOGraphene.waitElement(modalBy, browser);
		WebElement modal = browser.findElement(By.className("modal"));
		return Graphene.createPageFragment(PublisherPageFragment.class, modal);
	}
	
	/**
	 * Click the back button
	 * 
	 * @return
	 */
	public CoursePageFragment clickToolbarBack() {
		browser.findElement(toolbarBackBy).click();
		OOGraphene.waitBusy(browser);
		OOGraphene.closeBlueMessageWindow(browser);
		
		WebElement main = browser.findElement(By.id("o_main"));
		return Graphene.createPageFragment(CoursePageFragment.class, main);
	}
}
