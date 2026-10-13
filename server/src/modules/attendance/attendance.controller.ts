import {
  Controller,
  Post,
  Get,
  Query,
  Body,
  UseGuards,
  UseInterceptors,
  UploadedFiles,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiConsumes,
  ApiQuery,
  ApiBody,
} from '@nestjs/swagger';
import { AnyFilesInterceptor } from '@nestjs/platform-express';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import {
  CurrentUser,
  AuthenticatedUser,
} from '../../common/decorators/current-user.decorator';
import { AttendanceService } from './attendance.service';
import { SyncBatchResponseDto } from './dto/sync-batch-response.dto';
import { MonthlyAttendanceResponseDto } from './dto/monthly-attendance-response.dto';

@ApiTags('attendance')
@Controller('attendance')
@UseGuards(JwtAuthGuard)
@ApiBearerAuth()
export class AttendanceController {
  constructor(private readonly attendanceService: AttendanceService) {}

  @Post('sync')
  @HttpCode(HttpStatus.OK)
  @UseInterceptors(AnyFilesInterceptor())
  @ApiConsumes('multipart/form-data')
  @ApiOperation({ summary: 'Batch upload punch records' })
  @ApiBody({
    schema: {
      type: 'object',
      required: ['batch_json'],
      properties: {
        batch_json: {
          type: 'string',
          description: 'JSON stringified array of SyncPunchItem',
        },
        images: {
          type: 'array',
          items: {
            type: 'string',
            format: 'binary',
          },
          description: 'Captured punch frame images',
        },
      },
    },
  })
  @ApiResponse({
    status: 200,
    description: 'Batch processed with individual record statuses',
    type: SyncBatchResponseDto,
  })
  @ApiResponse({ status: 400, description: 'Invalid payload format' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async syncPunches(
    @CurrentUser() user: AuthenticatedUser,
    @Body('batch_json') batchJson: string,
    @UploadedFiles() files: Express.Multer.File[],
  ): Promise<SyncBatchResponseDto> {
    return this.attendanceService.syncPunches(user.userId, batchJson, files || []);
  }

  @Get()
  @ApiOperation({ summary: 'Fetch monthly attendance history' })
  @ApiQuery({
    name: 'month',
    required: true,
    description: 'Target month in format YYYY-MM',
    example: '2026-09',
  })
  @ApiResponse({
    status: 200,
    description: 'Monthly attendance records',
    type: MonthlyAttendanceResponseDto,
  })
  @ApiResponse({ status: 400, description: 'Invalid month format' })
  @ApiResponse({ status: 401, description: 'Unauthorized' })
  async getMonthlyAttendance(
    @CurrentUser() user: AuthenticatedUser,
    @Query('month') month: string,
  ): Promise<MonthlyAttendanceResponseDto> {
    return this.attendanceService.getMonthlyAttendance(user.userId, month);
  }
}
