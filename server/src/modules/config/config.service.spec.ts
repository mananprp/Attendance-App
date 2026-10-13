import { Test, TestingModule } from '@nestjs/testing';
import { getRepositoryToken } from '@nestjs/typeorm';
import { ConfigService } from './config.service';
import { OfficeConfigEntity } from '../../database/entities/office-config.entity';

describe('ConfigService', () => {
  let service: ConfigService;
  let repo: any;

  beforeEach(async () => {
    repo = {
      findOne: jest.fn(),
    };

    const module: TestingModule = await Test.createTestingModule({
      providers: [
        ConfigService,
        {
          provide: getRepositoryToken(OfficeConfigEntity),
          useValue: repo,
        },
      ],
    }).compile();

    service = module.get<ConfigService>(ConfigService);
  });

  it('should return office config from database when found', async () => {
    repo.findOne.mockResolvedValue({
      officeId: 'DEFAULT_OFFICE',
      name: 'Headquarters',
      latitude: '28.613939',
      longitude: '77.209021',
      radiusM: 100,
      timezone: 'Asia/Kolkata',
      syncWindows: ['16:00', '00:00'],
      syncJitterMinutes: 30,
      matchThreshold: '0.5500',
    });

    const result = await service.getAppConfig();
    expect(result.office_lat).toBe(28.613939);
    expect(result.office_lng).toBe(77.209021);
    expect(result.radius_m).toBe(100);
    expect(result.timezone).toBe('Asia/Kolkata');
    expect(result.sync_windows).toEqual(['16:00', '00:00']);
  });

  it('should return fallback defaults when not found in database', async () => {
    repo.findOne.mockResolvedValue(null);

    const result = await service.getAppConfig();
    const expectedLat = parseFloat(process.env.DEFAULT_OFFICE_LAT || '28.490293');
    expect(result.office_lat).toBe(expectedLat);
    expect(result.match_threshold).toBe(0.55);
  });
});
