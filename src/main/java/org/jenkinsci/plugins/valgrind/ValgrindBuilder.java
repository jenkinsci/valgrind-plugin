package org.jenkinsci.plugins.valgrind;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.tools.ant.types.Commandline;
import org.jenkinsci.plugins.valgrind.call.ValgrindBooleanOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindCall;
import org.jenkinsci.plugins.valgrind.call.ValgrindExecutable;
import org.jenkinsci.plugins.valgrind.call.ValgrindStringOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindTrackOriginsOption;
import org.jenkinsci.plugins.valgrind.call.ValgrindVersion;
import org.jenkinsci.plugins.valgrind.util.ValgrindLogger;
import org.jenkinsci.plugins.valgrind.util.ValgrindUtil;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.AbortException;
import hudson.DescriptorExtensionList;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

/**
 *
 * @author Johannes Ohlemacher
 *
 */
public class ValgrindBuilder extends Builder implements SimpleBuildStep
{
	public static final ValgrindVersion VERSION_3_1_0 = ValgrindVersion.createInstance(3, 1, 0);
	public static final ValgrindVersion VERSION_3_2_0 = ValgrindVersion.createInstance(3, 2, 0);
	public static final ValgrindVersion VERSION_3_3_0 = ValgrindVersion.createInstance(3, 3, 0);
	public static final ValgrindVersion VERSION_3_4_0 = ValgrindVersion.createInstance(3, 4, 0);
	public static final ValgrindVersion VERSION_3_5_0 = ValgrindVersion.createInstance(3, 5, 0);
	public static final ValgrindVersion VERSION_3_6_0 = ValgrindVersion.createInstance(3, 6, 0);
	public static final ValgrindVersion VERSION_3_7_0 = ValgrindVersion.createInstance(3, 7, 0);
	public static final ValgrindVersion VERSION_3_8_0 = ValgrindVersion.createInstance(3, 8, 0);

	public String valgrindExecutable;
	public String workingDirectory;
	public String includePattern;
	public String excludePattern;
	public String outputDirectory;
	public String outputFileEnding;
	public String programOptions;
	public ValgrindTool tool;
	public String valgrindOptions;
	public boolean ignoreExitCode;
	public boolean traceChildren;
	public boolean childSilentAfterFork;
	public boolean generateSuppressions;
	public String  suppressionFiles;
	public boolean removeOldReports;

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
/*
	public ValgrindBuilderConfig getValgrindBuilderConfig()
	{

		return new valgrindBuilderConfig;
	}

	public void setValgrindBuilderConfig(ValgrindBuilderConfig valgrindBuilderConfig)
	{
		this.valgrindBuilderConfig = valgrindBuilderConfig;
	}
*/
	@Override
	public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException
	{
		try
		{
			EnvVars env = run.getEnvironment(listener);

			if(this.removeOldReports)
			{
				deleteOldReports(workspace, listener);
			}

			ValgrindExecutable valgrindExecutable = new ValgrindExecutable(launcher, env.expand(this.valgrindExecutable));

			ValgrindLogger.log( listener, "detected valgrind version ("
					+ valgrindExecutable.getExecutable() + "): "
					+ valgrindExecutable.getVersion()  );

			for (FilePath executable : getListOfExecutables(workspace, env, listener))
			{
				if (!callValgrindOnExecutable(workspace, env, listener, launcher, valgrindExecutable, executable))
				{
					throw new AbortException("Valgrind checks failed");
				}
			}
		}
                catch (RuntimeException|AbortException e)
                {
                        throw e;
                }
		catch (Exception e)
		{
			ValgrindLogger.log(listener, "ERROR, " + e.getClass().getCanonicalName() + ": " + e.getMessage());
		}
	}

