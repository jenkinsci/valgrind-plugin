package org.jenkinsci.plugins.valgrind;

import org.jenkinsci.plugins.valgrind.util.AbstractValgrindProjectAction;

import hudson.model.Result;
import hudson.model.Job;
import hudson.model.Run;


public class ValgrindProjectAction extends AbstractValgrindProjectAction
{
	protected ValgrindProjectAction(Job<?, ?> project)
	{
		super(project);
	}

	public String getDisplayName()
	{
		return "Valgrind Results";
	}

	public String getUrlName()
	{
		return ValgrindBuildAction.URL_NAME;
	}

	@Override
	public Run<?, ?> getLastFinishedBuild()
	{
		Run<?, ?> lastBuild = getJob().getLastBuild();
		while (lastBuild != null
				&& (lastBuild.isBuilding() || lastBuild.getAction(ValgrindBuildAction.class) == null))
		{
			lastBuild = lastBuild.getPreviousBuild();
		}
		return lastBuild;
	}

	@Override
	public Integer getLastResultBuild()
	{
		for (Run<?, ?> b = (Run<?, ?>) getJob().getLastBuild(); b != null; b = b.getPreviousBuiltBuild())
		{
			ValgrindBuildAction r = b.getAction(ValgrindBuildAction.class);
			
			if (r != null)
				return b.getNumber();
		}
		return null;
	}

	public final boolean isDisplayGraph()
	{
		// Latest
		Run<?, ?> b = getLastFinishedBuild();
		if (b == null)
			return false;

		// Affect previous
		b = b.getPreviousBuild();
		if (b != null)
		{
			for (; b != null; b = b.getPreviousBuild())
			{
				if (b.getResult().isWorseOrEqualTo(Result.FAILURE))
					continue;

				ValgrindBuildAction action = b.getAction(ValgrindBuildAction.class);
				if (action == null || action.getResult() == null)
					continue;

				ValgrindResult result = action.getResult();
				if (result == null)
					continue;

				return true;
			}
		}

		return false;
	}
}
