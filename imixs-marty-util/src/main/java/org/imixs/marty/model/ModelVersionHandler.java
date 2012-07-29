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

import java.util.Collection;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Vector;


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
