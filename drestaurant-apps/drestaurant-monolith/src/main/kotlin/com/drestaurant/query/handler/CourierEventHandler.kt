package com.drestaurant.query.handler

import com.drestaurant.courier.domain.api.CourierCreatedEvent
import com.drestaurant.query.model.CourierEntity
import com.drestaurant.query.repository.CourierRepository
import org.axonframework.config.ProcessingGroup
import org.axonframework.eventhandling.AllowReplay
import org.axonframework.eventhandling.EventHandler
import org.axonframework.eventhandling.ResetHandler
import org.axonframework.eventsourcing.SequenceNumber
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component

@Component
@ProcessingGroup("courier")
internal class CourierEventHandler(private val repository: CourierRepository, private val messagingTemplate: SimpMessageSendingOperations) {

    @EventHandler
    @AllowReplay(true)
    fun handle(event: CourierCreatedEvent, @SequenceNumber aggregateVersion: Long) {
        repository.save(CourierEntity(event.aggregateIdentifier, aggregateVersion, event.name.firstName, event.name.lastName, event.maxNumberOfActiveOrders));
        messagingTemplate.convertAndSend("/topic/couriers.updates", event);
    }

    @ResetHandler // Will be called before replay/reset starts. Do pre-reset logic, like clearing out the Projection table
    fun onReset() {
        repository.deleteAll()
    }

}
