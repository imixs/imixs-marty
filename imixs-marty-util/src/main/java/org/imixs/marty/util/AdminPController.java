package org.imixs.marty.util;

import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
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
			.getName());

	public AdminPController() {
		super();
		setType("adminp");

	}

	public void createRenameUserJob() {
		logger.fine("createRenameUserJob...");
		String fromName = this.getWorkitem().getItemValueString("namFrom");
		String toName = this.getWorkitem().getItemValueString("namTo");

		boolean replace = this.getWorkitem().getItemValueBoolean("keyReplace");

		setWorkitem(adminPService.createJobRenameUser(fromName, toName, replace));

		reset();

	}
	
	public void createRebuildLuceneIndexJob() {
		adminPService.createJobRebuildLuceneIndex();
		reset();
	}
	
	public void cancelJob(String id) {
		adminPService.cancleJob(id);
		reset();

	}

	public void restartJob(String id) {
		adminPService.restartJob(id);
		reset();

	}

	
	
	@Override
	public void reset() {
		super.reset();
		// create new empty adminP entity
		create(null);
	}

}
