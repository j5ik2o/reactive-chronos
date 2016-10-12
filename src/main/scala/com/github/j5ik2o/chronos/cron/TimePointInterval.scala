package com.github.j5ik2o.chronos.cron

import org.sisioh.baseunits.scala.intervals.{ Interval, Limit, LimitValue, Limitless }
import org.sisioh.baseunits.scala.time._

class TimePointInterval protected (
  startValue: LimitValue[TimePoint],
  endValue:   LimitValue[TimePoint],
  interval:   Duration
)
    extends Interval[TimePoint](startValue, true, endValue, true) with Serializable {

  def createStream(startValue: LimitValue[TimePoint]): Stream[TimePoint]  ={
    require(hasLowerLimit)
    Stream.cons(startValue.toValue, createStream(startValue.toValue + interval)).takeWhile{ v =>
      endValue match {
        case _: Limitless[TimePoint] => true
        case Limit(end)                => !v.isAfter(end)
      }
    }
  }

//  lazy val timesIterator: Iterator[TimePoint] =
//    createStream(startValue).toIterator


  lazy val timesIterator: Iterator[TimePoint] = {
    if (!hasLowerLimit) {
      throw new IllegalStateException
    }

    val start = lowerLimit
    val end = upperLimit

    new Iterator[TimePoint] {

      var _next = start

      override def hasNext = {
        end match {
          case _: Limitless[TimePoint] => true
          case Limit(v)                => !_next.toValue.isAfter(v)
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

      var _next = start

      override def hasNext = {
        end match {
          case _: Limitless[TimePoint] => true
          case Limit(v)                => !_next.toValue.isBefore(v)
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

  def apply(startValue: LimitValue[TimePoint], endValue: LimitValue[TimePoint], interval: Duration): TimePointInterval =
    new TimePointInterval(startValue, endValue, interval)

  def everFrom(startDate: LimitValue[TimePoint], interval: Duration): TimePointInterval =
    inclusive(startDate, Limitless[TimePoint](), interval)

  def inclusive(start: LimitValue[TimePoint], end: LimitValue[TimePoint], interval: Duration): TimePointInterval =
    new TimePointInterval(start, end, interval)

  def inclusive(startCalendarDate: CalendarDate, startTimeOfDay: TimeOfDay,
                endCalendarDate: CalendarDate, endTimeOfDay: TimeOfDay, interval: Duration): TimePointInterval = {
    val startDate = TimePoint.from(startCalendarDate, startTimeOfDay)
    val endDate = TimePoint.from(endCalendarDate, endTimeOfDay)
    new TimePointInterval(Limit(startDate), Limit(endDate), interval)
  }
}
