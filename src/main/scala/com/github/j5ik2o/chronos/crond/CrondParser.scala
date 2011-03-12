package com.github.j5ik2o.chronos.crond

import util.parsing.combinator._

trait Expr {

}

case class NoOp extends Expr

case class AsteriskValue extends Expr

case class MinValue(digit: Int) extends Expr
case class HourValue(digit: Int) extends Expr
case class DayValue(digit: Int) extends Expr
case class MonthValue(digit: Int) extends Expr
case class PerExpr(digit:Expr, option: Expr) extends Expr
case class RangeExpr(from: Expr, to: Expr, perOtion: Expr) extends Expr
case class ListExpr(exprs:List[Expr]) extends Expr
case class CronExpr(mins:Expr, hours:Expr, days:Expr)

class CrondParser extends JavaTokenParsers {

  def parse(source: String) = parseAll(instruction, source)

  def instruction = digitInstruction(minDigit) ~ digitInstruction(hourDigit) ~ digitInstruction(dayDigit) ^^ {
    case m ~ h ~ d => CronExpr(m, h, d)
  }

  def digitInstruction(digit: => Parser[Expr]) = list(digit) ||| range(digit) ||| asteriskPer(digit)


  def list(digit: => Parser[Expr]) = repsep(digit, ",") ^^ {
    case l => ListExpr(l)
  }

  def per(digit: => Parser[Expr]) = "/" ~> digit

  def rangePer(digit: => Parser[Expr]):Parser[Expr] = opt(per(digit)) ^^ {
    case None => NoOp()
    case Some(d) => d
  }
  
  def range(digit: => Parser[Expr]):Parser[Expr] = digit ~ "-" ~ digit ~ rangePer(digit) ^^ {
    case from ~ "-" ~ to ~ per => RangeExpr(from, to, per)
  }

  def asteriskPer(digit: => Parser[Expr]):Parser[Expr] = asterisk ~ per(digit) ^^ {
    case d ~ op => PerExpr(d, op)
  }


  //  lazy val hours:Parser[Expr] = digitInstruction(hourDigit)
  //  lazy val days = repsep(digitInstruction(dayDigit), ",")
  //  lazy val months = repsep(digitInstruction(monthDigit), ",")



  lazy val minDigit: Parser[Expr] = ("""0?[0-9]""".r ||| """[1-5][0-9]""".r) ^^ {
    case digit => MinValue(digit.toInt)
  }

  lazy val hourDigit: Parser[Expr] = ("""0?[0-9]""".r ||| """1[0-9]""".r ||| """2[0-3]""".r) ^^ {
    case digit => HourValue(digit.toInt)
  }

  lazy val dayDigit: Parser[Expr] = ("""0?[1-9]""".r ||| """[12][0-9]""".r ||| """3[01]""".r) ^^ {
    case digit => DayValue(digit.toInt)
  }

  lazy val monthDigit: Parser[Expr] = ("""0?[1-9]""".r ||| """1[0-2]""".r) ^^ {
    case digit => MonthValue(digit.toInt)
  }

  lazy val asterisk: Parser[Expr] = """[*]""".r ^^ {
    case asterisk => AsteriskValue()
  }

}