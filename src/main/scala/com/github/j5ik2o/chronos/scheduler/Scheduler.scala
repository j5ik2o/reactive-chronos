package com.github.j5ik2o.chronos.scheduler

import java.util.{ NoSuchElementException, UUID }

import akka.actor.{ Actor, ActorLogging, Cancellable, Props }
import com.github.j5ik2o.chronos.domain._
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.sisioh.baseunits.scala.time.{ TimePoint, Duration => BDuration }
import org.sisioh.baseunits.scala.timeutil.Clock

import scala.concurrent.duration._
import scala.util.Success
import scala.collection.JavaConverters._

object SchedulerProtocol {

  trait Message {
    protected def toStringBuilder = new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))

    override def toString: String = toStringBuilder.build()
  }

  sealed trait CommandRequest extends Message {
    val id: UUID

    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("id", id)
  }

  sealed trait CommandResponse extends Message {
    val id: UUID
    val requestId: Option[UUID]

    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("id", id)
        .append("requestId", requestId)
  }

  case class ScheduleJob(id: UUID = UUID.randomUUID(), job: Job, triggers: Seq[Trigger]) extends CommandRequest {
    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("job", job)
        .append("triggers", triggers.asJava)

  }

  case class UnScheduleJob(id: UUID = UUID.randomUUID(), triggerIds: Seq[UUID]) extends CommandRequest {
    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("triggerIds", triggerIds.asJava)
  }

  case class Start(id: UUID = UUID.randomUUID()) extends CommandRequest

  case class Stop(id: UUID = UUID.randomUUID()) extends CommandRequest

  case object Tick


}


object Scheduler {
  def name(id: UUID) = s"scheduler-$id"

  def props(id: UUID = UUID.randomUUID()): Props = Props(new Scheduler(id))
}

class Scheduler(id: UUID) extends Actor with ActorLogging {

  import context.dispatcher

  var jobRepository: JobRepository = JobRepository()

  var triggerRepository: TriggerRepository = TriggerRepository()

  var triggerStatusRepository: TriggerStatusRepository = TriggerStatusRepository()

  var jobStatusRepository: JobStatusRepository = JobStatusRepository()

  var cancellable: Option[Cancellable] = None

  val otherwise: Receive = {
    case v@SchedulerProtocol.ScheduleJob(_, job, triggers) =>
      log.debug("Job Scheduled: job = {}, triggers = {}", job, triggers)
      jobRepository = jobRepository.store(job).get
      triggerRepository = triggerRepository.storeMulti(triggers).get
    case SchedulerProtocol.UnScheduleJob(_, triggerIds) =>
      log.debug("Job UnScheduled: triggerIds = {}", triggerIds)
      triggerRepository = triggerRepository.deleteMulti(triggerIds).get
  }

  val stopped: Receive = {
    case SchedulerProtocol.Start(_) if cancellable.isEmpty =>
      context.become(startedWithOtherwise)
      cancellable = Some(context.system.scheduler.schedule(0 seconds, 500 milliseconds, self, SchedulerProtocol.Tick))
  }

  private def stoppedWithOtherwise: PartialFunction[Any, Unit] = stopped orElse otherwise

  private def resolveJobStatus(jobId: UUID): Option[JobStatus] = jobStatusRepository.resolveByJobId(jobId).map(Some(_)).recoverWith {
    case ex: NoSuchElementException => Success(None)
  }.get

  private def resolveTriggerStatus(trigger: Trigger, nextFireTimePoint: TimePoint): Option[TriggerStatus] = {
    triggerStatusRepository.resolveByTriggerIdAndComputedFireAt(trigger.id, nextFireTimePoint).map(Some(_)).recoverWith {
      case ex: NoSuchElementException => Success(None)
    }.get
  }


  val started: Receive = {
    case SchedulerProtocol.Stop(_) if cancellable.isDefined =>
      context.become(stoppedWithOtherwise)
      cancellable.foreach(_.cancel())
      cancellable = None
    case SchedulerProtocol.Tick =>
      val now = Clock.now
      triggerRepository.iterator.foreach { (trigger: Trigger) =>
        val nextFireTimePoint = trigger.nextFireTimePoint(now)
        val job = jobRepository.resolveBy(trigger.jobId).get
        val maybeJobStatus = resolveJobStatus(job.id)
        val maybeTriggerStatus = resolveTriggerStatus(trigger, nextFireTimePoint)
        // log.debug("trigger.id = {}, next = {}, jobStatus = {}", trigger.id, nextFireTimePoint.toString("yyyy/MM/dd HH:mm:ss.S"), maybeTriggerStatus)
        // log.debug("next = {}, jobStatus = {}", nextFireTimePoint.toString("yyyy/MM/dd HH:mm:ss.S"), maybeTriggerStatus)
        if (!job.startBeforeFinished && maybeJobStatus.exists(_.running)) {
          //log.warning("The job is running.")
        } else if (maybeTriggerStatus.nonEmpty) {
          //log.warning("The job was executed.")
        } else if (now.millisecondsFromEpoc <= nextFireTimePoint.millisecondsFromEpoc) {
          log.debug("fire trigger = {}", trigger)
          val jobStatus = JobStatus(UUID.randomUUID(), job.id, running = true)
          jobStatusRepository = jobStatusRepository.store(jobStatus).get
          val triggerStatus = TriggerStatus(UUID.randomUUID(), job.id, trigger.id, nextFireTimePoint)
          triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get
          val jobControllerId = UUID.randomUUID()
          val jobControllerRef = context.actorOf(JobController.props(jobControllerId, self, job, trigger, JobControlContext(jobStatus, triggerStatus), 3 seconds), name = JobController.name(jobControllerId))
          jobControllerRef ! JobControllerProtocol.Start(UUID.randomUUID())
        }
      }
    case msg@JobControllerProtocol.Started(_, _, job, trigger, JobControlContext(jobStatus, triggerStatus)) =>
      log.debug("Job Started = {}", msg)
      jobStatusRepository = jobStatusRepository.store(jobStatus).get
      triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get
    case msg@JobControllerProtocol.Finished(_, _, job, trigger, JobControlContext(jobStatus, triggerStatus)) =>
      log.debug("Job Finished = {}", msg)
      jobStatusRepository = jobStatusRepository.store(jobStatus).get
      triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get
  }


  def startedWithOtherwise = started orElse otherwise

  override def receive: Receive = stoppedWithOtherwise

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    cancellable.foreach(_.cancel())
    super.postStop()
  }
}
