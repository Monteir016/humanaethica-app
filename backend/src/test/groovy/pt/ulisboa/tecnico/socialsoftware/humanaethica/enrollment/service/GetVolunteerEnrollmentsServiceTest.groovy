package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetVolunteerEnrollmentsServiceTest extends SpockTest {
    def activityOne
    def activityTwo

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, null)
        activityOne = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activityOne)

        activityDto.name = ACTIVITY_NAME_2
        activityTwo = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activityTwo)
    }

    def "get enrollments for one volunteer"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        createEnrollment(activityOne, volunteer, ENROLLMENT_MOTIVATION_1)
        createEnrollment(activityTwo, volunteer, ENROLLMENT_MOTIVATION_2)

        when:
        def result = enrollmentService.getVolunteerEnrollments(volunteer.id)

        then:
        result.size() == 2
        result*.motivation.containsAll([ENROLLMENT_MOTIVATION_1, ENROLLMENT_MOTIVATION_2])
        result*.volunteerId.every { it == volunteer.id }
    }

    def "get enrollments returns empty when volunteer has none"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        when:
        def result = enrollmentService.getVolunteerEnrollments(volunteer.id)

        then:
        result.isEmpty()
    }

    def "get enrollments fails when user id is null"() {
        when:
        enrollmentService.getVolunteerEnrollments(null)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.USER_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
