package org.jenkinsci.plugins.valgrind.parser;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;
import org.jenkinsci.remoting.RoleChecker;


public class ValgrindParserResult implements FilePath.FileCallable<ValgrindReport>
{
	private static final long serialVersionUID = -5475538646374717099L;
	private String pattern;
	
	public ValgrindParserResult( String pattern )
	{
		this.pattern = pattern;
	}

	public ValgrindReport invoke(File basedir, VirtualChannel channel) throws IOException, InterruptedException
	{
		ValgrindLogger.logFine("looking for valgrind files in '" + basedir.getAbsolutePath() + "' with pattern '" + pattern + "'");
		
		ValgrindReport valgrindReport = new ValgrindReport();
		
		for ( String fileName : findValgrindsReports( basedir ) )
		{
			ValgrindLogger.logFine("parsing " + fileName + "...");
			try
			{
				ValgrindReport report = new ValgrindSaxParser().parse( new File(basedir, fileName) );
				if(report != null && report.isValid())
				{
					valgrindReport.integrate( report );										
				}
				else
				{
					valgrindReport.addParserError(fileName, "no valid data");					
				}
			}
                        catch (RuntimeException e)
                        {
                                throw e;
                        }
			catch (Exception e)
			{
				valgrindReport.addParserError(fileName, e.getMessage());
			}
		}

		return valgrindReport;
	}
	
	private String[] findValgrindsReports(File parentPath)
	{
		FileSet fs = Util.createFileSet(parentPath, this.pattern);
		DirectoryScanner ds = fs.getDirectoryScanner();
		return ds.getIncludedFiles();
	}

	@Override
	public void checkRoles(RoleChecker roleChecker) throws SecurityException {

	}
}
