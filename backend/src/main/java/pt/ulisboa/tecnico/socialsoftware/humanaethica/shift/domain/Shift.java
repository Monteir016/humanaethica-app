package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
@Table(name = "shift")
public class Shift {
    private static final int MIN_DESCRIPTION_SIZE = 20;
    private static final int MAX_DESCRIPTION_SIZE = 200;
    private static final int MIN_PARTICIPANTS_LIMIT = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String description;
    private LocalDateTime startingDate;
    private LocalDateTime endingDate;
    private Integer participantsLimit;

    @ManyToOne
    private Activity activity;

    public Shift() {
    }

    public Shift(Activity activity, ShiftDto shiftDto) {
        setActivity(activity);
        setDescription(shiftDto.getDescription());
        setStartingDate(DateHandler.toLocalDateTime(shiftDto.getStartingDate()));
        setEndingDate(DateHandler.toLocalDateTime(shiftDto.getEndingDate()));
        setParticipantsLimit(shiftDto.getParticipantsLimit());

        verifyInvariants();
    }

    private void verifyInvariants() {
        stringAttributesAreValid();
        startBeforeEnd();
        participantsLimitAboveZero();
    }

    private void stringAttributesAreValid() {
        if (this.description == null || this.description.length() < MIN_DESCRIPTION_SIZE || this.description.length() > MAX_DESCRIPTION_SIZE) {
            throw new HEException(SHIFT_DESCRIPTION_INVALID);
        }
    }

    private void startBeforeEnd() {
        if (this.startingDate == null || this.endingDate == null || !this.startingDate.isBefore(this.endingDate)) {
            throw new HEException(SHIFT_START_AFTER_END);
        }
    }

    private void participantsLimitAboveZero() {
        if (this.participantsLimit == null || this.participantsLimit < MIN_PARTICIPANTS_LIMIT) {
            throw new HEException(SHIFT_PARTICIPANTS_LIMIT_INVALID);
        }
    }

    public Integer getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartingDate() {
        return startingDate;
    }

    public void setStartingDate(LocalDateTime startingDate) {
        this.startingDate = startingDate;
    }

    public LocalDateTime getEndingDate() {
        return endingDate;
    }

    public void setEndingDate(LocalDateTime endingDate) {
        this.endingDate = endingDate;
    }

    public Integer getParticipantsLimit() {
        return participantsLimit;
    }

    public void setParticipantsLimit(Integer participantsLimit) {
        this.participantsLimit = participantsLimit;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        this.activity.addShift(this);
    }
}
