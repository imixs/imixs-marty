# Imixs-Marty - User Interface

Imixs-Marty contains UI components and JSF fragments to be integrated into a application.
The marty widgets are often based jquery. The JQuery libraries are included in the Imixs JSF Tools which can be added in the header on a jsf page.  

## Installation

Before you can add the marty custom component in one of your JSF pages, make sure that you link the   imixs and the marty namespaces:

	  <f:subview xmlns="http://www.w3.org/1999/xhtml"
	  .....
	  xmlns:i="http://java.sun.com/jsf/composite/imixs"
	  xmlns:marty="http://java.sun.com/jsf/composite/marty" 
	  .... >

Also the marty javascript library and marty css definition need to be loaded in the header of 
your jsp page. See the following example which includes the Imixs Header and the marty java script library:

	 <h:head>
	   <i:imixsHeader />
	   ....
	    <link href="#{facesContext.externalContext.requestContextPath}/marty/imixs-marty.css"
	       charset="UTF-8" type="text/css" rel="stylesheet" />
	   ...
	   <script type="text/javascript"
	     src="#{facesContext.externalContext.requestContextPath}/marty/imixs-marty.js"></script>
	  ....
	  </h:head>
 