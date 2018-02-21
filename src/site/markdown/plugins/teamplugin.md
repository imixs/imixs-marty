# The TeamPlugin

The Marty TeamPlugin organizes the hierarchical order of a workitem between
processes, spaces and workitems and computes the users associated with an orgunit.  
 
     org.imixs.marty.plugins.TeamPlugin


A WorkItem is typically assigned to one or more orgunits. These references are stored in the item _$UniqueIDRef_. 
TeamPlugin automatically computes the references and stores the information into the items 
_txtProcessRef_ and _txtSpaceRef_ which containing only uniqueIDs of the corresponding orgunit type.

The items _txtProcessRef_ and _txtSpaceRef_ can also be modified by the workflow model or a custom business logic.
 
The Marty TeamPlugin computes additional workflow properties:

  
| Item       		| Type      | Description                               						|
|-------------------|-----------|-------------------------------------------------------------------|
|namSpaceTeam   	| names		| current team members of an associated space orgunit. 				|
|namSpaceManager	| names   	|current managers of an associated space orgunit.					|
|namSpaceAssist		| names   	|current assists of an associated space orgunit. 					|
|namSpaceName		| text		|name of  an associated space orgunit. 								| 
|txtSpaceRef		| text		|$uniqueID  of an associated space orgunit. 						| 
|namProcessTeam		| names		|current team members of an associated process orgunit. 			| 
|namProcessManager	| names		|current managers of an associated process orgunit. 				| 
|namProcessAssist	| names		|current assists of an associated process orgunit. 					| 
|namProcessName		| text		|name of  an associated process orgunit. 							| 
|txtProcessRef		| text		|$uniqueID  of an associated process orgunit.						| 
 
The name items can be used in ACL settings or mail settings.
 
The item '_txtProcessRef_' and '_txtSpaceRef_' are optional and can update the current $uniqueIDs for referenced orgunits. 
The Plug-in updates the item _$UniqueIDRef_ automatically if these properties are filled.

### Evaluate a Orgunit

If the workflow result message of an Imixs-Event contains a space or process reference the plug-in will update the references

Example:

	<item name="space">...</item>
	<item name="process">...</item>

