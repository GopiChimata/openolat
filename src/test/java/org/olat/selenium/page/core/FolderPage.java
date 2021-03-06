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
package org.olat.selenium.page.core;

import java.util.List;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assert;
import org.olat.selenium.page.graphene.OOGraphene;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * 
 * Drive the folder component of contact
 * 
 * Initial date: 27.02.2015<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class FolderPage {
	
	@Drone
	private WebDriver browser;
	
	public FolderPage() {
		//
	}
	
	public FolderPage(WebDriver browser) {
		this.browser = browser;
	}
	
	public FolderPage assertOnFolderCmp() {
		By folderCmpBy = By.className("o_briefcase_foldercomp");
		List<WebElement> folderCmpEl = browser.findElements(folderCmpBy);
		Assert.assertFalse(folderCmpEl.isEmpty());
		return this;
	}
	
	public FolderPage createDirectory(String name) {
		By newFolderBy = By.className("b_bc_newfolder");
		browser.findElement(newFolderBy).click();
		OOGraphene.waitBusy(browser);
		
		By folderNameBy = By.cssSelector(".o_sel_folder_new_folder_name input[type='text']");
		OOGraphene.waitElement(folderNameBy, browser);
		browser.findElement(folderNameBy).sendKeys(name);
		
		By createBy = By.cssSelector(".o_sel_folder_new_folder button.btn-primary");
		browser.findElement(createBy).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public FolderPage assertOnDirectory(String name) {
		String escapedName = name.replace(" ", "%20");
		By directoryBy = By.xpath("//table[contains(@class,'o_bc_table')]//a[contains(@href,'" + escapedName + "')]");
		List<WebElement> directoryEls = browser.findElements(directoryBy);
		Assert.assertFalse(directoryEls.isEmpty());
		return this;
	}
	
	public FolderPage createHTMLFile(String name, String content) {
		By newFileBy = By.className("b_bc_newfile");
		browser.findElement(newFileBy).click();
		OOGraphene.waitBusy(browser);
		
		By filenameBy = By.cssSelector(".o_sel_folder_new_file_name input[type='text']");
		OOGraphene.waitElement(filenameBy, browser);
		browser.findElement(filenameBy).sendKeys(name);
		
		By createBy = By.cssSelector(".o_sel_folder_new_file button.btn-primary");
		browser.findElement(createBy).click();
		OOGraphene.waitBusy(browser);
		
		OOGraphene.tinymce(content, browser);
		
		By saveAndCloseButton = By.cssSelector("#o_button_saveclose a.btn");
		browser.findElement(saveAndCloseButton).click();
		OOGraphene.waitBusy(browser);
		return this;
	}
	
	public FolderPage assertOnFile(String filename) {
		By fileBy = By.xpath("//table[contains(@class,'o_bc_table')]//a[contains(text(),'" + filename + "')]");
		List<WebElement> fileEls = browser.findElements(fileBy);
		Assert.assertFalse(fileEls.isEmpty());
		return this;
	}
}