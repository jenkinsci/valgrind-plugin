package org.jenkinsci.plugins.valgrind;

import hudson.model.HealthReport;
import hudson.model.AbstractBuild;
import hudson.util.ChartUtil;
import hudson.util.DataSetBuilder;
import hudson.util.Graph;

import java.io.IOException;
import java.util.Calendar;

import org.jenkinsci.plugins.valgrind.config.ValgrindPublisherConfig;
import org.jenkinsci.plugins.valgrind.graph.ValgrindGraph;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.util.AbstractValgrindBuildAction;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;


public class ValgrindBuildAction extends AbstractValgrindBuildAction
{
	public static String URL_NAME = "valgrindResult";
	
	private ValgrindResult result;
	private ValgrindPublisherConfig config;

	public ValgrindBuildAction(AbstractBuild<?, ?> owner, ValgrindResult result,
			ValgrindPublisherConfig config)
	{
		super(owner);
		this.result = result;
		this.config = config;
	}

	AbstractBuild<?, ?> getBuild()
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

	public String getSearchUrl()
	{
		return getUrlName();
	}

	public Object getTarget()
	{
		return result;
	}

	public HealthReport getBuildHealth()
	{
		return new HealthReport();
	}

	public String getIconFileName()
	{
		return "/plugin/valgrind/icons/valgrind-48.png";
	}

	public String getDisplayName()
	{
		return "Valgrind Result";
	}

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
			ChartUtil.NumberOnlyBuildLabel label = new ChartUtil.NumberOnlyBuildLabel(buildAction.owner);
			ValgrindReport report = buildAction.getResult().getReport();

			dsb.add(report.getErrorList().getInvalidReadErrorCount() + report.getErrorList().getInvalidWriteErrorCount(), "Invalid reads/writes", label);
			dsb.add(report.getErrorList().getLeakDefinitelyLostErrorCount(), "Leaks (definitely lost)", label);
			dsb.add(report.getErrorList().getLeakPossiblyLostErrorCount(), "Leaks (possibly lost)", label);
			dsb.add(report.getErrorList().getUninitializedConditionErrorCount() + report.getErrorList().getUninitializedValueErrorCount(), "Uninitialized value/cond.", label);
			dsb.add(report.getErrorList().getInvalidFreeErrorCount() + report.getErrorList().getMismatchedFreeErrorCount(), "Illegal/mismatched frees", label);
			dsb.add(report.getErrorList().getOverlapErrorCount(), "Overlaps", label);
			dsb.add(report.getErrorList().getSyscallParamErrorCount(), "Illegal system calls", label);
		}
		return dsb;
	}
}
