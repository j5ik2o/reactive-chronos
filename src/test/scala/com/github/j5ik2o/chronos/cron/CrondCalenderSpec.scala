package com.github.j5ik2o.chronos.cron

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.intervals.Limit
import org.sisioh.baseunits.scala.time.Duration
import org.sisioh.baseunits.scala.timeutil.Clock

class CrondCalenderSpec extends FunSpec {
  private val crondParser = new CronParser()
  describe("CondCalendar") {
    it("seq") {
      val crondExpression = "*/2 * * * *"
      val expr = crondParser.parse(crondExpression)
      val start = Clock.now.asCalendarDateTime().asTimePoint()
      val end = start + Duration.minutes(20)
      val cal = new CrondCalender(Limit(start), Limit(end), TimePointSpecification(expr))

      val itr = cal.cronTimePointsIterator

      itr.foreach(println)
    }
    it("plus") {
      val crondExpression = "*/2 * * * *"
      val expr = crondParser.parse(crondExpression)
      val start = Clock.now.asCalendarDateTime().asTimePoint()
      val end = start + Duration.minutes(20)
      println(start, end)
      val cal = new CrondCalender(Limit(start), Limit(end), TimePointSpecification(expr))

      for { _ <- 1 to 10 } {
        val tp = cal.plusTimePoint(Clock.now, 1)
        println(tp)
      }

    }
  }

}
