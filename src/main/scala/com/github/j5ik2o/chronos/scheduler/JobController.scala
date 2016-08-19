package com.github.j5ik2o.chronos.scheduler

import java.util.UUID

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy }
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ ActorSubscriber, MaxInFlightRequestStrategy, RequestStrategy }
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

  case class Started(id: UUID, requestId: Option[UUID], jobControlContext: JobControlContext) extends CommandResponse {
    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("jobControllerContext", jobControlContext)
  }

  case class Finished(id: UUID, requestId: Option[UUID], jobControlContext: JobControlContext) extends CommandResponse {
    override protected val toStringBuilder: ToStringBuilder =
      new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))
        .append("jobControllerContext", jobControlContext)
  }

}


object JobController {
  def name(id: UUID): String = s"job-controller-$id"

  def props(id: UUID, schedulerRef: ActorRef, jobControlContext: JobControlContext, timeout: Duration): Props =
    Props(new JobController(id, schedulerRef, jobControlContext, timeout))
}

class JobController(id: UUID,
                    schedulerRef: ActorRef,
                    jobControlContext: JobControlContext,
                    timeout: Duration)
  extends ActorSubscriber with ActorLogging {

  val MaxBufferSize = 100
  var buffer = Map.empty[UUID, JobControllerProtocol.Message]

  override protected def requestStrategy: RequestStrategy = new MaxInFlightRequestStrategy(MaxBufferSize) {
    override def inFlightInternally: Int = buffer.size
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case ex: Exception =>
      schedulerRef ! JobControllerProtocol.Finished(
        UUID.randomUUID(),
        None,
        JobControlContext(
          jobControlContext.job,
          jobControlContext.trigger,
          jobControlContext.jobStatus.copy(running = false),
          jobControlContext.triggerStatus.copy(finishedAt = Some(Clock.now), result = Some(Failure(ex)))
        )
      )
      Stop
  }

  def started(requestId: UUID): Receive = {
    case JobProtocol.Finish(_, result) =>
      log.debug("Job Finish = {}", jobControlContext.job)
      schedulerRef ! JobControllerProtocol.Finished(
        UUID.randomUUID(),
        Some(requestId),
        JobControlContext(
          jobControlContext.job,
          jobControlContext.trigger,
          jobControlContext.jobStatus.copy(running = false),
          jobControlContext.triggerStatus.copy(finishedAt = Some(Clock.now), result = Some(result))
        )
      )
      buffer -= requestId
      context.unbecome()
  }


  override def receive: Receive = {
    case OnNext(msg@JobControllerProtocol.Start(id)) =>
      log.debug("Job Start = {}", jobControlContext.job)
      context.become(started(id))
      context.child(jobControlContext.job.name).fold(context.actorOf(jobControlContext.job.jobProps, name = jobControlContext.job.name) ! JobProtocol.Start(UUID.randomUUID(), jobControlContext.trigger.message)) {
        _ ! JobProtocol.Start(UUID.randomUUID(), jobControlContext.trigger.message)
      }
      buffer += (id -> msg)
      schedulerRef ! JobControllerProtocol.Started(
        UUID.randomUUID(),
        Some(id),
        JobControlContext(
          jobControlContext.job,
          jobControlContext.trigger,
          jobControlContext.jobStatus,
          jobControlContext.triggerStatus.copy(startedAt = Some(Clock.now))
        )
      )
      log.debug("Job Started = {}", jobControlContext.job)
  }

}
