package org.imixs.marty.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * 
 * The Plugin computes the read and write access for the blobworkitem attached
 * to the processed workItem. The Plugin should run immediate after the
 * AccessPlugin.
 * 
 * The Plugin only runs in workItems type=workitem or type=workitemarchive
 * 
 * The plugin provides additional static methods to set and get the 'dms'
 * property for a workitem. This property stores meta information for uploaded
 * filed.
 * 
 * 
 * If the workitem contains the property 'txtDmsImport" all files from the given
 * path will be added into the blobWorkitem
 * 
 * @author rsoika
 * 
 */
public class DMSPlugin extends AbstractPlugin {

	public final static String DMS_IMPORT_PROPERTY = "txtDmsImport";
	public final static String DEFAULT_PROTOCOLL = "file://";

	private EntityService entityService = null;
	ItemCollection workitem = null;
	private ItemCollection blobWorkitem = null;
	private static Logger logger = Logger.getLogger(DMSPlugin.class.getName());

	@Override
	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);
		// check for an instance of WorkflowService
		if (actx instanceof WorkflowService) {
			// yes we are running in a WorkflowService EJB
			WorkflowService ws = (WorkflowService) actx;
			entityService = ws.getEntityService();
		}

	}

	/**
	 * Update the read and writeaccess of the blobworkitem
	 * 
	 * If the workitem contains the property 'txtDmsImport" all files from the
	 * given path will be added into the blobWorkitem
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection aworkItem, ItemCollection documentActivity) {

		workitem = aworkItem;

		// skip if the workitem is from a different type (for example Teams
		// may not be processed by this plugin)
		String type = workitem.getItemValueString("type");
		if (!type.startsWith("workitem") && !type.startsWith("workitemarchive"))
			return Plugin.PLUGIN_OK;

		// load the blobWorkitem and update read- and write access
		blobWorkitem = loadBlobWorkitem(workitem);
		if (blobWorkitem != null) {

			// import files if txtDmsImport is defined.
			if (aworkItem.hasItem(DMS_IMPORT_PROPERTY)) {
				importFilesFromPath(workitem,blobWorkitem,
						aworkItem.getItemValue(DMS_IMPORT_PROPERTY));
				// remove proeprty
				aworkItem.removeItem(DMS_IMPORT_PROPERTY);
			}

			logger.fine("[BlobPlugin] updating $readaccess/$writeaccess for "
					+ workitem.getItemValueString(EntityService.UNIQUEID));

			// Update Read and write access list from parent workitem
			List<?> vAccess = workitem.getItemValue("$ReadAccess");
			blobWorkitem.replaceItemValue("$ReadAccess", vAccess);

			vAccess = workitem.getItemValue("$WriteAccess");
			blobWorkitem.replaceItemValue("$WriteAccess", vAccess);

			blobWorkitem.replaceItemValue("$uniqueidRef",
					workitem.getItemValueString(EntityService.UNIQUEID));
			blobWorkitem.replaceItemValue("type", "workitemlob");

			// Update BlobWorkitem
			// blobWorkitem = entityService.save(blobWorkitem);
			// new transaction....
			blobWorkitem = this.getEjbSessionContext()
					.getBusinessObject(WorkflowService.class)
					.getEntityService().saveByNewTransaction(blobWorkitem);

			// update property '$BlobWorkitem'
			workitem.replaceItemValue("$BlobWorkitem",
					blobWorkitem.getItemValueString(EntityService.UNIQUEID));
		}

		// update the dms list - e.g. if another plugin had added a file....
		updateDMSList(workitem);

		return Plugin.PLUGIN_OK;
	}

	public void close(int arg0) {
		// no op

	}

	/**
	 * This method creates a new dmsList and reads all existing dms meta data
	 * from the current workItem. The meta data is read from the property 'dms'.
	 * 
	 * If a file contained in the property '$file' is not part of the property
	 * 'dms' the method will automatically create a new dms entry by calling the
	 * method updateDmsList.
	 * 
	 * The dms property is saved during processing the workiItem.
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

		return dmsList;
	}

	/**
	 * Stores a given List of Meta information stored in ItemCollections into a
	 * List of Map Entries
	 * 
	 * This method also adds empty dms entries for new uploaded files or
	 * filesnames which are still not contained in the dms list.
	 * 
	 * 
	 * @param aWorkitem
	 * @param dmsList
	 *            - map with meta information for each file entry
	 * @param username
	 *            - optional username for new file entries
	 * @param comment
	 *            - optional comment for new file entries
	 * 
	 * */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void setDmsList(ItemCollection aWorkitem,
			List<ItemCollection> dmsList, String username, String comment) {
		Vector<Map> vDMSnew = new Vector<Map>();
		for (ItemCollection aEntry : dmsList) {
			vDMSnew.add(aEntry.getAllItems());
		}

		// check if some files are missing.....
		List<String> files = aWorkitem.getFileNames();

		String blobWorkitemId = aWorkitem.getItemValueString("$BlobWorkitem");
		if (blobWorkitemId.isEmpty()) {
			// WARNING we have no $BlobWorkitem - link can not be computed
			logger.warning("[DMSPlugin] Workitem '"
					+ aWorkitem.getItemValueString(EntityService.UNIQUEID)
					+ "' $BlobWorkitem in is empty. File URL can not be computed!");
		}

		// now we test for each file entry if a dms meta data entry still
		// exists. If not we create a new one...
		for (String aFilename : files) {

			// test filename already exists
			Map itemCol = findMetadata(aFilename, vDMSnew);
			if (itemCol == null) {
				// no meta data exists.... create a new meta object
				Map dmsEntry = new HashMap();
				dmsEntry.put("txtname", aFilename);
				dmsEntry.put("$uniqueidRef", blobWorkitemId);
				dmsEntry.put("$created", new Date());
				dmsEntry.put("namCreator", username);
				dmsEntry.put("txtcomment", comment);
				vDMSnew.add(dmsEntry);
			}

		}

		aWorkitem.replaceItemValue("dms", vDMSnew);
	}

	/**
	 * Stores a given List of Meta information stored in ItemCollections into a
	 * List of Map Entries
	 * 
	 * This method also adds empty dms entries for new uploaded files or
	 * filesnames which are still not contained in the dms list. Username and
	 * comment will be empty for these entries. see optional method
	 * 
	 * 
	 * @param aWorkitem
	 * @param dmsList
	 *            - map with meta information for each file entry
	 * 
	 * */
	public static void setDmsList(ItemCollection aWorkitem,
			List<ItemCollection> dmsList) {
		setDmsList(aWorkitem, dmsList, null, null);
	}

	/**
	 * This Method updates the DMS entry. The method verifies if a new file was
	 * added (e.g. from the ReportPlugin). If the $files contains a filename not
	 * listed in the current DMS entry the method updates this prperty.
	 * 
	 */
	private void updateDMSList(ItemCollection aWorkitem) {
		List<ItemCollection> dmsList = getDmsList(aWorkitem);
		setDmsList(aWorkitem, dmsList);
	}

	/**
	 * This method loads the BlobWorkitem for a given parent WorkItem. The
	 * BlobWorkitem is identified by the $unqiueidRef. If no BlobWorkitem still
	 * exists the method creates a new empty BlobWorkitem which can be saved
	 * later.
	 * 
	 * 
	 * 
	 */
	private ItemCollection loadBlobWorkitem(ItemCollection parentWorkitem) {
		ItemCollection blobWorkitem = null;

		// is parentWorkitem defined?
		if (parentWorkitem == null)
			return null;

		// try to load the blobWorkitem with the parentWorktiem reference....
		String sUniqueID = parentWorkitem.getItemValueString("$uniqueid");
		if (!"".equals(sUniqueID)) {
			// search entity...
			String sQuery = " SELECT lobitem FROM Entity as lobitem"
					+ " join lobitem.textItems as t2"
					+ " WHERE lobitem.type = 'workitemlob'"
					+ " AND t2.itemName = '$uniqueidref'"
					+ " AND t2.itemValue = '" + sUniqueID + "'";

			Collection<ItemCollection> itemcol = entityService.findAllEntities(
					sQuery, 0, 1);
			// if blobWorkItem was found return...
			if (itemcol != null && itemcol.size() > 0) {
				blobWorkitem = itemcol.iterator().next();

			}

		} else {
			// no $uniqueId set - create a UniqueID for the parentWorkitem
			parentWorkitem.replaceItemValue(EntityService.UNIQUEID,
					WorkflowKernel.generateUniqueID());

		}
		// if no blobWorkitem was found, create a empty itemCollection..
		if (blobWorkitem == null) {
			blobWorkitem = new ItemCollection();

			blobWorkitem.replaceItemValue("type", "workitemlob");
			// generate default uniqueid...
			blobWorkitem.replaceItemValue(EntityService.UNIQUEID,
					WorkflowKernel.generateUniqueID());
			blobWorkitem.replaceItemValue("$UniqueidRef",
					parentWorkitem.getItemValueString(EntityService.UNIQUEID));

		}
		return blobWorkitem;

	}

	/**
	 * This method returns the meta data of a specific file in the exiting
	 * filelist.
	 * 
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static Map findMetadata(String aFilename, List<Map> dmsList) {

		for (Map mapEntry : dmsList) {
			// test if filename matches...
			ItemCollection itemCol = new ItemCollection(mapEntry);
			String sName = itemCol.getItemValueString("txtname");
			if (sName.equals(aFilename))
				return mapEntry;

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
	 * */
	private void importFilesFromPath(ItemCollection adocumentContext , ItemCollection blobWorkitem,
			List<String> importList) {

		for (String fileUri : importList) {

			if (fileUri.isEmpty()) {
				continue;
			}
			
			String fullFileUri=fileUri;
			String fileName=null;
			
			// check for a protocoll
			if (!fullFileUri.contains("://")) {
				fullFileUri=DEFAULT_PROTOCOLL+fullFileUri;
			}
			
			// extract fileame....
			if (!fullFileUri.contains("/")) {
				continue;
			}
			fileName=fullFileUri.substring(fullFileUri.lastIndexOf("/")+1);
			
			
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
