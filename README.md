# Ansible Module: kafka-topic
Ansible Module for managing Apache Kafka topics written in Scala.

Supported features:
- Create/delete topics
- Change topic partitions
- Change topic replication factor

# Prerequisites
Scala and the [sbt Script runner](http://www.scala-sbt.org/0.13/docs/Scripts.html) needs to be installed on the host executing the module.
The sbt Script runner can be created manually or via [conscript](https://github.com/foundweekends/conscript).

# Installation
Place the [module](src/kafka-topic.scala) into the default Ansible library path or in the library directory within a role using it.

# Usage
```
# create a topic
- kafka-topic:
    name: testtopic
    zookeeper: "{{ zookeeper_host }}:{{ zookeeper_port }}"
    partitions: 3
    replication_factor: 3
  delegate_to: 127.0.0.1
  run_once: true

# delete a topic
- kafka-topic:
    name: testtopic
    zookeeper: "{{ zookeeper_host }}:{{ zookeeper_port }}"
    state: absent
  delegate_to: 127.0.0.1
  run_once: true

# increase partitions
- kafka-topic:
    name: testtopic
    zookeeper: "{{ zookeeper_host }}:{{ zookeeper_port }}"
    partitions: 5
    replication_factor: 3
  delegate_to: 127.0.0.1
  run_once: true

# change replication factor and/or decrease partitions
- kafka-topic:
    name: testtopic
    zookeeper: "{{ zookeeper_host }}:{{ zookeeper_port }}"
    partitions: 2
    replication_factor: 2
    force: true
  delegate_to: 127.0.0.1
  run_once: true
```

Using "delegate_to" forces Ansible to execute the module on localhost. It is best to install dependencies on the management host executing Ansible plays.
