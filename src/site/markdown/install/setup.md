# Initial Setup User DB

Imixs-Marty provides a SetupUserDBService to initialize the user db. The Service can be controlled by the 
imixs.property 'setup.mode':


| mode  		| Description                               						|
|---------------------------------------------------------------------------|
|auto (default)	| initializes the userDb default user and uploads the default models|
|none			| no action 														|

## Integration
The Setup Service is automatically started on deployment. You can also call the setup by the Rest API.



| URI                                           | Method| Description                                                           | 
|-----------------------------------------------|-------|----------------------------------------------------------------|
| /setup                                        | GET  | trigger the setup service |



### CDI Support 


You can adapt the setup by observing the CDI Event _SetupEvent_



## Import default Model

The default models are loaded by the [SetupService](https://www.imixs.org/doc/engine/setupservice.html) provided by the Imixs-Workflow Engine.
This service can also be used to load defauld data stored in a xml file.
See the   [Imixs-Workflow SetupService](https://www.imixs.org/doc/engine/setupservice.html) for details.

