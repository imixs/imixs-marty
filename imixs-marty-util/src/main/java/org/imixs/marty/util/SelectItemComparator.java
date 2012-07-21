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

package org.imixs.marty.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import javax.faces.model.SelectItem;

/**
 * Sorts a ArrayList of SelectItems by label 
 * @author rsoika
 *
 */ 
public class SelectItemComparator implements Comparator<SelectItem> {
	private final Collator collator;

	private final boolean ascending;

	public SelectItemComparator(Locale locale, boolean ascending) {
		this.collator = Collator.getInstance(locale);
		this.ascending = ascending;
	}

	public int compare(SelectItem a, SelectItem b) {
		int result = this.collator.compare(a.getLabel(), b.getLabel());
		if (!this.ascending) {
			result = -result;
		}
		return result;
	}

}
