package com.github.j5ik2o.chronos.cron

import java.util.TimeZone

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.time.{ TimePoint, ZoneIds }

/**
 * [[CronParser]]のためのテスト
 */
class CronParserSpec extends FunSpec {

  describe("CronParser") {
    it("カンマテスト") {
      val p = new CronParser
      val result = p.parse("1,2,3 1 1 1 *")
      assert {
        result match {
          case CronExpr(ListExpr(List(ValueExpr(1), ValueExpr(2), ValueExpr(3))), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
          case _ => false
        }
      }

      val evaluator = new CronEvaluator(TimePoint.at(2011, 1, 1, 1, 1, ZoneIds.Default))
      assert(result.accept(evaluator))
    }

    it("レンジテスト") {
      val p = new CronParser
      val result = p.parse("1-3 1 1 1 *")
      assert {
        result match {
          case CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), NoOp()), ValueExpr(1), ValueExpr(1),
            ValueExpr(1), AnyValueExpr()) => true
          case _ => false
        }
      }
    }

    it("分の解析処理ができること") {
      val p = new CronParser
      for (m <- 0 to 59) {
        val result = p.parse("%d 1 1 1 *".format(m))
        assert {
          result match {
            case CronExpr(ValueExpr(m), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }

    it("時の解析処理ができること") {
      val p = new CronParser
      for (h <- 0 to 23) {
        val result = p.parse("1 %d 1 1 *".format(h))
        assert {
          result match {
            case CronExpr(ValueExpr(1), ValueExpr(h), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }

    it("日の解析処理ができること") {
      val p = new CronParser
      for (d <- 1 to 31) {
        val result = p.parse("1 1 %d 1 *".format(d))
        assert {
          result match {
            case CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(d), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }

    it("月の解析処理ができること") {
      val p = new CronParser
      for (m <- 1 to 12) {
        val result = p.parse("1 1 1 %d *".format(m))
        assert {
          result match {
            case CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(1), ValueExpr(m), AnyValueExpr()) => true
            case _ => false
          }
        }

      }
    }
  }

}