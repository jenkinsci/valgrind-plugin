package org.jenkinsci.plugins.valgrind.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.valgrind.util.ValgrindErrorList;


public class ValgrindProcess implements Serializable
{
	private static final long	serialVersionUID	= -7073482135992069077L;
	
	private String tool;
	private String executable;
	private List<String> arguments;
	private List<String> valgrind_arguments;
	private String pid;
	private String ppid;
	private List<ValgrindError> errors;
	private List<ValgrindThread> threads;
	
	private transient ValgrindProcess parent;
	private transient List<ValgrindProcess> childs;
	
	public boolean isValid()
	{
		if(executable == null)
			return false;
		
		return true;
	}
	
	public ValgrindProcess()
	{
	}
	
	public ValgrindProcess getParent()
	{
		return parent;
	}
	
	public List<ValgrindProcess> getChilds()
	{
		return childs;
	}
	
	public String getExecutable()
	{
		return executable;
	}
	
	public void setExecutable(String executable)
	{
		this.executable = executable;
	}
	
	public List<String> getArguments()
	{
		return arguments;
	}
	
	public List<String> getValgrindArguments()
	{
		return valgrind_arguments;
	}
	
	public void setArguments(List<String> arguments)
	{
		this.arguments = arguments;
	}
	
	public void addArgument(String arg)
	{
		if ( arguments == null )
			arguments = new ArrayList<String>();
		
		arguments.add(arg);
	}
	
	public void addValgrindArgument(String arg)
	{
		if ( valgrind_arguments == null )
			valgrind_arguments = new ArrayList<String>();
		
		valgrind_arguments.add(arg);
		
		// actually, we only really care about the tool argument
		if ( arg.startsWith("--tool=") ) {
			tool = arg.substring(7);
		}
	}
		
	
	public String getPid()
	{
		return pid;
	}
	
	public void setPid(String pid)
	{
		this.pid = pid;
	}
	
	public String getPpid()
	{
		return ppid;
	}
	
	public void setPpid(String ppid)
	{
		this.ppid = ppid;
	}

	public List<ValgrindError> getErrors()
	{
		return errors;
	}

	public void setErrors(List<ValgrindError> errors)
	{
		this.errors = errors;
	}

	public void addError(ValgrindError error)
	{
		if ( errors == null )
			errors = new ArrayList<ValgrindError>();
		
		errors.add(error);
	}
	
	public ValgrindError findErrorByUniqueId(String id)
	{
		if ( errors == null )
			return null;
		
		for( ValgrindError error : errors )
			if ( error.getUniqueId().equals(id) )
				return error;
		
		return null;
	}
	
	public ValgrindErrorList getErrorList()
	{
		return new ValgrindErrorList(errors);
	}
	
	public List<ValgrindThread> getThreads()
	{
		return threads;
	}
	
	public void setThreads(List<ValgrindThread> threads)
	{
		this.threads = threads;
	}
	
	public void addThread(ValgrindThread thread)
	{
		if ( threads == null )
			threads = new ArrayList<ValgrindThread>();
		
		threads.add(thread);
	}
	
	public ValgrindThread findThreadByHthreadid(String hthreadid)
	{
		if ( threads == null )
			return null;
		
		for( ValgrindThread thread : threads )
			if ( thread.getHthreadid().equals(hthreadid) )
				return thread;
		
		return null;
	}
	
	public void setupParentChilds( List<ValgrindProcess> processes )
	{
		if ( parent != null || childs != null || processes == null )
			return;
		
		for ( ValgrindProcess p : processes )
		{
			if ( parent == null && ppid != null && ppid.equals(p.pid) )
				parent = p;
			
			if ( pid != null && pid.equals(p.ppid) )
			{
				if ( childs == null )
					childs = new ArrayList<ValgrindProcess>();
				
				childs.add(p);
			}
		}
	}
	
	String concatArguments(List<String> args)
	{
		if ( args == null )
			return "";
		
		String s = "";
		for ( String a : args )
		{
			s += a + "<br>";
		}
		return s.trim();
	}
	
	public String getArgumentsString()
	{
		return concatArguments(arguments);
	}

	public String getValgrindArgumentsString()
	{
		return concatArguments(valgrind_arguments);
	}

	public String getTool()
	{
		if (tool == null) {
			return "unknown tool";
		} else {
			return tool;
		}
	}
}
