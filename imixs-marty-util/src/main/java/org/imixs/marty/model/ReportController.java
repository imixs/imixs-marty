package org.imixs.marty.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.ReportService;
import org.imixs.workflow.jee.faces.workitem.DataController;

@Named("reportController")
@SessionScoped
public class ReportController extends DataController {

	@EJB
	protected ReportService reportService;

	Map<String, String> params;

	String uri = null;
	String format = null;
	String encoding = null;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ReportController.class.getName());

	public ReportController() {
		super();
		setType("report");

	}

	/**
	 * Returns a String sorted list of all report names.
	 * 
	 * @return list of report names
	 */
	public List<ItemCollection> getReports() {
		return reportService.getReportList();
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

	/**
	 * Returns the Rest Service URI
	 * 
	 * @return
	 */
	public String getUri() {
		logger.fine("[ReportController] Update uri...");

		String sReport = this.getWorkitem().getItemValueString("txtName");
		// cut . char
		if (sReport.contains(".")) {
			sReport = sReport.substring(0, sReport.indexOf('.'));
		}

		uri = "/rest-service/report/" + sReport + "." + getFormat() + "?count=-1";

		if (encoding != null && !encoding.isEmpty()) {
			uri += "&encoding=" + encoding;
		}

		// now parse params....
		if (params != null) {
			Set<Entry<String, String>> set = params.entrySet();
			for (Entry<String, String> entry : set) {
				uri += "&" + entry.getKey() + "=" + entry.getValue();
			}
		}

		logger.fine("[ReportController] uri=" + uri);

		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getFormat() {
		if (format == null || format.isEmpty())
			format = "html";

		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}
