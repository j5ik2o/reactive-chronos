package com.github.j5ik2o.chronos.scheduler

import java.util.UUID

import com.github.j5ik2o.chronos.utils.DefaultToStringStyle
import org.apache.commons.lang3.builder.ToStringBuilder

import scala.util.Try

object JobProtocol {

  trait Message {
    protected def toStringBuilder = new ToStringBuilder(this, DefaultToStringStyle.ofClass(getClass))

    override def toString: String = toStringBuilder.build()
  }

  sealed trait CommandRequest extends Message {
    val id: UUID

    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("id", id)
  }

  sealed trait CommandResponse extends Message {
    val id: UUID
    val requestId: Option[UUID]

    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("id", id)
        .append("requestId", requestId)
  }

  case class Start(id: UUID, message: Any) extends CommandRequest {
    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("message", message)
  }

  case class Finish(id: UUID, result: Try[Any]) extends CommandRequest {
    override protected def toStringBuilder: ToStringBuilder =
      super.toStringBuilder
        .append("result", result)
  }

}
