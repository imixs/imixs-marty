package org.imixs.marty.plugins;

import java.util.logging.Logger;

import org.imixs.workflow.ItemCollection;

/**
 * This plugin is deprecated an simply overwrite the Marty Deputy Plugin.
 * Because the plugin was used by older workflow models the plugin will not be
 * removed.
 * 
 * @author rsoika
 * @version 3.0
 * 
 */
@Deprecated
public class DeputyPlugin extends org.imixs.marty.profile.DeputyPlugin {

    private static Logger logger = Logger.getLogger(DeputyPlugin.class.getName());

    @Override
    public ItemCollection run(ItemCollection adocumentContext, ItemCollection documentActivity) {
        logger.warning(
                "This DeputyPlugin is deprecated and should be replaced with 'org.imixs.marty.profile.DeputyPlugin'");
        return super.run(adocumentContext, documentActivity);
    }
}
