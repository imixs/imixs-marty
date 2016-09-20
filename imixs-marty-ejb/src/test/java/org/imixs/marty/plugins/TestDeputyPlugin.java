package org.imixs.marty.plugins;

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
	public void testRegex() throws PluginException {

		deputyPlugin = new DeputyPlugin();

		Assert.assertTrue(deputyPlugin.matchIgnoreList("namCreator"));
		Assert.assertTrue(deputyPlugin.matchIgnoreList("namCurrentEditor"));
		Assert.assertTrue(deputyPlugin.matchIgnoreList("namProcessManagerApprovers"));
		Assert.assertTrue(deputyPlugin.matchIgnoreList("namSpaceTeamApprovedBy"));
		Assert.assertTrue(deputyPlugin.matchIgnoreList("namProcessSuper_ManagerApprovers"));
		Assert.assertTrue(deputyPlugin.matchIgnoreList("namProcessSuper_Manager123Approvers"));

		// negative tests
		Assert.assertFalse(deputyPlugin.matchIgnoreList("namSpaceTeam"));

		Assert.assertFalse(deputyPlugin.matchIgnoreList("_responsible"));
		Assert.assertFalse(deputyPlugin.matchIgnoreList("namProcessSuper-ManagerApprovers"));
		Assert.assertFalse(deputyPlugin.matchIgnoreList("lnamProcessSuperManagerApprovers"));

	}

}
