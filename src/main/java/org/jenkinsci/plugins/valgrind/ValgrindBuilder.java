package org.jenkinsci.plugins.valgrind;

import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.apache.tools.ant.types.Commandline;
import org.jenkinsci.plugins.valgrind.call.ValgrindBooleanOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindCall;
import org.jenkinsci.plugins.valgrind.call.ValgrindEnumOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindExecutable;
import org.jenkinsci.plugins.valgrind.call.ValgrindStringOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindTrackOriginsOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindVersion;
import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;
import org.jenkinsci.plugins.valgrind.util.ValgrindUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * 
 * @author Johannes Ohlemacher
 * 
 */
public class ValgrindBuilder extends Builder
{
	public static final ValgrindVersion VERSION_3_1_0 = ValgrindVersion.createInstance(3, 1, 0);
	public static final ValgrindVersion VERSION_3_2_0 = ValgrindVersion.createInstance(3, 2, 0);
	public static final ValgrindVersion VERSION_3_3_0 = ValgrindVersion.createInstance(3, 3, 0);
	public static final ValgrindVersion VERSION_3_4_0 = ValgrindVersion.createInstance(3, 4, 0);
	public static final ValgrindVersion VERSION_3_5_0 = ValgrindVersion.createInstance(3, 5, 0);
	public static final ValgrindVersion VERSION_3_6_0 = ValgrindVersion.createInstance(3, 6, 0);
	public static final ValgrindVersion VERSION_3_7_0 = ValgrindVersion.createInstance(3, 7, 0);
	public static final ValgrindVersion VERSION_3_8_0 = ValgrindVersion.createInstance(3, 8, 0);
	
