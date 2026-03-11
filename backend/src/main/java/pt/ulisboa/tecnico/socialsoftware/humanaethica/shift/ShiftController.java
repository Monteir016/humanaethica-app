package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;

import java.util.List;

@RestController()
public class ShiftController {
    @Autowired
    ShiftService shiftService;

    @GetMapping("/activities/{activityId}/shifts")
    public List<ShiftDto> getShiftsByActivity(@PathVariable Integer activityId) {
        return shiftService.getShiftsByActivity(activityId);
    }

    @PostMapping("/activities/{activityId}/shifts")
    @PreAuthorize("(hasRole('ROLE_MEMBER') and hasPermission(#activityId, 'ACTIVITY.MEMBER'))")
    public ShiftDto createShift(@PathVariable Integer activityId, @Valid @RequestBody ShiftDto shiftDto) {
        return shiftService.createShift(activityId, shiftDto);
    }
}
