package com.github.j5ik2o.chronos.scheduler

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy }
import com.github.j5ik2o.chronos.domain.{ Job, Trigger }
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.sisioh.baseunits.scala.timeutil.Clock

import scala.concurrent.duration.Duration
import scala.util.Failure


object JobControllerProtocol {

  trait Message {
    protected val toStringBuilder: ToStringBuilder

    override def toString: String = toStringBuilder.build()
  }

  sealed trait CommandRequest extends Message {
    val id: UUID

    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("id", id)
  }

  sealed trait CommandResponse extends Message {
    val id: UUID
    val requestId: Option[UUID]

    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("id", id)
        .append("requestId", requestId)
  }

  case class Start(id: UUID = UUID.randomUUID()) extends CommandRequest

  case class Started(id: UUID, requestId: Option[UUID], job: Job, trigger: Trigger, jobControlContext: JobControlContext) extends CommandResponse {
    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("job", job)
        .append("trigger", trigger)
        .append("jobControllerContext", jobControlContext)
  }

  case class Finished(id: UUID, requestId: Option[UUID], job: Job, trigger: Trigger, jobControlContext: JobControlContext) extends CommandResponse {
    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("job", job)
        .append("trigger", trigger)
        .append("jobControllerContext", jobControlContext)
  }

}


object JobController {
  def name(id: UUID): String = s"job-controller-$id"

  def props(id: UUID, schedulerRef: ActorRef, job: Job, trigger: Trigger, jobControlContext: JobControlContext, timeout: Duration): Props =
    Props(new JobController(id, schedulerRef, job, trigger, jobControlContext, timeout))
}

class JobController(id: UUID, schedulerRef: ActorRef, job: Job, trigger: Trigger, jobControlContext: JobControlContext, timeout: Duration)
  extends Actor with ActorLogging {

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case ex: Exception =>
      schedulerRef ! JobControllerProtocol.Finished(
        UUID.randomUUID(),
        None,
        job,
        trigger,
        JobControlContext(
          jobControlContext.jobStatus.copy(running = false),
          jobControlContext.triggerStatus.copy(finishedAt = Some(Clock.now), result = Some(Failure(ex)))
        )
      )
      Stop
  }

  def started(sender: ActorRef, requestId: UUID): Receive = {
    case JobProtocol.Finish(_, result) =>
      sender ! JobControllerProtocol.Finished(
        UUID.randomUUID(),
        Some(requestId),
        job,
        trigger,
        JobControlContext(
          jobControlContext.jobStatus.copy(running = false),
          jobControlContext.triggerStatus.copy(finishedAt = Some(Clock.now), result = Some(result))
        )
      )

  }


  override def receive: Receive = {
    case JobControllerProtocol.Start(id) =>
      log.debug("Job Start = {}", job)
      context.become(started(sender(), id))
      context.child(job.name).fold(context.actorOf(job.jobProps, name = job.name) ! JobProtocol.Start(UUID.randomUUID(), trigger.message)) {
        _ ! JobProtocol.Start(UUID.randomUUID(), trigger.message)
      }
      sender() ! JobControllerProtocol.Started(
        UUID.randomUUID(),
        Some(id),
        job,
        trigger,
        JobControlContext(
          jobControlContext.jobStatus,
          jobControlContext.triggerStatus.copy(startedAt = Some(Clock.now))
        )
      )
      log.debug("Job Started = {}", job)
  }
}
