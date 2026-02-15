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
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift

@DataJpaTest
class GetEnrollmentsByActivityServiceTest extends SpockTest {
    def activity
    def otherActivity
    def shift
    def otherShift

    def setup() {
        def institution = institutionService.getDemoInstitution()

        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,5,ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS,IN_THREE_DAYS,null)

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)

        def shiftDto = createShiftDto(IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(3), 5, SHIFT_LOCATION)
        shift = new Shift(activity, shiftDto)
        shiftRepository.save(shift)

        activityDto.name = ACTIVITY_NAME_2
        otherActivity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(otherActivity)

        def otherShiftDto = createShiftDto(IN_TWO_DAYS.plusHours(1), IN_TWO_DAYS.plusHours(3), 5, SHIFT_LOCATION)
        otherShift = new Shift(otherActivity, otherShiftDto)
        shiftRepository.save(otherShift)
    }

    def "get two enrollments of the same activity"() {
        given:
        def volunteerOne = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def volunteerTwo = createVolunteer(USER_2_NAME, USER_2_USERNAME, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createEnrollment(volunteerOne, ENROLLMENT_MOTIVATION_1, List.of(shift))
        createEnrollment(volunteerTwo, ENROLLMENT_MOTIVATION_2, List.of(shift))

        when:
        def enrollments = enrollmentService.getEnrollmentsByActivity(activity.id)

        then:
        enrollments.size() == 2
        enrollments.get(0).motivation == ENROLLMENT_MOTIVATION_1
        enrollments.get(1).motivation == ENROLLMENT_MOTIVATION_2
    }

    def "get one enrollment of an activity"() {
        given:
        def volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        createEnrollment(volunteer, ENROLLMENT_MOTIVATION_1, List.of(shift))
        createEnrollment(volunteer, ENROLLMENT_MOTIVATION_2, List.of(otherShift))

        when:
        def enrollments = enrollmentService.getEnrollmentsByActivity(activity.id)

        then:
        enrollments.size() == 1
        enrollments.get(0).motivation == ENROLLMENT_MOTIVATION_1
    }

    def "activity does not exist or is null: activityId=#activityId"() {
        when:
        enrollmentService.getEnrollmentsByActivity(activityId)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        activityId || errorMessage
        null       || ErrorMessage.ACTIVITY_NOT_FOUND
        222        || ErrorMessage.ACTIVITY_NOT_FOUND
    }



    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
