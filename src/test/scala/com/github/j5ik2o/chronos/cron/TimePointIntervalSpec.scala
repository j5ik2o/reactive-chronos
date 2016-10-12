package com.github.j5ik2o.chronos.cron

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.intervals.Limit
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }
import org.sisioh.baseunits.scala.timeutil.Clock

class TimePointIntervalSpec extends FunSpec {

  def createTimePointStream(value: TimePoint, duration: Duration, end: TimePoint): Stream[TimePoint] =
    Stream.cons(value, createTimePointStream(value + duration, duration, end)).takeWhile(_ <= end)

  describe("TimePointInterval") {
    it("should be expected iterator values") {
      val start: TimePoint = TimePoint.at(2016, 1, 1, 0, 0, 0, 0)
      val end: TimePoint = start + Duration.minutes(1)
      val duration = Duration.seconds(2)
      val interval = TimePointInterval.inclusive(Limit(start), Limit(end), Duration.seconds(2))
      val list = interval.timesIterator.toList
      val expected = createTimePointStream(start, duration, end).toList
      assert(list == expected)
    }
  }

}
