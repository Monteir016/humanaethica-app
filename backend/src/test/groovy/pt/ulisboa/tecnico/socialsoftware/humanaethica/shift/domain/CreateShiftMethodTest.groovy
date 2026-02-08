package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.BeanConfiguration
import pt.ulisboa.tecnico.socialsoftware.humanaethica.SpockTest
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler
import spock.lang.Unroll

import java.time.LocalDateTime

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
        shiftDto.location = "Test Location"
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
        result.getLocation() == "Test Location"
        result.getEnrollments().isEmpty()
        result.getParticipations().isEmpty()
        and: "invocations"
        1 * activity.addShift(_)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfiguration {}
}
