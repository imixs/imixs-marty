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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.engine.plugins.ResultPlugin;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The DMSSplitPlugin provides functionality to create sub-process instances or
 * each attachment added in an origin process. The configuration is similar to
 * the Imixs-Workflow SplitAndJoinPlugin. The item name to trigger the creation
 * of subprocesses is "dms_subprocess_create" The tag '<remove>true</remove>'
 * indicates that the attachments will be removed from the origin process after
 * the subprocess was created.
 * 
 * <code>
	<item name="dms_subprocess_create">
	    <modelversion>1.0.0</modelversion>
	    <processid>100</processid>
	    <activityid>10</activityid>
	    <items>namTeam</items>
	</item>
 * </code>
 * 
 * 
 * A subprocess will contain the $UniqueID of the origin process stored in the
 * property $uniqueidRef. The origin process will contain a link to the
 * subprocess stored in the property txtworkitemRef. So both workitems are
 * linked together.
 * 
 * The list of attachments will be taken from the BlobWorkitem.
 * 
 * 
 * @author Ralph Soika
 * @version 1.0
 * @see http://www.imixs.org/doc/engine/plugins/splitandjoinplugin.html
 * 
 */
public class DMSSplitPlugin extends AbstractPlugin {
	public static final String LINK_PROPERTY = "txtworkitemref";
	public static final String DMS_SUBPROCESS_CREATE = "dms_subprocess_create";

	private static Logger logger = Logger.getLogger(DMSSplitPlugin.class.getName());

	/**
	 * The method evaluates the workflow activity result for items with name:
	 * 
	 * subprocess_create
	 * 
	 * subprocess_update
	 * 
	 * origin_update
	 * 
	 * For each item a corresponding processing cycle will be started.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ItemCollection run(ItemCollection adocumentContext, ItemCollection adocumentActivity)
			throws PluginException {

		ItemCollection evalItemCollection = ResultPlugin.evaluateWorkflowResult(adocumentActivity, adocumentContext);

		if (evalItemCollection == null)
			return adocumentContext;

		// 1.) test for items with name dms_subprocess_create and create the
		// defined suprocesses
		if (evalItemCollection.hasItem(DMS_SUBPROCESS_CREATE)) {
			logger.fine("processing " + DMS_SUBPROCESS_CREATE);
			// extract the create subprocess definitions...
			List<String> processValueList = evalItemCollection.getItemValue(DMS_SUBPROCESS_CREATE);

			if (processValueList == null || processValueList.size() == 0) {
				// no definition found
				logger.warning("Invalid DMS_SUBPROCESS_CREATE definition found, please check model!");
				return adocumentContext;
			}

			if (processValueList.size() > 1) {
				// no definition found
				logger.warning("More than one DMS_SUBPROCESS_CREATE definitions found, please check model!");
			}

			String processValue = processValueList.get(0);
			if (processValue.trim().isEmpty()) {
				// no definition found
				logger.warning("Invalid DMS_SUBPROCESS_CREATE definition found, please check model!");
				return adocumentContext;
			}
			// evaluate the item content (XML format expected here!)
			ItemCollection dmsProcessData = ResultPlugin.parseItemStructure(processValue);

			adocumentContext = createSubprocesses(dmsProcessData, adocumentContext);
		}

		return adocumentContext;
	}

	/**
	 * This method expects a of DMS-Subprocess definition and creates for each
	 * attachment a new subprocess. The reference of the created subprocess will
	 * be stored in the property txtworkitemRef of the origin workitem. The
	 * method will remove attachments form the originWorkitem in case the tag
	 * <remove>true</remove> is found.
	 * 
	 * The method returns the originWorkitem
	 * 
	 * @param processData
	 *            - DMSsubProcessDefinition
	 * @param originWorkitem
	 * @return originWorkitem
	 * @throws AccessDeniedException
	 * @throws ProcessingErrorException
	 * @throws PluginException
	 */
	private ItemCollection createSubprocesses(final ItemCollection processData, final ItemCollection originWorkitem)
			throws AccessDeniedException, ProcessingErrorException, PluginException {

		if (processData != null) {

			// get all attachments
			Map<String, List<Object>> files = loadFilesFromBlobWorkitem(originWorkitem);

			if (files == null) {
				return originWorkitem;
			}

			for (Map.Entry<String, List<Object>> entry : files.entrySet()) {
				String key = entry.getKey();
				List<Object> value = entry.getValue();

				logger.fine("create dms_subprocess for " + key);
				// create new process instance for each attachment
				ItemCollection workitemSubProcess = new ItemCollection();

				// now clone the field list...
				copyItemList(processData.getItemValueString("items"), originWorkitem, workitemSubProcess);

				workitemSubProcess.replaceItemValue(WorkflowKernel.MODELVERSION,
						processData.getItemValueString("modelversion"));
				workitemSubProcess.replaceItemValue(WorkflowKernel.PROCESSID,
						new Integer(processData.getItemValueString("processid")));
				workitemSubProcess.replaceItemValue(WorkflowKernel.ACTIVITYID,
						new Integer(processData.getItemValueString("activityid")));

				// add the origin reference
				workitemSubProcess.replaceItemValue(WorkflowService.UNIQUEIDREF, originWorkitem.getUniqueID());

				// add the attachment
				byte[] bytes = (byte[]) value.get(1);
				workitemSubProcess.addFile(bytes, key, value.get(0).toString());

				// process the new subprocess...
				workitemSubProcess = getWorkflowService().processWorkItem(workitemSubProcess);

				logger.fine("successful created new subprocess.");
				// finally add the new workitemRef into the origin
				// documentContext
				addWorkitemRef(workitemSubProcess.getUniqueID(), originWorkitem);

			}

			// remove attachments?
			if (processData.getItemValueBoolean("remove")) {
				for (String filename : files.keySet()) {
					logger.fine("remove attachment '" + filename + "'");
					originWorkitem.removeFile(filename);
				}
			}

		}

		return originWorkitem;

	}

