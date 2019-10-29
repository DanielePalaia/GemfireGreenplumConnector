# Summary

This project serves as example on how to create a simple connector from Gemfire to Postgresql/Greenplum using AsynchListener.
When a modification (INSERT, UPDATE, DELETE) is done on a Gemfire region, it is propagated on the relative Postgresql/Greenplum table.
A Greenplum table will consists on two field id and data where id is related to Gemfire key and data to Gemfire value in a Gemfire region.

## Create the Greenplum database and table to ingest

For the moment the database and table names are embedded in the code, so you need to create
an example database and inside it a test table so defined:

```
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
create async-event-queue --id=kafka-queue --listener=example.geode.kafka.KafkaAsyncEventListener --listener-param=bootstrap.servers#192.168.127.165:9092 --batch-size=5 --batch-time-interval=1000
create region --name=test --type=PARTITION --async-event-queue-id=kafka-queue


```

## Do some operation on Geode and see operation propagated on Greenplum
Do some put to create items:</br>
put --region='test' --key='one' --value='one'</br>
put --region='test' --key='second' --value='second'</br>
put --region='test' --key='third' --value='third'</br>

Do some update:</br>
put --region='test' --key='one' --value='eleven'</br>

Do some delete:</br>
remove --region='test' --key='one'</br>
