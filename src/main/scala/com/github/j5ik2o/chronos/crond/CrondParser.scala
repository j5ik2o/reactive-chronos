package com.github.j5ik2o.chronos.crond

import util.parsing.combinator._


// 式を訪問するビジター
trait ExprVisitor[T] {
  def visit(e: Expr): T
}

// 式を表すトレイト
trait Expr {
  def accept[T](visitor: ExprVisitor[T]): T = {
    visitor.visit(this)
  }
}


case class NoOp extends Expr

case class ValueExpr(digit: Int) extends Expr

case class AnyValueExpr extends Expr

case class PerExpr(digit: Expr, option: Expr) extends Expr

case class RangeExpr(from: Expr, to: Expr, perOtion: Expr) extends Expr

case class ListExpr(exprs: List[Expr]) extends Expr

case class CronExpr(mins: Expr, hours: Expr, days: Expr, months: Expr) extends Expr

class CrondParser extends RegexParsers {

  def parse(source: String) = parseAll(instruction, source)

  def instruction: Parser[CronExpr] = digitInstruction(minDigit) ~ digitInstruction(hourDigit) ~ digitInstruction(dayDigit) ~ digitInstruction(monthDigit) ^^ {
    case mins ~ hours ~ days ~ months => CronExpr(mins, hours, days, months)
  }

  def digitInstruction(digit: => Parser[Expr]) = asterisk ||| list(digit) ||| range(digit) ||| asteriskPer(digit)


  def list(digit: => Parser[Expr]) = repsep(digit, ",") ^^ {
    case x :: Nil => x
    case l => ListExpr(l)
  }

  def per(digit: => Parser[Expr]) = "/" ~> digit

  def rangePer(digit: => Parser[Expr]) = opt(per(digit)) ^^ {
    case None => NoOp()
    case Some(d) => d
  }

  def range(digit: => Parser[Expr]) = digit ~ "-" ~ digit ~ rangePer(digit) ^^ {
    case from ~ "-" ~ to ~ per => RangeExpr(from, to, per)
  }

  def asteriskPer(digit: => Parser[Expr]) = asterisk ~ per(digit) ^^ {
    case d ~ op => PerExpr(d, op)
  }

  lazy val minDigit: Parser[Expr] = ("""0?[0-9]""".r ||| """[1-5][0-9]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val hourDigit: Parser[Expr] = ("""0?[0-9]""".r ||| """1[0-9]""".r ||| """2[0-3]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val dayDigit: Parser[Expr] = ("""0?[1-9]""".r ||| """[12][0-9]""".r ||| """3[01]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val monthDigit: Parser[Expr] = ("""0?[1-9]""".r ||| """1[0-2]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val asterisk: Parser[Expr] = """[*]""".r ^^ {
    case asterisk => AnyValueExpr()
  }

}