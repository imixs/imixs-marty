package org.imixs.marty.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.ItemCollectionComparator;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.engine.plugins.VersionPlugin;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The DMS Plug-in stores attached files of a workitem into a separated
 * BlobWorkitem and stores DMS meta data into the workitem.
 * 
 * The Plug-in computes the read and write access for the BlobWorkitem based on
 * the ACL of the processed workItem. The Plug-in should run immediate after the
 * AccessPlugin.
 * 
 * The Plug-in calculates a MD5 checksum for the file content.
 * 
 * <br>
 * <br>
 * The Plug-in only runs if workItem type is 'workitem' or 'workitemarchive'
 * 
 * <br>
 * <br>
 * The plug-in provides additional static methods to set and get the DMS meta
 * data for a workitem. The DMS meta data is stored in the property "dms". This
 * property provides a list of Map objects containing the dms meta data. The
 * method getDMSList can be used to convert this list into a List of
 * ItemCollection elements.
 * 
 * <br>
 * <br>
 * In addition the DMS Plug-in also provides a mechanism to import files from
 * the file system. If the workitem contains the property 'txtDmsImport" all
 * files from the given path will be added into the blobWorkitem <br>
 * <br>
 * The DMS Plug-in provides the following attriubtes: <br>
 * <ul>
 * <li>dms : meta data for files</li>
 * <li>dms_names : list of all file names (for lucene search)</li>
 * <li>dms_ocr : ocr text for each file (not yet used)</li>
 * </ul>
 * 
 * @author rsoika
 * @version 2.0
 */
public class DMSPlugin extends AbstractPlugin {

	public final static String DMS_ITEM = "dms";
	public final static String DMS_FILE_NAMES = "dms_names"; // list of files
	public final static String DMS_FILE_COUNT = "dms_count"; // count of files
	public final static String DMS_FILE_OCR = "dms_ocr"; // not yet in use!

	public final static String DMS_IMPORT_PROPERTY = "txtDmsImport";
	public final static String DEFAULT_PROTOCOLL = "file://";
	public final static String BLOBWORKITEMID = "$BlobWorkitem";
	public final static String CHECKSUM_ERROR = "CHECKSUM_ERROR";
	public final static String FILE_IMPORT_ERROR = "FILE_IMPORT_ERROR";

	ItemCollection workitem = null;
	private ItemCollection blobWorkitem = null;
	private static Logger logger = Logger.getLogger(DMSPlugin.class.getName());

