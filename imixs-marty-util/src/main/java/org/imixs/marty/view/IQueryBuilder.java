package org.imixs.marty.view;

import org.imixs.workflow.ItemCollection;

/**
 * The IQueryBuilder adapts the JPQL statement and Lucene search query used by
 * the SearchControler. You can provide a custom QueryBuilder to adapt the
 * behavior of a search result .
 * 
 * @author rsoika
 * 
 */

public interface IQueryBuilder {

	boolean isSearchMode(ItemCollection searchFilter);

	/**
	 * Returns a Lucene search query based on the define searchFilter parameter
	 * set
	 * 
	 * @param searchFilter
	 *            - ItemCollection with filter criteria
	 * 
	 * @return - a lucene search query
	 */
	String getSearchQuery(ItemCollection searchFilter);

	/**
	 * Returns a JPQL statement based on the defined searchFilter parameter set
	 * 
	 * @param searchFilter
	 *            - ItemCollection with filter criteria
	 * 
	 * @return - a JQPL statement
	 */
	String getJPQLStatement(ItemCollection queryFilter);
}
