package org.imixs.marty.util;

import java.util.List;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.imixs.marty.business.ProjectService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.Plugin;
import org.imixs.workflow.WorkflowContext;
import org.imixs.workflow.jee.ejb.EntityService;
import org.imixs.workflow.jee.ejb.WorkflowService;
import org.imixs.workflow.plugins.jee.AbstractPlugin;

/**
 * This plugin supports additional workflow properties for further processing.
 * The method computes the team members of a corresponding parent project so
 * these teams can be used in security and mail plugins.
 * <p>
 * The Plugin should run first
 * 
 * @author rsoika
 * 
 */
public class SywappTeamPlugin extends AbstractPlugin {

	private ProjectService projectService = null;
	private EntityService entityService = null;

	@Override
	public void init(WorkflowContext actx) throws Exception {
		super.init(actx);
		     // check for an instance of WorkflowService
	        if (actx instanceof WorkflowService) {
	                // yes we are running in a WorkflowService EJB
	                WorkflowService ws=(WorkflowService)actx;
	                // get latest model version....
	                entityService=ws.getEntityService();
	        }
	        
	        
		// lookup profile service EJB
		//String jndiName = "ejb/ProjectServiceBean";
		//InitialContext ictx = new InitialContext();
		
		 // FooBean foo = (FooBean) new InitialContext().lookup("java:module/FooBean");

		//Context ctx = (Context) ictx.lookup("java:comp/env");
		//projectService = (ProjectService) ctx.lookup(jndiName);
		  projectService= (ProjectService) new InitialContext().lookup("java:module/ProjectService");
		
		  if (projectService==null)
			  System.out.println(" war nix");
			  else
				  System.out.println(" war was hurrar");
				    

	}

	public ProjectService getProjectService() {
		return projectService;
	}

	public EntityService getEntityService() {
		return entityService;
	}

	/**
	 * Each Workitem holds a reference to a Project in the attriubte
	 * '$uniqueidref'.
	 * <p>
	 * The Method updates the corresponding Project informations:
	 * <ul>
	 * <li>namProjectTeam
	 * <li>namProjectOwner
	 * <li>namProjectManager
	 * <li>namProjectAssist
	 * <li>namProjectName
	 * <li>namParentProjectTeam
	 * <li>namParentProjectOwner
	 * 
	 * Additional the Method also computes the values namSubProjectTeam
	 * namSubProjectOwner which holds all members of all subprojects
	 * 
	 * If the workitem is not a child to a project but to a other workitem
	 * (Child Process) then these informations are fetched from the parent
	 * workitem.
	 * 
	 * Finally the Method updates the field "txtProjectname" with the name of
	 * the parent project.
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@Override
	public int run(ItemCollection workItem, ItemCollection documentActivity)
			throws Exception {

		/*
		 * the following code updates a project reference if the
		 * workflowresultmessage contains a project= ref
		 */

		String sResult = null;
		// test if a new project reference need to be set now!
		try {
			// Read workflow result directly from the activity definition
			sResult = documentActivity.getItemValueString("txtActivityResult");
			sResult = replaceDynamicValues(sResult, workItem);

			if (sResult.indexOf("project=") > -1) {
				sResult = sResult.substring(sResult.indexOf("project=") + 8);
				// cut next newLine
				if (sResult.indexOf("\n") > -1)
					sResult = sResult.substring(0, sResult.indexOf("\n"));

				if (!"".equals(sResult)) {
					System.out
							.println("[WorkitemService] Updating Project reference: "
									+ sResult);
					// try to update project reference
					ItemCollection parent = this.projectService
							.findProjectByName(sResult);
					if (parent != null) {

						// assign project name and reference
						workItem.replaceItemValue("$uniqueidRef", parent
								.getItemValueString("$uniqueid"));
						workItem.replaceItemValue("txtProjectName", parent
								.getItemValueString("txtname"));

					}
				}
			}
		} catch (Exception upr) {
			System.out
					.println("[WorkitemServiceBean] WARNING - Unable to update Project ("
							+ sResult + ") reference: " + upr);
		}

		/*
		 * Now the team lists will be updated depending of the current
		 * $uniqueidref
		 */

