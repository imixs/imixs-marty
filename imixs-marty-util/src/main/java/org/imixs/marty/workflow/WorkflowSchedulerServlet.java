package org.imixs.marty.workflow;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.WorkflowSchedulerService;

/**
 * This servlet runs during deployment and verifies if a
 * WorkflowSchedulerService need to be restarted.
 * 
 * @author rsoika
 * 
 */
@WebServlet(value = "/setup", loadOnStartup = 1)
public class WorkflowSchedulerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(WorkflowSchedulerServlet.class.getName());

	@EJB
	WorkflowSchedulerService workflowSchedulerService;

	/**
	 * This method is called on startup. The method verifies if a
	 * workflowScheduler timer exits and restarts the timer service
	 */
	@Override
	public void init() throws ServletException {

		super.init();

		// try to start the scheduler service
		try {
			ItemCollection configItemCollection = workflowSchedulerService.loadConfiguration();
			if (configItemCollection != null && configItemCollection.getItemValueBoolean("_enabled")) {
				logger.warning("Restarting WorkflowScheduler '" + configItemCollection.getUniqueID() + "'");
				workflowSchedulerService.start();
			}
		} catch (Exception e) {
			logger.warning("Error due to start workflowSchedulerService: " + e.getMessage());
		}
	}

}