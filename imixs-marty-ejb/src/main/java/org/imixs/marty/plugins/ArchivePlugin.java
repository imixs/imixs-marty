package org.imixs.marty.plugins;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.xml.XMLItemCollection;
import org.imixs.workflow.xml.XMLItemCollectionAdapter;

/**
 * The Archive Plug-in stores workitems to disk when reached the type
 * 'workitemarchive'. The plug-in reads properties to define the target
 * filesystem.
 * 
 * archive.path = output directory
 * 
 * 
 * @author rsoika
 * 
 */
public class ArchivePlugin extends AbstractPlugin {

	public final static String IO_ERROR = "IO_ERROR";
	public final static String DEFAULT_PROTOCOLL = "file://";
	public final static String BLOBWORKITEMID = "$BlobWorkitem";

	private static Logger logger = Logger.getLogger(ArchivePlugin.class.getName());

	/**
	 * Archive if workitem type changed to 'workitemarchive'
	 * 
	 * Therefore we compare the current task with the next task object.
	 * 
	 * @throws PluginException
	 * 
	 **/
	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection event) throws PluginException {
		Map<String, List<Object>> files = null;

		// get numNextProcessID and modelVersion
		int iNextProcessID = event.getItemValueInteger("numNextProcessID");
		int iProcessID = documentContext.getItemValueInteger("$processid");
		ItemCollection currentTask = null;
		ItemCollection nextTask = null;

		// get task objects from model version
		String modelVersion = event.getItemValueString("$modelVersion");

		try {

			currentTask = getCtx().getModelManager().getModel(modelVersion).getTask(iProcessID);
			nextTask = getCtx().getModelManager().getModel(modelVersion).getTask(iNextProcessID);
		} catch (ModelException e) {
			logger.warning("Warning - Task '" + iNextProcessID + "' is not defined by model version '" + modelVersion
					+ "' : " + e.getMessage());
			return documentContext;
		}
		// check if target task type is "workitemarchive" or target type isEmpty
		// and current type is "workitemarchive".
		// note: we need to compare the task objects, because the
		// documentContext can already be updated by the ApplicationPlugin!
		String targetTaskType = nextTask.getItemValueString("txttype");
		if ("workitemarchive".equals(targetTaskType)
				|| (targetTaskType.isEmpty() && "workitemarchive".equals(currentTask.getItemValueString("txttype")))) {
			// run archive mode!

			String archivePath = computeArchivePath(documentContext);

			// load the blobWorkitem to get the filelist
			ItemCollection blobWorkitem = BlobWorkitemHandler.load(this.getWorkflowService().getDocumentService(),
					documentContext);

			if (blobWorkitem != null) {
				files = blobWorkitem.getFiles();
			} else {
				// no blobworkitem - so we got the files form the current
				// documentContext
				files = documentContext.getFiles();
			}

			writeFiles(archivePath, files);

			// clone workitem and remove file content if available...
			ItemCollection clone = (ItemCollection) documentContext.clone();
			clone.replaceItemValue("$file", getCleanFileContent(files));
			// convert the ItemCollection into a XMLItemcollection...
			XMLItemCollection xmlItemCollection;
			try {
				xmlItemCollection = XMLItemCollectionAdapter.putItemCollection(clone);

				// marshal the Object into an XML Stream....
				JAXBContext context = JAXBContext.newInstance(XMLItemCollection.class);
				Marshaller m = context.createMarshaller();

				Path file = Paths.get(archivePath + "document.xml");
				m.marshal(xmlItemCollection, file.toFile());
			} catch (Exception e) {
				logger.severe("failed to archive document " + documentContext.getUniqueID() + " - " + e.getMessage());
				throw new PluginException(ArchivePlugin.class.getSimpleName(), IO_ERROR,
						"failed to archive document " + documentContext.getUniqueID() + "(" + e.getMessage() + ")", e);
			}
			logger.fine("Document '" + documentContext.getItemValueString(WorkflowKernel.UNIQUEID)
					+ "' sucessfull archived");

		}

		return documentContext;
	}

	/**
	 * This method writes the files assigned to the current workitem to disk
	 * 
	 * @throws PluginException
	 * 
	 */
	@SuppressWarnings("unused")
	private void writeFiles(String archivePath, Map<String, List<Object>> files) throws PluginException {

		if (files == null) {
			return;
		}
		// iterate over files..
		for (Entry<String, List<Object>> entry : files.entrySet()) {
			String sFileName = archivePath + entry.getKey();
			List<?> file = entry.getValue();

			// if data size >0 transfer file to filesystem
			if (file.size() >= 2) {
				String contentType = (String) file.get(0);
				byte[] data = (byte[]) file.get(1);
				if (data != null && data.length > 1) {
					// write file...

					logger.fine("archive file " + sFileName);
					Path newfile = Paths.get(sFileName);

					try {
						Files.write(newfile, data);
					} catch (IOException e) {
						logger.severe("Unable to archive file " + sFileName + " - " + e.getMessage());
						throw new PluginException(ArchivePlugin.class.getSimpleName(), IO_ERROR,
								"failed to write file  " + sFileName + " to disk (" + e.getMessage() + ")", e);
					}

				}
			}
		}

	}

	/**
	 * This method constructs the archive path form the imixs.properties
	 * archive.path and the workitem properties $modified, $uniqueid and
	 * txtWorkflowGroup
	 * 
	 * /ARCHIVE_PATH/YYYY/WORKFLOWGROUP/UNIQUEID/
	 * 
	 * @return
	 * @throws PluginException
	 */
	private String computeArchivePath(ItemCollection documentContext) throws PluginException {
		String archivePath = this.getWorkflowService().getPropertyService().getProperties().getProperty("archive.path",
				"archive");
		if (archivePath.endsWith(FileSystems.getDefault().getSeparator())) {
			archivePath = archivePath + FileSystems.getDefault().getSeparator();
		}

		// build path YEAR/WORKFLOWGRUP/UNIQUEID

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy");
		Date modified = documentContext.getItemValueDate("$modified");
		if (modified == null) {
			logger.warning("Document " + documentContext.getUniqueID() + " does not provide $modified!");
			modified = new Date();
		}
		String group = documentContext.getItemValueString("txtworkflowgroup");
		if (group.isEmpty()) {
			logger.warning("Document " + documentContext.getUniqueID() + " does not provide txtworkflowgroup!");
			group = "default";
		}
		archivePath = archivePath + FileSystems.getDefault().getSeparator() + formatter.format(modified)
				+ FileSystems.getDefault().getSeparator() + group + FileSystems.getDefault().getSeparator()
				+ documentContext.getUniqueID() + FileSystems.getDefault().getSeparator();

		logger.finest("archive path = " + archivePath);

		// now create dirs...
		try {
			Files.createDirectories(Paths.get(archivePath));
		} catch (IOException e) {
			logger.severe("failed to create archive directory '" + archivePath + "' - " + e.getMessage());
			throw new PluginException(ArchivePlugin.class.getSimpleName(), IO_ERROR,
					"failed to create archive directory '" + archivePath + "' (" + e.getMessage() + ")", e);

		}

		return archivePath;
	}

	/**
	 * This method clears the File content of the workitem if available. We
	 * don't want to write the files into the xml stream.
	 * 
	 * @param aWorkitem
	 * @param aBlobWorkitem
	 */
	private Map<String, List<Object>> getCleanFileContent(Map<String, List<Object>> files) {

		Map<String, List<Object>> cleanFileList = new HashMap<String, List<Object>>();

		if (files == null) {
			return cleanFileList;
		}

		for (Entry<String, List<Object>> entry : files.entrySet()) {
			String sFileName = entry.getKey();
			List<?> file = entry.getValue();

			// if data size >0 transfer file into blob
			if (file.size() >= 2) {
				String contentType = (String) file.get(0);
				byte[] empty = { 0 };
				List<Object> list = new ArrayList<Object>();
				list.add(contentType);
				list.add(empty);
				cleanFileList.put(sFileName, list);
			}
		}
		return cleanFileList;
	}

}
