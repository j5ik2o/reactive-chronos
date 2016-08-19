package com.github.j5ik2o.chronos.cron

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.intervals.Limit
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }
import org.sisioh.baseunits.scala.timeutil.Clock

class TimePointIntervalSpec extends FunSpec {
  describe("TimePointInterval") {
    it("stream") {
      val start: TimePoint = TimePoint.at(2016, 1, 1, 0, 0, 0, 0)
      val end: TimePoint = start + Duration.minutes(1)
      val interval = TimePointInterval.inclusive(Limit(start), Limit(end), Duration.seconds(2))
      val list = interval.timesIterator.toList
      assert(list.head == start)
      assert(list.last == end)
    }
  }
}
