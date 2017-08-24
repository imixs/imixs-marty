# Configuration

The marty project provide a set of configuration beans to be used to setup an instance or  provide custom configuration properties.

## The Marty Config Service
The Martty Config Service can be used to store and access configuration values stored in a  configuration entity (type='CONFIGURATION). The ConfigService EJB provides access to named  Config Entities stored in the database. A configuration Entity is identified by its name  (property txtName). So different configuration Entities can be managed in one application. The ConfigService ejb is implemented as a sigelton and uses an internal cache to cache config 
 entities.


## The CDI Bean ConfigControler
The ConfigController acts as a frontend controller to manage a single configuration entity  with multiple config params. The property 'workitem' (itemCollection) holds the config params.  The property 'txtname' is used to select the config entity by a query. The bean interacts with 
 the marty ConfigService EJB which is responsible for creation, loading and saving the entity.  The ConfigController bean is ApplicationScoped, so it is shared in one application. From the  backend it is possible to use the ConfigControler or also directly the ConfigService EJB.

The Bean can be overwritten to add additional busines logic (e.g. converting params or providing  additional custom getter methods). Use multiple instances in one application, bean can be declared  in the faces-config.xml file.The managed-ban-name as the manged property 'name' can be set to 
 custom values:

	<managed-bean>
			<managed-bean-name>myConfigMB</managed-bean-name>
			<managed-bean-class>org.imixs.marty.web.util.ConfigMB</managed-bean-class>
			<managed-property>
				<property-name>name</property-name>
				<value>REPORT_CONFIGURATION</value>
			</managed-property>
	</managed-bean>

The Bean provides easy access to the config params from a JSF Page. Example:

	<h:inputText value="#{configController.workitem.item['myParam1']}" />


## The CDI Bean ConfigMultiController
The CDI Bean ConfigMulitController provides a way to manage different custom configuration  documents. The type of the configuration is defined by the property 'type'. The key of a  single configuration entity is stored in the property 'txtName'. The property $WriteAccess is set to 'org.imixs.ACCESSLEVEL.MANAGERACCESS'. An instance of this bean is defined via  faces-config.xml faces-config example:

	<managed-bean>
			<managed-bean-name>carcompanyMB</managed-bean-name>
			<managed-bean-class>org.imixs.marty.web.util.ConfigMultiMB</managed-bean-class>
			<managed-bean-scope>session</managed-bean-scope>
			<managed-property>
				<property-name>type</property-name>
				<value>carcompany</value>
			</managed-property>
	</managed-bean>
 

## The Marty SetupController

The Marty SetupController extends the Marty ConfigController and holds the data from the  configuration entity 'BASIC'. This is the general configuration entity for a maty workflow  instance. The actionListener 'doSetup' can be used to reset the propertyService and the cache  from the ModelController and ProcessController


## The Imixs PropertyService
The Imixs Workflow Engine provides a general property service to manage application specific  properties in a common way. The properties are stored in a file named 'imixs.properties'.  For a marty workflow instance the file is typically located in the EJB module (src/main/resources/imixs.properties). The singleton EJB 'PropertyService' provides a service   to access the imxis.property file.


### The PropertyInterceptor

The marty PropertyInterceptor provides a mechanism to provide database based properties for  the imixs workflow PropertyService. The PropertyInterceptor intercepts the method getProperties()  from the org.imixs.workflow.jee.util.PropertyService and checks if the configuration entity  'BASIC' exists. If the configuration entity provides the property field 'properties' the values  of this item will overwrite the current settings of the imixs.property configuration file. 
 The configuration entity 'BASIC' can be controlled by an application  (see Imixs-Office-Workflow /pages/admin/config.xhtml)
 
