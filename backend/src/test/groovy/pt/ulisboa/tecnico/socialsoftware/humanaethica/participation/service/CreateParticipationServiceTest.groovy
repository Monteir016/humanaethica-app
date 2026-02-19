package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment

import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import spock.lang.Unroll
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired

import java.time.LocalDateTime

@DataJpaTest
class CreateParticipationServiceTest extends SpockTest {
    @Autowired
    EntityManager entityManager
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def volunteer
    def member
    def activity
    def enrollment

    def setup() {
        def institution = institutionService.getDemoInstitution()
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()
        member = authUserService.loginDemoMemberAuth().getUser()

        and: 'create activity with past dates'
        activity = createActivity(institution, ACTIVITY_NAME_1, ACTIVITY_REGION_1, 3, ACTIVITY_DESCRIPTION_1,
                THREE_DAYS_AGO, TWO_DAYS_AGO, ONE_DAY_AGO)

        and: 'create shift within activity dates'
        def shift = createShift(activity, TWO_DAYS_AGO, ONE_DAY_AGO, 3, SHIFT_LOCATION)

        and: 'create enrollment bypassing constructor invariants'
        volunteer = userRepository.findById(volunteer.getId()).get()
        enrollment = createEnrollmentBypassInvariantsValidation(volunteer, [shift], ENROLLMENT_MOTIVATION_1, THREE_DAYS_AGO.minusDays(1))

        entityManager.flush()
        entityManager.clear()

        volunteer = userRepository.findById(volunteer.getId()).get()
        activity = activityRepository.findById(activity.getId()).get()
        enrollment = enrollmentRepository.findById(enrollment.getId()).get()
    }

    def 'create participation as member' () {
        given:
        def participationDto = createParticipationDto(5, MEMBER_REVIEW, null, null)
        participationDto.volunteerId = volunteer.id
        participationDto.shiftId = activity.getShifts().get(0).getId()

        when:
        def result = participationService.createParticipation(activity.getShifts().get(0).getId(), enrollment.getId(), participationDto)

        then:
        result.memberRating == 5
        result.memberReview == MEMBER_REVIEW
        and:
        participationRepository.findAll().size() == 1
        def storedParticipation = participationRepository.findAll().get(0)
        storedParticipation.memberRating == 5
        storedParticipation.memberReview == MEMBER_REVIEW
        storedParticipation.acceptanceDate.isBefore(LocalDateTime.now())
        storedParticipation.activity.id == activity.id
        storedParticipation.volunteer.id == volunteer.id
    }

    @Unroll
    def 'invalid arguments: enrollmentId=#enrollmentId | shiftId=#shiftId'() {
        given:
        def participationDto = createParticipationDto(5, MEMBER_REVIEW, null, null)
        participationDto.volunteerId = volunteer.id
        participationDto.shiftId = activity.getShifts().get(0).getId()

        when:
        participationService.createParticipation(getShiftId(shiftId), getEnrollmentId(enrollmentId), getParticipationDto(participationValue,participationDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        participationRepository.findAll().size() == 0

        where:
        enrollmentId | shiftId    | participationValue || errorMessage
        null         | EXIST      | EXIST              || ErrorMessage.ENROLLMENT_NOT_FOUND
        NO_EXIST     | EXIST      | EXIST              || ErrorMessage.ENROLLMENT_NOT_FOUND
        EXIST        | null       | EXIST              || ErrorMessage.SHIFT_NOT_FOUND
        EXIST        | NO_EXIST   | EXIST              || ErrorMessage.SHIFT_NOT_FOUND
        EXIST        | EXIST      | null               || ErrorMessage.PARTICIPATION_REQUIRES_INFORMATION
    }

    @Unroll
    def 'invalid arguments: rating=#rating | review=#review'() {
        given:
        def participationDto = createParticipationDto(null, null, rating, review)
        participationDto.volunteerId = volunteer.id
        participationDto.shiftId = activity.getShifts().get(0).getId()

        when:
        participationService.createParticipation(activity.getShifts().get(0).getId(), enrollment.getId(), participationDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        entityManager.clear()
        participationRepository.findAll().size() == 0

        where:
        review                              | rating || errorMessage
        "A".repeat(MAX_REVIEW_LENGTH + 1)   | 5      || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        ""                                  | 5      || ErrorMessage.PARTICIPATION_REVIEW_LENGTH_INVALID
        VOLUNTEER_REVIEW                    | -1     || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
        VOLUNTEER_REVIEW                    | 10     || ErrorMessage.PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE
    }

    def getEnrollmentId(enrollmentId) {
        if (enrollmentId == EXIST)
            return enrollment.getId()
        else if (enrollmentId == NO_EXIST)
            return 222
        else
            return null
    }

    def getShiftId(shiftId) {
        if (shiftId == EXIST)
            return activity.getShifts().get(0).getId()
        else if (shiftId == NO_EXIST)
            return 222
        else
            return null
    }

    def getParticipationDto(value, participationDto) {
        if (value == EXIST) {
            return participationDto
        }
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
