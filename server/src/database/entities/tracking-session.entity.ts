import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
  Index,
} from 'typeorm';
import { UserEntity } from './user.entity';
import { TrackingPointEntity } from './tracking-point.entity';
import { TrackingStopEntity } from './tracking-stop.entity';

export enum TrackingSessionStatus {
  ACTIVE = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  ABORTED = 'ABORTED',
}

@Entity({ name: 'tracking_sessions' })
@Index(['userId', 'startTimeUtc'])
export class TrackingSessionEntity {
  @PrimaryColumn({ name: 'id', type: 'varchar', length: 36 })
  id: string;

  @Column({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'start_time_utc', type: 'datetime' })
  startTimeUtc: Date;

  @Column({ name: 'end_time_utc', type: 'datetime', nullable: true })
  endTimeUtc: Date | null;

  @Column({
    name: 'total_distance_m',
    type: 'decimal',
    precision: 10,
    scale: 2,
    default: 0,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : 0),
    },
  })
  totalDistanceM: number;

  @Column({
    name: 'status',
    type: 'varchar',
    length: 16,
    default: TrackingSessionStatus.ACTIVE,
  })
  status: TrackingSessionStatus;

  @Column({ name: 'stops_count', type: 'int', default: 0 })
  stopsCount: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @ManyToOne(() => UserEntity, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @OneToMany(() => TrackingPointEntity, (point) => point.session, { cascade: true })
  points: TrackingPointEntity[];

  @OneToMany(() => TrackingStopEntity, (stop) => stop.session, { cascade: true })
  stops: TrackingStopEntity[];
}
