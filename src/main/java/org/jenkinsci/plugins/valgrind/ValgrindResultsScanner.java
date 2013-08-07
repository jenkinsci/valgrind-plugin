package org.jenkinsci.plugins.valgrind;

import hudson.FilePath;
import hudson.Util;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.DirectoryScanner;

public class ValgrindResultsScanner implements FilePath.FileCallable<String[]> {
    private static final long serialVersionUID = -5475538646374717099L;
    private String pattern;

    public ValgrindResultsScanner(String pattern) {
	this.pattern = pattern;
    }

    public String[] invoke(File basedir, VirtualChannel channel)
	    throws IOException, InterruptedException {
	DirectoryScanner ds = new DirectoryScanner();
	String[] includes = { pattern };
	ds.setIncludes(includes);
	ds.setBasedir(basedir);
	ds.setCaseSensitive(true);
	ds.scan();
	return ds.getIncludedFiles();
    }
}
