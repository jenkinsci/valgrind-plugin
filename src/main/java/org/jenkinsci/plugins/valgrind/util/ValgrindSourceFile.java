package org.jenkinsci.plugins.valgrind.util;

import hudson.model.Run;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream;

import de.java2html.converter.JavaSource2HTMLConverter;
import de.java2html.javasource.JavaSource;
import de.java2html.javasource.JavaSourceParser;
import de.java2html.options.JavaSourceConversionOptions;


/**
 * 
 * @author Johannes Ohlemacher
 * 
 */
public class ValgrindSourceFile 
{
	public static final String SOURCE_DIRECTORY = "valgrind-plugin/source-files";
	
	private static final int GENERATED_HTML_SOURCE_HEADER_SIZE = 12;
	private static final int GENERATED_HTML_SOURCE_FOOTER_SIZE = 9;
	private static final String ERROR_LINE_COLOR = "#FCAF3E";
	private static final String SOURCE_NOT_AVAIABLE_MESSAGE = "<b>Source code not available</b>";	
	
	private Map<String, List<String> > sourceCodeBuffer = new HashMap<String, List<String> >();
	private Map<String, String> sourceFileLookup;
	private int linesBefore;
	private int linesAfter;	
	private Run<?, ?> build;
	
	public ValgrindSourceFile( int linesBefore, int linesAfter, Map<String, String> sourceFileLookup, Run<?, ?> build )
	{
		this.sourceFileLookup = sourceFileLookup;
		this.linesAfter = linesAfter;
		this.linesBefore = linesBefore;
		this.build = build;
	}
	
	public String getSnippet( String fileName, Integer lineNumber )
	{
		if ( fileName == null || lineNumber == null )
			return SOURCE_NOT_AVAIABLE_MESSAGE;
		
		if ( !sourceFileLookup.containsKey(fileName) || sourceFileLookup.get(fileName) == null )
			return SOURCE_NOT_AVAIABLE_MESSAGE;
		
		String localFileName = sourceFileLookup.get( fileName );
		
		if ( !sourceCodeBuffer.containsKey( localFileName ) )
			load( localFileName );		
		
		List<String> lines = sourceCodeBuffer.get( localFileName );
		if ( lines == null || lines.isEmpty() )
			return SOURCE_NOT_AVAIABLE_MESSAGE;
		
		StringBuilder output = new StringBuilder();
		ListIterator<String> it = lines.listIterator();
		int currentLine = 0;
		int errorLine = lineNumber + GENERATED_HTML_SOURCE_HEADER_SIZE;
		
		while( it.hasNext() )
		{
			currentLine++;
			String line = it.next();
			
			boolean append = false;
			
			//html header
			if ( currentLine <= GENERATED_HTML_SOURCE_HEADER_SIZE )
				append = true;					
			
			//lines of interest
			if ( currentLine >= errorLine - linesBefore &&  
				 currentLine <= errorLine + linesAfter )
			{
				append = true;
			}

			//html footer
			if ( currentLine > lines.size() - GENERATED_HTML_SOURCE_FOOTER_SIZE )
				append = true;
			

			if ( currentLine == errorLine )
			{
		        output.append("</code></td></tr>\n");
		        output.append("<tr><td bgcolor=\"" + ERROR_LINE_COLOR + "\">\n");
		        output.append("<code>\n");
		        output.append(line + "\n");
		        output.append("</code></td></tr>\n");
		        output.append("<tr><td>\n");
		        output.append("<code>\n");			        
			}
			else if ( append )
				output.append( line + "\n" );
		}
		
		return output.toString();
		
	}
	
	private void load(String filePath)
	{
		sourceCodeBuffer.put(filePath, null);
		
		try
		{
			File dir = new File( build.getRootDir(), SOURCE_DIRECTORY );
			
			File file = new File( dir, filePath );
			
			if  ( file.exists() && file.isFile() )
			{			
				String sourceCode = highlightSource( IOUtils.toString( new FileInputStream( file ) ) );
				
				@SuppressWarnings("unchecked")
				List<String> lines = IOUtils.readLines( new StringInputStream(sourceCode) );
				
				sourceCodeBuffer.put(filePath,  lines);				
			}
		}
		catch( FileNotFoundException e )
		{	
		} 
		catch (IOException e) 
		{
		}		
	}
	
	private String highlightSource( String src ) throws IOException
	{
		JavaSource source = new JavaSourceParser().parse( src );
		JavaSource2HTMLConverter converter = new JavaSource2HTMLConverter();
		StringWriter writer = new StringWriter();
		JavaSourceConversionOptions options = JavaSourceConversionOptions.getDefault();
		options.setShowLineNumbers(true);
		options.setAddLineAnchors(true);
		converter.convert(source, options, writer);
		
		return writer.toString();
	}
}
