package uk.gov.hmcts.reform.unspec.handler.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.ccd.client.model.AboutToStartOrSubmitCallbackResponse;
import uk.gov.hmcts.reform.ccd.client.model.CallbackResponse;
import uk.gov.hmcts.reform.payments.client.models.PaymentDto;
import uk.gov.hmcts.reform.unspec.callback.Callback;
import uk.gov.hmcts.reform.unspec.callback.CallbackHandler;
import uk.gov.hmcts.reform.unspec.callback.CallbackParams;
import uk.gov.hmcts.reform.unspec.callback.CaseEvent;
import uk.gov.hmcts.reform.unspec.config.PaymentsConfiguration;
import uk.gov.hmcts.reform.unspec.helpers.CaseDetailsConverter;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.service.PaymentsService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.unspec.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.unspec.callback.CaseEvent.MAKE_PBA_PAYMENT;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentsCallbackHandler extends CallbackHandler {

    private static final List<CaseEvent> EVENTS = Collections.singletonList(MAKE_PBA_PAYMENT);

    private final PaymentsConfiguration paymentsConfiguration;
    private final PaymentsService paymentsService;
    private final CaseDetailsConverter caseDetailsConverter;

    @Override
    protected Map<String, Callback> callbacks() {
        return Map.of(callbackKey(ABOUT_TO_SUBMIT), this::makePbaPayment);
    }

    @Override
    public List<CaseEvent> handledEvents() {
        return EVENTS;
    }

    private CallbackResponse makePbaPayment(CallbackParams callbackParams) {
        CaseData caseData = callbackParams.getCaseData();
        var builder = caseData.toBuilder();
        if (paymentsConfiguration.isEnabled()) {
            try {
                PaymentDto paymentDto = paymentsService.createCreditAccountPayment(caseData);
                builder.paymentReference(paymentDto.getReference());
            } catch (Exception e) {
                log.error(String.format("Error when making payment for case: %s, message: %s",
                                        caseData.getCcdCaseReference(), e.getMessage()
                ));
            }
        } else {
            log.info("Payment configuration is disabled.");
        }

        return AboutToStartOrSubmitCallbackResponse.builder()
            .data(caseDetailsConverter.toMap(builder.build()))
            .build();
    }
}