	/**
	 * Update the read and writeAccess of the blobWorkitem and generates the dms
	 * meta data item. If no blobWorkitem still exists for the current workitem,
	 * the method creates the blobWorkitem automatically.
	 * 
	 * If a file is contained in the property '$file', which is not yet part of
	 * the property 'dms' the method will automatically create a new dms entry
	 * and calculates a MD5 checksum for the file content. <br>
	 * <br>
	 * If the workItem contains the property 'txtDmsImport" all files from the
	 * given path will be added into the blobWorkitem
	 * 
	 * 
	 * @throws PluginException
	 * @version 1.0s
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

		workitem = documentContext;

		// skip if type is not workitem and workitemarchive
		if (!workitem.getType().equals("workitem")
		 && !workitem.getType().equals("workitemarchive")
		 && !workitem.getType().equals("childworkitem")
		 && !workitem.getType().equals("childworkitemarchive")
				
				) {
			return workitem;
		}

		// Skip if plugin this processing a new version created by the version
		// plugin - in this case no changes to the version are needed
		if (VersionPlugin.isProcssingVersion(workitem)) {
			return workitem;
		}

		// load the blobWorkitem and update read- and write access
		blobWorkitem = loadBlobWorkitem(workitem);

		if (blobWorkitem == null) {
			logger.warning(
					"can't access blobworkitem for '" + workitem.getUniqueID() + "'");
			return workitem;
		}

		// import files if txtDmsImport is defined.
		if (documentContext.hasItem(DMS_IMPORT_PROPERTY)) {
			importFilesFromPath(workitem, blobWorkitem, documentContext.getItemValue(DMS_IMPORT_PROPERTY));
			// remove property
			documentContext.removeItem(DMS_IMPORT_PROPERTY);
		}

		// update the dms list - e.g. if another plugin had added a file....
		// the blob workitem will only be saved in case any changes were
		// performed...
		try {
			boolean updateBlob = false;
			updateBlob = updateDmsList(workitem, blobWorkitem, this.getWorkflowService().getUserName());
			// Update Read and write access list from parent workItem

			if (!workitem.getItemValue(WorkflowService.READACCESS)
					.equals(blobWorkitem.getItemValue(WorkflowService.READACCESS))) {
				blobWorkitem.replaceItemValue(WorkflowService.READACCESS,
						workitem.getItemValue(WorkflowService.READACCESS));
				updateBlob = true;
			}

			if (!workitem.getItemValue(WorkflowService.WRITEACCESS)
					.equals(blobWorkitem.getItemValue(WorkflowService.WRITEACCESS))) {
				blobWorkitem.replaceItemValue(WorkflowService.WRITEACCESS,
						workitem.getItemValue(WorkflowService.WRITEACCESS));
				updateBlob = true;
			}
			// save BlobWorkitem...
			if (updateBlob) {
				logger.fine(
						"saving blobWorkitem '" + blobWorkitem.getUniqueID() + "'...");
				blobWorkitem = getWorkflowService().getDocumentService().save(blobWorkitem);
			}
		} catch (NoSuchAlgorithmException e) {
			logger.severe("failed to compute MD5 checksum: " + documentContext.getUniqueID() + " - " + e.getMessage());
			throw new PluginException(DMSPlugin.class.getSimpleName(), CHECKSUM_ERROR,
					"failed to compute MD5 checksum: " + documentContext.getUniqueID() + "(" + e.getMessage() + ")", e);
		}

		logger.fine("updating $readaccess/$writeaccess for " + workitem.getUniqueID());

		// update property '$BlobWorkitem'
		workitem.replaceItemValue(BLOBWORKITEMID, blobWorkitem.getUniqueID());
		// add $filecount
		workitem.replaceItemValue(DMS_FILE_COUNT, workitem.getFileNames().size());
		// add $filenames
		workitem.replaceItemValue(DMS_FILE_NAMES, workitem.getFileNames());

		return workitem;
	}

	/**
	 * This method returns a list of ItemCollections for all DMS elements
	 * attached to the current workitem. The DMS meta data is read from the
	 * property 'dms'.
	 * 
	 * The dms property is updated in the run() method of this plug-in.
	 * 
	 * The method is used by the DmsController to display the dms meta data.
	 * 
	 * @param workitem
	 *            - source of meta data, sorted by $creation
	 * @version 1.0
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<ItemCollection> getDmsList(ItemCollection workitem) {
		// build a new fileList and test if each file contained in the $files is
		// listed
		List<ItemCollection> dmsList = new ArrayList<ItemCollection>();
		if (workitem == null)
			return dmsList;

		List<Map> vDMS = workitem.getItemValue(DMS_ITEM);
		// first we add all existing dms informations
		for (Map aMetadata : vDMS) {
			dmsList.add(new ItemCollection(aMetadata));
		}

		// sort list by name
		// Collections.sort(dmsList, new ItemCollectionComparator("txtname",
		// true));
		// sort list by $modified
		Collections.sort(dmsList, new ItemCollectionComparator("$created", true));

		return dmsList;
	}

	/**
	 * This method converts a list of ItemCollections for DMS elements into Map
	 * objects and updates the workitem property 'dms'.
	 * 
	 * The method is used by the DmsController to update dms data provided by
	 * the user.
	 * 
	 * @param workitem
	 *            - the workitem to be updated
	 * @param dmsList
	 *            - the dms metha data to be put into the workitem
	 * @version 1.0
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
		workitem.replaceItemValue(DMS_ITEM, vDMSnew);
	}

	/**
	 * This method adds a new entry into the dms property. The method returns
	 * the updated DMS List.
	 * 
	 * The method is used by the DMSController to add links.
	 * 
	 * @param aworkitem
	 *            - the workitem to be updated
	 * @param dmsEntity
	 *            - the metha data to be added into the dms item
	 * @version 1.0
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
				logger.fine("remove dms entry '" + sName + "'");
			}
		}
		dmsList.add(dmsEntity);
		putDmsList(aworkitem, dmsList);

		return dmsList;
	}

	/**
	 * This method transfers new file content into the blobWorkitem and updates
	 * the property 'dms' of the current workitem with the meta data of attached
	 * files or links.
	 * 
	 * This method creates new empty DMS entries in the current workitem for new
	 * uploaded files which are still not contained in the dms item list. The
	 * workitem will only hold a empty byte array for files.
	 * 
	 * If a new file content was added, the MD5 checksum will be generated.
	 * 
	 * If the content of the blobWorkitem was updated, the method returns true.
	 * 
	 * @param aWorkitem
	 * @param dmsList
	 *            - map with meta information for each file entry
	 * @param defaultUsername
	 *            - default username for new dms entries
	 * @return true if the dms item was changed
	 * @throws NoSuchAlgorithmException
	 * 
	 */
	private boolean updateDmsList(ItemCollection aWorkitem, ItemCollection blobWorkitem, String defaultUsername)
			throws NoSuchAlgorithmException {

		boolean updateBlob = false;
		List<ItemCollection> currentDmsList = getDmsList(aWorkitem);
		List<String> fileNames = aWorkitem.getFileNames();
		Map<String, List<Object>> files = aWorkitem.getFiles();

		// first we remove all DMS entries which did not have a matching
		// $File-Entry and are not from type link
		if (fileNames == null) {
			fileNames = new ArrayList<String>();
		}
		for (Iterator<ItemCollection> iterator = currentDmsList.iterator(); iterator.hasNext();) {
			ItemCollection dmsEntry = iterator.next();
			String sName = dmsEntry.getItemValueString("txtName");
			String sURL = dmsEntry.getItemValueString("url");
			if (sURL.isEmpty() && !fileNames.contains(sName)) {
				// Remove the current element from the iterator and the list.
				logger.fine("remove dms entry '" + sName + "'");
				iterator.remove();
				// update = true;
			}
		}

		// now we test for each file entry if a dms meta data entry
		// exists. If not we create a new one...
		if (files != null) {
			for (Entry<String, List<Object>> entry : files.entrySet()) {
				String aFilename = entry.getKey();
				List<?> file = entry.getValue();

				// if data size >0 transfer file into blob and create a MD5
				// Checksum
				if (file.size() >= 2) {
					String contentType = (String) file.get(0);
					byte[] fileContent = (byte[]) file.get(1);
					if (fileContent != null && fileContent.length > 1) {
						// move...
						blobWorkitem.addFile(fileContent, aFilename, contentType);
						// empty data...
						byte[] empty = { 0 };
						// add the file name (with empty data) into the
						// parentWorkitem.
						aWorkitem.addFile(empty, aFilename, contentType);
						updateBlob = true;
					}
				}

				ItemCollection dmsEntry = findDMSEntry(aFilename, currentDmsList);
				if (dmsEntry == null) {
					// no meta data exists.... create a new meta object
					dmsEntry = new ItemCollection();
					dmsEntry.replaceItemValue("txtname", aFilename);
					dmsEntry.replaceItemValue("$uniqueidRef", blobWorkitem.getUniqueID());
					dmsEntry.replaceItemValue("$created", new Date());
					dmsEntry.replaceItemValue("namCreator", defaultUsername);// deprecated
					dmsEntry.replaceItemValue("$Creator", defaultUsername);
					dmsEntry.replaceItemValue("txtcomment", "");

					// compute md5 checksum
					byte[] fileContent = (byte[]) file.get(1);
					dmsEntry.replaceItemValue("md5Checksum", generateMD5(fileContent));
					currentDmsList.add(dmsEntry);
				} else {
					// dms entry exists. We update if new file content was added
					byte[] fileContent = (byte[]) file.get(1);
					if (fileContent != null && fileContent.length > 1) {
						dmsEntry.replaceItemValue("md5Checksum", generateMD5(fileContent));
						dmsEntry.replaceItemValue("$modified", new Date());
						dmsEntry.replaceItemValue("$editor", defaultUsername);
						dmsEntry.replaceItemValue("$uniqueidRef", blobWorkitem.getUniqueID());

						// update dmsEntry in dmsList..
						// ??? currentDmsList.f..remove(o)

					}

				}

			}
		}

		// now we remove all files form the blobWorkitem which are not part of
		// the workitem
		List<String> currentFileNames = aWorkitem.getFileNames();
		List<String> blobFileNames = blobWorkitem.getFileNames();
		List<String> removeList = new ArrayList<String>();
		for (String aname : blobFileNames) {
			if (!currentFileNames.contains(aname)) {
				removeList.add(aname);
			}
		}

		if (removeList.size() > 0) {
			for (String aname : removeList) {
				blobWorkitem.removeFile(aname);
			}
			updateBlob = true;
		}

		// finally update the modified dms list....
		putDmsList(aWorkitem, currentDmsList);

		return updateBlob;
	}

