package com.github.j5ik2o.chronos.scheduler

import java.util.UUID

import akka.actor.{ Actor, ActorLogging, ActorRef, ActorSystem, Props }
import akka.testkit.{ TestKit, TestProbe }
import com.github.j5ik2o.chronos.domain.{ Job, Trigger }
import com.github.j5ik2o.chronos.scheduler.JobSchedulerProtocol.{ Start, Stop, UnScheduleJob }
import org.scalatest.{ BeforeAndAfterAll, FunSpecLike }
import org.sisioh.baseunits.scala.time.Duration

import scala.concurrent.Await
import scala.concurrent.duration.{ Duration => SDuration, _ }
import scala.util.Success

class JobSchedulerSpec extends TestKit(ActorSystem("SchedulerSpec")) with FunSpecLike with BeforeAndAfterAll {

  val schedulerId = UUID.randomUUID()
  val scheduler = system.actorOf(JobScheduler.props(schedulerId), name = JobScheduler.name(schedulerId))

  private def jobProps(probeRef: ActorRef) = Props(new Actor with ActorLogging {
    override def receive: Receive = {
      case JobProtocol.Start(_, message) =>
        log.debug(message.toString)
        val response = JobProtocol.Finish(UUID.randomUUID(), Success(()))
        sender() ! response
        probeRef ! message.toString
      // context.stop(self)
    }
  })

  private def longTimeJobProps(probeRef: ActorRef) = Props(new Actor with ActorLogging {
    override def receive: Receive = {
      case JobProtocol.Start(_, message) =>
        log.debug(message.toString)
        Thread.sleep(5 * 1000)
        val response = JobProtocol.Finish(UUID.randomUUID(), Success(()))
        sender() ! response
        probeRef ! message.toString
      // context.stop(self)
    }
  })

  private def errorJobProps(probeRef: ActorRef) = Props(new Actor with ActorLogging {
    override def receive: Receive = {
      case message =>
        sys.error("test")
    }
  })

  describe("Scheduler") {
    it("long run") {
      val senderProbe = TestProbe()
      implicit val self = senderProbe.ref
      val delay = 2
      scheduler ! Start()
      val job = Job(UUID.randomUUID(), longTimeJobProps(self))
      val triggers = Seq(
        Trigger.ofInterval(UUID.randomUUID(), job.id, "test", Duration.seconds(delay), Duration.seconds(delay))
      )
      scheduler ! JobSchedulerProtocol.ScheduleJob(UUID.randomUUID(), job, triggers)
      senderProbe.receiveN(2, 30 seconds)
      scheduler ! UnScheduleJob(UUID.randomUUID(), triggers.map(_.id))
      scheduler ! Stop()
    }
    it("error run") {
      val senderProbe = TestProbe()
      implicit val self = senderProbe.ref
      val schedulerId = UUID.randomUUID()
      val scheduler = system.actorOf(JobScheduler.props(schedulerId), name = JobScheduler.name(schedulerId))
      val delay = 3
      scheduler ! Start()
      val job = Job(UUID.randomUUID(), errorJobProps(self))
      val triggers = Seq(
        Trigger.ofDelay(UUID.randomUUID, job.id, "test", Duration.seconds(delay))
      )
      scheduler ! JobSchedulerProtocol.ScheduleJob(UUID.randomUUID(), job, triggers)
      senderProbe.expectNoMsg()
      scheduler ! UnScheduleJob(UUID.randomUUID(), triggers.map(_.id))
      scheduler ! Stop()
    }
    it("delay run") {
      val senderProbe = TestProbe()
      implicit val self = senderProbe.ref
      val delay = 3
      scheduler ! Start()
      val job = Job(UUID.randomUUID(), jobProps(self))
      val triggers = Seq(
        Trigger.ofDelay(UUID.randomUUID, job.id, "test", Duration.seconds(delay))
      )
      scheduler ! JobSchedulerProtocol.ScheduleJob(UUID.randomUUID(), job, triggers)
      senderProbe.expectMsg((delay * 2) seconds, "test")
      scheduler ! UnScheduleJob(UUID.randomUUID(), triggers.map(_.id))
      scheduler ! Stop()
    }
    it("interval run") {
      val senderProbe = TestProbe()
      implicit val self = senderProbe.ref
      val delay = 3
      scheduler ! Start()
      val job = Job(UUID.randomUUID(), jobProps(self))
      val triggers = Seq(
        Trigger.ofInterval(UUID.randomUUID(), job.id, "test", Duration.seconds(delay), Duration.seconds(delay))
      )
      scheduler ! JobSchedulerProtocol.ScheduleJob(UUID.randomUUID(), job, triggers)
      senderProbe.expectMsg((delay * 2) seconds, "test")
      senderProbe.expectMsg((delay * 2) seconds, "test")
      scheduler ! UnScheduleJob(UUID.randomUUID(), triggers.map(_.id))
      scheduler ! Stop()
    }
    it("cron run") {
      val senderProbe = TestProbe()
      implicit val self = senderProbe.ref
      scheduler ! Start()
      val job = Job(UUID.randomUUID(), jobProps(self))
      val triggers = Seq(
        Trigger.ofCron(UUID.randomUUID(), job.id, "test", s"*/1 * * * *")
      )
      scheduler ! JobSchedulerProtocol.ScheduleJob(UUID.randomUUID(), job, triggers)
      senderProbe.expectMsg(2 minutes, "test")
      senderProbe.expectMsg(2 minutes, "test")
      scheduler ! JobSchedulerProtocol.UnScheduleJob(UUID.randomUUID(), triggers.map(_.id))
      scheduler ! Stop()
    }
  }

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), SDuration.Inf)
    super.afterAll()
  }

}
