package org.imixs.sywapps.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import org.imixs.workflow.ItemCollection;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;

/**
 * This Class handles the different model versions used in SYWAPPs. 
 * Versions can be added with the method addVersion().
 * 
 * ModelVersions are per default expected in the format:
 * 
 * domain-lang-style-version
 * 
 * e.g.: public-en-general-0.0.1
 * 
 * A Domain specifies the general Area the Models are assigend to (e.g. System,
 * public, CustomerA)
 * 
 * Language specifies the Language used in this Model
 * 
 * style is a qualifier for a specific Process Group (e.g. general, IT,
 * automotive)
 * 
 * 
 * The ModelVersionHandler also provides a method to create a RichFaces TreeNode
 * Representation. This methods are used by the projectMB to display a tree of
 * available process models and StartProcessEntities
 * 
 * 
 * The method getLatestVersion(domain,lang,style)) returns the latest version for a specific
 * domain/lang/style
 * 
 * The method getLatestSystemVersion(lang) returns the latest systemversion for
 * a specific language
 * 
 * @see method addVersion() for more details
 * @author rsoika
 * 
 */
public class ModelVersionHandler {

	private Vector<String> versions = null;
	private HashMap<String, Vector<String>> languages = null;
	private HashMap<String, Vector<String>> styles = null;
	private Vector<String> domains = null;

	private String currentDomain;
	private String currentLanguage;
	private String currentStyle;
	private String currentNumber;
	private HashMap<String, String> latestSystemModelVersion = null;

	private TreeNodeImpl modelTree = null;

	public ModelVersionHandler() {
		versions = new Vector<String>();
		domains = new Vector<String>();
		languages = new HashMap<String, Vector<String>>();
		styles = new HashMap<String, Vector<String>>();

		latestSystemModelVersion = new HashMap<String, String>();

	}

	/**
	 * adds a new version identifier and parses model-version string
	 * 
	 * A System Model will be be added to an internal HashMap to store the
	 * latest version System models are starting with präfix 'system-'
	 * 
	 * 
	 * @param aVersion
	 */
	public void addVersion(String aVersion) throws Exception {

		// Check if modelversion is a System Model - latest version will be
		// stored
		if (aVersion.toLowerCase().startsWith("system-")) {
			// now store System Model is current version is newer than last
			// stored model version
			StringTokenizer st = new StringTokenizer(aVersion, "-", false);
			currentDomain = st.nextToken();
			currentLanguage = st.nextToken();
			currentNumber = st.nextToken();

			String lastVersion = (String) latestSystemModelVersion
					.get(currentLanguage);
			if (lastVersion == null || lastVersion.compareTo(aVersion) < 0)
				latestSystemModelVersion.put(currentLanguage, aVersion);
			return;
		}

		// parse version string
		parseModelVersion(aVersion);

		addModelTreeNode(getModelTree(), aVersion, null);

		if (versions.indexOf(aVersion) == -1)
			versions.add(aVersion);

		// add area
		if (domains.indexOf(currentDomain) == -1)
			domains.add(currentDomain);

		// add language for domain
		String key = currentDomain;
		Vector<String> vectorLangs = (Vector<String>) languages.get(key);
		if (vectorLangs == null) {
			vectorLangs = new Vector<String>();
		}
		if (vectorLangs.indexOf(currentLanguage) == -1)
			vectorLangs.add(currentLanguage);
		languages.put(key, vectorLangs);

		// add ModelStyle, language and Domain
		key = currentDomain + "-" + currentLanguage;
		Vector<String> vectorTyps = (Vector<String>) styles.get(key);
		if (vectorTyps == null) {
			vectorTyps = new Vector<String>();
		}
		if (vectorTyps.indexOf(currentStyle) == -1)
			vectorTyps.add(currentStyle);
		styles.put(key, vectorTyps);

	}

