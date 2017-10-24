/*******************************************************************************
 *  Imixs Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *  
 *******************************************************************************/
package org.imixs.marty.config;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.imixs.marty.ejb.TextBlockService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;

/**
 * The TextBlockController is a CDI bean providing a methods to manage
 * text-block items. A text-block item is identified by its ID (txtname) and
 * holds a HTML or PlainText information. The controller extends the
 * DocumentController.
 * 
 * The type of a textBlock document is 'textblock'
 * 
 * A text-block can only be edited by a MANAGER. A text-block has no read
 * restriction.
 * 
 * @author rsoika
 * 
 */
@Named
@ApplicationScoped
public class TextBlockController implements Serializable {

	public static final String DOCUMENT_TYPE = "textblock";

	private ItemCollection textBlock;
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(TextBlockController.class.getName());

	@EJB
	private TextBlockService textBlockService;

	/**
	 * Returns the current text-block
	 * 
	 * @return - ItemCollection
	 */
	public ItemCollection getTextBlock() {
		return textBlock;
	}

	/**
	 * Set the current text-block
	 * 
	 * @param textBlockItemCollection
	 */
	public void setTextBlock(ItemCollection textBlockItemCollection) {
		this.textBlock = textBlockItemCollection;
	}

	/**
	 * The init method is used to add necessary indices to the entity index list if
	 * index still exists the method did change any data
	 */
	@PostConstruct
	public void init() {
		logger.finest("init...");
	}

	/**
	 * save method saves the current text-bloc
	 * 
	 * @return
	 */
	public void save() throws AccessDeniedException {
		textBlockService.save(textBlock);

	}

	/**
	 * Loads a text-block item by Name or uniqueid
	 * 
	 * @param name
	 * @return
	 */
	public ItemCollection load(String name) {
		textBlock = textBlockService.loadTextBlock(name);
		return textBlock;
	}

	public ItemCollection create() {
		textBlock = new ItemCollection();
		// set html mode
		textBlock.replaceItemValue("txtmode", "HTML");
		return textBlock;
	}

}
