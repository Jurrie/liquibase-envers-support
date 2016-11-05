package liquibase.parser.ext;

public final class EnversSupportUtils
{
	public static final String PLACEHOLDER_PREVIOUS_VERSION = "@PREVIOUS_VERSION@";
	public static final String PLACEHOLDER_CURRENT_VERSION = "@CURRENT_VERSION@";

	private EnversSupportUtils()
	{
	}

	public static String replacePlaceholders(final String input, final String previousVersion, final String currentVersion)
	{
		return input.replaceAll(PLACEHOLDER_PREVIOUS_VERSION, previousVersion).replaceAll(PLACEHOLDER_CURRENT_VERSION, currentVersion);
	}

	public static Object replacePlaceholders(final Object input, final String previousVersion, final String currentVersion)
	{
		if (input instanceof String)
		{
			return replacePlaceholders((String) input, previousVersion, currentVersion);
		}
		else
		{
			return input;
		}
	}
}