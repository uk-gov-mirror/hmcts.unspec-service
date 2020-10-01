package uk.gov.hmcts.reform.unspec.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.event.DispatchBusinessProcessEvent;
import uk.gov.hmcts.reform.unspec.model.BusinessProcess;
import uk.gov.hmcts.reform.unspec.model.BusinessProcessStatus;
import uk.gov.hmcts.reform.unspec.service.CoreCaseDataService;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.reform.unspec.model.BusinessProcessStatus.DISPATCHED;
import static uk.gov.hmcts.reform.unspec.model.BusinessProcessStatus.READY;

@ExtendWith(SpringExtension.class)
class DispatchBusinessProcessEventHandlerTest {

    @Mock
    private CoreCaseDataService coreCaseDataService;

    @InjectMocks
    private DispatchBusinessProcessEventHandler handler;

    @Test
    void shouldTriggerEventWithExpectedParams_whenBusinessProcessIsReady() {
        BusinessProcess businessProcess = BusinessProcess.builder()
            .activityId("someActivityId")
            .processInstanceId("someProcessId")
            .status(READY)
            .build();
        var event = new DispatchBusinessProcessEvent(1L, businessProcess);

        handler.dispatchBusinessProcess(event);

        verify(coreCaseDataService).triggerEvent(
            event.getCaseId(),
            CaseEvent.DISPATCH_BUSINESS_PROCESS,
            Map.of("businessProcess", businessProcess.toBuilder().status(DISPATCHED).build())
        );
    }

    @ParameterizedTest
    @EnumSource(value = BusinessProcessStatus.class, names = {"DISPATCHED", "STARTED", "FINISHED"})
    void shouldNotTriggerEvent_whenBusinessProcessIsNotReady(BusinessProcessStatus businessProcessStatus) {
        BusinessProcess businessProcess = BusinessProcess.builder()
            .activityId("someActivityId")
            .processInstanceId("someProcessId")
            .status(businessProcessStatus)
            .build();
        var event = new DispatchBusinessProcessEvent(1L, businessProcess);

        handler.dispatchBusinessProcess(event);

        verifyNoInteractions(coreCaseDataService);
    }
}
