package org.jenkinsci.plugins.valgrind.call;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValgrindVersion
{
	public final int	major;
	public final int	minor;
	public final int	patch;

	private ValgrindVersion(int major, int minor, int patch)
	{
		this.major = major;
		this.minor = minor;
		this.patch = patch;
	}

	public boolean isGreaterOrEqual(ValgrindVersion other)
	{
		if (major < other.major)
			return false;

		if (major > other.major)
			return true;

		if (minor < other.minor)
			return false;

		if (minor > other.minor)
			return true;

		if (patch < other.patch)
			return false;

		return true;
	}
	
	public String toString()
	{
		return major + "." + minor + "." + patch;
	}

	public static ValgrindVersion createInstance(int major, int minor, int patch)
	{
		return new ValgrindVersion(major, minor, patch);
	}

	public static ValgrindVersion createInstance(int major, int minor)
	{
		return createInstance(major, minor, 0);
	}

	public static ValgrindVersion createInstance(int major)
	{
		return createInstance(major, 0);
	}

	public static ValgrindVersion createInstanceFromString(String versionString)
	{
		if (versionString == null)
			return null;

		Pattern p = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
		Matcher m = p.matcher(versionString);

		if (!m.find())
			return null;

		return createInstance(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
	}
}
