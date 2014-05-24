package org.imixs.marty.plugins;

import java.util.logging.Level;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.test.WorkflowTestSuite;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This test class test the DMSPlubin import feature.
 * 
 * A new WokItem containing the property txtImportPath should import listed
 * files. The DMSPlugin should attach the file to the BlobWorkitem.
 * 
 * 
 * @author rsoika
 * 
 */
public class DMSImportTest {
	WorkflowTestSuite testSuite = null;

	@Before
	public void setup() {
		testSuite = WorkflowTestSuite.getInstance();

		testSuite.setHost("http://localhost:8080/office-rest/");

		// join users...
		testSuite.joinParty("admin", "adminadmin");
		testSuite.joinParty("anna", "anna");
	}

	@After
	public void teardown() {

	}

	@Test
	public void importTest() throws Exception {

		Assert.assertNotNull(testSuite.getClient("anna"));
		Assert.assertNull(testSuite.getClient("xxx"));
 
		ItemCollection workitem = createWorkitem();

		workitem.replaceItemValue("_subject", "Test-1");

		// import file
		workitem.replaceItemValue("txtDmsImport",
				"/home/rsoika/Downloads/Scan_1.pdf");

		workitem = testSuite.processWorkitem(workitem, "admin");

		String uid = workitem.getItemValueString("$UniqueID");
		WorkflowTestSuite.log(Level.INFO, "UID=" + uid);

	}

	/**
	 * Creates a user profile for this user
	 * 
	 * @throws Exception
	 */
	private ItemCollection createWorkitem() {

		WorkflowTestSuite.log(Level.INFO, "create new workitem" );

		ItemCollection workitem = new ItemCollection();
		workitem.replaceItemValue("type", "workitem");
		workitem.replaceItemValue("$ModelVersion", "office-de-0.0.2");
		workitem.replaceItemValue("$processid", 2000);
		workitem.replaceItemValue("$activityid", 10);

		return workitem;

	}

}
