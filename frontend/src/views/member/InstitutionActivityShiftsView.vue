<template>
  <v-card class="table">
    <div class="text-h3">{{ activity ? activity.name : 'Manage Shifts' }}</div>
    <v-card-text>
      <span>Shifts management page placeholder.</span>
    </v-card-text>
    <v-card-actions>
      <v-btn color="primary" dark @click="getActivities" data-cy="getActivities"
        >Activities</v-btn
      >
    </v-card-actions>
  </v-card>
</template>

<script lang="ts">
import { Component, Vue } from 'vue-property-decorator';
import Activity from '@/models/activity/Activity';

@Component
export default class InstitutionActivityShiftsView extends Vue {
  activity: Activity | null = null;

  created() {
    this.activity = this.$store.getters.getActivity;
  }

  async getActivities() {
    await this.$store.dispatch('setActivity', null);
    this.$router.push({ name: 'institution-activities' }).catch(() => {});
  }
}
</script>