		String sParentID = workItem.getItemValueString("$uniqueidref");

		// now add project Team and Owners to the current Workitem

		// there are two different situations.
		// if the parent is a project the method fetches team and owner form
		// project
		// if the parent is a workitem the method fetches the team and owner
		// from the workitem

		ItemCollection itemColProject = entityService.load(sParentID);
		if (itemColProject != null) {
			Vector vTeam = new Vector();
			Vector vOwner = new Vector();
			Vector vManager = new Vector();
			Vector vAssist = new Vector();
			Vector vParentTeam = new Vector();
			Vector vParentOwner = new Vector();
			Vector vParentManager = new Vector();
			Vector vParentAssist = new Vector();
			Vector vSubTeams = new Vector();
			Vector vSubOwner = new Vector();
			Vector vSubAssist = new Vector();
			Vector vSubManager = new Vector();
			String sProjectName = "";

			String parentType = itemColProject.getItemValueString("type");
			if ("project".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namTeam");
				vOwner = itemColProject.getItemValue("namOwner");
				vManager = itemColProject.getItemValue("namManager");
				vAssist = itemColProject.getItemValue("namAssist");
				vParentTeam = itemColProject.getItemValue("namParentTeam");
				vParentOwner = itemColProject.getItemValue("namParentOwner");
				// now add subproject teams and owners to additional attributes
				// - if
				// available
				List<ItemCollection> subProjects = projectService
						.findAllSubProjects(sParentID, 0, -1);
				for (ItemCollection aSubProject : subProjects) {
					vSubTeams.addAll(aSubProject.getItemValue("namTeam"));
					vSubOwner.addAll(aSubProject.getItemValue("namOwner"));
					vSubManager.addAll(aSubProject.getItemValue("namManager"));
					vSubAssist.addAll(aSubProject.getItemValue("namAssist"));
				}

				sProjectName = itemColProject.getItemValueString("txtname");
			}
			if ("workitem".equals(parentType)) {
				vTeam = itemColProject.getItemValue("namProjectTeam");
				vOwner = itemColProject.getItemValue("namProjectOwner");
				vAssist = itemColProject.getItemValue("namProjectAssist");
				vManager = itemColProject.getItemValue("namProjectManager");
				vParentTeam = itemColProject
						.getItemValue("namParentProjectTeam");
				vParentOwner = itemColProject
						.getItemValue("namParentProjectOwner");
				vParentAssist = itemColProject
						.getItemValue("namParentProjectAssist");
				vParentManager = itemColProject
						.getItemValue("namParentProjectManager");
				vSubTeams = itemColProject.getItemValue("namSubProjectTeam");
				vSubOwner = itemColProject.getItemValue("namSubProjectOwner");
				vSubAssist = itemColProject.getItemValue("namSubProjectAssist");
				vSubManager = itemColProject
						.getItemValue("namSubProjectManager");

				sProjectName = itemColProject
						.getItemValueString("txtProjectName");
			}

			// update team lists
			workItem.replaceItemValue("namProjectTeam", vTeam);
			workItem.replaceItemValue("namProjectOwner", vOwner);
			workItem.replaceItemValue("namParentProjectTeam", vParentTeam);
			workItem.replaceItemValue("namParentProjectOwner", vParentOwner);
			workItem.replaceItemValue("namSubProjectTeam", vSubTeams);
			workItem.replaceItemValue("namSubProjectOwner", vSubOwner);

			workItem.replaceItemValue("namProjectAssist", vAssist);
			workItem.replaceItemValue("namProjectManager", vManager);
			workItem.replaceItemValue("namParentProjectAssist", vParentAssist);
			workItem
					.replaceItemValue("namParentProjectManager", vParentManager);
			workItem.replaceItemValue("namSubProjectAssist", vSubAssist);
			workItem.replaceItemValue("namSubProjectManager", vSubManager);

			// update project name
			workItem.replaceItemValue("txtProjectName", sProjectName);

		}
		return Plugin.PLUGIN_OK;
	}

	@Override
	public void close(int arg0) throws Exception {
		// no op

	}

}
