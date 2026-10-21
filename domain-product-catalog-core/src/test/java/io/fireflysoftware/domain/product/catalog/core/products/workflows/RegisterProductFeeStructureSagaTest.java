/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.product.catalog.core.products.workflows;

import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductFeeStructureCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A registered fee structure is removed through the product configuration delete the core already exposes.
 */
@ExtendWith(MockitoExtension.class)
class RegisterProductFeeStructureSagaTest {

    @Mock
    private CommandBus commandBus;

    @Test
    void compensationDeletesTheCreatedFeeStructure() {
        UUID productId = UUID.randomUUID();
        UUID feeStructureId = UUID.randomUUID();
        when(commandBus.send(any(RegisterProductFeeStructureCommand.class))).thenReturn(Mono.just(feeStructureId));
        when(commandBus.send(any(RemoveProductFeeStructureCommand.class))).thenReturn(Mono.empty());
        RegisterProductFeeStructureSaga saga = new RegisterProductFeeStructureSaga(commandBus);
        ExecutionContext ctx = ExecutionContext.forSaga("RegisterProductFeeStructureSaga", "test");

        StepVerifier.create(saga.registerProductFeeStructure(new RegisterProductFeeStructureCommand().withProductId(productId), ctx))
                .expectNext(feeStructureId)
                .verifyComplete();
        StepVerifier.create(saga.removeProductFeeStructure(feeStructureId, ctx)).verifyComplete();

        ArgumentCaptor<RemoveProductFeeStructureCommand> captor = ArgumentCaptor.forClass(RemoveProductFeeStructureCommand.class);
        verify(commandBus).send(captor.capture());
        assertThat(captor.getValue().productId()).isEqualTo(productId);
        assertThat(captor.getValue().productFeeStructureId()).isEqualTo(feeStructureId);
    }
}
