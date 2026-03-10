package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.dto.ActivityDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import spock.lang.Unroll

@DataJpaTest
class CreateShiftServiceTest extends SpockTest {
    public static final String EXIST = 'exist'
    public static final String NO_EXIST = 'noExist'
    def activity

    def setup() {
        def institution = institutionService.getDemoInstitution()
        def activityDto = createActivityDto(
                ACTIVITY_NAME_1,
                ACTIVITY_REGION_1,
                5,
                ACTIVITY_DESCRIPTION_1,
                IN_ONE_DAY,
                IN_TWO_DAYS,
                IN_THREE_DAYS,
                null
        )

        activity = new Activity(activityDto, institution, new ArrayList<>())
        activityRepository.save(activity)
    }

    def 'create shift'() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        when:
        def result = shiftService.createShift(activity.id, shiftDto)

        then:
        result.description == SHIFT_DESCRIPTION_1
        result.participantsLimit == SHIFT_PARTICIPANTS_LIMIT_2
        result.activityId == activity.id
        and:
        shiftRepository.findAll().size() == 1
        def storedShift = shiftRepository.findAll().get(0)
        storedShift.description == SHIFT_DESCRIPTION_1
        storedShift.participantsLimit == SHIFT_PARTICIPANTS_LIMIT_2
        storedShift.activity.id == activity.id
    }

    @Unroll
    def 'invalid arguments: activityId=#activityId | shiftDto=#shiftDtoValue'() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        when:
        shiftService.createShift(getActivityId(activityId), getShiftDto(shiftDtoValue, shiftDto))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == errorMessage
        and:
        shiftRepository.findAll().size() == 0

        where:
        activityId | shiftDtoValue || errorMessage
        null       | EXIST         || ErrorMessage.ACTIVITY_NOT_FOUND
        NO_EXIST   | EXIST         || ErrorMessage.ACTIVITY_NOT_FOUND
        EXIST      | null          || ErrorMessage.SHIFT_REQUIRES_INFORMATION
    }

    def 'create shift with non-approved activity state'() {
        given:
        activity.setState(Activity.State.SUSPENDED)
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        when:
        shiftService.createShift(activity.id, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_ACTIVITY_NOT_APPROVED
        and:
        shiftRepository.findAll().size() == 0
    }

    def getActivityId(activityId) {
        if (activityId == EXIST)
            return activity.id
        else if (activityId == NO_EXIST)
            return 222
        else
            return null
    }

    def getShiftDto(value, shiftDto) {
        if (value == EXIST) {
            return shiftDto
        }
        return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
