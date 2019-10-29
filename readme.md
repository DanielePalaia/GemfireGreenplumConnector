# Summary

This project serves as example on how to create a simple connector from Gemfire to Postgresql/Greenplum using Gemfire AsyncListener ability.</br>
https://gemfire.docs.pivotal.io/98/geode/developing/events/implementing_write_behind_event_handler.html</br>
When a modification (INSERT, UPDATE, DELETE) is done on a Gemfire region, it is propagated on the relative Postgresql/Greenplum table.
It's a modification of:</br>
https://github.com/charliemblack/geode-kafka-integration-example </br>
to ingest on Postgresql rather than kafka. </br>
It is inserting, updating, deleting one row every time (not efficient use copy instead). </br>
A Greenplum table will consists on two field id and data where id is related to Gemfire key and data to Gemfire value in a Gemfire region.

## build the project

Build the project with </br>
gradle build

## Create the Greenplum database and table to ingest

For the moment the database and table names are embedded in the code, so you need to create
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
deploy --dir=/Users/dpalaia/Downloads/GemfireGreenplumConnector/geode-kafka-listener/build/dependancies
y
deploy --dir=/Users/dpalaia/Downloads/GemfireGreenplumConnector/geodee-kafka-listener/build/libs
y
create async-event-queue --id=jdbc-queue --listener=example.geode.kafka.KafkaAsyncEventListener --listener-param=bootstrap.servers#192.168.127.165:9092 --batch-size=5 --batch-time-interval=1000
create region --name=test --type=PARTITION --async-event-queue-id=jdbc-queue


```

## Do some operation on Geode and see operation propagated on Greenplum
Do some put to create items:</br>
```
put --region='test' --key='one' --value='one'</br>
put --region='test' --key='second' --value='second'</br>
put --region='test' --key='third' --value='third'</br>

Do some update:</br>
put --region='test' --key='one' --value='eleven'</br>

Do some delete:</br>
remove --region='test' --key='one'</br>
```

## What to do next

1) Make the connector generic: pass database name, table, username and passwd during async-event-queue definition with --params option </br>

2) Use Postgresql copy command instead of insert, update, delete one row every time:</br>
https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/copy/CopyManager.html

3) Change package names (some references to kafka yet)

