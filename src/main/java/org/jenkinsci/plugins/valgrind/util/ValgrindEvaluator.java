package org.jenkinsci.plugins.valgrind.util;

import hudson.EnvVars;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.Result;
import hudson.model.Run;

import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;


public class ValgrindEvaluator
{
	private ValgrindPublisherConfig config;
	private TaskListener listener;
	
	public ValgrindEvaluator( ValgrindPublisherConfig config, TaskListener listener )
	{
		this.config = config;
		this.listener = listener;
	}
	
	public void evaluate( ValgrindReport report, Run<?, ?> build, EnvVars env )
	{		
		build.setResult( evaluate(
				report.getErrorList().getLeakDefinitelyLostErrorCount(), 
				env.expand( config.getUnstableThresholdDefinitelyLost() ), 
				env.expand( config.getFailThresholdDefinitelyLost() ) ) );

		build.setResult( evaluate(
				report.getErrorList().getInvalidReadErrorCount() + report.getErrorList().getInvalidWriteErrorCount(), 
				env.expand( config.getUnstableThresholdInvalidReadWrite() ), 
				env.expand( config.getFailThresholdInvalidReadWrite() ) ) );
		
		build.setResult( evaluate(
				report.getErrorList().getErrorCount(), 
				env.expand( config.getUnstableThresholdTotal() ), 
				env.expand( config.getFailThresholdTotal() ) ) );
		
		if(report.hasParserErrors() && config.isFailBuildOnInvalidReports())
		{
			build.setResult( Result.FAILURE );
		}
	}	
	
	private boolean exceedsThreshold( int errorCount, String threshold )
	{
		if ( threshold == null || threshold.isEmpty() )
			return false;
		
		try
		{
			Integer i = Integer.valueOf(threshold);	
			return errorCount > i.intValue();
		}
		catch( NumberFormatException e )
		{
			ValgrindLogger.log( listener, "ERROR: '" + threshold + "' is not a valid threshold" );
		}
		
		return false;
	}
	
	private Result evaluate( int errorCount, String unstableThreshold, String failThreshold )
	{
		if ( exceedsThreshold( errorCount, failThreshold ) )
			return Result.FAILURE;
		
		if ( exceedsThreshold( errorCount, unstableThreshold ) )
			return Result.UNSTABLE;
		
		return Result.SUCCESS;
	}
	
}
