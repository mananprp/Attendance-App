import {
  Entity,
  PrimaryColumn,
  Column,
  ManyToOne,
  JoinColumn,
  Index,
} from 'typeorm';
import { TrackingSessionEntity } from './tracking-session.entity';

@Entity({ name: 'tracking_points' })
@Index(['sessionId', 'recordedAtUtc'])
export class TrackingPointEntity {
  @PrimaryColumn({ name: 'id', type: 'varchar', length: 36 })
  id: string;

  @Column({ name: 'session_id', type: 'varchar', length: 36 })
  sessionId: string;

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
    name: 'accuracy_m',
    type: 'float',
    nullable: true,
  })
  accuracyM: number | null;

  @Column({ name: 'recorded_at_utc', type: 'datetime' })
  recordedAtUtc: Date;

  @Column({ name: 'is_paused_gap', type: 'boolean', default: false })
  isPausedGap: boolean;

  @ManyToOne(() => TrackingSessionEntity, (session) => session.points, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'session_id' })
  session: TrackingSessionEntity;
}
