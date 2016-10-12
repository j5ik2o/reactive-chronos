package com.github.j5ik2o.chronos.cron

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.intervals.Limit
import org.sisioh.baseunits.scala.time.{ Duration, TimePoint }

class CronCalenderSpec extends FunSpec {

  def createTimePointStream(value: TimePoint, duration: Duration, end: TimePoint): Stream[TimePoint] =
    Stream.cons(value, createTimePointStream(value + duration, duration, end)).takeWhile(_ <= end)

  private val crondParser = new CronParser()

  describe("CondCalendar") {
    it("seq") {
      val cronExpression = "*/2 * * * *"
      val expr = crondParser.parse(cronExpression)
      val start = TimePoint.at(2016, 1, 1, 0, 0, 0, 0)
      val end = start + Duration.minutes(20)
      val cal = new CrondCalender(Limit(start), Limit(end), TimePointSpecification(expr))
      val list = cal.cronTimePointsIterator.toList
      val expected = createTimePointStream(start, Duration.minutes(2), end).toList
      assert(list == expected)
    }
    it("plus") {
      val cronExpression = "*/2 * * * *"
      val expr = crondParser.parse(cronExpression)
      val start = TimePoint.at(2016, 1, 1, 0, 0, 0, 0)
      val end = start + Duration.minutes(20)
      val cal = new CrondCalender(Limit(start), Limit(end), TimePointSpecification(expr))
      val result = for {min <- 0 to 20 by 2} yield {
        val now = TimePoint.at(2016, 1, 1, 0, min, 0, 0)
        val tp = cal.plusTimePoint(now, 1)
        (now, tp)
      }
      val (expected, actual) = result.unzip
      assert(actual == expected)
    }
  }

}
