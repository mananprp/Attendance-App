import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
  Index,
} from 'typeorm';
import { UserEntity } from './user.entity';
import { SyncBatchEntity } from './sync-batch.entity';

export enum PunchType {
  IN = 'IN',
  OUT = 'OUT',
}

export enum TimeSource {
  NTP = 'NTP',
  DEVICE = 'DEVICE',
}

@Entity({ name: 'attendance_punches' })
@Index(['userId', 'dateLocal'])
export class AttendancePunchEntity {
  @PrimaryColumn({ name: 'punch_uuid', type: 'varchar', length: 36 })
  punchUuid: string;

  @Column({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'date_local', type: 'varchar', length: 10 })
  dateLocal: string;

  @Column({ name: 'type', type: 'varchar', length: 8 })
  type: PunchType;

  @Column({ name: 'punch_time_utc', type: 'datetime' })
  punchTimeUtc: Date;

  @Column({
    name: 'time_source',
    type: 'varchar',
    length: 16,
    default: TimeSource.NTP,
  })
  timeSource: TimeSource;

  @Column({ name: 'time_unverified', type: 'boolean', default: false })
  timeUnverified: boolean;

  @Column({
    name: 'latitude',
    type: 'decimal',
    precision: 10,
    scale: 7,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : value),
    },
  })
  latitude: number;

  @Column({
    name: 'longitude',
    type: 'decimal',
    precision: 10,
    scale: 7,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : value),
    },
  })
  longitude: number;

  @Column({
    name: 'distance_m',
    type: 'decimal',
    precision: 8,
    scale: 2,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : value),
    },
  })
  distanceM: number;

  @Column({
    name: 'match_score',
    type: 'decimal',
    precision: 5,
    scale: 4,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : value),
    },
  })
  matchScore: number;

  @Column({ name: 'image_path', type: 'varchar', length: 512, nullable: true })
  imagePath: string | null;

  @Column({ name: 'sync_batch_id', type: 'varchar', length: 36, nullable: true })
  syncBatchId: string | null;

  @CreateDateColumn({ name: 'received_at' })
  receivedAt: Date;

  @ManyToOne(() => UserEntity, (user) => user.punches, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @ManyToOne(() => SyncBatchEntity, (batch) => batch.punches, {
    onDelete: 'SET NULL',
    nullable: true,
  })
  @JoinColumn({ name: 'sync_batch_id' })
  syncBatch: SyncBatchEntity | null;
}
