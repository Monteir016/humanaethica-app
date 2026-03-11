package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CreateShiftWebServiceTest extends SpockTest {
    @LocalServerPort
    private int port

    def activity
    def shiftDto

    def setup() {
        deleteAll()

        webClient = WebClient.create("http://localhost:" + port)
        headers = new HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)

        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 5, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )
    }

    def 'login as member, and create a shift'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then: "check response data"
        response.description == SHIFT_DESCRIPTION_1
        response.participantsLimit == SHIFT_PARTICIPANTS_LIMIT_2
        response.startingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(1))
        response.endingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(2))
        response.activityId == activity.id
        and: 'check database data'
        shiftRepository.count() == 1
        def shift = shiftRepository.findAll().get(0)
        shift.getDescription() == SHIFT_DESCRIPTION_1
        shift.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_2
        shift.getStartingDate().withNano(0) == IN_TWO_DAYS.plusHours(1).withNano(0)
        shift.getEndingDate().withNano(0) == IN_TWO_DAYS.plusHours(2).withNano(0)
        shift.getActivity().getId() == activity.id

        cleanup:
        deleteAll()
    }

    def 'login as volunteer, cannot create a shift'() {
        given:
        demoVolunteerLogin()

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'login as admin, cannot create a shift'() {
        given:
        demoAdminLogin()

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }

    def 'login as member of another institution, cannot create a shift'() {
        given:
        def otherInstitution = new Institution(INSTITUTION_1_NAME, INSTITUTION_1_EMAIL, INSTITUTION_1_NIF)
        institutionRepository.save(otherInstitution)
        createMember(USER_1_NAME, USER_1_USERNAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, otherInstitution, User.State.APPROVED)
        normalUserLogin(USER_1_USERNAME, USER_1_PASSWORD)

        when:
        webClient.post()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .bodyValue(shiftDto)
                .retrieve()
                .bodyToMono(ShiftDto.class)
                .block()

        then:
        def error = thrown(WebClientResponseException)
        error.statusCode == HttpStatus.FORBIDDEN
        shiftRepository.count() == 0

        cleanup:
        deleteAll()
    }
}