	/**
	 * returns a the latest version number to a specific domain/lang/style
	 * combination
	 * 
	 * @param domain
	 * @param lang
	 * @param style
	 * @return
	 */
	public String getLatestVersion(String domain, String lang, String style) {
		String key = domain + "-" + lang + "-" + style;
		String sBestModel = null;
		for (String aModelVersion : versions) {
			if (aModelVersion.startsWith(key))
				sBestModel = aModelVersion;

		}
		return sBestModel;

	}

	/**
	 * returns the latest System Version number
	 * 
	 * @param lang
	 * @return
	 */
	public String getLatestSystemVersion(String lang) {
		String lastVersion = (String) latestSystemModelVersion.get(lang);

		return lastVersion;

	}

	/**
	 * adds an array of supported languages for a specific model area
	 * 
	 * @param aArea
	 * @return
	 */
	public Collection<String> getLanguageSupport(String aModelDomain) {

		Vector<String> vectorLangs = (Vector<String>) languages
				.get(aModelDomain);
		if (vectorLangs == null) {
			vectorLangs = new Vector<String>();
		}

		return vectorLangs;

	}

	/**
	 * adds an array of supported Model Types for a specific model Domain and
	 * Language
	 * 
	 * @param aArea
	 *            and Language
	 * @return String with supported Types
	 */
	public Collection<String> getModelStyles(String aModelDomain,
			String aLanguage) {
		String key = aModelDomain + "-" + aLanguage;
		Vector<String> vectorTyps = (Vector<String>) styles.get(key);
		if (vectorTyps == null) {
			vectorTyps = new Vector<String>();
		}

		return vectorTyps;

	}

	/**
	 * returns a richFacess TreeNode containing all Model informations The tree
	 * is build during the addVersion() method.
	 * 
	 * 
	 * @return
	 */
	public TreeNodeImpl getModelTree() {
		if (modelTree != null)
			return modelTree;

		// create new TreeNode Instance....
		modelTree = new TreeNodeImpl();
		return modelTree;
	}

