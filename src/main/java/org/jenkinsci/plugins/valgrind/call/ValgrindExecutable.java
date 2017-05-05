package org.jenkinsci.plugins.valgrind.call;

import hudson.Launcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ValgrindExecutable
{
	private final String executable;
	private final ValgrindVersion version;

	public ValgrindExecutable(Launcher launcher, String executable) throws IOException, InterruptedException
	{
		this.executable = (executable == null || executable.trim().isEmpty()) ? "valgrind" : executable.trim();
		this.version = detectValgrindVersion(launcher, this.executable);
	}

	public String getExecutable()
	{
		return executable;
	}

	public ValgrindVersion getVersion()
	{
		return version;
	}

	private static ValgrindVersion detectValgrindVersion(Launcher launcher, String executable) throws IOException,
			InterruptedException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		Launcher.ProcStarter starter = launcher.launch();
		starter = starter.stdout(os);
		starter = starter.stderr(os);
		starter = starter.cmds(executable, "--version");

		int ret = starter.join();
		
		if (ret != 0)
		{
			throw new IllegalArgumentException("Failed to detect version of valgrind " + executable + " (return code "
					+ ret + "): " + os.toString("UTF-8"));
		}

		ValgrindVersion version = ValgrindVersion.createInstanceFromString(os.toString("UTF-8"));

		if (version == null)
		{
			throw new IllegalArgumentException("Failed to detect version of valgrind " + executable + ", '"
					+ os.toString("UTF-8") + "' is not a valid version string");
		}

		return version;
	}
}
