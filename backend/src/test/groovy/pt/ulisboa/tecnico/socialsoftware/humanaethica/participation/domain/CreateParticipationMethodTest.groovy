package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime


@DataJpaTest
class CreateParticipationMethodTest extends SpockTest {
    Activity activity = Mock()
    Shift shift = Mock()
    Volunteer volunteer = Mock()
    Volunteer otherVolunteer = Mock()
    Enrollment enrollment = Mock()
    Participation otherParticipation = Mock()
    def participationDto

    def setup() {
        given:
        participationDto = new ParticipationDto()
        shift.getActivity() >> activity
        activity.getShifts() >> [shift]
        enrollment.getVolunteer() >> volunteer
        enrollment.getShifts() >> [shift]
    }

    def "member creates a participation"() {
        given:
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        def result = new Participation(activity, enrollment, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview ==  MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.activity == activity
        result.shift == shift
        result.volunteer == volunteer
        and: "check that it is added"
        1 * shift.addParticipation(_)
        1 * enrollment.addParticipation(_)
    }

    def "create participation and violate participate once invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> volunteer

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_VOLUNTEER_IS_ALREADY_PARTICIPATING
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberRating = null

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE
    }

    def "create participation and violate rating before end invariant"() {
        given:
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.memberRating = 5
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> ONE_DAY_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate number of participants less or equal limit invariant"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation, Mock(Participation)]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 1
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_IS_FULL
    }

    @Unroll
    def "create participation and violate member rating in range 1..5: rating=#rating"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberRating = rating

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer rating in range 1..5: rating=#rating"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.volunteerRating = rating

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer review length: review=#review"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.volunteerReview = review

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @Unroll
    def "create participation and violate member review length: review=#review"() {
        given:
        activity.getParticipations() >> [otherParticipation]
        shift.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shift.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberReview = review

        when:
        new Participation(activity, enrollment, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    def "create participation with enrollment containing the shift"() {
        given:
        def enrollment = Mock(Enrollment)
        def shiftInEnrollment = Mock(Shift)
        shiftInEnrollment.getActivity() >> activity
        enrollment.getVolunteer() >> volunteer
        enrollment.getShifts() >> [shiftInEnrollment]
        activity.getShifts() >> [shiftInEnrollment]
        activity.getParticipations() >> [otherParticipation]
        shiftInEnrollment.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shiftInEnrollment.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        def result = new Participation(activity, enrollment, shiftInEnrollment, participationDto)

        then: "checks results"
        result.enrollment == enrollment
        result.shift == shiftInEnrollment
        result.activity == activity
        result.volunteer == volunteer
    }

    @Unroll
    def "create participation and violate enrollment must have shift invariant: #description"() {
        given:
        def enrollment = Mock(Enrollment)
        def shiftInParticipation = Mock(Shift)
        def otherShift = Mock(Shift)
        shiftInParticipation.getActivity() >> activity
        otherShift.getActivity() >> activity
        enrollment.getShifts() >> enrollmentShifts
        activity.getParticipations() >> [otherParticipation]
        shiftInParticipation.getParticipations() >> [otherParticipation]
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        shiftInParticipation.getParticipantsLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        activity.getShifts() >> [shiftInParticipation]

        when:
        new Participation(activity, enrollment, shiftInParticipation, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ENROLLMENT_DOES_NOT_CONTAIN_SHIFT

        where:
        description                    | enrollmentShifts
        "enrollment has no shifts"     | []
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
