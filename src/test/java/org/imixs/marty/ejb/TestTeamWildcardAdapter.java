package org.imixs.marty.ejb;

import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.imixs.marty.plugins.TeamPlugin;
import org.imixs.marty.team.TeamRoleWildcardAdapter;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.WorkflowKernel;
import org.imixs.workflow.engine.DocumentService;
import org.imixs.workflow.engine.TextEvent;
import org.imixs.workflow.exceptions.PluginException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import junit.framework.Assert;

/**
 * Test class for TeamPlugin
 * 
 * @author rsoika
 */
public class TestTeamWildcardAdapter {
	TeamPlugin teamPlugin = null;
	ItemCollection documentActivity;
	ItemCollection documentContext;
	Map<String, ItemCollection> database = new HashMap<String, ItemCollection>();

	@Spy
	TeamRoleWildcardAdapter teamRoleWildcardAdapter;

	/**
	 * Setup script to simulate process and space entities for test cases.
	 * 
	 * @throws PluginException
	 */
	@Before
	public void setup() throws PluginException {
		MockitoAnnotations.initMocks(this);

		ItemCollection entity = null;

		System.out.println("ClassName: " + TestTeamWildcardAdapter.class.getName());

		// simulate process and space entities
		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "process");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "P0000-0000" + i);
			entity.replaceItemValue("txtName", "Process " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "space");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "S0000-0000" + i);
			entity.replaceItemValue("txtName", "Space " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue("type", "workitem");
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "W0000-0000" + i);
			entity.replaceItemValue("txtName", "Workitem " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		for (int i = 1; i < 6; i++) {
			entity = new ItemCollection();
			entity.replaceItemValue(WorkflowKernel.UNIQUEID, "C0000-0000" + i);
			entity.replaceItemValue("txtName", "ChildWorkitem " + i);
			database.put(entity.getItemValueString(WorkflowKernel.UNIQUEID), entity);
		}

		// Mockito setup
		DocumentService documentService = Mockito.mock(DocumentService.class);
		// Simulate entityService.load()...
		when(documentService.load(Mockito.anyString())).thenAnswer(new Answer<ItemCollection>() {
			@Override
			public ItemCollection answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				String id = (String) args[0];
				ItemCollection result = database.get(id);
				return result;
			}
		});

		documentActivity = new ItemCollection();
		documentContext = new ItemCollection();

	}

	/**
	 * This test validates the wildcard orgunit role as a ACL notation:
	 * 
	 * {process:?:team}
	 * 
	 * The Plugin should compute the orgunit bas"{space:?:team}"ed on the assigement
	 * to the current workitem.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testWildcardOrgunitRole() throws PluginException {

		// test a space team role...
		documentContext.replaceItemValue("space.ref", "S0000-00002");
		documentContext.replaceItemValue("process.ref", "P0000-00003");

		String testRole = "{space:?:team}";
		TextEvent event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		String roleResult = event.getText();
		List<String> roleList = event.getTextList();

		// text should not be changed..
		Assert.assertEquals("{space:?:team}", roleResult);
		// test role list...
		Assert.assertNotNull(roleList);
		Assert.assertEquals(1, roleList.size());
		Assert.assertEquals(roleList.get(0), "{space:S0000-00002:team}");

		// test member role
		testRole = "{process:?:member}";
		event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		roleResult = event.getText();
		roleList = event.getTextList();

		// text should not be changed..
		Assert.assertEquals("{process:?:member}", roleResult);
		// test role list...
		Assert.assertNotNull(roleList);
		Assert.assertEquals(1, roleList.size());
		Assert.assertEquals(roleList.get(0), "{process:P0000-00003:member}");

	}

	/**
	 * This test validates the wildcard orgunit role as a ACL notation:
	 * 
	 * {process:?:team}
	 * 
	 * The Plugin should compute the orgunit bas"{space:?:team}"ed on the assigement
	 * to the current workitem.
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testWildcardMultiOrgunitRole() throws PluginException {

		// test two space team roles...
		documentContext.replaceItemValue("space.ref", "S0000-00002");
		documentContext.appendItemValue("space.ref", "S0000-00003");

		documentContext.replaceItemValue("process.ref", "P0000-00003");

		String testRole = "{space:?:team}";
		TextEvent event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);

		List<String> listResult = event.getTextList();

		Assert.assertEquals(2, listResult.size());

		Assert.assertEquals(listResult.get(0), "{space:S0000-00002:team}");
		Assert.assertEquals(listResult.get(1), "{space:S0000-00003:team}");

		// test member role
		testRole = "{process:?:member}";
		event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		listResult = event.getTextList();
		Assert.assertEquals(listResult.get(0), "{process:P0000-00003:member}");

	}

	/**
	 * This test validates a non role specific text fragment. The Adapter should not
	 * change the text.
	 * 
	 * 
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testNonRoleSpecificText() throws PluginException {

		// test some other text fragment
		String testText = "<item name=\"comment\" ignore=\"true\"/>";
		TextEvent event = new TextEvent(testText, documentContext);
		teamRoleWildcardAdapter.onEvent(event);
		String testResult = event.getText();

		Assert.assertNotNull(testResult);
		Assert.assertEquals(testText, testResult);

	}

	/**
	 * This test validates non wildcard roles
	 * 
	 * {process:team}
	 * 
	 * @throws PluginException
	 * 
	 */
	@Test
	public void testNonWildcardRole() throws PluginException {

		// test two space team roles...
		documentContext.replaceItemValue("txtSpaceRef", "S0000-00002");
		documentContext.appendItemValue("txtSpaceRef", "S0000-00003");

		documentContext.replaceItemValue("txtProcessRef", "P0000-00003");

		String testRole = "{space:team}";
		TextEvent event = new TextEvent(testRole, documentContext);
		teamRoleWildcardAdapter.onEvent(event);

		List<String> listResult = event.getTextList();
		// no text list expected
		Assert.assertEquals(1, listResult.size());
		Assert.assertEquals("{space:team}", listResult.get(0));

		String resultRole = event.getText();

		Assert.assertEquals("{space:team}", resultRole);

	}

	/**
	 * helper test.
	 * 
	 * See:
	 * https://stackoverflow.com/questions/8061302/regex-to-check-with-starts-with-http-https-or-ftp
	 */
	@Test
	public void testRegex() {

		String regex = "^(http|https|ftp)://.*";
		if (!"http://www.imixs.org".matches(regex)) {
			Assert.fail();
		}

		regex = "^(http|https)://.*";
		if (!"http://www.imixs.rog".matches(regex)) {
			Assert.fail();
		}

		regex = "^(XYZ).*";

		if (!"XYZ SUppi".matches(regex)) {
			Assert.fail();
		}
	}

}
