package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment

import org.springframework.security.core.Authentication
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User
import spock.lang.Specification

class EnrollmentControllerTest extends Specification {
    def enrollmentService = Mock(EnrollmentService)
    def controller = new EnrollmentController()

    def setup() {
        controller.enrollmentService = enrollmentService
    }

    def "get activity enrollments delegates to service"() {
        given:
        def dto = new EnrollmentDto()
        dto.setId(1)
        enrollmentService.getEnrollmentsByActivity(10) >> [dto]

        when:
        def result = controller.getActivityEnrollments(10)

        then:
        result.size() == 1
        result.get(0).id == 1
    }

    def "get volunteer enrollments delegates to service"() {
        given:
        def principal = Mock(Authentication)
        def authUser = Mock(AuthUser)
        def user = Mock(User)
        user.getId() >> 20
        authUser.getUser() >> user
        principal.getPrincipal() >> authUser

        def dto = new EnrollmentDto()
        dto.setId(2)
        enrollmentService.getVolunteerEnrollments(20) >> [dto]

        when:
        def result = controller.getVolunteerEnrollments(principal)

        then:
        result.size() == 1
        result.get(0).id == 2
    }

    def "create enrollment delegates to service"() {
        given:
        def principal = Mock(Authentication)
        def authUser = Mock(AuthUser)
        def user = Mock(User)
        user.getId() >> 30
        authUser.getUser() >> user
        principal.getPrincipal() >> authUser

        def request = new EnrollmentDto()
        request.setMotivation("valid motivation text")

        def response = new EnrollmentDto()
        response.setId(3)
        enrollmentService.createEnrollment(30, request) >> response

        when:
        def result = controller.createEnrollment(principal, request)

        then:
        result.id == 3
    }

    def "update enrollment delegates to service"() {
        given:
        def request = new EnrollmentDto()
        request.setMotivation("updated valid motivation")
        def response = new EnrollmentDto()
        response.setId(4)
        enrollmentService.updateEnrollment(40, request) >> response

        when:
        def result = controller.updateEnrollemt(40, request)

        then:
        result.id == 4
    }

    def "remove enrollment delegates to service"() {
        given:
        def response = new EnrollmentDto()
        response.setId(5)
        enrollmentService.removeEnrollment(50) >> response

        when:
        def result = controller.removeEnrollment(50)

        then:
        result.id == 5
    }
}
