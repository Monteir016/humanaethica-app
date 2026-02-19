package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.institution.domain.Institution
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.theme.domain.Theme
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class DeleteParticipationMethodTest extends SpockTest {
    Institution institution = Mock()
    Theme theme = Mock()
    Activity otherActivity = Mock()

    def activity
    def shift
    def volunteer
    def participation
    def otherParticipation
    def participationDto

    def setup() {
        otherActivity.getName() >> ACTIVITY_NAME_2
        theme.getState() >> Theme.State.APPROVED
        institution.getActivities() >> [otherActivity]
        given: "an activity"
        def themes = [theme]
        def deadline = NOW.plusHours(1)
        def start = NOW.plusDays(1)
        def end = NOW.plusDays(3)
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, NOW.minusHours(1), start, end, themes)
        and:
        shift = createShift(activity, NOW.plusDays(1).plusHours(1), NOW.plusDays(2), 2, SHIFT_LOCATION)
        and: "a volunteer"
        volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and: "an enrollment"
        def enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], "Motivation necessary for enrollment", NOW.minusHours(2))
        and: "a participation"
        participationDto = createParticipationDto(null, null, null, null)
        participation = new Participation(enrollment, shift, participationDto)
    }

    def "delete participation"() {
        when: "a participation is deleted"
        participation.delete()

        then: "checks results"
        shift.getParticipations().size() == 0
        volunteer.getParticipations().size() == 0
    }

    def "delete one of multiple participations in activity"() {
        given: "another participation for the same activity"
        def otherVolunteer = createVolunteer(USER_2_NAME, USER_2_PASSWORD, USER_2_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        and:
        def otherEnrollment = createEnrollmentBypassInvariantsValidation(otherVolunteer, [shift], "Another valid motivation", NOW.minusHours(2))
        and:
        otherParticipation = new Participation(otherEnrollment, shift, participationDto)

        when: "one participation is deleted"
        participation.delete()

        then: "the other participation remains"
        shift.getParticipations().size() == 1
        shift.getParticipations().contains(otherParticipation)
    }

    def "delete one of multiple participations in volunteer"() {
        given: "another participation for the same volunteer"
        def themes = [theme]
        def deadline = NOW.plusHours(1)
        def start = NOW.plusDays(1)
        def end = NOW.plusDays(3)
        def otherActivity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 2, ACTIVITY_DESCRIPTION_1, NOW.minusHours(1), start, end, themes)
        def otherShift = createShift(otherActivity, start.plusHours(1), NOW.plusDays(2), 2, SHIFT_LOCATION)
        participationDto = createParticipationDto(null, null, null, null)
        and:
        def otherEnrollment = createEnrollmentBypassInvariantsValidation(volunteer, [otherShift], "Motivation again", NOW.minusHours(2))

        otherParticipation = new Participation(otherEnrollment, otherShift, participationDto)

        when: "one participation is deleted"
        participation.delete()

        then: "the other participation remains"
        volunteer.getParticipations().size() == 1
        volunteer.getParticipations().contains(otherParticipation)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}