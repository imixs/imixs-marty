package org.imixs.marty.web.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.component.UIData;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.imixs.marty.util.LoginMB;
import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.jee.ejb.EntityService;
import org.richfaces.event.UploadEvent;

/**
 * This Bean extends the fileUploadBean with a lazy-loading mechanism. In
 * different to the FileUploadBean Blobworkitems will be only loaded if a file
 * needs to be added or removed. The information of existing files is stored in
 * the property dms from the parent workitem.
 * 
 * @see org.imixs.marty.web.util.FileUploadBean
 * @author rsoika
 * 
 */
public class DmsMB extends FileUploadBean {

	private ItemCollection configItemCollection = null;
	private ItemCollection dmsItemCollection = null;
	private ArrayList<ItemCollection> workitems = null;
	private int count = 10;
	private int row = 0;
	private boolean endOfList = false;
	private boolean blobWorkitemLoaded = false;

	/* Backing Beans */
	private LoginMB loginMB = null;

	/* EJBs */
	@EJB
	org.imixs.marty.dms.DmsSchedulerService dmsSchedulerService;
	@EJB
	EntityService entityService;

	public DmsMB() {
		super();
	}

	/**
	 * This method tries to load the config entity. If no Entity exists than the
	 * method creates a new entity
	 * 
	 * */
	@PostConstruct
	public void init() {
		super.init();
		doLoadConfiguration(null);
	}

	/**
	 * returns the configuration workitem.
	 * 
	 * @return
	 */
	public ItemCollection getConfiguration() {
		return this.configItemCollection;
	}

	/**
	 * returns the current DMSWorkitem
	 * 
	 * @return
	 */
	public ItemCollection getWorkitem() {
		return this.dmsItemCollection;
	}

	/**
	 * save method tries to load the config entity. if not availabe. the method
	 * will create the entity the first time
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doSaveConfiguration(ActionEvent event) throws Exception {
		// save entity
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);

	}

	/**
	 * This method reloads the configuration entity for the dms scheduler
	 * service
	 */
	public void doLoadConfiguration(ActionEvent event) {
		configItemCollection = dmsSchedulerService.findConfiguration();
	}

	/**
	 * starts the timer service
	 * 
	 * @return
	 * @throws Exception
	 */
	public void doStartScheduler(ActionEvent event) throws Exception {
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = dmsSchedulerService.start();
	}

	public void doStopScheduler(ActionEvent event) throws Exception {
		configItemCollection = dmsSchedulerService
				.saveConfiguration(configItemCollection);
		configItemCollection = dmsSchedulerService.stop();
	}

	public void doRestartScheduler(ActionEvent event) throws Exception {
		doStopScheduler(event);
		doStartScheduler(event);
	}

	/**
	 * This Method Selects the current dms object
	 * 
	 * @return
	 */
	public void doEdit(ActionEvent event) {

		if (event == null) {
			return;
		}

		// find current data row....
		UIComponent component = event.getComponent();

		for (UIComponent parent = component.getParent(); parent != null; parent = parent
				.getParent()) {

			if (!(parent instanceof UIData))
				continue;

			try {
				// get current project from row
				dmsItemCollection = (ItemCollection) ((UIData) parent)
						.getRowData();
				break;
			} catch (Exception e) {
				// unable to select data
			}
		}

	}

	public List<ItemCollection> getWorkitems() {
		if (workitems == null)
			loadDMSWorkitems();
		return workitems;

	}

	/**
	 * resets the current worklist list
	 * 
	 * @return
	 */
	public void doReset(ActionEvent event) {
		workitems = null;
		row = 0;
	}

	public void doLoadNext(ActionEvent event) {
		row = row + count;
		workitems = null;
	}

	public void doLoadPrev(ActionEvent event) {
		row = row - count;
		if (row < 0)
			row = 0;
		workitems = null;
	}

