package org.imixs.marty.plugins.minutes;

import java.util.List;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.engine.WorkflowService;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * This Plugin controles minute items of a parent workflow.
 * <p>
 * A Minute Item can be of any type (e.g. 'workitem' or 'childworkitem') The
 * plugin number all minute items.
 * <p>
 * When a version is processed, than the plugin copies all workitems from the
 * master with the types 'workitem' or 'childworkitem' into the new version and
 * renumbers the minute items (sequencenumber).
 * <p>
 * When a new minute item is created or has no sequencenumber, the plugin
 * computes the next sequence number.
 * 
 * @author rsoika
 * @version 2.0
 * 
 */
public class MinutePlugin extends AbstractPlugin {

	// private SystemWorkitemService systemWorkitemService = null;
	private static Logger logger = Logger.getLogger(MinutePlugin.class.getName());

	public final static String MINUTE_TYPE_PARENT = "minuteparent";
	public final static String MINUTE_TYPE_ITEM = "minuteitem";

	public void init(WorkflowContext actx) throws PluginException {
		super.init(actx);

		// lookup profile service EJB
		// String jndiName = "ejb/SystemWorkitemService";
		// InitialContext ictx;
		// try {
		// ictx = new InitialContext();
		// Context ctx = (Context) ictx.lookup("java:comp/env");
		// systemWorkitemService = (SystemWorkitemService) ctx.lookup(jndiName);
		//
		// } catch (NamingException e) {
		// throw new PluginException(MinutePlugin.class.getSimpleName(),
		// this.INVALID_ITEMVALUE_FORMAT,
		// "unable to find SystemWorkitemService", e);
		// }

	}

	/**
	 * The method verifies if a sequencenumber is set. If not a new sequencenumber
	 * is computed.
	 * <p>
	 * If a version was created, all workitems from the master with the types
	 * 'workitem' or 'childworkitem' are copied into the new version. . In this case
	 * the minute items are renumbered. *
	 * 
	 * @return
	 * @throws PluginException
	 * @throws AddressException
	 */
	@Override
	public ItemCollection run(ItemCollection documentContext, ItemCollection documentActivity) throws PluginException {

		// skip if it is no workitem or a 'numsequencenumber' already defined
		if (documentContext.hasItem("numsequencenumber") || !(documentContext.getType().contains("workitem"))) {
			logger.fine("skip computeSequenceNumber - number already exits");
			return documentContext;
		}

		// update the attribute minuteType)
		String minuteType = updateMinuteType(documentContext);

		if (MINUTE_TYPE_ITEM.equals(minuteType) && documentContext.getItemValueInteger("numsequencenumber") <= 0) {
			// Compute a sequencenumber for new child workitems
			computeNextSequenceNumber(documentContext);
		}

		if (MINUTE_TYPE_PARENT.equals(minuteType)) {
			// do something...
			logger.warning("CODE MISSING!");
		}

		// Versioning....
		// documentContext = super.run(documentContext, documentActivity);

		// check if a Version was created

		// recompute the action - we whant to change to the new version..

		return documentContext;
	}

	/**
	 * This method verifies if the current workitem has already a minutetype. If not
	 * the minutetype will be updated and set to 'minuteitem' or 'minuteparent'. If
	 * the workitem is a minute item, the minute parent uniqueid will be stored into
	 * the filed 'minuteParentRef'.
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	private String updateMinuteType(ItemCollection documentContext) {
		ItemCollection parent = null;
		String minutetype = documentContext.getItemValueString("minutetype");

		if (!minutetype.isEmpty()) {
			// we already identified this type..
			return minutetype;
		}

		// we have not yet computed, so we need to verify it.
		boolean foundMinuteParent = false;
		List<String> uniqueIdRefList = documentContext.getItemValue(WorkflowService.UNIQUEIDREF);
		for (String id : uniqueIdRefList) {
			parent = this.getWorkflowService().getWorkItem(id);
			if (parent != null) {
				// test if a parent workitem exits
				if (MINUTE_TYPE_PARENT.equals(parent.getItemValueString("minutetype"))) {
					foundMinuteParent = true;
					break;
				}
			}
		}

		if (foundMinuteParent) {
			// it is a minute item!
			documentContext.replaceItemValue("minutetype", MINUTE_TYPE_ITEM);
			documentContext.replaceItemValue("minuteParentRef", parent.getUniqueID());
			return MINUTE_TYPE_ITEM;
		} else {
			// Mark this workitem as a MINUTE_TYPE_PARENT
			logger.fine("mark workitem as minute parent");
			documentContext.replaceItemValue("minutetype", MINUTE_TYPE_PARENT);
			// Temporally store ....
			// This is because in the case that a minute-item was created (splitPlugin)
			// before the parent was saved the first time (first process step), a lookup for
			// this parent later did not return the parent.
			// The DocumentService is clever enough to handle this case :-)
			this.getWorkflowService().getDocumentService().save(documentContext);
			return MINUTE_TYPE_PARENT;
		}
	}

	/**
	 * This method compute a new numsequencenumber for a minute item. The next
	 * number is computed based on the count numbered minute items.
	 * 
	 */
	private void computeNextSequenceNumber(ItemCollection documentContext) {
		// test if minute parent workitem exits
		ItemCollection parent = this.getWorkflowService()
				.getWorkItem(documentContext.getItemValueString("minuteParentRef"));
		int nummer = 1;
		if (parent != null) {
			// find all minute itmes and comptue the next Number...
			List<ItemCollection> childs = this.getWorkflowService().getWorkListByRef(parent.getUniqueID());
			for (ItemCollection minute : childs) {
				if (minute != null && MINUTE_TYPE_ITEM.equals(minute.getItemValueString("minutetype"))
						&& minute.getItemValueInteger("numsequencenumber") > 0) {
					nummer++;
				}
			}
			documentContext.replaceItemValue("numsequencenumber", nummer);
		}

	}
}