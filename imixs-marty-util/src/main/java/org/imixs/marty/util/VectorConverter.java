/*******************************************************************************
 *  Imixs IX Workflow Technology
 *  Copyright (C) 2003, 2008 Imixs Software Solutions GmbH,  
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
 *  Contributors:  
 *  	Imixs Software Solutions GmbH - initial API and implementation
 *  	Ralph Soika
 *  
 *******************************************************************************/
package org.imixs.marty.util;

import java.util.ListIterator;
import java.util.Vector;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

/*
 * für ConfigItem benutzter Converter, der einen Komma-separierten String in einen Vektor umwandelt
 * und umgekehrt.
 * Das ist alles noch sehr basic und ich fürchte auch nicht sehr defensiv programmiert.
 * 
 * Noch dringend zu tun:
 * - Dem Converter im Fehlerfall noch eine eigene Fehlermeldung mitgeben
 * - müssen da nicht noch eine Menge try-catch blöcke und Typ-Prüfungen rein?
 *   Derzeit geht das alles sehr optimistisch davon aus, dass in dem Vektor wirklich
 *   auch Strings drin sind; was eigentlich auch der Fall ist. Interessant wird es, wenn
 *   man bestehende Felder umbiegt.
 *    
 * Schön wäre noch folgendes:
 * - Den Separator im converter-tag der JSP Seite definieren. Das wird allerdings ein Act (Vorgehen
 *   beschrieben in Kap. 20.4 in "Kito Mann - JSF in Action")
 */

public class VectorConverter implements Converter {

	String separator = "\n";

	public Object getAsObject(FacesContext context, UIComponent component,
			String value) throws ConverterException {

		
		Vector v = new Vector();
		String[] tokens = value.split(separator);
		for (int i = 0; i < tokens.length; i++) {
			v.addElement(tokens[i].trim());
		}

		return v;

	}

	public String getAsString(FacesContext context, UIComponent component,
			Object value) throws ConverterException {

		// test if object is vector
		if (! (value instanceof Vector))
			return value.toString();
		
		String s = "";
		
		Vector vValues = (Vector)value;
		ListIterator li = vValues.listIterator();
		while(li.hasNext()){
			if(li.hasPrevious()){
				s += separator;
			}
			s += li.next();
		}
		
		return s;

	}

}
