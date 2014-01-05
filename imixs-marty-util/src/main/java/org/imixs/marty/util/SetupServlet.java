package org.imixs.marty.util;

import java.util.logging.Logger;

import javax.annotation.security.RunAs;
import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.imixs.marty.ejb.SetupService;
import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.WorkflowSchedulerService;

/**
 * This servlet checks userdb configuration and scheduler on startup. The servlet is
 * configured with the option load-on-startup=1 which means that
 * the servlet init() method is triggered after deployment.
 * 
 * @author rsoika
 * 
 */
@WebServlet(loadOnStartup=1)
@RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SetupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(SetupServlet.class.getName());

	@EJB
	WorkflowSchedulerService workflowSchedulerService;

	@EJB
	UserGroupService userGroupService;
	
	@EJB
	SetupService setupService;


	@Override
	public void init() throws ServletException {

		super.init();
		logger.info("Imixs Office Workflow - setup...");

		// init userIDs for user db
		try {
			if (userGroupService != null) {				
				userGroupService.initUserIDs();
			}

		} catch (Exception e) {
			logger.warning("SetupServlet - unable to initUserIds "
					+ e.getMessage());
		}
		
		// try to init system indizies and load default models
		try {
			setupService.init();
		} catch (AccessDeniedException e1) {
			logger.severe("SetupServlet - unable to init system "
					+ e1.getMessage());
			e1.printStackTrace();
		}

		// try to start the scheduler service
		try {
			ItemCollection configItemCollection = workflowSchedulerService
		 			.loadConfiguration();

			if (configItemCollection.getItemValueBoolean("_enabled"))
				workflowSchedulerService.start();
		} catch (Exception e) {
			logger.warning("SetupServlet - unable to start scheduler service "
					+ e.getMessage());
		}

	}

}