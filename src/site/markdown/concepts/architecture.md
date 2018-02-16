# The Architecture

The Imixs-Marty project consist of different modules to provide a stable and flexible framework for building workflow applications. As the marty project uses the maven build and configuration framework, each of these modules is a separate maven artifact. This architecture makes it easy 
 to reuse these modules in different business applications.

### imixs-marty-util
The marty-util module contains CDI backing beans, facelets and util classes for building web front-ends  based on the JSF and Faces technology. This module give developers the ability to setup custom business application front-ends in a fast and easy way.

### imixs-marty-ejb
The marty-ejb module contains services classes and  workflow plugins extending the Imixs-Workflow engine. 


## The Components
The Imixs-Marty project provides different components to be used in front-end and back-end layers of a workflow system.


### Services

Services provided by the Imixs-Marty project extend the functionality of the Imixs-Workflow engine. The services are implemented either as  stateless EJBs or Interceptor classes. 

[Reed more](../services/index.html)

### Plug-Ins
In a Imixs-Worklfow application main part of the business logic is encapsulated in the BPMN model or implemented by custom plug-in classes. 
The Imixs-Marty project provides plugins to extend general workflow behavior. 
The plug-in concept enables a client to access these business logic through all the different interfaces from 
 the imixs workflow. For example a RESTfull service client has the same functionality like a JSF web client as far as the 
 business logic is encapsulated into plugins.
 See the [Imixs-Workflow Plugin API](http://www.imixs.org/doc/engine/plugins/index.html) for additional information.
 
[Reed more](../plugins/index.html)

### Controllers
The front-end or service facade is represented by CDI backing beans provided from the marty-util module. 
These backing beans can easily be integrated into any web front-end. CDI is fully supported by 
 these components. This provides a very flexible way to uses these components in a modern web  architecture. 

[Reed more](../controller/index.html)

### UI Interfaces

The Imixs-Marty UI module provides additional UI interfaces and web components to implement modern web front-ends. 
The UI interfaces are provided as custom facelets having a custom namespace. 
 The marty widgets are often based jquery. The JQuery libraries are included in the Imixs JSF Tools which can be added in the header on a jsf page.
 
[Reed more](../ui/index.html)