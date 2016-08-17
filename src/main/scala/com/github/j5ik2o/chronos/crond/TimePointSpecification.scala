package com.github.j5ik2o.chronos.crond

import org.sisioh.baseunits.scala.time.TimePoint
import org.sisioh.baseunits.scala.util.Specification

abstract class TimePointSpecification(expr: Expr) extends Specification[TimePoint] {

  override def isSatisfiedBy(t: TimePoint): Boolean

}

object TimePointSpecification {

  lazy val never: TimePointSpecification = new TimePointSpecification(NoOp()) {
    override def isSatisfiedBy(t: TimePoint): Boolean = false
  }

  def apply(expr: Expr) = new TimePointSpecification(expr) {
    override def isSatisfiedBy(t: TimePoint): Boolean  = {
      val ev = new CronEvaluator(t)
      ev.visit(expr)
    }
  }

}
