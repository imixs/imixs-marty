package org.imixs.marty.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.engine.plugins.VersionPlugin;
import org.imixs.workflow.jee.ejb.EntityService;

/**
 * The DMS Plug-in stores attached files of a workitem into a separated
 * BlobWorkitem and stores DMS meta data into the workitem.
 * 
 * The Plug-in computes the read and write access for the BlobWorkitem based on
 * the ACL of the processed workItem. The Plug-in should run immediate after the
 * AccessPlugin.
 * 
 * The Plug-in only runs in workItems type=workitem or type=workitemarchive
 * 
 * The plug-in provides additional static methods to set and get the DMS
 * metadata for a workitem. The DMS meta data is stored in the property "dms".
 * This property provides a list of Map objects containing the dms meta data.
 * The method getDMSList can be used to convert this list into a List of
 * ItemCollection elements.
 * 
 * The DMS Plug-in also provides a mechanism to import files from the file
 * system. If the workitem contains the property 'txtDmsImport" all files from
 * the given path will be added into the blobWorkitem
 * 
 * @author rsoika
 * 
 */
public class DMSPlugin extends AbstractPlugin {

	public final static String DMS_IMPORT_PROPERTY = "txtDmsImport";
	public final static String DEFAULT_PROTOCOLL = "file://";
	public final static String BLOBWORKITEMID = "$BlobWorkitem";

	ItemCollection workitem = null;
	private ItemCollection blobWorkitem = null;
	private static Logger logger = Logger.getLogger(DMSPlugin.class.getName());

	

	/**
	 * Update the read and writeAccess of the blobWorkitem
	 * 
	 * If the workItem contains the property 'txtDmsImport" all files from the
	 * given path will be added into the blobWorkitem
	 * 
	 * If a file contained in the property '$file' is not part of the property
	 * 'dms' the method will automatically create a new dms entry by calling the
	 * method updateDmsList.
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection documentContext, ItemCollection documentActivity) {

		workitem = documentContext;

		// Skip if plugin this processing a new version created by the version
		// plugin - in this case no changes to the version are needed
		if (VersionPlugin.isProcssingVersion(workitem)) {
			return Plugin.PLUGIN_OK;

		}

		// load the blobWorkitem and update read- and write access
		blobWorkitem = loadBlobWorkitem(workitem);

		if (blobWorkitem == null) {
			logger.warning("[DMSPlugin] can't access blobworkitem for '"
					+ workitem.getItemValueString(WorkflowService.UNIQUEID) + "'");
			return Plugin.PLUGIN_WARNING;
		}

		// import files if txtDmsImport is defined.
		if (documentContext.hasItem(DMS_IMPORT_PROPERTY)) {
			importFilesFromPath(workitem, blobWorkitem, documentContext.getItemValue(DMS_IMPORT_PROPERTY));
			// remove property
			documentContext.removeItem(DMS_IMPORT_PROPERTY);
		}

		logger.fine("[DMSPlugin] updating $readaccess/$writeaccess for "
				+ workitem.getItemValueString(WorkflowService.UNIQUEID));

		// Update Read and write access list from parent workItem
		List<?> vAccess = workitem.getItemValue("$ReadAccess");
		blobWorkitem.replaceItemValue("$ReadAccess", vAccess);

		vAccess = workitem.getItemValue("$WriteAccess");
		blobWorkitem.replaceItemValue("$WriteAccess", vAccess);

		blobWorkitem.replaceItemValue("$uniqueidRef", workitem.getItemValueString(WorkflowService.UNIQUEID));
		blobWorkitem.replaceItemValue("type", "workitemlob");

		logger.fine(
				"[DMBPlugin] saving blobWorkitem '" + blobWorkitem.getItemValueString(EntityService.UNIQUEID) + "'...");

		// update file content and save BlobWorkitem...
		updateFileContent(workitem, blobWorkitem);
		blobWorkitem = getWorkflowService().getDocumentService().save(blobWorkitem);

		// update property '$BlobWorkitem'
		workitem.replaceItemValue(BLOBWORKITEMID, blobWorkitem.getItemValueString(EntityService.UNIQUEID));

		// update the dms list - e.g. if another plugin had added a file....
		updateDmsList(workitem, this.getWorkflowService().getUserName());

		return Plugin.PLUGIN_OK;
	}

	public void close(int arg0) {
		// no op

	}

	/**
	 * This method returns a list of ItemCollections for all DMS elements
	 * attached to the current workitem. The DMS meta data is read from the
	 * property 'dms'.
	 * 
	 * The dms property is updated in the run() method of this Plugin.
	 * 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<ItemCollection> getDmsList(ItemCollection workitem) {
		// build a new fileList and test if each file contained in the $files is
		// listed
		List<ItemCollection> dmsList = new ArrayList<ItemCollection>();
		if (workitem == null)
			return dmsList;

		List<Map> vDMS = workitem.getItemValue("dms");
		// first we add all existing dms informations
		for (Map aMetadata : vDMS) {
			dmsList.add(new ItemCollection(aMetadata));
		}

		// sort list by name
		Collections.sort(dmsList, new ItemCollectionComparator("txtname", true));

		return dmsList;
	}

	/**
	 * This method converts a list of ItemCollections for DMS elements into Map
	 * objects and updates the workitem property 'dms'.
	 * 
	 * 
	 */
	@SuppressWarnings("rawtypes")
	public static void putDmsList(ItemCollection workitem, List<ItemCollection> dmsList) {
		// convert the List<ItemCollection> into a List<Map>
		List<Map> vDMSnew = new ArrayList<Map>();
		if (dmsList != null) {
			for (ItemCollection dmsEntry : dmsList) {
				vDMSnew.add(dmsEntry.getAllItems());
			}
		}
		// update the workitem
		workitem.replaceItemValue("dms", vDMSnew);
	}

