import {
  Controller,
  Get,
  Post,
  Patch,
  Body,
  Param,
  UseGuards,
} from '@nestjs/common';
import {
  ApiTags,
  ApiOperation,
  ApiResponse,
  ApiBearerAuth,
} from '@nestjs/swagger';
import { JwtAuthGuard } from '../../common/guards/jwt-auth.guard';
import { AdminGuard } from '../../common/guards/admin.guard';
import { AdminService } from './admin.service';
import { AdminUserDto } from './dto/admin-user.dto';
import { AdminSiteDto } from './dto/admin-site.dto';
import { CreateUserDto } from './dto/create-user.dto';
import { UpdateUserSiteDto } from './dto/update-user-site.dto';
import { UpdateUserRoleDto } from './dto/update-user-role.dto';

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
}
