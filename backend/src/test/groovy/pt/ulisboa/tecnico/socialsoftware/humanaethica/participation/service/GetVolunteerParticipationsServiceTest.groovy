package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class GetVolunteerParticipationsServiceTest extends SpockTest {
    def activity
    def volunteer

    def setup() {
        def institution = institutionService.getDemoInstitution()
        volunteer = createVolunteer(USER_1_NAME, USER_1_USERNAME, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)

        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        createShift(activity, SHIFT_DESCRIPTION_1, 3, ONE_DAY_AGO, NOW)

        def participationDto = new ParticipationDto()
        participationDto.volunteerRating = 4
        participationDto.volunteerReview = VOLUNTEER_REVIEW
        participationDto.volunteerId = volunteer.id

        createParticipation(activity, volunteer, participationDto)
    }

    def 'get volunteer participations returns sorted dto list'() {
        when:
        def result = participationService.getVolunteerParticipations(volunteer.id)

        then:
        result.size() == 1
        result.get(0).volunteerId == volunteer.id
        result.get(0).volunteerRating == 4
        result.get(0).volunteerReview == VOLUNTEER_REVIEW
    }

    def 'get volunteer participations with null user id fails'() {
        when:
        participationService.getVolunteerParticipations(null)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.USER_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
