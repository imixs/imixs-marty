package org.imixs.marty.plugins;

import static org.mockito.Mockito.when;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.SessionContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.exceptions.PluginException;
import org.imixs.workflow.jee.ejb.AbstractWorkflowServiceTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import junit.framework.Assert;

/**
 * This test class will test the comment feature of the CommentPlugin
 * 
 * @author rsoika
 */
public class TestCommentPlugin extends AbstractWorkflowServiceTest {
	private final static Logger logger = Logger.getLogger(TestCommentPlugin.class.getName());

	@Spy
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
	 */
	@Before
	public void setup() throws PluginException {
		// initialize @Mock annotations....
		MockitoAnnotations.initMocks(this);

		super.setup();

		// mock session context of plugin
		ctx = Mockito.mock(SessionContext.class);
		Principal principal = Mockito.mock(Principal.class);
		when(principal.getName()).thenReturn("manfred");
		when(commentPlugin.getEjbSessionContext()).thenReturn(ctx);
		when(commentPlugin.getEjbSessionContext().getCallerPrincipal()).thenReturn(principal);

		// init plugin..
		try {
			commentPlugin.init(workflowContext);
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
		entityService.save(documentContext);

	}

	/**
	 * This simple test verifies the default comment feature
	 * 
	 * @throws PluginException
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testSimpleComment() throws PluginException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getActivityEntity(100, 10);

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
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testIgnoreComment() throws PluginException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getActivityEntity(100, 10);

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
	 * 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testFixedComment() throws PluginException {

		documentContext.replaceItemValue("txtComment", "Some Comment");
		documentActivity = this.getActivityEntity(100, 10);

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
