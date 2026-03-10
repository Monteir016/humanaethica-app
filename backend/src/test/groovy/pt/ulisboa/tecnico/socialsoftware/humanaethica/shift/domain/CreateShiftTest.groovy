package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

@DataJpaTest
class CreateShiftTest extends SpockTest {
    Activity activity = Mock()

    def "create Shift with valid attributes"() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        when:
        def result = new Shift(activity, shiftDto)

        then:
        result.getDescription() == SHIFT_DESCRIPTION_1
        result.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        result.getStartingDate() == DateHandler.toLocalDateTime(shiftDto.getStartingDate())
        result.getEndingDate() == DateHandler.toLocalDateTime(shiftDto.getEndingDate())
        result.getActivity() == activity
        1 * activity.addShift(_ as Shift)
    }

    def "create Shift and set bidirectional relation"() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_2,
                SHIFT_PARTICIPANTS_LIMIT_2,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(3)
        )

        when:
        def shift = new Shift(activity, shiftDto)

        then:
        shift.getActivity() == activity
        1 * activity.addShift(_ as Shift)
    }

    def "activity receives both shifts in association"() {
        given:
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
        def shift1 = new Shift(activity, shiftDto1)
        def shift2 = new Shift(activity, shiftDto2)

        then:
        shift1.getActivity() == activity
        shift2.getActivity() == activity
        2 * activity.addShift(_ as Shift)
    }

    def "test Shift getters and setters"() {
        given:
        def shift = new Shift()

        when:
        shift.setDescription(SHIFT_DESCRIPTION_1)
        shift.setStartingDate(IN_TWO_DAYS.plusHours(1))
        shift.setEndingDate(IN_TWO_DAYS.plusHours(2))
        shift.setParticipantsLimit(SHIFT_PARTICIPANTS_LIMIT_1)
        shift.setActivity(activity)

        then:
        shift.getDescription() == SHIFT_DESCRIPTION_1
        shift.getStartingDate() == IN_TWO_DAYS.plusHours(1)
        shift.getEndingDate() == IN_TWO_DAYS.plusHours(2)
        shift.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        shift.getActivity() == activity
        1 * activity.addShift(shift)
    }

    def "test ShiftDto constructor from Shift"() {
        given:
        activity.getId() >> 100
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                IN_TWO_DAYS.plusHours(1),
                IN_TWO_DAYS.plusHours(2)
        )

        def shift = new Shift(activity, shiftDto)

        when:
        def resultDto = new ShiftDto(shift)

        then:
        resultDto.getId() == shift.getId()
        resultDto.getDescription() == SHIFT_DESCRIPTION_1
        resultDto.getParticipantsLimit() == SHIFT_PARTICIPANTS_LIMIT_1
        resultDto.getActivityId() == 100
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

    @Unroll
    def "create Shift and violate start before end invariant: startOffset=#startOffset | endOffset=#endOffset"() {
        given:
        def shiftDto = createShiftDto(
                SHIFT_DESCRIPTION_1,
                SHIFT_PARTICIPANTS_LIMIT_1,
                startOffset != null ? IN_TWO_DAYS.plusHours(startOffset) : null,
                endOffset != null ? IN_TWO_DAYS.plusHours(endOffset) : null
        )

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == ErrorMessage.SHIFT_START_AFTER_END

        where:
        startOffset | endOffset
        2           | 2          // start == end
        3           | 1          // start after end
        null        | 2          // null start
        2           | null       // null end
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
