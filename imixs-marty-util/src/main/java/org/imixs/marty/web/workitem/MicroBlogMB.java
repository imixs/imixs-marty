package org.imixs.marty.web.workitem;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.business.WorkitemServiceBean;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.util.ItemCollectionAdapter;

/**
 * This MicroBlogMB is used to store a micro blog into a single ItemCollection
 * mapped to a exiting Workitem. The MicroBlogMB supports methods to add
 * comments (mirco blogs or tweets).
 * <p>
 * The MicroBlogMB is always bounded to a parent workitem by its referrer id
 * ($uniqueidRef). An application can implement a lazy loading for MicroBlogMB.
 * 
 * The readaccess of the MicroBlogWorkitem is always synchronized to the
 * settings of the parent workitem. The writeAccess of the MicroBlogWorkitem is
 * set to "org.imixs.ACCESSLEVEL.AUTHORACCESS". So every reader of a workitem
 * can write into a MicroBlogWorkitem.
 * 
 * Before the BlobWorkitem can be accessed the workitem needs to be loaded by
 * the load() method. The Data can be accessed by the embedded Itemcollection
 * through the method getWorkitem(). The BlobWorkitem can be saved by calling
 * the save() method. Both - the load() and the save() method expect the Parent
 * ItemCollection where the BlobWorkitem should be bound to.
 * 
 * 
 * This bean provides a blog function to history blog entered by a user into the
 * field txtBlog. The Bean creates the property txtBlogLog with a Map providing
 * following informations per each blog entry:
 * 
 * <ul>
 * <li>txtComment = comment</li>
 * <li>datComment = date of creation</li>
 * <li>namEditor = user name</li>
 * </ul>
 * 
 * The property log provides a ArrayList with ItemCollection Adapters providing
 * the comment details.
 * 
 * @version 0.0.1
 * @author rsoika
 * 
 */
public class MicroBlogMB {

	public final static String TYPE = "microblogworkitem";
	@EJB
	org.imixs.workflow.jee.ejb.EntityService entityService;

	private ItemCollection blogWorkitem = null;
	private ItemCollection blogEntry = null;
	protected ItemCollectionAdapter workitemAdapter = null;

	public MicroBlogMB() {
		super();

		// set the workitemAdapter
		clear();
	}

	/**
	 * update the workitemAdapter and the blobworkitem reference
	 * 
	 * @param aItemcol
	 */
	public void setWorkitem(ItemCollection aItemcol) {
		blogEntry = aItemcol;
		workitemAdapter = new ItemCollectionAdapter(blogEntry);
	}

	/**
	 * returns the ItemCollection for the curren BlobWorkitem object.
	 * 
	 * @return
	 */
	public ItemCollection getWorkitem() {
		return blogEntry;
	}

	/**
	 * Removes the connection to the parend workitem and clear the
	 * itemCollection
	 */
	public void clear() {
		setWorkitem(new ItemCollection());
	}

	/**
	 * This method updates the $uniqueIDRef property and the read- and write
	 * access. Finally the method saves the current BlogWorkitem. Therefore the
	 * method copies the read- and write access list from the given parent
	 * workitem into the BlogWorkitem before save.
	 * 
	 * So this method should be called after a WorkflowProcessing step to update
	 * the read- and write access identically to a valid parentWorkitem
	 * <p>
	 * The method did not save the blogWorkitem if the parent workitem has no
	 * $unqiueID!
	 * <p>
	 * 
	 * @throws Exception
	 */
	public void save(ItemCollection parentWorkitem) throws Exception {

		if (blogWorkitem != null && parentWorkitem != null) {

			// verify if a uniqueid is still defined. If not return without
			// saving!
			if ("".equals(parentWorkitem.getItemValueString("$uniqueID")))
				return;

			// Update Read access list from parent workitem
			Vector vAccess = parentWorkitem.getItemValue("$ReadAccess");
			blogWorkitem.replaceItemValue("$ReadAccess", vAccess);

			// Update the Write access to static
			// 'org.imixs.ACCESSLEVEL.AUTHORACCESS'
			blogWorkitem.replaceItemValue("$WriteAccess",
					"org.imixs.ACCESSLEVEL.AUTHORACCESS");

			blogWorkitem.replaceItemValue("$uniqueidRef",
					parentWorkitem.getItemValueString("$uniqueID"));
			blogWorkitem.replaceItemValue("type", TYPE);
			// Update blogWorkitem
			blogWorkitem = entityService.save(blogWorkitem);
			// update adapter
			setWorkitem(blogWorkitem);

		}
	}

