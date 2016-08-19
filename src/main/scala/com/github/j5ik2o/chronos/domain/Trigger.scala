package com.github.j5ik2o.chronos.domain

import java.util.UUID

import com.github.j5ik2o.chronos.cron.{ CronParser, CrondCalender, TimePointInterval, TimePointSpecification }
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.sisioh.baseunits.scala.intervals.{ Limit, Limitless }
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }
import org.sisioh.baseunits.scala.timeutil.Clock

abstract class Trigger(
    val id:      UUID,
    val jobId:   UUID,
    val message: Any
) {
  val nextFireTimePoint: TimePoint

  protected def toStringBuilder(prefix: String): ToStringBuilder = new ToStringBuilder(this, DefaultToStringStyle.ofString(prefix))
    .append("id", id)
    .append("jobId", jobId)
    .append("message", message)
    .append("nextFireTimePoint", nextFireTimePoint.toString("yyyy/MM/dd HH:mm:ss.S"))

  def getNextFireTimePoint: TimePoint = getNextFireTimePoint(Clock.now)

  def getNextFireTimePoint(current: TimePoint): TimePoint

  def recreate: Trigger

}

case class CronTrigger(override val id: UUID, override val jobId: UUID, override val message: Any, cronExpression: String)
    extends Trigger(id, jobId, message) {
  private val expr = new CronParser().parse(cronExpression)
  private val calendar = new CrondCalender(
    Clock.now.asCalendarDateTime().asTimePoint(), Limitless[TimePoint](), TimePointSpecification(expr)
  )

  override lazy val nextFireTimePoint: TimePoint = getNextFireTimePoint

  override def getNextFireTimePoint(current: TimePoint): TimePoint =
    calendar.plusTimePoint(current, 1)

  override def toString: String = toStringBuilder("CronTrigger")
    .append("cronExpression", cronExpression)
    .build()

  override def recreate: CronTrigger = copy()

}

case class DelayTrigger(override val id: UUID, override val jobId: UUID, override val message: Any, delay: Duration)
    extends Trigger(id, jobId, message) {

  private val start = Clock.now

  override lazy val nextFireTimePoint: TimePoint = getNextFireTimePoint

  override def getNextFireTimePoint(current: TimePoint): TimePoint = start + delay

  override def toString: String = toStringBuilder("DelayTrigger")
    .append("delay", delay)
    .build()

  override def recreate: DelayTrigger = copy()

}

case class IntervalTrigger(override val id: UUID, override val jobId: UUID, override val message: Any, delay: Duration, interval: Duration)
    extends Trigger(id, jobId, message) {
  private val timePointInterval = TimePointInterval.everFrom(Limit(Clock.now + delay), interval)

  private val timePointIntervalIterator = timePointInterval.timesIterator

  private var cursor = timePointIntervalIterator.next

  override val nextFireTimePoint: TimePoint = getNextFireTimePoint

  override def getNextFireTimePoint(current: TimePoint): TimePoint = {
    // scalastyle:off
    val result = cursor
    while (cursor.millisecondsFromEpoc <= current.millisecondsFromEpoc) {
      cursor = timePointIntervalIterator.next()
    }
    // scalastyle:on
    result
  }

  override def recreate: IntervalTrigger = copy()

  override def toString: String = toStringBuilder("IntervalTrigger")
    .append("delay", delay)
    .append("interval", interval)
    .build()

}

object Trigger {

  def ofCron(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, cronExpression: String): CronTrigger = CronTrigger(id, jobId, message, cronExpression)

  def ofDelay(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, delay: Duration): DelayTrigger = DelayTrigger(id, jobId, message, delay)

  def ofInterval(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, delay: Duration, interval: Duration): IntervalTrigger = IntervalTrigger(id, jobId, message, delay, interval)

}

