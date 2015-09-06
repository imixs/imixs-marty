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

import java.util.Set;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.ResultPlugin;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * The SubprozessPlugin evaluates the Result Attribute of the ActivityEntity for
 * the tag subprocess to create new subprocess:
 * 
 * <code>
 * <item name="subprocess">100200.10</item> 
 * <item name="subprocess">modelversion-xxx-0|100200.10</item>
 * </code>
 * 
 * @author rsoika
 * @version 3.0
 * 
 */
public class SubprocessPlugin extends AbstractPlugin {
	ItemCollection documentContext;
	public final static String SUBPROCESS_ITEM = "subprocess";
	public final static String LINK_PROPERTY = "txtworkitemref";

	private static Logger logger = Logger.getLogger(SubprocessPlugin.class
			.getName());

	private WorkflowService workflowService = null;

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			workflowService = (WorkflowService) actx;
		}

	}

	@Override
	public int run(ItemCollection adocumentContext,
			ItemCollection documentActivity) throws PluginException {

		documentContext = adocumentContext;
		// check if a subprocess should be started now....
		createSubProcess(adocumentContext, documentActivity);

		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws PluginException {

	}

	/**
	 * This plugin creates a subprocess depending on the activity result
	 * settings
	 * 
	 * Example:
	 * 
	 * <code>
			<item name="subprocess">10400.10</item>
   	   </code>
	 * 
	 * optional a model version can be provided
	 * 
	 * <code>
			<item name="subprocess">mode-de-1.0.0|10400.10</item>
   	   </code>
	 * 
	 * The new WorkItem will become a referent to the main WorkItem by updating
	 * the property 'txtworkitemref'.
	 * 
	 * @param adocumentContext
	 * @param documentActivity
	 * @throws PluginException
	 */
	@SuppressWarnings({ "unused" })
	void createSubProcess(ItemCollection adocumentContext,
			ItemCollection documentActivity) throws PluginException {
		int iMainProcessId = adocumentContext
				.getItemValueInteger(WorkflowService.PROCESSID);
		String modelVersion = null;

		logger.fine("[SubprozessPlugin] createSubProcess....");
		// find: <item name="subprocess">100200.10</item>
		String sResult = documentActivity
				.getItemValueString("txtActivityResult");

		ItemCollection evalItemCollection = new ItemCollection();
		ResultPlugin.evaluate(sResult, evalItemCollection);

		if (evalItemCollection.hasItem(SUBPROCESS_ITEM)) {
			// Subprozess erzeugen....
			String sSubprocessItem = evalItemCollection
					.getItemValueString(SUBPROCESS_ITEM);
			logger.fine("[SubprozessPlugin] start subprocess " + sSubprocessItem);

			// test for modelversion
			if (sSubprocessItem.indexOf('|') > -1) {
				modelVersion = sSubprocessItem.substring(0,
						sSubprocessItem.indexOf('|'));
				sSubprocessItem = sSubprocessItem.substring(sSubprocessItem
						.indexOf('|') + 1);
			} else {
				// take the model version from the main process
				modelVersion = adocumentContext
						.getItemValueString(WorkflowService.MODELVERSION);
			}

			// now split processid and activityid
			int pos = sSubprocessItem.indexOf('.');
			if (pos == -1)
				throw new PluginException(this.getClass().getSimpleName(),
						"WRONG SUBPROCESS FORMAT", "Subprocess '"
								+ sSubprocessItem + "' has invalid format ");
			int iProcessId = new Integer(sSubprocessItem.substring(0, pos));
			int iActivityId = new Integer(sSubprocessItem.substring(pos + 1));

			// create Workitem
			ItemCollection subprocess = new ItemCollection();
			subprocess.replaceItemValue("type", "workitem");
			subprocess.replaceItemValue(WorkflowService.MODELVERSION,
					modelVersion);
			subprocess.replaceItemValue(WorkflowService.UNIQUEIDREF,
					adocumentContext
							.getItemValueString(WorkflowService.UNIQUEID));
			subprocess.replaceItemValue(WorkflowService.PROCESSID, iProcessId);
			subprocess
					.replaceItemValue(WorkflowService.ACTIVITYID, iActivityId);

			// copy processRef
			subprocess.replaceItemValue("txtProcessRef",
					adocumentContext.getItemValueString("txtProcessRef"));

			// set txtWorkitemRef....
			subprocess.replaceItemValue(LINK_PROPERTY, adocumentContext
					.getItemValueString(WorkflowService.UNIQUEID));

			// Copy all items starting with fz_
			Set<String> fieldNames = adocumentContext.getAllItems().keySet();
			for (String sFieldName : fieldNames) {
				if (sFieldName.startsWith("fz_")) {
					subprocess.replaceItemValue(sFieldName,
							adocumentContext.getItemValue(sFieldName));
				}
			}

			subprocess = this.workflowService.processWorkItem(subprocess);
			logger.fine("[SubprozessPlugin] successful processed subprocess.");
		}

	}

}
