package com.github.j5ik2o.chronos.utils

import org.apache.commons.lang3.SystemUtils
import org.apache.commons.lang3.builder.ToStringStyle

@SerialVersionUID(-4763645948532141458L)
case class DefaultToStringStyle(prefix: String) extends ToStringStyle() {

  setUseClassName(false)
  setUseShortClassName(false)
  setUseIdentityHashCode(false)
  setFieldSeparator(SystemUtils.LINE_SEPARATOR + DefaultToStringStyle.INDENT)
  setFieldNameValueSeparator(" = ")
  setContentStart(s"$prefix(")
  setFieldSeparatorAtStart(true)
  setContentEnd(SystemUtils.LINE_SEPARATOR + ")")

  override def appendFieldStart(buffer: StringBuffer, fieldName: String): Unit = {
    val cur = DefaultToStringStyle.level.get
    (0 until cur).foreach { _ =>
      buffer.append(DefaultToStringStyle.INDENT)
    }
    super.appendFieldStart(buffer, fieldName)
    DefaultToStringStyle.level.set(cur + 1)
  }

  override def appendFieldEnd(buffer: StringBuffer, fieldName: String): Unit = {
    super.appendFieldEnd(buffer, fieldName)
    val cur = DefaultToStringStyle.level.get
    DefaultToStringStyle.level.set(cur - 1)
  }

  override def appendContentEnd(buffer: StringBuffer): Unit = {
    buffer.append(SystemUtils.LINE_SEPARATOR)
    val cur = DefaultToStringStyle.level.get
    (0 until cur).foreach { _ =>
      buffer.append(DefaultToStringStyle.INDENT)
    }
    buffer.append(")")
    //super.appendContentEnd(")")
  }
}

object DefaultToStringStyle {

  val INDENT = "  "
  //val INSTANCE = new DefaultToStringStyle()

  def ofClass(clazz: Class[_]): DefaultToStringStyle = DefaultToStringStyle(clazz.getSimpleName)

  def ofString(prefix: String): DefaultToStringStyle = DefaultToStringStyle(prefix)

  val level = new ThreadLocal[Integer]() {
    override def initialValue(): Integer = 0
  }

}
