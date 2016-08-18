package com.github.j5ik2o.chronos.cron

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.intervals.Limit
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }
import org.sisioh.baseunits.scala.timeutil.Clock

class TimePointIntervalSpec extends FunSpec {
  describe("TimePointInterval") {
    it("seq") {
      val start: TimePoint = Clock.now
      val end: TimePoint = start + Duration.minutes(1)
      val interval = TimePointInterval.inclusive(Limit(start), Limit(end), Duration.seconds(2))
      interval.timesIterator.toList.foreach(println)
    }
  }
}
