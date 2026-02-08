package pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.dto.ShiftDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.utils.DateHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Shift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer participantsLimit;
    private Integer currentParticipants;
    private String location;

    @ManyToOne
    private Activity activity;

    @OneToMany(mappedBy = "shift")
    private List<Enrollment> enrollments = new ArrayList<>();

    @OneToMany(mappedBy = "shift")
    private List<Participation> participations = new ArrayList<>();

    public Shift() {
    }

    public Shift(Activity activity, ShiftDto shiftDto) {
        setActivity(activity);
        setStartTime(DateHandler.toLocalDateTime(shiftDto.getStartTime()));
        setEndTime(DateHandler.toLocalDateTime(shiftDto.getEndTime()));
        setParticipantsLimit(shiftDto.getParticipantsLimit());
        setCurrentParticipants(shiftDto.getCurrentParticipants());
        setLocation(shiftDto.getLocation());
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getParticipantsLimit() {
        return participantsLimit;
    }

    public void setParticipantsLimit(Integer participantsLimit) {
        this.participantsLimit = participantsLimit;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
        if (activity != null) {
            activity.addShift(this);
        }
    }

    public List<Enrollment> getEnrollments() {
        return enrollments;
    }

    public void addEnrollment(Enrollment enrollment) {
        this.enrollments.add(enrollment);
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public void addParticipation(Participation participation) {
        this.participations.add(participation);
    }
}
