package org.imixs.sywapps.model;

import java.util.StringTokenizer;

/**
 * The ModelData is a helper object that is used in the ProcessTree Selector.
 * Each node in the ProcessTree has a ModelData Field. This Field holds the
 * version number, the name to be displayed and the type of the model part
 * 
 * @author rsoika
 * 
 */
public class ModelData {

	public static final int MODEL_DOMAIN = 0;
	public static final int MODEL_LANGUAGE = 1;
	public static final int MODEL_TYPE = 2;
	public static final int MODEL_PROCESS = 3;

	int modelpart = 0;
	String name = "";

	String version = null;

	int id;

	String domain, language, type, number;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * this constructor expects a modelVerison string area-lang-type-version and
	 * parses its parts
	 * 
	 * @param aVersion
	 * @throws Exception
	 */
	public ModelData(int aModelPart, String aVersion, String aname, int aid)
			throws Exception {
		version = aVersion;
		modelpart = aModelPart;
		id=aid;

		StringTokenizer st = new StringTokenizer(aVersion, "-", false);
		if (st.countTokens() != 4)
			throw new Exception(
					"Illegal ModelVersion - expectd format=domain-lang-type-version");

		domain = st.nextToken();
		language = st.nextToken();
		type = st.nextToken();
		number = st.nextToken();

		switch (modelpart) {
		case MODEL_DOMAIN:
			name = domain;
			break;
		case MODEL_LANGUAGE:
			name = language;
			break;
		case MODEL_TYPE:
			name = type;
			break;
		default:
			name = aname;
			break;
		}

	}

	public int getId() {
		return id;
	}

	public int getModelpart() {
		return modelpart;
	}

	public void setModelpart(int part) {
		this.modelpart = part;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * returns a unique id for each tree node element. This id is needed later to 
	 * identify a node for remove treeNodes
	 * @return
	 */
	public String getNodeID() {
		
		switch (modelpart) {
		case MODEL_DOMAIN:
			return domain;
			
		case MODEL_LANGUAGE:
			return domain+"-"+language;
		case MODEL_TYPE:
			return domain+"-"+language+"-"+type;
			
		default:
			return domain+"-"+language+"-"+type+"-"+id;
		}		
	}
	
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof ModelData) {
			ModelData aData=(ModelData)obj;
			return (  
					this.getNodeID().equals(aData.getNodeID())				
			);
			
		} else
			return false;
	}
	
	
	

}
