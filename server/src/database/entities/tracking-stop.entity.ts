import {
  Entity,
  PrimaryColumn,
  Column,
  ManyToOne,
  JoinColumn,
  Index,
} from 'typeorm';
import { TrackingSessionEntity } from './tracking-session.entity';

@Entity({ name: 'tracking_stops' })
@Index(['sessionId', 'arrivedAtUtc'])
export class TrackingStopEntity {
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
    name: 'stop_name',
    type: 'varchar',
    length: 128,
    nullable: true,
  })
  stopName: string | null;

  @Column({ name: 'arrived_at_utc', type: 'datetime' })
  arrivedAtUtc: Date;

  @ManyToOne(() => TrackingSessionEntity, (session) => session.stops, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'session_id' })
  session: TrackingSessionEntity;
}
