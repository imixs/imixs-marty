package org.imixs.marty.view;

import org.imixs.workflow.ItemCollection;

/**
 * The IQueryBuilder adapts the JQOL statement and Lucene search query used by the
 * SearchControler. You can provide a custom QueryBuilder to adapt the behavior of a search result
 * .
 * 
 * @author rsoika
 *
 */

public interface IQueryBuilder {

	String getSearchQuery(ItemCollection searchFilter);
	
	String getJPQLStatement(ItemCollection queryFilter);
}
