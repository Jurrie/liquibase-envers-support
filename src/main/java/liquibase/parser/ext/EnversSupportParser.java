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

		final List<TagDatabaseChange> tagDatabaseChanges = new LinkedList<TagDatabaseChange>();
		final ChangeSet enversTemplateChangeSet = findTemplatesAndTagDatabaseChangeSets(changeSets, tagDatabaseChanges);
		if (tagDatabaseChanges.isEmpty())
		{
			return;
		}

		for (int i = 0; i < tagDatabaseChanges.size(); i++)
		{
			final String previousVersion = tagDatabaseChanges.get(i).getTag();
			final String currentVersion = tagDatabaseChanges.size() == i + 1 ? VERSION_NAME_AFTER_LAST_TAG : tagDatabaseChanges.get(i + 1).getTag();
			final ChangeSet changeSet = tagDatabaseChanges.get(i).getChangeSet();

			appendEnversChangeSet(changeSet, previousVersion, currentVersion, enversTemplateChangeSet);
		}
	}

	private ChangeSet findTemplatesAndTagDatabaseChangeSets(final List<ChangeSet> changeSets, final List<TagDatabaseChange> tagDatabaseChanges)
	{
		ChangeSet enversTemplateChangeSet = null;
		TagDatabaseChange lastFoundTagDatabaseChange = null;
		for (int i = 0; i < changeSets.size(); i++)
		{
			final ChangeSet changeSet = changeSets.get(i);
			if (enversTemplateChangeSet == null)
			{
				if (changeSet.getAuthor().equals(ENVERS_SUPPORT_CHANGESET_AUTHOR))
				{
					// Found the Envers changeSet template - from here on out we can gather TagDatabaseChange instances
					enversTemplateChangeSet = changeSet;
					changeSets.remove(i);
					i--;
				}
			}
			else
			{
				final TagDatabaseChange tagDatabaseChange = findTagDatabaseChangeInChangeSet(changeSet);
				if (tagDatabaseChange != null)
				{
					lastFoundTagDatabaseChange = tagDatabaseChange;
				}
				else if (lastFoundTagDatabaseChange != null)
				{
					// ChangeSet did not contain a tag database change, so it's a 'normal' changeSet.
					// Because the lastFoundTagDatabaseChange is not null, we know there will be a 'normal' changeSet after the lastFoundTagDatabaseChange.
					tagDatabaseChanges.add(lastFoundTagDatabaseChange);
					lastFoundTagDatabaseChange = null;
				}
			}
		}

		return enversTemplateChangeSet;
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

	private void appendEnversChangeSet(final ChangeSet afterChangeSet, final String previousVersion, final String currentVersion, final ChangeSet enversTemplateChangeSet)
	{
		final DatabaseChangeLog changeLog = afterChangeSet.getChangeLog();

		final String changeSetId = EnversSupportUtils.replacePlaceholders(enversTemplateChangeSet.getId(), previousVersion, currentVersion);
		final String contextList = StringUtils.join(enversTemplateChangeSet.getContexts(), ",");
		final String dbmsList = StringUtils.join(enversTemplateChangeSet.getDbmsSet(), ",");

		final ChangeSet enversChangeSet = new ChangeSet(changeSetId, ENVERS_SUPPORT_CHANGESET_AUTHOR, enversTemplateChangeSet.isAlwaysRun(), enversTemplateChangeSet.isRunOnChange(), afterChangeSet.getFilePath(), contextList, dbmsList, enversTemplateChangeSet.isRunInTransaction(), enversTemplateChangeSet.getObjectQuotingStrategy(), changeLog);

		for (Change change : enversTemplateChangeSet.getChanges())
		{
			enversChangeSet.addChange(new TemplateSupportChange(change, previousVersion, currentVersion));
		}
		for (Change change : enversTemplateChangeSet.getRollBackChanges())
		{
			enversChangeSet.addRollbackChange(new TemplateSupportChange(change, previousVersion, currentVersion));
		}

		int index = changeLog.getChangeSets().indexOf(afterChangeSet) + 1;
		changeLog.getChangeSets().add(index, enversChangeSet);
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