	/**
	 * Loads the BlobWorkitem of a given parent Workitem. The BlobWorkitem is
	 * identified by the $unqiueidRef. If no BlobWorkitem still exists the
	 * method creates a new empty BlobWorkitem which can be saved later.
	 * 
	 * @param itemCol
	 *            - parent workitem where the BlobWorkitem will be attached to
	 * @throws Exception
	 */
	public void load(ItemCollection itemCol) throws Exception {
		blogWorkitem = null;
		String sUniqueID = null;

		if (itemCol != null)
			sUniqueID = itemCol.getItemValueString("$uniqueid");

		if (!"".equals(sUniqueID)) {
			// search entity...
			String sQuery = " SELECT lobitem FROM Entity as lobitem"
					+ " join lobitem.textItems as t2"
					+ " WHERE lobitem.type = '" + TYPE + "'"
					+ " AND t2.itemName = '$uniqueidref'"
					+ " AND t2.itemValue = '" + sUniqueID + "'";

			Collection<ItemCollection> itemcol = entityService.findAllEntities(
					sQuery, 0, 1);
			if (itemcol != null && itemcol.size() > 0) {
				blogWorkitem = itemcol.iterator().next();
			}
		}

		// create new if no one found
		if (blogWorkitem == null) {
			blogWorkitem = new ItemCollection();
			blogWorkitem.replaceItemValue("type", TYPE);
		}

	}

	/**
	 * adds a new blog entry into the txtMicroBlog field. The Method saves the
	 * MicorBlog Entity if a $UniqueIDRef is still available. The The MicroBlog
	 * Entity holds no reference to a parentWorkitem the method updatAccess()
	 * should be called
	 * 
	 * The BlogEntry will be automatically cleared by this method.
	 */
	public void doAddEntry(ActionEvent event) {
		// take comment and append it to txtCommentList
		try {
			Vector<Map> vCommentList = blogWorkitem
					.getItemValue("txtMicroBlog");

			// add date and modifier

			blogEntry.replaceItemValue("datEntry", Calendar.getInstance()
					.getTime());
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			blogEntry.replaceItemValue("nameditor", remoteUser);
			vCommentList.add(0, blogEntry.getAllItems());

			blogWorkitem.replaceItemValue("txtMicroBlog", vCommentList);

			// save workitem if $uniqueidRef is availalbe
			if (!"".equals(blogWorkitem.getItemValueString("$UniqueIDRef"))) {
				blogWorkitem = entityService.save(blogWorkitem);
				// update adapter
				setWorkitem(blogWorkitem);
			}

			// clear last comment
			doCreateBlogEntry(event);

		} catch (Exception e) {
			// unable to copy comment
			e.printStackTrace();
		}
	}

