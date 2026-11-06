import { Type } from 'class-transformer';
import {
  IsArray,
  IsBoolean,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  ValidateNested,
} from 'class-validator';

export class TrackingPointDto {
  @IsNotEmpty()
  @IsString()
  id: string;

  @IsNotEmpty()
  @IsNumber()
  latitude: number;

  @IsNotEmpty()
  @IsNumber()
  longitude: number;

  @IsOptional()
  @IsNumber()
  accuracyM?: number;

  @IsNotEmpty()
  @IsNumber()
  recordedAtUtc: number;

  @IsOptional()
  @IsBoolean()
  isPausedGap?: boolean;
}

export class TrackingStopDto {
  @IsNotEmpty()
  @IsString()
  id: string;

  @IsNotEmpty()
  @IsNumber()
  latitude: number;

  @IsNotEmpty()
  @IsNumber()
  longitude: number;

  @IsOptional()
  @IsString()
  stopName?: string;

  @IsNotEmpty()
  @IsNumber()
  arrivedAtUtc: number;
}

export class FinishTrackingSessionDto {
  @IsNotEmpty()
  @IsNumber()
  endTimeUtc: number;

  @IsNotEmpty()
  @IsNumber()
  totalDistanceM: number;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => TrackingPointDto)
  points: TrackingPointDto[];

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => TrackingStopDto)
  stops: TrackingStopDto[];
}
