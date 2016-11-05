# liquibase-envers-support

This project contains a Liquibase extension that will allow you to automatically add a given changeSet after a `<tagDatabase>` changeSet. It is primarily useful for adding a new Envers revision after you've tagged your database.

## By default this extension does nothing

This extension needs to be enabled before it will actually do anything. The extension is enabled by defining a template changeSet. All changeSets *before* the template changeSet  will be ignored. So, effectively, this extension will start working *after* the your template changeSet.

## Defining the template changeSet

A template changeSet is just like a normal changeSet, only with a specific author. The author needs to be "liquibase-envers-support plugin". The rest of the template changeSet is copied and placed below a `<tagDatabase>` changeSet by the extension.

### Placeholders
In the template, you can use placeholders. These are replaced by actual values by the liquibase-envers-support extension. The following placeholders are available:

 - @PREVIOUS_VERSION@: This will be replaced by the value of the previous `<tagDatabase>` tag.
 - @CURRENT_VERSION@: This will be replaced by the value of the next `<tagDatabase>` tag. If there is no next `<tagDatabase>` tag, the value will default to 'develop'.

### Template changeSet example
```xml
<changeSet context="context" dbms="postgresql" id="Previous DB version was @PREVIOUS_VERSION@; next DB version is @CURRENT_VERSION@" author="liquibase-envers-support plugin">
    <insert tableName="REVINFO">
        <column name="ID" valueComputed="${hibernateSequenceNextVal}"/>
        <column name="TIMESTAMP" valueComputed="${now_timestamp}"/>
        <column name="USERNAME" value="Liquibase (version @CURRENT_VERSION@)"/>
    </insert>

    <rollback>
        <delete tableName="REVINFO">
            <where>ID = (SELECT MAX(ID) FROM REVINFO WHERE USERNAME = 'Liquibase (version @CURRENT_VERSION@)')</where>
        </delete>
    </rollback>
</changeSet>
```

## When the last changeSet is a database tag...
When the very last changeSet is a `<tagDatabase>` changeSet, this extension will *not* add a copy of the template changeSet after it. This is specific to the nature of Envers revisions. (If we *would* add a copy of the template changeSet, it would lead to an empty Envers revision.)