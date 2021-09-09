package org.jurr.liquibase.envers;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.OfflineConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.Test;

public class EnverSupportParserTest
{
	private static final String SQL_UPDATE_CHANGESET_COMMENT = "-- Changeset ";
	private static final String SQL_ROLLBACK_CHANGESET_COMMENT = "-- Rolling Back ChangeSet: ";

	@Test
	public void testWithoutTag() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithoutTag"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Test 2::JUnit", updateChangeSets.get(1));
		assertEquals(2, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithoutTag"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 2::JUnit", rollbackChangeSets.get(0));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(1));
		assertEquals(2, rollbackChangeSets.size());
	}

	@Test
	public void testWithTagBeforeEnverSupportChangeSet() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithTagBeforeEnverSupportChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals("master.xml::Test 2::JUnit", updateChangeSets.get(2));
		assertEquals("master.xml::Test 3::JUnit", updateChangeSets.get(3));
		assertEquals("master.xml::Tag 2.0.0::liquibase-db-release", updateChangeSets.get(4));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", updateChangeSets.get(5));
		assertEquals("master.xml::Test 4::JUnit", updateChangeSets.get(6));
		assertEquals(7, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithTagBeforeEnverSupportChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 4::JUnit", rollbackChangeSets.get(0));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", rollbackChangeSets.get(1));
		assertEquals("master.xml::Tag 2.0.0::liquibase-db-release", rollbackChangeSets.get(2));
		assertEquals("master.xml::Test 3::JUnit", rollbackChangeSets.get(3));
		assertEquals("master.xml::Test 2::JUnit", rollbackChangeSets.get(4));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(5));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(6));
		assertEquals(7, rollbackChangeSets.size());
	}

	@Test
	public void testWithTagNoAdditionalChangeSet() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithTagNoAdditionalChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals(2, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithTagNoAdditionalChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(0));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(1));
		assertEquals(2, rollbackChangeSets.size());
	}

	@Test
	public void testWithTagAndAdditionalChangeSet() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithTagAndAdditionalChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", updateChangeSets.get(2));
		assertEquals("master.xml::Test 2::JUnit", updateChangeSets.get(3));
		assertEquals(4, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithTagAndAdditionalChangeSet"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 2::JUnit", rollbackChangeSets.get(0));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", rollbackChangeSets.get(1));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(2));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(3));
		assertEquals(4, rollbackChangeSets.size());
	}

	@Test
	public void testWithTwoAdjacentTags() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithTwoAdjacentTags"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", updateChangeSets.get(2));
		assertEquals("master.xml::Tag 1.0.1::liquibase-db-release", updateChangeSets.get(3));
		assertEquals("master.xml::Test 2::JUnit", updateChangeSets.get(4));
		assertEquals(5, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithTwoAdjacentTags"), "master.xml", databaseChangelogCsvFile);
		assertEquals("master.xml::Test 2::JUnit", rollbackChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.1::liquibase-db-release", rollbackChangeSets.get(1));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", rollbackChangeSets.get(2));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(3));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(4));
		assertEquals(5, rollbackChangeSets.size());
	}

	@Test
	public void testWithLatestChangesetWithDifferentContext() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithLatestChangesetWithDifferentContext"), "master.xml", databaseChangelogCsvFile, "core");
		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals(2, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithLatestChangesetWithDifferentContext"), "master.xml", databaseChangelogCsvFile, "core");
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(0));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(1));
		assertEquals(2, rollbackChangeSets.size());
	}

	@Test
	public void testWithBooleanLogicInContext1() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithBooleanLogicInContext"), "master.xml", databaseChangelogCsvFile, "core", "customer");

		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", updateChangeSets.get(2));
		assertEquals("master.xml::Test 2::JUnit", updateChangeSets.get(3));
		assertEquals(4, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithBooleanLogicInContext"), "master.xml", databaseChangelogCsvFile, "core", "customer");
		assertEquals("master.xml::Test 2::JUnit", rollbackChangeSets.get(0));
		assertEquals("master.xml::Envers revision for version develop::liquibase-envers-support plugin", rollbackChangeSets.get(1));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(2));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(3));
		assertEquals(4, rollbackChangeSets.size());
	}

	/**
	 * This test differs from {@link #testWithBooleanLogicInContext1()} in that here we do not include context 'customer'.
	 * Thus, no other changeSets are run after the tag database changeSet, and we do not expect an Envers revision to be inserted.
	 */
	@Test
	public void testWithBooleanLogicInContext2() throws Exception
	{
		final Path databaseChangelogCsvFile = Files.createTempFile(EnverSupportParserTest.class.getSimpleName(), ".JUNIT.csv");

		final List<String> updateChangeSets = getChangeSetsRunByUpdate(Paths.get("src/test/resources/testWithBooleanLogicInContext"), "master.xml", databaseChangelogCsvFile, "core", "otherCustomer");

		assertEquals("master.xml::Test 1::JUnit", updateChangeSets.get(0));
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", updateChangeSets.get(1));
		assertEquals(2, updateChangeSets.size());

		final List<String> rollbackChangeSets = getChangeSetsRunByRollback(Paths.get("src/test/resources/testWithBooleanLogicInContext"), "master.xml", databaseChangelogCsvFile, "core", "otherCustomer");
		assertEquals("master.xml::Tag 1.0.0::liquibase-db-release", rollbackChangeSets.get(0));
		assertEquals("master.xml::Test 1::JUnit", rollbackChangeSets.get(1));
		assertEquals(2, rollbackChangeSets.size());
	}

	private List<String> getChangeSetsRunByUpdate(final Path basedir, final String changeLogFile, final Path runChangeLogCsvFile, final String... contexts) throws LiquibaseException
	{
		final Liquibase liquibase = initLiquibase(basedir, changeLogFile, runChangeLogCsvFile);
		final String sql = updateDatabase(liquibase, contexts);
		return updateSqlToChangeSetList(sql);
	}

	private List<String> getChangeSetsRunByRollback(final Path basedir, final String changeLogFile, final Path runChangeLogCsvFile, final String... contexts) throws LiquibaseException
	{
		final Liquibase liquibase = initLiquibase(basedir, changeLogFile, runChangeLogCsvFile);
		final String sql = rollbackDatabase(liquibase, contexts);
		return rollbackSqlToChangeSetList(sql);
	}

	private String updateDatabase(final Liquibase liquibase, final String... contexts) throws LiquibaseException
	{
		final StringWriter output = new StringWriter();
		liquibase.update(new Contexts(contexts), output);
		return output.toString();
	}

	private String rollbackDatabase(final Liquibase liquibase, final String... contexts) throws LiquibaseException
	{
		final StringWriter output = new StringWriter();
		liquibase.rollback(Integer.MAX_VALUE, new Contexts(contexts), output);
		return output.toString();
	}

	private Liquibase initLiquibase(final Path basedir, final String changeLogFile, final Path runChangeLogCsvFile) throws LiquibaseException
	{
		final FileSystemResourceAccessor resourceAccessor = new FileSystemResourceAccessor(basedir.toAbsolutePath().toString());
		return new Liquibase(changeLogFile, resourceAccessor, new OfflineConnection("offline:postgresql?changeLogFile=" + runChangeLogCsvFile.toAbsolutePath(), resourceAccessor));
	}

	private List<String> updateSqlToChangeSetList(final String sql)
	{
		final List<String> result = new LinkedList<String>();
		for (final String line : sql.split("\n"))
		{
			if (line.startsWith(SQL_UPDATE_CHANGESET_COMMENT))
			{
				result.add(line.substring(SQL_UPDATE_CHANGESET_COMMENT.length()));
			}
		}
		return result;
	}

	private List<String> rollbackSqlToChangeSetList(final String sql)
	{
		final List<String> result = new LinkedList<String>();
		for (final String line : sql.split("\n"))
		{
			if (line.startsWith(SQL_ROLLBACK_CHANGESET_COMMENT))
			{
				result.add(line.substring(SQL_ROLLBACK_CHANGESET_COMMENT.length()));
			}
		}
		return result;
	}
}