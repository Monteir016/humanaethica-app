package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift
import spock.lang.Unroll

@DataJpaTest
class CreateEnrollmentServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def volunteer

    def setup() {
        def institution = institutionService.getDemoInstitution()
        volunteer = authUserService.loginDemoVolunteerAuth().getUser()

        def activityDto = createActivityDto(ACTIVITY_NAME_1,ACTIVITY_REGION_1,1,ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS,IN_THREE_DAYS,null)

        def activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
    }

    def 'create enrollment' () {
        given:
        def activity = activityRepository.findAll().get(0)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 1, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shift.id]

        when:
        def result = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        result.motivation == ENROLLMENT_MOTIVATION_1
        and:
        enrollmentRepository.findAll().size() == 1
        def storedEnrollment = enrollmentRepository.findAll().get(0)
        storedEnrollment.motivation == ENROLLMENT_MOTIVATION_1
        storedEnrollment.getActivity().id == activity.id
        storedEnrollment.volunteer.id == volunteer.id
    }

    @Unroll
    def 'invalid arguments: volunteerId=#volunteerId'() {
        given:
        def activity = activityRepository.findAll().get(0)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 1, IN_TWO_DAYS, IN_THREE_DAYS)
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shift.id]

        when:
        enrollmentService.createEnrollment(getVolunteerId(volunteerId), getEnrollmentDto(enrollmentValue, enrollmentDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        enrollmentRepository.findAll().size() == 0

        where:
        volunteerId | enrollmentValue || errorMessage
        null        | EXIST           || ErrorMessage.USER_NOT_FOUND
        NO_EXIST    | EXIST           || ErrorMessage.USER_NOT_FOUND
        EXIST       | null            || ErrorMessage.ENROLLMENT_REQUIRES_MOTIVATION
    }

    def getVolunteerId(volunteerId) {
        if (volunteerId == EXIST)
            return volunteer.id
        else if (volunteerId == NO_EXIST)
            return 222
        else
            return null
    }

    def getEnrollmentDto(value, enrollmentDto) {
        if (value == EXIST) {
            return enrollmentDto
        }
        return null
    }

    def 'create enrollment with shifts' () {
        given:
        def activity = activityRepository.findAll().get(0)
        def shift = createShift(activity, SHIFT_DESCRIPTION_1, 1, IN_TWO_DAYS, IN_THREE_DAYS)
        and:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shift.id]

        when:
        def result = enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        result.motivation == ENROLLMENT_MOTIVATION_1
        result.shiftIds.size() == 1
        result.shiftIds.get(0) == shift.id
        and:
        enrollmentRepository.findAll().size() == 1
        def storedEnrollment = enrollmentRepository.findAll().get(0)
        storedEnrollment.shifts.size() == 1
        storedEnrollment.shifts.get(0).id == shift.id
    }

    def 'create enrollment and fail with missing shifts'() {
        given:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = []

        when:
        enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_REQUIRES_SHIFTS
        enrollmentRepository.findAll().size() == 0
    }

    def 'create enrollment and fail with null shifts'() {
        given:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = null

        when:
        enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_REQUIRES_SHIFTS
        enrollmentRepository.findAll().size() == 0
    }

    def 'create enrollment and fail when shift does not exist'() {
        given:
        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [222]

        when:
        enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_NOT_FOUND
        enrollmentRepository.findAll().size() == 0
    }

    def 'create enrollment and fail when shifts belong to different activities'() {
        given:
        def institution = institutionService.getDemoInstitution()
        def activityOne = activityRepository.findAll().get(0)

        def activityDto = createActivityDto(ACTIVITY_NAME_2, ACTIVITY_REGION_1, 1, ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY, IN_TWO_DAYS, IN_THREE_DAYS, null)
        def activityTwo = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activityTwo)

        def shiftOne = createShift(activityOne, SHIFT_DESCRIPTION_1, 1, IN_TWO_DAYS, IN_THREE_DAYS)
        def shiftTwo = createShift(activityTwo, SHIFT_DESCRIPTION_2, 1, IN_TWO_DAYS, IN_THREE_DAYS)

        def enrollmentDto = new EnrollmentDto()
        enrollmentDto.motivation = ENROLLMENT_MOTIVATION_1
        enrollmentDto.shiftIds = [shiftOne.id, shiftTwo.id]

        when:
        enrollmentService.createEnrollment(volunteer.id, enrollmentDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ENROLLMENT_SHIFTS_FROM_DIFFERENT_ACTIVITIES
        enrollmentRepository.findAll().size() == 0
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
