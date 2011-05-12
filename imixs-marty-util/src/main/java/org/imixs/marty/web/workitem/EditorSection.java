package org.imixs.marty.web.workitem;

/**
 * This Class is provided as a property of the WorkitemMB to provide informations about EditorSections 
 * defined in the Model (txtWorkflowEditorID) 
 * 
 * <code>
 *     <c:forEach items="#{workitemMB.editorSections}" var="section">
 *         <ui:include src="/pages/workitems/forms/#{section.url}.xhtml" />
 *         .....
 *         
 *         
 *  other Example:   
 *     
 *      rendered="#{! empty workitemMB.editorSection['prototyp/files']}"
 *      
 *      
 * </code>
 * 
 * @see WorkitemMB.getEditorSections
 * @see WorkitemMB.getEditorSection
 * 
 * @author rsoika
 *
 */
public class EditorSection {
	String url;
	String name;

	public EditorSection(String url, String name) {
		super();
		this.url = url;
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
