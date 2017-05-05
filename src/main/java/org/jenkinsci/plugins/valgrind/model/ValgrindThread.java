package org.jenkinsci.plugins.valgrind.model;

import java.io.Serializable;

import org.jenkinsci.plugins.valgrind.util.ValgrindSourceFile;

public class ValgrindThread implements Serializable
{
	private static final long serialVersionUID = 6470943829358084901L;
	
	private String					hthreadid;
	private ValgrindStacktrace		stacktrace;
	private boolean					rootThread;

	public String toString()
	{
                if (hthreadid != null && stacktrace != null)
                {
                        return "id: " + hthreadid + "\n" +
                                "root_thread: " + rootThread + "\n" +
                                "stack: " + stacktrace.toString();
                }
                else
                {
                        return "";
                }
	}	
	
	public void setSourceCode( ValgrindSourceFile sourceFile )
	{
		if ( stacktrace != null )
			stacktrace.setSourceCode( sourceFile );
	}
	
	public ValgrindStacktrace getStacktrace()
	{
		return stacktrace;
	}
	
	public void setStacktrace(ValgrindStacktrace stacktrace)
	{
		this.stacktrace = stacktrace;
	}
	
	public String getDescription()
	{
		if (rootThread) {
			return "Root Thread (Helgrind ID " + hthreadid + ")";
		} else {
			return "Spawned Thread (Helgrind ID " + hthreadid + ")";
		}
	}
	
	public void setHthreadid(String hthreadid)
	{
		this.hthreadid = hthreadid;
	}
	
	public String getHthreadid()
	{
		return hthreadid;
	}

	public boolean isRootThread()
	{
		return rootThread;
	}
	
	public void setRootThread(boolean rootThread)
	{
		this.rootThread = rootThread;
	}

}
