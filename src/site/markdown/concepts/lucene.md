# Lucene Search

Imixs-office-Workflow uses the imixs lucene project for fulltext search.
See details about the Imixs Lucene Adapter project [here](https://github.com/imixs/imixs-adapters). 

## Default settings

The Imixs Lucene Adapter project provides two EJBs 

 * LuceneSearchService - for fulltext search
 * LuceneUpdateService - to update/write the lucene index
 
The lucene Service EJBs are using the org.apache.lucene.analysis.standard.ClassicAnalyzer per default.
To escape search terms the LuceneSearchService prvides the method escapeSearchTerm(). All special characters, except the '*' will be escaped by a backslash.

## Configuration

The following parameters can be provided in the imixs.properties file:

 * lucence.indexDir - location of the lucen index files
 * lucence.fulltextFieldList - field list to be added into the 'content' of a indexed workitem
 * lucence.indexFieldListAnalyze - additional optional fields to be added into the document (can be used for field search)
 * lucence.indexFieldListNoAnalyze - additional optional fields to be added into the document (can be used for field search). Content will not be analyzed (tokenized).
 * lucence.matchingType - regexpression with workitem types to be indexed
 * lucence.matchingProcessID - regexpression with process ids to be indexed
 

The default settings are:

	# Search Index Directory 
	lucence.indexDir=${imixs-office.IndexDir}
	
	# Fields to be added into the searchindex
	lucence.fulltextFieldList=txtsearchstring,txtSubject,txtname,txtEmail,txtUserName,namCreator,txtWorkflowAbstract,txtWorkflowSummary,_subject,_description,_name,_projectnumber,_projectname,_ordernumber,datDueDate,txtcommentlog,htmldescription,htmldocumentation
	lucence.indexFieldListAnalyze=
	lucence.indexFieldListNoAnalyze=type,$UniqueIDRef,$created,$modified,$ModelVersion,namCreator,$ProcessID,datDate,txtWorkflowGroup,_supplier
	# Matching Patterns to index a workitem
	lucence.matchingType=workitem|workitemarchive|profile
	lucence.matchingProcessID= 