	/**
	 * adds the current user to the owner list of the MicroBlog. The Method
	 * saves the MicorBlog Entity if a $UniqueIDRef is still available. The The
	 * MicroBlog Entity holds no reference to a parentWorkitem the method
	 * updatAccess() should be called
	 * 
	 */
	public void doFollow(ActionEvent event) {
		// take comment and append it to txtCommentList
		try {
			Vector<String> vownerList = blogWorkitem.getItemValue("namOwner");

			// add user?
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			if (vownerList.indexOf(remoteUser) == -1) {
				vownerList.add(remoteUser);

				blogWorkitem.replaceItemValue("namOwner", vownerList);

				// save workitem if $uniqueidRef is available
				if (!"".equals(blogWorkitem.getItemValueString("$UniqueIDRef"))) {
					blogWorkitem = entityService.save(blogWorkitem);
					// update adapter
					setWorkitem(blogWorkitem);
				}
			}
		} catch (Exception e) {
			// unable to copy comment
			e.printStackTrace();
		}
	}

	/**
	 * removes the current user from the owner list of the MicroBlog. The Method
	 * saves the MicorBlog Entity if a $UniqueIDRef is still available. The The
	 * MicroBlog Entity holds no reference to a parentWorkitem the method
	 * updatAccess() should be called
	 * 
	 */
	public void doUnfollow(ActionEvent event) {
		// take comment and append it to txtCommentList
		try {
			Vector<String> vownerList = blogWorkitem.getItemValue("namOwner");

			// remove user?
			FacesContext context = FacesContext.getCurrentInstance();
			ExternalContext externalContext = context.getExternalContext();
			String remoteUser = externalContext.getRemoteUser();
			if (vownerList.indexOf(remoteUser) > -1) {
				vownerList.remove(remoteUser);
				blogWorkitem.replaceItemValue("namOwner", vownerList);
				// save workitem if $uniqueidRef is available
				if (!"".equals(blogWorkitem.getItemValueString("$UniqueIDRef"))) {
					blogWorkitem = entityService.save(blogWorkitem);
					// update adapter
					setWorkitem(blogWorkitem);
				}
			}
		} catch (Exception e) {
			// unable to copy comment
			e.printStackTrace();
		}
	}

	/**
	 * This method creates a new empty blog Entry
	 * 
	 * @param event
	 * @return
	 */
	public void doCreateBlogEntry(ActionEvent event) throws Exception {

		this.setWorkitem(new ItemCollection());

	}

	/**
	 * returns the ItemCollection for the current Blog Entry inside the
	 * BlobWorkitem object.
	 * 
	 * @return
	 */
	public ItemCollection getBlogEntry() {
		return blogEntry;
	}

	/**
	 * returns a ItemCollectionAdapter list containing the Blog Entries
	 * 
	 * @return
	 */
	public List<ItemCollectionAdapter> getBlogEntries() {
		List<ItemCollectionAdapter> blog = new ArrayList<ItemCollectionAdapter>();

		Vector<Map> vCommentList = blogWorkitem.getItemValue("txtMicroBlog");

		for (Map map : vCommentList) {
			try {
				blog.add(new ItemCollectionAdapter(new ItemCollection(map)));
			} catch (Exception e) {
				// unable to add entry!
				e.printStackTrace();
			}

		}

		return blog;
	}

	/**
	 * returns the current selected row as a ItemCollectionAdapter
	 * 
	 * @param event
	 * @return
	 * @throws Exception
	 */

	private ItemCollectionAdapter xxxxxxxxxxxxgetCurrentSelection(
			ActionEvent event) throws Exception {

		ItemCollectionAdapter currentSelection = null;
		// suche selektierte Zeile....
		UIComponent component = event.getComponent();
		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {
			if (!(parent instanceof UIData))
				continue;

			// Zeile gefunden
			currentSelection = (ItemCollectionAdapter) ((UIData) parent)
					.getRowData();

			return currentSelection;
		}
		return null;
	}

	/**
	 * returns a workitemAdapter Item field
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map getItem() throws Exception {
		return workitemAdapter.getItem();
	}

	/**
	 * returns a workitemAdapter ItemList
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map getItemList() throws Exception {
		return workitemAdapter.getItemList();
	}

	/**
	 * returns a workitemAdapter ItemList Array
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map getItemListArray() throws Exception {
		return workitemAdapter.getItemListArray();
	}

}
