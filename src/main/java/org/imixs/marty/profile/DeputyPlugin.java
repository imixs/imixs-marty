package org.imixs.marty.profile;

import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jakarta.ejb.EJB;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.plugins.AbstractPlugin;

/**
 * This plugin computes for each name field (prefix = 'nam') if the
 * corresponding user profile contains a deputy. If so the deputy will be added
 * into the name field.
 * 
 * If a name Field (prafix = 'nam') is listed in the 'ignoreList' the field will
 * be skipped. The ignoreList can include regular expressions and can be
 * modified by a client.
 * 
 * The plugin runs on all kinds of workitems and childworkitems.
 * 
 * The Plugin should run after the TeamPlugin but before the ownerPlugin,
 * approverPlugin and accessPlugin
 * 
 * To avoid conflicts with the ApproverPlugin, the DeputyPlugin ignores fields
 * ending with 'approvers' and 'approvedby'.
 * 
 * @see https://github.com/imixs/imixs-marty/issues/130
 * 
 * 
 * @author rsoika
 * 
 */
public class DeputyPlugin extends AbstractPlugin {

    public static String PROFILESERVICE_NOT_BOUND = "PROFILESERVICE_NOT_BOUND";

    ItemCollection workitem = null;

    private String[] ignoreList = { "$creator", "$editor", "$lasteditor", "namcreator", "namcurrenteditor",
            "namlasteditor", "$owner", "namowner", "nam+(?:[a-z0-9_]+)approvers", "nam+(?:[a-z0-9_]+)approvedby" };

    private String[] supportList = { "\\.manager$", "\\.team$", "\\.assist$" };

    private static Logger logger = Logger.getLogger(DeputyPlugin.class.getName());

    @EJB
    ProfileService profileService;

    /**
     * iterate over all fields with the prefix nam...
     * 
     * Skip if not a workitem
     * 
     **/
    @SuppressWarnings("unchecked")
    @Override
    public ItemCollection run(ItemCollection aworkItem, ItemCollection documentActivity) {

        workitem = aworkItem;

        // skip if the workitem is from a different type (for example Teams
        // may not be processed by this plugin)
        String type = workitem.getItemValueString("type");

        if (!type.startsWith("workitem") && !type.startsWith("childworkitem"))
            return workitem;

        // iterate over name fields
        Map<String, List<Object>> map = workitem.getAllItems();
        for (String itemName : map.keySet()) {
            if (ignoreItem(itemName)) {
                continue;
            }

            // lookup deputies
            logger.fine("... lookup=" + itemName);
            List<String> oldNameList = workitem.getItemValue(itemName);
            List<String> newNameList = updateDeputies(oldNameList);
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("... new list=");
                for (String aentry : newNameList) {
                    logger.fine(aentry);
                }
            }
            // update field
            workitem.replaceItemValue(itemName, newNameList);
        }

        return workitem;
    }

    /**
     * This method updates a given list of names. For each name the method lookups a
     * deputy. If a deputy is defined but not par of the list he will be added to
     * the new list.
     * 
     * @param sourceNameList - source list of names
     * @return new list with all names plus deputies.
     */
    @SuppressWarnings("unchecked")
    private List<String> updateDeputies(List<String> sourceNameList) {
        Vector<String> resultNameList = new Vector<String>();

        resultNameList.addAll(sourceNameList);
        // test for each entry if a deputy is defined
        for (String aName : sourceNameList) {
            // now lookup the deputies
            ItemCollection profile = profileService.findProfileById(aName);
            if (profile != null) {
                List<String> deputyList = profile.getItemValue("namdeputy");
                // if we found deputies - we need to add them to the list
                for (String deputy : deputyList) {
                    if (deputy != null && !deputy.isEmpty() && resultNameList.indexOf(deputy) == -1) {
                        // add new entry
                        resultNameList.add(deputy);
                    }
                }
            }
        }
        return resultNameList;
    }

    public String[] getIgnoreList() {
        return ignoreList;
    }

    public void setIgnoreList(String[] ignoreList) {
        this.ignoreList = ignoreList;
    }

    /**
     * This method returns true in case the given itemName must not be modified.
     * Regular expressions are supported by the ignoreList and supportList.
     * 
     * @param itemName
     * @return true if fieldName matches the ignoreList
     */
    public boolean ignoreItem(String itemName) {
        if (itemName == null)
            return true;
        
        // contained in support list?
        for (String pattern : this.supportList) {
            Pattern p = Pattern.compile(pattern);
            if (p.matcher(itemName.toLowerCase()).find()) {
                return false;
            }
        }
        // contained in ignore list?
        for (String pattern : this.ignoreList) {
            Pattern p = Pattern.compile(pattern);
            if (p.matcher(itemName.toLowerCase()).find()) {
                return true;
            }
        }

        // ignore all not starting with 'nam' (deprecated)
        if (!itemName.toLowerCase().startsWith("nam")) {
            return true;
        }

        return false;
    }

}
