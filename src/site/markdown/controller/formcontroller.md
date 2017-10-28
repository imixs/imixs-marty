# FromController

The CDI Bean FormController provides information about the Form, FormSections and FormParts defined by a Workitem.
 
A form can be defined by the BPMN model . The definition is computed by the ApplicationPlugin and provided in the workitem property 'txtWorkflowEditorid'.
The definition is marked with the '#' character and separated with charater '|' to define the form and formsection:
 
	form_tab#basic_project|sub_timesheet[owner,manager]

## A FormSection

The Class FormSection is provided as a property of the FormController to provide informations about EditorSections  defined in the Model (txtWorkflowEditorID).

The FormSection property can be used to iterate over all sections to dynamically build a from:

	<c:forEach items="#{workitemMB.editorSections}" var="section">
        <ui:include src="/pages/workitems/forms/#{section.url}.xhtml" />
         .....

other Example:   

	rendered="#{! empty workitemMB.editorSection['prototyp/files']}"

