import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  UpdateDateColumn,
  ManyToOne,
  JoinColumn,
  Unique,
  Index,
} from 'typeorm';
import { UserEntity } from './user.entity';

export enum AttendanceDayStatus {
  COMPLETE = 'COMPLETE',
  MISSED_PUNCH = 'MISSED_PUNCH',
  NO_RECORD = 'NO_RECORD',
}

@Entity({ name: 'attendance_days' })
@Unique(['userId', 'date'])
@Index(['userId', 'date'])
export class AttendanceDayEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'date', type: 'varchar', length: 10 })
  date: string;

  @Column({ name: 'in_time', type: 'datetime', nullable: true })
  inTime: Date | null;

  @Column({ name: 'out_time', type: 'datetime', nullable: true })
  outTime: Date | null;

  @Column({
    name: 'status',
    type: 'varchar',
    length: 32,
    default: AttendanceDayStatus.NO_RECORD,
  })
  status: AttendanceDayStatus;

  @Column({ name: 'time_unverified', type: 'boolean', default: false })
  timeUnverified: boolean;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @ManyToOne(() => UserEntity, (user) => user.attendanceDays, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;
}
