# Summary

This project serves as example on how to create a simple connector from Gemfire to Postgresql/Greenplum using Gemfire AsyncListener ability.</br>
https://gemfire.docs.pivotal.io/98/geode/developing/events/implementing_write_behind_event_handler.html</br>
When a modification (INSERT, UPDATE, DELETE) is done on a Gemfire region, it is propagated on the relative Postgresql/Greenplum table.
It's a modification of:</br>
https://github.com/charliemblack/geode-kafka-integration-example </br>
to ingest on Postgresql rather than kafka. </br>
A Greenplum table will consists on two field id and data where id is related to Gemfire key and data to Gemfire value in a Gemfire region.

## build the project

Build the project with </br>
gradle build

## Create the Greenplum database and table to ingest

The jdbc connection string can be passed in input to the AsyncListener during definition but
for the moment the table name is embedded in the code, so you need to create
an example database and inside it a test table so defined. The connector will connect with gpadmin user without passwd:

```
create database example
\c example
create table test(id text, data text);
```

## How to deploy the project in Geode

The script below shows how to start up a Geode system and deploy code to integrate Geode and Kafka.

```
start locator --name=locator
start server --name=server1
deploy --dir=/Users/dpalaia/Downloads/GemfireGreenplumConnector/geode-greenplum-listener/build/dependancies
y
deploy --dir=/Users/dpalaia/Downloads/GemfireGreenplumConnector/geode-greenplum-listener/build/libs
y
create async-event-queue --id=jdbc-queue --listener=example.geode.greenplum.GreenplumAsyncEventListener --listener-param=--listener-param=jdbcString#jdbc:postgresql://172.16.125.152:5432/example --batch-size=5 --batch-time-interval=1000
create region --name=test --type=PARTITION --async-event-queue-id=jdbc-queue


```

jdbcString#jdbc:postgresql://172.16.125.152:5432/example is the connection string to use, specifying the ip address where Greenplum is stored and database name to use.

## Do some operation on Geode and see operation propagated on Greenplum
```
Do some put to create items:</br>

put --region='test' --key='one' --value='one'
put --region='test' --key='second' --value='second'
put --region='test' --key='third' --value='third'

Do some update:
put --region='test' --key='one' --value='eleven'

Do some delete:
remove --region='test' --key='one'
```

## What to do next

1) Make the connector generic: pass table name to use, username and passwd during async-event-queue definition with --params option (as done already for the connection string) </br>

2) Use Postgresql copy command instead of insert, update, delete one row every time: (IMPLEMENTED ALREADY ON LAST COMMIT)</br>
https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html

3) Change package names (some references to kafka yet)

