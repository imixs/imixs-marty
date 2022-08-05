# The SpacePlugin

The SpacePlugin is a system plugin running only in the system workflow groups 'space' and 'process'.

The plugin computes and updates the attributes of a space with in a hierarchical order.

    org.imixs.marty.team.SpacePlugin

The hierarchical order of a space is defined by the property "$uniqueidref" which is optional and pointing to a parent space entity. 
The plugin updates the following properties of a space entity:


 * name = combined name of the parent space name and the own name separated by a '.'
 * space.parent.name = name of the parent space in case the space entity is a subspace
 

## Unique Name

A process or a space has a unique name attribute 'txtname'. If the name provided by the user is already taken the plugin throws a PluginException. 

## Archived Spaces

Spaces can optional be archived. Archived spaces can be still managed by the orgunit owner. 
 