	private List<String> getSuppressionFileList()
	{
		List<String> files = new ArrayList<String>();

		if(this.suppressionFiles != null)
		{
			for (String s : this.suppressionFiles.split(" "))
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

	private void deleteOldReports(FilePath workspace, TaskListener listener) throws IOException, InterruptedException
	{
		if(this.outputFileEnding == null || this.outputFileEnding.isEmpty())
			return;

		final String oldReportPattern = "**/*" + this.outputFileEnding.trim();
		final FilePath reports[] = workspace.list(oldReportPattern);

		for(FilePath p : reports)
		{
			if(p.isDirectory())
				continue;

			if(p.delete())
				ValgrindLogger.log(listener, "deleted old report file: " + p.toURI());
			else
				ValgrindLogger.log(listener, "failed to delete old report file: " + p.toURI());
		}
	}

	private List<FilePath> getListOfExecutables(FilePath workspace, EnvVars env, TaskListener listener) throws IOException, InterruptedException
	{
		List<FilePath> includes = Arrays.asList(workspace.list(env.expand(this.includePattern)));
		ValgrindLogger.log( listener, "includes files: " + ValgrindUtil.join(includes, ", "));

		List<FilePath> excludes = null;
		if ( this.excludePattern != null && !this.excludePattern.isEmpty() )
		{
			excludes = Arrays.asList(workspace.list(env.expand(this.excludePattern)));
			ValgrindLogger.log( listener, "excluded files: " + ValgrindUtil.join(excludes, ", "));
		}

		List<FilePath> files = new ArrayList<FilePath>();

		for (FilePath file : includes)
		{
			if (file == null || (excludes != null && excludes.contains(file)) || file.getName().endsWith(this.outputFileEnding))
				continue;

			files.add(file);
		}

		return files;
	}

	private boolean callValgrindOnExecutable(FilePath workspace, EnvVars env, TaskListener listener, Launcher launcher, ValgrindExecutable valgrind, FilePath executable) throws IOException, InterruptedException
	{
		final String programName = executable.getName();
		env.put("PROGRAM_NAME", programName);

		final String programDir  = fullPath(executable.getParent());
		env.put("PROGRAM_DIR", programDir);

		final FilePath workDir = workspace.child(env.expand(this.workingDirectory));
		if (!workDir.exists() || !workDir.isDirectory())
			workDir.mkdirs();

		FilePath outDir = workspace.child(env.expand(this.outputDirectory));
		if (!outDir.exists() || !outDir.isDirectory())
			outDir.mkdirs();

		final FilePath xmlFile = outDir.child(executable.getName() + ".%p" + env.expand(this.outputFileEnding));
		final String xmlFilename = xmlFile.getRemote();

		ValgrindCall call = new ValgrindCall();
		call.setValgrindExecutable(valgrind);
		call.setEnv(env);
		call.setWorkingDirectory(workDir);
		call.setProgramName(executable.getRemote());
		call.addProgramArguments(Commandline.translateCommandline(this.programOptions));

		if (this.tool.getDescriptor() == ValgrindToolMemcheck.D) {
			ValgrindToolMemcheck memcheck = (ValgrindToolMemcheck) this.tool;

			call.addValgrindOption(new ValgrindStringOption("tool", "memcheck"));
			call.addValgrindOption(new ValgrindStringOption("leak-check", memcheck.leakCheckLevel));
			call.addValgrindOption(new ValgrindBooleanOption("show-reachable", memcheck.showReachable));
			call.addValgrindOption(new ValgrindBooleanOption("undef-value-errors", memcheck.undefinedValueErrors, VERSION_3_2_0));
			call.addValgrindOption(new ValgrindTrackOriginsOption("track-origins", memcheck.trackOrigins, memcheck.undefinedValueErrors, VERSION_3_4_0));
		} else if (this.tool.getDescriptor() == ValgrindToolHelgrind.D) {
			ValgrindToolHelgrind helgrind = (ValgrindToolHelgrind) this.tool;

			call.addValgrindOption(new ValgrindStringOption("tool", "helgrind"));
			call.addValgrindOption(new ValgrindStringOption("history-level", helgrind.historyLevel));
		} else {
			// This will cause the Valgrind call to fail...
			call.addValgrindOption(new ValgrindStringOption("tool", "unknown-tool"));
		}

		call.addValgrindOption(new ValgrindBooleanOption("child-silent-after-fork", this.childSilentAfterFork, VERSION_3_5_0));
		call.addValgrindOption(new ValgrindBooleanOption("trace-children", this.traceChildren, VERSION_3_5_0));
		call.addValgrindOption(new ValgrindStringOption("gen-suppressions", this.generateSuppressions ? "all" : "no"));
		call.addValgrindOption(new ValgrindStringOption("xml", "yes"));
		call.addValgrindOption(new ValgrindStringOption("xml-file", xmlFilename, VERSION_3_5_0));

		for(String s : getSuppressionFileList())
		{
			call.addValgrindOption(new ValgrindStringOption("suppressions", env.expand(s)));
		}

		if (this.valgrindOptions != null)
		{
			call.addCustomValgrindOptions(Commandline.translateCommandline(this.valgrindOptions));
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

			if (exitCode != 0 && !this.ignoreExitCode)
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
		@Override
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

		@Override
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

	public static abstract class ValgrindTool extends AbstractDescribableImpl<ValgrindTool> implements Serializable
	{
		private static final long serialVersionUID = 4775855657469295418L;

		public static class ValgrindToolDescriptor extends Descriptor<ValgrindTool>
		{
			private String name;

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

		@Override
		public ValgrindToolDescriptor getDescriptor()
		{
			return (ValgrindToolDescriptor) Jenkins.getInstance().getDescriptor(getClass());
		}
	}

	public static class ValgrindToolMemcheck extends ValgrindTool
	{
		private static final long serialVersionUID = 5175503606326531712L;

		public boolean showReachable;
		public boolean undefinedValueErrors;
		public String leakCheckLevel;
		public boolean trackOrigins;

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
		private static final long serialVersionUID = 5691098331605204573L;

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
