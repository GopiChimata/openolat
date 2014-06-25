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
package org.olat.selenium.page;

import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.Graphene;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.olat.selenium.page.repository.AuthoringEnvPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Page fragment which control the navigation bar with the static sites.
 * 
 * Initial date: 20.06.2014<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class NavigationPage {
	
	public static final By toolbarBackBy = By.cssSelector("li.o_breadcrumb_back>a");

	@Drone
	private WebDriver browser;
	
	@FindBy(css="ul.o_navbar_sites")
	private WebElement navigationSites;
	
	@FindBy(css = "li.o_site_author_env > a")
	private WebElement authoringEnvTab;
	@FindBy(css = "li.o_site_repository > a")
	private WebElement myCourseTab;
	
	public NavigationPage assertOnNavigationPage() {
		Assert.assertTrue(navigationSites.isDisplayed());
		return this;
	}
	
	public AuthoringEnvPage openAuthoringEnvironment() {
		if(!navigationSites.isDisplayed()) {
			//too small, open the black panel
		}
		//author?
		Assert.assertTrue(authoringEnvTab.isDisplayed());
		
		authoringEnvTab.click();
		OOGraphene.waitBusy();
		
		backToTheTop();
		OOGraphene.closeBlueMessageWindow(browser);
		
		WebElement main = browser.findElement(By.id("o_main"));
		return Graphene.createPageFragment(AuthoringEnvPage.class, main);
	}
	
	public void openCourse(String title) {
		By courseTab = By.xpath("//li/a[@title='" + title + "']");
		WebElement courseLink = browser.findElement(courseTab);
		
		courseLink.click();
		OOGraphene.waitBusy();
		OOGraphene.closeBlueMessageWindow(browser);
	}
	
	public NavigationPage backToTheTop() {
		List<WebElement> backList = browser.findElements(toolbarBackBy);
		
		int count = 0;
		while(backList.size() > 0) {
			backList.get(0).click();
			OOGraphene.waitBusy();
			backList = browser.findElements(toolbarBackBy);
			
			Assert.assertTrue(count++ < 3);
		}

		OOGraphene.closeBlueMessageWindow(browser);
		return this;
	}
}