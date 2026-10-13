import { TypeOrmModuleOptions } from '@nestjs/typeorm';
import {
  UserEntity,
  UserPhotoEntity,
  OfficeConfigEntity,
  SyncBatchEntity,
  AttendancePunchEntity,
  AttendanceDayEntity,
} from '../database/entities';

export const getDatabaseConfig = (): TypeOrmModuleOptions => {
  const dbType = process.env.DB_TYPE || 'mssql';

  const entities = [
    UserEntity,
    UserPhotoEntity,
    OfficeConfigEntity,
    SyncBatchEntity,
    AttendancePunchEntity,
    AttendanceDayEntity,
  ];

  if (dbType === 'sqlite') {
    const isTest = process.env.NODE_ENV === 'test';
    return {
      type: 'sqlite',
      database: isTest ? ':memory:' : (process.env.DB_SQLITE_FILE || 'attendance_dev.db'),
      entities,
      synchronize: true,
      logging: false,
    };
  }

  if (dbType === 'mysql') {
    return {
      type: 'mysql',
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '3306', 10),
      username: process.env.DB_USERNAME || 'root',
      password: process.env.DB_PASSWORD || 'password',
      database: process.env.DB_DATABASE || 'attendance_db',
      entities,
      synchronize: process.env.DB_SYNCHRONIZE === 'true' || false,
      logging: process.env.DB_LOGGING === 'true' || false,
    };
  }

  // Default: Microsoft SQL Server (MSSQL)
  return {
    type: 'mssql',
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '1433', 10),
    username: process.env.DB_USERNAME || 'sa',
    password: process.env.DB_PASSWORD || 'YourStrongPassword123!',
    database: process.env.DB_DATABASE || 'attendance_prod',
    entities,
    synchronize: process.env.DB_SYNCHRONIZE !== 'false',
    logging: process.env.DB_LOGGING === 'true' || false,
    options: {
      encrypt: process.env.DB_ENCRYPT === 'true' || false,
      trustServerCertificate: process.env.DB_TRUST_SERVER_CERTIFICATE !== 'false',
    },
  };
};
