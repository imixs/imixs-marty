package org.imixs.marty.view;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.ViewController;
import org.imixs.workflow.jee.faces.workitem.WorklistController;

//@Named("portletWorklistTasks")
//@ViewScoped
public class PortletWorklistTasks extends WorklistController {

	private static final long serialVersionUID = 1L;
	final String QUERY_WORKLIST_BY_OWNER = "worklist.owner";
	final String QUERY_WORKLIST_BY_CREATOR = "worklist.creator";
	final String QUERY_WORKLIST_BY_FAVORITE = "worklist.favorite";
	
	@EJB
	private org.imixs.workflow.jee.ejb.WorkflowService workflowService;
	
	@Inject
	UserController userController;

	public PortletWorklistTasks() {
		super();
		setViewAdapter(new PortletViewAdapter());
	}

	/**
	 * Custom implementation of a ViewAdapter to return workflow specific result
	 * lists.
	 * 
	 * @author rsoika
	 * 
	 */
	class PortletViewAdapter extends ViewAdapter {

		public List<ItemCollection> getViewEntries(
				final ViewController controller) {
		
			if (QUERY_WORKLIST_BY_CREATOR.equals(getView()))
				return workflowService.getWorkListByCreator(null,
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			if (QUERY_WORKLIST_BY_OWNER.equals(getView()))
				return workflowService.getWorkListByOwner(null,
						controller.getRow(), controller.getMaxResult(),
						controller.getType(), getSortOrder());
			if (QUERY_WORKLIST_BY_FAVORITE.equals(getView())) {
				return userController.getFavorites();
			}
				

			// default behaivor
			return super.getViewEntries(controller);
		}
	}
}
