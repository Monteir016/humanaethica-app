<template>
  <v-card class="table">
    <div class="text-h3">{{ activity.name }}</div>
    <v-data-table
      :headers="headers"
      :items="shifts"
      :search="search"
      :loading="loadingShifts"
      loading-text="Loading shifts..."
      no-data-text="No shifts available for this activity."
      disable-pagination
      :hide-default-footer="true"
      :mobile-breakpoint="0"
      data-cy="activityShiftsTable"
    >
      <template v-slot:top>
        <v-card-title>
          <v-text-field
            v-model="search"
            append-icon="search"
            label="Search"
            class="mx-2"
          />
          <v-spacer />
          <v-btn color="primary" dark @click="newShift" data-cy="newShift">
            New Shift
          </v-btn>
          <v-btn
            color="primary"
            dark
            class="ml-2"
            @click="getActivities"
            data-cy="getActivities"
          >
            Activities
          </v-btn>
        </v-card-title>
      </template>
    </v-data-table>
    <shift-dialog
      v-if="activity.id !== null && editShiftDialog"
      v-model="editShiftDialog"
      :activity="activity"
      v-on:save-shift="onSaveShift"
      v-on:close-shift-dialog="onCloseShiftDialog"
    />
  </v-card>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Activity from '@/models/activity/Activity';
import Shift from '@/models/shift/Shift';
import RemoteServices from '@/services/RemoteServices';
import ShiftDialog from '@/views/member/ShiftDialog.vue';

@Component({
  components: {
    'shift-dialog': ShiftDialog,
  },
})
export default class InstitutionActivityShiftsView extends Vue {
  activity: Activity = new Activity();
  shifts: Shift[] = [];
  search: string = '';
  loadingShifts: boolean = false;
  editShiftDialog: boolean = false;

  headers: object = [
    {
      text: 'Location',
      value: 'location',
      align: 'left',
      width: '40%',
    },
    {
      text: 'Start Date',
      value: 'startTime',
      align: 'left',
      width: '20%',
    },
    {
      text: 'End Date',
      value: 'endTime',
      align: 'left',
      width: '20%',
    },
    {
      text: 'Participants Limit',
      value: 'participantsLimit',
      align: 'left',
      width: '20%',
    },
  ];

  async created() {
    const currentActivity = this.$store.getters.getActivity;
    if (!currentActivity || currentActivity.id === null) {
      await this.$router.push({ name: 'institution-activities' });
      return;
    }

    this.activity = new Activity(currentActivity);
    await this.loadShifts();
  }

  async loadShifts() {
    if (this.activity.id === null) {
      return;
    }

    this.loadingShifts = true;
    await this.$store.dispatch('loading');
    try {
      this.shifts = await RemoteServices.getActivityShifts(this.activity.id);
      this.activity.shifts = this.shifts;
      await this.$store.dispatch('setActivity', this.activity);
    } catch (error) {
      await this.$store.dispatch('error', error);
    }
    this.loadingShifts = false;
    await this.$store.dispatch('clearLoading');
  }

  newShift() {
    this.editShiftDialog = true;
  }

  onCloseShiftDialog() {
    this.editShiftDialog = false;
  }

  async onSaveShift() {
    this.editShiftDialog = false;
    await this.loadShifts();
  }

  async getActivities() {
    await this.$store.dispatch('setActivity', null);
    this.$router.push({ name: 'institution-activities' }).catch(() => {});
  }
}
</script>

<style scoped lang="scss"></style>
