package org.jenkinsci.plugins.valgrind.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jenkinsci.plugins.valgrind.util.ValgrindErrorList;

public class ValgrindReport implements Serializable
{
	private static final long serialVersionUID = -9036045639715893780L;

	@Deprecated
	private List<ValgrindError> errors;	
	
	@SuppressWarnings("unused")
	@Deprecated
	private Set<String> executables;	
	
	private List<ValgrindProcess> processes;
	private Map<String, String> parserErrors;
	
	public boolean isValid()
	{
		if(processes == null || processes.isEmpty())
			return false;
		
		for(ValgrindProcess p : processes)
		{
			if(!p.isValid())
				return false;				
		}
		
		return true;
	}
	
	public void addProcess( ValgrindProcess process )
	{
		if ( processes == null )
			processes = new ArrayList<ValgrindProcess>();
		
		processes.add( process );
	}
	
	public void addParserError(String filename, String errorMessage)
	{
		if(parserErrors == null)
			parserErrors = new HashMap<String, String>();
		
		parserErrors.put(filename,  errorMessage);
	}
	
	public boolean hasParserErrors()
	{
		return (parserErrors != null) && !parserErrors.isEmpty();
	}
	
	public Map<String, String> getParserErrors()
	{
		return parserErrors;
	}
	
	public List<ValgrindError> getAllErrors()
	{
		List<ValgrindError> list = new ArrayList<ValgrindError>();
		
		if ( processes != null )
		{
			for( ValgrindProcess p : processes )
			{
				List<ValgrindError> l = p.getErrors();
				if ( l != null )
					list.addAll( l );
			}
		}
		
		if ( errors != null )
			list.addAll(errors);		
	
		if ( list.isEmpty() )
			return null;
		
		return list;		
	}
	
	public void integrate( ValgrindReport valgrindReport )
	{
		if ( valgrindReport == null || valgrindReport.processes == null )
			return;
		
		if ( processes == null )
			processes = new ArrayList<ValgrindProcess>();
		
		processes.addAll(valgrindReport.processes);
	}	
	
	@SuppressWarnings("deprecation")
	public ValgrindError findError( String pid, String uniqueId )
	{
		//for compatibility with older records, search for error with executable == pid
		if ( errors != null )
		{
			for ( ValgrindError error : errors )
				if ( error.getUniqueId().equals( uniqueId ) && error.getExecutable().equals( pid ) )
					return error;
		}
		
		ValgrindProcess process = findProcess(pid);
		
		return process.findErrorByUniqueId( uniqueId );
	}
	
	@SuppressWarnings("deprecation")
	public ValgrindProcess findProcess(String pid)
	{
		if ( processes != null )
		{
			for ( ValgrindProcess process : processes )
			{
				if ( process.getPid().equals(pid) )
				{
					process.setupParentChilds( processes );
					return process;
				}
			}
		}		
		
		ValgrindProcess process = new ValgrindProcess();
		process.setExecutable(pid);
		process.setPid(pid);
		
		if ( errors != null )	
		{
			for ( ValgrindError error : errors )
			{
				if ( error.getExecutable().equals(pid) )
					process.addError(error);
			}
		}
		
		return process;
	}

	public ValgrindErrorList getErrorList()
	{
		return new ValgrindErrorList(getAllErrors());
	}
	
	@SuppressWarnings("deprecation")
	public List<ValgrindProcess> getProcesses()
	{
		List<ValgrindProcess> result = new ArrayList<ValgrindProcess>();
		
		if ( processes != null )
		{
			for(ValgrindProcess p : processes)
			{
				if(p.isValid())
					result.add(p);
			}
		}
		
		if ( errors != null )
		{
			Map<String, ValgrindProcess> lookup = new HashMap<String, ValgrindProcess>();
			for( ValgrindError error : errors )
			{
				if ( !lookup.containsKey(error.getExecutable()) )
				{
					ValgrindProcess process = new ValgrindProcess();
					process.setExecutable(error.getExecutable());
					process.setPid(error.getExecutable());
					
					lookup.put(error.getExecutable(), process);
				}

				lookup.get(error.getExecutable()).addError(error);
			}	
			
			for(ValgrindProcess p : lookup.values())
			{
				if(p.isValid())
					result.add(p);
			}
		}		
		
		if ( result.isEmpty() )
			return null;
		
		for( ValgrindProcess p : result )
			p.setupParentChilds( processes );
		
		return result;
	}

	public ValgrindThread findThread(String pid, String hthreadid)
	{
		ValgrindProcess process = findProcess(pid);
		
		return process.findThreadByHthreadid(hthreadid);
	}	
}
