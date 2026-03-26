import { ISOtoString } from '@/services/ConvertDateService';

export default class Shift {
  id: number | null = null;
  startTime!: string;
  endTime!: string;
  participantsLimit!: number;
  location!: string;
  activityId: number | null = null;

  constructor(jsonObj?: Shift) {
    if (jsonObj) {
      this.id = jsonObj.id;
      this.startTime = ISOtoString(jsonObj.startTime);
      this.endTime = ISOtoString(jsonObj.endTime);
      this.participantsLimit = jsonObj.participantsLimit;
      this.location = jsonObj.location;
      this.activityId = jsonObj.activityId;
    }
  }
}
