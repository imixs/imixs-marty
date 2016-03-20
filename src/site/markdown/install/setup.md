#Initial Setup

Imixs-Marty provides a SetupServlet which is called during deployment and also can be triggered via the URI

    http://localhost:8080/office/setup

The SetupServlet verifies the status of a marty instance and inits default values. It creates a default user 'admin' and initalizes the entity indicies. The auto-setup during deplyoment can be disabled by the imixs.property key

    setup.mode=none

##Import default Data

During the deployment of a marty application it is possible to load default model data  into the database. This feature simplifies the installation of a Marty Workflow Instance  because no manually deploy of a system model or a business model before the first access  is necessary.
If no System Model is still available  (System Models start with the version numer 'system-") the servlet loads all entity data files defined by the imixs property key 

    setup.defaultModel

This property can define a single file or a list of files to be imported.

Example:

    # Setup default model - a list of model files to be imported as default configuation
    setup.mode=auto
    setup.defaultModel=configuration/system-de-0.0.1.ixm,configuration/system-en-0.0.1.ixm,configuration/office-de-0.0.2.ixm,configuration/projects.xml

The default entity data set should be stored in the file

    setup_data.xml

The file is typical placed in the folder /src/main/resources of the EJB module.



###How to generate a new initial setup data file

To generate a XML-File with the default entity data set the Rest Service API and 
ReportService API can be used. This is an example for a export of all model entries, 
and process data.

    SELECT wi FROM Entity AS wi
    WHERE wi.type IN ('ProcessEntity','ActivityEntity','WorkflowEnvironmentEntity','process','configuration')
 

A report file with this definition is located in the marty ejb module 
 
    /src/test/resources/reports/setup_data.xml

The data can be exported with the following URL

    http://localhost:8080/office-rest/report/setup_data.xml?count=-1

Make sure that the model data and project configuration is uptodate before you start a  new export. The following example specify the model version is a param '1'

    SELECT wi FROM Entity AS wi
    JOIN wi.textItems AS v
    WHERE wi.type IN ('ProcessEntity','ActivityEntity','WorkflowEnvironmentEntity')
    AND v.itemName = '$modelversion' AND v.itemValue = '?1'
 
 
# Autostart WorkflowScheduler
During first deployment the SetupServlet verifies if a WorkflowScheduler was defined once before. In this case the TimerSerivce will autoatically restarted by the SetupServlet
 