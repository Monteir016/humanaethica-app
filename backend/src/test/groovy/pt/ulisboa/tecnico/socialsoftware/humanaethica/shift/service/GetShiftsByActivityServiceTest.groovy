package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.service

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class GetShiftsByActivityServiceTest extends SpockTest {
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

    def 'get shifts by activity with no shifts returns empty list'() {
        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result != null
        result.isEmpty()
    }

    def 'get shifts by activity returns all shifts ordered by startingDate'() {
        given:
        def earliestDto = createShiftDto(
                "First shift description valid",
                1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )
        def middleDto = createShiftDto(
                "Second shift description valid",
                1,
                IN_TWO_DAYS.plusHours(2),
                IN_TWO_DAYS.plusHours(3)
        )
        def latestDto = createShiftDto(
                "Third shift description valid and longer",
                1,
                IN_TWO_DAYS.plusHours(3),
                IN_TWO_DAYS.plusHours(4)
        )

        // Create out of order to verify explicit ordering in service
        shiftService.createShift(activity.id, latestDto)
        shiftService.createShift(activity.id, earliestDto)
        shiftService.createShift(activity.id, middleDto)

        when:
        def result = shiftService.getShiftsByActivity(activity.id)

        then:
        result.size() == 3
        DateHandler.toLocalDateTime(result.get(0).startingDate) == IN_TWO_DAYS.plusHours(1)
        DateHandler.toLocalDateTime(result.get(1).startingDate) == IN_TWO_DAYS.plusHours(2)
        DateHandler.toLocalDateTime(result.get(2).startingDate) == IN_TWO_DAYS.plusHours(3)
        result.get(0).description == "First shift description valid"
        result.get(1).description == "Second shift description valid"
        result.get(2).description == "Third shift description valid and longer"
    }

    @Unroll
    def 'invalid arguments: activityId=#activityId'() {
        when:
        shiftService.getShiftsByActivity(getActivityId(activityId))

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.ACTIVITY_NOT_FOUND

        where:
        activityId << [null, NO_EXIST]
    }

    def getActivityId(activityId) {
        if (activityId == EXIST)
            return activity.id
        else if (activityId == NO_EXIST)
            return 222
        else
            return null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
