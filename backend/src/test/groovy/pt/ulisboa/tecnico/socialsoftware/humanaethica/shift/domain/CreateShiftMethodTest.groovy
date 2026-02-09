package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*

@DataJpaTest
class CreateShiftMethodTest extends SpockTest {
    Activity activity = Mock()
    def shiftDto

    def setup() {
        given: "shift info"
        shiftDto = new ShiftDto()
        shiftDto.startTime = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.endTime = DateHandler.toISOString(IN_TWO_DAYS)
        shiftDto.participantsLimit = 5
        shiftDto.currentParticipants = 0
        shiftDto.location = "This is a valid location with more than twenty characters"
    }

    def "create shift with valid data"() {
        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    def "create shift with null start time"() {
        given:
        shiftDto.startTime = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_START_TIME_REQUIRED
    }

    def "create shift with null end time"() {
        given:
        shiftDto.endTime = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_END_TIME_REQUIRED
    }

    def "create shift with null participants limit"() {
        given:
        shiftDto.participantsLimit = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_PARTICIPANTS_LIMIT_REQUIRED
    }

    def "create shift with null current participants"() {
        given:
        shiftDto.currentParticipants = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_CURRENT_PARTICIPANTS_REQUIRED
    }

    def "create shift with null location"() {
        given:
        shiftDto.location = null

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_REQUIRED
    }

    @Unroll
    def "create shift with valid location size, boundary analysis: length #length"() {
        given:
        shiftDto.location = "A" * length

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)

        where:
        length << [20, 100, 200]
    }

    @Unroll
    def "create shift with invalid location size: length #length"() {
        given:
        shiftDto.location = "A" * length

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_INVALID

        where:
        length << [0, 19, 201, 250]
    }

    def "create shift with valid time range, boundary analysis"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(IN_ONE_DAY)
        shiftDto.endTime = DateHandler.toISOString(IN_ONE_DAY.plusMinutes(1))

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == IN_ONE_DAY
        result.getEndTime() == IN_ONE_DAY.plusMinutes(1)
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    @Unroll
    def "create shift with invalid time range: #description"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_START_TIME_BEFORE_END_TIME

        where:
        startTime    | endTime      | description
        IN_ONE_DAY   | IN_ONE_DAY   | "start time equal to end time"
        IN_TWO_DAYS  | IN_ONE_DAY   | "start time after end time"
    }

    @Unroll
    def "create shift with start time immediately after now"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(NOW.plusMinutes(1))
        shiftDto.endTime = DateHandler.toISOString(IN_TWO_DAYS)

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == NOW.plusMinutes(1)
        result.getEndTime() == IN_TWO_DAYS
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }


    @Unroll
    def "create shift with start time in the past: #description"() {
        given:
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_START_TIME_AFTER_NOW

        where:
        startTime       | endTime         | description
        TWO_DAYS_AGO    | IN_ONE_DAY      | "start in past, end future"
        NOW             | IN_ONE_DAY      | "start now, end future"
    }

    @Unroll
    def "create shift with dates within activity range: #description"() {
        given:
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        def result = new Shift(activity, shiftDto)

        then:
        result.getStartTime() == startTime
        result.getEndTime() == endTime

        where:
        startTime                      | endTime                        | description
        IN_ONE_DAY                     | IN_TWO_DAYS                    | "start at activity start"
        IN_TWO_DAYS                    | IN_THREE_DAYS                  | "end at activity end"
    }


    @Unroll
    def "create shift with dates outside activity range: #description"() {
        given:
        activity.getStartingDate() >> IN_ONE_DAY
        activity.getEndingDate() >> IN_THREE_DAYS
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_DATES_WITHIN_ACTIVITY

        where:
        startTime                      | endTime                        | description
        IN_ONE_DAY.minusHours(10)      | IN_TWO_DAYS                    | "start before activity start"
        IN_ONE_DAY.minusMinutes(1)     | IN_TWO_DAYS                    | "start one minute before activity start"
        IN_TWO_DAYS                    | IN_THREE_DAYS.plusMinutes(1)   | "end one minute after activity end"
        IN_TWO_DAYS                    | IN_THREE_DAYS.plusDays(1)      | "end after activity end"
    }

    @Unroll
    def "create shift with valid current participants: #description"() {
        given:
        shiftDto.participantsLimit = limit
        shiftDto.currentParticipants = current

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getParticipantsLimit() == limit
        result.getCurrentParticipants() == current
        and: "invocations"
        1 * activity.addShift(_)

        where:
        limit | current | description
        5     | 0       | "current participants is zero"
        5     | 3       | "current participants within limit"
        5     | 5       | "current participants equals limit (boundary)"
    }

    @Unroll
    def "create shift with current participants exceeding limit: #description"() {
        given:
        shiftDto.participantsLimit = limit
        shiftDto.currentParticipants = current

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_CURRENT_PARTICIPANTS_EXCEEDS_LIMIT

        where:
        limit | current | description
        5     | 6       | "current participants exceeds limit by one"
        5     | 10      | "current participants exceeds limit significantly"
    }

    def "create shift and verify associations are initialized"() {
        when:
        def result = new Shift(activity, shiftDto)

        then:
        result.getEnrollments() != null
        result.getEnrollments().isEmpty()
        result.getParticipations() != null
        result.getParticipations().isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
