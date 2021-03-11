package uk.gov.hmcts.reform.unspec.service.docmosis.sealedclaim;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.unspec.model.Address;
import uk.gov.hmcts.reform.unspec.model.CaseData;
import uk.gov.hmcts.reform.unspec.model.LitigationFriend;
import uk.gov.hmcts.reform.unspec.model.Party;
import uk.gov.hmcts.reform.unspec.model.SolicitorReferences;
import uk.gov.hmcts.reform.unspec.model.docmosis.DocmosisDocument;
import uk.gov.hmcts.reform.unspec.model.docmosis.common.Applicant;
import uk.gov.hmcts.reform.unspec.model.docmosis.sealedclaim.Representative;
import uk.gov.hmcts.reform.unspec.model.docmosis.sealedclaim.Respondent;
import uk.gov.hmcts.reform.unspec.model.docmosis.sealedclaim.SealedClaimForm;
import uk.gov.hmcts.reform.unspec.model.documents.CaseDocument;
import uk.gov.hmcts.reform.unspec.model.documents.DocumentType;
import uk.gov.hmcts.reform.unspec.model.documents.PDF;
import uk.gov.hmcts.reform.unspec.service.OrganisationService;
import uk.gov.hmcts.reform.unspec.service.docmosis.DocumentGeneratorService;
import uk.gov.hmcts.reform.unspec.service.docmosis.TemplateDataGenerator;
import uk.gov.hmcts.reform.unspec.service.documentmanagement.DocumentManagementService;
import uk.gov.hmcts.reform.unspec.service.flowstate.FlowState;
import uk.gov.hmcts.reform.unspec.service.flowstate.StateFlowEngine;
import uk.gov.hmcts.reform.unspec.utils.DocmosisTemplateDataUtils;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.reform.unspec.model.docmosis.sealedclaim.Representative.fromOrganisation;
import static uk.gov.hmcts.reform.unspec.model.docmosis.sealedclaim.Representative.fromSolicitorOrganisationDetails;
import static uk.gov.hmcts.reform.unspec.service.docmosis.DocmosisTemplates.N1;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.Main.PROCEEDS_OFFLINE_UNREPRESENTED_DEFENDANT;
import static uk.gov.hmcts.reform.unspec.service.flowstate.FlowState.fromFullName;

@Service
@RequiredArgsConstructor
public class SealedClaimFormGenerator implements TemplateDataGenerator<SealedClaimForm> {

    //TODO this need ui implementation to capture claim details
    public static final String TEMP_CLAIM_DETAILS = "The claimant seeks compensation from injuries and losses arising"
        + " from a road traffic accident which occurred on 1st July 2017 as a result of the negligence of the first "
        + "defendant.The claimant seeks compensation from injuries and losses arising from a road traffic accident "
        + "which occurred on 1st July 2017 as a result of the negligence of the first defendant.";

    private static final Representative TEMP_REPRESENTATIVE = Representative.builder()
        .dxAddress("DX 751Newport")
        .organisationName("DBE Law")
        .phoneNumber("0800 206 1592")
        .emailAddress("jim.smith@slatergordon.com")
        .serviceAddress(Address.builder()
                            .addressLine1("AdmiralHouse")
                            .addressLine2("Queensway")
                            .postTown("Newport")
                            .postCode("NP204AG")
                            .build())
        .build(); //TODO Rep details need to be picked from reference data

    private final DocumentManagementService documentManagementService;
    private final DocumentGeneratorService documentGeneratorService;
    private final StateFlowEngine stateFlowEngine;
    private final OrganisationService organisationService;

    public CaseDocument generate(CaseData caseData, String authorisation) {
        SealedClaimForm templateData = getTemplateData(caseData);

        DocmosisDocument docmosisDocument = documentGeneratorService.generateDocmosisDocument(templateData, N1);
        return documentManagementService.uploadDocument(
            authorisation,
            new PDF(getFileName(caseData), docmosisDocument.getBytes(), DocumentType.SEALED_CLAIM)
        );
    }

    private String getFileName(CaseData caseData) {
        return String.format(N1.getDocumentTitle(), caseData.getLegacyCaseReference());
    }

    @Override
    public SealedClaimForm getTemplateData(CaseData caseData) {
        Optional<SolicitorReferences> solicitorReferences = ofNullable(caseData.getSolicitorReferences());
        return SealedClaimForm.builder()
            .applicants(getApplicants(caseData))
            .respondents(getRespondents(caseData))
            .claimValue(caseData.getClaimValue().formData())
            .statementOfTruth(caseData.getApplicantSolicitor1ClaimStatementOfTruth())
            .claimDetails(TEMP_CLAIM_DETAILS)
            .hearingCourtLocation(caseData.getCourtLocation().getApplicantPreferredCourt())
            .applicantRepresentative(TEMP_REPRESENTATIVE)
            .referenceNumber(caseData.getLegacyCaseReference())
            .issueDate(caseData.getClaimIssuedDate())
            .submittedOn(caseData.getClaimSubmittedDateTime().toLocalDate())
            .applicantExternalReference(solicitorReferences
                                           .map(SolicitorReferences::getApplicantSolicitor1Reference)
                                           .orElse(""))
            .respondentExternalReference(solicitorReferences
                                            .map(SolicitorReferences::getRespondentSolicitor1Reference)
                                            .orElse(""))
            .caseName(DocmosisTemplateDataUtils.toCaseName.apply(caseData))
            .build();
    }

    private List<Respondent> getRespondents(CaseData caseData) {
        Party respondent = caseData.getRespondent1();
        return List.of(Respondent.builder()
                           .name(respondent.getPartyName())
                           .primaryAddress(respondent.getPrimaryAddress())
                           .representative(getRepresentative(caseData))
                           .litigationFriendName(
                               ofNullable(caseData.getRespondent1LitigationFriend())
                                   .map(LitigationFriend::getFullName)
                                   .orElse(""))
                           .build());
    }

    private List<Applicant> getApplicants(CaseData caseData) {
        Party applicant = caseData.getApplicant1();
        return List.of(Applicant.builder()
                           .name(applicant.getPartyName())
                           .primaryAddress(applicant.getPrimaryAddress())
                           .litigationFriendName(
                               ofNullable(caseData.getApplicant1LitigationFriend())
                                   .map(LitigationFriend::getFullName)
                                   .orElse(""))
                           .build());
    }

    private Representative getRepresentative(CaseData caseData) {
        var stateFlow = stateFlowEngine.evaluate(caseData).getState();
        var organisationId = caseData.getRespondent1OrganisationPolicy().getOrganisation().getOrganisationID();
        if (fromFullName(stateFlow.getName()) != PROCEEDS_OFFLINE_UNREPRESENTED_DEFENDANT) {
            return fromOrganisation(organisationService.findOrganisationById(organisationId)
                                        .orElseThrow(RuntimeException::new));
        }
        return fromSolicitorOrganisationDetails(caseData.getRespondentSolicitor1OrganisationDetails());
    }
}
