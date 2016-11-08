#The DMSPlugin

The DMSPlugin computes the read and write access
 for the blobworkitem attached to the process instance. This ensures that the access 
 to an attachment is synchronized to the paren process instances.
 
     org.imixs.marty.plugins.DMSPlugin
 
 The Plugin only runs in workItems type 'workitem' or 'workitemarchiv'.
 The Plugin should run immediate after the AccessPlugin.
 
##DMS Info
 Additional the DMSPlugin manages the propert 'dms'. 
 This property stores meta information for uploaded files. The Plugin provides static 
 methods to acces the meta information. The DMS Controler uses this methods to update 
 new uploads or user comments to attachments.
 
 
##Import functionality
 The DMS Plugin also provides a mechanism to import files from an external storage. 
 There for the Plugin checks if process instance contains the property 'txtDmsImport'.
 This property can contain  a list of files to be imported. 
 The Plugin supports different file formats. 
 If a import was processed successfully the property 'txtDMSImport' will be automatically removed.
 