package com.github.j5ik2o.chronos.crond

import org.sisioh.baseunits.scala.intervals.LimitValue
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }

class CrondCalender(startTimePoint: LimitValue[TimePoint], endTimePoint: LimitValue[TimePoint], timePointSpecification: TimePointSpecification = TimePointSpecification.never) {

  def iterator = TimePointInterval.inclusive(startTimePoint, endTimePoint, Duration.minutes(1), unitForMinute = true).timesIterator

  def plusTimePoint(currentTimePoint: TimePoint, numberOfMinutes: Int): TimePoint = {
    nextNumberOfTimePoints(currentTimePoint, numberOfMinutes)
  }

  private def nextNumberOfTimePoints(currentTimePoint: TimePoint,
                                     numberOfDays: Int
                                    ): TimePoint = {
    require(numberOfDays >= 0)
    val cronTimePoints = cronTimePointsIterator.filterNot { tp =>
      tp.millisecondsFromEpoc < currentTimePoint.millisecondsFromEpoc
    }
    (0 to numberOfDays).foldLeft[Option[TimePoint]](None) { (_, _) =>
      Some(cronTimePoints.next())
    }.get
  }

  private def isTimePoint(value: TimePoint): Boolean = timePointSpecification.isSatisfiedBy(value)

  def cronTimePointsIterator: Iterator[TimePoint] = {
    val _iterator = iterator
    new Iterator[TimePoint] {

      var lookAhead = nextCronTimePoint

      override def hasNext = lookAhead.isDefined

      override def next: TimePoint = {
        if (!hasNext) {
          throw new NoSuchElementException
        }
        val _next = lookAhead
        lookAhead = nextCronTimePoint
        _next.get
      }

      private def nextCronTimePoint: Option[TimePoint] = {
        var result: Option[TimePoint] = None
        do {
          result = if (_iterator.hasNext) Some(_iterator.next())
          else None
        } while (!(result.isEmpty || isTimePoint(result.get)))
        result
      }
    }
  }

}
