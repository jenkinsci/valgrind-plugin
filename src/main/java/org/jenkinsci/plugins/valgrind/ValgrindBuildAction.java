package org.jenkinsci.plugins.valgrind;

import hudson.model.HealthReport;
import hudson.model.Run;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;
import hudson.model.Action;

import java.io.IOException;
import java.util.Calendar;

import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;
import org.jenkinsci.plugins.valgrind.graph.ValgrindGraph;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.util.AbstractValgrindBuildAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import jenkins.tasks.SimpleBuildStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ValgrindBuildAction extends AbstractValgrindBuildAction implements SimpleBuildStep.LastBuildAction
{
	public static final String URL_NAME = "valgrindResult";

	private ValgrindResult result;
	private ValgrindPublisherConfig config;

	public ValgrindBuildAction(Run<?, ?> owner, ValgrindResult result,
			ValgrindPublisherConfig config)
	{
		super(owner);
		this.result = result;
		this.config = config;
	}

	public Run<?, ?> getBuild()
	{
		return this.owner;
	}

	public ValgrindResult getResult()
	{
		return result;
	}

	public ValgrindPublisherConfig getConfig()
	{
		return config;
	}

	@Override
	public String getSearchUrl()
	{
		return getUrlName();
	}

	@Override
	public Object getTarget()
	{
		return result;
	}

	@Override
	public HealthReport getBuildHealth()
	{
		return new HealthReport();
	}

	@Override
	public String getIconFileName()
	{
		return "/plugin/valgrind/icons/valgrind-48.png";
	}

	@Override
	public String getDisplayName()
	{
		return "Valgrind Result";
	}

	@Override
	public String getUrlName()
	{
		return URL_NAME;
	}

	@Override
	public void doGraph(StaplerRequest req, StaplerResponse rsp) throws IOException, InterruptedException
	{
		if (ChartUtil.awtProblemCause != null)
		{
			rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
			return;
		}

		Calendar timestamp = getBuild().getTimestamp();
		if (req.checkIfModified(timestamp, rsp))
		{
			return;
		}

		//TODO: graph size should be part of global configuration
		Graph g = new ValgrindGraph(getOwner(), getDataSetBuilder().build(), "Number of errors", ValgrindGraph.DEFAULT_CHART_WIDTH, ValgrindGraph.DEFAULT_CHART_HEIGHT);
		g.doPng(req, rsp);
	}

	/**
	 * @return a DataSetBuilder
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> getDataSetBuilder() throws IOException, InterruptedException
	{
		DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel> dsb = new DataSetBuilder<String, ChartUtil.NumberOnlyBuildLabel>();

		for (ValgrindBuildAction buildAction = this; buildAction != null; buildAction = buildAction.getPreviousResult())
		{
			final Run<?,?> run = buildAction.owner;
			final ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(run);
			final ValgrindReport report = buildAction.getResult().getReport();

			// Memcheck:
			dsb.add(report.getErrorList().getInvalidReadErrorCount() + report.getErrorList().getInvalidWriteErrorCount(), "Invalid reads/writes", label);
			dsb.add(report.getErrorList().getLeakDefinitelyLostErrorCount(), "Leaks (definitely lost)", label);
			dsb.add(report.getErrorList().getLeakPossiblyLostErrorCount(), "Leaks (possibly lost)", label);
			dsb.add(report.getErrorList().getUninitializedConditionErrorCount() + report.getErrorList().getUninitializedValueErrorCount(), "Uninitialized value/cond.", label);
			dsb.add(report.getErrorList().getInvalidFreeErrorCount() + report.getErrorList().getMismatchedFreeErrorCount(), "Illegal/mismatched frees", label);
			dsb.add(report.getErrorList().getOverlapErrorCount(), "Overlaps", label);
			dsb.add(report.getErrorList().getSyscallParamErrorCount(), "Illegal system calls", label);

			// Helgrind:
			dsb.add(report.getErrorList().getRaceErrorCount(), "Data races", label);
			dsb.add(report.getErrorList().getUnlockUnlockedErrorCount() + report.getErrorList().getUnlockForeignErrorCount() + report.getErrorList().getUnlockBogusErrorCount(), "Unlock issues", label);
			dsb.add(report.getErrorList().getLockOrderErrorCount(), "Lock order", label);
			dsb.add(report.getErrorList().getPthAPIErrorCount(), "Pthread API", label);
			dsb.add(report.getErrorList().getMiscErrorCount(), "Helgrind misc", label);
		}
		return dsb;
	}
	@Override
	public Collection<? extends Action> getProjectActions() {
		List<ValgrindProjectAction> projectActions = new ArrayList<>();
 		projectActions.add(new ValgrindProjectAction(owner.getParent()));
		return projectActions;
	}

    @Override
    public void setOwner(Run<?, ?> owner) {
        super.setOwner(owner);

        result.setOwner(owner);
    }
}
