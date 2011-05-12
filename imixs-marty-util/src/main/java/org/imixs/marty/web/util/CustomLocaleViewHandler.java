package org.imixs.sywapps.web.util;

import java.io.IOException;
import java.util.Locale;

import javax.faces.FacesException;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import org.imixs.sywapps.web.profile.MyProfileMB;


public class CustomLocaleViewHandler extends ViewHandler {
	private final ViewHandler baseViewHandler;
	private MyProfileMB myProfileBean = null;

	public CustomLocaleViewHandler(ViewHandler base) {
		baseViewHandler = base;

	}

	public Locale calculateLocale(FacesContext facesContext) {
		Locale userLocale = null;

		// get the locale from userProfile
		/*
		if (myProfileBean == null) {
			myProfileBean = (MyProfileMB) FacesContext.getCurrentInstance()
					.getApplication().getELResolver().getValue(
							FacesContext.getCurrentInstance().getELContext(),
							null, "myProfileMB");
		}
		userLocale=myProfileBean.getLocale();
		*/
		userLocale=Locale.ENGLISH;
		
		if (userLocale != null)
			return userLocale;
		else
			return baseViewHandler.calculateLocale(facesContext);
	}

	@Override
	public String calculateRenderKitId(FacesContext arg0) {
		return baseViewHandler.calculateRenderKitId(arg0);
	}

	@Override
	public UIViewRoot createView(FacesContext arg0, String arg1) {
		return baseViewHandler.createView(arg0, arg1);
	}

	@Override
	public String getActionURL(FacesContext arg0, String arg1) {
		return baseViewHandler.getActionURL(arg0, arg1);
	}

	@Override
	public String getResourceURL(FacesContext arg0, String arg1) {
		return baseViewHandler.getResourceURL(arg0, arg1);
	}

	@Override
	public void renderView(FacesContext arg0, UIViewRoot arg1)
			throws IOException, FacesException {
		baseViewHandler.renderView(arg0, arg1);

	}

	@Override
	public UIViewRoot restoreView(FacesContext arg0, String arg1) {
		return baseViewHandler.restoreView(arg0, arg1);
	}

	@Override
	public void writeState(FacesContext arg0) throws IOException {
		baseViewHandler.writeState(arg0);

	}

}