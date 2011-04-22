package com.github.j5ik2o.chronos.crond

import jp.tricreo.baseunits.scala.time.TimePoint
import java.util.{TimeZone, Calendar}
import org.scalatest.{AbstractSuite, FunSuite}

/**
 * [[CrondParser]]のためのテスト
 */
class CrondParserTest extends FunSuite {

  test("カンマテスト") {
    val p = new CrondParser
    val result = p.parse("1,2,3 1 1 1 *")
    assert(result.successful)
    if (result.successful) {
      assert {
        result.get match {
          case CronExpr(ListExpr(List(ValueExpr(1), ValueExpr(2), ValueExpr(3))), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
          case _ => false
        }
      }

      val evaluator = new CrondEvaluator(TimePoint.at(2011, 1, 1, 1, 1, TimeZone.getDefault))
      assert(result.get.accept(evaluator))
    }
  }

  test("レンジテスト") {
    val p = new CrondParser
    val result = p.parse("1-3 1 1 1 *")
    assert(result.successful)
    if (result.successful) {
      assert {
        result.get match {
          case CronExpr(RangeExpr(ValueExpr(1), ValueExpr(3), NoOp()), ValueExpr(1), ValueExpr(1),
          ValueExpr(1), AnyValueExpr()) => true
          case _ => false
        }
      }
    }
  }


  test("分の解析処理ができること") {
    val p = new CrondParser
    for (m <- 0 to 59) {
      val result = p.parse("%d 1 1 1 *".format(m))
      assert(result.successful)
      if (result.successful) {
        assert {
          result.get match {
            case CronExpr(ValueExpr(m), ValueExpr(1), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }
  }


  test("時の解析処理ができること") {
    val p = new CrondParser
    for (h <- 0 to 23) {
      val result = p.parse("1 %d 1 1 *".format(h))
      assert(result.successful)
      if (result.successful) {
        assert {
          result.get match {
            case CronExpr(ValueExpr(1), ValueExpr(h), ValueExpr(1), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }
  }

  test("日の解析処理ができること") {
    val p = new CrondParser
    for (d <- 1 to 31) {
      val result = p.parse("1 1 %d 1 *".format(d))
      assert(result.successful)
      if (result.successful) {
        assert {
          result.get match {
            case CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(d), ValueExpr(1), AnyValueExpr()) => true
            case _ => false
          }
        }
      }
    }
  }

  test("月の解析処理ができること") {
    val p = new CrondParser
    for (m <- 1 to 12) {
      val result = p.parse("1 1 1 %d *".format(m))
      assert(result.successful)
      if (result.successful) {
        assert {
          result.get match {
            case CronExpr(ValueExpr(1), ValueExpr(1), ValueExpr(1), ValueExpr(m), AnyValueExpr()) => true
            case _ => false
          }
        }

      }
    }
  }

}