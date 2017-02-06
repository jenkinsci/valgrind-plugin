package org.jenkinsci.plugins.valgrind;

import hudson.model.Run;

import org.jenkinsci.plugins.valgrind.model.ValgrindProcess;

public class ValgrindProcessDetails
{
	private ValgrindProcess process;
	final private Run<?, ?> owner;
	
	public ValgrindProcessDetails( Run<?, ?> owner, ValgrindProcess process )
	{
		this.owner = owner;
		this.process = process;

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
