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

package org.imixs.marty.plugins;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.plugins.ResultPlugin;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plugin suports a commment feature. Comments entered by a user into the
 * field 'txtComment' are stored in the list property 'txtCommentList' which
 * contains a map for each comment. The map stores the username, the timestamp
 * and the comment. The plugin also stores the last comment in the field
 * 'txtLastComment'. The comment can be also controlled by the corresponding
 * workflow event:
 * 
 * <comment ignore="true" /> a new comment will not be added into the comment
 * list
 * 
 * <comment>xxx</comment> adds a fixed comment 'xxx' into the comment list
 * 
 * 
 * @author rsoika
 * @version 1.0
 * 
 */
public class CommentPlugin extends AbstractPlugin {
	ItemCollection documentContext;

	private static Logger logger = Logger.getLogger(CommentPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
	}

	/**
	 * This method updates the comment list. There for the method copies the
	 * txtComment into the txtCommentList and clears the txtComment field
	 * 
	 * @param workflowEvent
	 */
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection adocumentContext, ItemCollection documentActivity) throws PluginException {

		documentContext = adocumentContext;

		// update the comment
		// evaluate activity result
		String sResult = documentActivity.getItemValueString("txtActivityResult");
		sResult = replaceDynamicValues(sResult, adocumentContext);

		ItemCollection evalItemAttributes = evaluateItemAttributes(sResult, "comment");

		// test if comment is defined in model
		if (evalItemAttributes != null) {
			// test ignore
			if ("true".equals(evalItemAttributes.getItemValueString("ignore"))) {
				logger.fine("ignore=true - skipping txtCommentLog");
				return Plugin.PLUGIN_OK;
			}
		}

		// create new Comment data - important: property names in lower
		// case
		List<Map<String, Object>> vCommentList = documentContext.getItemValue("txtCommentLog");
		Map<String, Object> log = new HashMap<String, Object>();
		Date dt = Calendar.getInstance().getTime();
		String remoteUser = this.getEjbSessionContext().getCallerPrincipal().getName();
		log.put("datcomment", dt);
		log.put("nameditor", remoteUser);

		// test for fixed comment
		ItemCollection evalueItemValues = new ItemCollection();
		ResultPlugin.evaluate(sResult, evalueItemValues);
		String sComment = null;
		if (evalueItemValues != null && evalueItemValues.hasItem("comment")) {
			sComment = evalueItemValues.getItemValueString("comment");
		} else {
			sComment = documentContext.getItemValueString("txtComment");
			// clear comment
			documentContext.replaceItemValue("txtComment", "");
			// save last comment
			documentContext.replaceItemValue("txtLastComment", sComment);
		}
		if (sComment != null && !sComment.isEmpty()) {
			log.put("txtcomment", sComment);
			vCommentList.add(0, log);
			documentContext.replaceItemValue("txtcommentLog", vCommentList);
		}

		return Plugin.PLUGIN_OK;

	}

	@Override
	public void close(int arg0) throws PluginException {

	}

	
	
	/**
	 * This method evaluates the attributes stored in a named item
	 *
	 * 
	 * e.g. <item name="comment" ignore="true" />
	 * 
	 * will return a itemCollection with item 'ignore' and value 'true'
	 * 
	 * @param field
	 * @param sResult
	 * @return
	 */
	private static ItemCollection evaluateItemAttributes(String resultString, String itemName) throws PluginException {
		int iTagStartPos;
		int iTagEndPos;
		if (resultString == null)
			return null;

		// test if a <value> tag exists...
		while ((iTagStartPos = resultString.toLowerCase().indexOf("<item")) != -1) {

			iTagEndPos = resultString.toLowerCase().indexOf("/>", iTagStartPos);
			if (iTagEndPos == -1) {
				iTagEndPos = resultString.toLowerCase().indexOf("</item>", iTagStartPos);
			}
			// if no end tag found return string unchanged...
			if (iTagEndPos == -1)
				throw new PluginException(ResultPlugin.class.getSimpleName(), "INVALID_FORMAT", "</item>  expected!");

			
			ItemCollection itemColAttributes=new ItemCollection();
			String sItemTag=resultString.substring(iTagStartPos,iTagEndPos);
			// now we check the attributes in this item tag
			String spattern ="(\\S+)=[\"']?((?:.(?![\"']?\\s+(?:\\S+)=|[>\"']))+.)[\"']?";
			// alternative
			// (?:[^<]|<[^!]|<![^-\[]|<!\[(?!CDATA)|<!\[CDATA\[.*?\]\]>|<!--(?:[^-]|-[^-])*-->)
			Pattern pattern = Pattern.compile(spattern);
			Matcher matcher = pattern.matcher(sItemTag);
			while (matcher.find()) {
				String name=matcher.group(1);
				String value=matcher.group(2);
			    logger.fine(name);
			    logger.fine(value);
			    
			    itemColAttributes.replaceItemValue(name, value);
			    
			    
			    
			}
			
			// did we found the matching item tag?
			if (itemColAttributes.getItemValueString("name").equals(itemName)) {
				return itemColAttributes;
			}
			
			// skipp to next tag...
			resultString=resultString.substring(iTagEndPos);
			
		}

		return null;
	}

}
