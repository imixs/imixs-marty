package org.imixs.marty.view;

import java.util.List;

import javax.ejb.EJB;
import javax.inject.Inject;

import org.imixs.marty.profile.UserController;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.faces.workitem.ViewController;
import org.imixs.workflow.jee.faces.workitem.WorklistController;

/**
 * This CDI Controller can be used to provide different worklist views. The
 * controller provides a ViewAdapter implementation which can be configured by
 * faces-config.xml
 * 
 * Example:
 * 
 * <code>
 * <managed-bean>
		<managed-bean-name>portletWorklistTasks</managed-bean-name>
		<managed-bean-class>org.imixs.marty.view.PortletWorklistTasks</managed-bean-class>
		<managed-bean-scope>view</managed-bean-scope>
		<managed-property>
			<property-name>maxResult</property-name>
			<property-class>int</property-class>
			<value>5</value>
		</managed-property>
		<managed-property>
			<property-name>sortOrder</property-name>
			<property-class>int</property-class>
			<!-- SORT_ORDER_MODIFIED_DESC -->
			<value>2</value>
		</managed-property>
		<managed-property>
			<!-- default view -->
			<property-name>view</property-name>
			<property-class>java.lang.String</property-class>
			<value>worklist.owner</value>
		</managed-property>
	</managed-bean>

 * </code>
 * 
 * @author rsoika
 * 
 */
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
