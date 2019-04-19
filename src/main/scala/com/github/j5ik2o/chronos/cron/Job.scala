package com.github.j5ik2o.chronos.cron

import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors

import scala.collection.mutable.ArrayBuffer

object Job {

  sealed trait Message
  sealed trait Event          extends Message
  sealed trait CommandMessage extends Message

  case class ExecuteRequest(replyTo: ActorRef[ExecuteResponse])             extends CommandMessage
  sealed trait ExecuteResponse                                              extends CommandMessage
  case object ExecutedSucceeded                                             extends ExecuteResponse
  case class ExecutedFailed(message: String)                                extends ExecuteResponse
  private case class WrappedExecuteResponse(response: Step.ExecuteResponse) extends Message

  case class BehaviorWithName[A](name: String, behavior: Behavior[A])
  trait Steps
  case class SerialSteps(values: List[BehaviorWithName[Step.Message]])   extends Steps
  case class ParallelSteps(values: List[BehaviorWithName[Step.Message]]) extends Steps
  case class ContainerSteps(values: List[Steps])                         extends Steps

  def behavior(steps: Steps): Behavior[Message] = Behaviors.setup { ctx =>
    def serialBehavior(stepValues: List[BehaviorWithName[Step.Message]],
                       acc: Vector[Step.ExecuteResponse],
                       lastBehavior: Behavior[Message]): Behavior[Message] = {
      stepValues match {
        case Nil =>
          lastBehavior
        case BehaviorWithName(n, b) :: tail =>
          val ref         = ctx.spawn(b, n)
          val receiverRef = ctx.messageAdapter[Step.ExecuteResponse](WrappedExecuteResponse(_))
          ref ! Step.ExecuteRequest(receiverRef)
          Behaviors.receiveMessage[Message] {
            case WrappedExecuteResponse(response) =>
              serialBehavior(tail, acc :+ response, lastBehavior)
          }
      }
    }
    def parallelBehavior(stepValues: Seq[BehaviorWithName[Step.Message]],
                         lastBehavior: Behavior[Message]): Behavior[Message] = {
      stepValues.foreach {
        case BehaviorWithName(n, b) =>
          val ref         = ctx.spawn(b, n)
          val receiverRef = ctx.messageAdapter[Step.ExecuteResponse](WrappedExecuteResponse(_))
          ref ! Step.ExecuteRequest(receiverRef)
      }
      val responses = ArrayBuffer.empty[Step.ExecuteResponse]
      Behaviors.receiveMessage[Message] {
        case WrappedExecuteResponse(response) =>
          responses.append(response)
          if (stepValues.size == responses.size)
            lastBehavior
          else
            Behaviors.same
      }
    }
    Behaviors.receiveMessage[Message] {
      case ExecuteRequest(replyTo) =>
        def doSteps(steps: Steps, tailTasks: ContainerSteps = ContainerSteps(Nil)): Behavior[Message] = {
          steps match {
            case ContainerSteps(Nil) =>
              // TODO: 戻り値と失敗のハンドリング
              replyTo ! ExecutedSucceeded
              Behaviors.stopped
            case ContainerSteps(head :: tail) =>
              doSteps(head, ContainerSteps(tail))
            case SerialSteps(values) =>
              serialBehavior(values, Vector.empty, doSteps(tailTasks))
            case ParallelSteps(values) =>
              parallelBehavior(values, doSteps(tailTasks))
          }
        }
        doSteps(steps)
    }
  }

}
