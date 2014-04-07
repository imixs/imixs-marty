package org.imixs.marty.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Test;

/**
 * Test regex
 * 
 * @author rsoika
 */
public class TestRegex {

	@Test
	public void test() throws PluginException {

		
		
		Pattern pattern;
		Matcher matcher;

		String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
				+ "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";


		
		String USERID_PATTERN = "^[A-Za-z0-9\\-\\w]+";

		
		pattern = Pattern.compile(EMAIL_PATTERN);
		matcher = pattern.matcher("hl+o@imixs.com");
		Assert.assertTrue( matcher.matches());


	
	
		pattern = Pattern.compile(USERID_PATTERN);
		matcher = pattern.matcher("pin_gi-");
		Assert.assertTrue( matcher.matches());

	
	}

}
