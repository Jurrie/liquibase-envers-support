package org.jurr.liquibase.envers;

import java.util.Arrays;
import java.util.List;

import liquibase.RuntimeEnvironment;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.changelog.filter.ChangeSetFilter;
import liquibase.changelog.filter.ChangeSetFilterResult;
import liquibase.changelog.filter.ContextChangeSetFilter;
import liquibase.changelog.filter.DbmsChangeSetFilter;
import liquibase.changelog.filter.IgnoreChangeSetFilter;
import liquibase.changelog.filter.LabelChangeSetFilter;
import liquibase.changelog.filter.ShouldRunChangeSetFilter;
import liquibase.changelog.visitor.ChangeExecListener;
import liquibase.database.Database;
import liquibase.database.ObjectQuotingStrategy;
import liquibase.exception.DatabaseException;
import liquibase.exception.MigrationFailedException;

public class EnversSupportChangeSet extends ChangeSet
{
	public EnversSupportChangeSet(final String id, final String author, final boolean alwaysRun, final boolean runOnChange, final String filePath, final String contextList, final String dbmsList, final boolean runInTransaction, final ObjectQuotingStrategy quotingStrategy, final DatabaseChangeLog databaseChangeLog)
	{
		super(id, author, alwaysRun, runOnChange, filePath, contextList, dbmsList, runInTransaction, quotingStrategy, databaseChangeLog);
	}

	@Override
	public ExecType execute(final DatabaseChangeLog databaseChangeLog, final ChangeExecListener listener, final Database database) throws MigrationFailedException
	{
		if (thereIsAnAppliedChangeSetAfterUs(databaseChangeLog))
		{
			return super.execute(databaseChangeLog, listener, database);
		}
		else
		{
			return ExecType.SKIPPED;
		}
	}

	@Override
	public ExecType execute(final DatabaseChangeLog databaseChangeLog, final Database database) throws MigrationFailedException
	{
		if (thereIsAnAppliedChangeSetAfterUs(databaseChangeLog))
		{
			return super.execute(databaseChangeLog, database);
		}
		else
		{
			return ExecType.SKIPPED;
		}
	}

	private boolean thereIsAnAppliedChangeSetAfterUs(final DatabaseChangeLog databaseChangeLog) throws MigrationFailedException
	{
		final List<ChangeSet> changeSets = databaseChangeLog.getChangeSets();

		final int ourIndex = changeSets.indexOf(this);
		for (int i = ourIndex + 1; i < changeSets.size(); i++)
		{
			if (changeSetWillBeApplied(databaseChangeLog, changeSets.get(i)))
			{
				return true;
			}
		}

		return false;
	}

	private boolean changeSetWillBeApplied(final DatabaseChangeLog databaseChangeLog, final ChangeSet changeSet) throws MigrationFailedException
	{
		final RuntimeEnvironment runtimeEnvironment = databaseChangeLog.getRuntimeEnvironment();

		try
		{
			final List<ChangeSetFilter> changeSetFilters = Arrays.asList(
					new ShouldRunChangeSetFilter(runtimeEnvironment.getTargetDatabase(), databaseChangeLog.ignoreClasspathPrefix()),
					new ContextChangeSetFilter(runtimeEnvironment.getContexts()),
					new LabelChangeSetFilter(runtimeEnvironment.getLabels()),
					new DbmsChangeSetFilter(runtimeEnvironment.getTargetDatabase()),
					new IgnoreChangeSetFilter());

			for (ChangeSetFilter filter : changeSetFilters)
			{
				final ChangeSetFilterResult acceptsResult = filter.accepts(changeSet);
				if (!acceptsResult.isAccepted())
				{
					return false;
				}
			}

			return true;
		}
		catch (DatabaseException e)
		{
			throw new MigrationFailedException(changeSet, "Can not determine if this changeSet will be applied", e);
		}
	}
}