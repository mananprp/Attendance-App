import { Test, TestingModule } from '@nestjs/testing';
import { AuthController } from './auth.controller';
import { AuthService } from './auth.service';

describe('AuthController', () => {
  let controller: AuthController;
  let authService: any;

  beforeEach(async () => {
    authService = {
      login: jest.fn().mockResolvedValue({
        token: 'mock_jwt_token',
        user: {
          user_id: 'EMP1001',
          display_name: 'Alice Sharma',
          photo_change_used: false,
          has_photo: true,
          active_photo_version: 1,
        },
      }),
    };

    const module: TestingModule = await Test.createTestingModule({
      controllers: [AuthController],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compile();

    controller = module.get<AuthController>(AuthController);
  });

  it('should return login response', async () => {
    const req = { ip: '127.0.0.1', socket: {} } as any;
    const result = await controller.login(
      { user_id: 'EMP1001', password: 'Password@123' },
      req,
    );

    expect(result.token).toBe('mock_jwt_token');
    expect(result.user.user_id).toBe('EMP1001');
    expect(authService.login).toHaveBeenCalled();
  });
});
