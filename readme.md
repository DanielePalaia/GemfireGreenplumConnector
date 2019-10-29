# Summary



# How to deploy the project in Geode

The script below shows how to start up a Geode system and deploy code to integrate Geode and Kafka.

```
start locator --name=locator
configure pdx --read-serialized=true
start server --name=server1
deploy --dir=/Users/cblack/Downloads/geode-kafka/listener/build/dependancies
y
deploy --dir=/Users/cblack/Downloads/geode-kafka/listener/build/libs
y
create async-event-queue --id=kafka-queue --listener=example.geode.kafka.KafkaAsyncEventListener --listener-param=bootstrap.servers#192.168.127.165:9092 --batch-size=5 --batch-time-interval=1000
create region --name=test --type=PARTITION --async-event-queue-id=kafka-queue


```
