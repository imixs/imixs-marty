package org.imixs.marty.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.imixs.workflow.FileData;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.ReportService;
import org.imixs.workflow.exceptions.ModelException;
import org.imixs.workflow.faces.data.DocumentController;
import org.imixs.workflow.xml.XMLDocumentAdapter;
import org.xml.sax.SAXException;

@Named
@SessionScoped
public class ReportController implements Serializable {

	private ItemCollection reportUploads;

	@EJB
	protected ReportService reportService;

	@Inject
	DocumentController documentController;

	Map<String, String> params;

	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(ReportController.class.getName());

	public ReportController() {
		super();
		reportUploads = new ItemCollection();
	}

	public ItemCollection getReportUploads() {
		return reportUploads;
	}

	public void setReportUploads(ItemCollection reportUploads) {
		this.reportUploads = reportUploads;
	}

	/**
	 * Returns a String sorted list of all report names.
	 * 
	 * @return list of report names
	 */
	public List<ItemCollection> getReports() {
		return reportService.findAllReports();
	}

	/**
	 * Reset the params if a new report was loaded
	 */
	public String load(String uniqueID, String action) {
		params = null;
		documentController.load(uniqueID);
		return action;
	}

	public Map<String, String> getParams() {
		logger.fine("parsing params...");
		ItemCollection report = documentController.getDocument();
		if (params == null && report != null) {
			params = new HashMap<String, String>();

			// parse query
			String query = report.getItemValueString("txtquery");
			int i = 0;
			while ((i = query.indexOf('?', i)) > -1) {
				String sTest = query.substring(i + 1);
				// cut next space or ' or " or ] or :
				for (int j = 0; j < sTest.length(); j++) {
					char c = sTest.charAt(j);
					if (c == '\'' || c == '"' || c == ']' || c == ':' || c == ' ') {
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
	 * This method adds all uploaded imixs-report files.
	 * 
	 * @param event
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 * @throws ParseException
	 * @throws ModelException
	 * @throws JAXBException
	 * 
	 */
	public void uploadReport() throws ModelException, ParseException, ParserConfigurationException, SAXException,
			IOException, JAXBException {
		List<FileData> fileList = getReportUploads().getFileData();

		if (fileList == null) {
			return;
		}
		for (FileData file : fileList) {
			logger.info("Import report: " + file.getName());

			// test if imxis-report?
			if (file.getName().endsWith(".imixs-report")) {
				ByteArrayInputStream input = new ByteArrayInputStream(file.getContent());
				ItemCollection report = XMLDocumentAdapter.readItemCollectionFromInputStream(input);
				reportService.updateReport(report);
				continue;
			}
			// model type not supported!
			logger.warning("Invalid Report Type. Report can't be imported!");
		}

		// reset upploads
		reportUploads = new ItemCollection();
	}
}
