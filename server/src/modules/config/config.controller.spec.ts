import { Test, TestingModule } from '@nestjs/testing';
import { ConfigController } from './config.controller';
import { ConfigService } from './config.service';

describe('ConfigController', () => {
  let controller: ConfigController;
  let configService: any;

  beforeEach(async () => {
    configService = {
      getAppConfig: jest.fn().mockResolvedValue({
        office_lat: 28.613939,
        office_lng: 77.209021,
        radius_m: 100,
        timezone: 'Asia/Kolkata',
        sync_windows: ['16:00', '00:00'],
        sync_jitter_minutes: 30,
        match_threshold: 0.55,
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [ConfigController],
      providers: [{ provide: ConfigService, useValue: configService }],
    }).compile();

    controller = module.get<ConfigController>(ConfigController);
  });

  it('should return app configuration', async () => {
    const mockReq = { user: { userId: 'EMP1001' } };
    const result = await controller.getConfig(mockReq);
    expect(result.office_lat).toBe(28.613939);
    expect(result.radius_m).toBe(100);
    expect(configService.getAppConfig).toHaveBeenCalledWith('EMP1001');
  });
});