	/**
	 * This method loads the BlobWorkitem for a given parent WorkItem. The
	 * BlobWorkitem is identified by the $unqiueidRef.
	 * 
	 * If no BlobWorkitem still exists the method creates a new empty
	 * BlobWorkitem which can be saved later.
	 * 
	 * @param parentWorkitem
	 *            - the corresponding parent workitem
	 * @version 1.0
	 */
	private ItemCollection loadBlobWorkitem(ItemCollection parentWorkitem) {
		ItemCollection blobWorkitem = null;

		// is parentWorkitem defined?
		if (parentWorkitem == null) {
			logger.warning("Unable to load blobWorkitem from parent workitem == null!");
			return null;
		}

		// try to load the blobWorkitem with the parentWorktiem reference....
		String sUniqueID = parentWorkitem.getUniqueID();
		if (!"".equals(sUniqueID)) {
			// search entity...
			String sQuery = "(type:\"workitemlob\" AND $uniqueidref:\"" + sUniqueID + "\")";

			Collection<ItemCollection> itemcol = null;
			try {
				itemcol = getWorkflowService().getDocumentService().find(sQuery, 1, 0);
			} catch (QueryException e) {
				logger.severe("loadBlobWorkitem - invalid query: " + e.getMessage());
			}
			// if blobWorkItem was found return...
			if (itemcol != null && itemcol.size() > 0) {
				blobWorkitem = itemcol.iterator().next();
			}

		} else {
			logger.fine("generating inital $uniqueId  for new parent workitem...");
			// no $uniqueId set - create a UniqueID for the parentWorkitem
			parentWorkitem.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		}
		// if no blobWorkitem was found, create a empty itemCollection..
		if (blobWorkitem == null) {
			logger.fine("creating new blobWorkitem...");
			blobWorkitem = new ItemCollection();
			blobWorkitem.replaceItemValue("type", "workitemlob");
			// generate default uniqueid...
			blobWorkitem.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
			blobWorkitem.replaceItemValue("$UniqueidRef", parentWorkitem.getUniqueID());

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
	 * Generates a MD5 from a byte array
	 * 
	 * @param b
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	private static String generateMD5(byte[] b) throws NoSuchAlgorithmException {
		byte[] hash_bytes = MessageDigest.getInstance("MD5").digest(b);
		return DatatypeConverter.printHexBinary(hash_bytes);
	}

	/**
	 * Import files from a given location.
	 * 
	 * @param aWorkitem
	 * @param importList
	 *            - list of files
	 * @throws PluginException
	 * 
	 */
	private void importFilesFromPath(ItemCollection adocumentContext, ItemCollection blobWorkitem,
			List<String> importList) throws PluginException {

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
			logger.info("importFilesFromPath: " + fullFileUri);
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

				logger.info("file import successfull ");

			} catch (MalformedURLException e) {
				throw new PluginException(DMSPlugin.class.getSimpleName(), FILE_IMPORT_ERROR,
						"error importing files from: " + fullFileUri, e);
			} catch (IOException e) {
				throw new PluginException(DMSPlugin.class.getSimpleName(), FILE_IMPORT_ERROR,
						"error importing files from: " + fullFileUri, e);
			}

		}

	}

}
