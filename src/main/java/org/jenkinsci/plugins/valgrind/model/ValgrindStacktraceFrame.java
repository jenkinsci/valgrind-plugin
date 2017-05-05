package org.jenkinsci.plugins.valgrind.model;

import java.io.Serializable;

import org.jenkinsci.plugins.valgrind.util.ValgrindUtil;


public class ValgrindStacktraceFrame implements Serializable
{
	private static final long serialVersionUID = -2774574337822108808L;
	
	private String objectName;
	private String directoryName;
	private String fileName;
	private Integer lineNumber;
	private String functionName;
	private transient String sourceCode = null;
	
	public String toString()
	{
		return 
		"object    : " + objectName + "\n" +
		"function  : " + functionName + "\n" +
		"directory : " + directoryName + "\n" +
		"file      : " + fileName + "\n" +
		"lineNumber: " + lineNumber;
	}
	
	public String getObjectName()
	{
		return objectName;
	}
	public void setObjectName(String objectName)
	{
		this.objectName = ValgrindUtil.trimToNull( objectName );
	}
	public String getDirectoryName()
	{
		return directoryName;
	}
	public void setDirectoryName(String directoryName)
	{
		this.directoryName = ValgrindUtil.trimToNull( directoryName );
	}
	public String getFileName()
	{
		return fileName;
	}
	public void setFileName(String fileName)
	{
		this.fileName = ValgrindUtil.trimToNull( fileName );
	}
	public Integer getLineNumber()
	{
		return lineNumber;
	}
	public void setLineNumber(Integer lineNumber)
	{
		this.lineNumber = lineNumber;
	}
	public String getFunctionName()
	{
		return functionName;
	}
	public void setFunctionName(String functionName)
	{
		this.functionName = ValgrindUtil.trimToNull( functionName );
	}

	public String getSourceCode()
	{
		return sourceCode;
	}

	public void setSourceCode(String sourceCode)
	{
		this.sourceCode = ValgrindUtil.trimToNull( sourceCode );
	}
	
	public String getFilePath()
	{
		if ( directoryName == null && fileName == null )
			return null;
		
		if ( directoryName == null )
			return fileName;
		
		if ( fileName == null )
			return directoryName;		
		
		return directoryName + "/" + fileName;
	}
	
	public String getFilePathAndLine()
	{
		String filePath = getFilePath();
		if ( filePath == null && lineNumber == null )
			return null;
		
		if ( lineNumber == null )
			return filePath;
		
		if ( filePath == null )
			return lineNumber.toString();		
		
		return filePath + ":" + lineNumber;
	}
}
