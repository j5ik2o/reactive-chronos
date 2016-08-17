package com.github.j5ik2o.chronos.scheduler

import akka.actor._
import akka.event.Logging

object SchedulerExtension extends ExtensionKey[SchedulerExtension]

class SchedulerExtension(system: ExtendedActorSystem) extends Extension {

  //private val log = Logging(system, this)

  private var started = false

  def isStarted = started

  var schedulerRef: ActorRef = _

  def start(): Boolean = isStarted match {
    case true =>
      //log.warning("Cannot start scheduler, already started.")
      false
    case false =>
      started = true
      true
  }

}
