package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer
import spock.lang.Unroll

import java.time.LocalDateTime


@DataJpaTest
class UpdateVolunteerRatingParticipationMethodTest extends SpockTest {
    Activity activity
    Activity otherActivity = Mock()
    Volunteer volunteer = Mock()
    Institution institution = Mock()
    Theme theme = Mock()
    def themes = [theme]
    def participation
    def participationDto
    def participationDtoUpdated


    def setup() {
        volunteer.getEnrollments() >> []
        otherActivity.getName() >> ACTIVITY_NAME_2
        institution.getActivities() >> [otherActivity]
        theme.getState() >> Theme.State.APPROVED
        def NOW = LocalDateTime.now()
        and:
        def deadline = NOW.plusHours(1)
        def start = NOW.plusDays(1)
        def end = NOW.plusDays(3)
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, deadline, start, end, themes)
        and:
        def shift = createShift(activity, NOW.plusDays(1).plusHours(1), NOW.plusDays(2), 2, SHIFT_LOCATION)
        and:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.setMotivation("Motivation needed >= 10 chars")
        def enrollment = new Enrollment(volunteer, [shift], enrollmentDto)
        enrollment.setEnrollmentDateTime(NOW.minusDays(5))
        enrollmentRepository.save(enrollment)

        activity.setStartingDate(NOW.minusDays(3))
        activity.setEndingDate(NOW.minusDays(1))
        activity.setApplicationDeadline(NOW.minusDays(4))
        
        shift.setStartTime(NOW.minusDays(3).plusHours(1))
        shift.setEndTime(NOW.minusDays(2))
        shiftRepository.save(shift)
        and:
        participationDto = new ParticipationDto()
        participationDto.memberRating = 5
        participationDto.memberReview = MEMBER_REVIEW
        participation = new Participation(enrollment, shift, participationDto)
        participationDtoUpdated = new ParticipationDto()
    }

    def "volunteer updates a participation"() {
        given:
        participationDtoUpdated.volunteerRating = 3
        participationDtoUpdated.volunteerReview = VOLUNTEER_REVIEW

        when:
        participation.volunteerRating(participationDtoUpdated)

        then: "checks results"
        participation.volunteerRating == 3
        participation.volunteerReview == VOLUNTEER_REVIEW
        participation.acceptanceDate.isBefore(LocalDateTime.now())
        participation.activity == activity
        participation.volunteer == volunteer
    }

    @Unroll
    def "update participation and violate rating in range 1..5: rating=#rating"(){
        given:
        participationDtoUpdated.volunteerRating = rating
        participationDtoUpdated.volunteerReview = VOLUNTEER_REVIEW

        when:
        participation.volunteerRating(participationDtoUpdated)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE

        where:
        rating << [-5,0,6,20]
    }

    @Unroll
    def "update participation and violate review length: review=#review"(){
        given:
        participationDtoUpdated.volunteerRating = 5
        participationDtoUpdated.volunteerReview = review

        when:
        participation.volunteerRating(participationDtoUpdated)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID

        where:
        review << ["", "123456789","a".repeat(MAX_REVIEW_LENGTH + 1)]
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}