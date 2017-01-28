package org.imixs.marty.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.AbstractWorkflowEnvironment;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.exceptions.ProcessingErrorException;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This test class will test the comment feature of the CommentPlugin
 * 
 * @author rsoika
 */
public class TestCommentPlugin extends AbstractWorkflowEnvironment {
	private final static Logger logger = Logger.getLogger(TestCommentPlugin.class.getName());

	private CommentPlugin commentPlugin;
 
	// CommentPlugin commentPlugin = null;
	ItemCollection documentActivity;
	protected SessionContext ctx;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 * @throws ModelException 
	 * @throws ProcessingErrorException 
	 * @throws AccessDeniedException 
	 */
	@Before
	public void setup() throws PluginException, AccessDeniedException, ProcessingErrorException, ModelException {

		super.setup();


		commentPlugin=new CommentPlugin();
		// init plugin..
		try {
			commentPlugin.init(workflowService);
		} catch (PluginException e) {
			e.printStackTrace();
		}

		// prepare test workitem
		documentContext = new ItemCollection();
		logger.info("[TestApplicationPlugin] setup test data...");
		documentContext.replaceItemValue("namCreator", "ronny");
		documentContext.replaceItemValue(WorkflowKernel.MODELVERSION, "1.0.0");
		documentContext.replaceItemValue(WorkflowKernel.PROCESSID, 100);
		documentContext.replaceItemValue(WorkflowKernel.UNIQUEID, WorkflowKernel.generateUniqueID());
		documentService.save(documentContext);

	}

	/**
	 * This simple test verifies the default comment feature
	 * 
	 * @throws PluginException
	 * @throws ModelException 
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSimpleComment() throws PluginException, ModelException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getModel().getEvent(100, 10);

		try {
			commentPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List<Map> commentList = documentContext.getItemValue("txtcommentLog");
		String lastComment = documentContext.getItemValueString("txtLastComment");
		String currentComment = documentContext.getItemValueString("txtComment");
		Assert.assertEquals(1, commentList.size());
		Assert.assertEquals("Some Comment", ((Map) commentList.get(0)).get("txtcomment"));
		Assert.assertEquals("Some Comment", lastComment);
		Assert.assertTrue(currentComment.isEmpty());
	}

	/**
	 * This simple test verifies the comment ignore=true flag
	 * 
	 * @throws PluginException
	 * @throws ModelException 
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testIgnoreComment() throws PluginException, ModelException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getModel().getEvent(100, 10);

		// change result
		documentActivity.replaceItemValue("txtActivityResult", "<item name=\"comment\" ignore=\"true\" />");

		try {
			commentPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List<Map> commentList = documentContext.getItemValue("txtcommentLog");
		String lastComment = documentContext.getItemValueString("txtLastComment");
		String currentComment = documentContext.getItemValueString("txtComment");
		Assert.assertEquals(0, commentList.size());
		Assert.assertEquals("Some Comment", currentComment);
		Assert.assertTrue(lastComment.isEmpty());
	}

	/**
	 * This test verifies a fixed comment text
	 * 
	 * @throws PluginException
	 * @throws ModelException 
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testFixedComment() throws PluginException, ModelException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getModel().getEvent(100, 10);

		// change result
		documentActivity.replaceItemValue("txtActivityResult",
				"<item name=\"comment\" ignore=\"false\" >My Comment</item>");

		try {
			commentPlugin.run(documentContext, documentActivity);
		} catch (PluginException e) {

			e.printStackTrace();
			Assert.fail();
		}

		List<Map> commentList = documentContext.getItemValue("txtcommentLog");
		String lastComment = documentContext.getItemValueString("txtLastComment");
		String currentComment = documentContext.getItemValueString("txtComment");
		Assert.assertEquals(1, commentList.size());
		Assert.assertEquals("My Comment", ((Map) commentList.get(0)).get("txtcomment"));
		Assert.assertEquals("", lastComment);
		Assert.assertEquals("Some Comment", currentComment);
	}

}
