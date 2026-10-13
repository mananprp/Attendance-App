import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
  OneToMany,
} from 'typeorm';
import { UserPhotoEntity } from './user-photo.entity';
import { AttendancePunchEntity } from './attendance-punch.entity';
import { AttendanceDayEntity } from './attendance-day.entity';
import { SyncBatchEntity } from './sync-batch.entity';

export enum UserStatus {
  ACTIVE = 'ACTIVE',
  SUSPENDED = 'SUSPENDED',
  DEACTIVATED = 'DEACTIVATED',
}

export enum UserRole {
  ADMIN = 'ADMIN',
  EMPLOYEE = 'EMPLOYEE',
}

@Entity({ name: 'users' })
export class UserEntity {
  @PrimaryColumn({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'name', type: 'varchar', length: 128 })
  name: string;

  @Column({ name: 'password_hash', type: 'varchar', length: 255 })
  passwordHash: string;

  @Column({
    name: 'role',
    type: 'varchar',
    length: 16,
    default: UserRole.EMPLOYEE,
  })
  role: UserRole;

  @Column({
    name: 'site_code',
    type: 'varchar',
    length: 64,
    default: 'DEFAULT_OFFICE',
  })
  siteCode: string;

  @Column({
    name: 'status',
    type: 'varchar',
    length: 32,
    default: UserStatus.ACTIVE,
  })
  status: UserStatus;

  @Column({ name: 'photo_change_used', type: 'boolean', default: false })
  photoChangeUsed: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;

  @OneToMany(() => UserPhotoEntity, (photo) => photo.user)
  photos: UserPhotoEntity[];

  @OneToMany(() => AttendancePunchEntity, (punch) => punch.user)
  punches: AttendancePunchEntity[];

  @OneToMany(() => AttendanceDayEntity, (day) => day.user)
  attendanceDays: AttendanceDayEntity[];

  @OneToMany(() => SyncBatchEntity, (batch) => batch.user)
  syncBatches: SyncBatchEntity[];
}
