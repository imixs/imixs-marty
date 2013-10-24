package org.imixs.marty.plugins;

import java.util.Collection;
import java.util.List;
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
 * This plugin computes the read and write access for the blobworkitem attached
 * to the processed workItem. The Plugin should run immediate after the
 * AccessPlugin.
 * 
 * 
 * The Plugin only runs in workItems type=workitem or type=workitemarchive
 * 
 * @author rsoika
 * 
 */
public class BlobPlugin extends AbstractPlugin {
	private EntityService entityService = null;
	ItemCollection workitem = null;
	private ItemCollection blobWorkitem = null;

	private static Logger logger = Logger.getLogger(BlobPlugin.class
			.getSimpleName());

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
	 **/
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
			blobWorkitem = entityService.save(blobWorkitem);
		}

		return Plugin.PLUGIN_OK;
	}

	public void close(int arg0) {
		// no op

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

}
