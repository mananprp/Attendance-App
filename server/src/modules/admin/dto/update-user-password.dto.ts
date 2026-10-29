import { ApiProperty } from '@nestjs/swagger';
import { IsNotEmpty, IsString, MinLength } from 'class-validator';

export class UpdateUserPasswordDto {
  @ApiProperty({ example: 'NewPassword@123', description: 'New password for the user' })
  @IsString()
  @IsNotEmpty()
  @MinLength(6)
  password: string;
}
