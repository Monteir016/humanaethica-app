package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.webservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GetShiftsByActivityWebServiceTest extends SpockTest {
    @LocalServerPort
    private int port

    def activity

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
    }

    def 'login as member, and get two shifts'() {
        given:
        demoMemberLogin()
        and:
        def shiftDto1 = createShiftDto(SHIFT_DESCRIPTION_1, 2, IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(2))
        def shiftDto2 = createShiftDto(SHIFT_DESCRIPTION_2, 2, IN_TWO_DAYS.plusHours(3), IN_TWO_DAYS.plusHours(4))
        shiftService.createShift(activity.id, shiftDto1)
        shiftService.createShift(activity.id, shiftDto2)

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then: "check response data"
        response.size() == 2
        response.get(0).description == SHIFT_DESCRIPTION_1
        response.get(0).participantsLimit == 2
        response.get(0).startingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(1))
        response.get(0).endingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(2))
        response.get(0).activityId == activity.id
        response.get(1).description == SHIFT_DESCRIPTION_2
        response.get(1).participantsLimit == 2
        response.get(1).startingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(3))
        response.get(1).endingDate == DateHandler.toISOString(IN_TWO_DAYS.plusHours(4))
        response.get(1).activityId == activity.id

        cleanup:
        deleteAll()
    }

    def 'get shifts without authentication'() {
        given:
        def shiftDto1 = createShiftDto(SHIFT_DESCRIPTION_1, 2, IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(2))
        shiftService.createShift(activity.id, shiftDto1)

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then: "check response data"
        response.size() == 1
        response.get(0).description == SHIFT_DESCRIPTION_1
        response.get(0).participantsLimit == 2
        response.get(0).activityId == activity.id

        cleanup:
        deleteAll()
    }

    def 'get shifts for activity with no shifts'() {
        given:
        demoMemberLogin()

        when:
        def response = webClient.get()
                .uri('/activities/' + activity.id + '/shifts')
                .headers(httpHeaders -> httpHeaders.putAll(headers))
                .retrieve()
                .bodyToFlux(ShiftDto.class)
                .collectList()
                .block()

        then: "check response is empty"
        response.size() == 0

        cleanup:
        deleteAll()
    }
}
