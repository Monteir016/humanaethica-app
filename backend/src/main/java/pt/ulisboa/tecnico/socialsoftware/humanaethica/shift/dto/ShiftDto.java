package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto;

import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

public class ShiftDto {
    private Integer id;
    private String description;
    private String startingDate;
    private String endingDate;
    private Integer participantsLimit;
    private Integer activityId;

    public ShiftDto() {
    }

    public ShiftDto(Shift shift) {
        setId(shift.getId());
        setDescription(shift.getDescription());
        setStartingDate(DateHandler.toISOString(shift.getStartingDate()));
        setEndingDate(DateHandler.toISOString(shift.getEndingDate()));
        setParticipantsLimit(shift.getParticipantsLimit());
        if (shift.getActivity() != null) {
            setActivityId(shift.getActivity().getId());
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(String startingDate) {
        this.startingDate = startingDate;
    }

    public String getEndingDate() {
        return endingDate;
    }

    public void setEndingDate(String endingDate) {
        this.endingDate = endingDate;
    }

    public Integer getParticipantsLimit() {
        return participantsLimit;
    }

    public void setParticipantsLimit(Integer participantsLimit) {
        this.participantsLimit = participantsLimit;
    }

    public Integer getActivityId() {
        return activityId;
    }

    public void setActivityId(Integer activityId) {
        this.activityId = activityId;
    }
}
