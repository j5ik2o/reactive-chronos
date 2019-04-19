package com.github.j5ik2o.chronos.cron

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.stream.scaladsl.{ Flow, Keep, Sink, Source }
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.{ Done, NotUsed }

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object Tasklet {

  sealed trait Message
  sealed trait Event          extends Message
  sealed trait CommandMessage extends Message

  case class ExecuteRequest(replyTo: ActorRef[ExecuteResponse]) extends CommandMessage
  sealed trait ExecuteResponse                                  extends CommandMessage
  case object ExecutedSucceeded                                 extends ExecuteResponse
  case class ExecutedFailed(message: String)                    extends ExecuteResponse

  def chunkOriented[A, B](
      batchSize: Int,
      itemSource: Int => Source[Seq[A], NotUsed],
      itemFlow: Flow[A, B, NotUsed],
      itemSink: Sink[Seq[B], Future[Done]]
  ): Behavior[Message] =
    Behaviors.setup[Message] { ctx =>
      implicit val ec                     = ctx.executionContext
      implicit val mat: ActorMaterializer = ActorMaterializer()(ctx.system)
      Behaviors.receiveMessage[Message] {
        case ExecuteRequest(replyTo) =>
          itemSource(batchSize).async
            .mapConcat(_.toVector).via(itemFlow).grouped(batchSize).async.toMat(itemSink)(Keep.right).async.run() onComplete {
            case Success(_)         => replyTo ! ExecutedSucceeded
            case Failure(exception) => replyTo ! ExecutedFailed(exception.getMessage)
          }
          Behaviors.stopped
      }
    }

}
