package org.jenkinsci.plugins.valgrind;

import hudson.model.Run;

import org.jenkinsci.plugins.valgrind.model.ValgrindError;
import org.jenkinsci.plugins.valgrind.model.ValgrindProcess;
import org.jenkinsci.plugins.valgrind.util.ValgrindSourceFile;

/**
 * 
 * @author Johannes Ohlemacher
 * 
 */
public class ValgrindErrorDetail
{
	private ValgrindError error;
	private ValgrindProcess process;
	final private Run<?, ?> owner;
	
	public ValgrindErrorDetail( Run<?, ?> owner, ValgrindProcess process, ValgrindError error, ValgrindSourceFile valgrindSourceFile )
	{
		this.owner = owner;
		this.error = error;	
		this.process = process;
		
		if ( error != null )
			error.setSourceCode( valgrindSourceFile );
	}

	public ValgrindError getError()
	{
		return error;
	}
	
	public ValgrindProcess getProcess()
	{
		return process;
	}

	public Run<?, ?> getOwner()
	{
		return owner;
	}
}
