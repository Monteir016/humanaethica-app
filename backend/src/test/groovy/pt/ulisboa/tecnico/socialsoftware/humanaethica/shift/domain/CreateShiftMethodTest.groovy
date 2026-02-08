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
        result.getLocation() == "This is a valid location with more than twenty characters"
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
    def "create shift with location too short: #location"() {
        given:
        shiftDto.location = location

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_INVALID

        where:
        location << [
            "",
            "Exactly nineteen ch"
        ]
    }

    @Unroll
    def "create shift with location too long: length #length"() {
        given:
        shiftDto.location = "A" * length

        when:
        new Shift(activity, shiftDto)

        then:
        def error = thrown(HEException)
        error.getErrorMessage() == SHIFT_LOCATION_INVALID

        where:
        length << [201, 250]
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
