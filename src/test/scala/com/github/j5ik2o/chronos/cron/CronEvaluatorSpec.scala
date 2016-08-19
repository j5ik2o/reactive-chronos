package com.github.j5ik2o.chronos.cron

import java.util.TimeZone

import org.scalatest.FunSpec
import org.sisioh.baseunits.scala.time.{ TimePoint, ZoneIds }

class CronEvaluatorSpec extends FunSpec {

  describe("") {
    it("単一の分を評価できること") {
      val ast = CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
      val evaluator = new CronEvaluator(TimePoint.at(2011, 1, 1, 1, 1, ZoneIds.Default))
      assert(ast.accept(evaluator))

      val ast2 = CronExpr(ValueExpr(0), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
      val evaluator2 = new CronEvaluator(TimePoint.at(2011, 1, 1, 1, 1, ZoneIds.Default))
      assert(!ast2.accept(evaluator2))
    }

    //    it("複数の分を評価できること") {
    //      val ast = CronExpr(ListExpr(List(ValueExpr(1), ValueExpr(2), ValueExpr(3))), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    //      for (m <- 1 to 3) {
    //        assert(ast.accept(new CronEvaluator(TimePoint.at(2011, 1, 1, 1, m, ZoneIds.Default))))
    //      }
    //    }
    //
    //    it("範囲の分を評価できること") {
    //      val ast = CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), NoOp()), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    //      for (m <- 1 to 3) {
    //        assert(ast.accept(new CronEvaluator(TimePoint.at(2011, 1, 1, 1, m, ZoneIds.Default))))
    //      }
    //    }
    //
    //    it("範囲(分割)の分を評価できること") {
    //      val ast = CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), ValueExpr(2)), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    //      for (m <- 1 to 3 by 2) {
    //        assert(ast.accept(new CronEvaluator(TimePoint.at(2011, 1, 1, 1, m, ZoneIds.Default))))
    //      }
    //    }
  }

}