package com.github.j5ik2o.chronos.crond

import org.scalatest.FunSuite

/**
 * Created by IntelliJ IDEA.
 * User: junichi
 * Date: 11/03/11
 * Time: 2:50
 * To change this template use File | Settings | File Templates.
 */


class CrondParserTest extends FunSuite {

  test("カンマテスト") {
    val p = new CrondParser
    val result = p.parse("* 10,15 * *")
    //println(result)

    if (result.successful) {
      val evaluator = new CrondEvaluator
      val check = result.get.accept(evaluator)
      println(check)
    }


    assert(result.successful)
  }

  //  test("ハイフンテスト") {
  //    val p = new CrondParser
  //    val result = p.parse("1-2 1 1 1")
  //    println(result)
  //    assert(result.successful)
  //  }
  //
  //  test("分の解析処理ができること") {
  //    val p = new CrondParser
  //    for (m <- 0 to 59) {
  //      val result = p.parse("%d 1 1 1".format(m))
  //      println(result)
  //      assert(result.successful)
  //    }
  //  }

  //
  //  test("時の解析処理ができること") {
  //    val p = new CrondParser
  //    for (h <- 0 to 23) {
  //      val result = p.parse("0 %d 1 1".format(h))
  //      println(result)
  //      assert(result.successful)
  //    }
  //  }
  //
  //  test("日の解析処理ができること") {
  //    val p = new CrondParser
  //    for (d <- 1 to 31) {
  //      val result = p.parse("0 0 %d 1".format(d))
  //      println(result)
  //      assert(result.successful)
  //    }
  //  }
  //
  //  test("月の解析処理ができること") {
  //    val p = new CrondParser
  //    for (m <- 1 to 12) {
  //      val result = p.parse("0 0 1 %d".format(m))
  //      println(result)
  //      assert(result.successful)
  //    }
  //  }


}