package org.imixs.marty.util;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Named;

import org.imixs.marty.ejb.AdminPService;
import org.imixs.workflow.jee.faces.workitem.DataController;

@Named("adminPController")
@SessionScoped
public class AdminPController extends DataController {

	@EJB
	protected AdminPService adminPService;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(AdminPController.class
			.getSimpleName());

	public AdminPController() {
		super();
		setType("adminp");

	}

	public void doStart(ActionEvent event) throws Exception {
		logger.info("[AdminPController] doStart");
		String fromName = this.getWorkitem().getItemValueString("namFrom");
		String toName = this.getWorkitem().getItemValueString("namTo");

		boolean replace = this.getWorkitem().getItemValueBoolean("keyReplace");

		setWorkitem(adminPService.start(fromName, toName, replace));

		reset();

	}

	@Override
	public void reset() {
		super.reset();
		// create new empty adminP entity
		create(null);
	}

}
