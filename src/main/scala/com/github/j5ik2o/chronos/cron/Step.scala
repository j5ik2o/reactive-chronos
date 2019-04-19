package com.github.j5ik2o.chronos.cron

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

object Step {

  sealed trait Message
  sealed trait Event          extends Message
  sealed trait CommandMessage extends Message

  case class ExecuteRequest(replyTo: ActorRef[ExecuteResponse]) extends CommandMessage
  sealed trait ExecuteResponse                                  extends CommandMessage
  case object ExecutedSucceeded                                 extends ExecuteResponse
  case class ExecutedFailed(message: String)                    extends ExecuteResponse
  private final case class WrappedTaskletResponse(response: Tasklet.ExecuteResponse, replyTo: ActorRef[ExecuteResponse])
      extends Message

  def behavior(name: String, taskletBehavior: Behavior[Tasklet.Message]): Behavior[Message] = Behaviors.setup[Message] {
    ctx =>
      val taskletRef = ctx.spawn(taskletBehavior, name)
      ctx.watch(taskletRef)
      Behaviors.receiveMessage[Step.Message] {
        case ExecuteRequest(replyTo) =>
          val taskletReceiverRef: ActorRef[Tasklet.ExecuteResponse] =
            ctx.messageAdapter[Tasklet.ExecuteResponse](WrappedTaskletResponse(_, replyTo))
          taskletRef ! Tasklet.ExecuteRequest(taskletReceiverRef)
          Behaviors.stopped
        case WrappedTaskletResponse(response, replyTo) =>
          response match {
            case Tasklet.ExecutedSucceeded =>
              replyTo ! ExecutedSucceeded
            case Tasklet.ExecutedFailed(msg) =>
              replyTo ! ExecutedFailed(msg)
          }
          Behaviors.stopped
      }

  }

}
