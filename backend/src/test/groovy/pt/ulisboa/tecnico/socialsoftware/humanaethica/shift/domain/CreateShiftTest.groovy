package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler

@DataJpaTest
class CreateShiftTest extends SpockTest {

    def "create Shift with valid attributes"() {
        given:
        def activity = new Activity()
        activity.setName("Test Activity")
        activity.setRegion("Test Region")
        activity.setDescription("Test Description")
        activity.setParticipantsNumberLimit(50)
        activity.setStartingDate(IN_TWO_DAYS)
        activity.setEndingDate(IN_THREE_DAYS)
        activity.setApplicationDeadline(IN_ONE_DAY)
        activityRepository.save(activity)

        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        when:
        def result = new Shift(shiftDto, activity)

        then:
        result.getDescription() == SHIFT_DESCRIPTION_1
        result.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        result.getStartingDate() == DateHandler.toLocalDateTime(shiftDto.getStartingDate())
        result.getEndingDate() == DateHandler.toLocalDateTime(shiftDto.getEndingDate())
        result.getActivity() == activity
    }

    def "create Shift and persist to database"() {
        given:
        def activity = new Activity()
        activity.setName("Test Activity")
        activity.setRegion("Test Region")
        activity.setDescription("Test Description")
        activity.setParticipantsNumberLimit(50)
        activity.setStartingDate(IN_TWO_DAYS)
        activity.setEndingDate(IN_THREE_DAYS)
        activity.setApplicationDeadline(IN_ONE_DAY)
        activityRepository.save(activity)

        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_2,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(3)
        )

        when:
        def shift = new Shift(shiftDto, activity)
        activity.addShift(shift)
        shiftRepository.save(shift)

        then:
        shift.getId() != null
        activity.getShifts().contains(shift)
        shiftRepository.findById(shift.getId()).isPresent()
    }

    def "activity has shifts association"() {
        given:
        def activity = new Activity()
        activity.setName("Test Activity")
        activity.setRegion("Test Region")
        activity.setDescription("Test Description")
        activity.setParticipantsNumberLimit(50)
        activity.setStartingDate(IN_TWO_DAYS)
        activity.setEndingDate(IN_THREE_DAYS)
        activity.setApplicationDeadline(IN_ONE_DAY)
        activityRepository.save(activity)

        def shiftDto1 = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        def shiftDto2 = createShiftDto(
                SHIFT_DESCRIPTION_2,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(3),
                IN_TWO_DAYS.plusHours(4)
        )

        when:
        def shift1 = new Shift(shiftDto1, activity)
        def shift2 = new Shift(shiftDto2, activity)
        activity.addShift(shift1)
        activity.addShift(shift2)
        shiftRepository.save(shift1)
        shiftRepository.save(shift2)

        then:
        activity.getShifts().size() == 2
        activity.getShifts().contains(shift1)
        activity.getShifts().contains(shift2)
    }

    def "test Shift getters and setters"() {
        given:
        def activity = new Activity()
        activity.setName("Test Activity")
        activity.setRegion("Test Region")
        activity.setDescription("Test Description")
        activity.setParticipantsNumberLimit(50)
        activity.setStartingDate(IN_TWO_DAYS)
        activity.setEndingDate(IN_THREE_DAYS)
        activity.setApplicationDeadline(IN_ONE_DAY)
        activityRepository.save(activity)

        def shift = new Shift()

        when:
        shift.setDescription(SHIFT_DESCRIPTION_1)
        shift.setStartingDate(IN_TWO_DAYS.plusHours(1))
        shift.setEndingDate(IN_TWO_DAYS.plusHours(2))
        shift.setParticipantsLimit(SHIFT_PARTICIPANTS_LIMIT_1)
        shift.setActivity(activity)
        shiftRepository.save(shift)

        then:
        shift.getId() != null
        shift.getDescription() == SHIFT_DESCRIPTION_1
        shift.getStartingDate() == IN_TWO_DAYS.plusHours(1)
        shift.getEndingDate() == IN_TWO_DAYS.plusHours(2)
        shift.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        shift.getActivity() == activity
    }

    def "test ShiftDto constructor from Shift"() {
        given:
        def activity = new Activity()
        activity.setName("Test Activity")
        activity.setRegion("Test Region")
        activity.setDescription("Test Description")
        activity.setParticipantsNumberLimit(50)
        activity.setStartingDate(IN_TWO_DAYS)
        activity.setEndingDate(IN_THREE_DAYS)
        activity.setApplicationDeadline(IN_ONE_DAY)
        activityRepository.save(activity)

        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        def shift = new Shift(shiftDto, activity)
        shiftRepository.save(shift)

        when:
        def resultDto = new ShiftDto(shift)

        then:
        resultDto.getId() == shift.getId()
        resultDto.getDescription() == SHIFT_DESCRIPTION_1
        resultDto.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        resultDto.getActivityId() == activity.getId()
        resultDto.getStartingDate() != null
        resultDto.getEndingDate() != null
    }

    def "test ShiftDto getters and setters"() {
        given:
        def shiftDto = new ShiftDto()

        when:
        shiftDto.setId(1)
        shiftDto.setDescription(SHIFT_DESCRIPTION_1)
        shiftDto.setStartingDate(DateHandler.toISOString(IN_TWO_DAYS.plusHours(1)))
        shiftDto.setEndingDate(DateHandler.toISOString(IN_TWO_DAYS.plusHours(2)))
        shiftDto.setParticipantsLimit(SHIFT_PARTICIPANTS_LIMIT_1)
        shiftDto.setActivityId(100)

        then:
        shiftDto.getId() == 1
        shiftDto.getDescription() == SHIFT_DESCRIPTION_1
        shiftDto.getStartingDate() != null
        shiftDto.getEndingDate() != null
        shiftDto.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        shiftDto.getActivityId() == 100
    }

    def "test ShiftDto constructor from Shift with null activity"() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_2,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(3)
        )

        def shift = new Shift()
        shift.setDescription(shiftDto.getDescription())
        shift.setStartingDate(DateHandler.toLocalDateTime(shiftDto.getStartingDate()))
        shift.setEndingDate(DateHandler.toLocalDateTime(shiftDto.getEndingDate()))
        shift.setParticipantsLimit(shiftDto.getParticipantsLimit())

        when:
        def resultDto = new ShiftDto(shift)

        then:
        resultDto.getId() == null
        resultDto.getDescription() == SHIFT_DESCRIPTION_2
        resultDto.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_2
        resultDto.getActivityId() == null
        resultDto.getStartingDate() != null
        resultDto.getEndingDate() != null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
