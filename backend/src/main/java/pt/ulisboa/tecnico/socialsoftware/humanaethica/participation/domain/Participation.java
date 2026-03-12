package pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.domain;

import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.activity.domain.Activity;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.enrollment.domain.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.HEException;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.participation.dto.ParticipationDto;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.shift.domain.Shift;
import pt.ulisboa.tecnico.socialsoftware.humanaethica.user.domain.Volunteer;

import java.time.LocalDateTime;

import static pt.ulisboa.tecnico.socialsoftware.humanaethica.exceptions.ErrorMessage.*;

@Entity
public class Participation {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Integer id;
    private LocalDateTime acceptanceDate;
    private Integer volunteerRating;
    private Integer memberRating;
    private String volunteerReview;
    private String memberReview;

    @ManyToOne
    private Shift shift;
    @ManyToOne
    private Enrollment enrollment;

    public Participation() {}

    public Participation(Activity activity, Enrollment enrollment, ParticipationDto participationDto) {
        Shift shift = activity.getShifts().stream().findFirst().orElse(null);
        setShift(shift);
        setEnrollment(enrollment);
        setAcceptanceDate(LocalDateTime.now());
        setMemberRating(participationDto.getMemberRating());
        setMemberReview(participationDto.getMemberReview());
        setVolunteerRating(participationDto.getVolunteerRating());
        setVolunteerReview(participationDto.getVolunteerReview());

        verifyInvariants();
    }

    public Participation(Activity activity, Enrollment enrollment, Shift shift, ParticipationDto participationDto) {
        setShift(shift);
        setEnrollment(enrollment);
        setAcceptanceDate(LocalDateTime.now());
        setMemberRating(participationDto.getMemberRating());
        setMemberReview(participationDto.getMemberReview());
        setVolunteerRating(participationDto.getVolunteerRating());
        setVolunteerReview(participationDto.getVolunteerReview());

        verifyInvariants();
    }

    public void memberRating(ParticipationDto participationDto){
        setMemberRating(participationDto.getMemberRating());
        setMemberReview(participationDto.getMemberReview());
        verifyInvariants();
    }

    public void volunteerRating(ParticipationDto participationDto){
        setVolunteerRating(participationDto.getVolunteerRating());
        setVolunteerReview(participationDto.getVolunteerReview());
        verifyInvariants();
    }

    public void delete(){
        if (shift != null) {
            shift.deleteParticipation(this);
        }
        if (enrollment != null) {
            enrollment.deleteParticipation(this);
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public LocalDateTime getAcceptanceDate() {
        return acceptanceDate;
    }

    public void setAcceptanceDate(LocalDateTime acceptanceDate) {
        this.acceptanceDate = acceptanceDate;
    }

    public Integer getVolunteerRating() {
        return volunteerRating;
    }

    public void setVolunteerRating(Integer rating) {
        this.volunteerRating = rating;
    }

    public Integer getMemberRating() {
        return memberRating;
    }

    public void setMemberRating(Integer rating) {
        this.memberRating = rating;
    }

    public String getVolunteerReview() {
        return volunteerReview;
    }

    public void setVolunteerReview(String volunteerReview) {
        this.volunteerReview = volunteerReview;
    }

    public String getMemberReview() {
        return memberReview;
    }

    public void setMemberReview(String memberReview) {
        this.memberReview = memberReview;
    }

    public Activity getActivity() {
        if (this.shift != null) {
            return this.shift.getActivity();
        }
        if (this.enrollment != null) {
            return this.enrollment.getActivity();
        }
        return null;
    }

    public Volunteer getVolunteer() {
        return this.enrollment != null ? this.enrollment.getVolunteer() : null;
    }

    public Shift getShift() {
        return shift;
    }

    public void setShift(Shift shift) {
        this.shift = shift;
        if (shift != null) {
            shift.addParticipation(this);
        }
    }

    public Enrollment getEnrollment() {
        return enrollment;
    }

    public void setEnrollment(Enrollment enrollment) {
        this.enrollment = enrollment;
        if (enrollment != null) {
            enrollment.addParticipation(this);
        }
    }

    private void verifyInvariants() {
        participateOnce();
        numberOfParticipantsLessOrEqualLimit();
        acceptanceAfterDeadline();
        ratingAfterEnd();
        ratingBetweenOneAndFive();
        reviewSizeIsCorrect();
        enrollmentContainsShift();
    }

    private void participateOnce() {
        Volunteer volunteer = this.getVolunteer();
        if (volunteer == null) {
            return;
        }

        if (this.getActivity().getParticipations().stream()
                .anyMatch(participation -> participation != this && participation.getVolunteer() == volunteer)) {
            throw new HEException(PARTICIPATION_VOLUNTEER_IS_ALREADY_PARTICIPATING);
        }
    }

    private void numberOfParticipantsLessOrEqualLimit() {
        if (this.getShift().getParticipations().size() > this.getShift().getParticipantsLimit()) {
            throw new HEException(PARTICIPATION_IS_FULL);
        }
    }

    private void acceptanceAfterDeadline() {
        if (this.acceptanceDate.isBefore(this.getActivity().getApplicationDeadline())) {
            throw new HEException(PARTICIPATION_ACCEPTANCE_BEFORE_DEADLINE);
        }
    }

    private void ratingAfterEnd() {
        if ((volunteerRating != null || memberRating != null) && LocalDateTime.now().isBefore(getActivity().getEndingDate())) {
            throw new HEException(PARTICIPATION_RATING_BEFORE_END);
        }
    }


    private void ratingBetweenOneAndFive() {
        if (volunteerRating != null && (volunteerRating < 1 || volunteerRating > 5)) {
            throw new HEException(PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE, volunteerRating);
        }

        if (memberRating != null && (memberRating < 1 || memberRating > 5)) {
            throw new HEException(PARTICIPATION_RATING_BETWEEN_ONE_AND_FIVE, memberRating);
        }
    }

    private void reviewSizeIsCorrect() {
        if (volunteerReview != null && (volunteerReview.length() < 10 || volunteerReview.length() > 100)) {
            throw new HEException(PARTICIPATION_REVIEW_LENGTH_INVALID, volunteerReview.length());
        }

        if (memberReview != null && (memberReview.length() < 10 || memberReview.length() > 100)) {
            throw new HEException(PARTICIPATION_REVIEW_LENGTH_INVALID, memberReview.length());
        }
    }

    private void enrollmentContainsShift() {
        if (this.enrollment != null && this.shift != null
                && !this.enrollment.getShifts().contains(this.shift)) {
            throw new HEException(PARTICIPATION_ENROLLMENT_DOES_NOT_CONTAIN_SHIFT);
        }
    }
}
