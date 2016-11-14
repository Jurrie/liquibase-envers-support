package org.jurr.liquibase.envers;

import java.util.Map;

import liquibase.change.AbstractChange;
import liquibase.change.Change;
import liquibase.change.ChangeMetaData;
import liquibase.change.DatabaseChange;
import liquibase.database.Database;
import liquibase.exception.RollbackImpossibleException;
import liquibase.parser.ext.EnversSupportUtils;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.DeleteStatement;
import liquibase.statement.core.InsertStatement;

@DatabaseChange(name = "liquibase-envers-support", description = "Dummy change that wraps another change just for templateing support", priority = ChangeMetaData.PRIORITY_DEFAULT)
public class TemplateSupportChange extends AbstractChange
{
	private final Change wrappedChange;
	private final String previousVersion;
	private final String currentVersion;

	public TemplateSupportChange(final Change wrappedChange, final String previousVersion, final String currentVersion)
	{
		this.wrappedChange = wrappedChange;
		this.previousVersion = previousVersion;
		this.currentVersion = currentVersion;
	}

	@Override
	public String getConfirmationMessage()
	{
		return EnversSupportUtils.replacePlaceholders(wrappedChange.getConfirmationMessage(), previousVersion, currentVersion);
	}

	@Override
	public SqlStatement[] generateStatements(Database database)
	{
		return replacePlaceholdersSqlStatements(wrappedChange.generateStatements(database), previousVersion, currentVersion);
	}

	@Override
	public SqlStatement[] generateRollbackStatements(Database database) throws RollbackImpossibleException
	{
		return replacePlaceholdersSqlStatements(wrappedChange.generateRollbackStatements(database), previousVersion, currentVersion);
	}

	private SqlStatement[] replacePlaceholdersSqlStatements(final SqlStatement[] input, final String previousVersion, final String currentVersion)
	{
		final SqlStatement[] result = new SqlStatement[input.length];
		for (int i = 0; i < result.length; i++)
		{
			final SqlStatement sqlStatement = input[i];
			result[i] = replacePlaceholdersSqlStatement(sqlStatement, previousVersion, currentVersion);
		}
		return result;
	}

	private SqlStatement replacePlaceholdersSqlStatement(final SqlStatement input, final String previousVersion, final String currentVersion)
	{
		if (input instanceof InsertStatement)
		{
			return replacePlaceholdersSqlStatement((InsertStatement) input, previousVersion, currentVersion);
		}
		else if (input instanceof DeleteStatement)
		{
			return replacePlaceholdersSqlStatement((DeleteStatement) input, previousVersion, currentVersion);
		}
		else
		{
			throw new IllegalArgumentException("Envers support plugin does not (yet) work with " + input.getClass().getSimpleName() + " statements - please add it!");
		}
	}

	private InsertStatement replacePlaceholdersSqlStatement(final InsertStatement input, final String previousVersion, final String currentVersion)
	{
		final InsertStatement result = new InsertStatement(input.getCatalogName(), input.getSchemaName(), input.getTableName());
		for (Map.Entry<String, Object> entry : input.getColumnValues().entrySet())
		{
			final Object newValue = EnversSupportUtils.replacePlaceholders(entry.getValue(), previousVersion, currentVersion);
			result.addColumnValue(entry.getKey(), newValue);
		}
		return result;
	}

	private DeleteStatement replacePlaceholdersSqlStatement(final DeleteStatement input, final String previousVersion, final String currentVersion)
	{
		final DeleteStatement result = new DeleteStatement(input.getCatalogName(), input.getSchemaName(), input.getTableName());
		result.setWhere(EnversSupportUtils.replacePlaceholders(input.getWhere(), previousVersion, currentVersion));
		for (Object whereParameter : input.getWhereParameters())
		{
			result.addWhereParameter(EnversSupportUtils.replacePlaceholders(whereParameter, previousVersion, currentVersion));
		}
		for (String columnName : input.getWhereColumnNames())
		{
			result.addWhereColumnName(EnversSupportUtils.replacePlaceholders(columnName, previousVersion, currentVersion));
		}
		return result;
	}
}