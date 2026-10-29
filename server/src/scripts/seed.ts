import * as dotenv from 'dotenv';
dotenv.config();

import { DataSource } from 'typeorm';
import * as bcrypt from 'bcryptjs';
import {
  UserEntity,
  UserStatus,
  UserRole,
  UserPhotoEntity,
  OfficeConfigEntity,
  SyncBatchEntity,
  AttendancePunchEntity,
  AttendanceDayEntity,
} from '../database/entities';

async function seed() {
  const dbType = process.env.DB_TYPE || (process.env.NODE_ENV === 'test' ? 'sqlite' : 'mssql');

  const entities = [
    UserEntity,
    UserPhotoEntity,
    OfficeConfigEntity,
    SyncBatchEntity,
    AttendancePunchEntity,
    AttendanceDayEntity,
  ];

  let dataSource: DataSource;

  if (dbType === 'sqlite') {
    dataSource = new DataSource({
      type: 'sqlite',
      database: process.env.DB_SQLITE_FILE || 'attendance_dev.db',
      entities,
      synchronize: true,
    });
  } else if (dbType === 'mysql') {
    dataSource = new DataSource({
      type: 'mysql',
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '3306', 10),
      username: process.env.DB_USERNAME || 'root',
      password: process.env.DB_PASSWORD || 'password',
      database: process.env.DB_DATABASE || 'attendance_prod',
      entities,
      synchronize: true,
    });
  } else {
    // Default: MSSQL
    dataSource = new DataSource({
      type: 'mssql',
      host: process.env.DB_HOST || 'localhost',
      port: parseInt(process.env.DB_PORT || '1433', 10),
      username: process.env.DB_USERNAME || 'sa',
      password: process.env.DB_PASSWORD || 'YourStrongPassword123!',
      database: process.env.DB_DATABASE || 'attendance_prod',
      entities,
      synchronize: true,
      options: {
        encrypt: process.env.DB_ENCRYPT === 'true' || false,
        trustServerCertificate: process.env.DB_TRUST_SERVER_CERTIFICATE !== 'false',
      },
    });
  }

  console.log(`Connecting to ${dbType} database for seeding...`);
  await dataSource.initialize();
  console.log('Database connected.');

  const officeRepo = dataSource.getRepository(OfficeConfigEntity);
  const userRepo = dataSource.getRepository(UserEntity);

  // 1. Seed Office Config
  console.log('Seeding office config...');
  const officeId = process.env.DEFAULT_OFFICE_ID || 'DEFAULT_OFFICE';
  const name = process.env.DEFAULT_OFFICE_NAME || 'Headquarters';
  const latitude = parseFloat(process.env.DEFAULT_OFFICE_LAT || '28.490293');
  const longitude = parseFloat(process.env.DEFAULT_OFFICE_LNG || '77.060544');
  const radiusM = parseInt(process.env.DEFAULT_OFFICE_RADIUS_M || '100', 10);
  const timezone = process.env.DEFAULT_OFFICE_TIMEZONE || 'Asia/Kolkata';
  const matchThreshold = 0.46;
  const syncWindows = ['16:00', '00:00'];
  const syncJitterMinutes = 30;

  const existingOffice = await officeRepo.findOne({ where: { officeId } });
  const officeConfig = existingOffice || officeRepo.create({ officeId });

  officeConfig.name = name;
  officeConfig.latitude = latitude;
  officeConfig.longitude = longitude;
  officeConfig.radiusM = radiusM;
  officeConfig.timezone = timezone;
  officeConfig.syncWindows = syncWindows;
  officeConfig.syncJitterMinutes = syncJitterMinutes;
  officeConfig.matchThreshold = matchThreshold;

  await officeRepo.save(officeConfig);
  console.log(`Office config saved successfully (Lat: ${latitude}, Lng: ${longitude}, Radius: ${radiusM}m).`);

  // 2. Seed Users
  console.log('Seeding default test users...');
  const saltRounds = 10;
  const defaultPasswordHash = await bcrypt.hash('Password@123', saltRounds);
  const adminPasswordHash = await bcrypt.hash('Admin@123', saltRounds);

  const testUsers: Partial<UserEntity>[] = [
    {
      userId: 'EMP1001',
      name: 'Alice Sharma',
      passwordHash: defaultPasswordHash,
      role: UserRole.EMPLOYEE,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    },
    {
      userId: 'EMP1002',
      name: 'Bob Kumar',
      passwordHash: defaultPasswordHash,
      role: UserRole.EMPLOYEE,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    },
    {
      userId: 'EMP1003',
      name: 'Charlie Singh',
      passwordHash: defaultPasswordHash,
      role: UserRole.EMPLOYEE,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: true,
    },
    {
      userId: 'EMP2001',
      name: 'Manan',
      passwordHash: defaultPasswordHash,
      role: UserRole.ADMIN,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    },
    {
      userId: 'GUARD01',
      name: 'Security Guard Ramesh',
      passwordHash: defaultPasswordHash,
      role: UserRole.GUARD,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    },
    {
      userId: 'ADMIN01',
      name: 'System Admin',
      passwordHash: adminPasswordHash,
      role: UserRole.ADMIN,
      siteCode: 'DEFAULT_OFFICE',
      status: UserStatus.ACTIVE,
      photoChangeUsed: false,
    },
  ];

  for (const u of testUsers) {
    const existing = await userRepo.findOne({ where: { userId: u.userId } });
    if (!existing) {
      const userEntity = userRepo.create(u);
      await userRepo.save(userEntity);
      console.log(`Seeded user: ${u.userId} (${u.name})`);
    } else {
      console.log(`User already exists: ${u.userId} (${u.name})`);
    }
  }

  console.log('Seeding completed successfully.');
  await dataSource.destroy();
}

seed().catch((err) => {
  console.error('Seeding error:', err);
  process.exit(1);
});
