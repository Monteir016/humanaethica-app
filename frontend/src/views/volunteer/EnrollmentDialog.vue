<template>
  <v-dialog v-model="dialog" persistent width="800">
    <v-card>
      <v-card-title>
        <span class="headline">
          {{
            editEnrollment && editEnrollment.id === null
              ? 'New Application'
              : 'Edit Application'
          }}
        </span>
      </v-card-title>
      <v-card-text>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12" v-if="isCreatingEnrollment">
              <v-select
                v-model="editEnrollment.shiftIds"
                :items="availableShifts"
                item-text="label"
                item-value="id"
                label="*Shifts"
                multiple
                chips
                deletable-chips
                :rules="[
                  (v) =>
                    (Array.isArray(v) && v.length > 0) ||
                    'At least one shift must be selected',
                ]"
                required
                data-cy="shiftIdsInput"
              ></v-select>
            </v-col>
            <v-col cols="12">
              <v-textarea
                label="*Motivation"
                :rules="[(v) => !!v || 'Motivation is required']"
                required
                v-model="editEnrollment.motivation"
                data-cy="motivationInput"
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
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-enrollment-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          v-if="canSave"
          color="blue-darken-1"
          variant="text"
          @click="updateEnrollment"
          data-cy="saveEnrollment"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import RemoteServices from '@/services/RemoteServices';
import { ISOtoString } from '@/services/ConvertDateService';
import Enrollment from '@/models/enrollment/Enrollment';
import Activity from '@/models/activity/Activity';
import Shift from '@/models/shift/Shift';

@Component({
  methods: { ISOtoString },
})
export default class EnrollmentDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Enrollment, required: true }) readonly enrollment!: Enrollment;
  @Prop({ type: Object, default: null }) readonly activity!: Activity | null;

  editEnrollment: Enrollment = new Enrollment();

  async created() {
    this.editEnrollment = new Enrollment(this.enrollment);
  }

  get isCreatingEnrollment(): boolean {
    return this.editEnrollment.id === null;
  }

  get availableShifts(): { id: number; label: string }[] {
    return (this.activity?.shifts ?? [])
      .filter((shift: Shift) => shift.id !== null)
      .map((shift: Shift) => ({
        id: shift.id as number,
        label: `${shift.location} (${ISOtoString(
          shift.startTime,
        )} - ${ISOtoString(shift.endTime)})`,
      }));
  }

  get canSave(): boolean {
    return (
      !!this.editEnrollment.motivation &&
      this.editEnrollment.motivation.length >= 10 &&
      (!this.isCreatingEnrollment || this.editEnrollment.shiftIds.length > 0)
    );
  }

  async updateEnrollment() {
    //editar
    if (
      this.editEnrollment.id !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.editEnrollment(
          this.editEnrollment.id,
          this.editEnrollment,
        );
        this.$emit('update-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
    //criar
    else if (
      this.editEnrollment.activityId !== null &&
      this.editEnrollment.shiftIds.length > 0 &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      try {
        const result = await RemoteServices.createEnrollment(
          this.editEnrollment,
        );
        this.$emit('save-enrollment', result);
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
    }
  }
}
</script>

<style scoped lang="scss"></style>
