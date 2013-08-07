package org.jenkinsci.plugins.valgrind.util;

import java.io.IOException;

import org.jenkinsci.plugins.valgrind.ValgrindBuildAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Actionable;
import hudson.model.ProminentProjectAction;

public abstract class AbstractValgrindProjectAction extends Actionable implements ProminentProjectAction
{
	protected final AbstractProject<?, ?> project;
	
	protected AbstractValgrindProjectAction(AbstractProject<?, ?> project)
	{
		this.project = project;
	}
	
	public AbstractProject<?, ?> getProject()
	{
		return project;
	}

	public String getIconFileName()
	{
		return "/plugin/valgrind/icons/valgrind-48.png";
	}

	public String getSearchUrl()
	{
		return getUrlName();
	}

    protected abstract AbstractBuild<?, ?> getLastFinishedBuild();

    protected abstract Integer getLastResultBuild();

	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException
	{
		AbstractBuild<?, ?> lastBuild = getLastFinishedBuild();
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
