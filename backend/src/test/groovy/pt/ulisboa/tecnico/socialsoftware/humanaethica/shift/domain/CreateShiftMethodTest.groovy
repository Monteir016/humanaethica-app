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

    @Unroll
    def "create shift with valid data: location=#locationLength chars, time=#timeDescription"() {
        given:
        shiftDto.location = "A" * locationLength
        shiftDto.startTime = DateHandler.toISOString(startTime)
        shiftDto.endTime = DateHandler.toISOString(endTime)

        when:
        def result = new Shift(activity, shiftDto)

        then: "check result"
        result.getActivity() == activity
        result.getStartTime() == startTime
        result.getEndTime() == endTime
        result.getParticipantsLimit() == 5
        result.getCurrentParticipants() == 0
        result.getLocation() == shiftDto.location
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)

        where:
        locationLength | startTime       | endTime                      | timeDescription
        100            | IN_ONE_DAY      | IN_TWO_DAYS                  | "all middle values"
        20             | IN_ONE_DAY      | IN_TWO_DAYS                  | "length is 20"
        200            | TWO_DAYS_AGO    | NOW                          | "length is 200"
        100            | NOW             | NOW.plusMinutes(1)           | "one minute interval"
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
