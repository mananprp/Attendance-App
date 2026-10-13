import {
  Entity,
  PrimaryColumn,
  Column,
  UpdateDateColumn,
} from 'typeorm';

@Entity({ name: 'office_config' })
export class OfficeConfigEntity {
  @PrimaryColumn({
    name: 'office_id',
    type: 'varchar',
    length: 64,
    default: 'DEFAULT_OFFICE',
  })
  officeId: string;

  @Column({
    name: 'name',
    type: 'varchar',
    length: 128,
    default: 'Headquarters',
  })
  name: string;

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

  @Column({ name: 'radius_m', type: 'int', default: 100 })
  radiusM: number;

  @Column({
    name: 'timezone',
    type: 'varchar',
    length: 64,
    default: 'Asia/Kolkata',
  })
  timezone: string;

  @Column({
    name: 'sync_windows',
    type: 'text',
    transformer: {
      to: (value: string[] | string) =>
        Array.isArray(value) ? JSON.stringify(value) : value,
      from: (value: string | string[]) => {
        if (Array.isArray(value)) return value;
        try {
          return JSON.parse(value);
        } catch {
          return ['16:00', '00:00'];
        }
      },
    },
  })
  syncWindows: string[];

  @Column({ name: 'sync_jitter_minutes', type: 'int', default: 30 })
  syncJitterMinutes: number;

  @Column({
    name: 'match_threshold',
    type: 'decimal',
    precision: 5,
    scale: 4,
    default: 0.46,
    transformer: {
      to: (value: number) => value,
      from: (value: string | number) => (value != null ? parseFloat(value as string) : value),
    },
  })
  matchThreshold: number;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
