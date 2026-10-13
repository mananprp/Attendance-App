import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  OneToMany,
  JoinColumn,
} from 'typeorm';
import { UserEntity } from './user.entity';
import { AttendancePunchEntity } from './attendance-punch.entity';

export enum SyncBatchStatus {
  SUCCESS = 'SUCCESS',
  PARTIAL = 'PARTIAL',
  FAILED = 'FAILED',
}

@Entity({ name: 'sync_batches' })
export class SyncBatchEntity {
  @PrimaryColumn({ name: 'batch_id', type: 'varchar', length: 36 })
  batchId: string;

  @Column({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'records_count', type: 'int', default: 0 })
  recordsCount: number;

  @CreateDateColumn({ name: 'received_at' })
  receivedAt: Date;

  @Column({
    name: 'status',
    type: 'varchar',
    length: 32,
    default: SyncBatchStatus.SUCCESS,
  })
  status: SyncBatchStatus;

  @ManyToOne(() => UserEntity, (user) => user.syncBatches, {
    onDelete: 'CASCADE',
  })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;

  @OneToMany(() => AttendancePunchEntity, (punch) => punch.syncBatch)
  punches: AttendancePunchEntity[];
}
