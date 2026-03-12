package pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.dto.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain.Participation;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Integer id;
    private String motivation;
    private LocalDateTime enrollmentDateTime;
    @ManyToOne
    private Volunteer volunteer;
    @ManyToMany
    private List<Shift> shifts = new ArrayList<>();
    @OneToMany(mappedBy = "enrollment")
    private List<Participation> participations = new ArrayList<>();

    public Enrollment() {}

    public Enrollment(Volunteer volunteer, List<Shift> shifts, EnrollmentDto enrollmentDto) {
        setVolunteer(volunteer);
        setShifts(shifts);
        setMotivation(enrollmentDto.getMotivation());
        setEnrollmentDateTime(LocalDateTime.now());

        verifyInvariants();
    }

    public void update(EnrollmentDto enrollmentDto) {  
        setMotivation(enrollmentDto.getMotivation());

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public void delete(){
        volunteer.removeEnrollment(this);
        for (Shift shift : this.shifts) {
            shift.getEnrollments().remove(this);
        }

        editOrDeleteEnrollmentBeforeDeadline();
        verifyInvariants();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }

    public LocalDateTime getEnrollmentDateTime() {
        return enrollmentDateTime;
    }

    public void setEnrollmentDateTime(LocalDateTime enrollmentDateTime) {
        this.enrollmentDateTime = enrollmentDateTime;
    }

    public Activity getActivity() {
        if (this.shifts == null || this.shifts.isEmpty()) {
            return null;
        }
        return this.shifts.get(0).getActivity();
    }

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
        this.volunteer.addEnrollment(this);
    }

    public List<Shift> getShifts() {
        return shifts;
    }

    public void setShifts(List<Shift> shifts) {
        this.shifts = shifts;
        for (Shift shift : shifts) {
            shift.addEnrollment(this);
        }
    }

    public List<Participation> getParticipations() {
        return participations;
    }

    public void addParticipation(Participation participation) {
        this.participations.add(participation);
    }

    public void deleteParticipation(Participation participation) {
        this.participations.remove(participation);
    }

    private void verifyInvariants() {
        motivationIsRequired();
        enrollOnce();
        enrollBeforeDeadline();
        shiftsBelongToSameActivity();
    }

    private void motivationIsRequired() {
        if (this.motivation == null || this.motivation.trim().length() < 10) {
            throw new HEException(ENROLLMENT_REQUIRES_MOTIVATION);
        }
    }

    private void enrollOnce() {
        if (getActivity().getEnrollments().stream()
                .anyMatch(enrollment -> enrollment != this && enrollment.getVolunteer() == this.volunteer)) {
            throw new HEException(ENROLLMENT_VOLUNTEER_IS_ALREADY_ENROLLED);
        }
    }

    private void enrollBeforeDeadline() {
        if (this.enrollmentDateTime.isAfter(getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }

    private void shiftsBelongToSameActivity() {
        if (this.shifts.size() > 1) {
            Activity expected = this.shifts.get(0).getActivity();
            if (this.shifts.stream().anyMatch(s -> s.getActivity() != expected)) {
                throw new HEException(ENROLLMENT_SHIFTS_FROM_DIFFERENT_ACTIVITIES);
            }
        }
    }

    private void editOrDeleteEnrollmentBeforeDeadline() {
        if (LocalDateTime.now().isAfter(getActivity().getApplicationDeadline())) {
            throw new HEException(ENROLLMENT_AFTER_DEADLINE);
        }
    }
}
