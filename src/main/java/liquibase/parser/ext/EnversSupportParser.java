package liquibase.parser.ext;

import java.util.LinkedList;
import java.util.List;

import liquibase.change.Change;
import liquibase.change.core.TagDatabaseChange;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.exception.ChangeLogParseException;
import liquibase.exception.LiquibaseException;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import liquibase.servicelocator.PrioritizedService;
import liquibase.util.StringUtils;
import org.jurr.liquibase.envers.TemplateSupportChange;

public class EnversSupportParser implements ChangeLogParser
{
	public static final String ENVERS_SUPPORT_CHANGESET_AUTHOR = "liquibase-envers-support plugin";
	public static final String VERSION_NAME_AFTER_LAST_TAG = "develop";

	@Override
	public int getPriority()
	{
		return PrioritizedService.PRIORITY_DATABASE + 1;
	}

	@Override
	public DatabaseChangeLog parse(String physicalChangeLogLocation, ChangeLogParameters changeLogParameters, ResourceAccessor resourceAccessor) throws ChangeLogParseException
	{
		// Unregister ourselves, let the previous parser parse the whole database changelog.
		// If we do not do this, we will be called again for included changelog files.
		final ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
		changeLogParserFactory.unregister(this);
		try
		{

			final ChangeLogParser previousParser;
			try
			{
				previousParser = changeLogParserFactory.getParser(physicalChangeLogLocation, resourceAccessor);
			}
			catch (LiquibaseException e)
			{
				throw new ChangeLogParseException("Could not get previous parser.", e);
			}

			final DatabaseChangeLog databaseChangeLog = previousParser.parse(physicalChangeLogLocation, changeLogParameters, resourceAccessor);

			addEnversChangeSets(databaseChangeLog);

			return databaseChangeLog;
		}
		finally
		{
			changeLogParserFactory.register(this);
		}
	}

	private void addEnversChangeSets(final DatabaseChangeLog databaseChangeLog)
	{
		final List<ChangeSet> changeSets = databaseChangeLog.getChangeSets();

		final FindTemplatesAndTagDatabaseChangeSetsResult returnValues = findTemplatesAndTagDatabaseChangeSets(changeSets);
		final List<TagDatabaseChange> tagDatabaseChanges = returnValues.tagDatabaseChanges;

		if (tagDatabaseChanges.isEmpty())
		{
			return;
		}

		for (int i = 0; i < tagDatabaseChanges.size(); i++)
		{
			final ChangeSet changeSet = tagDatabaseChanges.get(i).getChangeSet();
			final String previousVersion = tagDatabaseChanges.get(i).getTag();
			final String currentVersion;
			if (tagDatabaseChanges.size() == i + 1)
			{
				if (returnValues.otherChangeSetsAfterLastTagDatabaseChange)
				{
					currentVersion = VERSION_NAME_AFTER_LAST_TAG;
				}
				else
				{
					// Nothing after this last tag database changeSet, so skip creating an Envers revision (to avoid creating an empty revision).
					continue;
				}
			}
			else
			{
				currentVersion = tagDatabaseChanges.get(i + 1).getTag();
			}

			appendEnversChangeSet(databaseChangeLog, changeSet, previousVersion, currentVersion, returnValues.enversTemplateChangeSet);
		}
	}

	private class FindTemplatesAndTagDatabaseChangeSetsResult
	{
		private ChangeSet enversTemplateChangeSet = null;
		private List<TagDatabaseChange> tagDatabaseChanges = new LinkedList<TagDatabaseChange>();
		private boolean otherChangeSetsAfterLastTagDatabaseChange;
	}

