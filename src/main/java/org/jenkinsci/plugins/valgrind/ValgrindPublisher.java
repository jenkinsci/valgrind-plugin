package org.jenkinsci.plugins.valgrind;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.FilePath;

import java.io.IOException;
import java.util.Map.Entry;

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
import org.jenkinsci.plugins.valgrind.util.ValgrindSourceResolver;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import jenkins.tasks.SimpleBuildStep;
import javax.annotation.Nonnull;


/**
 *
 * @author Johannes Ohlemacher
 *
 */
public class ValgrindPublisher extends Recorder implements SimpleBuildStep
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
			String sourceSubstitutionPaths,
			boolean publishResultsForAbortedBuilds,
			boolean publishResultsForFailedBuilds,
			boolean failBuildOnMissingReports,
			boolean failBuildOnInvalidReports)
	{
		valgrindPublisherConfig = new ValgrindPublisherConfig(
				pattern,
				failThresholdInvalidReadWrite,
				failThresholdDefinitelyLost,
				failThresholdTotal,
				unstableThresholdInvalidReadWrite,
				unstableThresholdDefinitelyLost,
				unstableThresholdTotal,
				sourceSubstitutionPaths,
				publishResultsForAbortedBuilds,
				publishResultsForFailedBuilds,
				failBuildOnMissingReports,
				failBuildOnInvalidReports);
	}

	@Override
	public ValgrindPublisherDescriptor getDescriptor()
	{
		return DESCRIPTOR;
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService()
	{
		return BuildStepMonitor.NONE;
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
	public void perform(@Nonnull Run<?, ?> build, @Nonnull FilePath workspace, @Nonnull Launcher launcher, @Nonnull TaskListener listener)
			throws InterruptedException, IOException
	{
		if (!canContinue(build.getResult()))
			return;

		if ( valgrindPublisherConfig.getPattern() == null || valgrindPublisherConfig.getPattern().isEmpty() )
		{
			ValgrindLogger.log(listener, "ERROR: no pattern for valgrind xml files configured");
			return;
		}

		EnvVars env = build.getEnvironment(listener);

		FilePath baseFileFrom = workspace;
		FilePath baseFileTo =  new FilePath(build.getRootDir());
		ValgrindResultsScanner scanner = new ValgrindResultsScanner(valgrindPublisherConfig.getPattern());
		String[] files = baseFileFrom.act(scanner);

		if(files.length == 0 && valgrindPublisherConfig.isFailBuildOnMissingReports())
		{
			ValgrindLogger.log( listener, "ERROR: no report files found for pattern '" + valgrindPublisherConfig.getPattern() + "'" );
			build.setResult( Result.FAILURE );
		}
		else
		{
			ValgrindLogger.log(listener, "Files to copy:");
			for (int i = 0; i < files.length; i++)
			{
				ValgrindLogger.log(listener, files[i]);
			}

			for (int i = 0; i < files.length; i++)
			{
				FilePath fileFrom = new FilePath(baseFileFrom, files[i]);
				FilePath fileTo = new FilePath(baseFileTo, "valgrind-plugin/valgrind-results/" + files[i]);
				ValgrindLogger.log(listener, "Copying " + files[i] + " to " + fileTo.getRemote());
				fileFrom.copyTo(fileTo);
			}

			ValgrindParserResult parser = new ValgrindParserResult("valgrind-plugin/valgrind-results/"+valgrindPublisherConfig.getPattern());

			ValgrindResult valgrindResult = new ValgrindResult(build, parser);
			ValgrindReport valgrindReport = valgrindResult.getReport();
			logParserError(listener, valgrindReport);

			new ValgrindEvaluator(valgrindPublisherConfig, listener).evaluate(valgrindReport, build, env);

			ValgrindLogger.log(listener, "Analysing valgrind results");

			String resolvedSubstitutionPaths = valgrindPublisherConfig.getSourceSubstitutionPaths();
			if (env!=null){
				resolvedSubstitutionPaths = env.expand(resolvedSubstitutionPaths);
			}

			ValgrindSourceResolver sourceResolver = new ValgrindSourceResolver(resolvedSubstitutionPaths);
			ValgrindSourceGrabber sourceGrabber = new ValgrindSourceGrabber(listener,  workspace, sourceResolver);

			if ( !sourceGrabber.init( build.getRootDir() ) )
				return;

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
				String workspacePath = workspace.getRemote() + "/";
				ValgrindLogger.log(listener, "workspacePath: " + workspacePath);


				for ( ValgrindProcess p : valgrindReport.getProcesses() )
				{
					if(!p.isValid())
						continue;

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
		}

		return;

	}

	public ValgrindPublisherConfig getValgrindPublisherConfig()
	{
		return valgrindPublisherConfig;
	}

	public void setValgrindPublisherConfig(ValgrindPublisherConfig valgrindPublisherConfig)
	{
		this.valgrindPublisherConfig = valgrindPublisherConfig;
	}

	private void logParserError(TaskListener listener, ValgrindReport report)
	{
		if(report == null || report.getParserErrors() == null)
			return;

		for (Entry<String, String> entry : report.getParserErrors().entrySet())
		{
			ValgrindLogger.log(listener, "ERROR: failed to parse " + entry.getKey() + ": " + entry.getValue());
		}
	}

	public String getPattern()
	{
		return valgrindPublisherConfig.getPattern();
	}

	public String getFailThresholdInvalidReadWrite()
	{
		return valgrindPublisherConfig.getFailThresholdInvalidReadWrite();
	}

	public String getFailThresholdDefinitelyLost()
	{
		return valgrindPublisherConfig.getFailThresholdDefinitelyLost();
	}

	public String getFailThresholdTotal()
	{
		return valgrindPublisherConfig.getFailThresholdTotal();
	}

	public String getUnstableThresholdInvalidReadWrite()
	{
		return valgrindPublisherConfig.getUnstableThresholdInvalidReadWrite();
	}

	public String getUnstableThresholdDefinitelyLost()
	{
		return valgrindPublisherConfig.getUnstableThresholdDefinitelyLost();
	}

	public String getUnstableThresholdTotal()
	{
		return valgrindPublisherConfig.getUnstableThresholdTotal();
	}

	public String getSourceSubstitutionPaths()
	{
		return valgrindPublisherConfig.getSourceSubstitutionPaths();
	}

	public boolean isPublishResultsForAbortedBuilds()
	{
		return valgrindPublisherConfig.isPublishResultsForAbortedBuilds();
	}

	public boolean isPublishResultsForFailedBuilds()
	{
		return valgrindPublisherConfig.isPublishResultsForFailedBuilds();
	}

	public boolean isFailBuildOnMissingReports()
	{
		return valgrindPublisherConfig.isFailBuildOnMissingReports();
	}

	public boolean isFailBuildOnInvalidReports()
	{
		return valgrindPublisherConfig.isFailBuildOnInvalidReports();
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
			return true;
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
