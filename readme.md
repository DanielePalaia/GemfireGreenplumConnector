# Summary

This project serves as example on how to create a simple connector from Gemfire to Postgresql/Greenplum using Gemfire AsyncListener ability.</br>
https://gemfire.docs.pivotal.io/98/geode/developing/events/implementing_write_behind_event_handler.html</br>
When a modification (INSERT, UPDATE, DELETE) is done on a Gemfire region, it is propagated on the relative Postgresql/Greenplum table.</br>
A Greenplum table will consists on two field id and data where id is related to Gemfire key and data to Gemfire value in a Gemfire region.
The connector is generic and will take in input during configurations: Jdbc connection string, username/passwd and namespace.table of the Greenplum database where we want to ingest the rows.
It is using Copy to batch rows and insert them in Greenplum to maximize performance.

## build the project

Build the project with </br>
gradle build

## Create the Greenplum database and table to ingest

The jdbc connection string can be passed in input to the AsyncListener during definition but
for the moment the table name is embedded in the code, so you need to create
an example database and inside it a test table so defined. The connector will connect with gpadmin user without passwd:

```
create database dashboard
\c dashboard
create schema rws;
create table rws.test(id text, data json);
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
create async-event-queue --id=jdbc-queue --listener=example.geode.greenplum.GreenplumAsyncEventListener --listener-param=jdbcString#jdbc:postgresql://172.16.125.152:5432/dashboard,username#gpadmin,passwd#,tablename#rws.test,delim#|,rejectlimit#10 --batch-size=3 --batch-time-interval=3000000
create region --name=test --type=PARTITION --async-event-queue-id=jdbc-queue


```

## How to specify input
Input are specified with  --listener-param option where:</br></br>
**jdbcString#jdbc:postgresql://172.16.125.152:5432/example** is the connection string to use, specifying the ip address where Greenplum is stored and database name to use.</br>
**username#gpadmin,passwd#** are the credentials to use to connect to GPDB </br>
**tablename#rws.test** will be the schemaname.tablename to use in our case rws.table1 </br>
**delim#|** will be the delimiter to use by the copy command: in this case pipe</br>
**rejectlimit#10** will be the reject limit option by the copy command if reached all the copy transaction will be rejected

## Do some operation on Geode with json and see operation propagated on Greenplum
```
Do some put to create items:</br>

put --region=test --key='first' --value='{"name": "John", "age": "31", "city": "New York"}'
put --region='test' --key='second' --value='{"name": "John", "age": "31", "city": "New York"}'
put --region='test' --key='third' ---value='{"name": "John", "age": "31", "city": "New York"}'

Do some update:
put --region='test' --key='first' --value='{"name": "John", "age": "31", "city": "New YorkUpdated"}'

Do some delete:
remove --region='test' --key='first'
```

## How to detect copy errors

Use the SELECT * from gp_read_error_log('rws.test1'); to read the errors detected by the copy command and the entries skipped.
