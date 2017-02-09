package org.jenkinsci.plugins.valgrind.util;

import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.valgrind.model.ValgrindStacktrace;
import org.jenkinsci.plugins.valgrind.model.ValgrindStacktraceFrame;


/**
 * 
 * @author Johannes Ohlemacher
 * 
 */
public class ValgrindSourceGrabber
{
	private TaskListener listener;
	private Map<String, String> lookup = new HashMap<String, String>();
	private File destDirectory;
	private FilePath basedir;
	private int index = 0;
	
	public ValgrindSourceGrabber(TaskListener listener, FilePath basedir)
	{
		this.listener = listener;
		this.basedir = basedir;	
	}
	
	public boolean init(File localRoot)
	{
        this.destDirectory = new File(localRoot, ValgrindSourceFile.SOURCE_DIRECTORY);
        
        if ( !destDirectory.exists() ) 
        {
            if ( !destDirectory.mkdirs() )
            {
            	ValgrindLogger.log(listener, "ERROR: failed to create local directory for source files: '" + destDirectory.getAbsolutePath() + "'");
            	return false;
            }
        }
        
        return true;
	}
	
	public void grabFromStacktrace(ValgrindStacktrace stacktrace)
	{
		if ( stacktrace == null )
			return;
		
		for ( ValgrindStacktraceFrame frame : stacktrace.getFrames() )
		{
			if ( frame == null )
				continue;
			
			String filePath =  frame.getFilePath();
			
			if ( filePath == null || filePath.isEmpty() || lookup.containsKey( filePath ) )
				continue;				
			
			FilePath file = new FilePath( basedir, filePath );
			
			index++;				
			lookup.put( filePath, retrieveSourceFile( file ) );
		}
	}
	
	public Map<String, String> getLookupMap()
	{
		return lookup;
	}
	
	private String retrieveSourceFile( FilePath file )
	{		
		try
		{			
			if ( !file.exists() )
			{
				ValgrindLogger.log(listener, "'" + file.getRemote() + "' does not exist, source code won't be available");
				return null;
			}
			
			if ( file.isDirectory() )
			{
				ValgrindLogger.log(listener, "WARN: '" + file.getRemote() + "' is a directory, source code won't be available");
				return null;
			}		
			
			String fileName = "source_" + index + ".tmp";
			File masterFile = new File( destDirectory, fileName );
			
			ValgrindLogger.log(listener, "copying source file '" + file.getRemote() + "' to '" + fileName + "'...");
			
			if ( masterFile.exists() )
			{
				ValgrindLogger.log(listener, "WARN: local file '" + fileName + "' already exists");
				return null;
			}
            
            FileOutputStream outputStream = new FileOutputStream(masterFile);
            
            file.copyTo(outputStream);            
            
			return fileName;			
		}
		catch (Exception e)
		{
			ValgrindLogger.log(listener, "ERROR: failed to retrieve '" + file.getRemote() + "', " + e.getMessage() );
		}
		
		return null;		
	}
}
