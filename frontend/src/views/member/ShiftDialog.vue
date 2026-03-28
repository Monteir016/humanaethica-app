<template>
  <v-dialog v-model="dialog" persistent width="900">
    <v-card>
      <v-card-title>
        <span class="headline">Create Shift</span>
      </v-card-title>
      <v-card-text>
        <v-alert
          v-if="hasInvalidDateRange"
          type="error"
          dense
          data-cy="shiftDateRangeError"
        >
          Start date must be before or equal to end date.
        </v-alert>
        <v-form ref="form" lazy-validation>
          <v-row>
            <v-col cols="12">
              <v-text-field
                label="*Location"
                :rules="[
                  (v) => !!v || 'Location is required',
                  (v) =>
                    isLocationValid(v) ||
                    'Location must have between 20 and 200 characters',
                ]"
                required
                v-model="editShift.location"
                data-cy="locationInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <v-text-field
                label="*Participants Limit"
                :rules="[
                  (v) =>
                    isParticipantsLimitValid(v) ||
                    'Participants limit must be a positive number',
                ]"
                required
                v-model="editShift.participantsLimit"
                data-cy="participantsLimitInput"
              ></v-text-field>
            </v-col>
            <v-col cols="12" sm="6">
              <VueCtkDateTimePicker
                id="startTimeInput"
                v-model="editShift.startTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*Start Date"
                data-cy="startTimeInput"
              ></VueCtkDateTimePicker>
            </v-col>
            <v-col cols="12" sm="6">
              <VueCtkDateTimePicker
                id="endTimeInput"
                v-model="editShift.endTime"
                format="YYYY-MM-DDTHH:mm:ssZ"
                label="*End Date"
                data-cy="endTimeInput"
              ></VueCtkDateTimePicker>
            </v-col>
          </v-row>
        </v-form>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue-darken-1"
          variant="text"
          @click="$emit('close-shift-dialog')"
        >
          Close
        </v-btn>
        <v-btn
          :disabled="!canSave"
          :loading="creatingShift"
          color="blue-darken-1"
          variant="text"
          @click="saveShift"
          data-cy="saveShift"
        >
          Save
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script lang="ts">
import { Vue, Component, Prop, Model } from 'vue-property-decorator';
import Activity from '@/models/activity/Activity';
import Shift from '@/models/shift/Shift';
import RemoteServices from '@/services/RemoteServices';
import VueCtkDateTimePicker from 'vue-ctk-date-time-picker';
import 'vue-ctk-date-time-picker/dist/vue-ctk-date-time-picker.css';

Vue.component('VueCtkDateTimePicker', VueCtkDateTimePicker);

@Component
export default class ShiftDialog extends Vue {
  @Model('dialog', Boolean) dialog!: boolean;
  @Prop({ type: Activity, required: true }) readonly activity!: Activity;

  editShift: Shift = new Shift();
  creatingShift: boolean = false;

  isLocationValid(value: string) {
    const trimmedValue = value?.trim() ?? '';
    return trimmedValue.length >= 20 && trimmedValue.length <= 200;
  }

  isParticipantsLimitValid(value: any) {
    if (!/^\d+$/.test(String(value))) return false;
    return parseInt(value) > 0;
  }

  get hasInvalidDateRange(): boolean {
    if (!this.editShift.startTime || !this.editShift.endTime) return false;
    const start = new Date(this.editShift.startTime).getTime();
    const end = new Date(this.editShift.endTime).getTime();
    return start > end;
  }

  get canSave(): boolean {
    return (
      !!this.editShift.location &&
      this.isLocationValid(this.editShift.location) &&
      !!this.editShift.startTime &&
      !!this.editShift.endTime &&
      this.isParticipantsLimitValid(this.editShift.participantsLimit) &&
      !this.hasInvalidDateRange
    );
  }

  async saveShift() {
    if (this.hasInvalidDateRange) return;
    if (
      this.activity.id !== null &&
      (this.$refs.form as Vue & { validate: () => boolean }).validate()
    ) {
      this.creatingShift = true;
      try {
        const result = await RemoteServices.createShift(
          this.activity.id,
          this.editShift,
        );
        this.$emit('save-shift', result);
        this.$emit('close-shift-dialog');
      } catch (error) {
        await this.$store.dispatch('error', error);
      }
      this.creatingShift = false;
    }
  }
}
</script>

<style scoped lang="scss"></style>
