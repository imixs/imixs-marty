# UserInput Widgets

Imixs Marty provides jsf custom input widgets for UserIds or list of UserIds.  These widgets allow the user to search for UserIds in the internal profile list and store the result into a workitem.

## UserListInput widget

The custom widget '_userListInput_' handles list of UserIds. The widget provides a suggest input field for user profiles. See the following example:

	<marty:userListInput value="#{workitem.itemList['nammanager']}" />

The custom tag 'userListInput' provides the following attributes:

 * value : defines the itemList to store the userIds 
 
 * editmode : default = true - if false no input and changes are allowed (read only mode)
Layout

 * render : defines an optional component id to be rendered if the userlist changed. 
 
### How to Render Form Components on Change 

If a part of a form should be rendered when the userIdList changed, the optional attribute 'render' can be set with the component-id of the form fragment. See the following example:


	<div>
		  Users: 	
		  <marty:userListInput value="#{workitem.itemList['_Team']}"
				editmode="#{editmode}" render="#{present.clientId}" />
	</div>
	<div>
		 Present: 
		 <h:panelGroup id="presentid" binding="#{present}">
				<h:selectManyCheckbox layout="pageDirection"
					value="#{workitem.itemListArray['_present']}">
					<c:forEach var="user" items="#{workflowController.workitem.itemList['_Team']}">
						<f:selectItem itemLabel="#{userController.getUserName(user)}"
							itemValue="#{user}"></f:selectItem>
					</c:forEach>
				</h:selectManyCheckbox>
		</h:panelGroup>
	</div>
 
**Note**: To transfere the full component ID to the _userListInput component_, the from fragment must be assigned to an Id and be linked to a property using the 'binding' attribute. Then the full clientID can be transfered to the _userListInput_ component using the expression _present.clientId_. 

  
### Layout the UserInputList
 
There are a set of CSS classes that can be used for the layout of the component. 

 * imixs-username -  input field to search for a users
 * marty-userlistinput - outer div
 * marty-userlistinput-list - div containing the current user list
 * marty-userlistinput-inputbox - the input container to search for profiles containing a suggest feature
 * marty-userlistinput-suggestresultlist - the result list of a search
 * marty-userlistinput-suggestresultlist-entry - a result list entry

# UserInput widget

The custom widget '_userInput_' can be used to handle the input of one userId. The widget  provides a suggest input field for user profiles. See the following example:

	<marty:userInput value="#{workitem.item['nammanager']}" editmode="true" />

 The custom tag 'userInput' provides the following attributes:

 * value : defines the workitem value to store the usernames (do not use itemList here!)
 * editmode : default = true - if false no input and changes are allowed (read only mode)
 * render : defines an optional component id to be rendered if the userlist changed. (see UserListInput)

 
### Layout the UserInputList
There are a set of CSS classes that can be used for the layout of the component. 

 * imixs-usergroup - input field to search for users
 * marty-userinput - outer div
 * marty-userinput-inputbox - the input box to search for profiles containing a suggest feature
 * marty-userinput-suggestresultlist - the result list of a search
 * marty-userinput-suggestresultlist-entry - a result list entry
 

 