package org.imixs.marty.plugins;

import java.util.regex.Pattern;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for DeputyPlugin
 * 
 * @author rsoika
 */
public class TestDeputyPlugin {
    DeputyPlugin deputyPlugin = null;

    /**
     * This simple test verifies the regex of the default deputy ignore list
     * 
     * @throws PluginException
     * 
     */
    @Test
    public void testignoreItem() throws PluginException {

        deputyPlugin = new DeputyPlugin();

        Assert.assertTrue(deputyPlugin.ignoreItem("namCreator"));
        Assert.assertTrue(deputyPlugin.ignoreItem("namCurrentEditor"));
        Assert.assertTrue(deputyPlugin.ignoreItem("namProcessManagerApprovers"));
        Assert.assertTrue(deputyPlugin.ignoreItem("namSpaceTeamApprovedBy"));
        Assert.assertTrue(deputyPlugin.ignoreItem("namProcessSuper_ManagerApprovers"));
        Assert.assertTrue(deputyPlugin.ignoreItem("namProcessSuper_Manager123Approvers"));

        Assert.assertTrue(deputyPlugin.ignoreItem("_responsible"));
        Assert.assertTrue(deputyPlugin.ignoreItem("txtNamOwner"));
        Assert.assertTrue(deputyPlugin.ignoreItem("lnamProcessSuperManagerApprovers"));
        Assert.assertTrue(deputyPlugin.ignoreItem("$activityid"));
        Assert.assertTrue(deputyPlugin.ignoreItem("$readaccess"));

        // negative tests
        Assert.assertFalse(deputyPlugin.ignoreItem("namSpaceTeam"));
        Assert.assertFalse(deputyPlugin.ignoreItem("namProcessSuper-ManagerApprovers"));
        Assert.assertFalse(deputyPlugin.ignoreItem("namResponsible"));
        Assert.assertFalse(deputyPlugin.ignoreItem("nam_Responsible"));
        
        Assert.assertFalse(deputyPlugin.ignoreItem("Space.manager"));
        Assert.assertFalse(deputyPlugin.ignoreItem("space.assist"));
        Assert.assertFalse(deputyPlugin.ignoreItem("process.manager"));
        Assert.assertTrue(deputyPlugin.ignoreItem("process.foo"));

    }
    
    
    /**
     * Test regex behavior.
     * See Issue: https://github.com/imixs/imixs-marty/issues/353
     * 
     * @throws PluginException
     */
    @Test
    public void testRegex()  {
        String pattern="\\.manager$";
        String itemName="space.manager";
        Pattern p = Pattern.compile(pattern);
        Assert.assertTrue(p.matcher(itemName.toLowerCase()).find());
        Assert.assertFalse(p.matcher(itemName.toLowerCase()).matches());
        
    }



}
