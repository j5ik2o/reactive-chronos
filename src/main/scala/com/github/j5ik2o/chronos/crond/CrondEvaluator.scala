package com.github.j5ik2o.chronos.crond

import java.util.{Calendar, Date}

/**
 * Created by IntelliJ IDEA.
 * User: junichi
 * Date: 11/03/28
 * Time: 11:50
 * To change this template use File | Settings | File Templates.
 */

// M H D M
// * * * *

class CrondEvaluator extends ExprVisitor[Boolean] {

  case class ExpressionEvaluator(now: Int, max: Int) extends ExprVisitor[Boolean] {
    def visit(e: Expr) = e match {
      case AnyValueExpr() => true
      case ValueExpr(n) if (now == n) => true
      case ListExpr(list) => list.exists(_.accept(this))
      case RangeExpr(ValueExpr(start), ValueExpr(end), op) => op match {
        case NoOp() if (start <= now && now <= end) => true
        case ValueExpr(per) => (start to end by per).exists(_ == now)
        case _ => false
      }
      case PerExpr(AnyValueExpr(), ValueExpr(p)) => (0 until max by p).exists(_ == now)
      case _ => false
    }
  }

  val calendar = Calendar.getInstance()
  val min = calendar.get(Calendar.MINUTE)
  val minMax = calendar.getActualMaximum(Calendar.MINUTE)
  val hour = calendar.get(Calendar.HOUR_OF_DAY)
  val hourMax = calendar.getActualMaximum(Calendar.HOUR_OF_DAY)
  val day = calendar.get(Calendar.DATE)
  val dayMax = calendar.getActualMaximum(Calendar.DATE)
  val month = calendar.get(Calendar.MONTH) + 1
  val monthMax = calendar.getActualMaximum(Calendar.MONTH)
  val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
  val dayOfWeekMax = calendar.getActualMaximum(Calendar.DAY_OF_WEEK)

  def visit(e: Expr) = e match {
    case CronExpr(mins, hours, days, months, dayOfWeeks) => {

      val m = mins.accept(ExpressionEvaluator(min, minMax))
      val h = hours.accept(ExpressionEvaluator(hour, hourMax))
      val d = days.accept(ExpressionEvaluator(day, dayMax))
      val M = months.accept(ExpressionEvaluator(month, monthMax))
      val dw = dayOfWeeks.accept(ExpressionEvaluator(dayOfWeek, dayOfWeekMax))

      m && h && d && M && dw
    }
    case _ => false
  }

}