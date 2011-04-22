package com.github.j5ik2o.chronos.crond

import util.parsing.combinator._
import java.text.ParseException


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

case class LastValue extends Expr

case class AnyValueExpr extends Expr

case class PerExpr(digit: Expr, option: Expr) extends Expr

case class RangeExpr(from: Expr, to: Expr, perOtion: Expr) extends Expr

case class ListExpr(exprs: List[Expr]) extends Expr

case class CronExpr(mins: Expr, hours: Expr, days: Expr, months: Expr, dayOfWeeks: Expr) extends Expr

case class CrondParseException(message:String) extends Exception(message)

class CrondParser extends RegexParsers {

  def parse(source: String) = parseAll(instruction, source) match {
    case Success(result, _) => result
    case Failure(msg, _) => throw new CrondParseException(msg)
    case Error(msg, _) => throw new CrondParseException(msg)
  }

  def instruction: Parser[CronExpr] =
    digitInstruction(minDigit) ~ digitInstruction(hourDigit) ~
      digitInstruction(dayDigit) ~ digitInstruction(monthDigit) ~
      digitInstruction(dayOfWeekDigit | dayOfWeekText) ^^ {
      case mins ~ hours ~ days ~ months ~ dayOfWeeks => CronExpr(mins, hours, days, months, dayOfWeeks)
    }

  def digitInstruction(digit: => Parser[Expr]) = asterisk ||| list(digit ||| range(digit)) ||| range(digit) ||| asteriskPer(digit)


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
  } ||| LAST ^^ {
    case _ => LastValue()
  }

  lazy val monthDigit: Parser[Expr] = ("""0?[1-9]""".r ||| """1[0-2]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val dayOfWeekDigit: Parser[Expr] = ("""[1-7]""".r) ^^ {
    case digit => ValueExpr(digit.toInt)
  }

  lazy val SUN = """(?i)SUN""".r
  lazy val MON = """(?i)MON""".r
  lazy val TUE = """(?i)TUE""".r
  lazy val WED = """(?i)WED""".r
  lazy val THU = """(?i)THU""".r
  lazy val FRI = """(?i)FRI""".r
  lazy val SAT = """(?i)SAT""".r
  lazy val LAST = "L"

  lazy val dayOfWeekText: Parser[Expr] =
    SUN ^^ {
      case _ => ValueExpr(1)
    } ||| MON ^^ {
      case _ => ValueExpr(2)
    } ||| TUE ^^ {
      case _ => ValueExpr(3)
    } ||| WED ^^ {
      case _ => ValueExpr(4)
    } ||| THU ^^ {
      case _ => ValueExpr(5)
    } ||| FRI ^^ {
      case _ => ValueExpr(6)
    } ||| SAT ^^ {
      case _ => ValueExpr(7)
    } ||| LAST ^^ {
      case _ => LastValue()
    }


  lazy val asterisk: Parser[Expr] = """[*]""".r ^^ {
    case asterisk => AnyValueExpr()
  }

}