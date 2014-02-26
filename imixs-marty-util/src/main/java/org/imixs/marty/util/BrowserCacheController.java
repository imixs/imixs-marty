package org.imixs.marty.util;

import java.io.Serializable;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;

import org.imixs.marty.workflow.WorkflowController;
import org.imixs.workflow.jee.ejb.WorkflowService;

/**
 * This class is to avoid history back button in browser by discarding the
 * browser cache
 * 
 * @author rsoika
 * 
 */
@Named("browserCacheController")
@RequestScoped
public class BrowserCacheController implements Serializable {

	@Inject
	protected WorkflowController workflowController;

	private static Logger logger = Logger
			.getLogger(BrowserCacheController.class.getName());
	private static final long serialVersionUID = 1L;

	/**
	 * This method changes the response header and set the browser cash to 0.
	 * This disables a browser history back navigation.
	 * 
	 * Can be included into a jsf page:
	 * 
	 * <code>
	 * 
		<f:view>
			<f:event type="preRenderView" listener="#{browserCacheController.clearCache}" />
		   ....
	 * 
	 * </code>
	 * 
	 */
	public void clearCache() {
		// Do here your job which should run right before the RENDER_RESPONSE.
		if (workflowController.getWorkitem() != null) {
			logger.info("[WorkflowMB] clear cache-control for: "
					+ workflowController.getWorkitem().getItemValueString(
							WorkflowService.UNIQUEID));
			// clear cache
			ExternalContext context = FacesContext.getCurrentInstance()
					.getExternalContext();
			HttpServletResponse response = (HttpServletResponse) context
					.getResponse();
			response.setHeader("Cache-Control",
					"no-cache, no-store, must-revalidate"); // HTTP 1.1.
			response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
			response.setDateHeader("Expires", 0); // Proxies.

		}
	}
	
	/**
	 * Returns a unique increasing id for each request
	 * @return
	 */
	public long getCacheID() {
		return System.nanoTime();
	}
	
}
