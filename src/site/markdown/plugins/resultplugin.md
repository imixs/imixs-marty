# The ResultPlugin
 The Marty WorkflowCcntroller evaluates the workflow result computed by the standard Imixs ResultPluigin.
 
 If the action result of the activty starts with "workitem=" followed a  uniqueid, then the controller 
 loads that new workitem. In case of a list the first will be taken
 
 
###Example - load parent workitem:
 

	<item name="action">/pages/workitems/workitem?workitem=<itemvalue>$uniqueidref</itemvalue></item>

This example loads the parent workitem into the  page '/pages/workitems/workitem'.
	
###Example - load first worktiem in list:
 
	<item name="action">/pages/workitems/workitem?workitem=<itemvalue separator=";">txtworkitemref</itemvalue></item>

This example loads the first element from the list txtworkitemref into the page /pages/workitems/workitem


###Example - load process in new page:
 
	<item name="action">/pages/admin/process?workitem=<itemvalue separator=";">txtProcessRef</itemvalue></item>

This example loads the process entity into the process admin page.
 
 
##Filter
If more then one workitems are in a list, then an optional filter param can be used to test the content of the workitems:
 
	<item name="action">/pages/workitems/workitem?workitem=<itemvalue separator=";">txtworkitemref</itemvalue>&txtworkflowgroup=Auftrag</item>  

This example loads the first workitem with the fieldvalue "txtWorkflowGroup=Auftrag". 
 
If no workitem can be load or matches a optional filter condition, no new workitem will be loaded.
  