package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation

import org.springframework.security.core.Authentication
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Specification

class ParticipationControllerTest extends Specification {
    def participationService = Mock(ParticipationService)
    def controller = new ParticipationController()

    def setup() {
        controller.participationService = participationService
    }

    def "get activity participations delegates to service"() {
        given:
        def dto = new ParticipationDto()
        dto.setId(1)
        participationService.getParticipationsByActivity(10) >> [dto]

        when:
        def result = controller.getActivityParticipations(10)

        then:
        result.size() == 1
        result.get(0).id == 1
    }

    def "get volunteer participations delegates to service"() {
        given:
        def principal = Mock(Authentication)
        def authUser = Mock(AuthUser)
        def user = Mock(User)
        user.getId() >> 20
        authUser.getUser() >> user
        principal.getPrincipal() >> authUser

        def dto = new ParticipationDto()
        dto.setId(2)
        participationService.getVolunteerParticipations(20) >> [dto]

        when:
        def result = controller.getVolunteerParticipations(principal)

        then:
        result.size() == 1
        result.get(0).id == 2
    }

    def "create participation delegates to service"() {
        given:
        def request = new ParticipationDto()
        request.setVolunteerId(30)
        def response = new ParticipationDto()
        response.setId(3)
        participationService.createParticipation(11, request) >> response

        when:
        def result = controller.createParticipation(11, request)

        then:
        result.id == 3
    }

    def "volunteer rating delegates to service"() {
        given:
        def request = new ParticipationDto()
        request.setVolunteerRating(5)
        def response = new ParticipationDto()
        response.setId(4)
        participationService.volunteerRating(40, request) >> response

        when:
        def result = controller.volunteerRating(40, request)

        then:
        result.id == 4
    }

    def "member rating delegates to service"() {
        given:
        def request = new ParticipationDto()
        request.setMemberRating(5)
        def response = new ParticipationDto()
        response.setId(5)
        participationService.memberRating(50, request) >> response

        when:
        def result = controller.memberRating(50, request)

        then:
        result.id == 5
    }

    def "delete participation delegates to service"() {
        given:
        def response = new ParticipationDto()
        response.setId(6)
        participationService.deleteParticipation(60) >> response

        when:
        def result = controller.deleteParticipation(60)

        then:
        result.id == 6
    }
}
