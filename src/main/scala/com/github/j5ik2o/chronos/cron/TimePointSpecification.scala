package com.github.j5ik2o.chronos.cron

import org.sisioh.baseunits.scala.time.TimePoint
import org.sisioh.baseunits.scala.util.Specification

abstract class TimePointSpecification(expr: Expr) extends Specification[TimePoint]

object TimePointSpecification {

  lazy val never: TimePointSpecification = new TimePointSpecification(NoOp()) {
    override def isSatisfiedBy(t: TimePoint): Boolean = false
  }

  def apply(expr: Expr): TimePointSpecification = new TimePointSpecification(expr) {
    override def isSatisfiedBy(t: TimePoint): Boolean = new CronEvaluator(t).visit(expr)
  }

}
