package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
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
    Volunteer otherVolunteer = Mock()
    Participation otherParticipation = Mock()
    Shift shift = Mock()
    def participationDto
    
    def setup() {
        given:
        participationDto = new ParticipationDto()
        shift.getActivity() >> activity
        activity.getShifts() >> [shift]
        shift.getParticipations() >> [otherParticipation]
        activity.getNumberOfParticipatingVolunteers() >> 2
    }

    def "member creates a participation"() {
        given:
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        def result = new Participation(volunteer, shift, participationDto)

        then: "checks results"
        result.memberRating == 5
        result.memberReview ==  MEMBER_REVIEW
        result.acceptanceDate.isBefore(LocalDateTime.now())
        result.shift == shift
        result.activity == activity
        result.volunteer == volunteer
        and: "check that it is added"
        1 * shift.addParticipation(_)
        1 * volunteer.addParticipation(_)
    }

    def "create participation and violate participate once invariant"() {
        given:
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> volunteer

        when:
        new Participation(volunteer, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_VOLUNTEER_IS_ALREADY_PARTICIPATING
    }

    def "create participation and violate acceptance after deadline invariant"() {
        given:
        activity.getApplicationDeadline() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_TWO_DAYS
        activity.getParticipantsNumberLimit() >> 3
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberRating = null

        when:
        new Participation(volunteer, shift, participationDto)

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
        otherParticipation.getVolunteer() >> otherVolunteer

        when:
        new Participation(volunteer, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BEFORE_END
    }

    def "create participant and violate shift participants less or equal limit invariant"() {
        given:
        def specificShift = Mock(Shift)
        specificShift.getActivity() >> activity
        specificShift.getParticipantsLimit() >> 1
        specificShift.getParticipations() >> [otherParticipation, Mock(Participation)]
        
        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 10

        when:
        new Participation(volunteer, specificShift, participationDto)

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
        def participationsList = new ArrayList()
        for (int i = 0; i < existing + 1; i++) {
            participationsList.add(Mock(Participation))
        }
        specificShift.getParticipations() >> participationsList

        activity.getApplicationDeadline() >> TWO_DAYS_AGO
        activity.getEndingDate() >> ONE_DAY_AGO
        activity.getParticipantsNumberLimit() >> 10

        when:
        new Participation(volunteer, specificShift, participationDto)

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
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberRating = rating

        when:
        new Participation(volunteer, shift, participationDto)

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
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.volunteerRating = rating

        when:
        new Participation(volunteer, shift, participationDto)

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
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.volunteerReview = review

        when:
        new Participation(volunteer, shift, participationDto)

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
        otherParticipation.getVolunteer() >> otherVolunteer
        and:
        participationDto.memberReview = review

        when:
        new Participation(volunteer, shift, participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}