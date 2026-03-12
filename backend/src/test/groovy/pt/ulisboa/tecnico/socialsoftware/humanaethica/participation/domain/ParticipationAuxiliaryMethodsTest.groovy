package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.auth.domain.AuthUser
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.User

@DataJpaTest
class ParticipationAuxiliaryMethodsTest extends SpockTest {
    def "getActivity returns shift activity when shift exists"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 2, ONE_DAY_AGO, NOW)
        def participation = new Participation()

        when:
        participation.setShift(shift)

        then:
        participation.getActivity() == activity
        shift.getParticipations().contains(participation)
    }

    def "getActivity returns enrollment activity when shift is null"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 2, ONE_DAY_AGO, NOW)
        def volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment = createEnrollment(activity, volunteer, ENROLLMENT_MOTIVATION_1)
        def participation = new Participation()

        when:
        participation.setEnrollment(enrollment)
        participation.setShift(null)

        then:
        participation.getActivity() == activity
        enrollment.getParticipations().contains(participation)
    }

    def "getActivity returns null when shift and enrollment are null"() {
        given:
        def participation = new Participation()

        expect:
        participation.getActivity() == null
    }

    def "setId sets participation id"() {
        given:
        def participation = new Participation()

        when:
        participation.setId(10)

        then:
        participation.getId() == 10
    }

    def "setShift with null does not add to shift"() {
        given:
        def participation = new Participation()

        when:
        participation.setShift(null)

        then:
        participation.getShift() == null
    }

    def "setEnrollment with null does not add to enrollment"() {
        given:
        def participation = new Participation()

        when:
        participation.setEnrollment(null)

        then:
        participation.getEnrollment() == null
    }

    def "getVolunteer returns enrollment volunteer"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment = createEnrollment(activity, volunteer, ENROLLMENT_MOTIVATION_1)
        def participation = new Participation()

        when:
        participation.setEnrollment(enrollment)

        then:
        participation.getVolunteer() == volunteer
    }

    def "getVolunteer returns null when enrollment is null"() {
        given:
        def participation = new Participation()

        expect:
        participation.getVolunteer() == null
    }

    def "enrollmentContainsShift does nothing when enrollment exists but shift is null"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment = createEnrollment(activity, volunteer, ENROLLMENT_MOTIVATION_1)
        def participation = new Participation()
        participation.setEnrollment(enrollment)
        participation.setShift(null)

        when:
        def method = Participation.class.getDeclaredMethod("enrollmentContainsShift")
        method.setAccessible(true)
        method.invoke(participation)

        then:
        noExceptionThrown()
    }

    def "enrollmentContainsShift does nothing when enrollment is null and shift exists"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 2, ONE_DAY_AGO, NOW)
        def participation = new Participation()
        participation.setShift(shift)
        participation.setEnrollment(null)

        when:
        def method = Participation.class.getDeclaredMethod("enrollmentContainsShift")
        method.setAccessible(true)
        method.invoke(participation)

        then:
        noExceptionThrown()
    }

    def "delete without shift and enrollment does not fail"() {
        given:
        def participation = new Participation()

        when:
        participation.delete()

        then:
        noExceptionThrown()
    }

    def "delete with shift and enrollment removes from all associations"() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                TWO_DAYS_AGO, ONE_DAY_AGO, NOW, null)
        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 2, ONE_DAY_AGO, NOW)
        def volunteer = createVolunteer(USER_1_NAME, USER_1_PASSWORD, USER_1_EMAIL, AuthUser.Type.NORMAL, User.State.APPROVED)
        def enrollment = createEnrollment(activity, volunteer, ENROLLMENT_MOTIVATION_1)
        def participation = new Participation()
        participation.setShift(shift)
        participation.setEnrollment(enrollment)

        when:
        participation.delete()

        then:
        !shift.getParticipations().contains(participation)
        !enrollment.getParticipations().contains(participation)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
