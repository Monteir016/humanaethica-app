package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ENROLLMENT_AT_LEAST_ONE_SHIFT
import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.ENROLLMENT_SHIFTS_MUST_BELONG_TO_SAME_ACTIVITY
import spock.lang.Unroll

import java.time.LocalDateTime

@DataJpaTest
class CreateEnrollmentMethodTest extends SpockTest {
    Activity activity = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Enrollment otherEnrollment = Mock()
    Shift shift = Mock()
    def enrolmentDto

    def setup() {
        given: "enrolment info"
        enrolmentDto = new EnrollmentDto()
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1
        and:
        shift.getActivity() >> activity
    }

    def "create enrollment"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer
        
        when:
        def result = new Enrollment(activity, volunteer, List.of(shift), enrolmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.activity == activity
        result.volunteer == volunteer
        result.getShifts().size() == 1
        result.getShifts().contains(shift)
        and: "check that it is added"
        1 * activity.addEnrollment(_)
        1 * volunteer.addEnrollment(_)
        1 * shift.addEnrollment(_)
    }

    @Unroll
    def "create enrollment and violate motivation is required invariant: motivation=#motivation"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer
        and:
        enrolmentDto.motivation = motivation

        when:
        new Enrollment(activity, volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage

        where:
        motivation || errorMessage
        null       || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "   "      || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
        "< 10"     || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }

    def "create enrollment and violate enrollment before deadline invariant"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        otherEnrollment.getVolunteer() >> otherVolunteer
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(activity, volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    def "create enrollment with shift from another activity"() {
        given:
        Activity otherActivity = Mock()
        activity.getEnrollments() >> []
        activity.getApplicationDeadline() >> IN_ONE_DAY
        and:
        Shift otherShift = Mock()
        otherShift.getActivity() >> otherActivity
        and: "an enrollment dto"
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.setMotivation(ENROLLMENT_MOTIVATION_1)

        when:
        new Enrollment(activity, volunteer, List.of(shift, otherShift), enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ENROLLMENT_SHIFTS_MUST_BELONG_TO_SAME_ACTIVITY
    }

    def "create enrollment and violate enroll once invariant"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> volunteer
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(activity, volunteer, List.of(shift), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED
    }

    def "create enrollment and violate at least one shift invariant"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        new Enrollment(activity, volunteer, new ArrayList<>(), enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AT_LEAST_ONE_SHIFT
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}