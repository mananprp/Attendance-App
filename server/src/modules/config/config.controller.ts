import { Controller, Get, UseGuards, Request } from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { ConfigService } from './config.service';
import { AppConfigResponseDto } from './dto/app-config-response.dto';

@ApiTags('config')
@Controller('config')
export class ConfigController {
  constructor(private readonly configService: ConfigService) {}

  @Get()
  @UseGuards(JwtAuthGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Fetch app configuration for authenticated user' })
  @ApiResponse({
    status: 200,
    description: 'Configuration retrieved successfully',
    type: AppConfigResponseDto,
  })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async getConfig(@Request() req): Promise<AppConfigResponseDto> {
    return this.configService.getAppConfig(req.user?.userId);
  }
}
