package org.imixs.marty.profile;

import java.util.List;

import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.imixs.workflow.faces.data.ViewController;

/**
 * This View Controller extends the default viewController and provides a custom
 * getQuery method based on the current user profile
 * 
 * @author rsoika
 *
 */
@Named
@ViewScoped
public class FavoritesViewController extends ViewController {

	private static final long serialVersionUID = 1L;

	@Inject
	protected UserController userController;

	/**
	 * Generate custom query based on the user profile
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String getQuery() {
		List<String> favorites = null;
		if (userController.getWorkitem() == null) {
			return null;
		}

		// get favorite ids from profile
		favorites = userController.getWorkitem().getItemValue("txtWorkitemRef");
		if (favorites == null || favorites.size() == 0) {
			return null;
		}

		String sQuery = "(type:\"workitem\" OR type:\"workitemarchive\") ";

		// create IN list
		sQuery += " ( ";
		for (String aID : favorites) {
			sQuery += "$uniqueid:\"" + aID + "\" OR ";
		}
		// cut last ,
		sQuery = sQuery.substring(0, sQuery.length() - 3);
		sQuery += " ) ";

		return sQuery;
	}

}
