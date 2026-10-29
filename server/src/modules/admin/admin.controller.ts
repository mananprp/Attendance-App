import {
  Controller,
  Get,
  Post,
  Patch,
  Delete,
  Body,
  Param,
  Query,
  Res,
  UseGuards,
} from '@nestjs/common';
import { Response } from 'express';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { AdminGuard } from '../../common/guards/admin.guard';
import {
  CurrentUser,
  AuthenticatedUser,
} from '../../common/decorators/current-user.decorator';
import { AdminService } from './admin.service';
import { AdminUserDto } from './dto/admin-user.dto';
import { AdminSiteDto } from './dto/admin-site.dto';
import { CreateUserDto } from './dto/create-user.dto';
import { CreateSiteDto } from './dto/create-site.dto';
import { UpdateUserSiteDto } from './dto/update-user-site.dto';
import { UpdateUserRoleDto } from './dto/update-user-role.dto';
import { UpdateUserPasswordDto } from './dto/update-user-password.dto';
import { AdminPunchDto } from './dto/admin-punch.dto';
import { AdminPunchFilterDto } from './dto/admin-punch-filter.dto';
import { AdminDailyAttendanceDto } from './dto/admin-daily-attendance.dto';

@ApiTags('admin')
@ApiBearerAuth()
@UseGuards(JwtAuthGuard, AdminGuard)
@Controller('admin')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('sites')
  @ApiOperation({ summary: 'List all configured office sites' })
  @ApiResponse({ status: 200, type: [AdminSiteDto] })
  async getSites(): Promise<AdminSiteDto[]> {
    return this.adminService.getSites();
  }

  @Post('sites')
  @ApiOperation({ summary: 'Create a new office site location' })
  @ApiResponse({ status: 201, type: AdminSiteDto })
  async createSite(@Body() dto: CreateSiteDto): Promise<AdminSiteDto> {
    return this.adminService.createSite(dto);
  }

  @Get('users')
  @ApiOperation({ summary: 'List all users with their roles and assigned sites' })
  @ApiResponse({ status: 200, type: [AdminUserDto] })
  async getUsers(): Promise<AdminUserDto[]> {
    return this.adminService.getUsers();
  }

  @Post('users')
  @ApiOperation({ summary: 'Create a new user account' })
  @ApiResponse({ status: 201, type: AdminUserDto })
  async createUser(@Body() dto: CreateUserDto): Promise<AdminUserDto> {
    return this.adminService.createUser(dto);
  }

  @Patch('users/:userId/site')
  @ApiOperation({ summary: 'Update assigned site for a user' })
  @ApiResponse({ status: 200, type: AdminUserDto })
  async updateUserSite(
    @Param('userId') userId: string,
    @Body() dto: UpdateUserSiteDto,
  ): Promise<AdminUserDto> {
    return this.adminService.updateUserSite(userId, dto.site_code);
  }

  @Patch('users/:userId/role')
  @ApiOperation({ summary: 'Update role (ADMIN/EMPLOYEE) for a user' })
  @ApiResponse({ status: 200, type: AdminUserDto })
  async updateUserRole(
    @Param('userId') userId: string,
    @Body() dto: UpdateUserRoleDto,
  ): Promise<AdminUserDto> {
    return this.adminService.updateUserRole(userId, dto.role);
  }

  @Patch('users/:userId/password')
  @ApiOperation({ summary: 'Update password for a user' })
  @ApiResponse({ status: 200, type: AdminUserDto })
  async updateUserPassword(
    @Param('userId') userId: string,
    @Body() dto: UpdateUserPasswordDto,
  ): Promise<AdminUserDto> {
    return this.adminService.updateUserPassword(userId, dto.password);
  }

  @Delete('users/:userId')
  @ApiOperation({ summary: 'Delete a user account' })
  @ApiResponse({ status: 200, description: 'User deleted successfully' })
  async deleteUser(
    @Param('userId') userId: string,
    @CurrentUser() adminUser: AuthenticatedUser,
  ): Promise<{ success: boolean; message: string }> {
    return this.adminService.deleteUser(userId, adminUser?.userId);
  }

  @Get('attendance')
  @ApiOperation({ summary: 'List consolidated daily attendance (1 row per user per day with In, Out, Shift, Status)' })
  @ApiResponse({ status: 200, type: [AdminDailyAttendanceDto] })
  async getAttendance(@Query() filter: AdminPunchFilterDto): Promise<AdminDailyAttendanceDto[]> {
    return this.adminService.getAllAttendance(filter);
  }

  @Get('attendance/export')
  @ApiOperation({ summary: 'Export consolidated daily attendance to CSV with optional user/date/site filtering' })
  async exportAttendanceCsv(
    @Query() filter: AdminPunchFilterDto,
    @Res() res: Response,
  ): Promise<void> {
    const csv = await this.adminService.exportAttendanceCsv(filter);
    const timestamp = new Date().toISOString().slice(0, 10);
    const userSuffix = filter.user_id && filter.user_id.trim() ? `_${filter.user_id.trim()}` : '';
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader(
      'Content-Disposition',
      `attachment; filename="attendance${userSuffix}_${timestamp}.csv"`,
    );
    res.send(csv);
  }

  @Get('punches')
  @ApiOperation({ summary: 'List all punches with optional filtering by date range, user, site' })
  @ApiResponse({ status: 200, type: [AdminPunchDto] })
  async getPunches(@Query() filter: AdminPunchFilterDto): Promise<AdminPunchDto[]> {
    return this.adminService.getAllPunches(filter);
  }

  @Get('punches/export')
  @ApiOperation({ summary: 'Export punches to CSV with optional filtering' })
  async exportPunchesCsv(
    @Query() filter: AdminPunchFilterDto,
    @Res() res: Response,
  ): Promise<void> {
    const csv = await this.adminService.exportPunchesCsv(filter);
    const timestamp = new Date().toISOString().slice(0, 10);
    res.setHeader('Content-Type', 'text/csv');
    res.setHeader(
      'Content-Disposition',
      `attachment; filename="attendance_punches_${timestamp}.csv"`,
    );
    res.send(csv);
  }
}
