package org.imixs.marty.util;

import java.util.List;

import javax.faces.component.UIParameter;
import javax.faces.event.ActionEvent;

/**
 * This Backing Bean acts as a OpenID Helper Class. 
 * This Bean is used to identify the OpenID Provider selected during the 
 * login process. This state is only for ui representation. It is not for authenticating user or processing the login throught the openID Provider
 * 
 *   @author rsoika
 * 
 */
public class OpenidMB {
	
	private String provider;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	
	/**
	 * This method selects a provider 
	 * The mehtod expects a parameter 'provider' with the name (String) of the selected
	 * Provider.
	 * 
	 * @param event
	 * @throws Exception
	 */
	public void doSelectProvider(ActionEvent event) throws Exception {
		//provider="verisign";
		
		List children = event.getComponent().getChildren();
		String aProvider="";

		for (int i = 0; i < children.size(); i++) {
			if (children.get(i) instanceof UIParameter) {
				UIParameter currentParam = (UIParameter) children.get(i);
				if (currentParam.getName().equals("provider")
						&& currentParam.getValue() != null) {
					aProvider =  currentParam.getValue().toString();
					provider= aProvider;
					break;
				}
			}
		}
		
	}
}
