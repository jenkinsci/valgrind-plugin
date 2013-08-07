package org.jenkinsci.plugins.valgrind.util;

import java.io.IOException;

import org.jenkinsci.plugins.valgrind.ValgrindResult;

public class ValgrindSummary
{
	/**
	 * Creates an HTML valgrind summary.
	 * 
	 * @param result
	 *            the valgrind result object
	 * @return the HTML fragment representing the valgrind report summary
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public static String createReportSummary(ValgrindResult result) throws IOException, InterruptedException
	{

		StringBuilder summary = new StringBuilder();
		int errorCount = result.getReport().getErrorList().getErrorCount();

		if (errorCount == 0)
		{
			summary.append("no errors");
		} 
		else
		{
			summary.append("<a href=\"valgrindResult\">");

			if (errorCount == 1)
				summary.append("one error, ");
			else
				summary.append(Integer.toString(errorCount) + " errors, ");

			summary.append(result.getReport().getErrorList().getDefinitelyLeakedBytes());
			summary.append(" bytes definitely lost");
			
			summary.append("</a>");
		}

		return summary.toString();
	}
	
}