	public final String valgrindExecutable;
	public final String workingDirectory;
	public final String includePattern;
	public final String excludePattern;
	public final String outputDirectory;
	public final String outputFileEnding;
	public final String programOptions;
	public final ValgrindTool tool;
	public final String valgrindOptions;
	public final boolean ignoreExitCode;
	public final boolean traceChildren;
	public final boolean childSilentAfterFork;
	public final boolean generateSuppressions;
	public final String  suppressionFiles;
	public final boolean removeOldReports;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public ValgrindBuilder(String valgrindExecutable,
			String workingDirectory, 
			String includePattern, 
			String excludePattern,
			String outputDirectory,
			String outputFileEnding,
			String programOptions,
			ValgrindTool tool,
			String valgrindOptions,
			boolean ignoreExitCode,
			boolean traceChildren,
			boolean childSilentAfterFork,
			boolean generateSuppressions,
			String  suppressionFiles,
			boolean removeOldReports)
	{
		this.valgrindExecutable = valgrindExecutable.trim();
		this.workingDirectory = workingDirectory.trim();
		this.includePattern = includePattern.trim();
		this.excludePattern = excludePattern;
		this.outputDirectory = outputDirectory.trim();
		this.outputFileEnding = outputFileEnding.trim();
		this.programOptions = programOptions;
		this.tool = tool;
		this.valgrindOptions = valgrindOptions;
		this.ignoreExitCode = ignoreExitCode;
		this.traceChildren = traceChildren;
		this.childSilentAfterFork = childSilentAfterFork;
		this.generateSuppressions = generateSuppressions;
		this.suppressionFiles = suppressionFiles;
		this.removeOldReports = removeOldReports;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
	{
		try
		{
			if(removeOldReports)
				deleteOldReports(build, listener);
			
			ValgrindExecutable valgrindExecutable = new ValgrindExecutable(launcher,
					build.getEnvironment(listener).expand(this.valgrindExecutable));

			ValgrindLogger.log( listener, "detected valgrind version ("
					+ valgrindExecutable.getExecutable() + "): "
					+ valgrindExecutable.getVersion()  );
			
			for (FilePath executable : getListOfexecutables(build, listener))
			{
				if (!callValgrindOnExecutable(build, listener, launcher, valgrindExecutable, executable))
					return false;
			}
		}
                catch (RuntimeException e)
                {
                        throw e;
                }
		catch (Exception e)
		{
			ValgrindLogger.log(listener, "ERROR, " + e.getClass().getCanonicalName() + ": " + e.getMessage());
			return false;
		}

		return true;
	}

	public List<String> getSuppressionFileList()
	{
		List<String> files = new ArrayList<String>();

		if(suppressionFiles != null)
		{
			for (String s : suppressionFiles.split(" "))
			{
				if (s == null)
					continue;

				s = s.trim();

				if (s.isEmpty())
					continue;

				files.add(s);
			}
		}

		return files;
	}

	@Override
	public DescriptorImpl getDescriptor()
	{
		return (DescriptorImpl) super.getDescriptor();
	}
	
	private static String fullPath(FilePath fp)
	{
		if(fp == null)
			return "";

		return fullPath(fp.getParent()) + "/" + fp.getName();
	}

	private void deleteOldReports(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException
	{
		if(outputFileEnding == null || outputFileEnding.isEmpty())
			return;

		final String oldReportPattern = "**/*" + outputFileEnding.trim();
		final FilePath reports[] = build.getWorkspace().list(oldReportPattern);

		for(FilePath p : reports)
		{
			if(p.isDirectory())
				continue;

			if(p.delete())
				ValgrindLogger.log( listener, "deleted old report file: " + p.toURI());
			else
				ValgrindLogger.log( listener, "failed to delete old report file: " + p.toURI());
		}
	}

	private List<FilePath> getListOfexecutables(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException
	{
		EnvVars env = build.getEnvironment(null);

		List<FilePath> includes = Arrays.asList(build.getWorkspace().list(env.expand(includePattern)));
		ValgrindLogger.log( listener, "includes files: " + ValgrindUtil.join(includes, ", "));

		List<FilePath> excludes = null;
		if ( excludePattern != null && !excludePattern.isEmpty() )
		{
			excludes = Arrays.asList(build.getWorkspace().list(env.expand(excludePattern)));
			ValgrindLogger.log( listener, "excluded files: " + ValgrindUtil.join(excludes, ", "));
		}

		List<FilePath> files = new ArrayList<FilePath>();

		for (FilePath file : includes)
		{
			if (file == null || (excludes != null && excludes.contains(file)) || file.getName().endsWith(outputFileEnding))
				continue;

			files.add(file);
		}

		return files;
	}

	private boolean callValgrindOnExecutable(AbstractBuild build, BuildListener listener, Launcher launcher, ValgrindExecutable valgrind, FilePath executable) throws IOException, InterruptedException
	{
		EnvVars env = build.getEnvironment(null);

		final String programName = executable.getName();
		env.put("PROGRAM_NAME", programName);

		final String programDir  = fullPath(executable.getParent());
		env.put("PROGRAM_DIR", programDir);

		final FilePath workDir = build.getWorkspace().child(env.expand(workingDirectory));
		if (!workDir.exists() || !workDir.isDirectory())
			workDir.mkdirs();

		FilePath outDir = build.getWorkspace().child(env.expand(outputDirectory));
		if (!outDir.exists() || !outDir.isDirectory())
			outDir.mkdirs();

		final FilePath xmlFile = outDir.child(executable.getName() + ".%p" + env.expand(outputFileEnding));
		final String xmlFilename = xmlFile.getRemote();

		ValgrindCall call = new ValgrindCall();
		call.setValgrindExecutable(valgrind);
		call.setEnv(env);
		call.setWorkingDirectory(workDir);
		call.setProgramName(executable.getRemote());
		call.addProgramArguments(Commandline.translateCommandline(programOptions));
		
		if (tool.getDescriptor() == ValgrindToolMemcheck.D) {
			ValgrindToolMemcheck memcheck = (ValgrindToolMemcheck) tool;

			call.addValgrindOption(new ValgrindStringOption("tool", "memcheck"));
			call.addValgrindOption(new ValgrindStringOption("leak-check", memcheck.leakCheckLevel));
			call.addValgrindOption(new ValgrindBooleanOption("show-reachable", memcheck.showReachable));
			call.addValgrindOption(new ValgrindBooleanOption("undef-value-errors", memcheck.undefinedValueErrors, VERSION_3_2_0));
			call.addValgrindOption(new ValgrindTrackOriginsOption("track-origins", memcheck.trackOrigins, memcheck.undefinedValueErrors, VERSION_3_4_0));
		} else if (tool.getDescriptor() == ValgrindToolHelgrind.D) {
			ValgrindToolHelgrind helgrind = (ValgrindToolHelgrind) tool;
			
			call.addValgrindOption(new ValgrindStringOption("tool", "helgrind"));
			call.addValgrindOption(new ValgrindStringOption("history-level", helgrind.historyLevel));
		} else {
			// This will cause the Valgrind call to fail...
			call.addValgrindOption(new ValgrindStringOption("tool", "unknown-tool"));
		}
		
		call.addValgrindOption(new ValgrindBooleanOption("child-silent-after-fork", childSilentAfterFork, VERSION_3_5_0));
		call.addValgrindOption(new ValgrindBooleanOption("trace-children", traceChildren, VERSION_3_5_0));
		call.addValgrindOption(new ValgrindStringOption("gen-suppressions", generateSuppressions ? "all" : "no"));
		call.addValgrindOption(new ValgrindStringOption("xml", "yes"));
		call.addValgrindOption(new ValgrindStringOption("xml-file", xmlFilename, VERSION_3_5_0));

		for(String s : getSuppressionFileList())
		{
			call.addValgrindOption(new ValgrindStringOption("suppressions", env.expand(s)));
		}

		if ( valgrindOptions != null )
		{
			call.addCustomValgrindOptions(Commandline.translateCommandline(valgrindOptions));
		}

		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		ByteArrayOutputStream stderr = new ByteArrayOutputStream();
		try
		{
			int exitCode = call.exec(listener, launcher, stdout, stderr);
			ValgrindLogger.log(listener, "valgrind exit code: " + exitCode);

			if ( !valgrind.getVersion().isGreaterOrEqual(VERSION_3_5_0) )
			{
				ValgrindLogger.log(listener, "WARNING: valgrind version does not support writing xml output to file directly " +
						"(requires version 3.5.0 or later), xml output will be captured from error out");
                                OutputStream os = xmlFile.write();
                                PrintStream out = new PrintStream(os, true, "UTF-8");
                                try
                                {
                                        out.print(stderr.toString("UTF-8"));
                                }
                                finally
                                {
                                        out.close();
                                        os.close();
                                }
			}

			if (exitCode != 0 && !ignoreExitCode)
				return false;
		}
		finally
		{
			String stdoutString = stdout.toString("UTF-8").trim();
			String stderrString = stderr.toString("UTF-8").trim();

			if ( !stdoutString.isEmpty() )
				ValgrindLogger.log(listener, "valgrind standard out: \n" + stdoutString);

			if ( !stderrString.isEmpty() )
				ValgrindLogger.log(listener, "valgrind error out: \n" + stderrString);
		}

		return true;
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
	{
		@SuppressWarnings("rawtypes")
		public boolean isApplicable(Class<? extends AbstractProject> aClass)
		{
			return true;
		}

		public static DescriptorExtensionList<ValgrindTool,ValgrindTool.ValgrindToolDescriptor> getToolDescriptors()
		{
			return Jenkins.getInstance().<ValgrindTool,ValgrindTool.ValgrindToolDescriptor>getDescriptorList(ValgrindTool.class);
		}

		public FormValidation doCheckIncludePattern(@QueryParameter String includePattern) throws IOException, ServletException
		{
			if (includePattern.length() == 0)
				return FormValidation.error("Please set a pattern");
			
			return FormValidation.ok();
		}
		
		public FormValidation doCheckOutputFileEnding(@QueryParameter String value) throws IOException, ServletException
		{
			if (value.length() == 0)
				return FormValidation.error("Please set a file ending for generated xml reports");
			if (value.charAt(0) != '.' )
				return FormValidation.warning("File ending does not start with a dot");
			
			return FormValidation.ok();
		}		

		public String getDisplayName()
		{
			return "Run Valgrind";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
		{
			return super.configure(req, formData);
		}
	}
	
	public static class ValgrindTool extends AbstractDescribableImpl<ValgrindTool>
	{
		public static class ValgrindToolDescriptor extends Descriptor<ValgrindTool>
		{
			String name;
			
			public ValgrindToolDescriptor(String name, Class<? extends ValgrindTool> clazz)
			{
				super(clazz);
				this.name = name;
			}
			
			@Override
			public String getDisplayName() {
				return name;
			}
		}
		
		public ValgrindToolDescriptor getDescriptor()
		{
			return (ValgrindToolDescriptor) Jenkins.getInstance().getDescriptor(getClass());
		}
	}
	
	public static class ValgrindToolMemcheck extends ValgrindTool
	{
		public final boolean showReachable;
		public final boolean undefinedValueErrors;
		public final String leakCheckLevel;
		public final boolean trackOrigins;

		@DataBoundConstructor
		public ValgrindToolMemcheck(
				boolean showReachable,
				boolean undefinedValueErrors,
				String leakCheckLevel,
				boolean trackOrigins)
		{
			this.showReachable = showReachable;
			this.undefinedValueErrors = undefinedValueErrors;
			this.leakCheckLevel = leakCheckLevel.trim();
			this.trackOrigins = trackOrigins;
		}

		@Extension public static final ValgrindToolDescriptor D = new ValgrindToolDescriptor("Memcheck", ValgrindToolMemcheck.class);
	}
	
	public static class ValgrindToolHelgrind extends ValgrindTool
	{
		public final String historyLevel;
		
		@DataBoundConstructor
		public ValgrindToolHelgrind(
				String historyLevel)
		{
			this.historyLevel = historyLevel;
		}
		
		@Extension public static final ValgrindToolDescriptor D = new ValgrindToolDescriptor("Helgrind", ValgrindToolHelgrind.class);
	}
}
