package org.imixs.marty.rest;

import java.util.Random;
import java.util.logging.Level;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.test.WorkflowTestSuite;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test class for a stress test against rest interface and backend jpa
 * imlementation
 * 
 * 
 * @author rsoika
 */
public class StressTest {
	WorkflowTestSuite testSuite = null;

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 */
	@Before
	public void setup() throws PluginException {
		testSuite = WorkflowTestSuite.getInstance();
		testSuite.setHost("http://localhost:8080/office-rest/");
		testSuite.joinParty("anna", "anna");
		testSuite.joinParty("rsoika", "ne-pt-un");
		testSuite.joinParty("Anonymous", null);
	}

	/**
	 * This simple test verifies if the txtProcessRef is transfered into
	 * $UnqiueIDref
	 * 
	 * @throws PluginException
	 * 
	 * */
	@Test
	@Ignore
	public void testSimpleWorkflow() throws PluginException {
		long l = System.currentTimeMillis();
  
		Assert.assertNotNull(testSuite.getClient("rsoika"));
 
		ItemCollection workitem = createWorkitem();
  
		workitem = processWorkitem(workitem);
		Assert.assertNotNull(workitem);
		String uid = workitem.getItemValueString("$UniqueID");
		
		Assert.assertFalse(uid.isEmpty());

		WorkflowTestSuite.log(Level.INFO,"testSimpleWorkflow -> total time="
				+ (System.currentTimeMillis() - l) + " ms");

	}

	/**
	 *  Creates 10 workitems
	 */
	@Test
	@Ignore
	public void stressTest1() throws PluginException {
		long l = System.currentTimeMillis();

		Assert.assertNotNull(testSuite.getClient("rsoika"));

		for (int i = 0; i < 10; i++) {

			ItemCollection workitem = createWorkitem();

			workitem = processWorkitem(workitem);
			Assert.assertNotNull(workitem);
			String uid = workitem.getItemValueString("$UniqueID");
			
			Assert.assertFalse(uid.isEmpty());
		}

		WorkflowTestSuite.log(Level.INFO,"testSimpleWorkflow -> total time="
				+ (System.currentTimeMillis() - l) + " ms");

	}

	/**
	 * Creates 10 workitems and processes each 10 times
	 * 
	 * @throws PluginException
	 */
	@Test
	@Ignore
	public void stressTest2() throws PluginException {
		long l = System.currentTimeMillis();
 
		Assert.assertNotNull(testSuite.getClient("rsoika"));

		for (int i = 0; i < 10; i++) {

			long li=System.currentTimeMillis();
			ItemCollection workitem = createWorkitem();

			workitem = processWorkitem(workitem);
			Assert.assertNotNull(workitem);
			String uid = workitem.getItemValueString("$UniqueID");
			Assert.assertFalse(uid.isEmpty());
			
			for (int j = 0; j < 10; j++) {
				workitem.replaceItemValue("$activityid", 10);
				workitem = processWorkitem(workitem);
			}
			
			WorkflowTestSuite.log(Level.INFO,"testSimpleWorkflow -> procssing 10 times in "
					+ (System.currentTimeMillis() - li) + " ms");
			
		}

		WorkflowTestSuite.log(Level.INFO,"testSimpleWorkflow -> total time="
				+ (System.currentTimeMillis() - l) + " ms");

	}

	private ItemCollection createWorkitem() {
		Random rand = new Random();

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("type", "workitem");
		workitem.replaceItemValue("$ModelVersion", "office-de-0.0.2");
		workitem.replaceItemValue("$processid", 2000);
		workitem.replaceItemValue("$activityid", 10);
		workitem.replaceItemValue("_subject", "JUnit-Test-" + rand.nextInt(50)
				+ 1);

		return workitem;
	}

	private ItemCollection processWorkitem(ItemCollection workitem)
			throws PluginException {

		workitem = testSuite.processWorkitem(workitem, "rsoika");

		return workitem;
	}
}
