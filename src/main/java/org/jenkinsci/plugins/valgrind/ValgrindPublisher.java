package org.jenkinsci.plugins.valgrind;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixProject;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;
import org.jenkinsci.plugins.valgrind.model.ValgrindAuxiliary;
import org.jenkinsci.plugins.valgrind.model.ValgrindError;
import org.jenkinsci.plugins.valgrind.model.ValgrindProcess;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.parser.ValgrindParserResult;
import org.jenkinsci.plugins.valgrind.util.ValgrindEvaluator;
import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;
import org.jenkinsci.plugins.valgrind.util.ValgrindSourceGrabber;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * @author Johannes Ohlemacher
 * 
 */
public class ValgrindPublisher extends Recorder
{
	private ValgrindPublisherConfig valgrindPublisherConfig;

	@DataBoundConstructor
	public ValgrindPublisher( String pattern, 
			String failThresholdInvalidReadWrite, 
			String failThresholdDefinitelyLost, 
			String failThresholdTotal,
			String unstableThresholdInvalidReadWrite, 
			String unstableThresholdDefinitelyLost, 
			String unstableThresholdTotal,
			boolean publishResultsForAbortedBuilds,
			boolean publishResultsForFailedBuilds)
	{
		valgrindPublisherConfig = new ValgrindPublisherConfig(
				pattern, 
				failThresholdInvalidReadWrite, 
				failThresholdDefinitelyLost, 
				failThresholdTotal,
				unstableThresholdInvalidReadWrite, 
				unstableThresholdDefinitelyLost, 
				unstableThresholdTotal,
				publishResultsForAbortedBuilds,
				publishResultsForFailedBuilds );		
	}	

	@Override
	public ValgrindPublisherDescriptor getDescriptor()
	{
		return DESCRIPTOR;
	}

	@Override
	public Action getProjectAction(AbstractProject<?, ?> project)
	{
		return new ValgrindProjectAction(project);
	}

	public BuildStepMonitor getRequiredMonitorService()
	{
		return BuildStepMonitor.BUILD;
	}

	protected boolean canContinue(final Result result)
	{
		if ( result == Result.ABORTED && !valgrindPublisherConfig.isPublishResultsForAbortedBuilds() )
			return false;

		if ( result == Result.FAILURE && !valgrindPublisherConfig.isPublishResultsForFailedBuilds() )
			return false;
		
		return true;
	}

	@Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
			throws InterruptedException, IOException
	{
		if (!canContinue(build.getResult()))
			return true;
		
		if ( valgrindPublisherConfig.getPattern() == null || valgrindPublisherConfig.getPattern().isEmpty() )
		{
			ValgrindLogger.log(listener, "ERROR: no pattern for valgrind xml files configured");
			return false;
		}
		
		EnvVars env = build.getEnvironment();

		ValgrindLogger.log(listener, "Analysing valgrind results; configure Jenkins system log (ValgrindLogger) for details");

		ValgrindParserResult parser = new ValgrindParserResult(env.expand(valgrindPublisherConfig.getPattern()));

		ValgrindResult valgrindResult = new ValgrindResult(build, parser);
		ValgrindReport valgrindReport = valgrindResult.getReport();
		
		new ValgrindEvaluator(valgrindPublisherConfig, listener).evaluate(valgrindReport, build, env); 
		
		ValgrindLogger.log(listener, "Analysing valgrind results");	
		
		ValgrindSourceGrabber sourceGrabber = new ValgrindSourceGrabber(listener,  build.getModuleRoot());
		
		if ( !sourceGrabber.init( build.getRootDir() ) )
			return false;
		
		if ( valgrindReport.getAllErrors() != null )
		{
			for ( ValgrindError error : valgrindReport.getAllErrors() )
			{
				if ( error.getStacktrace() != null )
					sourceGrabber.grabFromStacktrace( error.getStacktrace() );
				
				if ( error.getAuxiliaryData() != null )
				{
					for ( ValgrindAuxiliary aux : error.getAuxiliaryData() )
					{
						if ( aux.getStacktrace() != null )
							sourceGrabber.grabFromStacktrace(aux.getStacktrace());
					}				
				}
			}
		}
		
		//remove workspace path from executable name
		if ( valgrindReport.getProcesses() != null )
		{
			String workspacePath = build.getWorkspace().getRemote() + "/";
			ValgrindLogger.log(listener, "workspacePath: " + workspacePath);
			
			for ( ValgrindProcess p : valgrindReport.getProcesses() )
			{
				if ( p.getExecutable().startsWith(workspacePath) )
					p.setExecutable( p.getExecutable().substring(workspacePath.length()));
				
				if ( p.getExecutable().startsWith("./") )
					p.setExecutable( p.getExecutable().substring(2) );
			}
		}
		
		valgrindResult.setSourceFiles(sourceGrabber.getLookupMap());

		ValgrindBuildAction buildAction = new ValgrindBuildAction(build, valgrindResult,
				valgrindPublisherConfig);
		build.addAction(buildAction);

		ValgrindLogger.log(listener, "Ending the valgrind analysis.");

		return true;
	}
	
	public ValgrindPublisherConfig getValgrindPublisherConfig()
	{
		return valgrindPublisherConfig;
	}

	public void setValgrindPublisherConfig(ValgrindPublisherConfig valgrindPublisherConfig)
	{
		this.valgrindPublisherConfig = valgrindPublisherConfig;
	}	
	
	@Extension
	public static final ValgrindPublisherDescriptor DESCRIPTOR = new ValgrindPublisherDescriptor();

	public static final class ValgrindPublisherDescriptor extends BuildStepDescriptor<Publisher>
	{
		private int	linesBefore	= 10;
		private int	linesAfter	= 5;
		
		public ValgrindPublisherDescriptor()
		{
			super(ValgrindPublisher.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{
			linesBefore = formData.getInt("linesBefore");
			linesAfter = formData.getInt("linesAfter");
			save();
			return super.configure(req, formData);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType)
		{
			return FreeStyleProject.class.isAssignableFrom(jobType)
					|| MatrixProject.class.isAssignableFrom(jobType);
		}

		@Override
		public String getDisplayName()
		{
			return "Publish Valgrind results";
		}

		public int getLinesBefore()
		{
			return linesBefore;
		}

		public int getLinesAfter()
		{
			return linesAfter;
		}
		
		public ValgrindPublisherConfig getConfig()
		{
			return new ValgrindPublisherConfig();
		}      
	}
}
