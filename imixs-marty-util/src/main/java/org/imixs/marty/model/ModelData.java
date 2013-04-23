/*******************************************************************************
 *  Imixs Workflow 
 *  Copyright (C) 2001, 2011 Imixs Software Solutions GmbH,  
 *  http://www.imixs.com
 *  
 *  This program is free software; you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License 
 *  as published by the Free Software Foundation; either version 2 
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 *  General Public License for more details.
 *  
 *  You can receive a copy of the GNU General Public
 *  License at http://www.gnu.org/licenses/gpl.html
 *  
 *  Project: 
 *  	http://www.imixs.org
 *  	http://java.net/projects/imixs-workflow
 *  
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika - Software Developer
 *******************************************************************************/

package org.imixs.marty.model;

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
