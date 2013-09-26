package org.imixs.marty.model;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.ReportService;
import org.imixs.workflow.jee.faces.workitem.DataController;

@Named("reportController")
@ApplicationScoped
public class ReportController extends DataController {

	@EJB
	protected ReportService reportService;

	Map<String, String> params;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ReportController.class
			.getSimpleName());

	public ReportController() {
		super();
		setType("report");

	}

	/**
	 * Reset the params if a new report was loaded
	 */
	@Override
	public String load(String uniqueID, String action) {
		params = null;
		return super.load(uniqueID, action);
	}

	public Map<String, String> getParams() {
		ItemCollection report = this.getWorkitem();
		if (params == null && report != null) {
			params = new HashMap<String, String>();

			// parse query
			String query = report.getItemValueString("txtquery");
			int i = 0;
			while ((i = query.indexOf('?', i)) > -1) {
				String sTest = query.substring(i + 1);
				// cut next space or ' or "
				for (int j = 0; j < sTest.length(); j++) {
					char c = sTest.charAt(j);
					if (c == '\'' || c == '"' || c == ' ') {
						// cut here!
						String sKey = query.substring(i + 1, i + j + 1);
						params.put(sKey, "");
						i++;
						break;
					}
				}

			}
		}

		return params;
	}

}
