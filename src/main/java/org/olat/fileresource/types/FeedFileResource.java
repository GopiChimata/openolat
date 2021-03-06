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
package org.olat.fileresource.types;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.PathUtils;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.fileresource.FileResourceManager;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.models.Feed;
import org.olat.modules.webFeed.models.Item;

/**
 * Abstract feed file resource class. Used to decrease redundancy.
 * 
 * <P>
 * Initial Date: Aug 3, 2009 <br>
 * 
 * @author gwassmann
 */
public abstract class FeedFileResource extends FileResource {
	
	private static final OLog log = Tracing.createLoggerFor(FeedFileResource.class);

	public FeedFileResource(String type) {
		super(type);
	}

	public FeedFileResource(File root, File resourceFolder, String type) {
		super(type);
		// After unziping the uploaded folder, I would like to copy it to the
		// appropriate location right away (and not on the next read). So, I put the
		// code here. Note that this constructor is also called on copying a
		// resource. We know that the resource folder is valid.

		// Let's now copy the resource folder to the root folder.
		VFSContainer rootContainer = new LocalFolderImpl(root);
		String folderName = FeedManager.getInstance().getFeedKind(this);
		if (rootContainer.resolve(folderName) == null) {
			// If the podcast directory doesn't exist yet, create it and copy content
			// from uploaded folder
			rootContainer = rootContainer.createChildContainer(folderName);
			VFSContainer resourceContainer = new LocalFolderImpl(resourceFolder);
			for (VFSItem item : resourceContainer.getItems()) {
				rootContainer.copyFrom(item);
				// Delete the item if it is located in the _unzipped_ dir.
				// Remember that the resource folder could be a valid folder of a
				// different resource (when copying the resource).
				if (resourceContainer.getName().equals(FileResourceManager.ZIPDIR)) {
					item.delete();
				}
			}
		}
	}
	
	public static ResourceEvaluation evaluate(File file, String filename, String type) {
		ResourceEvaluation eval = new ResourceEvaluation();
		try {
			IndexFileFilter visitor = new IndexFileFilter();
			Path fPath = PathUtils.visit(file, filename, visitor);
			
			if(visitor.isValid()) {
				Path feedXml = fPath.resolve(FeedManager.FEED_FILE_NAME);
				Feed feed = FeedManager.getInstance().readFeedFile(feedXml);
				if(feed != null && type.equals(feed.getResourceableTypeName())) {
					eval.setValid(true);
					eval.setDisplayname(feed.getTitle());
					eval.setDescription(feed.getDescription());
				}
			}
		} catch (IOException | IllegalArgumentException e) {
			log.error("", e);
		}
		return eval;
	}
	
	private static class IndexFileFilter extends SimpleFileVisitor<Path> {
		private boolean feedFile;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
		throws IOException {

			String filename = file.getFileName().toString();
			if(FeedManager.FEED_FILE_NAME.equals(filename)) {
				feedFile = true;
			}
			return feedFile ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
		}
		
		public boolean isValid() {
			return feedFile;
		}
	}

	/**
	 * Validates the uploaded resource directory
	 * 
	 * @param directory
	 * @return True if it is falid
	 */
	public static boolean validate(File directory, String type) {
		boolean valid = false;
		if (directory != null) {
			// Verify the directory structure:
			// /root
			// __feed.xml
			// __/items
			// ____/item
			// ______item.xml
			// ______/media.xml
			// ________...
			// ____/item
			// ______...
			VFSContainer root = new LocalFolderImpl(directory);
			// try to read podcast
			try {
				Feed feed = FeedManager.getInstance().readFeedFile(root);
				if (feed != null) {
					// The feed is valid, let's check the items
					if (feed.isInternal()) {
						List<String> itemIds = feed.getItemIds();
						VFSContainer itemsContainer = (VFSContainer) root.resolve(FeedManager.ITEMS_DIR);
						if (itemsContainer == null) {
							valid = itemIds.isEmpty(); //empty podcast
						} else {
							int validItemsCount = 0;
							for (String itemId : itemIds) {
								// Try loading each item
								VFSItem itemContainer = itemsContainer.resolve(itemId);
								Item item = FeedManager.getInstance().loadItem(itemContainer);
								if (item != null) {
									// This item is valid, increase the counter
									validItemsCount++;
								}
							}
							if (validItemsCount == itemIds.size()) {
								// The feed and all items are valid
								valid = true;
							}
						}
					} else if (feed.isExternal()) {
						// assume the feed url is valid.
						valid = true;
					} else if (feed.isUndefined()) {
						// the feed is empty.
						valid = true;
					}
					// check type
					if (!type.equals(feed.getResourceableTypeName())) {
						valid = false;
					}
				}
			} catch (Exception e) {
				// Reading feed failed, the directory is hence invalid
			}
		}
		return valid;
	}
}
