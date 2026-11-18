import {
  Entity,
  PrimaryColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

@Entity({ name: 'user_sites' })
export class UserSiteEntity {
  @PrimaryColumn({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @PrimaryColumn({ name: 'site_code', type: 'varchar', length: 64 })
  siteCode: string;

  @Column({ name: 'assigned_by', type: 'varchar', length: 64, nullable: true })
  assignedBy?: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
