#!/usr/bin/env scalas

/*
  Ansible Module: kafka-topic
  Created by Thomas Hamm on 06.03.17.
  
   WANT_JSON
*/
 
/***         
  scalaVersion := "2.11.8"

  resolvers ++= Seq(
    Resolver.typesafeRepo("releases"),
    "Confluent" at "http://packages.confluent.io/maven/",
    Resolver.url("ansiblemodule-basic", url("https://dl.bintray.com/thmshmm/ansible"))(Resolver.ivyStylePatterns)
  )

  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-json" % "2.5.12",
    "org.apache.kafka" % "kafka_2.11" % "0.10.1.1-cp1",
    "ansiblemodule-basic" %% "ansiblemodule-basic" % "0.1.0"
  )

   trapExit := false 
*/

import ansible.{AnsibleArgument, AnsibleModule}
import kafka.admin.AdminUtils
import kafka.utils.ZkUtils
import org.apache.log4j.{Level, Logger}
import play.api.libs.json._


object Main extends App {
  // disable log output
  Logger.getRootLogger().setLevel(Level.OFF)

  // initialize module arguments
  val am = AnsibleModule(Seq(
    AnsibleArgument("name", true),
    AnsibleArgument("zookeeper", true, Some(JsString("localhost:2181"))),
    AnsibleArgument("state", false, Some(JsString("present"))),
    AnsibleArgument("force", false, Some(JsBoolean(false)))
  ), args(0))

  val topic: String = am.getArg("name").get.as[String]
  val partitions: Int = am.getArg("partitions").get.as[Int]
  val replicationFactor: Int = am.getArg("replication_factor").get.as[Int]
  val forceRecreation: Boolean = am.getArg("force").get.as[Boolean]

  val zkUtils = ZkUtils(am.getArg("zookeeper").get.as[String], 30000, 30000, false)
  val topicExists: Boolean = AdminUtils.topicExists(zkUtils, topic)

  am.getArg("state").get.as[String] match {
    case "present" =>
      if (topicExists) {
        val topicPartitions = zkUtils.getPartitionsForTopics(Seq(topic)).get(topic).get.size
        val replicas = zkUtils.getReplicasForPartition(topic, 0).size

        if (replicas != replicationFactor && !forceRecreation) {
          am.failJson("Changing the replication factor is unsupported (set argument 'force=true' to force topic recreation)")
        } else if (replicas != replicationFactor && forceRecreation) {
          recreateTopic(zkUtils, topic, partitions, replicationFactor)
        }

        if (topicPartitions == partitions) {
          am.exitJson(false)
        } else if (topicPartitions < partitions) {
          AdminUtils.addPartitions(zkUtils, topic, partitions)
          am.exitJson(true)
        } else {
          forceRecreation match {
            case true => recreateTopic(zkUtils, topic, partitions, replicationFactor)
            case false => am.failJson(s"The number of partitions can only be increased (current: $topicPartitions, set argument 'force=true' to force topic recreation)")
          }
        }
      } else {
        AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor)
        am.exitJson(true)
      }
    case "absent" =>
      if (topicExists) {
        AdminUtils.deleteTopic(zkUtils, topic)
        am.exitJson(true)
      } else {
        am.exitJson(false)
      }
    case _ => am.failJson("Invalid value for argument: state (supported: present, absent)")
  }

  def recreateTopic(zkUtils: ZkUtils, topic: String, partitions: Int, replicationFactor: Int, timeout: Int = 10000): Unit = {
    AdminUtils.deleteTopic(zkUtils, topic)

    var topicExists: Boolean = AdminUtils.topicExists(zkUtils, topic)
    var timeout: Int = 10

    // wait for topic being deleted
    while (timeout > 0 || topicExists) {
      Thread.sleep(1000)
      timeout -= 1000
      topicExists = AdminUtils.topicExists(zkUtils, topic)
    }

    topicExists match {
      case true => am.failJson("Waiting for topic deletion timed out, try again")
      case false =>
        AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor)
        am.exitJson(true)
    }
  }
}

Main.main(args)

