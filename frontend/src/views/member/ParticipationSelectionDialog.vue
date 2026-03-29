<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editParticipation && editParticipation.id === null
              ? 'Create Participation'
              : 'Your Rating'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row v-if="isCreateMode">
            <v-col cols="12">
              <v-select
                v-model="selectedEnrollmentId"
                :items="enrollments"
                item-value="id"
                label="Enrollment"
                :item-text="enrollmentLabel"
                :rules="[(v) => v != null || 'Enrollment is required']"
                data-cy="participationEnrollmentSelect"
              />
            </v-col>
            <v-col cols="12">
              <v-select
                v-model="selectedShiftId"
                :items="shiftsForSelectedEnrollment"
                item-value="id"
                label="Shift"
                :item-text="shiftLabel"
                :disabled="selectedEnrollmentId == null"
                :rules="[(v) => v != null || 'Shift is required']"
                data-cy="participationShiftSelect"
              />
            </v-col>
          </v-row>
          <v-row>
            <v-col cols="12" class="d-flex align-center">
              <v-text-field
                label="Rating"
                :rules="[(v) => isNumberValid(v) || 'Rating between 1 and 5']"
                v-model="editParticipation.memberRating"
                data-cy="participantsNumberInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12">
              <v-textarea
                label="Review"
                v-model="editParticipation.memberReview"
                :rules="[
                  (v) =>
                    !v ||
                    (v.length >= 10 && v.length < 100) ||
                    'Review must be 10–100 characters if provided',
                ]"
                data-cy="participantsReviewInput"
                auto-grow
                rows="1"
              ></v-textarea>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="primary"
          dark
          variant="text"
          @click="$emit('close-participation-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="primary"
          dark
          variant="text"
          @click="createUpdateParticipation"
          data-cy="createParticipation"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import { Vue, Component, Prop, Model, Watch } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { ISOtoString } from '@/services/ConvertDateService';
import Participation from '@/models/participation/Participation';
import Enrollment from '@/models/enrollment/Enrollment';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class ParticipationSelectionDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Participation, required: true })
  readonly participation!: Participation;
  @Prop({ type: Array, required: true })
  readonly enrollments!: Enrollment[];
  @Prop({ type: Array, required: true })
  readonly activityShifts!: Shift[];

  editParticipation: Participation = new Participation();

  selectedEnrollmentId: number | null = null;
  selectedShiftId: number | null = null;

  get isCreateMode(): boolean {
    return this.editParticipation.id === null;
  }

  get shiftsForSelectedEnrollment(): Shift[] {
    if (this.selectedEnrollmentId == null) {
      return [];
    }
    const enrollment = this.enrollments.find(
      (e) => e.id === this.selectedEnrollmentId,
    );
    if (!enrollment || !enrollment.shiftIds.length) {
      return [];
    }
    return this.activityShifts.filter(
      (s) => s.id != null && enrollment.shiftIds.includes(s.id),
    );
  }

  enrollmentLabel(enrollment: Enrollment): string {
    const name = enrollment.volunteerName ?? 'Volunteer';
    const id = enrollment.id != null ? `#${enrollment.id}` : '';
    return `${name} ${id}`.trim();
  }

  shiftLabel(shift: Shift): string {
    const start = shift.startTime ? ISOtoString(shift.startTime) : '';
    const end = shift.endTime ? ISOtoString(shift.endTime) : '';
    const loc = shift.location || '';
    return `${start} → ${end}${loc ? ` · ${loc}` : ''}`;
  }

  created() {
    this.editParticipation = new Participation(this.participation);
    this.selectedEnrollmentId = this.participation.enrollmentId ?? null;
    this.selectedShiftId = this.participation.shiftId ?? null;
  }

  @Watch('selectedEnrollmentId')
  onEnrollmentIdChanged(
    newVal: number | null,
    oldVal: number | null | undefined,
  ) {
    if (oldVal !== undefined && newVal !== oldVal) {
      this.selectedShiftId = null;
    }
  }

  get isReviewValid(): boolean {
    return (
      !this.editParticipation.memberReview ||
      (this.editParticipation.memberReview.length >= 10 &&
        this.editParticipation.memberReview.length < 100)
    );
  }

  get isRatingValid(): boolean {
    return (
      !this.editParticipation.memberRating ||
      (this.editParticipation.memberRating >= 1 &&
        this.editParticipation.memberRating <= 5)
    );
  }

  get canSave(): boolean {
    if (!this.isReviewValid || !this.isRatingValid) {
      return false;
    }
    if (this.isCreateMode) {
      return this.selectedEnrollmentId != null && this.selectedShiftId != null;
    }
    return true;
  }

  isNumberValid(value: any) {
    if (value === null || value === undefined || value === '') return true;
    if (!/^\d+$/.test(value)) return false;
    const parsedValue = parseInt(value, 10);
    return parsedValue >= 1 && parsedValue <= 5;
  }

  async createUpdateParticipation() {
    if ((this.$refs.form as Vue & { validate: () => boolean }).validate()) {
      try {
        if (this.isCreateMode) {
          this.editParticipation.enrollmentId = this.selectedEnrollmentId;
          this.editParticipation.shiftId = this.selectedShiftId;
        }
        const result =
          this.editParticipation.id !== null
            ? await RemoteServices.updateParticipationMember(
                this.editParticipation.id,
                this.editParticipation,
              )
            : await RemoteServices.createParticipation(
                this.selectedShiftId!,
                this.selectedEnrollmentId!,
                this.editParticipation,
              );
        this.$emit('save-participation', result);
        this.$emit('close-participation-dialog');
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>
