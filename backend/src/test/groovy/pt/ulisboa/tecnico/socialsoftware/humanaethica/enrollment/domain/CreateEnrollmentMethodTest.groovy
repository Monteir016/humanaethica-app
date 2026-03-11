package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
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
        and: "shift belongs to activity"
        shift.getActivity() >> activity
    }

    def "create enrollment"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer

        when:
        def result = new Enrollment(volunteer, [shift], enrolmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.getActivity() == activity
        result.volunteer == volunteer
        and: "check that it is added"
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
        new Enrollment(volunteer, [shift], enrolmentDto)

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
        new Enrollment(volunteer, [shift], enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_AFTER_DEADLINE
    }

    def "create enrollment and violate enroll once invariant"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> volunteer
        and:
        enrolmentDto.motivation = ENROLLMENT_MOTIVATION_1

        when:
        new Enrollment(volunteer, [shift], enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED
    }

    def "create enrollment with shifts"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer
        def shift1 = Mock(Shift)
        def shift2 = Mock(Shift)
        shift1.getActivity() >> activity
        shift2.getActivity() >> activity

        when:
        def result = new Enrollment(volunteer, [shift1, shift2], enrolmentDto)

        then: "checks results"
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.enrollmentDateTime.isBefore(LocalDateTime.now())
        result.getActivity() == activity
        result.volunteer == volunteer
        result.shifts.size() == 2
        result.shifts.contains(shift1)
        result.shifts.contains(shift2)
        and: "check that it is added"
        1 * volunteer.addEnrollment(_)
        1 * shift1.addEnrollment(_)
        1 * shift2.addEnrollment(_)
    }

    def "create enrollment with shifts from different activities violates invariant"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer
        def otherActivity = Mock(Activity)
        def shift1 = Mock(Shift)
        def shift2 = Mock(Shift)
        shift1.getActivity() >> activity
        shift2.getActivity() >> otherActivity

        when:
        new Enrollment(volunteer, [shift1, shift2], enrolmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_SHIFTS_FROM_DIFFERENT_ACTIVITIES
    }

    def "create enrollment with single shift is valid"() {
        given:
        activity.getEnrollments() >> [otherEnrollment]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        otherEnrollment.getVolunteer() >> otherVolunteer
        def shift1 = Mock(Shift)
        shift1.getActivity() >> activity

        when:
        def result = new Enrollment(volunteer, [shift1], enrolmentDto)

        then:
        result.shifts.size() == 1
        result.shifts.contains(shift1)
    }

    def "get activity returns null when enrollment has no shifts"() {
        given:
        def enrollment = new Enrollment()

        expect:
        enrollment.getActivity() == null
    }

    def "get activity returns null when shifts list is null"() {
        given:
        def enrollment = new Enrollment()
        enrollment.@shifts = null

        expect:
        enrollment.getActivity() == null
    }

    def "set id sets enrollment id"() {
        given:
        def enrollment = new Enrollment()

        when:
        enrollment.setId(10)

        then:
        enrollment.getId() == 10
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
