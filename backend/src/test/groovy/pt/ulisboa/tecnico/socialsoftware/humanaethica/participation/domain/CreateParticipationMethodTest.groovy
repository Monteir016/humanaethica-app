package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
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
    Volunteer volunteer = Mock()
    Enrollment enrollment = Mock()
    Volunteer otherVolunteer = Mock()
    Enrollment otherEnrollment = Mock()
    Participation otherParticipation = Mock()
    Shift shift = Mock()
    def participationDto
    
    def setup() {
        given:
        participationDto = new ParticipationDto()
        shift.getActivity() >> activity
        activity.getShifts() >> [shift]
        def initialEnrollment = Mock(Enrollment)
        initialEnrollment.getParticipation() >> otherParticipation
        shift.getEnrollments() >> [initialEnrollment]
        activity.getNumberOfParticipatingVolunteers() >> 2
        enrollment.getVolunteer() >> volunteer
        enrollment.getActivity() >> activity
        enrollment.getShifts() >> [shift]
        enrollment.getParticipation() >> Mock(Participation)
        otherEnrollment.getVolunteer() >> otherVolunteer
        otherParticipation.getEnrollment() >> otherEnrollment
    }

    def "member creates a participation"() {
        given:
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3

        when:
        def result = new Participation(enrollment, shift, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview ==  MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.shift == shift
        result.activity == activity
        result.volunteer == volunteer
        and: "check that it is added"
        1 * shift.addParticipation(_)
        1 * enrollment.setParticipation(_)
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        and:
        participationDto.memberRating = null

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE
    }

    def "create participation and violate rating before end invariant"() {
        given:
        participationDto.memberReview = MEMBER_REVIEW
        participationDto.memberRating = 5
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate shift participants less or equal limit invariant"() {
        given:
        def specificShift = Mock(Shift)
        specificShift.getActivity() >> activity
        specificShift.getParticipantsLimit() >> 1
        specificShift.getParticipations() >> [Mock(Participation), Mock(Participation)]
        and:
        def localEnrollment = Mock(Enrollment)
        localEnrollment.getShifts() >> [specificShift]

        when:
        new Participation(localEnrollment, specificShift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_CURRENT_PARTICIPANTS_EXCEEDS_LIMIT
    }

    @Unroll
    def "create participant with success (limit boundary): existing=#existing, limit=#limit"() {
        given:
        def specificShift = Mock(Shift)
        specificShift.getActivity() >> activity
        specificShift.getParticipantsLimit() >> limit
        def participationList = new ArrayList()
        for (int i = 0; i < existing; i++) {
            def p = Mock(Participation)
            participationList.add(p)
        }
        specificShift.getParticipations() >> participationList
        and:
        def localEnrollment = Mock(Enrollment)
        localEnrollment.getShifts() >> [specificShift]

        when:
        new Participation(localEnrollment, specificShift, participationDto)

        then:
        noExceptionThrown()

        where:
        existing | limit
        0        | 10
        5        | 10
        9        | 10
    }

    @Unroll
    def "create participation and violate member rating in range 1..5: rating=#rating"() {
        given:
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        participationDto.memberRating = rating

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer rating in range 1..5: rating=#rating"() {
        given:
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        participationDto.volunteerRating = rating

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "create participation and violate volunteer review length: review=#review"() {
        given:
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        participationDto.volunteerReview = review

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @Unroll
    def "create participation and violate member review length: review=#review"() {
        given:
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        participationDto.memberReview = review

        when:
        new Participation(enrollment, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    def "create participation and violate shift belongs to enrollment invariant"() {
        given:
        def otherShift = Mock(Shift)
        otherShift.getActivity() >> activity
        otherShift.getParticipantsLimit() >> 10
        otherShift.getParticipations() >> []
        and:
        def localEnrollment = Mock(Enrollment)
        localEnrollment.getShifts() >> [shift]
        localEnrollment.getActivity() >> activity

        when:
        new Participation(localEnrollment, otherShift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_SHIFT_NOT_IN_ENROLLMENT
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}