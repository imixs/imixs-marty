package org.imixs.marty.plugins;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.imixs.workflow.exceptions.PluginException;
import org.junit.Test;

/**
 * Test regex from the ProfilePugin
 * 
 * @author rsoika
 */
public class TestRegex {
	Pattern pattern;
	Matcher matcher;

	
	@Test
	public void testEmail() throws PluginException {
		pattern = Pattern.compile(ProfilePlugin.EMAIL_PATTERN);
		matcher = pattern.matcher("hl+o@imixs.com");
		Assert.assertTrue( matcher.matches());
	
	}
	
	
	
	
	@Test
	public void testUserid() throws PluginException {
		pattern = Pattern.compile(ProfilePlugin.USERID_PATTERN);
		matcher = pattern.matcher("pin_gi-");
		Assert.assertTrue( matcher.matches());

		
		pattern = Pattern.compile(ProfilePlugin.USERID_PATTERN);
		matcher = pattern.matcher("pingiimixs.com");
		Assert.assertTrue( matcher.matches());
		
		pattern = Pattern.compile(ProfilePlugin.USERID_PATTERN);
		matcher = pattern.matcher("www.imixs.com");
		Assert.assertTrue( matcher.matches());
	
		
		pattern = Pattern.compile(ProfilePlugin.USERID_PATTERN);
		matcher = pattern.matcher("pingi@imixs.com");
		Assert.assertTrue( matcher.matches());
	}

}