	/**
	 * This method adds a new entry into the dms property. The method returns
	 * the updated DMS List
	 * 
	 */
	public static List<ItemCollection> addDMSEntry(ItemCollection aworkitem, ItemCollection dmsEntity) {

		List<ItemCollection> dmsList = DMSPlugin.getDmsList(aworkitem);
		String sNewName = dmsEntity.getItemValueString("txtName");
		String sNewUrl = dmsEntity.getItemValueString("url");

		// test if the entry already exists - than overwrite it....
		for (Iterator<ItemCollection> iterator = dmsList.iterator(); iterator.hasNext();) {
			ItemCollection admsEntry = iterator.next();
			String sName = admsEntry.getItemValueString("txtName");
			String sURL = admsEntry.getItemValueString("url");
			if (sURL.endsWith(sNewUrl) && sName.equals(sNewName)) {
				// Remove the current element from the iterator and the list.
				iterator.remove();
				logger.fine("[DMSPlugin] remove dms entry '" + sName + "'");
			}
		}

		dmsList.add(dmsEntity);

		putDmsList(aworkitem,dmsList);
		
		return dmsList;

	}

	/**
	 * This method updates the property 'dms' with the meta data of attached
	 * files or links.
	 * 
	 * This method creates new empty DMS entries for new uploaded files which
	 * are still not contained in the dms list.
	 * 
	 * 
	 * @param aWorkitem
	 * @param dmsList
	 *            - map with meta information for each file entry
	 * @param defaultUsername
	 *            - default username for new dms entries
	 * 
	 */
	private void updateDmsList(ItemCollection aWorkitem, String defaultUsername) {

		List<ItemCollection> currentDmsList = getDmsList(aWorkitem);

		List<String> currentFileList = aWorkitem.getFileNames();
		String blobWorkitemId = aWorkitem.getItemValueString(BLOBWORKITEMID);

		// first we remove all DMS entries which did not have a matching
		// $File-Entry and are not from type link
		for (Iterator<ItemCollection> iterator = currentDmsList.iterator(); iterator.hasNext();) {
			ItemCollection dmsEntry = iterator.next();
			String sName = dmsEntry.getItemValueString("txtName");
			String sURL = dmsEntry.getItemValueString("url");
			if (sURL.isEmpty() && !currentFileList.contains(sName)) {
				// Remove the current element from the iterator and the list.
				iterator.remove();
				logger.fine("[DMSPlugin] remove dms entry '" + sName + "'");
			}
		}

		// now we test for each file entry if a dms meta data entry
		// exists. If not we create a new one...
		for (String aFilename : currentFileList) {

			if (findDMSEntry(aFilename, currentDmsList) == null) {
				// no meta data exists.... create a new meta object
				ItemCollection dmsEntry = new ItemCollection();
				dmsEntry.replaceItemValue("txtname", aFilename);
				dmsEntry.replaceItemValue("$uniqueidRef", blobWorkitemId);
				dmsEntry.replaceItemValue("$created", new Date());
				dmsEntry.replaceItemValue("namCreator", defaultUsername);
				dmsEntry.replaceItemValue("txtcomment", "");
				currentDmsList.add(dmsEntry);
			}

		}

		putDmsList(aWorkitem,currentDmsList);
	}

	/**
	 * This method transfers the File content of the workitem into the blob
	 * workitem. The workitem will only hold a empty byte array for files
	 * 
	 * @param aWorkitem
	 * @param aBlobWorkitem
	 */
	private void updateFileContent(ItemCollection aWorkitem, ItemCollection aBlobWorkitem) {
		// check files from master workitem
		Map<String, List<Object>> files = aWorkitem.getFiles();
		if (files == null) {
			aBlobWorkitem.removeItem("$file");
			return;
		}

		for (Entry<String, List<Object>> entry : files.entrySet()) {
			String sFileName = entry.getKey();
			List<?> file = entry.getValue();

			// if data size >0 transfer file into blob
			if (file.size() >= 2) {
				String contentType = (String) file.get(0);
				byte[] data = (byte[]) file.get(1);
				if (data != null && data.length > 1) {
					// move...
					aBlobWorkitem.addFile(data, sFileName, contentType);
					// empty data...
					byte[] empty = { 0 };
					// add the file name (with empty data) into the
					// parentWorkitem.
					aWorkitem.addFile(empty, sFileName, "");
				}
			}
		}
		// now we remove all files form the blobWorkitem which are not part of
		// the workitem
		List<String> currentFileNames = aWorkitem.getFileNames();
		List<String> blobFileNames = aBlobWorkitem.getFileNames();
		List<String> removeList = new ArrayList<String>();
		for (String aname : blobFileNames) {
			if (!currentFileNames.contains(aname)) {
				removeList.add(aname);
			}
		}
		for (String aname : removeList) {
			aBlobWorkitem.removeFile(aname);
		}

	}

