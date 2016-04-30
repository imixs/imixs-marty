#The SpacePlugin Guide WildFly

The SpacePlugin is a system plugin running only in the system workflow group 'space'.

The plugin computes and updates the child and parent information of a space within his hierarchical order.

    org.imixs.marty.plugins.SpacePlugin

The hierarchical order of a space is defined by the property "$uniqueidref" which is optional and pointing to a parent space entity. 
The plugin updates the following properties of a space entity:


 * txtparentname = name of the parent space in case the space entity is a supspace
 * namParentTeam = team-list of the parent space (if assigned)
 * namParentManager = manager-list of the parent space (if assigned)
 * namParentAssist = assist-list of the parent space (if assigned)
 