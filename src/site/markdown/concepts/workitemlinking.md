
# Workitem Linking

Imixs Marty provides a way to link workitems together - independent from the property 
 "$UnqiueIDRef". The uniqueIDs from workitems linked to the current workitem are stored in 
 the property "txtWorkitemRef".

The custom ui widget 'workitemlink' can be used to link a workitem with other workitems.  The widget is provided as a custom ui component and can be added into a jsf page using the marty component library. See the following example:

	 <marty:workitemLink workitem="#{workflowController.workitem}"
	   			hidereferences="false"  filter="$processid:9..." /> 

With the attribute 'minimumChars' it is possible to define the minimum length of a search phrase. The default value is 3. 
 
	 <marty:workitemLink workitem="#{workflowController.workitem}"
	  hidereferences="false"  filter="$processid:9..." minimumChars="5"/> 

The custom tag 'workitemlink' provides the following attributes:

 * workitem : defines the workitem the references should be added to
 
 * filter : a custom filter for the lucene search and display existing references (reg expressions are supported here)

 * hidereferences : default = false - true hides the reference list from the widget.


 
Note: the references displayed by the widget are bound directly to the workitem managed by 
 the workflowController bean. This is independet from the property 'workitem'
 
### Display external references
It is also possible to display workitems referred by the current workitem using the widget _workitemExternalReferences_

	<marty:workitemExternalReferences workitem="#{workflowController.workitem}"
					filter="$processid:9..." /> 

External references can not be edited by the current workitem. 

 
### The Marty Tag Libray
Before you can add the custom component in one of your JSF pages, make sure that you link 
 the marty namespace:

	  <f:subview xmlns="http://www.w3.org/1999/xhtml"
	  .....
	  xmlns:i="http://xmlns.jcp.org/jsf/composite/imixs"
	  xmlns:marty="http://xmlns.jcp.org/jsf/composite/marty
	  .... >

Also the marty javascript library and marty css definition need to be loaded in the 
 header of your jsp page:

	 <h:head>
	   ....
	    <link href="#{facesContext.externalContext.requestContextPath}/marty/imixs-marty.css"
	       charset="UTF-8" type="text/css" rel="stylesheet" />
	   ...
	   <script type="text/javascript"
	     src="#{facesContext.externalContext.requestContextPath}/marty/imixs-marty.js"></script>
	  ....
	  </h:head>

 

##Layout and CSS
There are different CSS classes defined wthin the imixs-marty.css file. You can overwrite
  the layout of the workitemlink widget.

 * marty-workitemlink - defines the main widget container

 * marty-workitemlink-referencebox - containing the reference workitem entries
 
 * marty-workitemlink-referencebox-entry - a single workitem entry

 * marty-workitemlink-inputbox - container with the input field

 * marty-workitemlink-resultlist - the container where the suggest result is presented

 * marty-workitemlink-resultlist-entry - a single workitem entry
 

 