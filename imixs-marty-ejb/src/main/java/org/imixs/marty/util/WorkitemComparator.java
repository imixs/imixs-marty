package org.imixs.marty.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import javax.faces.context.FacesContext;

import org.imixs.workflow.ItemCollection;

/**
 * The WorkitemComparator provides a Comparator for ItemColections. The item to
 * be compared can be provided in the constructor.
 * 
 * @author rsoika
 * 
 */
public class WorkitemComparator implements Comparator<ItemCollection> {
	private final Collator collator;
	private final boolean ascending;
	private final String itemName;

	public WorkitemComparator(String aItemName, boolean ascending, Locale locale) {
		this.collator = Collator.getInstance(locale);
		this.ascending = ascending;
		itemName = aItemName;
	}

	public WorkitemComparator(String aItemName, boolean ascending) {
		// get user locale...
		Locale locale = FacesContext.getCurrentInstance().getViewRoot()
				.getLocale();

		this.collator = Collator.getInstance(locale);
		this.ascending = ascending;
		itemName = aItemName;

	}

	public int compare(ItemCollection a, ItemCollection b) {

		// date compare?
		if (a.isItemValueDate(itemName)) {

			Date dateA = a.getItemValueDate(itemName);
			Date dateB = b.getItemValueDate(itemName);
			int result = dateB.compareTo(dateA);
			if (!this.ascending) {
				result = -result;
			}
			return result;

		}

		// integer compare?
		if (a.isItemValueInteger(itemName)) {
			int result = a.getItemValueInteger(itemName)-b.getItemValueInteger(itemName);
			if (!this.ascending) {
				result = -result;
			}
			return result;

		}

		// String compare
		int result = this.collator.compare(a.getItemValueString(itemName),
				b.getItemValueString(itemName));
		if (!this.ascending) {
			result = -result;
		}
		return result;
	}

}
