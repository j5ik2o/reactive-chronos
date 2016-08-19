package com.github.j5ik2o.chronos.scheduler

import java.util.{ NoSuchElementException, UUID }

import akka.actor.{ Actor, ActorLogging, ActorRef, Cancellable, Props }
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{ Sink, Source }
import com.github.j5ik2o.chronos.domain._
import com.github.j5ik2o.chronos.scheduler.JobControllerProtocol.Message
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.sisioh.baseunits.scala.time.{ TimePoint, Duration => BDuration }
import org.sisioh.baseunits.scala.timeutil.Clock

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.concurrent.duration._
import scala.util.Success

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

  case class CircuitBreakSettings(maxFailures: Int,
                                  callTimeout: BDuration,
                                  resetTimeout: BDuration)

  case class ScheduleJob(id: UUID = UUID.randomUUID(), job: Job, triggers: Seq[Trigger], circuitBreakSettings: Option[CircuitBreakSettings] = None) extends CommandRequest {
    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("job", job)
        .append("triggers", triggers.asJava)
        .append("circuitBreakSettings", circuitBreakSettings)

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
  def name(id: UUID): String = s"scheduler-$id"

  def props(id: UUID = UUID.randomUUID(), tickInterval: FiniteDuration = 1 seconds): Props = Props(new Scheduler(id, tickInterval))
}

class Scheduler(id: UUID, tickInterval: FiniteDuration) extends Actor with ActorLogging {

  import context.dispatcher

  private var jobRepository: JobRepository = JobRepository()

  private var triggerRepository: TriggerRepository = TriggerRepository()

  private var triggerStatusRepository: TriggerStatusRepository = TriggerStatusRepository()

  private var jobStatusRepository: JobStatusRepository = JobStatusRepository()

  private var maybeNextTimePointCancellable: Option[Cancellable] = None

  implicit val materializer = ActorMaterializer()

  val otherwise: Receive = {
    case v@SchedulerProtocol.ScheduleJob(_, job, triggers, circuitBreakSettings) =>
      log.debug("Job Scheduled: job = {}, triggers = {}", job, triggers)
      jobRepository = jobRepository.store(job).get
      triggerRepository = triggerRepository.storeMulti(triggers).get
    case SchedulerProtocol.UnScheduleJob(_, triggerIds) =>
      log.debug("Job UnScheduled: triggerIds = {}", triggerIds)
      triggerRepository = triggerRepository.deleteMulti(triggerIds).get
  }

  private val stopped: Receive = {
    case SchedulerProtocol.Start(_) if maybeNextTimePointCancellable.isEmpty =>
      context.become(startedWithOtherwise)
      maybeNextTimePointCancellable = Some(context.system.scheduler.schedule(0 seconds, tickInterval, self, SchedulerProtocol.Tick))
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

  private def resolveJobById(jobId: UUID): Job = {
    jobRepository.resolveBy(jobId).get
  }

  val jobControllerSinks = mutable.Map.empty[UUID, Sink[Message, ActorRef]]

  private val started: Receive = {
    case SchedulerProtocol.Stop(_) if maybeNextTimePointCancellable.isDefined =>
      context.become(stoppedWithOtherwise)
      maybeNextTimePointCancellable.foreach(_.cancel())
      maybeNextTimePointCancellable = None
    case msg@JobControllerProtocol.Started(_, _, JobControlContext(job, trigger, jobStatus, triggerStatus)) =>
      log.debug("Job Started = {}", msg)
      jobStatusRepository = jobStatusRepository.store(jobStatus).get
      triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get
    case msg@JobControllerProtocol.Finished(_, _, JobControlContext(job, trigger, jobStatus, triggerStatus)) =>
      log.debug("Job Finished = {}", msg)
      jobStatusRepository = jobStatusRepository.store(jobStatus).get
      triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get
    case SchedulerProtocol.Tick =>
      val now = Clock.now
      triggerRepository.iterator.foreach { trigger =>
        val job = resolveJobById(trigger.jobId)
        val maybeJobStatus = resolveJobStatus(job.id)
        //log.debug("trigger = {}", trigger)
        if (!job.startBeforeFinished && maybeJobStatus.exists(_.running)) {
        } else if (trigger.nextFireTimePoint.millisecondsFromEpoc <=  now.millisecondsFromEpoc) {
          log.debug("fire job = {}, trigger = {}", job, trigger)
          val jobStatus = JobStatus(UUID.randomUUID(), job.id, running = true)
          val triggerStatus = TriggerStatus(UUID.randomUUID(), job.id, trigger.id, trigger.nextFireTimePoint, Some(Clock.now))

          triggerRepository = triggerRepository.store(trigger.recreate).get
          jobStatusRepository = jobStatusRepository.store(jobStatus).get
          triggerStatusRepository = triggerStatusRepository.store(triggerStatus).get

          val sink = jobControllerSinks.getOrElseUpdate(job.id,
            Sink.actorSubscriber[JobControllerProtocol.Message](
              JobController.props(UUID.randomUUID(), self, JobControlContext(job, trigger, jobStatus, triggerStatus), 3 seconds)
            )
          )
          Source.single(JobControllerProtocol.Start(UUID.randomUUID())).runWith(sink)
        }
      }
  }


  def startedWithOtherwise = started orElse otherwise

  override def receive: Receive = stoppedWithOtherwise

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    maybeNextTimePointCancellable.foreach(_.cancel())
    super.postStop()
  }
}
