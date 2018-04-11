package org.jenkinsci.plugins.valgrind.call;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;

public class ValgrindCall
{
	private ValgrindExecutable		valgrindExecutable;
	private EnvVars					env;
	private String					programName;
	private FilePath				workingDirectory;	

	private List<ValgrindOption>	valgrindOptions		 = new ArrayList<ValgrindOption>();
	private List<String>            customValgindOptions = new ArrayList<String>();
	private List<String>			programArguments	 = new ArrayList<String>();
	
	public ValgrindCall()
	{		
	}
	
	public void setValgrindExecutable( ValgrindExecutable valgrindExecutable )
	{
		this.valgrindExecutable = valgrindExecutable;
	}
	
	public void setEnv(EnvVars env)
	{
		this.env = env;
	}
	
	public void setProgramName(String programName)
	{
		this.programName = programName;		
		
		if ( this.programName != null && this.programName.isEmpty() )
			this.programName = null;
	}
	
	public void setWorkingDirectory(FilePath workingDirectory)
	{
		this.workingDirectory = workingDirectory;
	}
	
	public void addValgrindOption(ValgrindOption option)
	{
		if (option != null)
			valgrindOptions.add(option);
	}
	
	public void addCustomValgrindOptions(String... options)
	{
		for( String option : options )
			addCustomValgrindOption(option);
	}
	
	public void addCustomValgrindOption(String option)
	{
		if ( option == null )
			return;
		
		option = option.trim();
		
		if ( option.isEmpty() )
			return;
		
		customValgindOptions.add(option);
	}
	
	public void addProgramArguments(String... arguments)
	{
		for (String arg : arguments)
		{
			if ( arg != null )
				programArguments.add(arg);
		}
	}	

	public int exec(TaskListener listener, Launcher launcher, ByteArrayOutputStream stdout, ByteArrayOutputStream stderr) throws IOException, InterruptedException
	{
		if ( valgrindExecutable == null )
			throw new IllegalStateException("valgrind executable is null");
		
		if ( programName == null )
			throw new IllegalStateException("program name is null");
		
		if ( env == null )
			throw new IllegalStateException("environment is null");
		
		if ( workingDirectory == null )
			throw new IllegalStateException("working directory is null");		
		
		List<String> cmds = new ArrayList<String>();

		cmds.add(valgrindExecutable.getExecutable());

		for (ValgrindOption option : valgrindOptions)
		{
			try
			{
				cmds.add(env.expand(option.getArgumentString(valgrindExecutable.getVersion())));				
			}
			catch(ValgrindOptionNotApplicableException e)
			{
				ValgrindLogger.log(listener, "option '" + option.getName() + "' is not applicable: " + e.getMessage());				
			}			
		}
		
		for (String option : customValgindOptions)
			cmds.add(env.expand(option));

		if (!System.getProperty("os.name").startsWith("Windows")) {
			programName = "." + programName;
		}
		
		cmds.add(programName);

		for (String argument : programArguments)
			cmds.add(env.expand(argument));		
		
		ValgrindLogger.log(listener, "working dir: " + workingDirectory);

		Launcher.ProcStarter starter = launcher.launch();
		starter = starter.pwd(workingDirectory);
		starter = starter.stdout(stdout);
		starter = starter.stderr(stderr);
		starter = starter.cmds(cmds);
		starter = starter.envs(env);

		return starter.join();
	}

}
