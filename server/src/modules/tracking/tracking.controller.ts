import {
  Controller,
  Post,
  Get,
  Param,
  Body,
  Query,
  UseGuards,
  HttpCode,
  HttpStatus,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
  ApiQuery,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { AdminGuard } from '../../common/guards/admin.guard';
import {
  CurrentUser,
  AuthenticatedUser,
} from '../../common/decorators/current-user.decorator';
import { TrackingService } from './tracking.service';
import { StartTrackingSessionDto } from './dto/start-tracking-session.dto';
import { FinishTrackingSessionDto } from './dto/finish-tracking-session.dto';

@ApiTags('tracking')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard)
@Controller()
export class TrackingController {
  constructor(private readonly trackingService: TrackingService) {}

  @Post('tracking/sessions/start')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Start a new field tracking session' })
  @ApiResponse({ status: 200, description: 'Tracking session initialized' })
  async startSession(
    @CurrentUser() user: AuthenticatedUser,
    @Body() dto: StartTrackingSessionDto,
  ) {
    return this.trackingService.startSession(user.userId, dto);
  }

  @Post('tracking/sessions/:id/finish')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Finish tracking session and upload route points and stops' })
  @ApiResponse({ status: 200, description: 'Tracking session finished successfully' })
  async finishSession(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') sessionId: string,
    @Body() dto: FinishTrackingSessionDto,
  ) {
    return this.trackingService.finishSession(user.userId, sessionId, dto);
  }

  @Get('tracking/sessions/my')
  @ApiOperation({ summary: 'Get recent tracking sessions for the current user' })
  @ApiResponse({ status: 200, description: 'List of tracking sessions' })
  async getMySessions(@CurrentUser() user: AuthenticatedUser) {
    return this.trackingService.getUserSessions(user.userId);
  }

  @Get('tracking/sessions/:id')
  @ApiOperation({ summary: 'Get tracking session details with route points' })
  @ApiResponse({ status: 200, description: 'Session details with points and stops' })
  async getSessionDetails(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') sessionId: string,
  ) {
    return this.trackingService.getSessionDetails(user.userId, sessionId, false);
  }

  // --- ADMIN ENDPOINTS ---

  @Get('admin/tracking/sessions')
  @UseGuards(AdminGuard)
  @ApiOperation({ summary: 'Get all tracking sessions for admin overview' })
  @ApiQuery({ name: 'userId', required: false, type: String })
  @ApiResponse({ status: 200, description: 'List of sessions with user details' })
  async getAdminSessions(@Query('userId') userId?: string) {
    return this.trackingService.getAdminSessions(userId);
  }

  @Get('admin/tracking/sessions/:id')
  @UseGuards(AdminGuard)
  @ApiOperation({ summary: 'Get full route and stops of a session for admin map review' })
  @ApiResponse({ status: 200, description: 'Session details with full coordinate trail' })
  async getAdminSessionDetails(
    @CurrentUser() user: AuthenticatedUser,
    @Param('id') sessionId: string,
  ) {
    return this.trackingService.getSessionDetails(user.userId, sessionId, true);
  }
}