	/**
	 * This Method copies the fields defined in 'items' into the targetWorkitem.
	 * Multiple values are separated with comma ','.
	 * 
	 * In case a item name contains '|' the target field name will become the
	 * right part of the item name.
	 */
	private void copyItemList(String items, ItemCollection source, ItemCollection target) {
		// clone the field list...
		StringTokenizer st = new StringTokenizer(items, ",");
		while (st.hasMoreTokens()) {
			String field = st.nextToken().trim();

			int pos = field.indexOf('|');
			if (pos > -1) {
				target.replaceItemValue(field.substring(pos + 1).trim(),
						source.getItemValue(field.substring(0, pos).trim()));
			} else {
				target.replaceItemValue(field, source.getItemValue(field));
			}
		}
	}

	/**
	 * This methods adds a new workItem reference into a workitem
	 */
	private void addWorkitemRef(String aUniqueID, ItemCollection workitem) {

		logger.fine("LinkController add workitem reference: " + aUniqueID);

		@SuppressWarnings("unchecked")
		List<String> refList = workitem.getItemValue(LINK_PROPERTY);

		// clear empty entry if set
		if (refList.size() == 1 && "".equals(refList.get(0)))
			refList.remove(0);

		// test if not yet a member of
		if (refList.indexOf(aUniqueID) == -1) {
			refList.add(aUniqueID);
			workitem.replaceItemValue(LINK_PROPERTY, refList);
		}

	}

	/**
	 * This method loads the BlobWorkitem for a given parent WorkItem and
	 * returns the FileList. The BlobWorkitem is identified by the $unqiueidRef.
	 * 
	 * If no BlobWorkitem still exists the method returns null
	 * 
	 */
	private Map<String, List<Object>> loadFilesFromBlobWorkitem(ItemCollection parentWorkitem) {
		ItemCollection blobWorkitem = null;

		// is parentWorkitem defined?
		if (parentWorkitem == null)
			return null;

		blobWorkitem = BlobWorkitemHandler.load(this.getWorkflowService().getDocumentService(), parentWorkitem);

		// if blobWorkitem was found, return the file list.
		if (blobWorkitem != null) {
			Map<String, List<Object>> files = blobWorkitem.getFiles();
			return files;
		}
		return null;

	}
}
