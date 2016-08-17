package com.github.j5ik2o.chronos.crond

import java.time.ZoneId

import org.sisioh.baseunits.scala.intervals.{ Interval, Limit, LimitValue, Limitless }
import org.sisioh.baseunits.scala.time._

class TimePointInterval  protected (
                                     startValue: LimitValue[TimePoint],
                                     endValue:   LimitValue[TimePoint],
                                     interval: Duration,
                                     unitForMinute: Boolean
                                   )
  extends Interval[TimePoint](startValue, true, endValue, true) with Serializable {

  lazy val timesIterator: Iterator[TimePoint] = {
    if (!hasLowerLimit) {
      throw new IllegalStateException
    }

    val start = lowerLimit
    val end = upperLimit

    new Iterator[TimePoint] {

      var _next = if (unitForMinute) Limit(start.asCalendarDateTime().asTimePoint()) else start

      override def hasNext = {
        end match {
          case _: Limitless[TimePoint] => true
          case Limit(v)                 => !_next.toValue.isAfter(v)
        }
      }

      override def next: TimePoint = {
        if (!hasNext) {
          throw new NoSuchElementException
        }
        val current = _next
        _next = Limit(_next.toValue.plus(interval))
        current.toValue
      }
    }
  }

  lazy val timesInReverseIterator: Iterator[TimePoint] = {
    if (!hasUpperLimit) {
      throw new IllegalStateException
    }

    val start = upperLimit
    val end = lowerLimit

    new Iterator[TimePoint] {

      var _next = {
        val result = if (unitForMinute) Limit(start.asCalendarDateTime().asTimePoint()) else start
        result
      }

      override def hasNext = {
        end match {
          case _: Limitless[TimePoint] => true
          case Limit(v)                 => !_next.toValue.isBefore(v)
        }
      }

      override def next = {
        if (!hasNext) {
          throw new NoSuchElementException
        }
        val current = _next
        _next = Limit(_next.toValue.minus(interval))
        current.toValue
      }
    }
  }

}

object TimePointInterval {

  def apply(startValue: LimitValue[TimePoint], endValue: LimitValue[TimePoint], interval: Duration, unitForMinute: Boolean): TimePointInterval =
    new TimePointInterval(startValue, endValue, interval, unitForMinute)

  def everFrom(startDate: LimitValue[TimePoint], interval: Duration, unitForMinute: Boolean): TimePointInterval =
    inclusive(startDate, Limitless[TimePoint](), interval, unitForMinute)

  def inclusive(start: LimitValue[TimePoint], end: LimitValue[TimePoint], interval: Duration, unitForMinute: Boolean): TimePointInterval =
    new TimePointInterval(start, end, interval, unitForMinute)

  def inclusive(startCalendarDate: CalendarDate, startTimeOfDay: TimeOfDay,
                endCalendarDate: CalendarDate, endTimeOfDay: TimeOfDay, interval: Duration, unitForMinute: Boolean): TimePointInterval = {
    val startDate = TimePoint.from(startCalendarDate, startTimeOfDay)
    val endDate = TimePoint.from(endCalendarDate, endTimeOfDay)
    new TimePointInterval(Limit(startDate), Limit(endDate), interval, unitForMinute)
  }
}
