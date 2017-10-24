/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.ejb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.QueryException;
 
/**
 * The TextBlockService is an EJB handling documents containing textual information.
 * A text-block document is identified by its ID (txtname) and holds a HTML or PlainText information.
 * 
 * A text-block document holds the following items
 * 
 * <ul>
 * <li>txtmode - html/text</li>
 * <li>txtcontent - textual information</li>
 * </ul>
 * 
 * The type of a textBlock document is 'textblock'
 * 
 * A text-block can only be edited by a MANAGER. 
 * A text-block has no read restriction.
 * 
 * 
 * 
 * 
 * The TextBlockService ejb is implemented as a sigelton and uses an internal cache
 * to cache config entities.
 * 
 * 
 * @author rsoika
 */

@DeclareRoles({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@RolesAllowed({ "org.imixs.ACCESSLEVEL.NOACCESS", "org.imixs.ACCESSLEVEL.READERACCESS",
		"org.imixs.ACCESSLEVEL.AUTHORACCESS", "org.imixs.ACCESSLEVEL.EDITORACCESS",
		"org.imixs.ACCESSLEVEL.MANAGERACCESS" })
@Singleton
public class TextBlockService {

	int DEFAULT_CACHE_SIZE = 30;

	@EJB
	private DocumentService documentService;

	@Resource
	SessionContext ctx;
	
	private Cache cache = null;

	final String TYPE = "textblock";
	
	private static Logger logger = Logger.getLogger(TextBlockService.class.getName());


	/**
	 * PostContruct event - loads the imixs.properties.
	 */
	@PostConstruct
	void init() {
		// initialize cache
		cache = new Cache(DEFAULT_CACHE_SIZE);
	}



	/**
	 * This method deletes an existing text-block.
	 * 
	 * @param aconfig
	 * @throws AccessDeniedException
	 */
	public void deleteTextBlock(ItemCollection aconfig) throws AccessDeniedException {
		cache.remove(aconfig.getItemValueString("txtName"));
		documentService.remove(aconfig);
	}

	/**
	 * This method returns a text-block ItemCollection for a specified name or ID. If no
	 * text-block is found for this name the Method creates an empty
	 * text-block object. The text-block entity is cached internally and read
	 * from the cache
	 * 
	 * @param name
	 *            in attribute txtname
	 * 
	 */
	public ItemCollection loadTextBlock(String name) {
		return this.loadTextBlock(name, false);
	}

	/**
	 * This method returns a text-block ItemCollection for a specified name or id. If no
	 * text-block is found for this name the Method creates an empty
	 * text-block object. The text-block entity is cached internally. 
	 * 
	 * 
	 * @param name
	 *            in attribute txtname
	 *            
	 * @param discardCache
	 *            - indicates if the internal cache should be discarded.
	 */
	public ItemCollection loadTextBlock(String name, boolean discardCache) {
		ItemCollection textBlockItemCollection = null;
		// check cache...
		textBlockItemCollection = cache.get(name);
		if (textBlockItemCollection == null || discardCache) {
			
			// try to load by ID....
			textBlockItemCollection=documentService.load(name);
			if (textBlockItemCollection==null) {
				// not found by ID so lets try to load it by txtname.....
				// load text-block....
				String sQuery="(type:\"" + TYPE + "\" AND txtname:\"" + name + "\")";
				Collection<ItemCollection> col;
				try {
					col = documentService.find(sQuery, 1 ,0);

					if (col.size() > 0) {
						textBlockItemCollection = col.iterator().next();
					} else {
						logger.warning("Missing text-block : '" + name + "'");
					}
				} catch (QueryException e) {
					logger.warning("getTextBlock - invalid query: " + e.getMessage());
				}
				
			}
			
			if (textBlockItemCollection==null) {
				// create default values
				textBlockItemCollection = new ItemCollection();
				textBlockItemCollection.replaceItemValue("type", TYPE);
				textBlockItemCollection.replaceItemValue("txtname", name);
			}
			cache.put(name, textBlockItemCollection);
		}
		return textBlockItemCollection;
	}

	/**
	 * SavesO the text-block entity
	 * 
	 * @return
	 * @throws AccessDeniedException
	 */
	public ItemCollection save(ItemCollection textBlockItemCollection) throws AccessDeniedException {
		if (textBlockItemCollection==null) {
			return textBlockItemCollection;
		}
		// update write and read access
		textBlockItemCollection.replaceItemValue("type", TYPE);
		// update write and read access
		textBlockItemCollection.replaceItemValue("$writeAccess", "org.imixs.ACCESSLEVEL.MANAGERACCESS");
		textBlockItemCollection.replaceItemValue("$readAccess", "");
		textBlockItemCollection.replaceItemValue("namcurrentEditor", ctx.getCallerPrincipal().getName().toString());
		
		// save entity
		textBlockItemCollection = documentService.save(textBlockItemCollection);

		cache.put(textBlockItemCollection.getItemValueString("txtName"), textBlockItemCollection);

		return textBlockItemCollection;
	}

	/**
	 * Returns a list of all configuration entities. The method uses JQPL staements instead of lucene index. 
	 * 
	 * @see issue #172
	 * @return
	 */
	public List<ItemCollection> findAllConfigurations() {
		ArrayList<ItemCollection> configList = new ArrayList<ItemCollection>();
		Collection<ItemCollection> col = documentService.getDocumentsByType(TYPE);
		for (ItemCollection aworkitem : col) {
			configList.add(aworkitem);
		}
		
		// sort by txtname
		Collections.sort(configList, new ItemCollectionComparator("txtname", true));
		
		
		return configList;
	}

	/**
	 * Cache implementation to hold config entities
	 * 
	 * @author rsoika
	 * 
	 */
	class Cache extends LinkedHashMap<String, ItemCollection> implements Serializable {
		private static final long serialVersionUID = 1L;
		private final int capacity;

		public Cache(int capacity) {
			super(capacity + 1, 1.1f, true);
			this.capacity = capacity;
		}

		protected boolean removeEldestEntry(Entry<String, ItemCollection> eldest) {
			return size() > capacity;
		}
	}

}
