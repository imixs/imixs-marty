# WorklistController

 The backing Bean org.imixs.marty.web.workitem.WorklistController is used to manage collections of workitems. 
 The bean is used in view scope to display the current result set of a worklist selection or a search result:

    <h:dataTable var="workitem" value="#{worklistController.workitems}">

 The WorklistMB also provides methods to navigate through a list of workitems (also called paging)

	  <h:commandButton actionListener="#{worklistController.doLoadPrev}"
	    disabled="#{worklistMB.row==0}" value="#{global.prev}">
	  </h:commandButton>
	  <h:commandButton actionListener="#{worklistMB.doLoadNext}"
	 	disabled="#{worklistMB.endOfList}" value="#{global.next}">
	  </h:commandButton>
