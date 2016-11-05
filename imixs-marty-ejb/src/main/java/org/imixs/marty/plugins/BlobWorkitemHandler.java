package org.imixs.marty.plugins;

import java.util.Collection;
import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.exceptions.QueryException;

/**
 * The BlobWorkitemHandler provides a method to load a BlobWorktiem for the
 * current DocumentContext.
 * 
 * @author rsoika
 * 
 */
public class BlobWorkitemHandler {

	public final static String BLOBWORKITEMID = "$BlobWorkitem";

	private static Logger logger = Logger.getLogger(BlobWorkitemHandler.class.getName());

	/**
	 * This method loads the BlobWorkitem for a given parent WorkItem. The
	 * BlobWorkitem is identified by the $unqiueidRef.
	 * 
	 * If no BlobWorkitem still exists the method returns null.
	 * 
	 */
	public static ItemCollection load(DocumentService documentService, ItemCollection parentWorkitem) {
		ItemCollection blobWorkitem = null;

		// is parentWorkitem defined?
		if (parentWorkitem == null)
			return null;

		String sUniqueID = parentWorkitem.getItemValueString(WorkflowService.UNIQUEID);

		// try to load the blobWorkitem with the parentWorktiem reference....
		if (!"".equals(sUniqueID)) {
			// search entity...
			String sQuery = "(type:\"workitemlob\" AND $uniqueidref:\"" + sUniqueID + "\")";

			Collection<ItemCollection> itemcol = null;
			try {
				itemcol = documentService.find(sQuery, 1, 0);
			} catch (QueryException e) {
				logger.severe("loadBlobWorkitem - invalid query: " + e.getMessage());
			}
			// if blobWorkItem was found return...
			if (itemcol != null && itemcol.size() > 0) {
				blobWorkitem = itemcol.iterator().next();
				// see also imixs-workflow issue #230
			}

		} else {
			logger.warning("no $uniqueId set for parentWorkitem!");
		}
		return blobWorkitem;
	}

}