	/**
	 * This method loads the BlobWorkitem for a given parent WorkItem. The
	 * BlobWorkitem is identified by the $unqiueidRef.
	 * 
	 * If no BlobWorkitem still exists the method creates a new empty
	 * BlobWorkitem which can be saved later.
	 * 
	 */
	private ItemCollection loadBlobWorkitem(ItemCollection parentWorkitem) {
		ItemCollection blobWorkitem = null;

		// is parentWorkitem defined?
		if (parentWorkitem == null)
			return null;

		String sUniqueID = parentWorkitem.getItemValueString(WorkflowService.UNIQUEID);

		// try to load the blobWorkitem with the parentWorktiem reference....
		if (!"".equals(sUniqueID)) {
			// search entity...
//			String sQuery = " SELECT lobitem FROM Entity as lobitem" + " join lobitem.textItems as t2"
//					+ " WHERE lobitem.type = 'workitemlob'" + " AND t2.itemName = '$uniqueidref'"
//					+ " AND t2.itemValue = '" + sUniqueID + "'";
//			
			
			String sQuery="(type:\"workitemlob\" AND $uniqueidref:\""+sUniqueID + "\")";
		

			Collection<ItemCollection> itemcol = getWorkflowService().getDocumentService().find(sQuery, 0, 1);
			// if blobWorkItem was found return...
			if (itemcol != null && itemcol.size() > 0) {
				blobWorkitem = itemcol.iterator().next();

			}

		} else {
			// no $uniqueId set - create a UniqueID for the parentWorkitem
			parentWorkitem.replaceItemValue(EntityService.UNIQUEID, WorkflowKernel.generateUniqueID());

		}
		// if no blobWorkitem was found, create a empty itemCollection..
		if (blobWorkitem == null) {
			blobWorkitem = new ItemCollection();

			blobWorkitem.replaceItemValue("type", "workitemlob");
			// generate default uniqueid...
			blobWorkitem.replaceItemValue(EntityService.UNIQUEID, WorkflowKernel.generateUniqueID());
			blobWorkitem.replaceItemValue("$UniqueidRef", parentWorkitem.getItemValueString(EntityService.UNIQUEID));

		}
		return blobWorkitem;

	}

	/**
	 * This method returns the meta data of a specific file in the exiting
	 * filelist.
	 * 
	 * @return
	 */
	private static ItemCollection findDMSEntry(String aFilename, List<ItemCollection> dmsList) {

		for (ItemCollection dmsEntry : dmsList) {
			// test if filename matches...
			String sName = dmsEntry.getItemValueString("txtname");
			if (sName.equals(aFilename))
				return dmsEntry;
		}
		// no matching meta data found!
		return null;
	}

	/**
	 * Import files from a given location.
	 * 
	 * @param aWorkitem
	 * @param importList
	 *            - list of files
	 * 
	 */
	private void importFilesFromPath(ItemCollection adocumentContext, ItemCollection blobWorkitem,
			List<String> importList) {

		for (String fileUri : importList) {

			if (fileUri.isEmpty()) {
				continue;
			}

			String fullFileUri = fileUri;
			String fileName = null;

			// check for a protocoll
			if (!fullFileUri.contains("://")) {
				fullFileUri = DEFAULT_PROTOCOLL + fullFileUri;
			}

			// extract fileame....
			if (!fullFileUri.contains("/")) {
				continue;
			}
			fileName = fullFileUri.substring(fullFileUri.lastIndexOf("/") + 1);

			logger.info("[DMSPlugin] importFilesFromPath: " + fullFileUri);

			try {

				URL url = new URL(fullFileUri);

				ByteArrayOutputStream bais = new ByteArrayOutputStream();
				InputStream is = null;
				try {
					is = url.openStream();
					byte[] byteChunk = new byte[4096]; // Or whatever size you
														// want to read in at a
														// time.
					int n;

					while ((n = is.read(byteChunk)) > 0) {
						bais.write(byteChunk, 0, n);
					}
				} catch (IOException e) {
					logger.severe("Failed while reading bytes from " + url.toExternalForm());
					e.printStackTrace();
					// Perform any other exception handling that's appropriate.
				} finally {
					if (is != null) {
						is.close();
					}
				}

				blobWorkitem.addFile(bais.toByteArray(), fileName, "");

				// add the file name (with empty data) into the
				// parentWorkitem.
				byte[] empty = { 0 };
				adocumentContext.addFile(empty, fileName, "");

				logger.info("[DMSPlugin] file import successfull ");

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
