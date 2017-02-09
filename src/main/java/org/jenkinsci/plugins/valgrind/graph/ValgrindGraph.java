package org.jenkinsci.plugins.valgrind.graph;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import hudson.util.ColorPalette;
import hudson.util.Graph;
import hudson.util.ShiftedCategoryAxis;
import hudson.model.Run;

public class ValgrindGraph extends Graph
{
	private final String yLabel;

	private final CategoryDataset categoryDataset;

	public static final int DEFAULT_CHART_WIDTH = 500;
	public static final int DEFAULT_CHART_HEIGHT = 250;

	public ValgrindGraph(Run<?, ?> owner, CategoryDataset categoryDataset, String yLabel,
			int chartWidth, int chartHeight)
	{
		super(owner.getTimestamp(), chartWidth, chartHeight);
		this.yLabel = yLabel;
		this.categoryDataset = categoryDataset;
	}

	@Override
	protected JFreeChart createGraph()
	{
		final JFreeChart chart = ChartFactory.createLineChart(
				null, // chart				
				null, // unused
				yLabel, // range axis label
				categoryDataset, // data
				PlotOrientation.VERTICAL, // orientation
				true, // include legend
				true, // tooltips
				false // urls
				);

		// NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...

		final LegendTitle legend = chart.getLegend();
		legend.setPosition(RectangleEdge.RIGHT);

		chart.setBackgroundPaint(Color.white);

		final CategoryPlot plot = chart.getCategoryPlot();

		// plot.setAxisOffset(new Spacer(Spacer.ABSOLUTE, 5.0, 5.0, 5.0, 5.0));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlinePaint(null);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.black);

		CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
		plot.setDomainAxis(domainAxis);
		domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		domainAxis.setLowerMargin(0.0);
		domainAxis.setUpperMargin(0.0);
		domainAxis.setCategoryMargin(0.0);

		final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
		rangeAxis.setLowerBound(0);
		rangeAxis.setAutoRange(true);

		final LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
		renderer.setBaseStroke(new BasicStroke(2.0f));
		ColorPalette.apply(renderer);

		// crop extra space around the graph
		plot.setInsets(new RectangleInsets(5.0, 0, 0, 5.0));

		return chart;
	}

}
