import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  ManyToOne,
  JoinColumn,
  Unique,
} from 'typeorm';
import { UserEntity } from './user.entity';

const getBinaryColumnOptions = () => {
  const dbType = process.env.DB_TYPE || 'mssql';
  if (dbType === 'sqlite') {
    return { type: 'blob' as const };
  }
  if (dbType === 'mysql') {
    return { type: 'longblob' as const };
  }
  return { type: 'varbinary' as const, length: 'MAX' };
};

@Entity({ name: 'user_photos' })
@Unique(['userId', 'version'])
export class UserPhotoEntity {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'user_id', type: 'varchar', length: 64 })
  userId: string;

  @Column({ name: 'version', type: 'smallint' })
  version: number;

  @Column({ name: 'image_path', type: 'varchar', length: 512 })
  imagePath: string;

  @Column({ name: 'hash', type: 'varchar', length: 64 })
  hash: string;

  @Column({
    name: 'mime_type',
    type: 'varchar',
    length: 32,
    nullable: true,
  })
  mimeType: string | null;

  @Column({
    name: 'image_data',
    ...getBinaryColumnOptions(),
    nullable: true,
    select: false,
  })
  imageData?: Buffer | null;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  @CreateDateColumn({ name: 'uploaded_at' })
  uploadedAt: Date;

  @ManyToOne(() => UserEntity, (user) => user.photos, { onDelete: 'CASCADE' })
  @JoinColumn({ name: 'user_id' })
  user: UserEntity;
}
