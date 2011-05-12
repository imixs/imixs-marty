package org.imixs.sywapps.util;

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
