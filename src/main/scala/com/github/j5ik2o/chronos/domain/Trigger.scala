package com.github.j5ik2o.chronos.domain

import java.util.UUID

import com.github.j5ik2o.chronos.cron.{ CronParser, CrondCalender, TimePointInterval, TimePointSpecification }
import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder
import org.sisioh.baseunits.scala.intervals.{ Limit, Limitless }
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }
import org.sisioh.baseunits.scala.timeutil.Clock

abstract class Trigger(
                        val id: UUID,
                        val jobId: UUID,
                        val message: Any
                      ) {

  protected def toStringBuilder(prefix: String): ToStringBuilder = new ToStringBuilder(this, DefaultToStringStyle.ofString(prefix))
    .append("id", id)
    .append("jobId", jobId)
    .append("message", message)

  def nextFireTimePoint: TimePoint = nextFireTimePoint(Clock.now)

  def nextFireTimePoint(current: TimePoint): TimePoint

}

object Trigger {

  def ofCron(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, cronExpression: String): Trigger = new Trigger(id, jobId, message) {
    private val crondParser = new CronParser()
    private val expr = crondParser.parse(cronExpression)
    private val calendar = new CrondCalender(Clock.now.asCalendarDateTime().asTimePoint(), Limitless[TimePoint](), TimePointSpecification(expr))

    override def nextFireTimePoint(current: TimePoint): TimePoint = {
      calendar.plusTimePoint(current, 1)
    }

    override def toString: String = toStringBuilder("CronTrigger")
      .append("cronExpression", cronExpression)
      .build()

  }

  def ofDelay(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, delay: Duration): Trigger = new Trigger(id, jobId, message) {
    val start = Clock.now

    override def nextFireTimePoint(current: TimePoint): TimePoint = start + delay

    override def toString: String = toStringBuilder("DelayTrigger")
      .append("delay", delay)
      .build()

  }

  def ofInterval(id: UUID = UUID.randomUUID(), jobId: UUID, message: Any, delay: Duration, interval: Duration): Trigger = new Trigger(id, jobId, message) {

    private val timePointInterval = TimePointInterval.everFrom(Limit(Clock.now + delay), interval)

    private val timePointIntervalIterator = timePointInterval.timesIterator

    private var cursor = timePointIntervalIterator.next

    override def nextFireTimePoint(current: TimePoint): TimePoint = {
      val result = cursor
      while (cursor.millisecondsFromEpoc <= current.millisecondsFromEpoc) {
        cursor = timePointIntervalIterator.next()
      }
      result
    }

    override def toString: String = toStringBuilder("IntervalTrigger")
      .append("delay", delay)
      .append("interval", interval)
      .build()

  }

}

