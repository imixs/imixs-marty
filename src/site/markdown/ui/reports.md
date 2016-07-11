# Reporting

Imixs-office-Workflow provides a user frontend to start imixs-reports from the web ui. 
The report form provides input fields for all parameters defined in a report.


## Date Parameters

If a JPQL parameter starts with the prafix 'date_' the report form will display the parameter input field as an calendar widget.
This is an example of a JPQL report definition with date input fields:

	SELECT workitem FROM Entity AS workitem
	  JOIN workitem.textItems AS p
	  WHERE workitem.type IN( 'workitem' ,'workitemarchive')
	  AND p.itemName='txtworkflowgroup' AND p.itemValue='Order'
	  AND workitem.created BETWEEN '?date_from' AND '?date_to' 
	ORDER BY workitem.created DESC
