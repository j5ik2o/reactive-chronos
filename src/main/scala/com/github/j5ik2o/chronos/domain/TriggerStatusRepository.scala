package com.github.j5ik2o.chronos.domain

import java.util.UUID

import org.sisioh.baseunits.scala.time.TimePoint

import scala.util.Try

case class TriggerStatusRepository(entities: Map[UUID, TriggerStatus] = Map.empty) {

  def store(aggregate: TriggerStatus): Try[TriggerStatusRepository] = Try {
    copy(entities = this.entities + (aggregate.id -> aggregate))
  }

  def storeMulti(aggregates: Seq[TriggerStatus]): Try[TriggerStatusRepository] = Try {
    copy(entities = this.entities ++ aggregates.map { e => e.id -> e }.toMap)
  }

  def delete(id: UUID): Try[TriggerStatusRepository] = Try {
    copy(entities = this.entities.filterNot { case (k, _) => k == id })
  }

  def resolveBy(id: UUID): Try[TriggerStatus] = Try {
    entities(id)
  }

  def resolveByTriggerId(triggerId: UUID): Try[TriggerStatus] = Try{
    iterator.find(e => e.triggerId == triggerId).get
  }

  def resolveByTriggerIdAndComputedFireAt(triggerId: UUID, computedFireAt: TimePoint): Try[TriggerStatus] = Try {
    iterator.find(e => e.triggerId == triggerId && e.computedFireAt == computedFireAt).get
  }

  def iterator: Iterator[TriggerStatus] = entities.map { case (_, v) => v }.toIterator
}
