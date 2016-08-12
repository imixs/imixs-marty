# WorkflowController
 
The marty-util project provides a custom workflow controller bean to process workitems by the Imixs-Workflow engine, called 

    workflowController

The workflowController is independent from the type 'workitem' and is used not only to process workitems but also entities form the type 

 * profile
 * process
 * space



## How to use
The WorkflowController provides easy acccess to properties of a workitem. Input fields can be bound to any properties of a workitem:

	 <h:panelGroup layout="block"  >
	  <h:outputLabel value="#{workflowController.workitem.item['txtaddress']}" />
	  <h:outputLabel value="#{workflowController.workitem.item['txtzip']} #{workitemMB.item['txtcity']}" />
	  <h:outputLabel value="#{workflowController.workitemB.item['txtstate']}" />
	  <h:outputLabel value="#{workflowController.workitem.item['txtcountry']}" />
	 </h:panelGroup>

### Editor and EditorSections

The workflowController implements getter methods to compute a form and subforms based on the property "txtWorkflowEditorID" of a workitem. 
The 'editor' property can be used ot load a form:

	<ui:include src="/pages/workitems/forms/#{workflowController.editor}.xhtml" />
							
The 'editorSections' propert can be used to load subforms e.g. in tabs:

	<c:forEach items="#{workflowController.editorSections}" var="section">
			<div class="imixs-form-panel">
				<h1>
					<h:outputText value="#{section.name}" />
				</h1>
				<f:subview>
					<ui:include src="/pages/workitems/forms/#{section.url}.xhtml">
						<ui:param name="workitem" value="#{workflowController.workitem}" />
					</ui:include>
				</f:subview>
			</div>
	</c:forEach>

The format to provide an editor and optional editor sections is:

	form#subform|subform[...]

e.g:

	form_tab#basic_order|sub_agreement|sub_documentation
   


## The Action Result

The Marty WorkflowController is implementing a custom behavior to redirect the user after a workitem was processed successfully. An action result can be defined in an BPMN Event of the Imixs workflow model. The workflow result for an action has the following format:

    <item name="action">NAVTGATION-RULE</item>

The Marty WorkflowController redirects the user automatically to the action provided by the model. If no navigation rule is defined, the default action will be returned (/pages/workitems/workitem). The default action can also be overwritten by the property _defaultActionResult_.

If the action result contains no _faces-redirect_ the process method will automatically append a faces-redirect=true to redirect the user by a so called 'post-get-request' after a workitem was processed. To avoid this default behavior a action result can set the faces-redirect to 'false'. 

E.g., the action result:

    /pages/worklist
    
will be translated in 

    /pages/worklist?faces-redirect=true

__Note:__ A Navigation Rule will not be changed by the  WorkflowController and need to be configured using the faces-config.xml file. 
 
### Loading a new Workitem

After processing a workitem, also a new workitem can be loaded by specifying the uniqueID in the action result. See the following example of a navigation rule:

    /pages/workitems/workitem?id=<UNIQUEID>
 
This rule will result in
 
    /pages/workitems/workitem?id=<UNIQUEID>&faces-redirect=true
 
To load the workitem, the corresponding JSF page need to provide the optional _deepLinkId_ viewParam, which is supported by the Marty WorkflowController:

	<h:head>
		<!-- support deep link for workitems to be loaded by the WorkflowController (optional params 'id' and 'workitem' supported) -->
		<f:metadata>
			<f:viewParam name="workitem" value="#{workflowController.deepLinkId}" />
			<f:viewParam name="id" value="#{workflowController.deepLinkId}" />
		</f:metadata>
	</h:head>


## WorkflowEvents

The WorkflowController fires CDI events depending on the state of the workitem which can be consumed by other CDI beans in an application. The event handling is implemented using the CDI observer pattern (JSR-299/JSR-330). To consume an event a CDI bean have to implement a method with a @Observes annotation for the WorkflowEvent.

    public void onWorkflowEvent(@Observes WorkflowEvent workflowEvent) {
         .....
     }   

Events are only triggered by the CDI Bean WorkflowController. If a Workitem is updated or processed by the WorkflowService EJB directyl in the backend (e.g. from the TimerSerivce) no events will be fired.

 * WORKITEM_INITIALIZED - fired during the workflowController is initialized

 * WORKITEM_CREATED - fired after a new empty woritem was created

 * WORKITEM_CHANGED - fired when the workitem has changed (method setWorkitem() called). The event is typical fired if a workitem was clicked by the user in a view. Note: this event is not fired if a workitem was created or during it is processed!

 * WORKITEM_BEFORE_PROCESS - fired before a workitem will be processed by the workflow engine

 * WORKITEM_AFTER_PROCESS - fired after a workitem was processed by the workflow engine


The controller fires the WORKITEM_BEFORE_PROCESS event before processing a workitem and the  WORKITEM_AFTER_PROCESS event after a successful processing life cycle. 

## Conversation State
The marty workflowController inherits from the 'org.imixs.workflow.jee.faces.workitem.WorkflowController' and implements a custom conversation state. 
The CDI controller is annotated with '@ConversationScoped' which allows to open different browser windows/tabs by one user to load and process the same or different workitems. 
A new conversation is started when the methods 'create' or 'load' are called. A running conversation is closed when the method 'close' or 'process' was called .
 