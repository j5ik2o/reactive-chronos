package com.github.j5ik2o.chronos.crond

import org.scalatest.FunSuite
import jp.tricreo.baseunits.scala.time.TimePoint
import java.util.TimeZone

/**
 * Created by IntelliJ IDEA.
 * User: junichi
 * Date: 11/03/31
 * Time: 19:25
 * To change this template use File | Settings | File Templates.
 */

class CrondEvaluatorTest extends FunSuite {

  test("単一の分を評価できること") {
    val ast = CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    val evaluator = new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, 1, TimeZone.getDefault))
    assert(ast.accept(evaluator))

    val ast2 = CronExpr(ValueExpr(0), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    val evaluator2 = new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, 1, TimeZone.getDefault))
    assert(ast2.accept(evaluator2) == false)
  }

  test("複数の分を評価できること") {
    val ast = CronExpr(ListExpr(List(ValueExpr(1), ValueExpr(2), ValueExpr(3))), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    for (m <- 1 to 3) {
      assert(ast.accept(new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, m, TimeZone.getDefault))))
    }
  }

  test("範囲の分を評価できること") {
    val ast = CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), NoOp()), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    for (m <- 1 to 3) {
      assert(ast.accept(new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, m, TimeZone.getDefault))))
    }
  }

  test("範囲(分割)の分を評価できること") {
    val ast = CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), ValueExpr(2)), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr())
    for (m <- 1 to 3 by 2) {
      assert(ast.accept(new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, m, TimeZone.getDefault))))
    }
  }

}