# ProfileService

The org.imixs.marty.ej.ProfileService provides methods to lookup the user profile 
for a userID or Email.
The service uses an internal cache to minimize the lookup of userprofizels in the database.
The cache size is set to 100 profile entries per default. This size can be set by the 
imixs.properties:
 
	profileservice.cachesize=100
