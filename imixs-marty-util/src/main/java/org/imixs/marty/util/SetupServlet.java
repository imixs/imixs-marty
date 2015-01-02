package org.imixs.marty.util;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.imixs.marty.ejb.SetupService;
import org.imixs.marty.ejb.security.UserGroupService;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.exceptions.AccessDeniedException;
import org.imixs.workflow.jee.ejb.WorkflowSchedulerService;

/**
 * This servlet checks userdb configuration and scheduler on startup. The
 * servlet is configured with the option load-on-startup=1 which means that the
 * servlet init() method is triggered after deployment.
 * 
 * @author rsoika
 * 
 */
@WebServlet(value = "/setup", loadOnStartup = 1)
// Because of a Deployment Issue in GlassFish we need to disable this runas
// option here!
// @RunAs("org.imixs.ACCESSLEVEL.MANAGERACCESS")
public class SetupServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(SetupServlet.class
			.getName());

	boolean initMode = false;

	@EJB
	WorkflowSchedulerService workflowSchedulerService;

	@EJB
	UserGroupService userGroupService;

	@EJB
	SetupService setupService;

	@Override
	public void init() throws ServletException {

		super.init();

		Properties properties = loadProperties();
		if (properties.containsKey("setup.mode")
				&& "auto".equals(properties.getProperty("setup.mode", "auto"))) {
			// avoid calling twice
			initMode = true;
			setup();
			initMode = false;
		}
	}

	@Override
	public void service(ServletRequest req, ServletResponse res)
			throws IOException, ServletException {

		// avoid calling twice
		if (!initMode)
			setup();

		res.getWriter().println("Imixs-Marty Setup: Satus=ok");
	}

	/**
	 * loads a imixs.property file
	 * 
	 * (located at current threads classpath)
	 * 
	 */
	private Properties loadProperties() {
		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader()
					.getResource("imixs.properties").openStream());
		} catch (Exception e) {
			logger.warning("PropertyService unable to find imixs.properties in current classpath");
			if (logger.isLoggable(Level.FINE)) {
				e.printStackTrace();
			}
		}

		return properties;
	}

	private void setup() {
		logger.info("Imixs-Office-Workflow - setup...");

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