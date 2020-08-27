package com.drestaurant.query.handler

import com.drestaurant.courier.domain.api.CourierOrderAssignedEvent
import com.drestaurant.courier.domain.api.CourierOrderCreatedEvent
import com.drestaurant.courier.domain.api.CourierOrderDeliveredEvent
import com.drestaurant.courier.domain.api.CourierOrderNotAssignedEvent
import com.drestaurant.courier.domain.api.model.CourierOrderState
import com.drestaurant.query.model.CourierOrderEntity
import com.drestaurant.query.repository.CourierOrderRepository
import com.drestaurant.query.repository.CourierRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.axonframework.eventhandling.SequenceNumber
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("courierorder")
internal class CourierOrderHandler(private val repository: CourierOrderRepository, private val courierRepository: CourierRepository, private val messagingTemplate: SimpMessageSendingOperations) {

    @EventHandler
    @AllowReplay(true)
    fun handle(event: CourierOrderCreatedEvent, @SequenceNumber aggregateVersion: Long) {
        val record = CourierOrderEntity(event.aggregateIdentifier.identifier, aggregateVersion, null, CourierOrderState.CREATED)
        repository.save(record)
        broadcastUpdates()
    }

    @EventHandler
    @AllowReplay(true)
    fun handle(event: CourierOrderAssignedEvent, @SequenceNumber aggregateVersion: Long) {
        val courierEntity = courierRepository.findById(event.courierId.identifier).get()
        val record = repository.findById(event.aggregateIdentifier.identifier).get()
        record.state = CourierOrderState.ASSIGNED
        record.courier = courierEntity
        repository.save(record)
        broadcastUpdates()
    }

    @EventHandler
    @AllowReplay(true)
    fun handle(event: CourierOrderNotAssignedEvent, @SequenceNumber aggregateVersion: Long) {
        // Do nothing
        broadcastUpdates()
    }

    @EventHandler
    @AllowReplay(true)
    fun handle(event: CourierOrderDeliveredEvent, @SequenceNumber aggregateVersion: Long) {
        val record = repository.findById(event.aggregateIdentifier.identifier).get()
        record.state = CourierOrderState.DELIVERED
        repository.save(record)
        broadcastUpdates()
    }

    /* Will be called before replay/reset starts. Do pre-reset logic, like clearing out the Projection table */
    @ResetHandler
    fun onReset() = repository.deleteAll()

    private fun broadcastUpdates() = messagingTemplate.convertAndSend("/topic/couriers/orders.updates", repository.findAll())
}
