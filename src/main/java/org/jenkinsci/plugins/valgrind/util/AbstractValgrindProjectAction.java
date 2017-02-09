package org.jenkinsci.plugins.valgrind.util;

import java.io.IOException;

import org.jenkinsci.plugins.valgrind.ValgrindBuildAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;
import hudson.model.Job;
import hudson.model.Run;

public abstract class AbstractValgrindProjectAction extends Actionable implements ProminentProjectAction
{
	protected final Job<?, ?> job;
	
	protected AbstractValgrindProjectAction(Job<?, ?> job)
	{
		this.job = job;
	}
	
	public Job<?, ?> getJob()
	{
		return job;
	}

	public String getIconFileName()
	{
		return "/plugin/valgrind/icons/valgrind-48.png";
	}

	public String getSearchUrl()
	{
		return getUrlName();
	}

    protected abstract Run<?, ?> getLastFinishedBuild();

    protected abstract Integer getLastResultBuild();

	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException
	{
		Run<?, ?> lastBuild = getLastFinishedBuild();
		ValgrindBuildAction valgrindBuildAction = lastBuild.getAction(ValgrindBuildAction.class);
		if (valgrindBuildAction != null)
		{
			valgrindBuildAction.doGraph(req, rsp);
		}
	}

	public void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException
	{
		Integer buildNumber = getLastResultBuild();
		if (buildNumber == null)
		{
			rsp.sendRedirect2("nodata");
		} else
		{
			rsp.sendRedirect2("../" + buildNumber + "/" + getUrlName());
		}
	}
}