	/**
	 * This method is called by the addVersion method and adds all corresponding
	 * TreeNode Elements for a ModelVersion into the given root modelTree
	 * 
	 * If a startProcess ItemCollection is supported the method adds also a leaf
	 * for the StartProcess under the ModelTypeNode. If no startProcess
	 * ItemCollection is supported the method will generate a dynamic list of
	 * start processTress per default. Therefore the method creates Nodes of the
	 * type 'ProcessGroupsTreeNode' This is a dynamic implementation of a
	 * treeNode containing all StartProcess Nodes. The ProcessNodes are only
	 * computed if the node is expanded by the user during a ajax request.
	 * 
	 * The method adds nodes for each element of a model version - domain -
	 * language - type
	 * 
	 * The method is called by addVersion method of this class to generate the
	 * ProcessTree Node Selection for a project and also by the ProjectMB to
	 * generate a ProcessTree of selected StartProcesses for a specific Project
	 * 
	 * 
	 * @param amodelTree
	 *            - the root model tree node where the nodes should be added
	 * @param aVersion
	 *            - the version string for a node set
	 * @param aStartProcess
	 *            - optional ItemCollection of a StartProcess Entity to be added
	 *            as a leaf in the processNodetree
	 * 
	 * @throws Exception
	 */
	public void addModelTreeNode(TreeNode amodelTree, String aVersion,
			ItemCollection processEntity) throws Exception {

		StringTokenizer st = new StringTokenizer(aVersion, "-", false);
		if (st.countTokens() != 4)
			throw new Exception(
					"Illegal ModelVersion - expectd format=domain-lang-type-version");

		String domain = st.nextToken();
		String aLanguage = st.nextToken();
		String aType = st.nextToken();

		// try to find Domain node
		ModelData mdDomain = new ModelData(ModelData.MODEL_DOMAIN, aVersion,
				null, -1);
		TreeNode nodeDomain = amodelTree.getChild(mdDomain.getNodeID());
		if (nodeDomain == null) {
			// add new domain Node
			nodeDomain = new TreeNodeImpl();
			nodeDomain.setData(mdDomain);
			amodelTree.addChild(mdDomain.getNodeID(), nodeDomain);

		}

		// try to find Language node
		ModelData mdLanguage = new ModelData(ModelData.MODEL_LANGUAGE,
				aVersion, null, -1);
		TreeNode nodeLanguage = nodeDomain.getChild(mdLanguage.getNodeID());
		if (nodeLanguage == null) {
			// add new language Node
			nodeLanguage = new TreeNodeImpl();
			nodeLanguage.setData(mdLanguage);
			nodeDomain.addChild(mdLanguage.getNodeID(), nodeLanguage);

		}

		// try to find Type node
		// if aStartProcessID ==-1 the node will be added as a dynamic
		// ProcessGroupsTreeNode which
		// expands dynamically the start processes under this node
		ModelData mdType = new ModelData(ModelData.MODEL_TYPE, aVersion, null,
				-1);
		TreeNode nodeType = nodeLanguage.getChild(mdType.getNodeID());
		if (nodeType == null) {

			if (processEntity == null) {
				// add new ProcessGroups Node
				nodeType = new ProcessGroupsTreeNode();
				nodeType.setData(mdType);
				nodeLanguage.addChild(mdType.getNodeID(), nodeType);
			} else {
				// add a normal typ node to add later the start process as a
				// leaf node for this processID

				// add new language Node
				nodeType = new TreeNodeImpl();
				nodeType.setData(mdType);
				nodeLanguage.addChild(mdType.getNodeID(), nodeType);

			}
		}

		// add ProcessNode as a leaf if (processEntity!=null)
		if (processEntity != null) {
			String sGroupName = processEntity
					.getItemValueString("txtWorkflowGroup");
			Integer iProccessID = processEntity
					.getItemValueInteger("numProcessID");

			// add new language Node
			TreeNode nodeProcess = new TreeNodeImpl();

			// optimize groupName with (version)
			sGroupName = sGroupName + " ("
					+ aVersion.substring(aVersion.lastIndexOf("-") + 1) + ")";

			ModelData mdProcess = new ModelData(ModelData.MODEL_PROCESS,
					aVersion, sGroupName, iProccessID);
			nodeProcess.setData(mdProcess);

			nodeType.addChild(mdProcess.getNodeID(), nodeProcess);

		}

	}

	/**
	 * This method removes a specific TreeNode form a TreeRootNode. The treenode
	 * to be removed is identified by the corresponding ModelData object
	 * 
	 * @param aNodeRoot
	 * @param adata
	 */
	@SuppressWarnings("unchecked")
	public void removeModelTreeNode(TreeNode aNodeRoot, ModelData adata) {
		java.util.Iterator<java.util.Map.Entry> iterChilds = aNodeRoot
				.getChildren();
		while (iterChilds.hasNext()) {
			Map.Entry aEntry = iterChilds.next();
			TreeNode childNode = (TreeNode) aEntry.getValue();
			// check if dataobject is equal...
			ModelData childData = (ModelData) childNode.getData();
			if (childData.equals(adata)) {
				// aNodeRoot.removeChild(adata.getNodeID());
				// aNodeRoot.removeChild(childNode);
				aNodeRoot.removeChild(aEntry.getKey());
				break;
			}
			// if not a leaf call method recursively
			if (!childNode.isLeaf())
				removeModelTreeNode(childNode, adata);
		}

	}

	/**
	 * ModelVersions are expected in the format
	 * 
	 * area-lang-typ-version
	 * 
	 * @param aVersion
	 * @throws Exception
	 *             throws an exeption if format is illegal
	 */
	private void parseModelVersion(String aVersion) throws Exception {

		StringTokenizer st = new StringTokenizer(aVersion, "-", false);
		if (st.countTokens() != 4)
			throw new Exception(
					"Illegal ModelVersion - expectd format=area-lang-type-version");

		currentDomain = st.nextToken();
		currentLanguage = st.nextToken();
		currentStyle = st.nextToken();
		currentNumber = st.nextToken();

	}

}