	private FindTemplatesAndTagDatabaseChangeSetsResult findTemplatesAndTagDatabaseChangeSets(final List<ChangeSet> changeSets)
	{
		final FindTemplatesAndTagDatabaseChangeSetsResult returnValues = new FindTemplatesAndTagDatabaseChangeSetsResult();

		TagDatabaseChange lastFoundTagDatabaseChange = null;
		for (int i = 0; i < changeSets.size(); i++)
		{
			final ChangeSet changeSet = changeSets.get(i);
			if (returnValues.enversTemplateChangeSet == null)
			{
				if (changeSet.getAuthor().equals(ENVERS_SUPPORT_CHANGESET_AUTHOR))
				{
					// Found the Envers changeSet template - from here on out we can gather TagDatabaseChange instances
					returnValues.enversTemplateChangeSet = changeSet;
					changeSets.remove(i);
					i--;
				}
			}
			else
			{
				final TagDatabaseChange tagDatabaseChange = findTagDatabaseChangeInChangeSet(changeSet);
				if (tagDatabaseChange != null)
				{
					// If two subsequent tag database changeSets are found, skip the first one.
					lastFoundTagDatabaseChange = tagDatabaseChange;
				}
				else if (lastFoundTagDatabaseChange != null)
				{
					// ChangeSet did not contain a tag database change, so it's a 'normal' changeSet.
					// Because the lastFoundTagDatabaseChange is not null, we know there will be a 'normal' changeSet after the lastFoundTagDatabaseChange.
					returnValues.tagDatabaseChanges.add(lastFoundTagDatabaseChange);
					lastFoundTagDatabaseChange = null;
				}
			}
		}

		returnValues.otherChangeSetsAfterLastTagDatabaseChange = lastFoundTagDatabaseChange == null;

		if (lastFoundTagDatabaseChange != null)
		{
			returnValues.tagDatabaseChanges.add(lastFoundTagDatabaseChange);
		}

		return returnValues;
	}

	private TagDatabaseChange findTagDatabaseChangeInChangeSet(final ChangeSet changeSet)
	{
		final List<Change> changes = changeSet.getChanges();
		for (int j = 0; j < changes.size(); j++)
		{
			final Change change = changes.get(j);
			if (change instanceof TagDatabaseChange)
			{
				return (TagDatabaseChange) change;
			}
		}
		return null;
	}

	private void appendEnversChangeSet(final DatabaseChangeLog masterChangeLog, final ChangeSet afterChangeSet, final String previousVersion, final String currentVersion, final ChangeSet enversTemplateChangeSet)
	{
		// The changeSetChangeLog is the DatabaseChangeLog that contains the changeSet directly.
		// The mainChangeLog is the "master" file - it's the file that's run by Liquibase.
		// Because of the <include /> directive, these two variables *can* differ.
		final DatabaseChangeLog changeSetChangeLog = afterChangeSet.getChangeLog();

		final String changeSetId = EnversSupportUtils.replacePlaceholders(enversTemplateChangeSet.getId(), previousVersion, currentVersion);
		final String contextList = StringUtils.join(enversTemplateChangeSet.getContexts(), ",");
		final String dbmsList = StringUtils.join(enversTemplateChangeSet.getDbmsSet(), ",");

		final ChangeSet enversChangeSet = new ChangeSet(changeSetId, ENVERS_SUPPORT_CHANGESET_AUTHOR, enversTemplateChangeSet.isAlwaysRun(), enversTemplateChangeSet.isRunOnChange(), afterChangeSet.getFilePath(), contextList, dbmsList, enversTemplateChangeSet.isRunInTransaction(), enversTemplateChangeSet.getObjectQuotingStrategy(), changeSetChangeLog);

		for (Change change : enversTemplateChangeSet.getChanges())
		{
			enversChangeSet.addChange(new TemplateSupportChange(change, previousVersion, currentVersion));
		}
		for (Change change : enversTemplateChangeSet.getRollBackChanges())
		{
			enversChangeSet.addRollbackChange(new TemplateSupportChange(change, previousVersion, currentVersion));
		}

		int changeSetChangeLogInsertIndex = changeSetChangeLog.getChangeSets().indexOf(afterChangeSet) + 1;
		changeSetChangeLog.getChangeSets().add(changeSetChangeLogInsertIndex, enversChangeSet);

		if (masterChangeLog != changeSetChangeLog)
		{
			int masterChangeLogInsertIndex = masterChangeLog.getChangeSets().indexOf(afterChangeSet) + 1;
			masterChangeLog.getChangeSets().add(masterChangeLogInsertIndex, enversChangeSet);
		}
	}

	@Override
	public boolean supports(String changeLogFile, ResourceAccessor resourceAccessor)
	{
		final ChangeLogParserFactory changeLogParserFactory = ChangeLogParserFactory.getInstance();
		changeLogParserFactory.unregister(this);
		try
		{
			return changeLogParserFactory.getParser(changeLogFile, resourceAccessor).supports(changeLogFile, resourceAccessor);
		}
		catch (LiquibaseException e)
		{
			return false;
		}
		finally
		{
			changeLogParserFactory.register(this);
		}
	}
}