package org.jenkinsci.plugins.valgrind.parser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.jenkinsci.plugins.valgrind.model.ValgrindAuxiliary;
import org.jenkinsci.plugins.valgrind.model.ValgrindError;
import org.jenkinsci.plugins.valgrind.model.ValgrindErrorKind;
import org.jenkinsci.plugins.valgrind.model.ValgrindProcess;
import org.jenkinsci.plugins.valgrind.model.ValgrindReport;
import org.jenkinsci.plugins.valgrind.model.ValgrindStacktrace;
import org.jenkinsci.plugins.valgrind.model.ValgrindStacktraceFrame;
import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class ValgrindSaxParser implements Serializable
{
	private static final long serialVersionUID = -6889121223670989851L;
	
	private class Handler extends DefaultHandler
	{
		private ValgrindReport currentReport;
		private ValgrindProcess currentProcess;
		private ValgrindError currentError;
		private ValgrindStacktrace currentStacktrace;
		private ValgrindStacktraceFrame currentStacktraceFrame;
		private ValgrindAuxiliary currentAuxiliary;
		private StringBuilder data;
		private String path = "";
		
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException
		{
			path += "/" + qName;
			
			if ( path.equalsIgnoreCase("/valgrindoutput") )
			{
				currentReport = new ValgrindReport();
				currentProcess = new ValgrindProcess();
				currentReport.addProcess(currentProcess);
			}
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error") )
				currentError = new ValgrindError();
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/unique") )
				data = new StringBuilder();
			
			if ( path.equalsIgnoreCase("/valgrindoutput/pid") )
				data = new StringBuilder();			
			
			if ( path.equalsIgnoreCase("/valgrindoutput/ppid") )
				data = new StringBuilder();			
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/kind") )
				data = new StringBuilder();
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/what") )
				data = new StringBuilder();
			
			if ( path.equalsIgnoreCase("/valgrindoutput/args/argv/exe") )
				data = new StringBuilder();
			
			if ( path.equalsIgnoreCase("/valgrindoutput/args/argv/arg") )
				data = new StringBuilder();			
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/text") )
				data = new StringBuilder();				
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/leakedbytes") )
				data = new StringBuilder();				
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/leakedblocks") )
				data = new StringBuilder();				
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/auxwhat") )
				data = new StringBuilder();			
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/stack") )
				currentStacktrace = new ValgrindStacktrace();
			
			if ( currentStacktrace != null )
			{
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame") )
					currentStacktraceFrame = new ValgrindStacktraceFrame();
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/obj")  )
					data = new StringBuilder();
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/fn") )
					data = new StringBuilder();
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/dir") )
					data = new StringBuilder();
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/file") )
					data = new StringBuilder();
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/line") )
					data = new StringBuilder();
			}
		}

		public void endElement(String uri, String localName, String qName) throws SAXException
		{			
			if ( path.equalsIgnoreCase("/valgrindoutput/error") )
			{
				if ( currentAuxiliary != null )
					currentError.addAuxiliaryData(currentAuxiliary);
				
				if ( currentError.getKind() != null && currentProcess != null )
					currentProcess.addError( currentError );
				
				currentError = null;
				currentAuxiliary = null;
			}	
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/unique") )
				currentError.setUniqueId( data.toString() );
			
			if ( path.equalsIgnoreCase("/valgrindoutput/pid") && currentProcess != null )
				currentProcess.setPid(data.toString());
			
			if ( path.equalsIgnoreCase("/valgrindoutput/ppid") && currentProcess != null )
				currentProcess.setPpid(data.toString());				
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/kind") )
			{
				try
				{
					currentError.setKind( ValgrindErrorKind.valueOf( data.toString() ) );					
				}
				catch( IllegalArgumentException e )
				{
					ValgrindLogger.logWarn("Valgrind error not supported: " + data.toString());
				}				
			}
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/what") )
				currentError.setDescription( data.toString() );
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/auxwhat") && currentError != null )
			{
				if ( currentAuxiliary != null )
					currentError.addAuxiliaryData(currentAuxiliary);
				
				currentAuxiliary = new ValgrindAuxiliary();
				currentAuxiliary.setDescription( data.toString() );
			}
			
			if ( path.equalsIgnoreCase("/valgrindoutput/args/argv/exe") && currentProcess != null )
				currentProcess.setExecutable(data.toString());
			
			if ( path.equalsIgnoreCase("/valgrindoutput/args/argv/arg") && currentProcess != null )
				currentProcess.addArgument(data.toString());			
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/text") )
				currentError.setDescription( data.toString() );				
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/leakedbytes") )
			{
				try
				{
					currentError.setLeakedBytes( Integer.valueOf(data.toString()) );
				}
				catch( NumberFormatException e )
				{
					ValgrindLogger.logWarn("'" + data.toString() + "' is not a valid number of leaked bytes");
				}
			}
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/xwhat/leakedblocks") )
			{
				try
				{
					currentError.setLeakedBlocks( Integer.valueOf(data.toString()) );
				}
				catch( NumberFormatException e )
				{
					ValgrindLogger.logWarn("'" + data.toString() + "' is not a valid number of leaked blocks");
				}
			}
			
			if ( path.equalsIgnoreCase("/valgrindoutput/error/stack") && currentStacktrace != null )
			{
				if ( currentAuxiliary != null )
				{
					currentAuxiliary.setStacktrace(currentStacktrace);
				}
				else if ( currentError.getStacktrace() == null )
				{
					currentError.setStacktrace( currentStacktrace );
				}
				
				currentStacktrace = null;				
			}
			
			if ( currentStacktraceFrame != null )
			{
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame") )
				{					
					currentStacktrace.addFrame( currentStacktraceFrame );
					currentStacktraceFrame = null;
				}
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/obj")  )
					currentStacktraceFrame.setObjectName( data.toString() );
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/fn") )
					currentStacktraceFrame.setFunctionName( data.toString() );
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/dir") )
					currentStacktraceFrame.setDirectoryName( data.toString() );
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/file") )
					currentStacktraceFrame.setFileName( data.toString() );
				
				if ( path.equalsIgnoreCase("/valgrindoutput/error/stack/frame/line") )
				{		
					try
					{
						currentStacktraceFrame.setLineNumber( Integer.valueOf( data.toString() ) );
					}
					catch( NumberFormatException e )
					{
					}
				}	
			}
			
			
			data = null;
			path = path.substring(0, path.length() - ( qName.length() + 1 ) );
		}

		public void characters(char ch[], int start, int length) throws SAXException
		{
			if ( data == null )
				return;
			
			data.append(new String(ch,start,length));
		}
		
		public ValgrindReport getReport()
		{
			return currentReport;
		}
	}
	
	public ValgrindReport parse( final File file ) throws ParserConfigurationException, SAXException, IOException
	{
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		SAXParser saxParser = factory.newSAXParser();
		
		Handler handler = new Handler();
		
		saxParser.parse(file, handler);
		
		return handler.getReport();
	}
}
