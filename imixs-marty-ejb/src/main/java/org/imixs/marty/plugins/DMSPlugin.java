package org.imixs.marty.plugins;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;
import org.imixs.workflow.exceptions.PluginException;

/**
 * The DMSPlugin is deprecated!
 * 
 * @see SnapshotService
 * 
 * @version 1.0
 * @author rsoika
 */
public class DMSPlugin extends AbstractPlugin {
	private static Logger logger = Logger.getLogger(DMSPlugin.class.getName());

	/**
	 * This plugin is deprecated. It is replaced by the Imixs-Archive SnapshotService. 
	 * 
	 * @throws PluginException
	 */
	@Override
	public ItemCollection run(ItemCollection document, ItemCollection event) throws PluginException {

		logger.warning("The DMSPlugin is deprected and can be removed from this model!");
		return document;

	}

}