	public int getRow() {
		return row;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	/**
	 * Loads the dms list
	 * 
	 * 
	 * 
	 * @return
	 */
	private List<ItemCollection> loadDMSWorkitems() {
		try {
			workitems = new ArrayList<ItemCollection>();
			String sQuery = "SELECT entity FROM Entity entity "
					+ " JOIN entity.textItems AS r"
					+ "  WHERE entity.type='workitemlob'"
					+ "  AND r.itemName = '$uniqueidref'"
					+ "  AND r.itemValue = '' "
					+ "ORDER BY entity.created DESC";

			Collection<ItemCollection> col = entityService.findAllEntities(
					sQuery, row, count);

			for (ItemCollection aworkitem : col) {
				workitems.add(aworkitem);
			}
			endOfList = col.size() < count;

		} catch (Exception ee) {

			ee.printStackTrace();
		}
		return workitems;
	}

	/**
	 * Overwrite FileUploadBean and implement the lazyLoading mechanism...
	 * 
	 * @param event
	 * @throws Exception
	 */
	@Override
	public void listener(UploadEvent event) throws Exception {
		doLazyLoading();
		super.listener(event);
	}

	/**
	 * Overwrite FileUploadBean and implement the lazyLoading mechanism...
	 */
	@Override
	public void doDeleteFile(ActionEvent event) throws Exception {
		doLazyLoading();
		super.doDeleteFile(event);

		// remove the file also from the current dms property...
		List children = event.getComponent().getChildren();
		String sFileName = "";
		// find the file name....
		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("filename")
						&& currentParam.getValue() != null) {
					// Value can be provided as String or Integer Object
					sFileName = currentParam.getValue().toString();
					break;
				}
			}
		}
		// try to remove the file metadata form the dms...
		if (sFileName != null && !"".equals(sFileName))
			removeMetadata(sFileName);

	}

	/**
	 * This method updates the property dms from the current BlobWorkitem. The
	 * dms holds a List of meta data for each attached file. If a file was
	 * removed also the meta data will be removed.
	 * 
	 * The method only runs if a new file was added or an existing file was
	 * removed.
	 * 
	 * 
	 */
	@Override
	public void onWorkitemProcess(ItemCollection aworkitem) {
		if (blobWorkitemLoaded == false)
			// no changes - exit!
			return;
		else
			// update the dms property....
			updateDmsMetaData(aworkitem);
	}

	/**
	 * Avoid preloading the blobworkitem on a workitemchanged event
	 * 
	 */
	@Override
	public void onWorkitemChanged(ItemCollection aworkitem) {
		blobWorkitemLoaded = false;
		
		// test if dms property still exists - if not create a new one
		if (!getWorkitemBlobBean().getWorkitem().hasItem("dms")) {
			doLazyLoading();
			// create the dms property....
			updateDmsMetaData(aworkitem);
		}
			
		// reset the file upload list
		resetFileUpload();
	}

	/**
	 * This method implements the lazyLoading mechanism for the BlogWOrkitem.
	 * The BlobWorkitem will only be loaded if the flag 'lazyloading' is true.
	 * After the method call the flag will be cleared again.
	 * 
	 */
	private void doLazyLoading() {
		if (!blobWorkitemLoaded) {
			super.onWorkitemChanged(this.getWorkitemBean().getWorkitem());
			// clear flag
			blobWorkitemLoaded = true;
		}
	}

	/**
	 * This method updates the property dms from the current Blobworkitem. The
	 * dms holds a List of Maps with the meta data for each file. The standard
	 * property of the meta data is the field txtName holding the file name. All
	 * other properties are optional. If a file was removed also the meta data
	 * will be removed
	 */
	private void updateDmsMetaData(ItemCollection aworkitem) {
		try {
			// get the current file list
			String[] fileNames = getWorkitemBlobBean().getFiles();

			// get the current dms value....
			Vector<Map> vDMSnew = new Vector<Map>();

			// now we test if for each file entry a dms meta data entry still
			// exists
			for (String aFilename : fileNames) {

				// test filename already exists
				ItemCollection itemCol = findMetadata(aFilename);
				if (itemCol != null) {
					// the meta data exists....
					// update the $ref....
					itemCol.replaceItemValue("$uniqueidRef",
							getWorkitemBlobBean().getWorkitem()
									.getItemValueString("$UniqueID"));
					vDMSnew.add(itemCol.getAllItems());

				} else {
					// no meta data exists.... create a new meta object
					ItemCollection aMetadata = new ItemCollection();

					aMetadata.replaceItemValue("txtname", aFilename);
					aMetadata.replaceItemValue("$uniqueidRef",
							getWorkitemBlobBean().getWorkitem()
									.getItemValueString("$UniqueID"));
					aMetadata.replaceItemValue("$created", new Date());
					aMetadata.replaceItemValue("namCreator", this
							.getLoginBean().getUserPrincipal());

					// Important! do only store the Map not the ItemCollection!!
					vDMSnew.add(aMetadata.getAllItems());
				}

			}
			// update dms property....
			aworkitem.replaceItemValue("dms", vDMSnew);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * this method returns a list of files meta data
	 * 
	 * @return
	 */
	public List<ItemCollection> getFileList() {

		List<Map> vDMS = this.getWorkitemBean().getWorkitem()
				.getItemValue("dms");

		List<ItemCollection> filelist = new ArrayList<ItemCollection>();
		try {

			for (Map aMetadata : vDMS) {

				ItemCollection itemCol;
				// itemCol = new ItemCollection(aMetadata);
				itemCol = new ItemCollection();
				itemCol.setAllItems(aMetadata);
				filelist.add(itemCol);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return filelist;
	}

	/**
	 * This method returns the meta data of a specific file in the exiting list.
	 * 
	 * @return
	 */
	private ItemCollection findMetadata(String aFilename) {

		List<Map> vDMS = this.getWorkitemBean().getWorkitem()
				.getItemValue("dms");

		try {

			for (Map aMetadata : vDMS) {

				ItemCollection itemCol;
				itemCol = new ItemCollection(aMetadata);

				// test if filename matches...
				String sName = itemCol.getItemValueString("txtName");
				if (sName.equals(aFilename))
					return itemCol;

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// no matching meta data found!
		return null;
	}

	/**
	 * This method removes the meta data of a specific file in the exiting list.
	 * 
	 * @return
	 */
	private void removeMetadata(String aFilename) {
		try {
			List<Map> vDMS = this.getWorkitemBean().getWorkitem()
					.getItemValue("dms");

			for (int i = 0; i < vDMS.size(); i++) {
				Map aMetadata = vDMS.get(i);
				ItemCollection itemCol;
				itemCol = new ItemCollection(aMetadata);
				if (itemCol.getItemValueString("txtName").equals(aFilename)) {
					vDMS.remove(i);
					break;
				}

			}

			this.getWorkitemBean().getWorkitem().replaceItemValue("dms", vDMS);
		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	private LoginMB getLoginBean() {
		if (loginMB == null)
			loginMB = (LoginMB) FacesContext
					.getCurrentInstance()
					.getApplication()
					.getELResolver()
					.getValue(FacesContext.getCurrentInstance().getELContext(),
							null, "loginMB");
		return loginMB;
	}

}
