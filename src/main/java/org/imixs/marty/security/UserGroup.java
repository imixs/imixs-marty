package org.imixs.marty.security;

import jakarta.persistence.Id;

/**
 * User object to provide a database for username/password/groups. The user
 * object is managed by the UserDBPlugin.
 * 
 * @author rsoika
 * 
 */
@jakarta.persistence.Entity
public class UserGroup implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private String id;
	
	
	/**
	 * default constructor for JPA
	 */
	public UserGroup() {
		super();
	}

	/**
	 * A User will be initialized with an id
	 */
	public UserGroup(String aid) {
		this.id = aid;
	}

	/**
	 * returns the unique identifier for the Entity.
	 * 
	 * @return universal id
	 */
	@Id
	public String getId() {
		return id;
	}

	protected void setId(String aID) {
		id = aID;
	}

	
}
