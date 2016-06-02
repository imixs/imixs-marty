#The Form Parts

Imixs-office-Workflow provides a set of input elements which can be used to build custom forms or sub-forms. These custom elements are called 'parts' and are explained in the following section.

Each of the input elements provides a set of attribues:

* label - the label to be displayed
* required - indicates if input is required
* imputname - optional field name to store the input value

##datdate.xhtml

This component provides a date input widget. The default itemName is 'datdate'. 

		<ui:include src="/pages/workitems/parts/datdate.xhtml">
			<ui:param name="label" value="Some Label" />
			<ui:param name="required" value="true" />
		</ui:include>
	
##datdate_year_month.xhtml

This component provides a date input widget consisting of a Year and Month Combo-box widget. The day of the date will be set to '01':
		
		<ui:include src="/pages/workitems/parts/datdate_year_month.xhtml">
			<ui:param name="label" value="Some Label" />
			<ui:param name="required" value="true" />
		</ui:include>
		
##namteam.xhtml

Input widget to enter a teamlist: 
		
		<ui:include src="/pages/workitems/parts/namteam.xhtml">
			<ui:param name="label" value="Some Label" />
			<ui:param name="required" value="true" />
		</ui:include>

##txtspaceref.xhtml

Input widget to select a space. The optional param 'byprocess' can be set to 'true' to restrict the selection to spaces assigend to the current process ref of the workitem.		
		
		<ui:include src="/pages/workitems/parts/txtspaceref.xhtml">
			<ui:param name="label" value="Der Bereich" />
			<ui:param name="required" value="true" />
			<ui:param name="byprocess" value="false"></ui:param>
		</ui:include>
		
##namresponsible.xhtml
		
Input widget to enter a single username:
		
		<ui:include src="/pages/workitems/parts/namresponsible.xhtml">
			<ui:param name="label" value="Some Label" />
			<ui:param name="required" value="true" />
		</ui:include>
		



## namresponsiblebyref.xhtml

An input widget to select one username from a team list of a referred team list. 

The following example shows all team members for the current space ref and stores the selection into the item 'namresponsiblespaceteam'

		<ui:include src="/pages/workitems/parts/namresponsiblebyref.xhtml"> 
			<ui:param name="label" value="Some Label" />
			<ui:param name="itemname" value="namresponsiblespaceteam" />
			<ui:param name="itemref" value="namspaceteam" />
		</ui:include>

The next example shows all manager members for the current space ref and stores the selection into the item 'namresponsiblespacemanager'
		
		
		<ui:include src="/pages/workitems/parts/namresponsiblebyref.xhtml"> 
			<ui:param name="label" value="Some Label" />
			<ui:param name="itemname" value="namresponsiblespacemanager" />
			<ui:param name="itemref" value="namspacemanager" />
			
		</ui:include>
